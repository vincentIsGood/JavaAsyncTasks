package com.vincentcodes.tests;

import java.util.Arrays;
import java.util.stream.IntStream;

import com.vincentcodes.async.AsyncTask;

/**
 * Tbh, I should not do manual testings after the main functionalities have 
 * been implemented. But, whatever.
 */
public class Main {
    private static boolean suppressLog = false;

    public static void main(String[] args) throws InterruptedException {
        // check_real_async();
        test_await_should_return_value();
    }

    private static void test_basic(){
        AsyncTask<Object> task = new AsyncTask<String>((resolve, reject)->{
            resolve.accept("asd");
        }).then(str -> {
            printDebug("Str: " + str);
            throw new RuntimeException();
        }).catchErr(exception -> {
            printDebug("Except: " + exception.getClass());
            return null;
        });
    }

    private static void test_multiple_long_chains(){
        AsyncTask<Object> task = new AsyncTask<String[]>((resolve, reject)->{
            resolve.accept(new String[]{"I am vincent"});
        }).then(strArr -> {
            return strArr[0];
        }).then(str -> {
            printDebug(str.substring(5));
            return null;
        });
    }

    private static void test_exception_oncreate(){
        AsyncTask<Object> task = new AsyncTask<>((resolve, reject)->{
            throw new RuntimeException();
        }).catchErr(exception -> {
            printDebug("Except: " + exception.getClass());
            return null;
        });
    }

    private static void check_real_async(){
        AsyncTask<Object> task1 = new AsyncTask<int[]>((resolve, reject)->{
            resolve.accept(IntStream.range(0, 20).toArray());
        }).then(intArr -> {
            for(int i : (int[])intArr){
                printDebug("task1: " + i);
            }
            return "I'm done";
        }).then(str -> {
            printDebug(str);
            return null;
        });
        AsyncTask<Object> task2 = new AsyncTask<int[]>((resolve, reject)->{
            resolve.accept(IntStream.range(0, 20).toArray());
        }).then(intArr -> {
            for(int i : (int[])intArr){
                printDebug("task2: " + i);
            }
            return "I'm done";
        }).then(str -> {
            printDebug(str);
            return null;
        });
    }

    private static void test_await_should_return_value(){
        int[] result = new AsyncTask<int[]>((resolve, reject)->{
            resolve.accept(IntStream.range(0, 20).toArray());
        }).then(intArr -> {
            for(int i : (int[])intArr){
                printDebug("task1: " + i);
            }
            printDebug("I'm done");
            intArr[0] = 100;
            return intArr;
        }).await();
        printDebug(Arrays.toString(result));
        
        int[] result2 = new AsyncTask<int[]>((resolve, reject)->{
            resolve.accept(IntStream.range(0, 20).toArray());
        }).then(intArr -> {
            for(int i : (int[])intArr){
                printDebug("task1: " + i);
            }
            printDebug("I'm done");
            intArr[0] = 100;
            return intArr;
        }).await();
        printDebug(Arrays.toString(result2));

        String resultStr = new AsyncTask<String[]>((resolve, reject)->{
            resolve.accept(new String[]{"I am vincent"});
        }).then(strArr -> strArr[0])
        .then(str -> str.substring(5))
        .then(str -> str += " is shit")
        .await();
        printDebug(resultStr);
    }

    private static void printDebug(String msg){
        if(!suppressLog)
            System.out.println(msg);
    }
}
