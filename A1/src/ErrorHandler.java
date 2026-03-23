import java.util.ArrayList;
import java.util.List;


public class ErrorHandler 
{

    public static class Err 
    {
        private String type;
        private int    ln;
        private int    col;
        private String lex;
        private String msg;

        public Err(String type, int ln, int col, String lex, String msg) 
        {
            this.type = type;
            this.ln   = ln;
            this.col  = col;
            this.lex  = lex;
            this.msg  = msg;
        }

        @Override
        public String toString() 
        {
            return "Error: [" + type + "] at Line " + ln + ", Col " + col
                    + ": Lexeme \"" + lex + "\" - " + msg;
        }
    }

    private List<Err> errs;

    public ErrorHandler() 
    {
        errs = new ArrayList<>();
    }

    public void badChar(int ln, int col, String lex) 
    {
        errs.add(new Err("Invalid Character", ln, col, lex,
                "Character is not part of the language alphabet."));
    }

    public void badLit(int ln, int col, String lex, String detail) 
    {
        errs.add(new Err("Malformed Literal", ln, col, lex, detail));
    }
    public void noCloseStr(int ln, int col, String lex) 
    {
        errs.add(new Err("Unclosed String", ln, col, lex,
                "String literal was never terminated."));
    }

    public void noCloseChr(int ln, int col, String lex) 
    {
        errs.add(new Err("Unclosed Char", ln, col, lex,
                "Character literal was never terminated."));
    }

    public void noCloseCmnt(int ln, int col, String lex) 
    {
        errs.add(new Err("Unclosed Comment", ln, col, lex,
                "Multi-line comment was never terminated with |#."));
    }
    public void longId(int ln, int col, String lex) 
    {
        errs.add(new Err("Identifier Too Long", ln, col, lex,
                "Identifier exceeds maximum length of 31 characters."));
    }

    public void badEsc(int ln, int col, String lex) 
    {
        errs.add(new Err("Invalid Escape", ln, col, lex,
                "Unrecognized escape sequence."));
    }

    public void log(String type, int ln, int col, String lex, String msg) 
    {
        errs.add(new Err(type, ln, col, lex, msg));
    }

    public boolean any() 
    {
        return !errs.isEmpty();
    }

    public int count() 
    {
        return errs.size();
    }

    public List<Err> list() 
    {
        return errs;
    }

    public void show() 
    {
        if (errs.isEmpty()) 
        {
            System.out.println("\n[ErrorHandler] No lexical errors detected.");
            return;
        }

        System.err.println("\n||============================================================||");
        System.err.println("||                     LEXICAL ERRORS                         ||");
        System.err.println("||============================================================||");


        for (Err e : errs) 
        {
            System.err.println("||  " + e);
        }

        System.err.println("||============================================================||");
        System.err.printf("||  Total errors: %-45d ||%n", errs.size());
        System.err.println("||============================================================||");
    }
}
