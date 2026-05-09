package json2xml;

/**
 * TokenType.java — All token types produced by the Lexer.
 * CS-4031 Compiler Construction — Assignment 04 (Java)
 */
public enum TokenType {
    // Structural punctuation
    LBRACE,       // {
    RBRACE,       // }
    LBRACKET,     // [
    RBRACKET,     // ]
    COLON,        // :
    COMMA,        // ,

    // Value tokens
    STRING,       // "..."
    NUMBER,       // integer, float, scientific
    TRUE,         // true
    FALSE,        // false
    NULL,         // null

    // Sentinel
    EOF
}
