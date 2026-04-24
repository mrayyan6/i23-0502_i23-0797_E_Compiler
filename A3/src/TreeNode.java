import java.util.*;

/**
 * TreeNode
 * -----------------------------------------------------------------------------
 * Part 8 of the assignment: a node in the parse tree.
 *
 * The parser builds the parse tree bottom-up during reductions:
 *   - When a terminal is shifted, a leaf node is pushed onto a parallel
 *     "tree stack" alongside the normal parsing stack.
 *   - When a reduction A -> X1 X2 ... Xn happens, the top n tree nodes are
 *     popped, collected as children, and a new internal node for A is pushed.
 *   - When accept fires, the single remaining tree node (below state 0) is
 *     the root of the complete parse tree.
 *
 * Rendering is done with the classic "pipes and tees" ASCII style so the tree
 * can be printed straight to a text file or to stdout.
 * -----------------------------------------------------------------------------
 */
public class TreeNode {

    private final String symbol;             // terminal or non-terminal label
    private final boolean terminal;          // true for leaves (shifted tokens)
    private final List<TreeNode> children = new ArrayList<>();

    public TreeNode(String symbol, boolean terminal) {
        this.symbol   = symbol;
        this.terminal = terminal;
    }

    public String         getSymbol()   { return symbol; }
    public boolean        isTerminal()  { return terminal; }
    public List<TreeNode> getChildren() { return children; }

    public void addChild(TreeNode child) { children.add(child); }

    /** Convenience: renders the tree into a single newline-separated string. */
    public String render() {
        StringBuilder sb = new StringBuilder();
        // Root line: no connector in front of it
        sb.append(symbol);
        if (terminal) sb.append("  (terminal)");
        sb.append('\n');
        for (int i = 0; i < children.size(); i++) {
            children.get(i).renderInto(sb, "", i == children.size() - 1);
        }
        return sb.toString();
    }

    private void renderInto(StringBuilder sb, String prefix, boolean isLast) {
        // This node's own line: parent prefix + tee/corner + label
        sb.append(prefix).append(isLast ? "└── " : "├── ").append(symbol);
        if (terminal) sb.append("  (terminal)");
        sb.append('\n');

        // Children inherit the parent prefix plus either a vertical bar or spaces
        String childPrefix = prefix + (isLast ? "    " : "│   ");
        for (int i = 0; i < children.size(); i++) {
            children.get(i).renderInto(sb, childPrefix, i == children.size() - 1);
        }
    }

    @Override
    public String toString() { return render(); }
}