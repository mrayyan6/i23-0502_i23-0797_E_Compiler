import java.util.*;
import java.io.*;

/**
 * Tree.java
 * Parse tree generation and display.
 * Each node holds a symbol (terminal or non-terminal) and children.
 */
public class Tree {

    /**
     * A single node in the parse tree.
     */
    public static class TreeNode {
        public String symbol;
        public List<TreeNode> children;
        public TreeNode parent;

        public TreeNode(String symbol) {
            this.symbol = symbol;
            this.children = new ArrayList<>();
            this.parent = null;
        }

        public void addChild(TreeNode child) {
            child.parent = this;
            children.add(child);
        }

        public boolean isLeaf() {
            return children.isEmpty();
        }
    }

    private TreeNode root;

    public Tree() {
        root = null;
    }

    public TreeNode getRoot() {
        return root;
    }

    public void setRoot(TreeNode node) {
        root = node;
    }

    // -----------------------------------------------------------------------
    // Display: indented text format
    // -----------------------------------------------------------------------
    public void printIndented() {
        if (root == null) {
            System.out.println("  (empty tree)");
            return;
        }
        printIndented(root, "", true);
    }

    private void printIndented(TreeNode node, String prefix, boolean isLast) {
        String connector = isLast ? "└── " : "├── ";
        String label = node.symbol;
        if (node.isLeaf() && !node.symbol.equals("epsilon")) {
            label = "\"" + node.symbol + "\""; // Terminals in quotes
        }
        System.out.println(prefix + connector + label);

        String childPrefix = prefix + (isLast ? "    " : "│   ");
        for (int i = 0; i < node.children.size(); i++) {
            printIndented(node.children.get(i), childPrefix, i == node.children.size() - 1);
        }
    }

    // -----------------------------------------------------------------------
    // Display: ASCII art tree
    // -----------------------------------------------------------------------
    public String toASCII() {
        if (root == null) return "(empty tree)";
        StringBuilder sb = new StringBuilder();
        buildASCII(root, "", true, sb);
        return sb.toString();
    }

    private void buildASCII(TreeNode node, String prefix, boolean isLast, StringBuilder sb) {
        String connector = isLast ? "└── " : "├── ";
        String label = node.symbol;
        if (node.isLeaf() && !node.symbol.equals("epsilon")) {
            label = "\"" + node.symbol + "\"";
        }
        sb.append(prefix).append(connector).append(label).append("\n");

        String childPrefix = prefix + (isLast ? "    " : "│   ");
        for (int i = 0; i < node.children.size(); i++) {
            buildASCII(node.children.get(i), childPrefix, i == node.children.size() - 1, sb);
        }
    }

    // -----------------------------------------------------------------------
    // Preorder traversal
    // -----------------------------------------------------------------------
    public List<String> preorder() {
        List<String> result = new ArrayList<>();
        preorder(root, result);
        return result;
    }

    private void preorder(TreeNode node, List<String> result) {
        if (node == null) return;
        result.add(node.symbol);
        for (TreeNode child : node.children) {
            preorder(child, result);
        }
    }

    // -----------------------------------------------------------------------
    // Postorder traversal
    // -----------------------------------------------------------------------
    public List<String> postorder() {
        List<String> result = new ArrayList<>();
        postorder(root, result);
        return result;
    }

    private void postorder(TreeNode node, List<String> result) {
        if (node == null) return;
        for (TreeNode child : node.children) {
            postorder(child, result);
        }
        result.add(node.symbol);
    }

    // -----------------------------------------------------------------------
    // Save tree to file
    // -----------------------------------------------------------------------
    public void saveToFile(PrintWriter pw, String header) {
        pw.println(header);
        if (root == null) {
            pw.println("  (empty tree)");
            return;
        }
        StringBuilder sb = new StringBuilder();
        buildASCII(root, "", true, sb);
        pw.print(sb.toString());

        pw.println("\nPreorder:  " + String.join(" ", preorder()));
        pw.println("Postorder: " + String.join(" ", postorder()));
    }

    // -----------------------------------------------------------------------
    // DOT format for Graphviz (optional/bonus)
    // -----------------------------------------------------------------------
    public String toDOT() {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph ParseTree {\n");
        sb.append("  node [shape=box];\n");
        int[] counter = {0};
        if (root != null) {
            buildDOT(root, counter, sb);
        }
        sb.append("}\n");
        return sb.toString();
    }

    private int buildDOT(TreeNode node, int[] counter, StringBuilder sb) {
        int myId = counter[0]++;
        String label = node.symbol.replace("\"", "\\\"");
        if (node.isLeaf() && !node.symbol.equals("epsilon")) {
            sb.append("  n").append(myId).append(" [label=\"").append(label)
              .append("\", shape=ellipse, style=filled, fillcolor=lightblue];\n");
        } else if (node.symbol.equals("epsilon")) {
            sb.append("  n").append(myId).append(" [label=\"ε\", shape=plain];\n");
        } else {
            sb.append("  n").append(myId).append(" [label=\"").append(label).append("\"];\n");
        }
        for (TreeNode child : node.children) {
            int childId = buildDOT(child, counter, sb);
            sb.append("  n").append(myId).append(" -> n").append(childId).append(";\n");
        }
        return myId;
    }
}
