package json2xml;

import java.io.PrintStream;

/**
 * XMLGenerator.java — Traverses the AST and emits well-formed, pretty-printed XML.
 *
 * Conversion rules
 * ─────────────────
 *   Rule 1  Top-level value wrapped in <root> … </root>
 *   Rule 2  JSON object  → XML element whose children come from key-value pairs
 *   Rule 3  JSON key     → XML tag name
 *   Rule 4  Scalar       → text content inside the tag
 *   Rule 5  null         → empty/self-closing element  <tag/>
 *   Rule 6  Array        → repeated <item> children
 *   Rule 7  Nesting      → preserved recursively
 *
 * XML special characters in text content are escaped:
 *   &  →  &amp;
 *   <  →  &lt;
 *   >  →  &gt;
 *   "  →  &quot;
 *   '  →  &apos;
 *
 * CS-4031 Compiler Construction — Assignment 04 (Java)
 */
public class XMLGenerator {

    private static final int INDENT_SIZE = 2;

    private final PrintStream out;

    public XMLGenerator(PrintStream out) {
        this.out = out;
    }

    // ────────────────────────────────────────────────────────────────────
    //  Entry point
    // ────────────────────────────────────────────────────────────────────

    /**
     * Generate XML for the root AST node.
     * Top-level objects have their key-value pairs emitted directly under
     * &lt;root&gt; to avoid &lt;root&gt;&lt;root&gt;…&lt;/root&gt;&lt;/root&gt;.
     */
    public void generate(ASTNode root) {
        if (root instanceof ASTNode.ObjectNode) {
            out.println("<root>");
            ASTNode.ObjectNode obj = (ASTNode.ObjectNode) root;
            for (ASTNode.KVPair pair : obj.pairs) {
                emit(pair.value, pair.key, 1);
            }
            out.println("</root>");
        } else {
            // Bare array or scalar at top level
            emit(root, "root", 0);
        }
    }

    // ────────────────────────────────────────────────────────────────────
    //  Recursive emitter
    // ────────────────────────────────────────────────────────────────────

    private void emit(ASTNode node, String tag, int depth) {
        String pad = indent(depth);

        if (node instanceof ASTNode.NullNode) {
            // Rule 5: null → <tag/>
            out.printf("%s<%s/>%n", pad, tag);

        } else if (node instanceof ASTNode.StringNode) {
            // Rule 4: string scalar
            String text = xmlEscape(((ASTNode.StringNode) node).value);
            out.printf("%s<%s>%s</%s>%n", pad, tag, text, tag);

        } else if (node instanceof ASTNode.NumberNode) {
            // Rule 4: number scalar (no escaping needed for numbers)
            out.printf("%s<%s>%s</%s>%n", pad, tag, ((ASTNode.NumberNode) node).value, tag);

        } else if (node instanceof ASTNode.BoolNode) {
            // Rule 4: boolean scalar
            String boolStr = ((ASTNode.BoolNode) node).value ? "true" : "false";
            out.printf("%s<%s>%s</%s>%n", pad, tag, boolStr, tag);

        } else if (node instanceof ASTNode.ObjectNode) {
            // Rule 2 & 7: object → element with children
            ASTNode.ObjectNode obj = (ASTNode.ObjectNode) node;
            out.printf("%s<%s>%n", pad, tag);
            for (ASTNode.KVPair pair : obj.pairs) {
                emit(pair.value, pair.key, depth + 1);
            }
            out.printf("%s</%s>%n", pad, tag);

        } else if (node instanceof ASTNode.ArrayNode) {
            // Rule 6: array → repeated <item> elements
            ASTNode.ArrayNode arr = (ASTNode.ArrayNode) node;
            out.printf("%s<%s>%n", pad, tag);
            for (ASTNode item : arr.items) {
                emit(item, "item", depth + 1);
            }
            out.printf("%s</%s>%n", pad, tag);
        }
    }

    // ────────────────────────────────────────────────────────────────────
    //  Utilities
    // ────────────────────────────────────────────────────────────────────

    private String indent(int depth) {
        return " ".repeat(depth * INDENT_SIZE);
    }

    /**
     * Escape the five XML special characters in text content.
     * Must handle &amp; first to avoid double-escaping.
     */
    private String xmlEscape(String s) {
        // Order matters: & must be first
        s = s.replace("&",  "&amp;");
        s = s.replace("<",  "&lt;");
        s = s.replace(">",  "&gt;");
        s = s.replace("\"", "&quot;");
        s = s.replace("'",  "&apos;");
        return s;
    }
}
