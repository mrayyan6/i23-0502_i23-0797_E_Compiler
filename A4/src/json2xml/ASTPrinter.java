package json2xml;

import java.io.PrintStream;

/**
 * ASTPrinter.java — Bonus feature: pretty-print the AST to a stream.
 *
 * Enabled via the  --ast  command-line flag.
 * Output goes to stderr so it doesn't pollute the XML on stdout.
 *
 * Example output for  {"name":"Ali","age":19} :
 *
 *   === AST ===
 *   OBJECT {
 *     KEY "name"
 *       STRING "Ali"
 *     KEY "age"
 *       NUMBER 19
 *   }
 *   ===========
 *
 * CS-4031 Compiler Construction — Assignment 04 (Java)
 */
public class ASTPrinter {

    private final PrintStream out;

    public ASTPrinter(PrintStream out) {
        this.out = out;
    }

    public void print(ASTNode node) {
        out.println("=== AST ===");
        printNode(node, 0);
        out.println("===========");
    }

    private void printNode(ASTNode node, int depth) {
        String pad = "  ".repeat(depth);

        if (node instanceof ASTNode.ObjectNode) {
            ASTNode.ObjectNode obj = (ASTNode.ObjectNode) node;
            out.println(pad + "OBJECT {");
            for (ASTNode.KVPair pair : obj.pairs) {
                out.printf("%s  KEY \"%s\"%n", pad, pair.key);
                printNode(pair.value, depth + 2);
            }
            out.println(pad + "}");

        } else if (node instanceof ASTNode.ArrayNode) {
            ASTNode.ArrayNode arr = (ASTNode.ArrayNode) node;
            out.println(pad + "ARRAY [");
            for (ASTNode item : arr.items) {
                printNode(item, depth + 1);
            }
            out.println(pad + "]");

        } else if (node instanceof ASTNode.StringNode) {
            out.printf("%sSTRING \"%s\"%n", pad, ((ASTNode.StringNode) node).value);

        } else if (node instanceof ASTNode.NumberNode) {
            out.printf("%sNUMBER %s%n",  pad, ((ASTNode.NumberNode) node).value);

        } else if (node instanceof ASTNode.BoolNode) {
            out.printf("%sBOOL %s%n",    pad, ((ASTNode.BoolNode) node).value);

        } else if (node instanceof ASTNode.NullNode) {
            out.printf("%sNULL%n",        pad);
        }
    }
}
