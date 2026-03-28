import java.io.*;
import java.util.*;

/**
 * Main.java
 * Entry point for the LL(1) Parser.
 * 
 * Usage:
 *   java Main <grammar_file> <input_file>
 *   java Main                                (uses default files)
 * 
 * The program:
 *   1. Reads a CFG from a file
 *   2. Applies left factoring and left recursion removal
 *   3. Computes FIRST and FOLLOW sets
 *   4. Builds the LL(1) parsing table
 *   5. Parses input strings with step-by-step traces
 *   6. Handles errors with panic mode recovery
 *   7. Generates parse trees for accepted strings
 */
public class Main {

    public static void main(String[] args) {
        String grammarFile = "input/grammar1.txt";
        String inputFile = "input/input_valid.txt";

        if (args.length >= 2) {
            grammarFile = args[0];
            inputFile = args[1];
        } else if (args.length == 1) {
            grammarFile = args[0];
        }

        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║           LL(1) Predictive Parser                       ║");
        System.out.println("║           CS4031 - Compiler Construction                ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("Grammar file: " + grammarFile);
        System.out.println("Input file:   " + inputFile);

        // ---------------------------------------------------------------
        // Part 1: Grammar Transformation & Table Construction
        // ---------------------------------------------------------------
        Grammar grammar = new Grammar();
        if (!grammar.readFromFile(grammarFile)) {
            System.err.println("Failed to read grammar file: " + grammarFile);
            System.exit(1);
        }

        grammar.printGrammar("Original Grammar");

        // Left Factoring
        grammar.applyLeftFactoring();
        grammar.printGrammar("After Left Factoring");

        // Left Recursion Removal
        grammar.removeLeftRecursion();
        grammar.printGrammar("After Left Recursion Removal");

        // FIRST and FOLLOW
        FirstFollow ff = new FirstFollow(grammar);
        ff.computeAll();
        ff.printFirstSets();
        ff.printFollowSets();

        // Parsing Table
        grammar.buildParsingTable();
        grammar.printParsingTable();

        // Check if grammar is LL(1)
        if (!grammar.isLL1) {
            System.out.println("\nWARNING: Grammar is NOT LL(1). Parsing may produce incorrect results.");
            System.out.println("Conflicts exist in the parsing table.");
        }

        // Save Part 1 outputs
        new File("output").mkdirs();
        grammar.saveTransformedGrammar("output/grammar_transformed.txt");
        grammar.saveFirstFollowSets("output/first_follow_sets.txt");
        grammar.saveParsingTable("output/parsing_table.txt");

        // ---------------------------------------------------------------
        // Part 2: Stack-Based Parser
        // ---------------------------------------------------------------
        System.out.println("\n" + "═".repeat(80));
        System.out.println("PART 2: STACK-BASED PARSING");
        System.out.println("═".repeat(80));

        Parser parser = new Parser(grammar);

        // Check if input file exists
        File inFile = new File(inputFile);
        if (!inFile.exists()) {
            System.err.println("Input file not found: " + inputFile);
            System.out.println("Create an input file with one string per line.");
            System.exit(1);
        }

        List<Parser.ParseResult> results = parser.parseFile(inputFile);

        // Save outputs
        parser.saveTraces(results, "output/parsing_trace1.txt");
        parser.saveParseTrees(results, "output/parse_trees.txt");

        // Summary
        System.out.println("\n" + "═".repeat(60));
        System.out.println("PARSING SUMMARY");
        System.out.println("═".repeat(60));
        int accepted = 0, rejected = 0;
        for (Parser.ParseResult r : results) {
            String status = r.accepted ? "ACCEPTED" : "REJECTED";
            System.out.printf("  %-30s  %s", r.inputString, status);
            if (!r.accepted) {
                System.out.printf(" (%d error(s))", r.errorCount);
            }
            System.out.println();
            if (r.accepted) accepted++;
            else rejected++;
        }
        System.out.println("-".repeat(60));
        System.out.println("  Total: " + results.size() + " | Accepted: " + accepted +
                           " | Rejected: " + rejected);

        System.out.println("\nOutput files saved to output/ directory:");
        System.out.println("  - grammar_transformed.txt");
        System.out.println("  - first_follow_sets.txt");
        System.out.println("  - parsing_table.txt");
        System.out.println("  - parsing_trace1.txt");
        System.out.println("  - parse_trees.txt");
    }
}
