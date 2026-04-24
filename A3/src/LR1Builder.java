import java.io.*;
import java.util.*;

/**
 * LR1Builder
 * -----------------------------------------------------------------------------
 * Part 6 of the assignment: constructs the LR(1) parsing table from the
 * Canonical Collection of LR(1) items produced by LR1CollectionBuilder.
 *
 * Key difference vs. SLRBuilder:
 *   - SLR(1) places reduce actions under EVERY terminal in FOLLOW(A).
 *   - LR(1)  places reduce actions ONLY under the specific lookahead carried
 *            inside the item [A -> alpha ., a].  This extra precision is what
 *            lets LR(1) resolve conflicts that SLR(1) cannot.
 *
 * Produces:
 *   - actionTable : state  -> (terminal    -> "shift k" | "reduce p" | "accept")
 *   - gotoTable   : state  -> (nonTerminal -> state)
 *   - conflicts   : list of human-readable conflict reports
 * -----------------------------------------------------------------------------
 */
public class LR1Builder {

    private final Grammar grammar;
    private final LR1CollectionBuilder collectionBuilder;
    private final List<State1> canonicalCollection;

    private final Map<Integer, Map<String, String>>  actionTable = new LinkedHashMap<>();
    private final Map<Integer, Map<String, Integer>> gotoTable   = new LinkedHashMap<>();
    private final List<String> conflicts = new ArrayList<>();

    public LR1Builder(Grammar grammar, LR1CollectionBuilder collectionBuilder) {
        this.grammar = grammar;
        this.collectionBuilder = collectionBuilder;
        this.canonicalCollection = collectionBuilder.getCanonicalCollection();
        buildParsingTable();
    }

    // -----------------------------------------------------------------
    //  Table construction
    // -----------------------------------------------------------------
    private void buildParsingTable() {
        for (State1 state : canonicalCollection) {
            int stateId = state.getStateId();
            actionTable.put(stateId, new LinkedHashMap<>());
            gotoTable.put(stateId, new LinkedHashMap<>());

            // 1) SHIFT + GOTO actions come from the state's outgoing transitions.
            //    If the symbol is a terminal -> ACTION[stateId, t] = shift target.
            //    If the symbol is a non-terminal -> GOTO[stateId, A] = target.
            for (Map.Entry<String, Integer> transition : state.getTransitions().entrySet()) {
                String symbol       = transition.getKey();
                int    targetState  = transition.getValue();

                if (grammar.getTerminals().contains(symbol)) {
                    setActionEntry(stateId, symbol, "shift " + targetState);
                } else if (grammar.getNonTerminals().contains(symbol)) {
                    gotoTable.get(stateId).put(symbol, targetState);
                }
            }

            // 2) REDUCE / ACCEPT actions come from items whose dot is at the end.
            for (Item1 item : state.getItems()) {
                if (!item.isDotAtEnd()) continue;

                String lhs = item.getLhs();

                // Accept when the item is [S' -> S ., $]
                if (lhs.equals(grammar.getAugmentedStart())
                        && "$".equals(item.getLookahead())) {
                    setActionEntry(stateId, "$", "accept");
                    continue;
                }

                // Reduce on the item's SPECIFIC lookahead only -- this is the
                // defining LR(1) behaviour (SLR would use FOLLOW(lhs) here).
                int prodIndex = grammar.getProductionIndex(item.getProduction());
                setActionEntry(stateId, item.getLookahead(), "reduce " + prodIndex);
            }
        }
    }

    /** Adds an entry to the ACTION table and records a conflict if one arises. */
    private void setActionEntry(int stateId, String symbol, String newAction) {
        Map<String, String> row = actionTable.get(stateId);
        String existing = row.get(symbol);

        if (existing == null) {
            row.put(symbol, newAction);
            return;
        }
        if (existing.equals(newAction)) return;    // same decision, no real conflict

        String kind;
        boolean existingShift = existing.startsWith("shift");
        boolean newShift      = newAction.startsWith("shift");
        if (existingShift != newShift) {
            kind = "SHIFT-REDUCE";
        } else {
            kind = "REDUCE-REDUCE";
        }

        conflicts.add(kind + " conflict in state " + stateId
                + " on symbol '" + symbol + "': '"
                + existing + "' vs '" + newAction + "'");
        row.put(symbol, existing + " / " + newAction);
    }

    // -----------------------------------------------------------------
    //  Accessors
    // -----------------------------------------------------------------
    public Map<Integer, Map<String, String>>  getActionTable() { return Collections.unmodifiableMap(actionTable); }
    public Map<Integer, Map<String, Integer>> getGotoTable()   { return Collections.unmodifiableMap(gotoTable);   }
    public List<String>                       getConflicts()   { return Collections.unmodifiableList(conflicts);  }
    public List<State1>                       getStates()      { return canonicalCollection; }
    public Grammar                            getGrammar()     { return grammar; }

    // -----------------------------------------------------------------
    //  Pretty printing
    // -----------------------------------------------------------------
    public void printParsingTable() {
        Set<String> terminalCols = new LinkedHashSet<>(grammar.getTerminals());
        terminalCols.add("$");
        Set<String> nonTerminalCols = new LinkedHashSet<>(grammar.getNonTerminals());
        nonTerminalCols.remove(grammar.getAugmentedStart());

        System.out.println("\n===== LR(1) Parsing Table =====");
        System.out.println("Total states: " + canonicalCollection.size());
        System.out.println();

        System.out.printf("%-8s", "State");
        for (String t : terminalCols)    System.out.printf("%-20s", t);
        for (String nt : nonTerminalCols) System.out.printf("%-15s", nt);
        System.out.println();
        System.out.println("-".repeat(8 + terminalCols.size() * 20 + nonTerminalCols.size() * 15));

        for (int id = 0; id < canonicalCollection.size(); id++) {
            System.out.printf("%-8d", id);
            Map<String, String>  a = actionTable.getOrDefault(id, Collections.emptyMap());
            Map<String, Integer> g = gotoTable.getOrDefault(id,   Collections.emptyMap());
            for (String t : terminalCols)    System.out.printf("%-20s", a.getOrDefault(t, ""));
            for (String nt : nonTerminalCols) System.out.printf("%-15s", g.containsKey(nt) ? g.get(nt) : "");
            System.out.println();
        }

        if (conflicts.isEmpty()) {
            System.out.println("\nGrammar is LR(1) -- no conflicts detected.");
        } else {
            System.out.println("\n===== LR(1) Conflicts =====");
            for (String c : conflicts) System.out.println(c);
        }
    }

    /** Writes the same table (plus conflicts) to a file -- used by Main2.java. */
    public void saveTableToFile(String path) throws IOException {
        Set<String> terminalCols = new LinkedHashSet<>(grammar.getTerminals());
        terminalCols.add("$");
        Set<String> nonTerminalCols = new LinkedHashSet<>(grammar.getNonTerminals());
        nonTerminalCols.remove(grammar.getAugmentedStart());

        try (BufferedWriter w = new BufferedWriter(new FileWriter(path))) {
            w.write("LR(1) Parsing Table"); w.newLine();
            w.write("Total states: " + canonicalCollection.size()); w.newLine();
            w.write("=".repeat(80));       w.newLine(); w.newLine();

            StringBuilder header = new StringBuilder();
            header.append(String.format("%-8s", "State"));
            for (String t : terminalCols)    header.append(String.format("%-20s", t));
            for (String nt : nonTerminalCols) header.append(String.format("%-15s", nt));
            w.write(header.toString()); w.newLine();
            w.write("-".repeat(8 + terminalCols.size() * 20 + nonTerminalCols.size() * 15)); w.newLine();

            for (int id = 0; id < canonicalCollection.size(); id++) {
                StringBuilder row = new StringBuilder();
                row.append(String.format("%-8d", id));
                Map<String, String>  a = actionTable.getOrDefault(id, Collections.emptyMap());
                Map<String, Integer> g = gotoTable.getOrDefault(id,   Collections.emptyMap());
                for (String t : terminalCols)    row.append(String.format("%-20s", a.getOrDefault(t, "")));
                for (String nt : nonTerminalCols) row.append(String.format("%-15s", g.containsKey(nt) ? g.get(nt) : ""));
                w.write(row.toString()); w.newLine();
            }

            w.newLine();
            if (conflicts.isEmpty()) {
                w.write("Grammar is LR(1) -- no conflicts detected.");
                w.newLine();
            } else {
                w.write("Conflicts:"); w.newLine();
                for (String c : conflicts) { w.write("  " + c); w.newLine(); }
            }
        }
    }
}