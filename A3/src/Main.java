import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class Main {

    public static void main(String[] args) {
        String grammarFilePath = args.length > 0 ? args[0] : "grammar.txt";

        try {
            Grammar grammar = new Grammar(grammarFilePath);

            System.out.println("===== Grammar =====");
            System.out.println("Start symbol : " + grammar.getStartSymbol());
            System.out.println("Augmented start: " + grammar.getAugmentedStart());
            System.out.println("Non-terminals: " + grammar.getNonTerminals());
            System.out.println("Terminals    : " + grammar.getTerminals());
            System.out.println("Productions  :");
            for (String[] prod : grammar.getProductions()) {
                StringBuilder sb = new StringBuilder(prod[0] + " ->");
                for (int i = 1; i < prod.length; i++) sb.append(" ").append(prod[i]);
                System.out.println("  " + sb);
            }

            FirstFollow firstFollow = new FirstFollow(grammar);

            System.out.println("\n===== FIRST Sets =====");
            for (Map.Entry<String, Set<String>> entry : firstFollow.getAllFirstSets().entrySet()) {
                System.out.println("FIRST(" + entry.getKey() + ") = " + entry.getValue());
            }

            System.out.println("\n===== FOLLOW Sets =====");
            for (Map.Entry<String, Set<String>> entry : firstFollow.getAllFollowSets().entrySet()) {
                System.out.println("FOLLOW(" + entry.getKey() + ") = " + entry.getValue());
            }

            SLRBuilder slrBuilder = new SLRBuilder(grammar, firstFollow);
            slrBuilder.printCanonicalCollection();
            slrBuilder.printParsingTable();

        } catch (IOException e) {
            System.err.println("Error reading grammar file: " + e.getMessage());
        }
    }
}
