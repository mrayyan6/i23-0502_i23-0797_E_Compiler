import java.io.*;
import java.util.*;

public class SLRBuilder {

    private final Grammar grammar;
    private final FirstFollow firstFollow;
    private final List<State> canonicalCollection = new ArrayList<>();
    private final Map<Integer, Map<String, String>> actionTable = new LinkedHashMap<>();
    private final Map<Integer, Map<String, Integer>> gotoTable = new LinkedHashMap<>();
    private final List<String> conflicts = new ArrayList<>();

    public SLRBuilder(Grammar grammar, FirstFollow firstFollow) {
        this.grammar = grammar;
        this.firstFollow = firstFollow;
        buildCanonicalCollection();
        buildSLRParsingTable();
    }

    private Set<Item> closure(Set<Item> initialItems) {
        Set<Item> closureSet = new LinkedHashSet<>(initialItems);
        Queue<Item> worklist = new LinkedList<>(initialItems);

        while (!worklist.isEmpty()) {
            Item currentItem = worklist.poll();
            String symbolAfterDot = currentItem.getSymbolAfterDot();

            if (symbolAfterDot == null || !grammar.getNonTerminals().contains(symbolAfterDot)) {
                continue;
            }

            for (String[] production : grammar.getProductionsFor(symbolAfterDot)) {
                Item newItem = new Item(production, 0);
                if (closureSet.add(newItem)) {
                    worklist.add(newItem);
                }
            }
        }

        return closureSet;
    }

    private Set<Item> goto_(Set<Item> stateItems, String symbol) {
        Set<Item> movedItems = new LinkedHashSet<>();

        for (Item item : stateItems) {
            if (symbol.equals(item.getSymbolAfterDot())) {
                movedItems.add(item.advance());
            }
        }

        return movedItems.isEmpty() ? Collections.emptySet() : closure(movedItems);
    }

    private void buildCanonicalCollection() {
        String[] augmentedProduction = grammar.getProductionsFor(grammar.getAugmentedStart()).get(0);
        Item startItem = new Item(augmentedProduction, 0);
        Set<Item> initialClosureSet = closure(new LinkedHashSet<>(Collections.singleton(startItem)));

        State initialState = new State(0, initialClosureSet);
        canonicalCollection.add(initialState);

        Queue<State> unprocessed = new LinkedList<>();
        unprocessed.add(initialState);

        Set<String> allSymbols = new LinkedHashSet<>();
        allSymbols.addAll(grammar.getNonTerminals());
        allSymbols.addAll(grammar.getTerminals());

        while (!unprocessed.isEmpty()) {
            State currentState = unprocessed.poll();

            for (String symbol : allSymbols) {
                Set<Item> gotoItems = goto_(currentState.getItems(), symbol);
                if (gotoItems.isEmpty()) continue;

                int existingStateId = findExistingState(gotoItems);

                if (existingStateId == -1) {
                    int newStateId = canonicalCollection.size();
                    State newState = new State(newStateId, gotoItems);
                    canonicalCollection.add(newState);
                    currentState.addTransition(symbol, newStateId);
                    unprocessed.add(newState);
                } else {
                    currentState.addTransition(symbol, existingStateId);
                }
            }
        }
    }

    private int findExistingState(Set<Item> items) {
        for (State state : canonicalCollection) {
            if (state.hasSameCore(items)) {
                return state.getStateId();
            }
        }
        return -1;
    }

    private void buildSLRParsingTable() {
        for (State state : canonicalCollection) {
            int stateId = state.getStateId();
            actionTable.put(stateId, new LinkedHashMap<>());
            gotoTable.put(stateId, new LinkedHashMap<>());

            for (Map.Entry<String, Integer> transition : state.getTransitions().entrySet()) {
                String symbol = transition.getKey();
                int targetStateId = transition.getValue();

                if (grammar.getTerminals().contains(symbol)) {
                    setActionEntry(stateId, symbol, "shift " + targetStateId);
                } else if (grammar.getNonTerminals().contains(symbol)) {
                    gotoTable.get(stateId).put(symbol, targetStateId);
                }
            }

            for (Item item : state.getItems()) {
                if (!item.isDotAtEnd()) continue;

                String lhs = item.getLhs();

                if (lhs.equals(grammar.getAugmentedStart())) {
                    setActionEntry(stateId, "$", "accept");
                    continue;
                }

                int productionIndex = grammar.getProductionIndex(item.getProduction());
                String reduceAction = "reduce " + productionIndex;

                for (String followSymbol : firstFollow.getFollowSet(lhs)) {
                    setActionEntry(stateId, followSymbol, reduceAction);
                }
            }
        }
    }

    private void setActionEntry(int stateId, String symbol, String newAction) {
        Map<String, String> row = actionTable.get(stateId);
        String existingAction = row.get(symbol);

        if (existingAction == null) {
            row.put(symbol, newAction);
        } else if (!existingAction.equals(newAction)) {
            String conflictType = (existingAction.startsWith("shift") && newAction.startsWith("reduce"))
                    || (existingAction.startsWith("reduce") && newAction.startsWith("shift"))
                    ? "SHIFT-REDUCE" : "REDUCE-REDUCE";

            String conflictReport = conflictType + " conflict in state " + stateId
                    + " on symbol '" + symbol + "': existing='" + existingAction
                    + "' vs new='" + newAction + "'";
            conflicts.add(conflictReport);
            row.put(symbol, existingAction + " / " + newAction);
        }
    }

    public List<State> getCanonicalCollection() {
        return Collections.unmodifiableList(canonicalCollection);
    }

    public Map<Integer, Map<String, String>> getActionTable() {
        return Collections.unmodifiableMap(actionTable);
    }

    public Map<Integer, Map<String, Integer>> getGotoTable() {
        return Collections.unmodifiableMap(gotoTable);
    }

    public List<String> getConflicts() {
        return Collections.unmodifiableList(conflicts);
    }

    public void saveStates(PrintWriter pw) {
        pw.println("LR(0) / SLR(1) Canonical Collection");
        pw.println("Total states: " + canonicalCollection.size());
        pw.println("=".repeat(60));
        pw.println();
        for (State s : canonicalCollection) {
            pw.print(s.toString());
            Map<String, Integer> tr = s.getTransitions();
            if (!tr.isEmpty()) {
                pw.println("  Goto:");
                for (Map.Entry<String, Integer> e : tr.entrySet()) {
                    pw.println("    " + e.getKey() + " -> State " + e.getValue());
                }
            }
            pw.println("-".repeat(60));
        }
    }

    public void saveTable(PrintWriter pw) {
        Set<String> termCols = new LinkedHashSet<>(grammar.getTerminals());
        termCols.add("$");
        Set<String> ntCols = new LinkedHashSet<>(grammar.getNonTerminals());
        ntCols.remove(grammar.getAugmentedStart());

        pw.println("SLR(1) Parsing Table");
        pw.println("Total states: " + canonicalCollection.size());
        pw.println("=".repeat(80));
        pw.println();

        StringBuilder header = new StringBuilder();
        header.append(String.format("%-8s", "State"));
        for (String t : termCols)  header.append(String.format("%-20s", t));
        for (String nt : ntCols)   header.append(String.format("%-15s", nt));
        pw.println(header.toString());
        pw.println("-".repeat(8 + termCols.size() * 20 + ntCols.size() * 15));

        for (int id = 0; id < canonicalCollection.size(); id++) {
            StringBuilder row = new StringBuilder();
            row.append(String.format("%-8d", id));
            Map<String, String>  a = actionTable.getOrDefault(id, Collections.emptyMap());
            Map<String, Integer> g = gotoTable.getOrDefault(id, Collections.emptyMap());
            for (String t : termCols)  row.append(String.format("%-20s", a.getOrDefault(t, "")));
            for (String nt : ntCols)   row.append(String.format("%-15s", g.containsKey(nt) ? g.get(nt) : ""));
            pw.println(row.toString());
        }

        pw.println();
        if (conflicts.isEmpty()) {
            pw.println("Grammar is SLR(1) -- no conflicts detected.");
        } else {
            pw.println("SLR(1) Conflicts:");
            for (String c : conflicts) pw.println("  " + c);
        }
    }

    public void printCanonicalCollection() {
        System.out.println("===== Canonical Collection of LR(0) States =====");
        for (State state : canonicalCollection) {
            System.out.print(state);
        }
    }

    public void printParsingTable() {
        Set<String> terminalColumns = new LinkedHashSet<>(grammar.getTerminals());
        terminalColumns.add("$");
        Set<String> nonTerminalColumns = new LinkedHashSet<>(grammar.getNonTerminals());
        nonTerminalColumns.remove(grammar.getAugmentedStart());

        System.out.println("\n===== SLR(1) Parsing Table =====");

        System.out.printf("%-8s", "State");
        for (String terminal : terminalColumns) {
            System.out.printf("%-20s", terminal);
        }
        for (String nonTerminal : nonTerminalColumns) {
            System.out.printf("%-15s", nonTerminal);
        }
        System.out.println();

        System.out.println("-".repeat(8 + terminalColumns.size() * 20 + nonTerminalColumns.size() * 15));

        for (int stateId = 0; stateId < canonicalCollection.size(); stateId++) {
            System.out.printf("%-8d", stateId);
            Map<String, String> actionRow = actionTable.getOrDefault(stateId, Collections.emptyMap());
            for (String terminal : terminalColumns) {
                String entry = actionRow.getOrDefault(terminal, "");
                System.out.printf("%-20s", entry);
            }
            Map<String, Integer> gotoRow = gotoTable.getOrDefault(stateId, Collections.emptyMap());
            for (String nonTerminal : nonTerminalColumns) {
                String entry = gotoRow.containsKey(nonTerminal) ? String.valueOf(gotoRow.get(nonTerminal)) : "";
                System.out.printf("%-15s", entry);
            }
            System.out.println();
        }

        if (!conflicts.isEmpty()) {
            System.out.println("\n===== Conflicts Detected =====");
            for (String conflict : conflicts) {
                System.out.println(conflict);
            }
        } else {
            System.out.println("\nGrammar is SLR(1) — no conflicts detected.");
        }
    }
}
