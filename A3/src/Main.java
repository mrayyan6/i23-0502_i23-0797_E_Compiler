import java.io.*;
import java.util.*;

public class Main {

    private static final String INPUT_DIR      = "input";
    private static final String OUTPUT_DIR     = "output";
    private static final String OUTPUT_SLR_DIR = "output/slr";
    private static final String OUTPUT_LR1_DIR = "output/lr1";

    private static final String GRAMMAR_FILE = INPUT_DIR + "/grammar.txt";
    private static final String STRINGS_FILE = INPUT_DIR + "/strings.txt";

    private static final String AUGMENTED_GRAMMAR_FILE = OUTPUT_DIR     + "/augmented_grammar.txt";
    private static final String COMPARISON_FILE        = OUTPUT_DIR     + "/comparison.txt";
    private static final String PARSE_TREES_FILE       = OUTPUT_DIR     + "/parse_trees.txt";
    private static final String SLR_STATES_FILE        = OUTPUT_SLR_DIR + "/lr0_states.txt";
    private static final String SLR_TABLE_FILE         = OUTPUT_SLR_DIR + "/slr_table.txt";
    private static final String SLR_TRACE_FILE         = OUTPUT_SLR_DIR + "/slr_trace.txt";
    private static final String LR1_STATES_FILE        = OUTPUT_LR1_DIR + "/lr1_states.txt";
    private static final String LR1_TABLE_FILE         = OUTPUT_LR1_DIR + "/lr1_table.txt";
    private static final String LR1_TRACE_FILE         = OUTPUT_LR1_DIR + "/lr1_trace.txt";

    public static void main(String[] args) {
        try {
            new File(OUTPUT_DIR).mkdirs();
            new File(OUTPUT_SLR_DIR).mkdirs();
            new File(OUTPUT_LR1_DIR).mkdirs();
            runPipeline();
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void runPipeline() throws IOException {
        System.out.println("=".repeat(54));
        System.out.println(" SLR(1) + LR(1) Parser");
        System.out.println(" Grammar : " + GRAMMAR_FILE);
        System.out.println(" Strings : " + STRINGS_FILE);
        System.out.println("=".repeat(54));

        Grammar grammar = new Grammar(GRAMMAR_FILE);
        FirstFollow ff  = new FirstFollow(grammar);

        try (PrintWriter pw = new PrintWriter(new FileWriter(AUGMENTED_GRAMMAR_FILE))) {
            writeAugmentedGrammar(pw, grammar);
        }

        long slrBuildStart = System.nanoTime();
        SLRBuilder slr = new SLRBuilder(grammar, ff);
        long slrBuildNs = System.nanoTime() - slrBuildStart;

        try (PrintWriter pw = new PrintWriter(new FileWriter(SLR_STATES_FILE))) {
            slr.saveStates(pw);
        }
        try (PrintWriter pw = new PrintWriter(new FileWriter(SLR_TABLE_FILE))) {
            slr.saveTable(pw);
        }

        long lr1BuildStart = System.nanoTime();
        LR1CollectionBuilder lr1Coll = new LR1CollectionBuilder(grammar, ff);
        LR1Builder lr1 = new LR1Builder(grammar, lr1Coll);
        long lr1BuildNs = System.nanoTime() - lr1BuildStart;

        try (PrintWriter pw = new PrintWriter(new FileWriter(LR1_STATES_FILE))) {
            lr1Coll.saveStates(pw);
        }
        try (PrintWriter pw = new PrintWriter(new FileWriter(LR1_TABLE_FILE))) {
            lr1.saveTable(pw);
        }

        List<String> inputs = readInputStrings(STRINGS_FILE);

        Parser slrParser = new Parser(grammar, slr.getActionTable(), slr.getGotoTable(), "SLR(1)");
        Parser lr1Parser = new Parser(grammar, lr1.getActionTable(), lr1.getGotoTable(), "LR(1)");

        long slrParseNs = 0, lr1ParseNs = 0;
        int  slrAccepted = 0, lr1Accepted = 0;

        try (PrintWriter slrTrace = new PrintWriter(new FileWriter(SLR_TRACE_FILE));
             PrintWriter lr1Trace = new PrintWriter(new FileWriter(LR1_TRACE_FILE));
             PrintWriter trees    = new PrintWriter(new FileWriter(PARSE_TREES_FILE))) {

            for (String input : inputs) {
                long t0 = System.nanoTime();
                boolean slrOk = slrParser.parse(input);
                slrParseNs += System.nanoTime() - t0;
                if (slrOk) slrAccepted++;
                slrParser.saveTrace(slrTrace, input);

                long t1 = System.nanoTime();
                boolean lr1Ok = lr1Parser.parse(input);
                lr1ParseNs += System.nanoTime() - t1;
                if (lr1Ok) lr1Accepted++;
                lr1Parser.saveTrace(lr1Trace, input);

                String inputLabel = "INPUT: " + (input.isEmpty() ? "(empty)" : input);
                trees.println(inputLabel);
                trees.println("-".repeat(80));
                trees.println("SLR(1) tree:");
                trees.println(slrOk && slrParser.getParseTree() != null
                        ? slrParser.getParseTree().render() : "  (not accepted)");
                trees.println("LR(1)  tree:");
                trees.println(lr1Ok && lr1Parser.getParseTree() != null
                        ? lr1Parser.getParseTree().render() : "  (not accepted)");
                trees.println();
            }
        }

        try (PrintWriter pw = new PrintWriter(new FileWriter(COMPARISON_FILE))) {
            writeComparison(pw, inputs.size(),
                    slr.getCanonicalCollection().size(), slr.getConflicts(),
                    lr1.getStates().size(),              lr1.getConflicts(),
                    slrBuildNs, lr1BuildNs, slrParseNs, lr1ParseNs,
                    slrAccepted, lr1Accepted,
                    slr.getActionTable(), slr.getGotoTable(),
                    lr1.getActionTable(), lr1.getGotoTable());
        }

        System.out.println();
        System.out.println("SLR(1): " + slr.getCanonicalCollection().size() + " states, "
                + slr.getConflicts().size() + " conflicts, "
                + slrAccepted + "/" + inputs.size() + " accepted.");
        System.out.println("LR(1):  " + lr1.getStates().size() + " states, "
                + lr1.getConflicts().size() + " conflicts, "
                + lr1Accepted + "/" + inputs.size() + " accepted.");
        System.out.println("Outputs written to: " + OUTPUT_DIR + "/");
    }

    private static List<String> readInputStrings(String path) throws IOException {
        List<String> result = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().startsWith("#")) result.add(line);
            }
        }
        return result;
    }

    private static void writeAugmentedGrammar(PrintWriter pw, Grammar grammar) {
        pw.println("Augmented Grammar");
        pw.println("Start symbol    : " + grammar.getStartSymbol());
        pw.println("Augmented start : " + grammar.getAugmentedStart());
        pw.println("Non-terminals   : " + grammar.getNonTerminals());
        pw.println("Terminals       : " + grammar.getTerminals());
        pw.println("Productions (index: rule):");
        List<String[]> prods = grammar.getProductions();
        for (int i = 0; i < prods.size(); i++) {
            pw.println("  " + i + ":  " + productionToString(prods.get(i)));
        }
    }

    private static void writeComparison(PrintWriter pw, int numInputs,
                                        int slrStates, List<String> slrConflicts,
                                        int lr1States, List<String> lr1Conflicts,
                                        long slrBuildNs, long lr1BuildNs,
                                        long slrParseNs, long lr1ParseNs,
                                        int slrAccepted, int lr1Accepted,
                                        Map<Integer, Map<String, String>>  slrAction,
                                        Map<Integer, Map<String, Integer>> slrGoto,
                                        Map<Integer, Map<String, String>>  lr1Action,
                                        Map<Integer, Map<String, Integer>> lr1Goto) {

        int slrActionCells = countCells(slrAction);
        int slrGotoCells   = countCells(slrGoto);
        int lr1ActionCells = countCells(lr1Action);
        int lr1GotoCells   = countCells(lr1Goto);

        pw.println("SLR(1) vs LR(1) Comparison Report");
        pw.println("Grammar file : " + GRAMMAR_FILE);
        pw.println("Inputs tested: " + numInputs);
        pw.println("=".repeat(70));
        pw.println();

        pw.printf("%-32s %-18s %-18s%n", "Metric", "SLR(1)", "LR(1)");
        pw.println("-".repeat(70));
        pw.printf("%-32s %-18d %-18d%n", "States",                 slrStates,              lr1States);
        pw.printf("%-32s %-18d %-18d%n", "Non-empty ACTION cells", slrActionCells,         lr1ActionCells);
        pw.printf("%-32s %-18d %-18d%n", "Non-empty GOTO cells",   slrGotoCells,           lr1GotoCells);
        pw.printf("%-32s %-18d %-18d%n", "Conflicts",              slrConflicts.size(),    lr1Conflicts.size());
        pw.printf("%-32s %-18s %-18s%n", "Table build time (ms)",
                String.format("%.3f", slrBuildNs / 1_000_000.0),
                String.format("%.3f", lr1BuildNs / 1_000_000.0));
        pw.printf("%-32s %-18s %-18s%n", "Total parse time (ms)",
                String.format("%.3f", slrParseNs / 1_000_000.0),
                String.format("%.3f", lr1ParseNs / 1_000_000.0));
        pw.printf("%-32s %-18s %-18s%n", "Accepted inputs",
                slrAccepted + "/" + numInputs, lr1Accepted + "/" + numInputs);
        pw.println();

        pw.println("----- SLR(1) Conflicts -----");
        if (slrConflicts.isEmpty()) {
            pw.println("  (none)");
        } else {
            for (String c : slrConflicts) pw.println("  " + c);
        }
        pw.println();

        pw.println("----- LR(1) Conflicts -----");
        if (lr1Conflicts.isEmpty()) {
            pw.println("  (none)");
        } else {
            for (String c : lr1Conflicts) pw.println("  " + c);
        }
        pw.println();

        pw.println("----- Verdict -----");
        if (slrConflicts.isEmpty() && lr1Conflicts.isEmpty()) {
            pw.println("Both parsers handle this grammar without conflicts.");
            pw.println("LR(1) uses more states (" + lr1States + " vs " + slrStates
                    + ") for the same language, illustrating its space cost.");
        } else if (!slrConflicts.isEmpty() && lr1Conflicts.isEmpty()) {
            pw.println("SLR(1) has " + slrConflicts.size() + " conflict(s) but LR(1) has none.");
            pw.println("This grammar is LR(1) but NOT SLR(1) -- a direct demonstration"
                    + " that LR(1) is strictly more powerful.");
        } else if (!slrConflicts.isEmpty() && !lr1Conflicts.isEmpty()) {
            pw.println("Both parsers report conflicts; the grammar is not even LR(1).");
        } else {
            pw.println("Unexpected: SLR(1) has no conflicts but LR(1) does.");
        }
    }

    private static int countCells(Map<Integer, ? extends Map<?, ?>> table) {
        int n = 0;
        for (Map<?, ?> row : table.values()) n += row.size();
        return n;
    }

    private static String productionToString(String[] p) {
        StringBuilder sb = new StringBuilder(p[0]).append(" ->");
        for (int i = 1; i < p.length; i++) sb.append(' ').append(p[i]);
        return sb.toString();
    }
}
