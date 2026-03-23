import java.util.HashMap;
import java.util.Map;


public class SymbolTable 
{

    public static class Entry 
    {
        private String name;
        private String type;
        private String scope;
        private int    ln;

        public Entry(String name, String type, String scope, int ln) 
        {
            this.name  = name;
            this.type  = type;
            this.scope = scope;
            this.ln    = ln;
        }

        public String name()  { return name;  }
        public String type()  { return type;  }
        public String scope() { return scope; }
        public int    ln()    { return ln;    }

        public void setType(String t)  { this.type  = t; }
        public void setScope(String s) { this.scope = s; }

        @Override
        public String toString() 
        {
            return String.format("%-20s %-12s %-12s %d", name, type, scope, ln);
        }
    }

    private Map<String, Entry> tbl;

    public SymbolTable() 
    {
        tbl = new HashMap<>();
    }

    // add new id, first wins
    public boolean add(String name, String type, String scope, int ln) 
    {
        if (tbl.containsKey(name)) 
        {
            return false;
        }
        tbl.put(name, new Entry(name, type, scope, ln));
        return true;
    }

    // find by name
    public Entry find(String name) 
    {
        return tbl.get(name);
    }

    // prints table
    public void show() 
    {
        System.out.println("\n||============================================================||");
        System.out.println("||                        SYMBOL TABLE                        ||");
        System.out.println("||============================================================||");
        System.out.printf("|| %-20s %-12s %-12s %-6s ||%n", "Name", "Type", "Scope", "Line");
        System.out.println("||============================================================||");

        if (tbl.isEmpty()) 
        {
            System.out.println("||                     (empty table)                          ||");
        } 
        else 
        {            
            for (Entry e : tbl.values()) 
            {
                System.out.printf("|| %-20s %-12s %-12s %-6d ||%n",
                        e.name(), e.type(), e.scope(), e.ln());
            }
        }

        System.out.println("||============================================================||");
    }

    public int size() 
    {
        return tbl.size();
    }
}
