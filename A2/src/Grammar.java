import java.io.*;
import java.util.*;

/**
 * Grammar.java
 * Handles reading a CFG from file, left factoring, left recursion removal,
 * FIRST/FOLLOW set computation, and LL(1) parsing table construction.
 */
public class Grammar {

    // Ordered list of non-terminals (preserves insertion order)
    public List<String> nonTerminals = new ArrayList<>();
    public Set<String> nonTerminalSet = new LinkedHashSet<>();
    public Set<String> terminals = new LinkedHashSet<>();
    public List<Production> productions = new ArrayList<>();
    public String startSymbol = null;

    // FIRST and FOLLOW sets
    public Map<String, Set<String>> firstSets = new LinkedHashMap<>();
    public Map<String, Set<String>> followSets = new LinkedHashMap<>();

    // LL(1) Parsing Table: M[NonTerminal][Terminal] -> production index (-1 = empty)
    public Map<String, Map<String, Integer>> parsingTable = new LinkedHashMap<>();
    public boolean isLL1 = true;

    // -----------------------------------------------------------------------
    // Read grammar from file
    // -----------------------------------------------------------------------
    public boolean readFromFile(String filename) {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("//") || line.startsWith("#")) continue;

                // Split on ->
                int arrowIdx = line.indexOf("->");
                if (arrowIdx < 0) continue;

                String lhs = line.substring(0, arrowIdx).trim();
                String rhsStr = line.substring(arrowIdx + 2).trim();

                if (!nonTerminalSet.contains(lhs)) {
                    nonTerminals.add(lhs);
                    nonTerminalSet.add(lhs);
                }
                if (startSymbol == null) startSymbol = lhs;

                // Split alternatives on |
                String[] alternatives = splitAlternatives(rhsStr);
                for (String alt : alternatives) {
                    alt = alt.trim();
                    if (alt.isEmpty()) continue;
                    List<String> symbols = tokenizeRHS(alt);
                    productions.add(new Production(lhs, symbols));
                }
            }
            collectTerminals();
            return true;
        } catch (IOException e) {
            System.err.println("Error reading grammar file: " + e.getMessage());
            return false;
        }
    }

    /**
     * Split RHS by | but not inside parentheses.
     */
    private String[] splitAlternatives(String rhs) {
        List<String> alts = new ArrayList<>();
        int depth = 0;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < rhs.length(); i++) {
            char c = rhs.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            if (c == '|' && depth == 0) {
                alts.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        alts.add(current.toString());
        return alts.toArray(new String[0]);
    }

    /**
     * Tokenize the RHS of a production into individual symbols.
     * Multi-character names starting with uppercase are non-terminals.
     * Lowercase words, operators, keywords are terminals.
     * 'epsilon' or '@' is epsilon.
     */
    private List<String> tokenizeRHS(String rhs) {
        List<String> tokens = new ArrayList<>();
        String[] parts = rhs.trim().split("\\s+");
        for (String p : parts) {
            if (p.isEmpty()) continue;
            tokens.add(p);
        }
        return tokens;
    }

    /**
     * Collect all terminal symbols from productions.
     */
    public void collectTerminals() {
        terminals.clear();
        for (Production prod : productions) {
            for (String sym : prod.rhs) {
                if (!isNonTerminal(sym) && !isEpsilon(sym)) {
                    terminals.add(sym);
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Left Factoring
    // -----------------------------------------------------------------------
    public void applyLeftFactoring() {
        boolean changed = true;
        while (changed) {
            changed = false;
            List<String> ntsCopy = new ArrayList<>(nonTerminals);
            for (String nt : ntsCopy) {
                if (leftFactorNT(nt)) {
                    changed = true;
                }
            }
        }
        collectTerminals();
    }

    private boolean leftFactorNT(String nt) {
        // Get all productions for this non-terminal
        List<Production> prods = getProductionsFor(nt);
        if (prods.size() <= 1) return false;

        // Group by first symbol
        Map<String, List<Production>> groups = new LinkedHashMap<>();
        for (Production p : prods) {
            String firstSym = p.rhs.isEmpty() ? "epsilon" : p.rhs.get(0);
            groups.computeIfAbsent(firstSym, k -> new ArrayList<>()).add(p);
        }

        for (Map.Entry<String, List<Production>> entry : groups.entrySet()) {
            List<Production> group = entry.getValue();
            if (group.size() <= 1) continue;

            // Find longest common prefix
            List<String> prefix = new ArrayList<>(group.get(0).rhs);
            for (int i = 1; i < group.size(); i++) {
                List<String> other = group.get(i).rhs;
                int len = Math.min(prefix.size(), other.size());
                int common = 0;
                for (int j = 0; j < len; j++) {
                    if (prefix.get(j).equals(other.get(j))) common++;
                    else break;
                }
                prefix = prefix.subList(0, common);
            }

            if (prefix.isEmpty()) continue;

            // Create new non-terminal
            String newNT = makePrimeName(nt);
            addNonTerminalAfter(newNT, nt);

            // Remove old productions
            productions.removeAll(group);

            // Add A -> prefix A'
            List<String> newRhs = new ArrayList<>(prefix);
            newRhs.add(newNT);
            productions.add(new Production(nt, newRhs));

            // Add A' -> suffix1 | suffix2 | ...
            for (Production p : group) {
                List<String> suffix = new ArrayList<>(p.rhs.subList(prefix.size(), p.rhs.size()));
                if (suffix.isEmpty()) {
                    suffix.add("epsilon");
                }
                productions.add(new Production(newNT, suffix));
            }

            return true; // Restart
        }
        return false;
    }

    // -----------------------------------------------------------------------
    // Left Recursion Removal (handles both direct and indirect)
    // -----------------------------------------------------------------------
    public void removeLeftRecursion() {
        // Order non-terminals
        List<String> ordered = new ArrayList<>(nonTerminals);

        for (int i = 0; i < ordered.size(); i++) {
            String ai = ordered.get(i);

            // Substitute indirect left recursion
            for (int j = 0; j < i; j++) {
                String aj = ordered.get(j);
                List<Production> aiProds = new ArrayList<>(getProductionsFor(ai));
                for (Production p : aiProds) {
                    if (!p.rhs.isEmpty() && p.rhs.get(0).equals(aj)) {
                        // Replace Ai -> Aj gamma with Ai -> delta1 gamma | delta2 gamma | ...
                        productions.remove(p);
                        List<String> gamma = new ArrayList<>(p.rhs.subList(1, p.rhs.size()));
                        List<Production> ajProds = getProductionsFor(aj);
                        for (Production ajP : ajProds) {
                            List<String> newRhs = new ArrayList<>();
                            if (ajP.rhs.size() == 1 && isEpsilon(ajP.rhs.get(0))) {
                                // Aj -> epsilon: Ai -> gamma
                                if (gamma.isEmpty()) {
                                    newRhs.add("epsilon");
                                } else {
                                    newRhs.addAll(gamma);
                                }
                            } else {
                                newRhs.addAll(ajP.rhs);
                                newRhs.addAll(gamma);
                            }
                            productions.add(new Production(ai, newRhs));
                        }
                    }
                }
            }

            // Eliminate direct left recursion from Ai
            eliminateDirectLeftRecursion(ai);
        }
        collectTerminals();
    }

    private void eliminateDirectLeftRecursion(String nt) {
        List<Production> prods = getProductionsFor(nt);
        List<Production> recursive = new ArrayList<>();
        List<Production> nonRecursive = new ArrayList<>();

        for (Production p : prods) {
            if (!p.rhs.isEmpty() && p.rhs.get(0).equals(nt)) {
                recursive.add(p);
            } else {
                nonRecursive.add(p);
            }
        }

        if (recursive.isEmpty()) return;

        String newNT = makePrimeName(nt);
        addNonTerminalAfter(newNT, nt);

        // Remove old productions
        productions.removeAll(prods);

        // A -> beta1 A' | beta2 A' | ...
        for (Production p : nonRecursive) {
            List<String> newRhs = new ArrayList<>();
            if (p.rhs.size() == 1 && isEpsilon(p.rhs.get(0))) {
                newRhs.add(newNT);
            } else {
                newRhs.addAll(p.rhs);
                newRhs.add(newNT);
            }
            productions.add(new Production(nt, newRhs));
        }

        // A' -> alpha1 A' | alpha2 A' | ... | epsilon
        for (Production p : recursive) {
            List<String> alpha = new ArrayList<>(p.rhs.subList(1, p.rhs.size()));
            alpha.add(newNT);
            productions.add(new Production(newNT, alpha));
        }
        productions.add(new Production(newNT, Arrays.asList("epsilon")));
    }

    // -----------------------------------------------------------------------
    // FIRST Set Computation
    // -----------------------------------------------------------------------
    public void computeFirstSets() {
        // Initialize
        for (String nt : nonTerminals) {
            firstSets.put(nt, new LinkedHashSet<>());
        }

        boolean changed = true;
        while (changed) {
            changed = false;
            for (Production prod : productions) {
                Set<String> firstOfRhs = computeFirstOfSequence(prod.rhs);
                Set<String> currentFirst = firstSets.get(prod.lhs);
                if (currentFirst == null) {
                    currentFirst = new LinkedHashSet<>();
                    firstSets.put(prod.lhs, currentFirst);
                }
                if (currentFirst.addAll(firstOfRhs)) {
                    changed = true;
                }
            }
        }
    }

    public Set<String> computeFirstOfSequence(List<String> seq) {
        Set<String> result = new LinkedHashSet<>();
        if (seq.isEmpty() || (seq.size() == 1 && isEpsilon(seq.get(0)))) {
            result.add("epsilon");
            return result;
        }

        for (int i = 0; i < seq.size(); i++) {
            String sym = seq.get(i);
            if (isEpsilon(sym)) {
                result.add("epsilon");
                return result;
            }
            if (isTerminal(sym) || sym.equals("$")) {
                result.add(sym);
                return result;
            }
            // Non-terminal
            Set<String> firstOfSym = firstSets.getOrDefault(sym, new LinkedHashSet<>());
            result.addAll(firstOfSym);
            result.remove("epsilon");
            if (!firstOfSym.contains("epsilon")) {
                return result;
            }
            // If epsilon in FIRST(sym), continue to next symbol
        }
        // All symbols can derive epsilon
        result.add("epsilon");
        return result;
    }

    // -----------------------------------------------------------------------
    // FOLLOW Set Computation
    // -----------------------------------------------------------------------
    public void computeFollowSets() {
        for (String nt : nonTerminals) {
            followSets.put(nt, new LinkedHashSet<>());
        }
        // Rule 1: $ in FOLLOW(Start)
        followSets.get(startSymbol).add("$");

        boolean changed = true;
        while (changed) {
            changed = false;
            for (Production prod : productions) {
                for (int i = 0; i < prod.rhs.size(); i++) {
                    String B = prod.rhs.get(i);
                    if (!isNonTerminal(B)) continue;

                    // beta = symbols after B
                    List<String> beta = new ArrayList<>(prod.rhs.subList(i + 1, prod.rhs.size()));
                    Set<String> firstOfBeta = computeFirstOfSequence(beta);

                    Set<String> followB = followSets.get(B);
                    if (followB == null) {
                        followB = new LinkedHashSet<>();
                        followSets.put(B, followB);
                    }

                    // Rule 2: Add FIRST(beta) - {epsilon} to FOLLOW(B)
                    Set<String> toAdd = new LinkedHashSet<>(firstOfBeta);
                    toAdd.remove("epsilon");
                    if (followB.addAll(toAdd)) changed = true;

                    // Rule 3: If beta can derive epsilon, add FOLLOW(A) to FOLLOW(B)
                    if (firstOfBeta.contains("epsilon")) {
                        Set<String> followA = followSets.getOrDefault(prod.lhs, new LinkedHashSet<>());
                        if (followB.addAll(followA)) changed = true;
                    }
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // LL(1) Parsing Table Construction
    // -----------------------------------------------------------------------
    public void buildParsingTable() {
        isLL1 = true;

        // Initialize table
        List<String> cols = new ArrayList<>(terminals);
        cols.add("$");

        for (String nt : nonTerminals) {
            Map<String, Integer> row = new LinkedHashMap<>();
            for (String t : cols) {
                row.put(t, -1); // -1 means empty
            }
            parsingTable.put(nt, row);
        }

        // Fill table
        for (int idx = 0; idx < productions.size(); idx++) {
            Production prod = productions.get(idx);
            Set<String> firstOfRhs = computeFirstOfSequence(prod.rhs);

            // For each terminal a in FIRST(rhs)
            for (String a : firstOfRhs) {
                if (!a.equals("epsilon")) {
                    Map<String, Integer> row = parsingTable.get(prod.lhs);
                    if (row != null && row.containsKey(a)) {
                        if (row.get(a) != -1 && row.get(a) != idx) {
                            isLL1 = false; // Conflict
                            row.put(a, -2); // -2 = conflict
                        } else {
                            row.put(a, idx);
                        }
                    }
                }
            }

            // If epsilon in FIRST(rhs)
            if (firstOfRhs.contains("epsilon")) {
                Set<String> followLhs = followSets.getOrDefault(prod.lhs, new LinkedHashSet<>());
                for (String b : followLhs) {
                    Map<String, Integer> row = parsingTable.get(prod.lhs);
                    if (row != null && row.containsKey(b)) {
                        if (row.get(b) != -1 && row.get(b) != idx) {
                            isLL1 = false;
                            row.put(b, -2);
                        } else {
                            row.put(b, idx);
                        }
                    }
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Utility methods
    // -----------------------------------------------------------------------
    public boolean isNonTerminal(String sym) {
        return nonTerminalSet.contains(sym);
    }

    public boolean isTerminal(String sym) {
        return !isNonTerminal(sym) && !isEpsilon(sym) && !sym.equals("$");
    }

    public boolean isEpsilon(String sym) {
        return sym.equals("epsilon") || sym.equals("@");
    }

    public List<Production> getProductionsFor(String nt) {
        List<Production> result = new ArrayList<>();
        for (Production p : productions) {
            if (p.lhs.equals(nt)) result.add(p);
        }
        return result;
    }

    private String makePrimeName(String name) {
        String candidate = name + "Prime";
        while (nonTerminalSet.contains(candidate)) {
            candidate += "Prime";
        }
        return candidate;
    }

    private void addNonTerminalAfter(String newNT, String afterNT) {
        if (nonTerminalSet.contains(newNT)) return;
        nonTerminalSet.add(newNT);
        int idx = nonTerminals.indexOf(afterNT);
        if (idx >= 0) {
            nonTerminals.add(idx + 1, newNT);
        } else {
            nonTerminals.add(newNT);
        }
    }

    // -----------------------------------------------------------------------
    // Display methods
    // -----------------------------------------------------------------------
    public void printGrammar(String title) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println(title);
        System.out.println("=".repeat(60));
        for (String nt : nonTerminals) {
            List<Production> prods = getProductionsFor(nt);
            if (prods.isEmpty()) continue;
            StringBuilder sb = new StringBuilder();
            sb.append(nt).append(" -> ");
            for (int i = 0; i < prods.size(); i++) {
                if (i > 0) sb.append(" | ");
                sb.append(String.join(" ", prods.get(i).rhs));
            }
            System.out.println(sb.toString());
        }
    }

    public void printFirstSets() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("FIRST Sets");
        System.out.println("=".repeat(60));
        System.out.printf("%-20s %s%n", "Non-Terminal", "FIRST Set");
        System.out.println("-".repeat(60));
        for (String nt : nonTerminals) {
            Set<String> first = firstSets.getOrDefault(nt, new LinkedHashSet<>());
            System.out.printf("%-20s { %s }%n", nt, String.join(", ", first));
        }
    }

    public void printFollowSets() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("FOLLOW Sets");
        System.out.println("=".repeat(60));
        System.out.printf("%-20s %s%n", "Non-Terminal", "FOLLOW Set");
        System.out.println("-".repeat(60));
        for (String nt : nonTerminals) {
            Set<String> follow = followSets.getOrDefault(nt, new LinkedHashSet<>());
            System.out.printf("%-20s { %s }%n", nt, String.join(", ", follow));
        }
    }

    public void printParsingTable() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("LL(1) Parsing Table");
        System.out.println("=".repeat(80));

        List<String> cols = new ArrayList<>(terminals);
        cols.add("$");

        // Header
        System.out.printf("%-18s", "");
        for (String t : cols) {
            System.out.printf("| %-18s", t);
        }
        System.out.println("|");
        System.out.println("-".repeat(18 + cols.size() * 20 + 1));

        // Rows
        for (String nt : nonTerminals) {
            System.out.printf("%-18s", nt);
            Map<String, Integer> row = parsingTable.get(nt);
            for (String t : cols) {
                int idx = (row != null && row.containsKey(t)) ? row.get(t) : -1;
                String cell;
                if (idx == -1) {
                    cell = "";
                } else if (idx == -2) {
                    cell = "CONFLICT";
                } else {
                    Production p = productions.get(idx);
                    cell = p.lhs + "->" + String.join(" ", p.rhs);
                }
                System.out.printf("| %-18s", cell);
            }
            System.out.println("|");
        }

        System.out.println("\nGrammar is " + (isLL1 ? "LL(1)" : "NOT LL(1)"));
    }

    // -----------------------------------------------------------------------
    // Save to file methods
    // -----------------------------------------------------------------------
    public void saveTransformedGrammar(String filename) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            pw.println("Transformed Grammar");
            pw.println("=".repeat(60));
            for (String nt : nonTerminals) {
                List<Production> prods = getProductionsFor(nt);
                if (prods.isEmpty()) continue;
                StringBuilder sb = new StringBuilder();
                sb.append(nt).append(" -> ");
                for (int i = 0; i < prods.size(); i++) {
                    if (i > 0) sb.append(" | ");
                    sb.append(String.join(" ", prods.get(i).rhs));
                }
                pw.println(sb.toString());
            }
        } catch (IOException e) {
            System.err.println("Error saving grammar: " + e.getMessage());
        }
    }

    public void saveFirstFollowSets(String filename) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            pw.println("FIRST Sets");
            pw.println("=".repeat(60));
            pw.printf("%-20s %s%n", "Non-Terminal", "FIRST Set");
            pw.println("-".repeat(60));
            for (String nt : nonTerminals) {
                Set<String> first = firstSets.getOrDefault(nt, new LinkedHashSet<>());
                pw.printf("%-20s { %s }%n", nt, String.join(", ", first));
            }
            pw.println();
            pw.println("FOLLOW Sets");
            pw.println("=".repeat(60));
            pw.printf("%-20s %s%n", "Non-Terminal", "FOLLOW Set");
            pw.println("-".repeat(60));
            for (String nt : nonTerminals) {
                Set<String> follow = followSets.getOrDefault(nt, new LinkedHashSet<>());
                pw.printf("%-20s { %s }%n", nt, String.join(", ", follow));
            }
        } catch (IOException e) {
            System.err.println("Error saving FIRST/FOLLOW: " + e.getMessage());
        }
    }

    public void saveParsingTable(String filename) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            pw.println("LL(1) Parsing Table");
            pw.println("=".repeat(80));

            List<String> cols = new ArrayList<>(terminals);
            cols.add("$");

            pw.printf("%-18s", "");
            for (String t : cols) pw.printf("| %-18s", t);
            pw.println("|");
            pw.println("-".repeat(18 + cols.size() * 20 + 1));

            for (String nt : nonTerminals) {
                pw.printf("%-18s", nt);
                Map<String, Integer> row = parsingTable.get(nt);
                for (String t : cols) {
                    int idx = (row != null && row.containsKey(t)) ? row.get(t) : -1;
                    String cell;
                    if (idx == -1) cell = "";
                    else if (idx == -2) cell = "CONFLICT";
                    else {
                        Production p = productions.get(idx);
                        cell = p.lhs + "->" + String.join(" ", p.rhs);
                    }
                    pw.printf("| %-18s", cell);
                }
                pw.println("|");
            }
            pw.println("\nGrammar is " + (isLL1 ? "LL(1)" : "NOT LL(1)"));
        } catch (IOException e) {
            System.err.println("Error saving parsing table: " + e.getMessage());
        }
    }
}
