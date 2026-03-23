
import java.io.IOException;
import java.nio.file.Files;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;


public class TestRunner 
{

    private static final String[] FILES = {
        "tests/test1.lang",
        "tests/test2.lang",
        "tests/test3.lang",
        "tests/test4.lang",
        "tests/test5.lang"
    };
 private static final String OUT_FILE = "TestResults.txt";
    public static void main(String[] args) 
    {
        PrintStream file = null;

        try 
        {
            file = new PrintStream(new FileOutputStream(OUT_FILE));
        } 
        catch (IOException e) 
        {
            System.err.println("cant open " + OUT_FILE);
            System.exit(1);
        }
        
        for (String path : FILES) 
        {
            String hdr = "--- Running " + path + " ---";
            both(System.out, file, "\n" + hdr);

            String src;
            try 
            {
                src = new String(Files.readAllBytes(Paths.get(path)));
            } 
            catch (IOException e) 
            {
                String msg = "  ERROR: cant read " + path + " - " + e.getMessage();
                both(System.out, file, msg);
                both(System.out, file, "");
                continue;
            }

            try
            {
            ManualScanner1 s2 = new ManualScanner1(src);
            List<Token> tokens = s2.all();

                both(System.out, file, "||============================================================||");
                both(System.out, file, "||                        TOKEN STREAM                        ||");
                both(System.out, file, "||============================================================||");

                for (Token t : tokens) 
                {
                    both(System.out, file, "  " + t);
                }
                both(System.out, file, "||============================================================||");
                both(System.out, file, String.format("||  Total tokens: %-45d||", tokens.size()));
                both(System.out, file, "||============================================================||");

                both(System.out, file, "");
                both(System.out, file, "||============================================================||");
                both(System.out, file, "||                        SYMBOL TABLE                        ||");
                both(System.out, file, "||============================================================||");
                both(System.out, file, String.format("|| %-20s %-12s %-12s %-6s ||", "Name", "Type", "Scope", "Line"));
                both(System.out, file, "||============================================================||");

                List<Token> ids = tokens.stream()
                    .filter(t -> t.type() == TokenType.IDENTIFIER)
                    .collect(Collectors.toList());

                if (ids.isEmpty()) 
                {
                    both(System.out, file, "||                     (empty table)                          ||");
                } 
                else 
                {
                    for (Token t : ids) 
                    {
                        both(System.out, file, String.format("|| %-20s %-12s %-12s %-6d ||",
                                t.lex(), "N/A", "Global", t.ln()));
                    }
                }
                both(System.out, file, "||============================================================||");

                // errors
                both(System.out, file, "");
                List<Token> errTokens = tokens.stream()
                    .filter(t -> t.type() == TokenType.ERROR)
                    .collect(Collectors.toList());

                if (errTokens.isEmpty()) 
                {
                    both(System.out, file, "[ErrorHandler] No lexical errors detected.");
                } 
                else 
                {
                    both(System.out, file, "||============================================================||");
                    both(System.out, file, "||                     LEXICAL ERRORS                         ||");
                    both(System.out, file, "||============================================================||");

                    for (Token e : errTokens) 
                    {
                        both(System.out, file, "||  " + e);
                    }

                    both(System.out, file, "||============================================================||");
                    both(System.out, file, String.format("||  Total errors: %-45d||", errTokens.size()));
                    both(System.out, file, "||============================================================||");
                }
            } 
            catch (Exception e) 
            {
                String msg = "  EXCEPTION on " + path + ": " + e.getMessage();
                both(System.out, file, msg);
                e.printStackTrace(file);
            }

            both(System.out, file, "");
        }

        both(System.out, file, "=== ALL TESTS COMPLETE ===");
        both(System.out, file, "Results written to " + OUT_FILE);

        file.close();
    
    }

    private static void both(PrintStream console, PrintStream file, String line) 
    {
        console.println(line);
        file.println(line);
    }
}
