import java.io.IOException;

public class Main1 {

    public static void main(String[] args) {
        String grammarFilePath = args.length > 0 ? args[0] : "grammar.txt";
        String outputFilePath = args.length > 1 ? args[1] : "lr1_states.txt";

        try {
            Grammar grammar = new Grammar(grammarFilePath);
            FirstFollow firstFollow = new FirstFollow(grammar);
            LR1CollectionBuilder builder = new LR1CollectionBuilder(grammar, firstFollow);

            builder.printCanonicalCollection();

            builder.saveStatesToFile(outputFilePath);
            System.out.println("States written to: " + outputFilePath);

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
