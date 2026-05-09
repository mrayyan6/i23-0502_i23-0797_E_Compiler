package json2xml;

import java.util.List;
import java.util.ArrayList;

/**
 * ASTNode.java — Abstract Syntax Tree node hierarchy.
 *
 * Node types:
 *   ObjectNode   — { key: value, ... }
 *   ArrayNode    — [ value, ... ]
 *   StringNode   — "text"
 *   NumberNode   — 42, 3.14, -1e5
 *   BoolNode     — true | false
 *   NullNode     — null
 *
 * CS-4031 Compiler Construction — Assignment 04 (Java)
 */
public abstract class ASTNode {

    // ── Abstract interface ───────────────────────────────────────────────

    /** Return this node's type label (used in AST debug printing). */
    public abstract String typeName();

    // ═══════════════════════════════════════════════════════════════════
    //  Concrete node types
    // ═══════════════════════════════════════════════════════════════════

    // ── Key-value pair (used inside ObjectNode) ──────────────────────────
    public static class KVPair {
        public final String  key;
        public final ASTNode value;
        public KVPair(String key, ASTNode value) {
            this.key   = key;
            this.value = value;
        }
    }

    // ── Object node ──────────────────────────────────────────────────────
    public static class ObjectNode extends ASTNode {
        public final List<KVPair> pairs;
        public ObjectNode(List<KVPair> pairs) {
            this.pairs = pairs;
        }
        @Override public String typeName() { return "OBJECT"; }
    }

    // ── Array node ───────────────────────────────────────────────────────
    public static class ArrayNode extends ASTNode {
        public final List<ASTNode> items;
        public ArrayNode(List<ASTNode> items) {
            this.items = items;
        }
        @Override public String typeName() { return "ARRAY"; }
    }

    // ── String node ──────────────────────────────────────────────────────
    public static class StringNode extends ASTNode {
        public final String value;
        public StringNode(String value) { this.value = value; }
        @Override public String typeName() { return "STRING"; }
    }

    // ── Number node ──────────────────────────────────────────────────────
    public static class NumberNode extends ASTNode {
        public final String value;   // stored as-is from the source
        public NumberNode(String value) { this.value = value; }
        @Override public String typeName() { return "NUMBER"; }
    }

    // ── Boolean node ─────────────────────────────────────────────────────
    public static class BoolNode extends ASTNode {
        public final boolean value;
        public BoolNode(boolean value) { this.value = value; }
        @Override public String typeName() { return "BOOL"; }
    }

    // ── Null node ────────────────────────────────────────────────────────
    public static class NullNode extends ASTNode {
        @Override public String typeName() { return "NULL"; }
    }

    // ── Factory helpers ──────────────────────────────────────────────────

    public static ObjectNode object(List<KVPair> pairs) { return new ObjectNode(pairs); }
    public static ArrayNode  array(List<ASTNode> items) { return new ArrayNode(items);  }
    public static StringNode string(String v)           { return new StringNode(v);     }
    public static NumberNode number(String v)           { return new NumberNode(v);     }
    public static BoolNode   bool(boolean v)            { return new BoolNode(v);       }
    public static NullNode   nullNode()                 { return new NullNode();        }
}
