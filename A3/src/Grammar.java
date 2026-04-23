import java.io.*;
import java.util.*;

public class Grammar {

    public static final String EPSILON = "ε";
    public static final String AUGMENTED_START_SUFFIX = "'";

    private final List<String[]> productions = new ArrayList<>();
    private final Set<String> nonTerminals = new LinkedHashSet<>();
    private final Set<String> terminals = new LinkedHashSet<>();
    private String startSymbol;
    private String augmentedStart;

    public Grammar(String filePath) throws IOException {
        loadFromFile(filePath);
        augment();
    }

    private void loadFromFile(String filePath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;
        boolean firstRule = true;

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;

            String[] arrowSplit = line.split("->", 2);
            if (arrowSplit.length < 2) continue;

            String lhs = arrowSplit[0].trim();
            String[] alternatives = arrowSplit[1].split("\\|");

            if (firstRule) {
                startSymbol = lhs;
                firstRule = false;
            }

            nonTerminals.add(lhs);

            for (String alternative : alternatives) {
                String[] rhsSymbols = alternative.trim().split("\\s+");
                String[] production = new String[rhsSymbols.length + 1];
                production[0] = lhs;
                System.arraycopy(rhsSymbols, 0, production, 1, rhsSymbols.length);
                productions.add(production);
            }
        }
        reader.close();

        for (String[] production : productions) {
            for (int i = 1; i < production.length; i++) {
                String symbol = production[i];
                if (!nonTerminals.contains(symbol) && !symbol.equals(EPSILON)) {
                    terminals.add(symbol);
                }
            }
        }
    }

    private void augment() {
        augmentedStart = startSymbol + AUGMENTED_START_SUFFIX;
        String[] augmentedProduction = {augmentedStart, startSymbol};
        productions.add(0, augmentedProduction);
        nonTerminals.add(augmentedStart);
    }

    public List<String[]> getProductions() {
        return Collections.unmodifiableList(productions);
    }

    public Set<String> getNonTerminals() {
        return Collections.unmodifiableSet(nonTerminals);
    }

    public Set<String> getTerminals() {
        return Collections.unmodifiableSet(terminals);
    }

    public String getStartSymbol() {
        return startSymbol;
    }

    public String getAugmentedStart() {
        return augmentedStart;
    }

    public List<String[]> getProductionsFor(String nonTerminal) {
        List<String[]> result = new ArrayList<>();
        for (String[] production : productions) {
            if (production[0].equals(nonTerminal)) {
                result.add(production);
            }
        }
        return result;
    }

    public int getProductionIndex(String[] targetProduction) {
        for (int i = 0; i < productions.size(); i++) {
            if (Arrays.equals(productions.get(i), targetProduction)) {
                return i;
            }
        }
        return -1;
    }
}
