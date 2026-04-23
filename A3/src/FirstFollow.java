import java.util.*;

public class FirstFollow {

    private final Grammar grammar;
    private final Map<String, Set<String>> firstSets = new HashMap<>();
    private final Map<String, Set<String>> followSets = new HashMap<>();

    public FirstFollow(Grammar grammar) {
        this.grammar = grammar;
        initializeSets();
        computeAllFirstSets();
        computeAllFollowSets();
    }

    private void initializeSets() {
        for (String nonTerminal : grammar.getNonTerminals()) {
            firstSets.put(nonTerminal, new LinkedHashSet<>());
            followSets.put(nonTerminal, new LinkedHashSet<>());
        }
    }

    private void computeAllFirstSets() {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (String[] production : grammar.getProductions()) {
                String lhs = production[0];
                Set<String> firstOfLhs = firstSets.get(lhs);
                Set<String> derived = computeFirstOfSequence(production, 1);
                if (firstOfLhs.addAll(derived)) {
                    changed = true;
                }
            }
        }
    }

    private Set<String> computeFirstOfSequence(String[] symbols, int startIndex) {
        Set<String> result = new LinkedHashSet<>();

        if (startIndex >= symbols.length) {
            result.add(Grammar.EPSILON);
            return result;
        }

        boolean allPrecedingDerivEpsilon = true;

        for (int i = startIndex; i < symbols.length && allPrecedingDerivEpsilon; i++) {
            String symbol = symbols[i];
            allPrecedingDerivEpsilon = false;

            if (grammar.getNonTerminals().contains(symbol)) {
                Set<String> firstOfSymbol = firstSets.getOrDefault(symbol, new HashSet<>());
                for (String terminal : firstOfSymbol) {
                    if (!terminal.equals(Grammar.EPSILON)) {
                        result.add(terminal);
                    }
                }
                if (firstOfSymbol.contains(Grammar.EPSILON)) {
                    allPrecedingDerivEpsilon = true;
                }
            } else if (symbol.equals(Grammar.EPSILON)) {
                allPrecedingDerivEpsilon = true;
            } else {
                result.add(symbol);
            }
        }

        if (allPrecedingDerivEpsilon) {
            result.add(Grammar.EPSILON);
        }

        return result;
    }

    private void computeAllFollowSets() {
        followSets.get(grammar.getAugmentedStart()).add("$");

        boolean changed = true;
        while (changed) {
            changed = false;
            for (String[] production : grammar.getProductions()) {
                String lhs = production[0];

                for (int dotPos = 1; dotPos < production.length; dotPos++) {
                    String symbol = production[dotPos];

                    if (!grammar.getNonTerminals().contains(symbol)) continue;

                    Set<String> followOfSymbol = followSets.get(symbol);
                    Set<String> firstOfBeta = computeFirstOfSequence(production, dotPos + 1);

                    for (String terminal : firstOfBeta) {
                        if (!terminal.equals(Grammar.EPSILON)) {
                            if (followOfSymbol.add(terminal)) changed = true;
                        }
                    }

                    if (firstOfBeta.contains(Grammar.EPSILON)) {
                        if (followOfSymbol.addAll(followSets.get(lhs))) {
                            changed = true;
                        }
                    }
                }
            }
        }
    }

    public Set<String> getFirstSet(String nonTerminal) {
        return Collections.unmodifiableSet(firstSets.getOrDefault(nonTerminal, new HashSet<>()));
    }

    public Set<String> getFollowSet(String nonTerminal) {
        return Collections.unmodifiableSet(followSets.getOrDefault(nonTerminal, new HashSet<>()));
    }

    public Map<String, Set<String>> getAllFirstSets() {
        return Collections.unmodifiableMap(firstSets);
    }

    public Map<String, Set<String>> getAllFollowSets() {
        return Collections.unmodifiableMap(followSets);
    }
}
