# Assignment: Haskell, Java Stream API & Python

**Version 1.0 — December 15, 2020**

---

## Instructions

This assignment is divided into **three parts**, consisting of exercises on:

* **Haskell**
* **Java Stream API**
* **Python**

⚠️ **Warning:** This document is subject to changes. Always ensure you are reading the most recent version.

---

# Part 1 — Implementing Multisets in Haskell

This part requires implementing a type constructor providing multiset functionality.

Your implementation must be based on the following concrete Haskell definition:

```haskell
data ListBag a = LB [(a, Int)]
  deriving (Show, Eq)
```

A `ListBag` contains a list of pairs:

* The first component is the element
* The second component is its **multiplicity** (number of occurrences)

A `ListBag` is **well-formed** if it does not contain two pairs `(v, k)` and `(v', k')` such that:

```
v = v'
```

---

## Exercise 1 — Constructors and Operations

The goal of this exercise is to implement multisets represented as `ListBag`.

Your implementation must:

* Be well documented
* Pass the provided tests

---

### 1. Well-formedness predicate

Implement the predicate `wf` such that:

* `wf bag == True` **if and only if** `bag` is well-formed

The inferred type must be:

```haskell
wf :: Eq a => ListBag a -> Bool
```

✅ **Important:** All operations returning a `ListBag` must ensure that the result is well-formed:

```
wf bag == True
```

---

### 2. Constructors

Implement the following constructors:

* `empty`
  Returns an empty `ListBag`

* `singleton v`
  Returns a `ListBag` containing exactly one occurrence of element `v`

* `fromList lst`
  Returns a `ListBag` containing all and only the elements of `lst`, each with correct multiplicity

---

### 3. Operations

Implement the following operations:

* `isEmpty bag`
  Returns `True` if and only if `bag` is empty

* `mul v bag`
  Returns the multiplicity of `v` in `bag`, or `0` if `v` is not present

* `toList bag`
  Returns a list containing all the elements of `bag`, each repeated according to its multiplicity

* `sumBag bag bag'`
  Returns the `ListBag` obtained by adding all elements of `bag'` to `bag`

---

### Testing

The attached files:

* `testEx1.hs`
* `testEx12.hs`

contain tests for this exercise.

To run the tests, you may need to install the `Test.HUnit` module.
Then load the file in the interpreter and execute:

```haskell
main
```

⚠️ **Note:** Solutions that do not pass these tests will not be evaluated.

---

### Solution Format (Exercise 1)

Submit a Haskell source file:

* `Ex1.hs`

Requirements:

* Must contain a module called `Ex1`
* Must define the type `ListBag` (copy the provided definition)
* Must define at least all the required functions

Additional requirements:

* File must be adequately commented
* Every function definition must be preceded by its inferred type signature

---

## Exercise 2 — Mapping and Folding

The goal is to experiment with type class constructors by extending the module from Exercise 1.

---

### 1. Foldable instance

Define an instance of the type class `Foldable` for `ListBag`.

Use the minimal set of required functions, as described in the official `Foldable` documentation.

**Important interpretation:** folding a `ListBag` should apply the folding function to the elements of the multiset **ignoring multiplicities**.

---

### 2. `mapLB`

Define a function:

```haskell
mapLB :: (a -> b) -> ListBag a -> ListBag b
```

It returns the `ListBag` obtained by applying the function to all elements.

---

### 3. Functor explanation

Explain (as a comment in the same file) why it is **not possible** to define a valid instance of `Functor` for `ListBag` by using `mapLB` as the implementation of `fmap`.

---

### Solution Format (Exercise 2)

Submit a Haskell source file:

* `Ex2.hs`

Requirements:

* Must contain a module called `Ex2`
* Must import module `Ex1`
* Must contain only the new functions introduced in this exercise

Additional requirements:

* File must be adequately commented
* Each function must be preceded by its inferred type signature

---

# Part 2 — A Map-Reduce Framework Using Java Stream API

The Map-Reduce paradigm is widely used for processing large amounts of data in parallel and distributed systems.

In this assignment, students must implement a simplified Map-Reduce framework:

* **ignoring parallelism**
* **ignoring distribution**

Two example applications of the framework must also be implemented.

---

## References

* *MapReduce: Simplified Data Processing on Large Clusters*
* Wikipedia overview:
  [https://en.wikipedia.org/wiki/MapReduce#Dataflow](https://en.wikipedia.org/wiki/MapReduce#Dataflow)
  (Partition function can be ignored)

---

## Solution Format (Part 2)

Submit a zip archive named:

```
MapReduce-<yourSurname>.zip
```

The archive must include the Java files implementing:

* Exercise 3
* Exercise 4
* (optionally) Exercise 5

If using NetBeans, submit the entire project.

---

## Exercise 3 — The Framework

Following the guidelines presented in the lesson of **October 23, 2020**:

[http://pages.di.unipi.it/corradini/Didattica/AP-20/index.html#framework](http://pages.di.unipi.it/corradini/Didattica/AP-20/index.html#framework)

Using the **Template Method Design Pattern**, implement a Java Map-Reduce framework.

---

### Constraints

* Use the provided class `Pair.java` for key/value pairs

  * You may change the package
  * You may not modify anything else

* Hot spots of the framework must be:

  * `read`
  * `map`
  * `compare`
  * `reduce`
  * `write`

* The framework must use the **Stream API** whenever possible

Example expectation:

* `map` takes a stream of key/value pairs and returns a stream of key/value pairs (types may differ)

---

## Exercise 4 — Counting Words

Instantiate the framework to implement a program that counts occurrences of words with:

```
length > 3
```

---

### Requirements

* Ask the user for the **absolute path** of the directory containing documents
* Only consider files ending in:

```
.txt
```

---

### Read Function

Must return a stream of pairs:

```
(fileName, contents)
```

Where:

* `fileName` is the name of the file
* `contents` is a list of strings (one per line)

You may use the provided `Reader.java` as desired.

---

### Map Function

Must take the output of `read` and return a stream of pairs containing, for each word `w` (length > 3) appearing in a line:

```
(w, k)
```

Where:

* `k` is the number of occurrences of `w` in that line

---

### Compare Function

Must compare strings using standard alphanumeric ordering (Java convention).

See `compareTo()` in `Comparable`.

---

### Reduce Function

Takes as input a stream of pairs:

```
(w, lst)
```

Where:

* `w` is a string
* `lst` is a list of integers

Returns a stream of pairs:

```
(w, sum)
```

Where:

* `sum` is the sum of all integers in `lst`

---

### Write Function

Takes the output of `reduce` and writes it into a CSV file:

* One pair per line
* Output must be sorted in alphanumeric order

You may use the provided `Writer.java`.

---

### Testing Data

For testing, use the provided archive:

* `Books.zip`

Containing excerpts of books from Project Gutenberg.

(Reference note: see *Raffaele Angius, Perché il Progetto Gutenberg sarà sotto sequestro per sempre*.)

---

## Exercise 5 (Optional) — Producing an Inverted Index

Instantiate the framework to implement an **Inverted Index** for words with:

```
length > 3
```

---

### Output Requirements

Given a directory path, produce a CSV file containing one line per occurrence:

```
w, filename, line
```

Where:

* `w` is a word of length > 3
* `filename` is the file where it appears
* `line` is the line number in which it appears

The output lines must be sorted in the natural order.

---

# Part 3 — Benchmarking Python Functions with Multithreading

You must benchmark Python functions and ensure benchmarking is performed correctly.

---

## Solution Format (Part 3)

Submit a single Python file named:

* `benchmark.py`

Containing the solutions to Exercises 6, 7, and 8.

---

## Exercise 6 — A Decorator for Benchmarking

Define a Python decorator named:

```python
benchmark
```

When a function `fun` decorated with `benchmark` is invoked:

* It executes the function multiple times (discarding results)
* Prints a small table showing:

  * average execution time
  * variance

---

### Optional Parameters

The decorator must support the following optional parameters:

* `warmups`
  Number of warm-up runs whose timing is ignored
  Default:

  ```python
  warmups = 0
  ```

* `iter`
  Number of runs to benchmark
  Default:

  ```python
  iter = 1
  ```

* `verbose`
  If `True`, prints timing of each warm-up and run
  Default:

  ```python
  verbose = False
  ```

* `csv_file`
  If provided, writes results to a CSV file with header:

  ```
  run num, is warmup, timing
  ```

  Default:

  ```python
  csv_file = None
  ```

If `csv_file = None`, results are only displayed on screen.

---

## Exercise 7 — Testing the Decorator with Multithreading

Test your implementation and evaluate the effectiveness of multithreading in Python.

---

### Requirements

Using the `threading` module and the benchmark decorator, write a function:

```python
test
```

The function takes another function `f` as argument and executes it with different numbers of iterations and threads.

The test must execute:

| Threads | Iterations per Thread | Total Runs |
| ------- | --------------------- | ---------- |
| 1       | 16                    | 16         |
| 2       | 8                     | 16         |
| 4       | 4                     | 16         |
| 8       | 2                     | 16         |

---

### Output Files

Benchmark information must be written into files named:

```
f_<numthreads>_<numiterations>
```

Example:

```
f_4_4
```

---

### Benchmark Function to Use

Run the program using a function that computes the `n`-th Fibonacci number using the standard inefficient double-recursive method.

Choose `n` carefully.

---

### Discussion

Discuss briefly the results in a comment inside the Python file.

---

# Metadata

**Author:** Andrea Corradini & Laura Bussi
**Created:** 2020-12-15 Tue 01:40
**Version:** 1.0
**Status:** Validate

---
