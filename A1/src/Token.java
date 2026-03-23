
public class Token 
{

    private TokenType type;
    private String lex;
    private int ln;
    private int col;

    public Token(TokenType type, String lex, int ln, int col) 
    {
        this.type = type;
        this.lex  = lex;
        this.ln   = ln;
        this.col  = col;
    }

    public TokenType type()  { return type; }
    public String    lex()   { return lex;  }
    public int       ln()    { return ln;   }
    public int       col()   { return col;  }

    // <TYPE, "lexeme", Line: X, Col: Y>
    @Override
    public String toString() 
    {
        return "<" + type + ", \"" + lex + "\", Line: " + ln + ", Col: " + col + ">";
    }
}
