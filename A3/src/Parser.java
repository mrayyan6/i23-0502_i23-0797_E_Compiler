import java.io.*;
import java.util.*;

/**
 * Parser
 * -----------------------------------------------------------------------------
 * Part 7 (shift-reduce engine) + Part 8 (parse-tree generation).
 *
 * This class is DELIBERATELY GENERIC: it takes a Grammar, an ACTION table and
 * a GOTO table, and runs the standard LR driver.  The exact same class is
 * used for both SLR(1) and LR(1) -- only the tables differ.
 *
 * Stack representation:
 *   Following the dragon book, the parsing stack holds alternating symbols
 *   and state numbers (s0 X1 s1 X2 s2 ... Xm sm).  For display we keep two
 *   parallel stacks (symbolStack, stateStack) which makes reductions easier
 *   to reason about without changing the semantics.
 *
 * Parse-tree construction is woven into the reduce step:
 *   When we reduce A -> X1 ... Xn, the TOP n nodes on `treeStack` are popped
 *   in order, attached as children of a new TreeNode(A), and the new node is
 *   pushed back.  After accept, treeStack holds exactly one node: the root.
 * -----------------------------------------------------------------------------
 */
public class Parser {

    // ---- Input ----
    private final Grammar grammar;
    private final Map<Integer, Map<String, String>>  actionTable;
    private final Map<Integer, Map<String, Integer>> gotoTable;
    private final String parserName;   // "SLR(1)" or "LR(1)" for trace headers

    // ---- Output of the last parse() call ----
    private boolean       accepted     = false;
    private String        errorMessage = null;
    private TreeNode      parseTree    = null;
    private List<String[]> traceRows   = new ArrayList<>();  // {step, stack, input, action}
    private List<Integer> reductionsApplied = new ArrayList<>();

    public Parser(Grammar grammar,
                  Map<Integer, Map<String, String>>  actionTable,
                  Map<Integer, Map<String, Integer>> gotoTable,
                  String parserName) {
        this.grammar     = grammar;
        this.actionTable = actionTable;
        this.gotoTable   = gotoTable;
        this.parserName  = parserName;
    }

    // =================================================================
    //                      Main driver loop
    // =================================================================
    public boolean parse(String inputString) {
        // Reset state from any prior parse
        accepted          = false;
        errorMessage      = null;
        parseTree         = null;
        traceRows         = new ArrayList<>();
        reductionsApplied = new ArrayList<>();

        // Tokenise: split on whitespace; if empty, the only token is $.
        List<String> tokens = new ArrayList<>();
        if (inputString != null && !inputString.trim().isEmpty()) {
            for (String tok : inputString.trim().split("\\s+")) {
                if (!tok.isEmpty()) tokens.add(tok);
            }
        }
        tokens.add("$");

        // Parallel stacks: states, display symbols, and parse-tree nodes.
        Deque<Integer>  stateStack  = new ArrayDeque<>();
        Deque<String>   symbolStack = new ArrayDeque<>();
        Deque<TreeNode> treeStack   = new ArrayDeque<>();
        stateStack.push(0);

        int ip   = 0;    // index into `tokens`
        int step = 1;

        while (true) {
            int    state   = stateStack.peek();
            String current = tokens.get(ip);
            String action  = lookupAction(state, current);

            // Record this step's snapshot BEFORE mutating the stacks.
            String stackDisplay = renderStack(stateStack, symbolStack);
            String inputDisplay = renderInput(tokens, ip);

            // ---- ERROR ---------------------------------------------------
            if (action == null || action.isEmpty()) {
                errorMessage = "No action for state " + state + " on symbol '" + current + "'";
                traceRows.add(new String[]{ String.valueOf(step), stackDisplay, inputDisplay,
                        "ERROR: " + errorMessage });
                return false;
            }

            // ---- ACCEPT --------------------------------------------------
            if (action.equals("accept")) {
                traceRows.add(new String[]{ String.valueOf(step), stackDisplay, inputDisplay, "accept" });
                accepted  = true;
                // The single remaining tree node is the parse tree root.
                parseTree = treeStack.isEmpty() ? null : treeStack.peek();
                return true;
            }

            // ---- SHIFT ---------------------------------------------------
            // A pure shift cell is "shift N".  In a conflicted cell such as
            // "shift 6 / reduce 5" we prefer shift (classic shift/reduce
            // resolution) so parsing can still make forward progress.
            if (action.startsWith("shift")) {
                int target = parseShiftTarget(action);
                if (target < 0) {
                    errorMessage = "Malformed shift action: " + action;
                    traceRows.add(new String[]{ String.valueOf(step), stackDisplay, inputDisplay,
                            "ERROR: " + errorMessage });
                    return false;
                }
                traceRows.add(new String[]{ String.valueOf(step), stackDisplay, inputDisplay,
                        "shift " + target
                                + (action.contains("/") ? "  [conflict: " + action + "]" : "") });

                symbolStack.push(current);
                stateStack.push(target);
                treeStack.push(new TreeNode(current, true));   // leaf for the shifted terminal
                ip++;
                step++;
                continue;
            }

            // ---- REDUCE --------------------------------------------------
            if (action.startsWith("reduce")) {
                // The action string may be "reduce 4" or, in a conflicted cell,
                // "shift 7 / reduce 4".  We take the first reduce we can find.
                int prodIndex = parseReduceIndex(action);
                if (prodIndex < 0) {
                    errorMessage = "Malformed reduce action: " + action;
                    traceRows.add(new String[]{ String.valueOf(step), stackDisplay, inputDisplay,
                            "ERROR: " + errorMessage });
                    return false;
                }

                String[] production = grammar.getProductions().get(prodIndex);
                String   lhs        = production[0];
                // RHS length excludes the LHS at index 0; epsilon (production[1]="ε")
                // counts as a 0-length RHS for popping purposes.
                int rhsLength = production.length - 1;
                boolean isEpsilon = rhsLength == 1 && production[1].equals(Grammar.EPSILON);
                if (isEpsilon) rhsLength = 0;

                // Pop rhsLength symbols/states and collect their tree nodes
                // in ORIGINAL left-to-right order to become the new node's children.
                TreeNode[] childrenReversed = new TreeNode[rhsLength];
                for (int k = 0; k < rhsLength; k++) {
                    stateStack.pop();
                    symbolStack.pop();
                    childrenReversed[k] = treeStack.pop();
                }
                TreeNode newNode = new TreeNode(lhs, false);
                for (int k = rhsLength - 1; k >= 0; k--) newNode.addChild(childrenReversed[k]);
                // Epsilon reduction: attach an explicit "ε" leaf so the tree
                // visibly records the empty production.
                if (isEpsilon) newNode.addChild(new TreeNode(Grammar.EPSILON, true));

                // GOTO on the exposed state with the LHS.
                int exposedState = stateStack.peek();
                Integer gotoTarget = gotoTable.getOrDefault(exposedState, Collections.emptyMap()).get(lhs);
                if (gotoTarget == null) {
                    errorMessage = "No GOTO for state " + exposedState + " on non-terminal '" + lhs + "'";
                    traceRows.add(new String[]{ String.valueOf(step), stackDisplay, inputDisplay,
                            "ERROR: " + errorMessage });
                    return false;
                }

                symbolStack.push(lhs);
                stateStack.push(gotoTarget);
                treeStack.push(newNode);
                reductionsApplied.add(prodIndex);

                traceRows.add(new String[]{ String.valueOf(step), stackDisplay, inputDisplay,
                        "reduce " + prodIndex + "  (" + productionToString(production) + ")" });
                step++;
                continue;
            }

            // ---- Unrecognised action (conflict marker, etc.) -------------
            errorMessage = "Unrecognised action '" + action + "' at state " + state
                    + " on '" + current + "'";
            traceRows.add(new String[]{ String.valueOf(step), stackDisplay, inputDisplay,
                    "ERROR: " + errorMessage });
            return false;
        }
    }

    // =================================================================
    //                      Helpers
    // =================================================================
    private String lookupAction(int state, String symbol) {
        Map<String, String> row = actionTable.get(state);
        return row == null ? null : row.get(symbol);
    }

    /** If the cell holds a conflict like "shift 7 / reduce 4", pull out the
     *  reduce production index; otherwise just parse "reduce N". Returns -1
     *  if no reduce is found. */
    private int parseReduceIndex(String action) {
        for (String part : action.split("/")) {
            part = part.trim();
            if (part.startsWith("reduce")) {
                try { return Integer.parseInt(part.substring("reduce".length()).trim()); }
                catch (NumberFormatException ignored) { }
            }
        }
        return -1;
    }

    /** Mirror of parseReduceIndex: extracts the shift target from a cell that
     *  may be pure ("shift 6") or conflicted ("shift 6 / reduce 5"). */
    private int parseShiftTarget(String action) {
        for (String part : action.split("/")) {
            part = part.trim();
            if (part.startsWith("shift")) {
                try { return Integer.parseInt(part.substring("shift".length()).trim()); }
                catch (NumberFormatException ignored) { }
            }
        }
        return -1;
    }

    private String renderStack(Deque<Integer> states, Deque<String> symbols) {
        // Dragon-book order: bottom ... top, alternating "state symbol state symbol ... state"
        List<Integer> st = new ArrayList<>(states); Collections.reverse(st);
        List<String>  sy = new ArrayList<>(symbols); Collections.reverse(sy);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < st.size(); i++) {
            if (i > 0) sb.append(' ').append(sy.get(i - 1)).append(' ');
            sb.append(st.get(i));
        }
        return sb.toString();
    }

    private String renderInput(List<String> tokens, int ip) {
        StringBuilder sb = new StringBuilder();
        for (int i = ip; i < tokens.size(); i++) {
            if (i > ip) sb.append(' ');
            sb.append(tokens.get(i));
        }
        return sb.toString();
    }

    private String productionToString(String[] p) {
        StringBuilder sb = new StringBuilder(p[0]).append(" ->");
        for (int i = 1; i < p.length; i++) sb.append(' ').append(p[i]);
        return sb.toString();
    }

    // =================================================================
    //                      Accessors + reporting
    // =================================================================
    public boolean        isAccepted()          { return accepted; }
    public String         getErrorMessage()     { return errorMessage; }
    public TreeNode       getParseTree()        { return parseTree; }
    public List<String[]> getTraceRows()        { return traceRows; }
    public List<Integer>  getReductionsApplied() { return reductionsApplied; }

    public void printTrace() {
        System.out.println("\n===== " + parserName + " Parsing Trace =====");
        System.out.printf("%-6s %-45s %-30s %s%n", "Step", "Stack", "Input", "Action");
        System.out.println("-".repeat(110));
        for (String[] r : traceRows) {
            System.out.printf("%-6s %-45s %-30s %s%n", r[0], r[1], r[2], r[3]);
        }
        System.out.println(accepted ? "Result: ACCEPTED" : "Result: REJECTED - " + errorMessage);
    }

    public void saveTraceToFile(String path, String inputString) throws IOException {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(path))) {
            w.write(parserName + " Parsing Trace");               w.newLine();
            w.write("Input: " + (inputString == null ? "" : inputString)); w.newLine();
            w.write("=".repeat(110));                             w.newLine();
            w.write(String.format("%-6s %-45s %-30s %s", "Step", "Stack", "Input", "Action"));
            w.newLine();
            w.write("-".repeat(110));                             w.newLine();
            for (String[] r : traceRows) {
                w.write(String.format("%-6s %-45s %-30s %s", r[0], r[1], r[2], r[3]));
                w.newLine();
            }
            w.newLine();
            w.write(accepted ? "Result: ACCEPTED" : "Result: REJECTED - " + errorMessage);
            w.newLine();
            if (accepted && parseTree != null) {
                w.write("=".repeat(110));        w.newLine();
                w.write("Parse Tree:");          w.newLine();
                w.write(parseTree.render());
            }
        }
    }
}