import java.util.Arrays;

public class Item1 {

    private final String[] production;
    private final int dotPosition;
    private final String lookahead;

    public Item1(String[] production, int dotPosition, String lookahead) {
        this.production = production;
        this.dotPosition = dotPosition;
        this.lookahead = lookahead;
    }

    public String[] getProduction() {
        return production;
    }

    public int getDotPosition() {
        return dotPosition;
    }

    public String getLookahead() {
        return lookahead;
    }

    public String getLhs() {
        return production[0];
    }

    public boolean isDotAtEnd() {
        return dotPosition >= production.length - 1;
    }

    public String getSymbolAfterDot() {
        int symbolIndex = dotPosition + 1;
        if (symbolIndex >= production.length) return null;
        return production[symbolIndex];
    }

    public Item1 advance() {
        if (isDotAtEnd()) return null;
        return new Item1(production, dotPosition + 1, lookahead);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Item1)) return false;
        Item1 otherItem = (Item1) other;
        return dotPosition == otherItem.dotPosition
                && lookahead.equals(otherItem.lookahead)
                && Arrays.equals(production, otherItem.production);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(production);
        result = 31 * result + dotPosition;
        result = 31 * result + lookahead.hashCode();
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(production[0]).append(" -> ");
        for (int i = 1; i < production.length; i++) {
            if (i - 1 == dotPosition) sb.append("• ");
            sb.append(production[i]).append(" ");
        }
        if (dotPosition == production.length - 1) sb.append("•");
        sb.append(", ").append(lookahead);
        return sb.toString().trim();
    }
}
