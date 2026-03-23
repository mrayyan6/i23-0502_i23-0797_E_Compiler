
import java.util.ArrayList;
import java.util.List;

%%



%class   Yylex
%unicode
%line
%column
%type    Token

%{
    /* ----- helpers accessible inside generated scanner ----- */
    private ErrorHandler  errors  = new ErrorHandler();
    private SymbolTable   symbols = new SymbolTable();

    public  ErrorHandler  errs()  { return errors;  }
    public  SymbolTable   syms()  { return symbols;  }

    
    private int ln()  { return yyline + 1; }
    /** convenience: 1-based column */
    private int col() { return yycolumn + 1; }

    
    private Token tok(TokenType t) {
        return new Token(t, yytext(), ln(), col());
    }
    private Token tok(TokenType t, String lex) {
        return new Token(t, lex, ln(), col());
    }

    /** register identifier in symbol table */
    private void addSym(String name) {
        symbols.add(name, "N/A", "Global", ln());
    }
%}

/* ---------- Macros ---------- */

DIGIT       = [0-9]
UPPER       = [A-Z]
LOWER       = [a-z]
LETTER      = [A-Za-z]
ID_TAIL     = [a-z0-9_]
WHITESPACE  = [ \t\r]
NEWLINE     = \n

/* ---------- States ---------- */
%state STRING_STATE
%state CHAR_STATE
%state MULTICOMMENT

%{
    /* ----- string / char accumulators ----- */
    private StringBuilder strBuf;
    private int           strLine;
    private int           strCol;
    private boolean       strHasError;

    private StringBuilder chrBuf;
    private int           chrLine;
    private int           chrCol;
    private int           chrCount;        /* characters inside the quotes */
    private boolean       chrHasEscape;
    private boolean       chrHasError;

    /* ----- multi-line comment tracking ----- */
    private int cmtLine;
    private int cmtCol;
%}

%%

/* ===================================================================
 *  LEXICAL RULES
 * =================================================================== */

/* -------- Multi-line comments  #| ... |# -------- */
<YYINITIAL> {
    "#|"    {
                cmtLine = ln(); cmtCol = col();
                yybegin(MULTICOMMENT);
            }
}

<MULTICOMMENT> {
    "|#"        { yybegin(YYINITIAL); }
    [^|]*       { /* consume */ }
    "|"         { /* consume lone pipe */ }
    <<EOF>>     {
                    errors.noCloseCmnt(cmtLine, cmtCol, "#|...");
                    yybegin(YYINITIAL);
                    return tok(TokenType.EOF, "EOF");
                }
}

/* -------- Single-line comments  /
<YYINITIAL> {
    "##"[^\n]*  { /* skip */ }
}

/* -------- Whitespace -------- */
<YYINITIAL> {
    {WHITESPACE}+   { /* skip */ }
    {NEWLINE}       { /* skip, line tracking automatic */ }
}

/* -------- String literals  /
<YYINITIAL> {
    \"  {
            strBuf = new StringBuilder("\"");
            strLine = ln(); strCol = col();
            strHasError = false;
            yybegin(STRING_STATE);
        }
}

<STRING_STATE> {
    /* valid escape sequences */
    \\\"    { strBuf.append("\\\""); }
    \\\\    { strBuf.append("\\\\"); }
    \\n     { strBuf.append("\\n");  }
    \\t     { strBuf.append("\\t");  }
    \\r     { strBuf.append("\\r");  }

    /* invalid escape sequence */
    \\.     {
                errors.badEsc(strLine, strCol, yytext());
                strBuf.append(yytext());
                strHasError = true;
            }

    /* closing quote */
    \"      {
                strBuf.append("\"");
                yybegin(YYINITIAL);
                if (strHasError)
                    return new Token(TokenType.ERROR, strBuf.toString(), strLine, strCol);
                return new Token(TokenType.STRING, strBuf.toString(), strLine, strCol);
            }

    /* newline inside string -> unclosed */
    \n      {
                errors.noCloseStr(strLine, strCol, strBuf.toString());
                yybegin(YYINITIAL);
                return new Token(TokenType.ERROR, strBuf.toString(), strLine, strCol);
            }

    /* any other character */
    [^\"\\\n]+  { strBuf.append(yytext()); }

    /* EOF inside string */
    <<EOF>> {
                errors.noCloseStr(strLine, strCol, strBuf.toString());
                yybegin(YYINITIAL);
                return new Token(TokenType.ERROR, strBuf.toString(), strLine, strCol);
            }
}

/* -------- Character literals  */
<YYINITIAL> {
    \'  {
            chrBuf = new StringBuilder("'");
            chrLine = ln(); chrCol = col();
            chrCount = 0;
            chrHasEscape = false;
            chrHasError = false;
            yybegin(CHAR_STATE);
        }
}

<CHAR_STATE> {
    /* valid escape sequences */
    \\\'    { chrBuf.append("\\'");  chrCount++; chrHasEscape = true; }
    \\\\    { chrBuf.append("\\\\"); chrCount++; chrHasEscape = true; }
    \\n     { chrBuf.append("\\n");  chrCount++; chrHasEscape = true; }
    \\t     { chrBuf.append("\\t");  chrCount++; chrHasEscape = true; }
    \\r     { chrBuf.append("\\r");  chrCount++; chrHasEscape = true; }

    /* invalid escape */
    \\.     {
                errors.badEsc(chrLine, chrCol, yytext());
                chrBuf.append(yytext());
                chrCount++;
                chrHasError = true;
            }

    /* closing quote */
    \'      {
                yybegin(YYINITIAL);
                if (chrCount == 0) {
                    /* empty char literal '' */
                    errors.badLit(chrLine, chrCol, "''", "Empty character literal.");
                    return new Token(TokenType.ERROR, "''", chrLine, chrCol);
                }
                if (chrCount > 1) {
                    chrBuf.append("'");
                    errors.badLit(chrLine, chrCol, chrBuf.toString(),
                            "Character literal contains more than one character.");
                    return new Token(TokenType.ERROR, chrBuf.toString(), chrLine, chrCol);
                }
                chrBuf.append("'");
                if (chrHasError)
                    return new Token(TokenType.ERROR, chrBuf.toString(), chrLine, chrCol);
                return new Token(TokenType.CHAR, chrBuf.toString(), chrLine, chrCol);
            }

    /* newline inside char -> unclosed */
    \n      {
                errors.noCloseChr(chrLine, chrCol, chrBuf.toString());
                yybegin(YYINITIAL);
                return new Token(TokenType.ERROR, chrBuf.toString(), chrLine, chrCol);
            }

    /* any other single character */
    [^\'\\\n]   { chrBuf.append(yytext()); chrCount++; }

    /* EOF inside char */
    <<EOF>> {
                errors.noCloseChr(chrLine, chrCol, chrBuf.toString());
                yybegin(YYINITIAL);
                return new Token(TokenType.ERROR, chrBuf.toString(), chrLine, chrCol);
            }
}


/* ==============================================
 *  YYINITIAL rules  (main scanning state)
 * ============================================== */
<YYINITIAL> {

    /* -------- Multi-character operators (checked first) -------- */
    "++"    { return tok(TokenType.INCREMENT);    }
    "--"    { return tok(TokenType.DECREMENT);    }
    "**"    { return tok(TokenType.POWER);        }
    "=="    { return tok(TokenType.EQUAL);        }
    "!="    { return tok(TokenType.NOT_EQUAL);    }
    "<="    { return tok(TokenType.LESS_EQUAL);   }
    ">="    { return tok(TokenType.GREATER_EQUAL);}
    "&&"    { return tok(TokenType.LOGICAL_AND);  }
    "||"    { return tok(TokenType.LOGICAL_OR);   }
    "+="    { return tok(TokenType.PLUS_ASSIGN);  }
    "-="    { return tok(TokenType.MINUS_ASSIGN); }
    "*="    { return tok(TokenType.MULT_ASSIGN);  }
    "/="    { return tok(TokenType.DIV_ASSIGN);   }

    /* -------- Signed numbers: +digits or -digits -------- */
    "+" {DIGIT}+ "." {DIGIT}{1,6} ([eE][+-]?{DIGIT}+)?  {
                return tok(TokenType.FLOAT);
            }
    "-" {DIGIT}+ "." {DIGIT}{1,6} ([eE][+-]?{DIGIT}+)?  {
                return tok(TokenType.FLOAT);
            }
    "+" {DIGIT}+ "." {DIGIT}{7}  {
                errors.badLit(ln(), col(), yytext(),
                        "Float literal exceeds maximum of 6 decimal digits.");
                return tok(TokenType.ERROR);
            }
    "-" {DIGIT}+ "." {DIGIT}{7}  {
                errors.badLit(ln(), col(), yytext(),
                        "Float literal exceeds maximum of 6 decimal digits.");
                return tok(TokenType.ERROR);
            }
    "+" {DIGIT}+    { return tok(TokenType.INTEGER); }
    "-" {DIGIT}+    { return tok(TokenType.INTEGER); }

    /* -------- Single-character operators (after multi-char) -------- */
    "+"     { return tok(TokenType.PLUS);         }
    "-"     { return tok(TokenType.MINUS);        }
    "*"     { return tok(TokenType.MULTIPLY);     }
    "/"     { return tok(TokenType.DIVIDE);       }
    "%"     { return tok(TokenType.MODULO);       }
    "="     { return tok(TokenType.ASSIGN);       }
    "!"     { return tok(TokenType.LOGICAL_NOT);  }
    "<"     { return tok(TokenType.LESS_THAN);    }
    ">"     { return tok(TokenType.GREATER_THAN); }

    /* -------- Punctuators -------- */
    "("     { return tok(TokenType.LPAREN);    }
    ")"     { return tok(TokenType.RPAREN);    }
    "{"     { return tok(TokenType.LBRACE);    }
    "}"     { return tok(TokenType.RBRACE);    }
    "["     { return tok(TokenType.LBRACKET);  }
    "]"     { return tok(TokenType.RBRACKET);  }
    ","     { return tok(TokenType.COMMA);     }
    ";"     { return tok(TokenType.SEMICOLON); }
    ":"     { return tok(TokenType.COLON);     }

    /* -------- Unsigned float literals -------- */
    {DIGIT}+ "." {DIGIT}{1,6} ([eE][+-]?{DIGIT}+)?  {
                return tok(TokenType.FLOAT);
            }
    /* float with too many decimals */
    {DIGIT}+ "." {DIGIT}{7}  {
                errors.badLit(ln(), col(), yytext(),
                        "Float literal exceeds maximum of 6 decimal digits.");
                return tok(TokenType.ERROR);
            }
    /* float with incomplete exponent */
    {DIGIT}+ "." {DIGIT}{1,6} [eE][+-]?  {
                errors.badLit(ln(), col(), yytext(),
                        "Exponent part of float is incomplete.");
                return tok(TokenType.ERROR);
            }

    /* -------- Unsigned integer literals -------- */
    {DIGIT}+    { return tok(TokenType.INTEGER); }

    /* -------- Keywords (exact match, case-sensitive) -------- */
    "start"     { return tok(TokenType.START);     }
    "finish"    { return tok(TokenType.FINISH);    }
    "loop"      { return tok(TokenType.LOOP);      }
    "condition" { return tok(TokenType.CONDITION);  }
    "declare"   { return tok(TokenType.DECLARE);   }
    "output"    { return tok(TokenType.OUTPUT);    }
    "input"     { return tok(TokenType.INPUT);     }
    "function"  { return tok(TokenType.FUNCTION);  }
    "return"    { return tok(TokenType.RETURN);    }
    "break"     { return tok(TokenType.BREAK);     }
    "continue"  { return tok(TokenType.CONTINUE);  }
    "else"      { return tok(TokenType.ELSE);      }

    /* -------- Boolean literals -------- */
    "true"      { return tok(TokenType.BOOLEAN);   }
    "false"     { return tok(TokenType.BOOLEAN);   }

    /* -------- Valid identifiers: [A-Z][a-z0-9_]{0,30} -------- */
    {UPPER}{ID_TAIL}{0,30}  {
                String id = yytext();
                addSym(id);
                return tok(TokenType.IDENTIFIER);
            }

    /* -------- Identifier too long -------- */
    {UPPER}{ID_TAIL}{31}  {
                errors.longId(ln(), col(), yytext());
                return tok(TokenType.ERROR);
            }

    /* -------- Invalid identifier (starts lowercase or underscore, then letters/digits) -------- */
    ({LOWER} | "_") ({LETTER} | {DIGIT} | "_")*  {
                errors.log("Invalid Identifier", ln(), col(), yytext(),
                        "Identifiers must start with an uppercase letter [A-Z].");
                return tok(TokenType.ERROR);
            }

    /* -------- Invalid identifier (starts uppercase but has uppercase after first char) -------- */
    {UPPER} ({LETTER} | {DIGIT} | "_")*  {
                errors.log("Invalid Identifier", ln(), col(), yytext(),
                        "Identifiers may only contain lowercase letters, digits, and underscores after the first character.");
                return tok(TokenType.ERROR);
            }

    /* -------- Single & or | (invalid) -------- */
    "&"     {
                errors.badChar(ln(), col(), "&");
                return tok(TokenType.ERROR, "&");
            }
    "|"     {
                errors.badChar(ln(), col(), "|");
                return tok(TokenType.ERROR, "|");
            }

    /* -------- Catch-all: any other character is invalid -------- */
    .       {
                errors.badChar(ln(), col(), yytext());
                return tok(TokenType.ERROR);
            }
}

/* -------- EOF -------- */
<<EOF>>     { return tok(TokenType.EOF, "EOF"); }
