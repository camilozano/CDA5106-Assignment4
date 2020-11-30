# LockFree Stack

## How To Run

### Compile
    $ javac EliminationBackoffStack.java LockFreeStack.java Runner.java Main.java
### Run

    $ java Main
### Parameters
In main.java, change any of the int variables to vary the run
The ratio variables should add up to 100

The Runner.run function is a single run of a given STACK_TYPE (either *Runner.STACK_TYPE.{ELIM_STACK,LOCK_STACK}*)

The Runner.one_to_n_threads function runs both stacks for the given inputs from 1 to n threads. It can either be random or ratio based (*Runner.RUN_TYPE.{RAND,RATIO}*)
Do note that using this function allows the stacks to warm up and give faster speed times

All results are printed in milliseconds

## Problem 1  - *LockFreeStack.java*

The stack was implemented by wrapping a simple Node\<T\> object around an AtomicReference. Each Node object has a next value that is a Node, and a value which is  T.  

The push function simply makes a new Node with the value, and makes its next value point to the old head. Then it performs a compare and set on the head Node to replace it with the new one. 
Similarly the pop function takes the head node next value and makes it the new head and returns the old head when successful.  

This is merely a lock free implementation as it does not provide any wait free guarantees. No backoff method was implemented on this version of the stack, so it relies on having a *while(true)* loop to eventually get a compare and set done. 
In regards to the numOps counter, it had originally been kept as an int and incremented between threads but this was giving inconsistent results, it was unclear whether or not this was supposed to be replaced too with an atomic counter but it was done in order to maintain consistency. 
