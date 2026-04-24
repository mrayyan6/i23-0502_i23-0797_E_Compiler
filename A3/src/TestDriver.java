import java.io.*;
import java.util.*;

/**
 * TestDriver
 * -----------------------------------------------------------------------------
 * Part 9 of the assignment: runs a grammar + a set of input strings through
 * BOTH the SLR(1) and LR(1) pipelines and writes all the artefacts the
 * assignment deliverables section requires:
 *
 *     output/augmented_grammar.txt
 *     output/slr_items.txt
 *     output/slr_parsing_table.txt
 *     output/slr_trace.txt
 *     output/lr1_items.txt
 *     output/lr1_parsing_table.txt
 *     output/lr1_trace.txt
 *     output/comparison.txt
 *     output/parse_trees.txt
 *
 * Usage:
 *     java TestDriver <grammar.txt> <inputs.txt> <outputDir>
 *
 * `inputs.txt` is a plain text file with one input string per line.  Lines
 * beginning with '#' are ignored.  An empty line denotes the empty string.
 * -----------------------------------------------------------------------------
 */
public class TestDriver {

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: java TestDriver <grammar.txt> <inputs.txt> <outputDir>");
            System.exit(1);
        }
        String grammarFile = args[0];
        String inputsFile  = args[1];
        String outDir      = args[2];

        try {
            new File(outDir).mkdirs();
            runAll(grammarFile, inputsFile, outDir);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // =================================================================
    //                      Main pipeline
    // =================================================================
    public static void runAll(String grammarFile, String inputsFile, String outDir) throws IOException {
        System.out.println("======================================================");
        System.out.println(" Running SLR(1) + LR(1) on grammar: " + grammarFile);
        System.out.println("======================================================");

        // ---- Load grammar + FIRST/FOLLOW --------------------------------
        Grammar     grammar = new Grammar(grammarFile);
        FirstFollow ff      = new FirstFollow(grammar);

        // Dump augmented grammar
        try (PrintWriter pw = new PrintWriter(new FileWriter(outDir + "/augmented_grammar.txt"))) {
            pw.println("Augmented Grammar");
            pw.println("Start symbol: " + grammar.getStartSymbol()
                    + "   Augmented start: " + grammar.getAugmentedStart());
            pw.println("Productions (index: production):");
            List<String[]> prods = grammar.getProductions();
            for (int i = 0; i < prods.size(); i++) {
                pw.println("  " + i + ":  " + productionToString(prods.get(i)));
            }
        }

        // ---- SLR(1) pipeline --------------------------------------------
        long slrStart = System.nanoTime();
        SLRBuilder slr = new SLRBuilder(grammar, ff);
        long slrBuildNs = System.nanoTime() - slrStart;

        writeSlrItems(slr, outDir + "/slr_items.txt");
        writeTableToFile(grammar, slr.getActionTable(), slr.getGotoTable(),
                         slr.getConflicts(), "SLR(1)",
                         outDir + "/slr_parsing_table.txt",
                         slr.getCanonicalCollection().size());

        // ---- LR(1) pipeline ---------------------------------------------
        long lr1Start = System.nanoTime();
        LR1CollectionBuilder lr1coll = new LR1CollectionBuilder(grammar, ff);
        LR1Builder           lr1     = new LR1Builder(grammar, lr1coll);
        long lr1BuildNs = System.nanoTime() - lr1Start;

        lr1coll.saveStatesToFile(outDir + "/lr1_items.txt");
        lr1.saveTableToFile(outDir + "/lr1_parsing_table.txt");

        // ---- Read input strings -----------------------------------------
        List<String> inputs = readInputs(inputsFile);

        // ---- Run each input through both parsers ------------------------
        Parser slrParser = new Parser(grammar, slr.getActionTable(), slr.getGotoTable(), "SLR(1)");
        Parser lr1Parser = new Parser(grammar, lr1.getActionTable(), lr1.getGotoTable(), "LR(1)");

        BufferedWriter slrTrace  = new BufferedWriter(new FileWriter(outDir + "/slr_trace.txt"));
        BufferedWriter lr1Trace  = new BufferedWriter(new FileWriter(outDir + "/lr1_trace.txt"));
        BufferedWriter trees     = new BufferedWriter(new FileWriter(outDir + "/parse_trees.txt"));

        long slrParseNs = 0, lr1ParseNs = 0;
        int  slrAccepted = 0, lr1Accepted = 0;

        for (String inp : inputs) {
            String header = "INPUT: " + (inp.isEmpty() ? "(empty)" : inp);
            slrTrace.write(header); slrTrace.newLine();
            slrTrace.write("-".repeat(110)); slrTrace.newLine();
            lr1Trace.write(header); lr1Trace.newLine();
            lr1Trace.write("-".repeat(110)); lr1Trace.newLine();
            trees.write(header);    trees.newLine();
            trees.write("-".repeat(80)); trees.newLine();

            // SLR
            long t0 = System.nanoTime();
            boolean slrOk = slrParser.parse(inp);
            slrParseNs += System.nanoTime() - t0;
            if (slrOk) slrAccepted++;
            writeTraceBlock(slrTrace, slrParser);
            slrTrace.write(slrOk ? "Result: ACCEPTED" : "Result: REJECTED - " + slrParser.getErrorMessage());
            slrTrace.newLine(); slrTrace.newLine();

            // LR(1)
            long t1 = System.nanoTime();
            boolean lr1Ok = lr1Parser.parse(inp);
            lr1ParseNs += System.nanoTime() - t1;
            if (lr1Ok) lr1Accepted++;
            writeTraceBlock(lr1Trace, lr1Parser);
            lr1Trace.write(lr1Ok ? "Result: ACCEPTED" : "Result: REJECTED - " + lr1Parser.getErrorMessage());
            lr1Trace.newLine(); lr1Trace.newLine();

            // Parse trees
            trees.write("SLR(1) tree:"); trees.newLine();
            trees.write(slrOk && slrParser.getParseTree() != null
                    ? slrParser.getParseTree().render()
                    : "  (not accepted)\n");
            trees.write("LR(1)  tree:"); trees.newLine();
            trees.write(lr1Ok && lr1Parser.getParseTree() != null
                    ? lr1Parser.getParseTree().render()
                    : "  (not accepted)\n");
            trees.newLine();
        }
        slrTrace.close(); lr1Trace.close(); trees.close();

        // ---- Comparison report ------------------------------------------
        writeComparison(outDir + "/comparison.txt",
                grammarFile, inputs.size(),
                slr.getCanonicalCollection().size(),  slr.getConflicts(),
                lr1.getStates().size(),               lr1.getConflicts(),
                slrBuildNs, lr1BuildNs,
                slrParseNs, lr1ParseNs,
                slrAccepted, lr1Accepted,
                slr.getActionTable(), slr.getGotoTable(),
                lr1.getActionTable(), lr1.getGotoTable());

        // ---- Console summary --------------------------------------------
        System.out.println();
        System.out.println("SLR(1): " + slr.getCanonicalCollection().size() + " states, "
                + slr.getConflicts().size() + " conflicts, "
                + slrAccepted + "/" + inputs.size() + " accepted.");
        System.out.println("LR(1):  " + lr1.getStates().size() + " states, "
                + lr1.getConflicts().size() + " conflicts, "
                + lr1Accepted + "/" + inputs.size() + " accepted.");
        System.out.println("Outputs written to: " + outDir);
    }

    // =================================================================
    //                      File helpers
    // =================================================================
    private static List<String> readInputs(String path) throws IOException {
        List<String> out = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("#")) continue;   // comment
                out.add(line);                           // keep as-is (empty line = empty input)
            }
        }
        return out;
    }

    private static void writeSlrItems(SLRBuilder slr, String path) throws IOException {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(path))) {
            w.write("LR(0) / SLR(1) Canonical Collection");      w.newLine();
            w.write("Total states: " + slr.getCanonicalCollection().size()); w.newLine();
            w.write("=".repeat(60));                             w.newLine(); w.newLine();
            for (State s : slr.getCanonicalCollection()) {
                w.write(s.toString());
                Map<String, Integer> tr = s.getTransitions();
                if (!tr.isEmpty()) {
                    w.write("  Goto:"); w.newLine();
                    for (Map.Entry<String, Integer> e : tr.entrySet()) {
                        w.write("    " + e.getKey() + " -> State " + e.getValue()); w.newLine();
                    }
                }
                w.write("-".repeat(60)); w.newLine();
            }
        }
    }

    private static void writeTableToFile(Grammar grammar,
                                         Map<Integer, Map<String, String>>  action,
                                         Map<Integer, Map<String, Integer>> gotoT,
                                         List<String> conflicts, String name,
                                         String path, int numStates) throws IOException {
        Set<String> termCols = new LinkedHashSet<>(grammar.getTerminals());
        termCols.add("$");
        Set<String> ntCols = new LinkedHashSet<>(grammar.getNonTerminals());
        ntCols.remove(grammar.getAugmentedStart());

        try (BufferedWriter w = new BufferedWriter(new FileWriter(path))) {
            w.write(name + " Parsing Table"); w.newLine();
            w.write("Total states: " + numStates); w.newLine();
            w.write("=".repeat(80)); w.newLine(); w.newLine();

            StringBuilder header = new StringBuilder();
            header.append(String.format("%-8s", "State"));
            for (String t : termCols) header.append(String.format("%-20s", t));
            for (String nt : ntCols)  header.append(String.format("%-15s", nt));
            w.write(header.toString()); w.newLine();
            w.write("-".repeat(8 + termCols.size() * 20 + ntCols.size() * 15)); w.newLine();

            for (int id = 0; id < numStates; id++) {
                StringBuilder row = new StringBuilder();
                row.append(String.format("%-8d", id));
                Map<String, String>  a = action.getOrDefault(id, Collections.emptyMap());
                Map<String, Integer> g = gotoT.getOrDefault(id,  Collections.emptyMap());
                for (String t : termCols) row.append(String.format("%-20s", a.getOrDefault(t, "")));
                for (String nt : ntCols)  row.append(String.format("%-15s", g.containsKey(nt) ? g.get(nt) : ""));
                w.write(row.toString()); w.newLine();
            }

            w.newLine();
            if (conflicts.isEmpty()) {
                w.write("Grammar is " + name + " -- no conflicts detected."); w.newLine();
            } else {
                w.write(name + " Conflicts:"); w.newLine();
                for (String c : conflicts) { w.write("  " + c); w.newLine(); }
            }
        }
    }

    private static void writeTraceBlock(BufferedWriter w, Parser p) throws IOException {
        w.write(String.format("%-6s %-45s %-30s %s", "Step", "Stack", "Input", "Action"));
        w.newLine();
        w.write("-".repeat(110)); w.newLine();
        for (String[] r : p.getTraceRows()) {
            w.write(String.format("%-6s %-45s %-30s %s", r[0], r[1], r[2], r[3]));
            w.newLine();
        }
    }

    // =================================================================
    //                      Comparison / analysis
    // =================================================================
    private static void writeComparison(String path,
                                        String grammarFile, int numInputs,
                                        int slrStates, List<String> slrConflicts,
                                        int lr1States, List<String> lr1Conflicts,
                                        long slrBuildNs, long lr1BuildNs,
                                        long slrParseNs, long lr1ParseNs,
                                        int  slrAccepted, int lr1Accepted,
                                        Map<Integer, Map<String, String>>  slrAction,
                                        Map<Integer, Map<String, Integer>> slrGoto,
                                        Map<Integer, Map<String, String>>  lr1Action,
                                        Map<Integer, Map<String, Integer>> lr1Goto) throws IOException {

        int slrActionCells = countCells(slrAction);
        int slrGotoCells   = countCells(slrGoto);
        int lr1ActionCells = countCells(lr1Action);
        int lr1GotoCells   = countCells(lr1Goto);

        try (BufferedWriter w = new BufferedWriter(new FileWriter(path))) {
            w.write("SLR(1) vs LR(1) Comparison Report"); w.newLine();
            w.write("Grammar file : " + grammarFile); w.newLine();
            w.write("Inputs tested: " + numInputs);   w.newLine();
            w.write("=".repeat(70)); w.newLine(); w.newLine();

            w.write(String.format("%-32s %-18s %-18s", "Metric", "SLR(1)", "LR(1)"));     w.newLine();
            w.write("-".repeat(70)); w.newLine();
            w.write(String.format("%-32s %-18d %-18d", "States",          slrStates,      lr1States));      w.newLine();
            w.write(String.format("%-32s %-18d %-18d", "Non-empty ACTION cells", slrActionCells, lr1ActionCells)); w.newLine();
            w.write(String.format("%-32s %-18d %-18d", "Non-empty GOTO cells",   slrGotoCells,   lr1GotoCells));   w.newLine();
            w.write(String.format("%-32s %-18d %-18d", "Conflicts",        slrConflicts.size(), lr1Conflicts.size())); w.newLine();
            w.write(String.format("%-32s %-18s %-18s", "Table build time (ms)",
                    String.format("%.3f", slrBuildNs / 1_000_000.0),
                    String.format("%.3f", lr1BuildNs / 1_000_000.0))); w.newLine();
            w.write(String.format("%-32s %-18s %-18s", "Total parse time (ms)",
                    String.format("%.3f", slrParseNs / 1_000_000.0),
                    String.format("%.3f", lr1ParseNs / 1_000_000.0))); w.newLine();
            w.write(String.format("%-32s %-18s %-18s", "Accepted inputs",
                    slrAccepted + "/" + numInputs,
                    lr1Accepted + "/" + numInputs)); w.newLine();
            w.newLine();

            // ---- Conflict breakdown -------------------------------------
            w.write("----- SLR(1) Conflicts -----"); w.newLine();
            if (slrConflicts.isEmpty()) {
                w.write("  (none)"); w.newLine();
            } else {
                for (String c : slrConflicts) { w.write("  " + c); w.newLine(); }
            }
            w.newLine();

            w.write("----- LR(1) Conflicts -----"); w.newLine();
            if (lr1Conflicts.isEmpty()) {
                w.write("  (none)"); w.newLine();
            } else {
                for (String c : lr1Conflicts) { w.write("  " + c); w.newLine(); }
            }
            w.newLine();

            // ---- Verdict -------------------------------------------------
            w.write("----- Verdict -----"); w.newLine();
            if (slrConflicts.isEmpty() && lr1Conflicts.isEmpty()) {
                w.write("Both parsers handle this grammar without conflicts."); w.newLine();
                w.write("LR(1) uses more states (" + lr1States + " vs " + slrStates
                        + ") for the same language, which illustrates its space cost."); w.newLine();
            } else if (!slrConflicts.isEmpty() && lr1Conflicts.isEmpty()) {
                w.write("SLR(1) has " + slrConflicts.size()
                        + " conflict(s) but LR(1) has none."); w.newLine();
                w.write("This grammar is LR(1) but NOT SLR(1) -- a direct demonstration "
                        + "that LR(1) is strictly more powerful.  SLR(1) over-approximates "
                        + "reduction lookaheads with FOLLOW(A), which here includes symbols "
                        + "that are not valid lookaheads in the specific state.  LR(1) "
                        + "tracks the actual valid lookahead per item and so avoids the "
                        + "spurious conflict."); w.newLine();
            } else if (!slrConflicts.isEmpty() && !lr1Conflicts.isEmpty()) {
                w.write("Both parsers report conflicts, so the grammar is not even LR(1)."); w.newLine();
                w.write("It would need to be rewritten or handled with LALR+heuristics / GLR."); w.newLine();
            } else {
                w.write("Unexpected: SLR(1) has no conflicts but LR(1) does. "
                        + "Check the collection builder."); w.newLine();
            }
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