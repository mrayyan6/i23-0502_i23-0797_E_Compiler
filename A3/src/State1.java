import java.util.*;

public class State1 {

    private final int stateId;
    private final Set<Item1> items;
    private final Map<String, Integer> transitions;

    public State1(int stateId, Set<Item1> items) {
        this.stateId = stateId;
        this.items = new HashSet<>(items);
        this.transitions = new LinkedHashMap<>();
    }

    public int getStateId() {
        return stateId;
    }

    public Set<Item1> getItems() {
        return Collections.unmodifiableSet(items);
    }

    public Map<String, Integer> getTransitions() {
        return Collections.unmodifiableMap(transitions);
    }

    public void addTransition(String symbol, int targetStateId) {
        transitions.put(symbol, targetStateId);
    }

    public Integer getTransitionTarget(String symbol) {
        return transitions.get(symbol);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof State1)) return false;
        State1 otherState = (State1) other;
        return items.equals(otherState.items);
    }

    @Override
    public int hashCode() {
        return items.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("State ").append(stateId).append(":\n");
        List<Item1> sortedItems = new ArrayList<>(items);
        sortedItems.sort(Comparator.comparing(Item1::toString));
        for (Item1 item : sortedItems) {
            sb.append("  ").append(item).append("\n");
        }
        if (!transitions.isEmpty()) {
            sb.append("  Transitions:\n");
            for (Map.Entry<String, Integer> entry : transitions.entrySet()) {
                sb.append("    ").append(entry.getKey()).append(" -> State ").append(entry.getValue()).append("\n");
            }
        }
        return sb.toString();
    }
}
