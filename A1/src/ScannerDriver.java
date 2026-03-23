import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ScannerDriver 
{

    /*
     * A PrintStream that writes to two underlying OutputStreams simultaneously.
     * Used to send output to both console and file at the same time.
     */
    private static class TeeStream extends PrintStream 
    {
        private final PrintStream second;

        public TeeStream(PrintStream main, PrintStream second) 
        {
            super(main, true);
            this.second = second;
        }

        @Override
        public void write(byte[] buf, int off, int len) 
        {
            super.write(buf, off, len);
            second.write(buf, off, len);
            second.flush();
        }

        @Override
        public void write(int b) 
        {
            super.write(b);
            second.write(b);
            second.flush();
        }

        @Override
        public void flush() 
        {
            super.flush();
            second.flush();
        }

        @Override
        public void close() 
        {
            super.close();
            second.close();
        }
    }


    private static void scanFile(String filePath) 
    {
        System.out.println("\n--- Running " + filePath + " ---");

        String source;
        try 
        {
            source = new String(Files.readAllBytes(Paths.get(filePath)));
        } 
        catch (IOException e) 
        {
            System.out.println("Error: Could not read file: " + filePath);
            System.out.println(e.getMessage());
            return;
        }

        try 
        {
            Yylex scanner = new Yylex(new StringReader(source));
            List<Token> tokens = new ArrayList<>();

            Token t;
            do 
            {
                t = scanner.yylex();
                tokens.add(t);
            } while (t.type() != TokenType.EOF);

            // ----- Token Stream -----
            System.out.println("||============================================================|| ");
            System.out.println("||                        TOKEN STREAM                        || ");
            System.out.println("||============================================================|| ");

            for (Token tok : tokens) 
            {
                System.out.println("  " + tok);
            }

            System.out.println("||============================================================|| ");
            System.out.printf("||  Total tokens: %-45d\u2551%n", tokens.size());
            System.out.println("||============================================================|| ");

            // ----- Symbol Table (uses System.out internally
            scanner.syms().show();

            // ----- Errors (uses System.err internally
            scanner.errs().show();
        } 
        catch (IOException e) 
        {
            System.out.println("Scanner I/O error: " + e.getMessage());
        }
    }


    public static void main(String[] args) 
    {
        String outputFile = "tests/TestResults.txt";
        String testsDir   = "tests";

        String[] testFiles = {
            testsDir + "/test1.lang",
            testsDir + "/test2.lang",
            testsDir + "/test3.lang",
            testsDir + "/test4.lang",
            testsDir + "/test5.lang"
        };

        // Save original streams
        PrintStream origOut = System.out;
        PrintStream origErr = System.err;

        try (PrintStream fileStream = new PrintStream(
                    Files.newOutputStream(Paths.get(outputFile)))) 
        {
            // Redirect both System.out and System.err through tee streams
            // so everything goes to console + file
            TeeStream teeOut = new TeeStream(origOut, fileStream);
            TeeStream teeErr = new TeeStream(origErr, fileStream);

            System.setOut(teeOut);
            System.setErr(teeErr);

            for (String testFile : testFiles) 
            {
                scanFile(testFile);
            }

            System.out.println();
            System.out.println("=== ALL TESTS COMPLETE ===");
            System.out.println("Results written to " + outputFile);
        } 
        catch (IOException e) 
        {
            origErr.println("Error: Could not write output file: " + outputFile);
            origErr.println(e.getMessage());
            System.exit(1);
        } 
        finally 
        {
            // Restore original streams
            System.setOut(origOut);
            System.setErr(origErr);
        }
    }
}