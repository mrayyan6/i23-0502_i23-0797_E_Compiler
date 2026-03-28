import java.util.*;

/**
 * Production.java
 * Represents a single production rule: LHS -> RHS
 */
public class Production {
    public String lhs;               // Left-hand side non-terminal
    public List<String> rhs;         // Right-hand side symbols

    public Production(String lhs, List<String> rhs) {
        this.lhs = lhs;
        this.rhs = new ArrayList<>(rhs);
    }

    @Override
    public String toString() {
        return lhs + " -> " + String.join(" ", rhs);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Production)) return false;
        Production p = (Production) o;
        return lhs.equals(p.lhs) && rhs.equals(p.rhs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lhs, rhs);
    }
}
