import java.util.*;

public class State {

    private final int stateId;
    private final Set<Item> items;
    private final Map<String, Integer> transitions;

    public State(int stateId, Set<Item> items) {
        this.stateId = stateId;
        this.items = new HashSet<>(items);
        this.transitions = new LinkedHashMap<>();
    }

    public int getStateId() {
        return stateId;
    }

    public Set<Item> getItems() {
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

    public boolean hasSameCore(Set<Item> otherItems) {
        return items.equals(otherItems);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("State ").append(stateId).append(":\n");
        for (Item item : items) {
            sb.append("  ").append(item).append("\n");
        }
        if (!transitions.isEmpty()) {
            sb.append("  Transitions: ").append(transitions).append("\n");
        }
        return sb.toString();
    }
}
