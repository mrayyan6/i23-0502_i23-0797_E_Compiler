import java.util.*;

/**
 * FirstFollow.java
 * Provides a clean interface for FIRST and FOLLOW set computation.
 * Delegates to Grammar methods but can be used independently.
 */
public class FirstFollow {

    private Grammar grammar;

    public FirstFollow(Grammar grammar) {
        this.grammar = grammar;
    }

    /**
     * Compute both FIRST and FOLLOW sets for the grammar.
     */
    public void computeAll() {
        grammar.computeFirstSets();
        grammar.computeFollowSets();
    }

    /**
     * Get FIRST set for a non-terminal.
     */
    public Set<String> getFirst(String nonTerminal) {
        return grammar.firstSets.getOrDefault(nonTerminal, new LinkedHashSet<>());
    }

    /**
     * Get FOLLOW set for a non-terminal.
     */
    public Set<String> getFollow(String nonTerminal) {
        return grammar.followSets.getOrDefault(nonTerminal, new LinkedHashSet<>());
    }

    /**
     * Get FIRST of a sequence of symbols.
     */
    public Set<String> getFirstOfSequence(List<String> seq) {
        return grammar.computeFirstOfSequence(seq);
    }

    /**
     * Display FIRST sets.
     */
    public void printFirstSets() {
        grammar.printFirstSets();
    }

    /**
     * Display FOLLOW sets.
     */
    public void printFollowSets() {
        grammar.printFollowSets();
    }
}
