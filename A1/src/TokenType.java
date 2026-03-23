
public enum TokenType 
{

    START, FINISH, LOOP, CONDITION, DECLARE, OUTPUT, INPUT,
    FUNCTION, RETURN, BREAK, CONTINUE, ELSE,

    INTEGER,       // [+-]?[0-9]+
    FLOAT,         // [+-]?[0-9]+\.[0-9]{1,6}([eE][+-]?[0-9]+)?
    STRING,        // $ ... $
    CHAR,          // ' ... '
    BOOLEAN,       // true | false

    IDENTIFIER,    // [A-Z][a-z0-9_]{0,30}

    PLUS,          // +
    MINUS,         // -
    MULTIPLY,      // *
    DIVIDE,        // /
    MODULO,        // %
    POWER,         // **
    EQUAL,         // ==
    NOT_EQUAL,     // !=
    LESS_EQUAL,    // <=
    GREATER_EQUAL, // >=
    LESS_THAN,     // <
    GREATER_THAN,  // >

    LOGICAL_AND,   // &&
    LOGICAL_OR,    // ||
    LOGICAL_NOT,   // !
    ASSIGN,        // =
    PLUS_ASSIGN,   // +=
    MINUS_ASSIGN,  // -=
    MULT_ASSIGN,   // *=
    DIV_ASSIGN,    // /=

    INCREMENT,     // ++
    DECREMENT,     // --

    LPAREN,        // (
    RPAREN,        // )
    LBRACE,        // {
    RBRACE,        // }
    LBRACKET,      // [
    RBRACKET,      // ]
    COMMA,         // ,
    SEMICOLON,     // ;
    COLON,         // :

    EOF,           // end of file
    ERROR          // bad token
}
