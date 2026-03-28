import java.io.*;
import java.util.*;

/**
 * Parser.java
 * LL(1) Stack-Based Predictive Parser.
 * Reads input strings, parses them using the LL(1) table, builds parse trees,
 * and handles errors with panic mode recovery.
 */
public class Parser {

    private Grammar grammar;
    private ErrorHandler errorHandler;

    public Parser(Grammar grammar) {
        this.grammar = grammar;
        this.errorHandler = new ErrorHandler(grammar);
    }

    // -----------------------------------------------------------------------
    // Read input strings from file
    // -----------------------------------------------------------------------
    public List<String> readInputFile(String filename) {
        List<String> inputs = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("//") && !line.startsWith("#")) {
                    inputs.add(line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading input file: " + e.getMessage());
        }
        return inputs;
    }

    // -----------------------------------------------------------------------
    // Tokenize an input string
    // -----------------------------------------------------------------------
    private List<String> tokenize(String input) {
        List<String> tokens = new ArrayList<>();
        // Remove trailing $ if user included it
        input = input.trim();
        if (input.endsWith("$")) {
            input = input.substring(0, input.length() - 1).trim();
        }
        String[] parts = input.split("\\s+");
        for (String p : parts) {
            if (!p.isEmpty()) tokens.add(p);
        }
        tokens.add("$"); // Append end marker
        return tokens;
    }

    // -----------------------------------------------------------------------
    // Parse a single input string
    // -----------------------------------------------------------------------
    public ParseResult parse(String inputStr, int lineNumber) {
        errorHandler.clearErrors();

        List<String> input = tokenize(inputStr);
        Stack stack = new Stack();
        StringBuilder traceLog = new StringBuilder();
        List<String[]> traceTable = new ArrayList<>(); // [step, stack, input, action]

        // Initialize stack: $ then start symbol
        stack.push("$");
        stack.push(grammar.startSymbol);

        // Create parse tree root
        Tree tree = new Tree();
        Tree.TreeNode root = new Tree.TreeNode(grammar.startSymbol);
        tree.setRoot(root);

        // Parallel stack of tree nodes (LinkedList allows null, ArrayDeque does not)
        LinkedList<Tree.TreeNode> nodeStack = new LinkedList<>();
        nodeStack.push(null); // corresponds to $
        nodeStack.push(root); // corresponds to start symbol

        int ptr = 0; // Input pointer
        int step = 1;
        boolean accepted = false;
        boolean hasErrors = false;

        // Header
        traceLog.append(String.format("%-6s %-35s %-25s %s%n", "Step", "Stack", "Input", "Action"));
        traceLog.append("-".repeat(100)).append("\n");

        while (true) {
            String X = stack.top();       // Top of stack
            String a = input.get(ptr);    // Current input symbol

            String stackStr = stack.contents();
            String inputStr2 = buildInputString(input, ptr);

            // Case 1: X = $ and a = $ => Accept
            if (X.equals("$") && a.equals("$")) {
                String action = "ACCEPT";
                traceTable.add(new String[]{String.valueOf(step), stackStr, inputStr2, action});
                traceLog.append(String.format("%-6d %-35s %-25s %s%n", step, stackStr, inputStr2, action));
                accepted = true;
                break;
            }

            // Case 2: X = a (terminal match)
            if (!grammar.isNonTerminal(X) && !X.equals("$")) {
                if (X.equals(a)) {
                    String action = "Match '" + a + "'";
                    traceTable.add(new String[]{String.valueOf(step), stackStr, inputStr2, action});
                    traceLog.append(String.format("%-6d %-35s %-25s %s%n", step, stackStr, inputStr2, action));
                    stack.pop();
                    nodeStack.pop(); // Pop corresponding tree node (already a leaf)
                    ptr++;
                    step++;
                    continue;
                } else {
                    // Case 4: Terminal mismatch
                    String action = "ERROR: Expected '" + X + "', found '" + a + "'";
                    traceTable.add(new String[]{String.valueOf(step), stackStr, inputStr2, action});
                    traceLog.append(String.format("%-6d %-35s %-25s %s%n", step, stackStr, inputStr2, action));

                    errorHandler.reportTerminalMismatch(lineNumber, ptr + 1, X, a);
                    hasErrors = true;

                    // Recovery: pop the expected terminal (insert missing symbol)
                    errorHandler.recoverTerminalMismatch(stack, traceLog);
                    nodeStack.pop();
                    step++;
                    continue;
                }
            }

            // Case 3: X is non-terminal
            if (grammar.isNonTerminal(X)) {
                Map<String, Integer> row = grammar.parsingTable.get(X);
                int prodIdx = -1;
                if (row != null && row.containsKey(a)) {
                    prodIdx = row.get(a);
                }

                if (prodIdx >= 0) {
                    Production prod = grammar.productions.get(prodIdx);
                    String action = prod.toString();
                    traceTable.add(new String[]{String.valueOf(step), stackStr, inputStr2, action});
                    traceLog.append(String.format("%-6d %-35s %-25s %s%n", step, stackStr, inputStr2, action));

                    stack.pop();
                    Tree.TreeNode parentNode = nodeStack.pop();

                    // Push RHS in reverse order (unless epsilon)
                    if (!(prod.rhs.size() == 1 && grammar.isEpsilon(prod.rhs.get(0)))) {
                        // Create child tree nodes
                        List<Tree.TreeNode> childNodes = new ArrayList<>();
                        for (String sym : prod.rhs) {
                            Tree.TreeNode child = new Tree.TreeNode(sym);
                            childNodes.add(child);
                            if (parentNode != null) {
                                parentNode.addChild(child);
                            }
                        }
                        // Push in reverse order
                        for (int i = prod.rhs.size() - 1; i >= 0; i--) {
                            stack.push(prod.rhs.get(i));
                            nodeStack.push(childNodes.get(i));
                        }
                    } else {
                        // Epsilon production - add epsilon leaf to tree
                        if (parentNode != null) {
                            Tree.TreeNode epsNode = new Tree.TreeNode("epsilon");
                            parentNode.addChild(epsNode);
                        }
                    }
                    step++;
                    continue;
                } else {
                    // Empty table entry - ERROR
                    String action = "ERROR: No entry M[" + X + ", " + a + "]";
                    traceTable.add(new String[]{String.valueOf(step), stackStr, inputStr2, action});
                    traceLog.append(String.format("%-6d %-35s %-25s %s%n", step, stackStr, inputStr2, action));

                    errorHandler.reportEmptyTableEntry(lineNumber, ptr + 1, X, a);
                    hasErrors = true;

                    // Panic mode recovery
                    int skipped = errorHandler.panicModeRecovery(X, input, ptr, stack, traceLog);
                    if (!nodeStack.isEmpty()) nodeStack.pop(); // Pop corresponding tree node
                    ptr += skipped;
                    step++;

                    // Safety check: prevent infinite loop
                    if (stack.isEmpty() || (stack.top().equals("$") && !input.get(ptr).equals("$"))) {
                        // Skip remaining input
                        while (ptr < input.size() && !input.get(ptr).equals("$")) {
                            traceLog.append("  Recovery: Skipping remaining '")
                                     .append(input.get(ptr)).append("'\n");
                            ptr++;
                        }
                        break;
                    }
                    continue;
                }
            }

            // Case: X = $ but a != $ => premature end / extra input
            if (X.equals("$") && !a.equals("$")) {
                String action = "ERROR: Extra input '" + a + "'";
                traceTable.add(new String[]{String.valueOf(step), stackStr, inputStr2, action});
                traceLog.append(String.format("%-6d %-35s %-25s %s%n", step, stackStr, inputStr2, action));

                errorHandler.reportPrematureEnd(lineNumber, ptr + 1, X);
                hasErrors = true;
                break;
            }

            // Safety: should not reach here
            break;
        }

        // Build result
        ParseResult result = new ParseResult();
        result.inputString = inputStr;
        result.accepted = accepted && !hasErrors;
        result.traceLog = traceLog.toString();
        result.traceTable = traceTable;
        result.parseTree = accepted ? tree : null;
        result.errors = new ArrayList<>(errorHandler.getErrors());
        result.errorCount = errorHandler.getErrorCount();

        return result;
    }

    /**
     * Build remaining input string from pointer position.
     */
    private String buildInputString(List<String> input, int ptr) {
        StringBuilder sb = new StringBuilder();
        for (int i = ptr; i < input.size(); i++) {
            if (i > ptr) sb.append(" ");
            sb.append(input.get(i));
        }
        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // Parse all strings from input file
    // -----------------------------------------------------------------------
    public List<ParseResult> parseFile(String filename) {
        List<String> inputs = readInputFile(filename);
        List<ParseResult> results = new ArrayList<>();

        for (int i = 0; i < inputs.size(); i++) {
            System.out.println("\n" + "=".repeat(80));
            System.out.println("Parsing input string #" + (i + 1) + ": " + inputs.get(i));
            System.out.println("=".repeat(80));

            ParseResult result = parse(inputs.get(i), i + 1);
            results.add(result);

            // Print trace
            System.out.println(result.traceLog);

            // Print result
            if (result.accepted) {
                System.out.println("Result: String ACCEPTED successfully!");
                System.out.println("\nParse Tree:");
                result.parseTree.printIndented();
            } else {
                System.out.println("Result: Parsing completed with " +
                                   result.errorCount + " error(s).");
                for (ErrorHandler.ParseError err : result.errors) {
                    System.out.println(err);
                }
            }
        }

        return results;
    }

    // -----------------------------------------------------------------------
    // Save parsing traces to file
    // -----------------------------------------------------------------------
    public void saveTraces(List<ParseResult> results, String filename) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            for (int i = 0; i < results.size(); i++) {
                ParseResult r = results.get(i);
                pw.println("=".repeat(80));
                pw.println("Input #" + (i + 1) + ": " + r.inputString);
                pw.println("=".repeat(80));
                pw.println(r.traceLog);
                if (r.accepted) {
                    pw.println("Result: ACCEPTED");
                } else {
                    pw.println("Result: REJECTED with " + r.errorCount + " error(s)");
                    for (ErrorHandler.ParseError err : r.errors) {
                        pw.println(err);
                    }
                }
                pw.println();
            }
        } catch (IOException e) {
            System.err.println("Error saving traces: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // Save parse trees to file
    // -----------------------------------------------------------------------
    public void saveParseTrees(List<ParseResult> results, String filename) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            for (int i = 0; i < results.size(); i++) {
                ParseResult r = results.get(i);
                pw.println("=".repeat(60));
                pw.println("Parse Tree for input #" + (i + 1) + ": " + r.inputString);
                pw.println("=".repeat(60));
                if (r.accepted && r.parseTree != null) {
                    r.parseTree.saveToFile(pw, "");
                } else {
                    pw.println("  (No parse tree - string was rejected)");
                    if (!r.errors.isEmpty()) {
                        pw.println("  Errors:");
                        for (ErrorHandler.ParseError err : r.errors) {
                            pw.println("    " + err);
                        }
                    }
                }
                pw.println();
            }
        } catch (IOException e) {
            System.err.println("Error saving parse trees: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // ParseResult inner class
    // -----------------------------------------------------------------------
    public static class ParseResult {
        public String inputString;
        public boolean accepted;
        public String traceLog;
        public List<String[]> traceTable;
        public Tree parseTree;
        public List<ErrorHandler.ParseError> errors;
        public int errorCount;
    }
}
