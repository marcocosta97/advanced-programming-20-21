# AP-20 – Programming Assignment #1

---

# Exercise 1 – Java Beans

## *Busses at the Time of Covid…*

Due to the coronavirus pandemic, bus capacity is reduced by **50%**.
We want to help the bus driver keep travellers safe by providing a system that allows passengers to enter only if there are available seats.

The system controls **only the entrance door** of the bus.

### Scenario

* Passengers at a bus stop can **book entrance** to the bus.
* When the bus stops:

  * The entrance door **opens** (if allowed) and stays open for a few seconds so passengers can enter.
  * Otherwise, the door remains **closed**, and the booking is canceled.
* Passengers can always **leave the bus**.

---

## System Architecture

The system is composed of:

1. A **Graphical Dashboard**
2. A **Bus Bean**
3. A **CovidController Bean**

---

## The `Bus` Bean

A **non-visual Java Bean** with the following properties:

| Property        | Type      | Initial Value | Notes                              |
| --------------- | --------- | ------------- | ---------------------------------- |
| `capacity`      | `int`     | `50`          | Maximum number of passengers       |
| `doorOpen`      | `boolean` | `false`       | **Bound** property                 |
| `numPassengers` | `int`     | `20`          | **Bound and Constrained** property |

### Behavior

#### `activate()` method

* Uses a **timer**
* Decreases `numPassengers` randomly every few seconds
* `numPassengers` must **never become negative**

#### When `numPassengers` is increased:

* If:

  * The new value **does not exceed capacity**
  * The change is **not blocked by a veto**
* Then:

  1. `doorOpen` is set to `true`
  2. The door remains open for **3 seconds**
  3. `doorOpen` is set back to `false`
  4. `numPassengers` is updated

### ⚠ Important

The `Bus` must correctly enforce the **capacity constraint** independently of `CovidController`.
It must be reusable in other projects.

---

## The `CovidController` Bean

A **non-visual Java Bean** with:

| Property          | Type  | Initial Value |
| ----------------- | ----- | ------------- |
| `reducedCapacity` | `int` | `25`          |

It implements:

```java
VetoableChangeListener
```

### Behavior

The `vetoChange()` method:

* Forbids updates to `numPassengers` if:

  ```
  newValue > reducedCapacity
  ```

---

## The Graphical Dashboard – `BusBoard`

* Must extend `JFrame`
* Uses a `Bus` and a `CovidController` bean

### Required Components

* A component showing `numPassengers`
* A component showing `doorOpen`
* A text field to enter an integer between **1–5**

  * Default value: `1`
* A button to request entrance

### Button Behavior

When clicked:

1. Button **immediately changes color**
2. After **2 seconds**:

   * `setNumPassenger()` is invoked
   * The property is increased by the amount in the text field
3. Regardless of success or failure:

   * The text field and button are reset

---

## Solution Format – Exercise 1

Submit:

* `Bus.java`
* `CovidController.java`
* `BusBoard.java`
* The corresponding **JAR files**

The `BusBoard` JAR may include copies of:

* `Bus.class`
* `CovidController.class`

---

# Exercise 2 – Java Reflection and Annotations

## *XML Serialization*

XML is a meta-language used to describe structured documents in a machine-readable format.

👉 Introduction:
[https://docs.microsoft.com/en-us/previous-versions/windows/desktop/ms766385(v=vs.85)](https://docs.microsoft.com/en-us/previous-versions/windows/desktop/ms766385%28v=vs.85%29)

---

## Example XML

```xml
<Student>
    <firstName type="String">Jane</firstName>
    <surname type="String">Doe</surname>
    <age type="int">42</age>
</Student>
```

---

## Goal

Implement a Java serializer:

```java
void serialize(Object[] arr, String fileName)
```

### Assumptions

* All objects in `arr` belong to the same class `C`
* Output is written to:

```
fileName.xml
```

The serializer must use **annotations** to determine what to serialize.

---

## Required Annotations

### `@XMLable`

* Marks a class as serializable
* If absent → method returns immediately
* The main XML tag is the class name

---

### `@XMLfield`

Marks serializable fields.

#### Constraints:

* Only:

  * Primitive types
  * `String`

#### Arguments:

| Argument | Type     | Required | Description                              |
| -------- | -------- | -------- | ---------------------------------------- |
| `type`   | `String` | ✅ Yes    | Field type (e.g. `"int"`, `"String"`)    |
| `name`   | `String` | ❌ No     | XML tag name (defaults to variable name) |

---

## Example Java Class

```java
@XMLable
public class Student {

    @XMLfield(type = "String")
    public String firstName;

    @XMLfield(type = "String", name="surname")
    public String lastName;

    @XMLfield(type = "int")
    private int age;

    public Student(){}

    public Student(String fn, String ln, int age) {
        this.firstName = fn;
        this.lastName = ln;
        this.age = age;
    }
}
```

Serializing:

```java
new Student("Jane", "Doe", 42);
```

Produces the XML shown earlier.

---

## Solution Format – Exercise 2

Submit:

* `XMLable.java`
* `XMLfield.java`
* `XMLSerializer.java`

Additionally (for testing):

* A sample annotated class:

  * At least **two different field types**
  * Uses `@XMLfield`:

    * With optional argument
    * Without optional argument
* A `Main` class that:

  * Builds an array of objects
  * Calls `XMLSerializer.serialize()`

---

# Exercise 3 (Optional) – XML Deserialization

Serialization is meaningful only if accompanied by deserialization.

You must:

1. Read an XML file produced by `XMLSerializer.serialize()`
2. Recreate the array of objects encoded in the file

---

## A Class is "Deserializable" If:

* Annotated with `@XMLable`
* Has a **no-argument constructor**
* All fields:

  * Are non-static
  * Are primitive types or `String`
  * Are annotated with `@XMLfield`

The `Student` class shown above satisfies these conditions.

---

## Requirements

Write a Java program that:

1. Reads the XML file
2. Uses reflection to verify the class is "deserializable"
3. If valid:

   * Returns an array of deserialized objects

---

## Solution Format – Exercise 3

**Free format**

---

# Summary

This assignment covers:

* Java Beans
* Bound and Constrained properties
* Event handling and veto mechanisms
* Swing GUI
* Java Reflection
* Custom annotations
* XML serialization and deserialization
