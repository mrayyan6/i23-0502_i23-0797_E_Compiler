package json2xml;

/**
 * Token.java — A single token produced by the Lexer.
 * CS-4031 Compiler Construction — Assignment 04 (Java)
 */
public class Token {
    public final TokenType type;
    public final String    value;   // raw text (meaningful for STRING, NUMBER)
    public final int       line;
    public final int       column;

    public Token(TokenType type, String value, int line, int column) {
        this.type   = type;
        this.value  = value;
        this.line   = line;
        this.column = column;
    }

    @Override
    public String toString() {
        return String.format("Token(%s, \"%s\", L%d:C%d)", type, value, line, column);
    }
}
