import java.io.*;

public class Main 
{
    public static void main(String[] args) 
    {
        String inFile; // inputFile
        if (args.length > 0) 
        {
            inFile = args[0];
        } 
        else 
        {
            inFile = "input/grammar1.txt";
        }

        System.out.println("===========================================");
        System.out.println(" LL(1) Predictive Parser — Part 1");
        System.out.println("===========================================");
        System.out.println("Input file: " + inFile);
        System.out.println();

        try 
        {
            System.out.println("[1] Parsing grammar from file...");
            Grammar g = new Grammar(inFile); // grammar
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

            String gPath = "output/grammar_transformed.txt"; // transformedPath
            g.writeFile(gPath);
            System.out.println("    -> Transformed grammar written to: " + gPath);
            System.out.println();

            System.out.println("[4] Computing FIRST and FOLLOW sets...");
            FirstFollow ff = new FirstFollow(g); // firstFollow
            System.out.println(indent(ff.toString()));

            String ffPath = "output/first_follow_sets.txt"; // firstFollowPath
            writeFile(ffPath, ff.toString());
            System.out.println("    -> FIRST/FOLLOW sets written to: " + ffPath);
            System.out.println();

            System.out.println("[5] Building LL(1) Parsing Table...");
            Parser p = new Parser(g, ff); // parser
            System.out.println(indent(p.toString()));

            String tPath = "output/parsing_table.txt"; // tablePath
            p.writeFile(tPath);
            System.out.println("    -> Parsing table written to: " + tPath);
            System.out.println();

            System.out.println("===========================================");
            if (p.hasConflict()) 
            {
                System.out.println(" Result: Grammar is NOT LL(1).");
                System.out.println(" See parsing_table.txt for conflict details.");
            } 
            else 
            {
                System.out.println(" Result: Grammar IS LL(1).");
            }
            System.out.println("===========================================");

        } 
        catch (IOException e) 
        {
            System.err.println("ERROR: Could not read file '" + inFile + "'");
            System.err.println("       " + e.getMessage());
            System.exit(1);
        }
    }

    private static String indent(String text) 
    {
        StringBuilder out = new StringBuilder(); // sb = string builder
        for (String line : text.split("\n")) 
        {
            out.append("    ").append(line).append("\n");
        }
        return out.toString();
    }

    private static void writeFile(String path, String txt) throws IOException 
    {
        File f = new File(path); // file
        f.getParentFile().mkdirs();
        BufferedWriter bw = new BufferedWriter(new FileWriter(path)); // writer
        bw.write(txt);
        bw.close();
    }
}
