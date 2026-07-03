# Petar

**Property Extraction from Trees to Annotated Representations**

Petar is a declarative language for defining properties over generic trees and extracting the properties satisfied by a given tree.

The language is based on three core concepts:

- **A type system** for representing values and properties.
- **Structural patterns** for matching nodes within a tree.
- **Production rules** for deriving new annotations from existing properties.

Petar is completely domain-independent. It can be applied to syntax trees, semantic trees, dependency trees, or any other tree-based representation.

Given an input tree in the right format, annotation rules derive increasingly specialized properties by producing new annotations from existing properties.

For example, when analyzing an Abstract Syntax Tree (AST), one may successively annotate:

- Repetitions
- Counted repetitions
- Complete traversals

This layered annotation model enables complex semantic information to be built from simple structural observations.

---

# Type System

## Primitive Value Types

Primitive types represent raw values only. They do not carry semantic information beyond the value they contain.

### `String`

Represents a Unicode character sequence.

#### Syntax

```petar
"Hello"
'Hello'
"123"
```

---

### `Number`

Represents numeric values.

There is no distinction between integers and floating-point numbers.

#### Examples

```petar
42
-3.14
0
```

---

### `Boolean`

Represents a boolean value.

#### Constants

```petar
True
False
```

---

### `Any`

Represents any non-null value regardless of its type.

There is no literal constant of type `Any`; it is a type constraint.

Examples of values assignable to `Any` include:

```petar
True
0
"Hello World"
```

---

### `Undefined`

Represents the absence of a value.

Whenever an optional field is omitted, its value is `Undefined`.

#### Constant

```petar
undefined
```

---

## Homogeneous List Types

Petar supports homogeneous collections of values.

Lists may represent ordered or unordered collections depending on the context in which they are used.

### Type Syntax

```petar
<Type>[]
```

### Value Syntax

```petar
[
    element1,
    element2,
    element3
]
```

---

# Properties

Properties are the fundamental semantic objects manipulated by Petar.

A property consists of a type name and a set of typed fields.

## Declaring a Property

### Syntax

```petar
property PropertyName(
    field1: Type1,
    field2: Type2
)
```

### Example

```petar
property Variable(
    name: String,
    mutable: Boolean
)
```

---

## Empty Properties

A property may define no fields.

Such properties usually represent the presence or absence of a particular characteristic.

### Example

```petar
property Leaf()
```

This property simply states that a node satisfies the concept of a leaf.

---

## Optional Fields

Fields may be declared optional using the `?` suffix.

```petar
property Variable(
    name: String,
    type?: String
)
```

Internally, an optional field has the type:

```text
String | Undefined
```

---

# Property Inheritance

Properties may inherit from another property.

A child property automatically contains all fields defined by its parent while allowing additional fields to be introduced.

A child may also **restrict** the type of inherited fields.

## Syntax

```petar
property Child(
    ...
)
: Parent(
    inheritedField: RestrictedType
)
```

When an instance of the child property is created, every inherited field must satisfy the restrictions declared by the child.

## Example

```petar
property Expression()

property BinaryExpression(
    left: Expression,
    right: Expression
)

property Addition(
    left: Expression,
    right: Expression
)
: BinaryExpression()
```

In this example:

- `Addition` inherits all fields from `BinaryExpression`.
- No fields need to be duplicated.
- Additional constraints or fields may be introduced if necessary.

---

The following chapter introduces **patterns**, the mechanism used to match tree structures and capture values for later processing.

# Patterns

Patterns are used to match properties and select the nodes to which production rules should be applied.

A property **matches** a pattern when it satisfies all structural and conditional constraints defined by that pattern.

Patterns can:

- Match a property by type
- Match field values
- Match nested structures
- Match ordered lists
- Capture matched values into local variables
- Define additional boolean conditions (guards)

---

## Basic Property Matching

The simplest pattern matches every property of a given type.

```petar
PropertyA()
```

Matches every property whose type is `PropertyA` (or one of its descendants).

---

Fields may also be constrained.

```petar
PropertyA(name = "Hello World")
```

Matches only if the `name` field equals `"Hello World"`.

---

Patterns may recursively describe nested properties.

```petar
PropertyA(
    name = "Hello World",
    children = [
        PropertyB()
    ]
)
```

Matches a `PropertyA` whose `children` field contains exactly one `PropertyB`.

---

# List Patterns

Lists can themselves be described using patterns.

Petar supports repetition operators inspired by regular expressions.

| Operator | Meaning |
|----------|---------|
| `*` | Zero or more occurrences |
| `+` | One or more occurrences |

For example:

```petar
[
    A()*,
    A()
]
```

Matches any ordered list containing at least one `A`.

Examples:

```text
✓ [A]
✓ [A, A]
✓ [A, A, A]
x [B, C, D]
x []
```

---

More advanced list matching is also possible.

```petar
[
    A()*,
    B(),
    A()+,
    B(),
    A()*
]
```

This matches an ordered list containing:

- one `B`
- followed by at least one `A`
- followed by another `B`

Examples:

```text
✓ [B, A, B]
✓ [A, A, B, A, A, B]
✓ [A, B, A, B, A]
```

---

# Capturing Values

Patterns may assign names to matched objects.

Captured values become local variables that can later be used inside:

- conditions
- production rules
- generated properties

A capture is written using the `#` operator.

---

## Capturing a Property

```petar
PropertyA() #root
```

Creates a local variable:

```text
root : PropertyA
```

---

## Capturing a Field

```petar
PropertyA(
    name = "Hello World" #name
) #root
```

Creates two variables:

```text
name : String
root : PropertyA
```

---

## Capturing Nested Properties

```petar
PropertyA(
    children = [
        PropertyB() #child
    ]
)
```

Creates:

```text
child : PropertyB
```

---

## Capturing List Segments

Entire portions of a matched list may also be captured.

```petar
[
    A()* #before,
    A() #last
]
```

Produces

```text
before : A[]
last : A
```

The variable `before` contains every matched element before the final `A`.

---

# Pattern Guards

Patterns may define additional boolean conditions.

A guard is introduced using the `->` operator.

The pattern only succeeds if the condition evaluates to `True`.

Example:

```petar
PropertyA() #root
    -> root.name = "Test"
```

This matches only properties whose `name` field equals `"Test"`.

Guards may use arbitrary expressions and combine multiple conditions.

```petar
PropertyA() #root
    -> root.name = "Test"
    && root.enabled
```

---

# Production Rules

Once a pattern matches, Petar may generate one or more new properties.

Production rules are introduced with the `=>` operator.

General syntax:

```petar
<Pattern>
    => <Produced Property>
```

---

## Simple Production

```petar
PropertyA() #root
    => PropertyB()
```

Whenever a `PropertyA` is matched, a new `PropertyB` annotation is produced.

---

## Using Captured Variables

Captured variables may be reused when constructing new properties.

```petar
PropertyA(
    name = "Hello World" #name
)
#root

=> PropertyC(
    name = name
)
```

The produced property receives the captured value.

---

## Duplicate Prevention

A production rule only creates an annotation if an annotation of the same produced type does **not** already exist for the matched property.

This guarantees that production rules are idempotent: executing the same rule multiple times does not create duplicate annotations.

---

# Example

The following rule illustrates the complete workflow.

```petar
PropertyA(
    name = "Hello"
) #node

-> node.enabled

=> PropertyB(
    label = node.name
)
```

Execution proceeds as follows:

1. Match every `PropertyA`.
2. Keep only those whose `name` equals `"Hello"`.
3. Evaluate the guard `node.enabled`.
4. Produce a `PropertyB`.
5. Reuse the captured value to initialize its `label` field.
6. Skip production if the annotation already exists.

# Expressions

Expressions are used in two main contexts:

- Pattern guards (conditions)
- Production rules (property construction)

They define how values are compared, accessed, computed, and queried inside Petar.

---

# Boolean Operators

Petar supports standard boolean logic operators:

| Operator | Meaning |
|----------|--------|
| `&&`     | Logical AND |
| `\|\|` | Logical OR |
| `!`      | Logical NOT |

## Examples

```petar
a = b && c
```

```petar
!(a = "Test" || b = "Test")
```

Boolean expressions can be arbitrarily nested.

---

# Equality and Pattern Matching

The `=` operator is used both for equality checks and structural pattern matching.

## Value Equality

```petar
a = 2
```

Checks that `a` equals `2`.

---

## Structural Matching

When used with properties, `=` performs structural validation:

```petar
node = Noeud()
```

Checks that `node` is of type `Noeud` (or a subtype).

---

```petar
node = Noeud(a = "Test")
```

Checks:

- `node` is a `Noeud` (or subtype)
- field `a` equals `"Test"`

---

```petar
node = Noeud(a = "Test", b = [Noeud() #c])
```

Checks both:

- field constraints
- nested structural constraints

---

# Data Access

Petar provides uniform access syntax for strings, lists, and properties.

---

## String Indexing

Strings behave like sequences of characters.

```petar
"Test"[0]   // "T"
"Test2"[-1] // "2"
```

---

## List Indexing

Lists support indexing and slicing.

```petar
[1, 2, 3, 4][0] // 1
```

---

## Property Field Access

Fields of a property are accessed using dot notation:

```petar
node.a
node.children[0].name
```

Access is recursively valid across nested structures.

---

# Subtree Search

Petar provides built-in functions to query tree structures.

---

## Existence Check

### Syntax

```petar
$exist(node, pattern)
```

Checks whether a given pattern exists anywhere in the subtree.

### Examples

```petar
$exist(node, "Test")
```

Checks if `"Test"` appears anywhere in `node`.

```petar
$exist(node, [Noeud(), Noeud()])
```

Checks whether a sequence of two `Noeud` exists in the descendants.

---

## Counting Matches

### Syntax

```petar
$count(node, pattern)
```

Returns the number of occurrences of a pattern in a subtree.

### Examples

```petar
$count(node, "Test")
```

Counts occurrences of `"Test"`.

```petar
$count(node, [Noeud(), Noeud()])
```

Counts occurrences of a two-node pattern.

---

# Regular Expression Matching

Petar supports regex-based validation over strings.

### Syntax

```petar
$regex(value, pattern)
```

### Example

```petar
$regex(value, "[a-z]*")
```

Checks that `value` contains only lowercase letters.

---

# Type Introspection

## Type Query

```petar
$type(node)
```

Returns the most specific type of a node as a string representation.

This is useful for debugging and dynamic analysis of annotations.

---

# Property Construction

Properties can be explicitly instantiated using constructor-like syntax.

---

## Basic Construction

```petar
Noeud()
```

Creates a `Noeud` instance.

---

## With Fields

```petar
Noeud2(value = 1)
```

Creates a `Noeud2` with field `value = 1`.

---

# General Constraints

A tree can only be processed by Petar if all its nodes can be typed using defined property types.

If a node cannot be typed, the tree is considered invalid in the Petar system.

---

# Summary

Expressions in Petar provide:

- Logical composition (`&&`, `||`, `!`)
- Structural equality and matching (`=`)
- Deep property access (`.` and indexing)
- Tree queries (`$exist`, `$count`)
- Regex validation (`$regex`)
- Type introspection (`$type`)
- Explicit construction of properties

Together, they form the evaluation layer used by patterns and production rules.

# Tutorial

This section demonstrates how Petar is used in practice to define and detect semantic patterns in trees.

The examples here focus on **annotation strategies**, where complex behaviors are derived from simple structural rules.

---

# Annotating Accumulations

## Idea

We want to detect assignments where the same variable appears on both sides of an expression.

Typical cases include:

- Self-increment patterns
- Indexed updates
- Recursive accumulation

---

## Examples

```text
a = a + 1
```

```text
counter[i] = counter[i] + 1
```

```text
acc = f(acc)
```

In all cases, the left-hand side also appears inside the right-hand expression.

---

## Type Model

We define a minimal AST model:

```petar
property Expression()

property Assignment(
    left: Expression,
    right: Expression
)

property Accumulation(): Assignment()
```

### Inheritance effect

`Accumulation` automatically inherits:

- `left`
- `right`

from `Assignment`.

---

## Annotation Rule

We define a rule that detects self-referential assignments.

```petar
Assignment() #assignment
    -> $exist(assignment.right, assignment.left)
=> Accumulation(): assignment
```

---

## Meaning

This rule reads as:

1. Match any `Assignment`
2. Capture it as `assignment`
3. Check whether `assignment.left` appears inside `assignment.right`
4. If yes, produce an `Accumulation` annotation
5. Copy the original assignment fields into the new annotation

---

## Result

For:

```text
a = a + 1
```

Petar produces:

```petar
Accumulation(
    left = a,
    right = a + 1
)
```

---

# Annotating Increment / Decrement Operations

## Idea

We normalize different syntactic forms of increment and decrement into a unified semantic representation.

We handle:

- `a++`
- `--a`
- `a += 1`
- `a -= 1`

All are transformed into equivalent assignment-based forms.

---

## Type Model

```petar
property Expression()

property Unary(
    operand: Expression,
    operator: String,
    isPrefix: Boolean
)

property Literal(
    value: String,
    targetType: String
)

property Binary(
    left: Expression,
    right: Expression,
    operator: String
)

property Accumulation(): Assignment()
```

---

# Rule 1 — Post/Pre Increment

## Increment (`++`)

```petar
Unary(
    operator = "++",
    operand = Expression() #operand
) =>
    Accumulation(): Assignment(
        left = operand,
        right = Binary(
            left = operand,
            right = Literal(
                value = "1",
                targetType = "Number"
            ),
            operator = "+"
        )
    )
```

---

## Decrement (`--`)

```petar
Unary(
    operator = "--",
    operand = Expression() #operand
) => 
    Accumulation(): Assignment(
        left = operand,
        right = Binary(
            left = operand,
            right = Literal(
                value = "1",
                targetType = "Number"
            ),
            operator = "-"
        )
    )
```

---

# Rule 2 — Compound Assignment (`+=`, `-=`)

## Increment Assignment

```petar
Binary(
    left = Expression() #operand,
    operator = "+=",
    right = Literal(value = "1")
) =>
    Accumulation(): Assignment(
        left = operand,
        right = Binary(
            left = operand,
            right = Literal(
                value = "1",
                targetType = "Number"
            ),
            operator = "+"
        )
)
```

---

## Decrement Assignment

```petar
Binary(
    left = Expression() #operand,
    operator = "-="
) =>
    Accumulation(): Assignment(
        left = operand,
        right = Binary(
            left = operand,
            right = Literal(
                value = "1",
                targetType = "Number"
            ),
            operator = "-"
        )
    )
```

---

# Conceptual Summary

This tutorial demonstrates Petar’s key strengths:

## 1. Structural Detection

We detect patterns in tree structures without hardcoding syntax.

## 2. Semantic Normalization

Different syntactic forms are unified into a single semantic representation.

## 3. Rule-Based Transformation

Transformations are expressed declaratively:

```petar
Pattern => Output
```

## 4. Self-Referential Reasoning

We can detect semantic relationships like:

- “left appears in right”
- “value reused in expression”
- “syntactic sugar expansion”

---

# End of Specification