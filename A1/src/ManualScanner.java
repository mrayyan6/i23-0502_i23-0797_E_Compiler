import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ManualScanner 
{

    private String file_name;     
    private int    pos;          
    private int    line;         
    private int    column;        

    private ErrorHandler  errors;
    private SymbolTable   symbols;

    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();

    static 
    {
        KEYWORDS.put("start",    TokenType.START);
        KEYWORDS.put("finish",   TokenType.FINISH);
        KEYWORDS.put("loop",     TokenType.LOOP);
        KEYWORDS.put("condition",TokenType.CONDITION);
        KEYWORDS.put("declare",  TokenType.DECLARE);
        KEYWORDS.put("output",   TokenType.OUTPUT);
        KEYWORDS.put("input",    TokenType.INPUT);
        KEYWORDS.put("function", TokenType.FUNCTION);
        KEYWORDS.put("return",   TokenType.RETURN);
        KEYWORDS.put("break",    TokenType.BREAK);
        KEYWORDS.put("continue", TokenType.CONTINUE);
        KEYWORDS.put("else",     TokenType.ELSE);
    }


    public ManualScanner(String fn) 
    {
        this.file_name    = fn;
        this.pos          = 0;
        this.line         = 1;
        this.column       = 1;
        this.errors = new ErrorHandler();
        this.symbols  = new SymbolTable();
    }

    // checks if at end of file
    private boolean done() 
    {
        return pos >= file_name.length();
    }

    private char peek() 
    {
        if (done()) return '\0';
        return file_name.charAt(pos);
    }

    // peeks one char ahead
    private char peek2() 
    {
        if (pos + 1 >= file_name.length()) return '\0';
        return file_name.charAt(pos + 1);
    }

    // peeks at offset
    private char peekN(int offset) 
    {
        int idx = pos + offset;
        if (idx >= file_name.length()) return '\0';
        return file_name.charAt(idx);
    }

    // rets the current char, advances pos
    private char advance() 
    {
        char c = file_name.charAt(pos);
        pos++;
        column++;
        return c;
    }

    private boolean match(char expected) 
    {
        if (done()) return false;
        if (file_name.charAt(pos) != expected) return false;
        pos++;
        column++;
        return true;
    }

    private void newline() 
    {
        line++;
        column = 1;
    }

    // skips whitespace, comments
    private void skip() 
    {
        while (!done()) 
        {
            char c = peek();

            if (c == ' ' || c == '\t' || c == '\r') 
            {
                advance();
                continue;
            }
            if (c == '\n') 
            {
                advance();
                newline();
                continue;
            }

            if (c == '#') 
            {
                if (peek2() == '|') 
                { 
                    multi();
                    continue;
                }
                if (peek2() == '#') 
                { 
                    single();
                    continue;
                }
            }

            break;
        }
    }

    // skips single line cmnt
    private void single() 
    {
        advance(); //   first #
        advance(); //   second #
        while (!done() && peek() != '\n') 
        {
            advance();
        }

    }

    // skip #| ... |# comment (non-nested)
    private void multi() 
    {
        int startLine = line;
        int startCol  = column;
        advance(); //   #
        advance(); //   |
        boolean closed = false;
        while (!done()) 
        {
            if (peek() == '|' && peek2() == '#') 
            {
                advance(); //   |
                advance(); //   #
                closed = true;
                break;
            }
            if (peek() == '\n') 
            {
                advance();
                newline();
            } 
            else 
            {
                advance();
            }
        }
        if (!closed) 
        {
            errors.noCloseCmnt(startLine, startCol, "#|...");
        }
    }

    // rets next token using longest match
    public Token next() 
    {
        skip();

        if (done()) 
        {
            return new Token(TokenType.EOF, "EOF", line, column);
        }

        int tokenLine = line;
        int tokenCol  = column;
        char c = peek();

        // string literal " ..." 
        if (c == '"') 
        {
            return str(tokenLine, tokenCol);
        }

        // char literal ' ... '
        if (c == '\'') 
        {
            return chr(tokenLine, tokenCol);
        }

        switch (c) 
        {
            case '+':
                advance();
                if (!done() && peek() == '+') { advance(); return new Token(TokenType.INCREMENT,   "++", tokenLine, tokenCol); }
                if (!done() && peek() == '=') { advance(); return new Token(TokenType.PLUS_ASSIGN, "+=", tokenLine, tokenCol); }
                // +digit = signed number
                if (!done() && Character.isDigit(peek())) 
                {
                    return num(tokenLine, tokenCol, "+");
                }
                return new Token(TokenType.PLUS, "+", tokenLine, tokenCol);

            case '-':
                advance();
                if (!done() && peek() == '-') { advance(); return new Token(TokenType.DECREMENT,    "--", tokenLine, tokenCol); }
                if (!done() && peek() == '=') { advance(); return new Token(TokenType.MINUS_ASSIGN, "-=", tokenLine, tokenCol); }
                // -digit = signed number
                if (!done() && Character.isDigit(peek())) 
                {
                    return num(tokenLine, tokenCol, "-");
                }
                return new Token(TokenType.MINUS, "-", tokenLine, tokenCol);

            case '*':
                advance();
                if (!done() && peek() == '*') { advance(); return new Token(TokenType.POWER,       "**", tokenLine, tokenCol); }
                if (!done() && peek() == '=') { advance(); return new Token(TokenType.MULT_ASSIGN, "*=", tokenLine, tokenCol); }
                return new Token(TokenType.MULTIPLY, "*", tokenLine, tokenCol);

            case '/':
                advance();
                if (!done() && peek() == '=') { advance(); return new Token(TokenType.DIV_ASSIGN, "/=", tokenLine, tokenCol); }
                return new Token(TokenType.DIVIDE, "/", tokenLine, tokenCol);

            case '%':
                advance();
                return new Token(TokenType.MODULO, "%", tokenLine, tokenCol);

            case '=':
                advance();
                if (!done() && peek() == '=') { advance(); return new Token(TokenType.EQUAL,  "==", tokenLine, tokenCol); }
                return new Token(TokenType.ASSIGN, "=", tokenLine, tokenCol);

            case '!':
                advance();
                if (!done() && peek() == '=') { advance(); return new Token(TokenType.NOT_EQUAL,   "!=", tokenLine, tokenCol); }
                return new Token(TokenType.LOGICAL_NOT, "!", tokenLine, tokenCol);

            case '<':
                advance();
                if (!done() && peek() == '=') { advance(); return new Token(TokenType.LESS_EQUAL,    "<=", tokenLine, tokenCol); }
                return new Token(TokenType.LESS_THAN, "<", tokenLine, tokenCol);

            case '>':
                advance();
                if (!done() && peek() == '=') { advance(); return new Token(TokenType.GREATER_EQUAL, ">=", tokenLine, tokenCol); }
                return new Token(TokenType.GREATER_THAN, ">", tokenLine, tokenCol);

            case '&':
                advance();
                if (!done() && peek() == '&') { advance(); return new Token(TokenType.LOGICAL_AND, "&&", tokenLine, tokenCol); }
                // invalid alone
                errors.badChar(tokenLine, tokenCol, "&");
                return new Token(TokenType.ERROR, "&", tokenLine, tokenCol);

            case '|':
                advance();
                if (!done() && peek() == '|') { advance(); return new Token(TokenType.LOGICAL_OR, "||", tokenLine, tokenCol); }
                errors.badChar(tokenLine, tokenCol, "|");
                return new Token(TokenType.ERROR, "|", tokenLine, tokenCol);

            case '(': advance(); return new Token(TokenType.LPAREN,    "(", tokenLine, tokenCol);
            case ')': advance(); return new Token(TokenType.RPAREN,    ")", tokenLine, tokenCol);
            case '{': advance(); return new Token(TokenType.LBRACE,    "{", tokenLine, tokenCol);
            case '}': advance(); return new Token(TokenType.RBRACE,    "}", tokenLine, tokenCol);
            case '[': advance(); return new Token(TokenType.LBRACKET,  "[", tokenLine, tokenCol);
            case ']': advance(); return new Token(TokenType.RBRACKET,  "]", tokenLine, tokenCol);
            case ',': advance(); return new Token(TokenType.COMMA,     ",", tokenLine, tokenCol);
            case ';': advance(); return new Token(TokenType.SEMICOLON, ";", tokenLine, tokenCol);
            case ':': advance(); return new Token(TokenType.COLON,     ":", tokenLine, tokenCol);
        }

        if (Character.isDigit(c)) 
        {
            return num(tokenLine, tokenCol, "");
        }

        if ((Character.isLetter(c)) || c == '_') 
        {
            return word(tokenLine, tokenCol);
        }

        advance();
        errors.badChar(tokenLine, tokenCol, String.valueOf(c));
        return new Token(TokenType.ERROR, String.valueOf(c), tokenLine, tokenCol);
    }

    // scans keyword/bool/identifier
    private Token word(int tokenLine, int tokenCol) 
    {
        StringBuilder sb = new StringBuilder();

        while (!done() && (Character.isLetterOrDigit(peek()) || peek() == '_')) 
        {
            sb.append(advance());
        }

        String word = sb.toString();

        if (KEYWORDS.containsKey(word)) 
        {
            return new Token(KEYWORDS.get(word), word, tokenLine, tokenCol);
        }

        if (word.equals("true") || word.equals("false")) 
        {
            return new Token(TokenType.BOOLEAN, word, tokenLine, tokenCol);
        }

        if (!Character.isUpperCase(word.charAt(0))) 
        {
            errors.log("Invalid Identifier", tokenLine, tokenCol, word, "Identifiers must start with an uppercase letter [A-Z].");
            return new Token(TokenType.ERROR, word, tokenLine, tokenCol);
        }

        for (int i = 1; i < word.length(); i++) 
        {
            char ch = word.charAt(i);
            if (!(ch >= 'a' && ch <= 'z') && !(ch >= '0' && ch <= '9') && ch != '_') 
            {
                errors.log("Invalid Identifier", tokenLine, tokenCol, word,
                        "Identifiers may only contain lowercase letters, digits, and underscores after the first character.");
                return new Token(TokenType.ERROR, word, tokenLine, tokenCol);
            }
        }

        if (word.length() > 31) 
        {
            errors.longId(tokenLine, tokenCol, word);
            return new Token(TokenType.ERROR, word, tokenLine, tokenCol);
        }

        symbols.add(word, "unknown", "global", tokenLine);

        return new Token(TokenType.IDENTIFIER, word, tokenLine, tokenCol);
    }

    // scans int, float
    private Token num(int tokenLine, int tokenCol, String prefix)   
    {
        StringBuilder sb = new StringBuilder(prefix);

        // whole number part
        while (!done() && Character.isDigit(peek())) 
        {
            {
                sb.append(advance());
            }

            // digit after .
            if (!done() && peek() == '.' && peek2() != '\0' && Character.isDigit(peek2()))  
            {
                sb.append(advance()); //   '.'
                int decimals = 0;
                while (!done() && Character.isDigit(peek())) 
                {
                    sb.append(advance());
                    decimals++;
                }
                if (decimals > 6)       
                {
                    errors.badLit(tokenLine, tokenCol, sb.toString(),
                            "Float literal exceeds maximum of 6 decimal digits.");
                    return new Token(TokenType.ERROR, sb.toString(), tokenLine, tokenCol);
                }

                // exp [eE][+-]?[0-9]+
                if (!done() && (peek() == 'e' || peek() == 'E')) 
                {
                    sb.append(advance()); //   e/E
                    if (!done() && (peek() == '+' || peek() == '-')) 
                    {
                        sb.append(advance());
                    }
                    if (done() || !Character.isDigit(peek())) 
                    {
                        errors.badLit(tokenLine, tokenCol, sb.toString(),
                                "Exponent part of float is incomplete.");
                        return new Token(TokenType.ERROR, sb.toString(), tokenLine, tokenCol);
                    }
                    while (!done() && Character.isDigit(peek())) 
                    {
                        sb.append(advance());
                    }
                }
                return new Token(TokenType.FLOAT, sb.toString(), tokenLine, tokenCol);
            }
        }
        return new Token(TokenType.INTEGER, sb.toString(), tokenLine, tokenCol);
    }

    // scans string  " ... "
    private Token str(int tokenLine, int tokenCol) 
    {
        StringBuilder sb = new StringBuilder();
        boolean hasError = false;
        advance(); //   opening "
        sb.append('"');

        while (!done() && peek() != '"') 
        {
            if (peek() == '\n') 
            {
                // newline inside string = unclosed string
                errors.noCloseStr(tokenLine, tokenCol, sb.toString());
                return new Token(TokenType.ERROR, sb.toString(), tokenLine, tokenCol);
            }
            if (peek() == '\\') 
            {
                sb.append(advance()); //  backslash
                if (done()) break;
                char esc = peek();
                switch (esc) 
                {
                    case '"': case '\\': case 'n': case 't': case 'r':
                        sb.append(advance());
                        break;
                    default:
                        errors.badEsc(tokenLine, tokenCol, "\\" + esc);
                        sb.append(advance());
                        hasError = true;
                        break;
                }
                continue;
            }
            sb.append(advance());
        }

        if (done() || !done() && peek() != '"')
        {
            errors.noCloseStr(tokenLine, tokenCol, sb.toString());
            return new Token(TokenType.ERROR, sb.toString(), tokenLine, tokenCol);
        }

        sb.append(advance()); //   closing "
        if (hasError)
        {
            return new Token(TokenType.ERROR, sb.toString(), tokenLine, tokenCol);
        }
        return new Token(TokenType.STRING, sb.toString(), tokenLine, tokenCol);
    }

    // scans char 
    private Token chr(int tokenLine, int tokenCol) 
    {
        StringBuilder sb = new StringBuilder();
        advance(); //   opening '
        sb.append('\'');

        if (done() || peek() == '\n') 
        {
            errors.noCloseChr(tokenLine, tokenCol, sb.toString());
            return new Token(TokenType.ERROR, sb.toString(), tokenLine, tokenCol);
        }

        if (peek() == '\\') 
        {
            sb.append(advance()); //   backslash
            if (done() || peek() == '\n') 
            {
                errors.noCloseChr(tokenLine, tokenCol, sb.toString());
                return new Token(TokenType.ERROR, sb.toString(), tokenLine, tokenCol);
            }
            char esc = peek();
            switch (esc)    
            {
                case '\'': case '\\': case 'n': case 't': case 'r':
                    sb.append(advance());
                    break;
                default:
                    errors.badEsc(tokenLine, tokenCol, "\\" + esc);
                    sb.append(advance());
                    break;
            }
        } 
        else if (peek() == '\'') 
        {
            // empty char literal ''
            errors.badLit(tokenLine, tokenCol, "''",
                    "Empty character literal.");
            advance(); //   closing '
            return new Token(TokenType.ERROR, "''", tokenLine, tokenCol);
        } 
        else 
        {
            sb.append(advance()); // the single character
        }

        // expect closing quote
        if (done() || peek() != '\'') 
        {
            // error recovery:   until ' or EOL
            while (!done() && peek() != '\'' && peek() != '\n') 
            {
                sb.append(advance());
            }
            if (!done() && peek() == '\'') 
            {
                sb.append(advance());
                errors.badLit(tokenLine, tokenCol, sb.toString(),
                        "Character literal contains more than one character.");
                return new Token(TokenType.ERROR, sb.toString(), tokenLine, tokenCol);
            }
            errors.noCloseChr(tokenLine, tokenCol, sb.toString());
            return new Token(TokenType.ERROR, sb.toString(), tokenLine, tokenCol);
        }

        sb.append(advance()); //   closing '
        return new Token(TokenType.CHAR, sb.toString(), tokenLine, tokenCol);
    }



    // rets error handler
    public ErrorHandler errs() { return errors; }
    // rets symbol table
    public SymbolTable  syms()  { return symbols;  }

    // scans all tokens
    public List<Token> all() 
    {
        List<Token> tokens = new ArrayList<>();
        Token t;
        do 
        {
            t = next();
            tokens.add(t);
        } while (t.type() != TokenType.EOF);
        return tokens;
    }

    // entry point: java ManualScanner <file-path>
    public static void main(String[] args) 
    {

        if (args.length < 1) 
        {
            System.err.println("Usage: java ManualScanner <file_name-file>");
            System.exit(1);
        }

        String filePath = args[0];
        String file_name;
        try 
        {
            file_name = new String(Files.readAllBytes(Paths.get(filePath)));
        } 
        catch (IOException e) 
        {
            System.err.println("Error: Could not read file: " + filePath);
            System.err.println(e.getMessage());
            System.exit(1);
            return;
        }

        ManualScanner scanner = new ManualScanner(file_name);
        List<Token> tokens = scanner.all();

            
        System.out.println("||============================================================|| ");
        System.out.println("||                        TOKEN STREAM                        || ");
        System.out.println("||============================================================|| ");

        for (Token tok : tokens) 
        {
            System.out.println("  " + tok);
        }
        System.out.println("||============================================================|| ");
        System.out.printf("||  Total tokens: %-45d ║%n", tokens.size());
        System.out.println("||============================================================|| ");

        scanner.syms().show();

        scanner.errs().show();
    }
}
