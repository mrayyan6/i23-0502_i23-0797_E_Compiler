import java.io.*;
import java.util.*;

public class Main
{
    public static void main(String[] args)
    {
        File cwd = new File(System.getProperty("user.dir"));
        File projectRoot = cwd.getName().equalsIgnoreCase("src") ? cwd.getParentFile() : cwd;

        String grammarFile = new File(projectRoot, "input/grammar1.txt").getPath();
        String inputFile = new File(projectRoot, "input/input_valid.txt").getPath();
        String outputDir = new File(projectRoot, "output").getPath();

        if (args.length >= 2)
        {
            grammarFile = args[0];
            inputFile = args[1];
        }
        else if (args.length == 1)
        {
            grammarFile = args[0];
        }

        System.out.println("===========================================");
        System.out.println(" LL(1) Predictive Parser - Part 1 + Part 2");
        System.out.println("===========================================");
        System.out.println("Grammar file: " + grammarFile);
        System.out.println("Input file:   " + inputFile);
        System.out.println();

        try
        {
            System.out.println("[1] Parsing grammar from file...");
            Grammar g = new Grammar(grammarFile);
            System.out.println("    Original Grammar:");
            System.out.println(indent(g.toString()));

            System.out.println("[2] Applying Left Factoring...");
            g.leftFactor();
            System.out.println("    Grammar after Left Factoring:");
            System.out.println(indent(g.toString()));

            System.out.println("[3] Removing Left Recursion (direct + indirect)...");
            g.removeLeftRec();
            System.out.println("    Grammar after Left Recursion Removal:");
            System.out.println(indent(g.toString()));

            String transformedPath = new File(outputDir, "grammar_transformed.txt").getPath();
            g.writeFile(transformedPath);
            System.out.println("    -> Transformed grammar written to: " + transformedPath);
            System.out.println();

            System.out.println("[4] Computing FIRST and FOLLOW sets...");
            FirstFollow ff = new FirstFollow(g);
            System.out.println(indent(ff.toString()));

            String firstFollowPath = new File(outputDir, "first_follow_sets.txt").getPath();
            writeFile(firstFollowPath, ff.toString());
            System.out.println("    -> FIRST/FOLLOW sets written to: " + firstFollowPath);
            System.out.println();

            System.out.println("[5] Building LL(1) Parsing Table...");
            Parser parser = new Parser(g, ff);
            System.out.println(indent(parser.toString()));

            String tablePath = new File(outputDir, "parsing_table.txt").getPath();
            parser.writeFile(tablePath);
            System.out.println("    -> Parsing table written to: " + tablePath);
            System.out.println();

            System.out.println("===========================================");
            if (parser.hasConflict())
            {
                System.out.println(" Result: Grammar is NOT LL(1).");
                System.out.println(" See parsing_table.txt for conflict details.");
            }
            else
            {
                System.out.println(" Result: Grammar IS LL(1).");
            }
            System.out.println("===========================================");

            System.out.println("\n" + "=".repeat(80));
            System.out.println("PART 2: STACK-BASED PARSING");
            System.out.println("=".repeat(80));

            File inFile = new File(inputFile);
            if (!inFile.exists())
            {
                System.err.println("Input file not found: " + inputFile);
                System.exit(1);
            }

            List<Parser.ParseResult> results = parser.parseFile(inputFile);
            parser.saveTraces(results, new File(outputDir, "parsing_trace1.txt").getPath());
            parser.saveParseTrees(results, new File(outputDir, "parse_trees.txt").getPath());

            int accepted = 0;
            int rejected = 0;
            for (Parser.ParseResult r : results)
            {
                if (r.accepted)
                {
                    accepted++;
                }
                else
                {
                    rejected++;
                }
            }

            System.out.println("\n" + "=".repeat(60));
            System.out.println("PARSING SUMMARY");
            System.out.println("=".repeat(60));
            System.out.println("  Total: " + results.size() + " | Accepted: " + accepted + " | Rejected: " + rejected);
            System.out.println("\nOutput files saved to output/ directory:");
            System.out.println("  - grammar_transformed.txt");
            System.out.println("  - first_follow_sets.txt");
            System.out.println("  - parsing_table.txt");
            System.out.println("  - parsing_trace1.txt");
            System.out.println("  - parse_trees.txt");
        }
        catch (IOException e)
        {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(1);
        }
    }

    private static String indent(String text)
    {
        StringBuilder out = new StringBuilder();
        for (String line : text.split("\n"))
        {
            out.append("    ").append(line).append("\n");
        }
        return out.toString();
    }

    private static void writeFile(String path, String txt) throws IOException
    {
        File f = new File(path);
        f.getParentFile().mkdirs();
        BufferedWriter bw = new BufferedWriter(new FileWriter(path));
        bw.write(txt);
        bw.close();
    }
}
