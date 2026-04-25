import java.io.*;
import java.util.*;

public class LR1CollectionBuilder {

    private final Grammar grammar;
    private final FirstFollow firstFollow;
    private final List<State1> canonicalCollection = new ArrayList<>();

    public LR1CollectionBuilder(Grammar grammar, FirstFollow firstFollow) {
        this.grammar = grammar;
        this.firstFollow = firstFollow;
        buildCanonicalCollection();
    }

    private Set<String> computeFirstOfBetaPlusLookahead(String[] production, int betaStartIndex, String lookahead) {
        Set<String> result = new LinkedHashSet<>();
        boolean betaDerivesEpsilon = true;

        for (int i = betaStartIndex; i < production.length && betaDerivesEpsilon; i++) {
            String symbol = production[i];
            betaDerivesEpsilon = false;

            if (grammar.getTerminals().contains(symbol)) {
                result.add(symbol);
            } else if (symbol.equals(Grammar.EPSILON)) {
                betaDerivesEpsilon = true;
            } else if (grammar.getNonTerminals().contains(symbol)) {
                Set<String> firstOfSymbol = firstFollow.getFirstSet(symbol);
                for (String terminal : firstOfSymbol) {
                    if (!terminal.equals(Grammar.EPSILON)) {
                        result.add(terminal);
                    }
                }
                if (firstOfSymbol.contains(Grammar.EPSILON)) {
                    betaDerivesEpsilon = true;
                }
            }
        }

        if (betaDerivesEpsilon) {
            result.add(lookahead);
        }

        return result;
    }

    private Set<Item1> closure(Set<Item1> initialItems) {
        Set<Item1> closureSet = new LinkedHashSet<>(initialItems);
        Queue<Item1> worklist = new LinkedList<>(initialItems);

        while (!worklist.isEmpty()) {
            Item1 currentItem = worklist.poll();
            String symbolAfterDot = currentItem.getSymbolAfterDot();

            if (symbolAfterDot == null || !grammar.getNonTerminals().contains(symbolAfterDot)) {
                continue;
            }

            int betaStartIndex = currentItem.getDotPosition() + 2;
            Set<String> lookaheadsForNewItems = computeFirstOfBetaPlusLookahead(
                    currentItem.getProduction(), betaStartIndex, currentItem.getLookahead());

            for (String[] production : grammar.getProductionsFor(symbolAfterDot)) {
                for (String newLookahead : lookaheadsForNewItems) {
                    Item1 newItem = new Item1(production, 0, newLookahead);
                    if (closureSet.add(newItem)) {
                        worklist.add(newItem);
                    }
                }
            }
        }

        return closureSet;
    }

    private Set<Item1> goto_(Set<Item1> stateItems, String symbol) {
        Set<Item1> movedItems = new LinkedHashSet<>();

        for (Item1 item : stateItems) {
            if (symbol.equals(item.getSymbolAfterDot())) {
                movedItems.add(item.advance());
            }
        }

        return movedItems.isEmpty() ? Collections.emptySet() : closure(movedItems);
    }

    private void buildCanonicalCollection() {
        String[] augmentedProduction = grammar.getProductionsFor(grammar.getAugmentedStart()).get(0);
        Item1 startItem = new Item1(augmentedProduction, 0, "$");
        Set<Item1> initialClosureSet = closure(new LinkedHashSet<>(Collections.singleton(startItem)));

        State1 initialState = new State1(0, initialClosureSet);
        canonicalCollection.add(initialState);

        Set<String> allGrammarSymbols = new LinkedHashSet<>();
        allGrammarSymbols.addAll(grammar.getNonTerminals());
        allGrammarSymbols.addAll(grammar.getTerminals());

        Queue<State1> unprocessed = new LinkedList<>();
        unprocessed.add(initialState);

        while (!unprocessed.isEmpty()) {
            State1 currentState = unprocessed.poll();

            for (String symbol : allGrammarSymbols) {
                Set<Item1> gotoItems = goto_(currentState.getItems(), symbol);
                if (gotoItems.isEmpty()) continue;

                int existingStateId = findExistingState(gotoItems);

                if (existingStateId == -1) {
                    int newStateId = canonicalCollection.size();
                    State1 newState = new State1(newStateId, gotoItems);
                    canonicalCollection.add(newState);
                    currentState.addTransition(symbol, newStateId);
                    unprocessed.add(newState);
                } else {
                    currentState.addTransition(symbol, existingStateId);
                }
            }
        }
    }

    private int findExistingState(Set<Item1> items) {
        State1 candidate = new State1(-1, items);
        for (State1 state : canonicalCollection) {
            if (state.equals(candidate)) {
                return state.getStateId();
            }
        }
        return -1;
    }

    public List<State1> getCanonicalCollection() {
        return Collections.unmodifiableList(canonicalCollection);
    }

    public void printCanonicalCollection() {
        System.out.println("===== Canonical Collection of LR(1) States =====");
        System.out.println("Total states: " + canonicalCollection.size());
        System.out.println();
        for (State1 state : canonicalCollection) {
            System.out.print(state);
            System.out.println();
        }
    }

    public void saveStates(PrintWriter pw) {
        pw.println("LR(1) Canonical Collection");
        pw.println("Total states: " + canonicalCollection.size());
        pw.println("=".repeat(60));
        pw.println();

        for (State1 state : canonicalCollection) {
            pw.println("State " + state.getStateId());
            pw.println("-".repeat(40));

            List<Item1> sortedItems = new ArrayList<>(state.getItems());
            sortedItems.sort(Comparator.comparing(Item1::toString));

            pw.println("Items:");
            for (Item1 item : sortedItems) {
                pw.println("  " + item.toString());
            }
            pw.println();

            Map<String, Integer> transitions = state.getTransitions();
            if (!transitions.isEmpty()) {
                pw.println("Goto:");
                List<String> sortedSymbols = new ArrayList<>(transitions.keySet());
                Collections.sort(sortedSymbols);
                for (String symbol : sortedSymbols) {
                    pw.println("  " + symbol + " -> State " + transitions.get(symbol));
                }
            } else {
                pw.println("Goto: (none)");
            }

            pw.println();
            pw.println("=".repeat(60));
            pw.println();
        }
    }
}
