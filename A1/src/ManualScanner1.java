import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class ManualScanner1 
{
    private String input;
    private int    pos;
    private int    line;
    private int    column;
    private int    startLine;
    private int    startCol;
    private boolean stringHasError;
    
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

    public ManualScanner1(String input) 
    {
        this.input  = input;
        this.pos    = 0;
        this.line   = 1;
        this.column = 1;
    }

    public List<Token> all() 
    {
        List<Token> tokens = new ArrayList<>();
        Token t;
        do 
        {
            t = next();
            if (t != null) 
            { 
                tokens.add(t);
            }
        } while (t == null || t.type() != TokenType.EOF);
        return tokens;
    }

    public Token next() 
    {
        if (pos >= input.length()) 
        {
            return new Token(TokenType.EOF, "EOF", line, column);
        }

        int state = 0;
        StringBuilder lexeme = new StringBuilder();
        startLine = line;
        startCol  = column;
        
        while (true) 
        {
            char c = (pos < input.length()) ? input.charAt(pos) : '\0';

            switch (state) 
            {
                case 0:
                    if (pos >= input.length()) 
                    {
                        return new Token(TokenType.EOF, "EOF", line, column);
                    }

                    if (c == ' ' || c == '\t' || c == '\r' || c == '\n') 
                    {
                        state = 14;
                        consume(); 
                        break;
                    }

                    if (c == ',' || c == ';' || c == ':' || 
                        c == '(' || c == ')' || 
                        c == '[' || c == ']' || 
                        c == '{' || c == '}') 
                    {
                        lexeme.append(c);
                        consume();
                        return createToken(lexeme.toString());
                    }

                    if (Character.isLetter(c)) 
                    {
                        state = 5;
                        lexeme.append(c);
                        consume();
                        break;
                    }
                    
                    if (c == '"') 
                    {
                        state = 30; 
                        consume(); 
                        lexeme.append('"');
                        stringHasError = false;
                        break;
                    }

                     if (c == '\'') 
                     {
                        state = 60; 
                        consume(); 
                        lexeme.append('\'');
                        break;
                    }

                    if (c == '+' || c == '-') 
                    {
                        state = 7;
                        lexeme.append(c);
                        consume();
                        break;
                    }
                    
                    if (Character.isDigit(c)) 
                    {
                        state = 8;
                        lexeme.append(c);
                        consume();
                        break;
                    }
                    
                    if (c == '*' || c == '/' || c == '%' || c == '=' || c == '!' || c == '<' || c == '>' || c == '&' || c == '|') 
                    {
                         state = 40; 
                         lexeme.append(c);
                         consume();
                         break;
                    }

                    if (c == '#') 
                    {
                        state = 12;
                        lexeme.append(c);
                        consume();
                        break;
                    }

                    consume(); 
                    return new Token(TokenType.ERROR, String.valueOf(c), startLine, startCol);

                case 5: 
                if (Character.isLetterOrDigit(c) || c == '_') 
                    {
                        state = 5;
                        lexeme.append(c);
                        consume();
                        break;
                    }
                    return checkKeywordOrId(lexeme.toString());

                case 6: 
                    state = 5;
                    break;

                case 7: 
                    if (Character.isDigit(c)) 
                    {
                        state = 8;
                        lexeme.append(c);
                        consume();
                        break;
                    }
                    if (lexeme.toString().equals("+") && c == '+') 
                    { 
                        lexeme.append(c); consume(); return new Token(TokenType.INCREMENT, "++", startLine, startCol);
                    }
                     if (lexeme.toString().equals("+") && c == '=') 
                     { 
                        lexeme.append(c); consume(); return new Token(TokenType.PLUS_ASSIGN, "+=", startLine, startCol);
                    }
                    if (lexeme.toString().equals("-") && c == '-') 
                    { 
                        lexeme.append(c); consume(); return new Token(TokenType.DECREMENT, "--", startLine, startCol);
                    }
                     if (lexeme.toString().equals("-") && c == '=') 
                     { 
                        lexeme.append(c); consume(); return new Token(TokenType.MINUS_ASSIGN, "-=", startLine, startCol);
                    }
                    return createOperatorToken(lexeme.toString());

                case 8:
                    if (Character.isDigit(c)) 
                    {
                        state = 8; 
                        lexeme.append(c);
                        consume();
                        break;
                    }
                    if (c == '.') 
                    {
                        if (pos + 1 < input.length() && Character.isDigit(input.charAt(pos + 1))) 
                        {
                            state = 91; 
                            lexeme.append(c);
                            consume(); 
                            break;
                        }
                        return new Token(TokenType.INTEGER, lexeme.toString(), startLine, startCol);
                    }
                    return new Token(TokenType.INTEGER, lexeme.toString(), startLine, startCol);

                case 91: 
                    if (Character.isDigit(c)) 
                    {
                        state = 9;
                        lexeme.append(c);
                        consume();
                        break;
                    }
                     return new Token(TokenType.ERROR, lexeme.toString(), startLine, startCol);

                case 9: 
                    if (Character.isDigit(c)) 
                    {
                        state = 9; 
                        lexeme.append(c);
                        consume();
                        break;
                    }
                    if (c == 'e' || c == 'E') 
                    {
                        state = 10;
                        lexeme.append(c);
                        consume();
                        break;
                    }
                    return checkFloat(lexeme.toString());

                case 10: 
                    if (c == '+' || c == '-') 
                    {
                        state = 11;
                        lexeme.append(c);
                        consume();
                        break;
                    }
                    if (Character.isDigit(c)) 
                    {
                        state = 120; 
                        lexeme.append(c);
                        consume();
                        break;
                    }
                    return new Token(TokenType.ERROR, lexeme.toString(), startLine, startCol);

                case 11: 
                    if (Character.isDigit(c)) 
                    {
                        state = 120; 
                        lexeme.append(c);
                        consume();
                        break;
                    }
                    return new Token(TokenType.ERROR, lexeme.toString(), startLine, startCol);

                case 120: 
                    if (Character.isDigit(c)) 
                    { 
                        state = 120;
                        lexeme.append(c);
                        consume();
                        break;
                    }
                    return checkFloat(lexeme.toString());

                case 12: 
                    if (c == '|' && lexeme.length() == 1) 
                    { 
                         state = 150; 
                         lexeme.append(c);
                         consume();
                         break;
                    }
                    if (c == '#' && lexeme.length() == 1) 
                    { 
                        state = 13; 
                        lexeme.append(c);
                        consume();
                        break;
                    }
                    return new Token(TokenType.ERROR, lexeme.toString(), startLine, startCol);

                case 13:
                    if (c == '\n' || pos >= input.length()) 
                    { 
                        state = 0;
                        lexeme.setLength(0);
                        if (c == '\n') consume(); 
                        return null; 
                    }
                    consume();
                    break;

                case 150: 
                    if (pos >= input.length()) 
                    {
                        return new Token(TokenType.ERROR, lexeme.toString(), startLine, startCol);
                    }
                    if (c == '|') 
                    {
                         state = 151; 
                         lexeme.append(c);
                         consume();
                         break;
                    }
                    if (c == '\n') 
                    {
                        lexeme.append(c);
                        consume();
                        break;
                    }
                    lexeme.append(c); 
                    consume();
                    break;
                
                case 151: 
                    if (c == '#') 
                    { 
                        consume();
                        state = 0;
                        lexeme.setLength(0);
                        return null;
                    }
                    if (c == '|') 
                    {
                         consume();
                         break; 
                    }
                    state = 150;
                    consume();
                    break;

                case 14: 
                    if (c == ' ' || c == '\t' || c == '\r' || c == '\n') 
                    {
                        state = 14;
                        consume();
                        break;
                    }
                    return null; 
                    
                case 30: 
                    if (pos >= input.length()) 
                    {
                         return new Token(TokenType.ERROR, lexeme.toString(), startLine, startCol);
                    }
                    if (c == '\n') 
                    {
                        return new Token(TokenType.ERROR, lexeme.toString(), startLine, startCol);
                    }
                    if (c == '\\') 
                    {
                        state = 31; 
                        lexeme.append(c);
                        consume();
                        break;
                    }
                    if (c == '"') 
                    {
                        lexeme.append('"');
                        consume();
                        if (stringHasError) 
                        {
                            return new Token(TokenType.ERROR, lexeme.toString(), startLine, startCol);
                        }
                        return new Token(TokenType.STRING, lexeme.toString(), startLine, startCol);
                    }
                    lexeme.append(c);
                    consume();
                    break;
                
                case 31: 
                    if (pos >= input.length()) return new Token(TokenType.ERROR, lexeme.toString(), startLine, startCol);
                    if (c != '"' && c != '\\' && c != 'n' && c != 't' && c != 'r') 
                    {
                        stringHasError = true;
                    }
                    lexeme.append(c);
                    consume();
                    state = 30; 
                    break;

                case 60:
                     if (pos >= input.length()) return new Token(TokenType.ERROR, lexeme.toString(), startLine, startCol);
                     if (c == '\'') 
                     { 
                         if (lexeme.length() == 1)
                         { 
                             lexeme.append(c); consume();
                             return new Token(TokenType.ERROR, lexeme.toString(), startLine, startCol);
                         } 
                         lexeme.append(c); consume();
                         return new Token(TokenType.CHAR, lexeme.toString(), startLine, startCol);
                     }
                     if (c == '\\') 
                     {
                         state = 61;
                         lexeme.append(c); consume();
                         break;
                     }
                     if (c == '\n')
                     {
                         return new Token(TokenType.ERROR, lexeme.toString(), startLine, startCol);
                     }
                     lexeme.append(c); consume();
                     state = 62;
                     break;

                 case 61:
                     if (pos >= input.length()) return new Token(TokenType.ERROR, lexeme.toString(), startLine, startCol);
                     lexeme.append(c); consume();
                     state = 62; 
                     break;

                 case 62: // expect closing '
                     if (c == '\'') 
                     {
                         lexeme.append(c); consume();
                         return new Token(TokenType.CHAR, lexeme.toString(), startLine, startCol);
                     }
                     // multi-char or unclosed char literal
                     while (pos < input.length()) 
                     {
                         char ch = input.charAt(pos);
                         if (ch == '\'' || ch == '\n') break;
                         lexeme.append(ch);
                         consume();
                     }
                     if (pos < input.length() && input.charAt(pos) == '\'') 
                     {
                         lexeme.append('\'');
                         consume();
                     }
                     return new Token(TokenType.ERROR, lexeme.toString(), startLine, startCol);

                 case 40:
                    String current = lexeme.toString();
                    
                    if (current.equals("*")) 
                    {
                         if (c == '*') { lexeme.append(c); consume(); return new Token(TokenType.POWER, "**", startLine, startCol); }
                         if (c == '=') { lexeme.append(c); consume(); return new Token(TokenType.MULT_ASSIGN, "*=", startLine, startCol); }
                    }
                    if (current.equals("/")) 
                    {
                         if (c == '=') { lexeme.append(c); consume(); return new Token(TokenType.DIV_ASSIGN, "/=", startLine, startCol); }
                    }
                    if (current.equals("=")) 
                    {
                         if (c == '=') { lexeme.append(c); consume(); return new Token(TokenType.EQUAL, "==", startLine, startCol); }
                    }
                    if (current.equals("!")) 
                    {
                         if (c == '=') { lexeme.append(c); consume(); return new Token(TokenType.NOT_EQUAL, "!=", startLine, startCol); }
                    }
                    if (current.equals("<")) 
                    {
                         if (c == '=') { lexeme.append(c); consume(); return new Token(TokenType.LESS_EQUAL, "<=", startLine, startCol); }
                    }
                    if (current.equals(">")) 
                    {
                         if (c == '=') { lexeme.append(c); consume(); return new Token(TokenType.GREATER_EQUAL, ">=", startLine, startCol); }
                    }
                    if (current.equals("&")) 
                    {
                        if (c == '&') { lexeme.append(c); consume(); return new Token(TokenType.LOGICAL_AND, "&&", startLine, startCol); }
                        return new Token(TokenType.ERROR, "&", startLine, startCol);
                    }
                    if (current.equals("|")) 
                    {
                        if (c == '|') { lexeme.append(c); consume(); return new Token(TokenType.LOGICAL_OR, "||", startLine, startCol); }
                        return new Token(TokenType.ERROR, "|", startLine, startCol);
                    }
                    
                    return createOperatorToken(current);

                default:
                    return new Token(TokenType.ERROR, "Unknown State: " + state, startLine, startCol);
            }
        }
    }

    private void consume() 
    {
        char c = (pos < input.length()) ? input.charAt(pos) : '\0';
        pos++;
        column++;
        if (c == '\n') 
        {
            line++;
            column = 1;
        }
    }
    
    private Token checkKeywordOrId(String word) 
    {
        if (KEYWORDS.containsKey(word)) 
        {
            return new Token(KEYWORDS.get(word), word, startLine, startCol);
        }
        
        if (word.equals("true") || word.equals("false")) 
        {
            return new Token(TokenType.BOOLEAN, word, startLine, startCol);
        }
        
        if (!Character.isUpperCase(word.charAt(0))) 
        {
             return new Token(TokenType.ERROR, word, startLine, startCol);
        }

        for (int i = 1; i < word.length(); i++) 
        {
            char ch = word.charAt(i);
            if (!(ch >= 'a' && ch <= 'z') && !(ch >= '0' && ch <= '9') && ch != '_') 
            {
                 return new Token(TokenType.ERROR, word, startLine, startCol);
            }
        }

        if (word.length() > 31) 
        {
             return new Token(TokenType.ERROR, word, startLine, startCol);
        }

        return new Token(TokenType.IDENTIFIER, word, startLine, startCol);
    }
    
    private Token checkFloat(String lex) 
    {
        int pointIndex = lex.indexOf('.');
        int expIndexLower = lex.indexOf('e');
        int expIndexUpper = lex.indexOf('E');
        int expIndex = (expIndexLower != -1) ? expIndexLower : expIndexUpper;
        
        if (pointIndex != -1) 
        {
            int end = (expIndex != -1) ? expIndex : lex.length();
            int decimals = end - pointIndex - 1;
            if (decimals > 6) 
            {
                return new Token(TokenType.ERROR, lex, startLine, startCol);
            }
        }
        return new Token(TokenType.FLOAT, lex, startLine, startCol);
    }

    private Token createToken(String lex) 
    {
         TokenType type = TokenType.ERROR;
         switch(lex) 
         {
             case ",": type = TokenType.COMMA; break;
             case ";": type = TokenType.SEMICOLON; break;
             case ":": type = TokenType.COLON; break;
             case "(": type = TokenType.LPAREN; break;
             case ")": type = TokenType.RPAREN; break;
             case "[": type = TokenType.LBRACKET; break;
             case "]": type = TokenType.RBRACKET; break;
             case "{": type = TokenType.LBRACE; break;
             case "}": type = TokenType.RBRACE; break;
         }
         return new Token(type, lex, startLine, startCol);
    }

    private Token createOperatorToken(String lex) 
    {
        TokenType type = TokenType.ERROR;
        switch(lex) 
        {
            case "+": type = TokenType.PLUS; break;
            case "-": type = TokenType.MINUS; break;
            case "*": type = TokenType.MULTIPLY; break;
            case "/": type = TokenType.DIVIDE; break;
            case "%": type = TokenType.MODULO; break;
            case "=": type = TokenType.ASSIGN; break;
            case "==": type = TokenType.EQUAL; break;
            case "!=": type = TokenType.NOT_EQUAL; break;
            case "<": type = TokenType.LESS_THAN; break;
            case ">": type = TokenType.GREATER_THAN; break;
            case "<=": type = TokenType.LESS_EQUAL; break;
            case ">=": type = TokenType.GREATER_EQUAL; break;
            case "!": type = TokenType.LOGICAL_NOT; break;
            case "&&": type = TokenType.LOGICAL_AND; break;
            case "||": type = TokenType.LOGICAL_OR; break;
            case "**": type = TokenType.POWER; break;
            case "+=": type = TokenType.PLUS_ASSIGN; break;
            case "-=": type = TokenType.MINUS_ASSIGN; break;
            case "*=": type = TokenType.MULT_ASSIGN; break;
            case "/=": type = TokenType.DIV_ASSIGN; break;
            case "++": type = TokenType.INCREMENT; break;
            case "--": type = TokenType.DECREMENT; break;
        }
        return new Token(type, lex, startLine, startCol);
    }
}
