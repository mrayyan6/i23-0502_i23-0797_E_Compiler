# Assignment 3 — SLR(1) and LR(1) Bottom-Up Parsers

## Team Members

| Name           | Roll Number |
|----------------|-------------|
| Rayyan Masroor | 23i-0502    |
| Hasan Naveed   | 23i-0797    |

---

## Programming Language

**Java** 

---

## Project Structure

```
A3/
├── src/               # All source files
│   ├── Grammar.java
│   ├── FirstFollow.java
│   ├── Item.java
│   ├── State.java
│   ├── SLRBuilder.java
│   ├── Item1.java
│   ├── State1.java
│   ├── LR1CollectionBuilder.java
│   ├── LR1Builder.java
│   ├── Parser.java
│   ├── TreeNode.java
│   ├── Main.java          # Full SLR(1) + LR(1) pipeline
│   └── Main1.java         # Automated comparison tool
├── input/
│   ├── grammar.txt         # Expression grammar (default)
│   ├── strings.txt         # Test strings for expression grammar
│   ├── grammar_conflict.txt  # Conflict grammar (S -> L=R | R)
│   └── test_strings.txt    # Test strings for conflict grammar
├── out/               # Compiled .class files (generated)
└── output/            # All output files (generated)
    ├── augmented_grammar.txt
    ├── comparison.txt
    ├── parse_trees.txt
    ├── comparison_log.txt
    ├── slr/
    │   ├── lr0_states.txt
    │   ├── slr_table.txt
    │   └── slr_trace.txt
    └── lr1/
        ├── lr1_states.txt
        ├── lr1_table.txt
        └── lr1_trace.txt
```

---

## Compilation Instructions

All commands must be run from inside the `A3/` directory.

**Step 1 — Create the output directory for compiled classes:**
```bash
mkdir out
```

**Step 2 — Compile all Java source files:**
```bash
javac src/*.java -d out
```

No external libraries are required. The standard JDK is sufficient.

---

## Execution Instructions

### Run the full SLR(1) + LR(1) pipeline (Main)

Reads `input/grammar.txt` and `input/strings.txt`. Produces the augmented grammar, LR(0)/LR(1) item sets, both parsing tables, parse traces, parse trees, and a side-by-side comparison report under `output/`.

```bash
java -cp out Main
```

**Expected console output:**
```
======================================================
 SLR(1) + LR(1) Parser
 Grammar : input/grammar.txt
 Strings : input/strings.txt
======================================================

SLR(1): 12 states, 0 conflicts, 7/7 accepted.
LR(1):  24 states, 0 conflicts, 7/7 accepted.
Outputs written to: output/
```

---

### Run the Automated Comparison Tool (Main1)

Reads `input/grammar_conflict.txt` and `input/test_strings.txt`. Instantiates both parsers, runs every test string through each, and prints a formatted ASCII comparison table. Also writes the log to `output/comparison_log.txt`.

```bash
java -cp out Main1
```

**Expected console output:**
```
SLR(1) vs LR(1) Automated Comparison
Grammar : input/grammar_conflict.txt
Strings : input/test_strings.txt

+-----------------------------------+------------+------------+----------+
| Test String                       | SLR Result | LR1 Result | Match?   |
+-----------------------------------+------------+------------+----------+
| id                                | ACCEPTED   | ACCEPTED   | YES      |
| id = id                           | ACCEPTED   | ACCEPTED   | YES      |
...
+-----------------------------------+------------+------------+----------+
  Total: 8  |  Matched: 8  |  Mismatched: 0  |  SLR conflicts: 1  |  LR1 conflicts: 0

SLR(1) Table Conflicts:
  SHIFT-REDUCE conflict in state 2 on symbol '=': ...
```

---

## Input File Format Specification

### Test Strings File (`input/strings.txt`, `input/test_strings.txt`)

- One input string per line.
- Tokens within each string must be **separated by spaces**.
- Lines beginning with `#` are treated as comments and ignored.
- Blank lines are included as empty-string test cases.

**Example:**
```
# Valid expressions
id + id * id
( id + id ) * id
# Invalid expression
id + * id
```

---

## Grammar File Format

- One production rule per line.
- The left-hand side (non-terminal) is separated from alternatives by `->`.
- Multiple right-hand side alternatives are separated by `|`.
- Terminals and non-terminals within each alternative are **separated by spaces**.
- The **first non-terminal** that appears on a left-hand side is taken as the start symbol.
- Use the Unicode character `ε` (or the literal string `ε`) to denote an epsilon (empty) production.

**Example — Expression Grammar (`input/grammar.txt`):**
```
E -> E + T | T
T -> T * F | F
F -> ( E ) | id
```

**Example — Conflict Grammar (`input/grammar_conflict.txt`):**
```
S -> L = R | R
L -> * R | id
R -> L
```

---

## Sample Commands

### SLR(1) Parser

```bash
# From the A3/ directory:
javac src/*.java -d out
java -cp out Main
# Parsing table written to: output/slr/slr_table.txt
# Parse trace written to:   output/slr/slr_trace.txt
```

### LR(1) Parser

```bash
# From the A3/ directory:
javac src/*.java -d out
java -cp out Main
# Parsing table written to: output/lr1/lr1_table.txt
# Parse trace written to:   output/lr1/lr1_trace.txt
```

Both parsers are invoked together by `Main`. Their tables, item sets, and traces are written to separate sub-directories under `output/`.

### Automated Comparison Tool

```bash
java -cp out Main1
# Comparison log written to: output/comparison_log.txt
```

---

## Known Limitations

1. **Conflict resolution — shift preference.** When a SHIFT-REDUCE conflict exists in a cell, the parser silently prefers the shift action. This mimics the convention used by many tools (e.g., yacc/bison) but means a grammar with conflicts may still appear to accept all strings correctly at runtime rather than failing hard.

2. **No LALR(1) support.** The LR(1) implementation builds the full canonical LR(1) collection. States with the same LR(0) core but different lookaheads are never merged, so the state count is higher than an equivalent LALR(1) parser would produce.

3. **Single grammar file at a time.** `Main` has its grammar and strings paths hard-coded to `input/grammar.txt` and `input/strings.txt`. To test a different grammar, either edit those constants in `Main.java` or use `Main1.java` (which targets `input/grammar_conflict.txt`).

4. **Epsilon productions.** Epsilon (`ε`) is supported in grammar rules, but the grammar must use the exact Unicode character `ε` — the ASCII string `eps` or `epsilon` is not recognised.

5. **Left-recursive grammars.** The parsers handle left-recursive grammars correctly (they are LR-based), but the provided expression grammar is already left-recursive by design. Right-recursive alternatives would produce different (right-associative) parse trees.
