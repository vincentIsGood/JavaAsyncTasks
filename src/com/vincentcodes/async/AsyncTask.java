package com.vincentcodes.async;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @param <T> result type of this async task (to be passed to {@link #resolve})
 */
@SuppressWarnings("unchecked")
public final class AsyncTask<T> {
    /**
     * Result of the current task
     */
    private Object result;
    private Consumer<Object> thenCallback;
    private Consumer<Object> catchCallback;
    private AsyncTaskState state;

    private boolean waitForResult = false;
    private boolean resolveRejectDone = false;

    /**
     * @param callback takes resolve, reject function pointers as argument
     */
    public AsyncTask(BiConsumer<Consumer<T>, Consumer<Object>> callback){
        state = AsyncTaskState.PENDING;
        callMain(callback);

        // we don't want this here because newly chained AsyncTask will run immediately causing a race condition
        //
        // A reasonable solution is to make resolve / reject async. Then, everything is in order.
        // ie. task1.resolve() -> task2.resolve() -> task3.resolve()
        // instead of: -> task1
        //             -> task2
        // EventLoop.EVENT_LOOP.submit(this);
    }

    public void callMain(BiConsumer<Consumer<T>, Consumer<Object>> callback) {
        try{
            callback.accept(this::resolve, this::reject);
        }catch(Exception e){
            reject(e);
        }
    }

    /**
     * To illustrate how this works, an example will suffice:
     *   AsyncTask.then(func1).then(func2);
     * 
     * Gives us an execution flow (simplified) like this: 
     *   AsyncTask (task1) -> then(func1), task1 += func1 
     *   -> AsyncTask (task2, ret from then()) -> then(func2), task2 += func2
     *   -> AsyncTask (task3)
     *   -> done.
     * 
     * Finally, tasks are executed in the following order: task1 -> task2 -> task3.
     */
    public <R> AsyncTask<R> then(Function<T,R> thenCallback, Function<Object,Object> catchCallback){
        // AsyncTask.then.then  =>  "new async" -> resolve -> "new async" -> resolve -> "new async"
        //
        // This is where you can see AsyncTask2 ref AsyncTask1
        // the following shows AsyncTask1 then/catch callback is set after resolve
        //
        // when AsyncTask is created, the following thing happens (#callMain(callback))
        return new AsyncTask<>((resolve, reject) -> {
            // AsyncTask1 stuff is IN AsynTask2
            // AsyncTask2.resolve(result) is called automatically
            //
            // Data flow: AsyncTask1.result -> AsyncTask2.thenCallback(result)
            this.thenCallback = result -> {
                // ie. if we have catchCallback. Then, skip catch (call resolve() to skip it)
                if(thenCallback == null){
                    resolve.accept((R)result);
                }
                try{
                    resolve.accept((R)thenCallback.apply((T)result));
                }catch(Exception e){
                    reject.accept(e);
                }
            };

            this.catchCallback = result -> {
                // ie. skip the then-callback
                if(catchCallback == null){
                    reject.accept(result);
                }
                try{
                    resolve.accept((R)catchCallback.apply(result));
                }catch(Exception e){
                    reject.accept(e);
                }
            };
            
            // run the new then&catch callback AsyncTask1 we JUST added up there.
            runThenOrCatchCallback();
        });
    }

    public <R> AsyncTask<R> then(Function<T,R> callback){
        return then(callback, null);
    }

    public AsyncTask<Object> catchErr(Function<Object,Object> callback){
        return then(null, callback);
    }

    public T await(){
        // we'll wait for the async task to finish as defined in resolve / reject
        waitForResult = true;
        while(waitForResult && !resolveRejectDone){
            synchronized(this){
                try{
                    // Wanna make sure we break
                    if(resolveRejectDone) break;
                    this.wait();
                }catch(InterruptedException e){
                    Thread.currentThread().interrupt();
                    System.out.println("Thread interrupted");
                }
            }
        }
        return result == null? null : (T)result;
    }

    /**
     * @param callback does not take any arguments (in JavaScript). Here, it takes null as argument
     */
    public AsyncTask<T> thenFinally(VoidFunction callback){
        // We want both then() and catch() to have finally(), so we the same function to both.
        return then((result)->{
            callback.apply();
            return result; // pass result to next callback (eg. then()) untouched
        }, (result)->{
            callback.apply();
            return result;
        });
    }

    private void runThenOrCatchCallback(){
        // `null` when this AsyncTask is the last one in the chain
        if(this.state == AsyncTaskState.FULFILLED && thenCallback != null){
            thenCallback.accept(result);
        }else if(this.state == AsyncTaskState.REJECTED && catchCallback != null)
            catchCallback.accept(result);
    }
    
    // ----------- In one task, we either have RESOLVE or REJECT ----------- //
    private void resolve(Object result){
        EventLoop.EVENT_LOOP.submit(()->{
            // we do want multiple RESOLVE / REJECTED to run this code.
            if(state != AsyncTaskState.PENDING) return;
            this.result = result;
            state = AsyncTaskState.FULFILLED;
            runThenOrCatchCallback();

            synchronized(this){
                resolveRejectDone = true;
                if(waitForResult){
                    waitForResult = false;
                    this.notify();
                }
            }
            return;
        });
    }

    private void reject(Object result){
        EventLoop.EVENT_LOOP.submit(()->{
            if(state != AsyncTaskState.PENDING) return;
            this.result = result;
            state = AsyncTaskState.REJECTED;
            runThenOrCatchCallback();
            
            synchronized(this){
                resolveRejectDone = true;
                if(waitForResult){
                    waitForResult = false;
                    this.notify();
                }
            }
            return;
        });
    }
}

@FunctionalInterface
interface VoidFunction{
    void apply();
}