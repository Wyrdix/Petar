# Petar

PETAR is a DSL used to produce annotated representation from tree-like data. 

This document explains how to use the CLI for the java interpreter.

This tool is distributed as a **JAR file** and is executed via `java -jar`.



**If you want to understand more about the Petar language go to the [Petar language specification](./Petar.md).**

---

# Requirements

- Java 17+ recommended
- A valid PETAR `.jar` file

---

# Basic Usage

```bash
java -jar petar.jar --spec <spec-files> -r <root type> [options]
```

At minimum, you must provide at least one **specification file** using `--spec` or `-s`.

---

# Command Options

## Required

### `--spec` / `-s`

Provide one or more specification files.

```
-s file1.petar file2.petar
--spec file1.petar file2.petar
```

- Type: `List<File>`
- Required: ✅ Yes

---

### `-r`

Root type to use for interpretation. This defines the entry point type used when interpreting the input tree.

```
-r Expression
```

- Type: `String`
- Required: ✅ Yes

---

## Optional

### `-i`

Input file to process.

```
-i input.json
```

- Type: `File`

If this is not specified the interpreter will parse and check the specification files provided.
This can be used to typecheck or namecheck Petar programs indecently from data.

---

### `-o`

Output file for results.

```
-o output.json
```

- Type: `File`

If omitted, output will be printed to standard output.

---

### `-p`

Print the loaded specification.

```
-p
```

- Type: `Boolean`
- Default: `false`

Useful for debugging and verifying that your `.petar` specification files are correctly loaded.

---

### `--help`

Displays help information about available commands.

```
--help
```

- Type: `Boolean`
- Default: `false`

When enabled, the program will print usage information and exit.

---

# Examples

## 1. Basic execution

```bash
java -jar petar.jar \
  --spec rules.petar \
  -i input.json \
  -o output.json \
  -r Expression
```

---

## 2. Multiple specification files

```bash
java -jar petar.jar \
  --spec base.petar transformations.petar \
  -i input.json \
  -o output.json
```

---

## 3. Print specification only (debug mode)

```bash
java -jar petar.jar \
  --spec rules.petar \
  -p
```

---

## 4. Minimal execution (stdout output)

```bash
java -jar petar.jar \
  --spec rules.petar \
  -i input.json
```

---

# Behavior Overview

When executed, PETAR performs the following steps:

1. Loads all specification files (`--spec`) and check it
2. Parses the input tree (`-i`) and check the root type (`-r`)
3. Applies pattern matching rules
4. Generates annotations via production rules
5. Writes results to output (`-o`) or stdout

---

# Notes

- Multiple `--spec` files are merged into a single rule set.
- The order of specification files should not the output.
- If no `-o` is provided, output defaults to standard output.


# Help

Run:

```bash
java -jar petar.jar --help
```

for built-in usage information.