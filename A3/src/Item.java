import java.util.Arrays;

public class Item {

    private final String[] production;
    private final int dotPosition;

    public Item(String[] production, int dotPosition) {
        this.production = production;
        this.dotPosition = dotPosition;
    }

    public String[] getProduction() {
        return production;
    }

    public int getDotPosition() {
        return dotPosition;
    }

    public boolean isDotAtEnd() {
        return dotPosition >= production.length - 1;
    }

    public String getSymbolAfterDot() {
        int symbolIndex = dotPosition + 1;
        if (symbolIndex >= production.length) return null;
        return production[symbolIndex];
    }

    public Item advance() {
        if (isDotAtEnd()) return null;
        return new Item(production, dotPosition + 1);
    }

    public String getLhs() {
        return production[0];
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Item)) return false;
        Item otherItem = (Item) other;
        return dotPosition == otherItem.dotPosition && Arrays.equals(production, otherItem.production);
    }

    @Override
    public int hashCode() {
        return 31 * Arrays.hashCode(production) + dotPosition;
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
        return sb.toString().trim();
    }
}
