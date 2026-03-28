import java.util.*;

/**
 * ErrorHandler.java
 * Handles error detection and recovery during LL(1) parsing.
 * Implements Panic Mode Recovery using FOLLOW sets as synchronizing tokens.
 */
public class ErrorHandler {

    /**
     * Represents a single parsing error.
     */
    public static class ParseError {
        public int lineNumber;
        public int position;
        public String errorType;
        public String message;
        public String expected;
        public String found;

        public ParseError(int lineNumber, int position, String errorType,
                          String message, String expected, String found) {
            this.lineNumber = lineNumber;
            this.position = position;
            this.errorType = errorType;
            this.message = message;
            this.expected = expected;
            this.found = found;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("ERROR [Line ").append(lineNumber)
              .append(", Position ").append(position).append("]: ");
            sb.append(errorType).append("\n");
            sb.append("  ").append(message).append("\n");
            if (expected != null && !expected.isEmpty()) {
                sb.append("  Expected: ").append(expected).append("\n");
            }
            if (found != null && !found.isEmpty()) {
                sb.append("  Found: ").append(found);
            }
            return sb.toString();
        }
    }

    private Grammar grammar;
    private List<ParseError> errors;

    public ErrorHandler(Grammar grammar) {
        this.grammar = grammar;
        this.errors = new ArrayList<>();
    }

    public List<ParseError> getErrors() {
        return errors;
    }

    public int getErrorCount() {
        return errors.size();
    }

    public void clearErrors() {
        errors.clear();
    }

    // -----------------------------------------------------------------------
    // Error Reporting
    // -----------------------------------------------------------------------

    /**
     * Report a terminal mismatch error (expected terminal != found terminal).
     */
    public void reportTerminalMismatch(int lineNum, int pos, String expected, String found) {
        ParseError err = new ParseError(lineNum, pos,
            "Missing Symbol",
            "Expected terminal '" + expected + "' but found '" + found + "'.",
            expected, found);
        errors.add(err);
    }

    /**
     * Report an empty table entry error (no production for M[X, a]).
     */
    public void reportEmptyTableEntry(int lineNum, int pos, String nonTerminal, String found) {
        // Determine what was expected from the parsing table
        Set<String> expectedSet = getExpectedTerminals(nonTerminal);
        String expectedStr = String.join(" or ", expectedSet);

        ParseError err = new ParseError(lineNum, pos,
            "Unexpected Symbol",
            "No production for M[" + nonTerminal + ", " + found + "].",
            expectedStr, found);
        errors.add(err);
    }

    /**
     * Report premature end of input.
     */
    public void reportPrematureEnd(int lineNum, int pos, String stackTop) {
        ParseError err = new ParseError(lineNum, pos,
            "Premature End of Input",
            "Input ended but stack still contains: " + stackTop,
            stackTop, "$");
        errors.add(err);
    }

    // -----------------------------------------------------------------------
    // Panic Mode Recovery
    // -----------------------------------------------------------------------

    /**
     * Perform panic mode recovery when an empty table entry is encountered.
     * Strategy:
     *   1. Find synchronizing tokens = FOLLOW(nonTerminal) ∪ {$}
     *   2. Skip input symbols until a synchronizing token is found
     *   3. Pop the non-terminal from the stack
     *
     * Returns the number of input tokens skipped.
     */
    public int panicModeRecovery(String nonTerminal, List<String> input, int inputPtr,
                                  Stack stack, StringBuilder traceLog) {
        Set<String> syncTokens = new LinkedHashSet<>();

        // Synchronizing tokens = FOLLOW(nonTerminal) + terminals that have entries
        Set<String> followSet = grammar.followSets.getOrDefault(nonTerminal, new LinkedHashSet<>());
        syncTokens.addAll(followSet);

        // Also add terminals that appear in the parsing table row for this NT
        Map<String, Integer> row = grammar.parsingTable.get(nonTerminal);
        if (row != null) {
            for (Map.Entry<String, Integer> entry : row.entrySet()) {
                if (entry.getValue() >= 0) {
                    syncTokens.add(entry.getKey());
                }
            }
        }

        int skipped = 0;
        int currentPtr = inputPtr;

        // Check if current input is already a sync token
        if (currentPtr < input.size()) {
            String currentSym = input.get(currentPtr);
            // If current symbol is in FOLLOW set, pop the non-terminal (insert epsilon)
            if (followSet.contains(currentSym) || currentSym.equals("$")) {
                traceLog.append("  Recovery: Popping '").append(nonTerminal)
                        .append("' (treating as epsilon)\n");
                stack.pop();
                return 0; // No tokens skipped
            }
        }

        // Skip input tokens until a sync token is found
        while (currentPtr < input.size()) {
            String sym = input.get(currentPtr);
            if (syncTokens.contains(sym)) {
                break;
            }
            traceLog.append("  Recovery: Skipping '").append(sym).append("'\n");
            currentPtr++;
            skipped++;
        }

        // Pop the non-terminal from the stack
        if (!stack.isEmpty() && stack.top().equals(nonTerminal)) {
            stack.pop();
            traceLog.append("  Recovery: Popped '").append(nonTerminal).append("' from stack\n");
        }

        return skipped;
    }

    /**
     * Recover from a terminal mismatch.
     * Strategy: Pop the expected terminal from the stack (insert the missing symbol).
     */
    public void recoverTerminalMismatch(Stack stack, StringBuilder traceLog) {
        String expected = stack.pop();
        traceLog.append("  Recovery: Inserted missing '").append(expected).append("'\n");
    }

    // -----------------------------------------------------------------------
    // Helper methods
    // -----------------------------------------------------------------------

    /**
     * Get the set of terminals expected for a given non-terminal
     * (terminals that have entries in the parsing table row).
     */
    private Set<String> getExpectedTerminals(String nonTerminal) {
        Set<String> expected = new LinkedHashSet<>();
        Map<String, Integer> row = grammar.parsingTable.get(nonTerminal);
        if (row != null) {
            for (Map.Entry<String, Integer> entry : row.entrySet()) {
                if (entry.getValue() >= 0) {
                    expected.add(entry.getKey());
                }
            }
        }
        if (expected.isEmpty()) {
            // Fallback: use FIRST set
            Set<String> first = grammar.firstSets.getOrDefault(nonTerminal, new LinkedHashSet<>());
            for (String s : first) {
                if (!s.equals("epsilon")) expected.add(s);
            }
        }
        return expected;
    }

    /**
     * Print all errors collected during parsing.
     */
    public void printErrors() {
        if (errors.isEmpty()) {
            System.out.println("  No errors detected.");
            return;
        }
        System.out.println("\n--- Error Summary ---");
        for (int i = 0; i < errors.size(); i++) {
            System.out.println("Error #" + (i + 1) + ":");
            System.out.println(errors.get(i));
            System.out.println();
        }
        System.out.println("Total errors: " + errors.size());
    }

    /**
     * Save errors to a PrintWriter.
     */
    public void saveErrors(java.io.PrintWriter pw) {
        if (errors.isEmpty()) {
            pw.println("  No errors detected.");
            return;
        }
        pw.println("\n--- Error Summary ---");
        for (int i = 0; i < errors.size(); i++) {
            pw.println("Error #" + (i + 1) + ":");
            pw.println(errors.get(i));
            pw.println();
        }
        pw.println("Total errors: " + errors.size());
    }
}
