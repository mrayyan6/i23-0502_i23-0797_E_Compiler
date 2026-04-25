import java.io.*;
import java.util.*;

public class Main1 {

    private static final String GRAMMAR_FILE = "input/grammar_conflict.txt";
    private static final String STRINGS_FILE = "input/test_strings.txt";
    private static final String OUTPUT_DIR   = "output";
    private static final String LOG_FILE     = OUTPUT_DIR + "/comparison_log.txt";

    private static final int COL_STRING = 35;
    private static final int COL_RESULT = 12;
    private static final int COL_MATCH  = 10;

    public static void main(String[] args) {
        try {
            new File(OUTPUT_DIR).mkdirs();
            runComparison();
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void runComparison() throws IOException {
        Grammar      grammar = new Grammar(GRAMMAR_FILE);
        FirstFollow  ff      = new FirstFollow(grammar);

        SLRBuilder           slr  = new SLRBuilder(grammar, ff);
        LR1CollectionBuilder coll = new LR1CollectionBuilder(grammar, ff);
        LR1Builder           lr1  = new LR1Builder(grammar, coll);

        Parser slrParser = new Parser(grammar, slr.getActionTable(), slr.getGotoTable(), "SLR(1)");
        Parser lr1Parser = new Parser(grammar, lr1.getActionTable(), lr1.getGotoTable(), "LR(1)");

        List<String> testStrings = readLines(STRINGS_FILE);

        String separator = buildSeparator();
        String header    = buildRow("Test String", "SLR Result", "LR1 Result", "Match?");

        try (PrintWriter log = new PrintWriter(new FileWriter(LOG_FILE))) {
            emit("SLR(1) vs LR(1) Automated Comparison", log);
            emit("Grammar : " + GRAMMAR_FILE, log);
            emit("Strings : " + STRINGS_FILE, log);
            emit("", log);
            emit(separator, log);
            emit(header, log);
            emit(separator, log);

            int matched = 0, mismatched = 0;

            for (String input : testStrings) {
                slrParser.parse(input);
                lr1Parser.parse(input);

                String  slrResult = slrParser.isAccepted() ? "ACCEPTED" : "REJECTED";
                String  lr1Result = lr1Parser.isAccepted() ? "ACCEPTED" : "REJECTED";
                boolean match     = slrParser.isAccepted() == lr1Parser.isAccepted();
                String  matchStr  = match ? "YES" : "NO !!!";

                String displayInput = input.isEmpty() ? "(empty)" : input;
                emit(buildRow(displayInput, slrResult, lr1Result, matchStr), log);

                if (!match) {
                    String slrDetail = slrParser.isAccepted()
                            ? "accepted"
                            : "rejected ('" + slrParser.getErrorMessage() + "')";
                    String lr1Detail = lr1Parser.isAccepted()
                            ? "accepted"
                            : "rejected ('" + lr1Parser.getErrorMessage() + "')";
                    emit("  >>> MISMATCH: SLR(1) " + slrDetail
                            + " but LR(1) " + lr1Detail
                            + "  |  input: [" + displayInput + "]", log);
                    mismatched++;
                } else {
                    matched++;
                }
            }

            emit(separator, log);
            emit(String.format(
                    "  Total: %d  |  Matched: %d  |  Mismatched: %d  |  SLR conflicts: %d  |  LR1 conflicts: %d",
                    testStrings.size(), matched, mismatched,
                    slr.getConflicts().size(), lr1.getConflicts().size()), log);
            emit(separator, log);

            if (!slr.getConflicts().isEmpty()) {
                emit("\nSLR(1) Table Conflicts:", log);
                for (String c : slr.getConflicts()) emit("  " + c, log);
            }
            if (!lr1.getConflicts().isEmpty()) {
                emit("\nLR(1) Table Conflicts:", log);
                for (String c : lr1.getConflicts()) emit("  " + c, log);
            }
        }

        System.out.println("\nComparison log written to: " + LOG_FILE);
    }

    private static void emit(String line, PrintWriter pw) {
        System.out.println(line);
        pw.println(line);
    }

    private static String buildRow(String c1, String c2, String c3, String c4) {
        return String.format("| %-" + (COL_STRING - 2) + "s | %-" + (COL_RESULT - 2) + "s | %-"
                        + (COL_RESULT - 2) + "s | %-" + (COL_MATCH - 2) + "s |",
                truncate(c1, COL_STRING - 2),
                truncate(c2, COL_RESULT - 2),
                truncate(c3, COL_RESULT - 2),
                truncate(c4, COL_MATCH - 2));
    }

    private static String buildSeparator() {
        return "+" + "-".repeat(COL_STRING) + "+"
                + "-".repeat(COL_RESULT) + "+"
                + "-".repeat(COL_RESULT) + "+"
                + "-".repeat(COL_MATCH)  + "+";
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }

    private static List<String> readLines(String path) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.startsWith("#")) lines.add(line);
            }
        }
        return lines;
    }
}
