# Java Promise implementation project
Inspired by the event loop technology with promise used in JavaScript (eg. node.js). 
I am motivated to create one on my own in Java. 

Overall, I have learned a lot in this project. Making generic types work is certainly
hard, though I found that start building from Object as generic types saved me a lot 
of time.

## Important
This project is not to be used commercially. It is a simple implementation to realize
the concepts. If you want real professional stuff, check out Project Reactor Core.

```js
// Basic Promise in JavaScript (Syntax)
const myPromise = new Promise((resolve, reject) => {
    // do sth...
    if(error)
        reject(errorData);
    resolve("return value");
});
myPromise
  .then(value => { return value + ' and bar'; })
  .then(value => { return value + ' and bar again'; })
  .then(value => { return value + ' and again'; })
  .then(value => { return value + ' and again'; })
  .then(value => { console.log(value) })
  .catch(err => { console.log(err) })
  .finally(() => { /* No argument. Do sth. */ });
```

## References
https://www.youtube.com/watch?v=1l4wHWQCCIc
https://www.geeksforgeeks.org/node-js-event-loop/