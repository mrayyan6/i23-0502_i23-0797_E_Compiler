import java.io.*;
import java.util.*;

public class Parser 
{
    private Grammar g; // grammar
    private FirstFollow ff; // firstFollow
    private Map<String, Map<String, List<String>>> tbl; // parsingTable
    private List<String> cols; // 
    private boolean hasCnf; // conflict
    private List<String> cnfMsgs; // conflict msg

    public Parser(Grammar g, FirstFollow ff) 
    {
        this.g = g;
        this.ff = ff;
        this.tbl = new LinkedHashMap<>();
        this.hasCnf = false;
        this.cnfMsgs = new ArrayList<>();

        cols = new ArrayList<>(g.getTerms());
        if (!cols.contains("$")) 
        {
            cols.add("$");
        }

        for (String nt : g.getNtOrder()) 
        {
            Map<String, List<String>> row = new LinkedHashMap<>();
            for (String col : cols) 
            {
                row.put(col, null); 
            }
            tbl.put(nt, row);
        }
        buildTbl();
    }

    private void buildTbl()
    {
        Map<String, Set<String>> follow = ff.getFollow();

        for (String nt : g.getNtOrder())
        {
            List<List<String>> alts = g.getProd().get(nt);
            if (alts == null)
            {
                continue;
            }

            for (List<String> alt : alts) 
            {
                String prodStr = nt + " -> " + String.join(" ", alt);
                Set<String> firstAlt = ff.firstOfSeq(alt);

                for (String sym : firstAlt) 
                {
                    if (!sym.equals("epsilon")) 
                    {
                        putCell(nt, sym, prodStr);
                    }
                }

                if (firstAlt.contains("epsilon")) 
                {
                    Set<String> fset = follow.get(nt);
                    if (fset != null) 
                    {
                        for (String fSym : fset) 
                        {
                            putCell(nt, fSym, prodStr);
                        }
                    }
                }
            }
        }
    }

    private void putCell(String nt, String term, String p)
    {
        Map<String, List<String>> row = tbl.get(nt);
        if (row == null)
        {
            return;
        }

        if (!cols.contains(term))
        {
            return;
        }

        List<String> ex = row.get(term);

        if (ex == null)
        {
            List<String> cell = new ArrayList<>();
            cell.add(p);
            row.put(term, cell);
        }
        else if (!ex.contains(p))
        {
            ex.add(p);
            hasCnf = true;
            cnfMsgs.add(
                "CONFLICT at M[" + nt + ", " + term + "]: " +
                String.join("  vs  ", ex)
            );
        }
    }

    public boolean hasConflict()
    {
        return hasCnf;
    }

    public List<String> getCnfMsgs()
    {
        return cnfMsgs;
    }

    @Override
    public String toString()
    {
        StringBuilder out = new StringBuilder();

        int ntW = 14;
        for (String nt : g.getNtOrder())
        {
            if (nt.length() + 2 > ntW)
            {
                ntW = nt.length() + 2;
            }
        }

        int cellW = 20;
        for (String nt : g.getNtOrder())
        {
            Map<String, List<String>> row = tbl.get(nt);
            if (row == null)
            {
                continue;
            }

            for (List<String> cell : row.values())
            {
                if (cell != null)
                {
                    String txt = String.join(" / ", cell);
                    if (txt.length() + 4 > cellW)
                    {
                        cellW = txt.length() + 4;
                    }
                }
            }
        }

        out.append(String.format("%-" + ntW + "s", ""));
        for (String col : cols)
        {
            out.append(String.format("| %-" + cellW + "s", col));
        }
        out.append("|\n");

        out.append("-".repeat(ntW));
        for (int i = 0; i < cols.size(); i++)
        {
            out.append("+").append("-".repeat(cellW + 1));
        }
        out.append("+\n");

        for (String nt : g.getNtOrder())
        {
            out.append(String.format("%-" + ntW + "s", nt));
            Map<String, List<String>> row = tbl.get(nt);

            for (String col : cols)
            {
                List<String> cell = (row != null) ? row.get(col) : null;
                String txt;
                if (cell == null)
                {
                    txt = "";
                }
                else
                {
                    txt = String.join(" / ", cell);
                }
                out.append(String.format("| %-" + cellW + "s", txt));
            }
            out.append("|\n");
        }

        if (hasCnf)
        {
            out.append("\n*** GRAMMAR IS NOT LL(1) ***\n");
            out.append("Conflicts detected:\n");
            for (String msg : cnfMsgs)
            {
                out.append("  - ").append(msg).append("\n");
            }
        }
        else
        {
            out.append("\nGrammar is LL(1). No conflicts detected.\n");
        }

        return out.toString();
    }

    public void writeFile(String path) throws IOException
    {
        File f = new File(path);
        f.getParentFile().mkdirs();

        BufferedWriter bw = new BufferedWriter(new FileWriter(path));
        bw.write(this.toString());
        bw.close();
    }
}
