package json2xml;

import java.io.*;
import java.util.List;

/**
 * Main.java — Entry point for the JSON to XML Translator.
 *
 * Usage:
 *   ./json2xml < input.json            (basic)
 *   ./json2xml --ast < input.json      (+ AST debug dump on stderr)
 *
 * Pipeline:
 *   stdin → Lexer → List<Token> → Parser → ASTNode → XMLGenerator → stdout
 *
 * Errors are printed to stderr as:
 *   Error: <description> at line L, column C
 *
 * CS-4031 Compiler Construction — Assignment 04 (Java)
 */
public class Main {

    public static void main(String[] args) {

        // ── Parse optional flags ─────────────────────────────────────
        boolean printAst = false;
        for (String arg : args) {
            if (arg.equals("--ast")) printAst = true;
        }

        // ── Read all of stdin ────────────────────────────────────────
        String source;
        try {
            source = new String(System.in.readAllBytes());
        } catch (IOException e) {
            System.err.println("Error: could not read input: " + e.getMessage());
            System.exit(1);
            return;
        }

        if (source.isBlank()) {
            System.err.println("Error: empty input");
            System.exit(1);
            return;
        }

        // ── Phase 1: Lexical analysis ────────────────────────────────
        List<Token> tokens;
        try {
            Lexer lexer = new Lexer(source);
            tokens = lexer.tokenize();
        } catch (Lexer.LexerException e) {
            System.err.printf("Error: %s at line %d, column %d%n",
                e.getMessage(), e.line, e.column);
            System.exit(1);
            return;
        }

        // ── Phase 2: Parsing → AST ───────────────────────────────────
        ASTNode root;
        try {
            Parser parser = new Parser(tokens);
            root = parser.parse();
        } catch (Parser.ParserException e) {
            System.err.printf("Error: %s at line %d, column %d%n",
                e.getMessage(), e.line, e.column);
            System.exit(1);
            return;
        }

        // ── Optional AST dump ────────────────────────────────────────
        if (printAst) {
            new ASTPrinter(System.err).print(root);
        }

        // ── Phase 3: XML generation ──────────────────────────────────
        new XMLGenerator(System.out).generate(root);
    }
}
