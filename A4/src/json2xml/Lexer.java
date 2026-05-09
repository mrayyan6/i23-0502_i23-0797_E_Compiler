package json2xml;

import java.util.ArrayList;
import java.util.List;

/**
 * Lexer.java — Hand-written lexical analyser for JSON.
 *
 * Equivalent role to scanner.l (Flex) in the C version.
 * Reads the full source string and produces a flat List of Tokens.
 *
 * Supported tokens
 * ─────────────────
 *   Punctuation  : { } [ ] : ,
   *   Strings      : "..." with escape sequences backslash-quote, backslash-backslash, backslash-n, backslash-t, backslash-uXXXX
 *   Numbers      : integer, decimal, scientific notation  (e.g. -3.14e+2)
 *   Keywords     : true  false  null
 *
 * Lexical errors (invalid character, bad escape, unterminated string)
 * cause a LexerException that is caught by Main and printed as:
 *   "Error: <description> at line L, column C"
 *
 * CS-4031 Compiler Construction — Assignment 04 (Java)
 */
public class Lexer {

    // ── source ──────────────────────────────────────────────────────────
    private final String src;
    private       int    pos    = 0;   // current character index
    private       int    line   = 1;
    private       int    col    = 1;

    // ── constructor ──────────────────────────────────────────────────────
    public Lexer(String source) {
        this.src = source;
    }

    // ────────────────────────────────────────────────────────────────────
    //  Public API
    // ────────────────────────────────────────────────────────────────────

    /** Tokenise the entire input and return the token list. */
    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (true) {
            skipWhitespace();
            if (pos >= src.length()) {
                tokens.add(new Token(TokenType.EOF, "", line, col));
                break;
            }
            tokens.add(nextToken());
        }
        return tokens;
    }

    // ────────────────────────────────────────────────────────────────────
    //  Private helpers
    // ────────────────────────────────────────────────────────────────────

    private char peek() {
        return src.charAt(pos);
    }

    private char advance() {
        char c = src.charAt(pos++);
        if (c == '\n') { line++; col = 1; }
        else           { col++; }
        return c;
    }

    private void skipWhitespace() {
        while (pos < src.length()) {
            char c = peek();
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') advance();
            else break;
        }
    }

    private Token nextToken() {
        int startLine = line;
        int startCol  = col;
        char c = peek();

        // ── Punctuation ───────────────────────────────────────────────
        switch (c) {
            case '{': advance(); return new Token(TokenType.LBRACE,    "{", startLine, startCol);
            case '}': advance(); return new Token(TokenType.RBRACE,    "}", startLine, startCol);
            case '[': advance(); return new Token(TokenType.LBRACKET,  "[", startLine, startCol);
            case ']': advance(); return new Token(TokenType.RBRACKET,  "]", startLine, startCol);
            case ':': advance(); return new Token(TokenType.COLON,     ":", startLine, startCol);
            case ',': advance(); return new Token(TokenType.COMMA,     ",", startLine, startCol);
        }

        // ── String ────────────────────────────────────────────────────
        if (c == '"') return scanString(startLine, startCol);

        // ── Number ────────────────────────────────────────────────────
        if (c == '-' || Character.isDigit(c)) return scanNumber(startLine, startCol);

        // ── Keywords: true / false / null ─────────────────────────────
        if (Character.isLetter(c)) return scanKeyword(startLine, startCol);

        // ── Invalid character ─────────────────────────────────────────
        advance(); // consume so we can report it
        throw new LexerException(
            String.format("invalid character '%c'", c), startLine, startCol);
    }

    // ── String scanner ────────────────────────────────────────────────
    private Token scanString(int startLine, int startCol) {
        advance(); // consume opening "
        StringBuilder sb = new StringBuilder();

        while (true) {
            if (pos >= src.length()) {
                throw new LexerException("unterminated string", startLine, startCol);
            }
            char c = peek();
            if (c == '\n') {
                throw new LexerException("unterminated string (newline inside string)",
                    line, col);
            }
            if (c == '"') { advance(); break; } // closing quote

            if (c == '\\') {
                advance(); // consume backslash
                if (pos >= src.length()) {
                    throw new LexerException("unterminated escape sequence", line, col);
                }
                char esc = advance();
                switch (esc) {
                    case '"':  sb.append('"');  break;
                    case '\\': sb.append('\\'); break;
                    case '/':  sb.append('/');  break;
                    case 'n':  sb.append('\n'); break;
                    case 't':  sb.append('\t'); break;
                    case 'r':  sb.append('\r'); break;
                    case 'b':  sb.append('\b'); break;
                    case 'f':  sb.append('\f'); break;
                    case 'u':  sb.append(scanUnicode()); break;
                    default:
                        throw new LexerException(
                            String.format("invalid escape sequence '\\%c'", esc),
                            line, col - 1);
                }
            } else {
                sb.append(advance());
            }
        }
        return new Token(TokenType.STRING, sb.toString(), startLine, startCol);
    }

    /** Read 4 hex digits after backslash-u and return the decoded char(s) as a String. */
    private String scanUnicode() {
        if (pos + 4 > src.length()) {
            throw new LexerException("incomplete \\uXXXX escape", line, col);
        }
        String hex = src.substring(pos, pos + 4);
        for (char h : hex.toCharArray()) {
            if (!isHexDigit(h)) {
                throw new LexerException(
                    String.format("invalid \\uXXXX escape '\\u%s'", hex), line, col);
            }
        }
        pos += 4; col += 4;
        int codePoint = Integer.parseInt(hex, 16);
        return new String(Character.toChars(codePoint));
    }

    private boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    // ── Number scanner ────────────────────────────────────────────────
    //   Grammar:  -? ( 0 | [1-9][0-9]* ) ( \.[0-9]+ )? ( [eE][+-]?[0-9]+ )?
    private Token scanNumber(int startLine, int startCol) {
        int start = pos;

        if (peek() == '-') advance();                   // optional minus

        if (pos >= src.length() || !Character.isDigit(peek())) {
            throw new LexerException("invalid number", startLine, startCol);
        }

        if (peek() == '0') {
            advance();
        } else {
            while (pos < src.length() && Character.isDigit(peek())) advance();
        }

        // fractional part
        if (pos < src.length() && peek() == '.') {
            advance();
            if (pos >= src.length() || !Character.isDigit(peek())) {
                throw new LexerException("invalid number (expected digits after '.')",
                    startLine, startCol);
            }
            while (pos < src.length() && Character.isDigit(peek())) advance();
        }

        // exponent part — supports scientific notation (bonus)
        if (pos < src.length() && (peek() == 'e' || peek() == 'E')) {
            advance();
            if (pos < src.length() && (peek() == '+' || peek() == '-')) advance();
            if (pos >= src.length() || !Character.isDigit(peek())) {
                throw new LexerException("invalid number (expected digits in exponent)",
                    startLine, startCol);
            }
            while (pos < src.length() && Character.isDigit(peek())) advance();
        }

        String numStr = src.substring(start, pos);
        return new Token(TokenType.NUMBER, numStr, startLine, startCol);
    }

    // ── Keyword scanner ───────────────────────────────────────────────
    private Token scanKeyword(int startLine, int startCol) {
        int start = pos;
        while (pos < src.length() && Character.isLetter(peek())) advance();
        String word = src.substring(start, pos);
        switch (word) {
            case "true":  return new Token(TokenType.TRUE,  "true",  startLine, startCol);
            case "false": return new Token(TokenType.FALSE, "false", startLine, startCol);
            case "null":  return new Token(TokenType.NULL,  "null",  startLine, startCol);
            default:
                throw new LexerException(
                    String.format("unknown keyword '%s'", word), startLine, startCol);
        }
    }

    // ────────────────────────────────────────────────────────────────────
    //  Nested exception class
    // ────────────────────────────────────────────────────────────────────

    /** Thrown on any lexical error. */
    public static class LexerException extends RuntimeException {
        public final int line;
        public final int column;

        public LexerException(String msg, int line, int col) {
            super(msg);
            this.line   = line;
            this.column = col;
        }
    }
}
