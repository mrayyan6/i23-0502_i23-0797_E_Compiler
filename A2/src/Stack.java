import java.util.*;

/**
 * Stack.java
 * Stack implementation for LL(1) parser using ArrayList.
 * Provides push, pop, top, isEmpty, and display operations.
 */
public class Stack {
    private List<String> data;

    public Stack() {
        data = new ArrayList<>();
    }

    public void push(String symbol) {
        data.add(symbol);
    }

    public String pop() {
        if (data.isEmpty()) return "";
        return data.remove(data.size() - 1);
    }

    public String top() {
        if (data.isEmpty()) return "";
        return data.get(data.size() - 1);
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    public int size() {
        return data.size();
    }

    public void clear() {
        data.clear();
    }

    /**
     * Returns stack contents from bottom to top as a string.
     */
    public String contents() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.size(); i++) {
            if (i > 0) sb.append(" ");
            sb.append(data.get(i));
        }
        return sb.toString();
    }
}
