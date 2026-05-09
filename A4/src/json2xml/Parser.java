package json2xml;

import java.util.ArrayList;
import java.util.List;

/**
 * Parser.java — Recursive-descent parser for JSON.
 *
 * Equivalent role to parser.y (Bison/Yacc) in the C version.
 * Consumes the token list produced by the Lexer and builds an AST.
 *
 * Grammar (EBNF)
 * ──────────────
 *   start   → value EOF
 *   value   → object | array | STRING | NUMBER | TRUE | FALSE | NULL
 *   object  → '{' '}'
 *            | '{' pair (',' pair)* '}'
 *   pair    → STRING ':' value
 *   array   → '[' ']'
 *            | '[' value (',' value)* ']'
 *
 * Syntax errors throw a ParserException which Main catches and prints as:
 *   "Error: unexpected token '<type>' at line L, column C"
 *
 * CS-4031 Compiler Construction — Assignment 04 (Java)
 */
public class Parser {

    private final List<Token> tokens;
    private       int         pos = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    // ────────────────────────────────────────────────────────────────────
    //  Public API
    // ────────────────────────────────────────────────────────────────────

    /** Parse the token stream and return the root AST node. */
    public ASTNode parse() {
        ASTNode root = parseValue();
        expect(TokenType.EOF);
        return root;
    }

    // ────────────────────────────────────────────────────────────────────
    //  Grammar rules
    // ────────────────────────────────────────────────────────────────────

    /**
     * value → object | array | STRING | NUMBER | TRUE | FALSE | NULL
     */
    private ASTNode parseValue() {
        Token t = peek();
        switch (t.type) {
            case LBRACE:   return parseObject();
            case LBRACKET: return parseArray();
            case STRING:   advance(); return ASTNode.string(t.value);
            case NUMBER:   advance(); return ASTNode.number(t.value);
            case TRUE:     advance(); return ASTNode.bool(true);
            case FALSE:    advance(); return ASTNode.bool(false);
            case NULL:     advance(); return ASTNode.nullNode();
            default:
                throw new ParserException(
                    "unexpected token '" + t.type + "'", t.line, t.column);
        }
    }

    /**
     * object → '{' '}'
     *         | '{' pair (',' pair)* '}'
     */
    private ASTNode.ObjectNode parseObject() {
        expect(TokenType.LBRACE);
        List<ASTNode.KVPair> pairs = new ArrayList<>();

        if (peek().type == TokenType.RBRACE) {
            advance(); // empty object
            return ASTNode.object(pairs);
        }

        pairs.add(parsePair());
        while (peek().type == TokenType.COMMA) {
            advance(); // consume ','
            pairs.add(parsePair());
        }

        expect(TokenType.RBRACE);
        return ASTNode.object(pairs);
    }

    /**
     * pair → STRING ':' value
     */
    private ASTNode.KVPair parsePair() {
        Token keyTok = peek();
        if (keyTok.type != TokenType.STRING) {
            throw new ParserException(
                "expected string key, got '" + keyTok.type + "'",
                keyTok.line, keyTok.column);
        }
        advance(); // consume key
        expect(TokenType.COLON);
        ASTNode value = parseValue();
        return new ASTNode.KVPair(keyTok.value, value);
    }

    /**
     * array → '[' ']'
     *        | '[' value (',' value)* ']'
     */
    private ASTNode.ArrayNode parseArray() {
        expect(TokenType.LBRACKET);
        List<ASTNode> items = new ArrayList<>();

        if (peek().type == TokenType.RBRACKET) {
            advance(); // empty array
            return ASTNode.array(items);
        }

        items.add(parseValue());
        while (peek().type == TokenType.COMMA) {
            advance(); // consume ','
            items.add(parseValue());
        }

        expect(TokenType.RBRACKET);
        return ASTNode.array(items);
    }

    // ────────────────────────────────────────────────────────────────────
    //  Token helpers
    // ────────────────────────────────────────────────────────────────────

    private Token peek() {
        return tokens.get(pos);
    }

    private Token advance() {
        Token t = tokens.get(pos);
        if (t.type != TokenType.EOF) pos++;
        return t;
    }

    private Token expect(TokenType type) {
        Token t = peek();
        if (t.type != type) {
            throw new ParserException(
                "expected '" + type + "' but got '" + t.type + "'",
                t.line, t.column);
        }
        return advance();
    }

    // ────────────────────────────────────────────────────────────────────
    //  Exception
    // ────────────────────────────────────────────────────────────────────

    public static class ParserException extends RuntimeException {
        public final int line;
        public final int column;

        public ParserException(String msg, int line, int col) {
            super(msg);
            this.line   = line;
            this.column = col;
        }
    }
}
