import java.util.*;

public class ErrorHandler {

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
    private FirstFollow firstFollow;
    private Map<String, Map<String, List<String>>> parsingTable;
    private List<ParseError> errors;

    public ErrorHandler(Grammar grammar) {
        this.grammar = grammar;
        this.firstFollow = null;
        this.parsingTable = null;
        this.errors = new ArrayList<>();
    }

    public ErrorHandler(Grammar grammar, FirstFollow firstFollow,
                        Map<String, Map<String, List<String>>> parsingTable) {
        this.grammar = grammar;
        this.firstFollow = firstFollow;
        this.parsingTable = parsingTable;
        this.errors = new ArrayList<>();
    }

    public void setContext(FirstFollow firstFollow,
                           Map<String, Map<String, List<String>>> parsingTable) {
        this.firstFollow = firstFollow;
        this.parsingTable = parsingTable;
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

    public void reportTerminalMismatch(int lineNum, int pos, String expected, String found) {
        ParseError err = new ParseError(lineNum, pos,
            "Missing Symbol",
            "Expected terminal '" + expected + "' but found '" + found + "'.",
            expected, found);
        errors.add(err);
    }

    public void reportEmptyTableEntry(int lineNum, int pos, String nonTerminal, String found) {
        Set<String> expectedSet = getExpectedTerminals(nonTerminal);
        String expectedStr = String.join(" or ", expectedSet);

        ParseError err = new ParseError(lineNum, pos,
            "Unexpected Symbol",
            "No production for M[" + nonTerminal + ", " + found + "].",
            expectedStr, found);
        errors.add(err);
    }

    public void reportPrematureEnd(int lineNum, int pos, String stackTop) {
        ParseError err = new ParseError(lineNum, pos,
            "Premature End of Input",
            "Input ended but stack still contains: " + stackTop,
            stackTop, "$");
        errors.add(err);
    }

    public int panicModeRecovery(String nonTerminal, List<String> input, int inputPtr,
                                  Stack stack, StringBuilder traceLog) {
        Set<String> syncTokens = new LinkedHashSet<>();

        Set<String> followSet = new LinkedHashSet<>();
        if (firstFollow != null) {
            followSet = firstFollow.getFollow().getOrDefault(nonTerminal, new LinkedHashSet<>());
        }
        syncTokens.addAll(followSet);

        Map<String, List<String>> row = null;
        if (parsingTable != null) {
            row = parsingTable.get(nonTerminal);
        }
        if (row != null) {
            for (Map.Entry<String, List<String>> entry : row.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    syncTokens.add(entry.getKey());
                }
            }
        }

        int skipped = 0;
        int currentPtr = inputPtr;

        if (currentPtr < input.size()) {
            String currentSym = input.get(currentPtr);
            if (followSet.contains(currentSym) || currentSym.equals("$")) {
                traceLog.append("  Recovery: Popping '").append(nonTerminal)
                        .append("' (treating as epsilon)\n");
                stack.pop();
                return 0; // No tokens skipped
            }
        }

        while (currentPtr < input.size()) {
            String sym = input.get(currentPtr);
            if (syncTokens.contains(sym)) {
                break;
            }
            traceLog.append("  Recovery: Skipping '").append(sym).append("'\n");
            currentPtr++;
            skipped++;
        }

        if (!stack.isEmpty() && stack.top().equals(nonTerminal)) {
            stack.pop();
            traceLog.append("  Recovery: Popped '").append(nonTerminal).append("' from stack\n");
        }

        return skipped;
    }

    public void recoverTerminalMismatch(Stack stack, StringBuilder traceLog) {
        String expected = stack.pop();
        traceLog.append("  Recovery: Inserted missing '").append(expected).append("'\n");
    }

    private Set<String> getExpectedTerminals(String nonTerminal) {
        Set<String> expected = new LinkedHashSet<>();
        Map<String, List<String>> row = null;
        if (parsingTable != null) {
            row = parsingTable.get(nonTerminal);
        }
        if (row != null) {
            for (Map.Entry<String, List<String>> entry : row.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    expected.add(entry.getKey());
                }
            }
        }
        if (expected.isEmpty()) {
            if (firstFollow != null) {
                Set<String> first = firstFollow.getFirst().getOrDefault(nonTerminal, new LinkedHashSet<>());
                for (String s : first) {
                    if (!s.equals("epsilon")) {
                        expected.add(s);
                    }
                }
            }
        }
        return expected;
    }

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
