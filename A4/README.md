# JSON to XML Translator (Java)
## CS-4031 Compiler Construction — Assignment 04

---

## Build Instructions

**Requirement:** JDK 11 or later (`javac`, `java` on PATH)

```bash
# Compile all sources → bin/  and create ./json2xml wrapper script
make

# Or compile manually without make:
mkdir -p bin
javac -d bin src/json2xml/*.java
```

---

## Run Instructions

```bash
# Read from stdin, write XML to stdout
./json2xml < input.json

# Optional: print the AST to stderr for debugging
./json2xml --ast < input.json

# Redirect XML output to a file
./json2xml < input.json > output.xml

# Run all tests
make test

# Quick demo
make run

# Build a self-contained JAR
make jar
java -jar json2xml.jar < input.json
```

---

## Architecture / Compiler Pipeline

```
stdin (JSON text)
      │
      ▼
  ┌──────────┐
  │  Lexer   │  scanner equivalent (Lexer.java)
  │          │  Reads characters → produces List<Token>
  └──────────┘
      │  List<Token>
      ▼
  ┌──────────┐
  │  Parser  │  parser equivalent (Parser.java)
  │          │  Recursive-descent, builds AST
  └──────────┘
      │  ASTNode (root)
      ▼
  ┌──────────────┐
  │ XMLGenerator │  AST traversal → well-formed XML
  └──────────────┘
      │
      ▼
  stdout (XML text)
```

---

## AST Node Types

Defined in `ASTNode.java` as a sealed class hierarchy:

| Class         | JSON Source         | Key Fields                        |
|---------------|---------------------|-----------------------------------|
| `ObjectNode`  | `{ "k": v, … }`     | `List<KVPair> pairs`              |
| `ArrayNode`   | `[ v, v, … ]`       | `List<ASTNode> items`             |
| `StringNode`  | `"hello"`           | `String value`                    |
| `NumberNode`  | `42`, `3.14`, `-1e5`| `String value` (stored verbatim)  |
| `BoolNode`    | `true` / `false`    | `boolean value`                   |
| `NullNode`    | `null`              | (no payload)                      |

`KVPair` is a simple inner record inside `ObjectNode` holding `String key` + `ASTNode value`.

### Example AST for `{"name":"Ali","age":19}`

```
OBJECT {
  KEY "name"
    STRING "Ali"
  KEY "age"
    NUMBER 19
}
```

---

## XML Conversion Rules

| JSON                  | XML                                   |
|-----------------------|---------------------------------------|
| Top-level value       | Wrapped in `<root>…</root>`           |
| Object `{ "k": v }`  | `<k>…</k>` child elements             |
| Array `[a, b]`        | `<item>a</item><item>b</item>`         |
| String / Number / Bool| Text content inside the tag           |
| `null`                | Self-closing `<tag/>`                 |
| `&` `<` `>` `"` `'`  | Escaped as XML entities               |

---

## Bonus Features (+10 marks)

| Feature                   | Status | Details                                       |
|---------------------------|--------|-----------------------------------------------|
| Pretty-printed XML        | ✅     | 2-space indentation (on by default)           |
| AST printing              | ✅     | `./json2xml --ast` dumps AST to stderr        |
| Column-based error detail | ✅     | All errors include line **and** column number |
| Unicode escape support    | ✅     | `\uXXXX` decoded to UTF-8 in `Lexer.java`     |
| Scientific notation       | ✅     | `-1.5e+10`, `3E-2` etc. parsed correctly      |

---

## Source File Reference

```
src/json2xml/
├── TokenType.java     — Token type enum
├── Token.java         — Token (type, value, line, col)
├── Lexer.java         — Lexical analyser (scanner.l equivalent)
├── ASTNode.java       — AST node class hierarchy
├── Parser.java        — Recursive-descent parser (parser.y equivalent)
├── XMLGenerator.java  — AST → XML emitter
├── ASTPrinter.java    — Bonus: AST debug printer
└── Main.java          — Entry point
```

---

## Test Files

| File         | Tests                                          |
|--------------|------------------------------------------------|
| `test1.json` | Simple flat object (id, name, age)             |
| `test2.json` | Object with array of strings (genres)          |
| `test3.json` | Nested object + boolean value                  |
| `test4.json` | Null value → self-closing tag                  |
| `test5.json` | Complex: nested object + array + bool + null   |

---

## Assumptions & Limitations

- JSON keys are assumed to be valid XML tag names (per assignment spec). No key sanitisation is performed.
- The top-level JSON value should be an object for the standard `<root>` output format. Bare arrays and scalars are wrapped in `<root>` as a fallback.
- Numbers are stored and emitted verbatim (no normalisation).
- The translator stops at the **first** lexical or syntax error and prints one clear message.
- Very deeply nested JSON structures may hit Java's default stack size. Use `-Xss` JVM flag if needed.
