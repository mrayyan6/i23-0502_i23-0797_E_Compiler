# LL(1) Predictive Parser
## CS4031 - Compiler Construction - Assignment 02

### Team Members
I23-0502 - Rayyan
I23-0797 - Hassan
Section: E

### Programming Language
Java (JDK 17+)

### Compilation Instructions
```bash
cd A2/src
javac -d ../classes *.java
```

All compiled `.class` files are redirected to `A2/classes`.

### Execution Instructions
```bash
cd A2

# Run with specific grammar and input files:
java -cp ../classes Main ../input/grammar1.txt ../input/input_valid.txt

# Example:
java -cp ../classes Main ../input/grammar2.txt ../input/input_errors.txt

# Run with defaults (input/grammar1.txt and input/input_valid.txt):
java -cp ../classes Main
```

Optional clean build (PowerShell):
```powershell
Remove-Item .\classes\*.class -Force -ErrorAction SilentlyContinue
cd .\src
javac -d ..\classes *.java
cd ..
```

### Input File Formats

#### Grammar File Format
- One production per line
- Format: `NonTerminal -> production1 | production2 | ...`
- Use `->` as arrow symbol
- Use `|` to separate alternatives
- Terminals: lowercase letters, operators, keywords
- Non-terminals: Multi-character names starting with uppercase (e.g., `Expr`, `Term`, `Factor`)
- Epsilon: use `epsilon` or `@`

Example (`grammar1.txt`):
```
Expr -> Expr + Term | Term
Term -> Term * Factor | Factor
Factor -> ( Expr ) | id
```

#### Input String File Format
- One input string per line
- Tokens separated by spaces
- Only terminals from the grammar
- `$` end marker is appended automatically (do not include it)

Example (`input_valid.txt`):
```
id + id * id
( id + id ) * id
id * id + id
```

### Sample Grammar and Input Files

| File | Description |
|------|-------------|
| `input/grammar1.txt` | Expression grammar with left recursion (`Expr`, `Term`, `Factor`) |
| `input/grammar2.txt` | Simple grammar with epsilon production (`Start`, `First`, `Second`) |
| `input/grammar3.txt` | Grammar with indirect left recursion (`Start`, `Alpha`) |
| `input/input_valid.txt` | 5 valid expression strings |
| `input/input_errors.txt` | 5 invalid strings for error recovery testing |
| `input/input_edge_cases.txt` | Edge cases: single id, nested parens, chained operations |

### Features Implemented
1. **Grammar Transformation**: Left factoring and left recursion removal (direct + indirect)
2. **FIRST/FOLLOW Sets**: Complete computation with epsilon handling
3. **LL(1) Parsing Table**: Construction with conflict detection
4. **Stack-Based Parser**: Full LL(1) predictive parsing with step-by-step trace
5. **Error Handling**: Panic mode recovery with synchronizing tokens from FOLLOW sets
6. **Parse Tree Generation**: ASCII art tree display, preorder/postorder traversal, DOT format

### Error Recovery Strategy
**Panic Mode Recovery** is implemented:
- When an empty table entry `M[X, a]` is encountered:
  - If `a` is in `FOLLOW(X)`: pop `X` from stack (treat as epsilon)
  - Otherwise: skip input tokens until a synchronizing token (from `FOLLOW(X)`) is found, then pop `X`
- When a terminal mismatch occurs: insert the missing terminal (pop from stack without advancing input)
- Parsing continues after recovery to detect multiple errors

### Output Files
| File | Contents |
|------|----------|
| `output/grammar_transformed.txt` | Grammar after left factoring + left recursion removal |
| `output/first_follow_sets.txt` | FIRST and FOLLOW sets for all non-terminals |
| `output/parsing_table.txt` | Complete LL(1) parsing table |
| `output/parsing_trace1.txt` | Step-by-step parsing traces |
| `output/parsing_trace2.txt` | Parsing traces for error inputs |
| `output/parse_trees.txt` | Parse trees for parsed strings |

### Known Limitations
- Statement grammar with `if-then-else` ambiguity (dangling else) produces a non-LL(1) conflict, which is detected and reported
- Very long input strings may produce wide trace table columns
