import java.io.*;
import java.util.*;

public class Parser
{
    private Grammar g; // grammar
    private FirstFollow ff; // firstFollow
    private Map<String, Map<String, List<String>>> tbl; // parsingTable
    private List<String> cols;
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

    public Map<String, Map<String, List<String>>> getTable()
    {
        return tbl;
    }

    public List<String> getColumns()
    {
        return cols;
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

    public static class ParseResult
    {
        public String inputString;
        public boolean accepted;
        public String traceLog;
        public Tree parseTree;
        public List<String> errors;
        public int errorCount;
    }

    public List<String> readInputFile(String filename)
    {
        List<String> inputs = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename)))
        {
            String line;
            while ((line = br.readLine()) != null)
            {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("//") && !line.startsWith("#"))
                {
                    inputs.add(line);
                }
            }
        }
        catch (IOException e)
        {
            System.err.println("Error reading input file: " + e.getMessage());
        }
        return inputs;
    }

    private List<String> tokenize(String input)
    {
        List<String> tokens = new ArrayList<>();
        input = input.trim();
        if (input.endsWith("$"))
        {
            input = input.substring(0, input.length() - 1).trim();
        }

        String[] parts = input.split("\\s+");
        for (String p : parts)
        {
            if (!p.isEmpty())
            {
                tokens.add(p);
            }
        }
        tokens.add("$");
        return tokens;
    }

    private List<String> rhsFromProduction(String prod)
    {
        int arrow = prod.indexOf("->");
        if (arrow < 0)
        {
            return Collections.emptyList();
        }

        String rhs = prod.substring(arrow + 2).trim();
        if (rhs.isEmpty())
        {
            return Collections.emptyList();
        }
        return new ArrayList<>(Arrays.asList(rhs.split("\\s+")));
    }

    private String buildInputString(List<String> input, int ptr)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = ptr; i < input.size(); i++)
        {
            if (i > ptr)
            {
                sb.append(" ");
            }
            sb.append(input.get(i));
        }
        return sb.toString();
    }

    public ParseResult parse(String inputStr, int lineNumber)
    {
        ParseResult result = new ParseResult();
        result.inputString = inputStr;
        result.errors = new ArrayList<>();

        List<String> input = tokenize(inputStr);
        Stack stack = new Stack();
        StringBuilder traceLog = new StringBuilder();

        stack.push("$");
        stack.push(g.getStart());

        Tree tree = new Tree();
        Tree.TreeNode root = new Tree.TreeNode(g.getStart());
        tree.setRoot(root);

        LinkedList<Tree.TreeNode> nodeStack = new LinkedList<>();
        nodeStack.push(null);
        nodeStack.push(root);

        int ptr = 0;
        int step = 1;
        boolean accepted = false;
        int safety = 0;

        traceLog.append(String.format("%-6s %-35s %-25s %s%n", "Step", "Stack", "Input", "Action"));
        traceLog.append("-".repeat(100)).append("\n");

        while (safety < 10000)
        {
            safety++;

            String X = stack.top();
            String a = input.get(ptr);

            String stackStr = stack.contents();
            String inputR = buildInputString(input, ptr);

            if (X.equals("$") && a.equals("$"))
            {
                traceLog.append(String.format("%-6d %-35s %-25s %s%n", step, stackStr, inputR, "ACCEPT"));
                accepted = true;
                break;
            }

            if (!g.isNt(X))
            {
                if (X.equals(a))
                {
                    traceLog.append(String.format("%-6d %-35s %-25s %s%n", step, stackStr, inputR, "Match '" + a + "'"));
                    stack.pop();
                    if (!nodeStack.isEmpty())
                    {
                        nodeStack.pop();
                    }
                    ptr++;
                    step++;
                    continue;
                }

                String err = "Expected '" + X + "' but found '" + a + "'";
                traceLog.append(String.format("%-6d %-35s %-25s %s%n", step, stackStr, inputR, "ERROR: " + err));
                result.errors.add("Line " + lineNumber + ": " + err);
                stack.pop();
                if (!nodeStack.isEmpty())
                {
                    nodeStack.pop();
                }
                step++;
                continue;
            }

            Map<String, List<String>> row = tbl.get(X);
            List<String> cell = (row != null) ? row.get(a) : null;

            if (cell == null || cell.isEmpty())
            {
                String err = "No entry M[" + X + ", " + a + "]";
                traceLog.append(String.format("%-6d %-35s %-25s %s%n", step, stackStr, inputR, "ERROR: " + err));
                result.errors.add("Line " + lineNumber + ": " + err);

                Set<String> sync = ff.getFollow().getOrDefault(X, Collections.emptySet());
                if (sync.contains(a) || a.equals("$"))
                {
                    stack.pop();
                    if (!nodeStack.isEmpty())
                    {
                        nodeStack.pop();
                    }
                }
                else
                {
                    ptr = Math.min(ptr + 1, input.size() - 1);
                }

                step++;
                continue;
            }

            String chosen = cell.get(0);
            List<String> rhs = rhsFromProduction(chosen);

            traceLog.append(String.format("%-6d %-35s %-25s %s%n", step, stackStr, inputR, chosen));

            stack.pop();
            Tree.TreeNode parent = nodeStack.pop();

            if (rhs.size() == 1 && rhs.get(0).equals("epsilon"))
            {
                if (parent != null)
                {
                    parent.addChild(new Tree.TreeNode("epsilon"));
                }
            }
            else
            {
                List<Tree.TreeNode> children = new ArrayList<>();
                for (String sym : rhs)
                {
                    Tree.TreeNode child = new Tree.TreeNode(sym);
                    children.add(child);
                    if (parent != null)
                    {
                        parent.addChild(child);
                    }
                }

                for (int i = rhs.size() - 1; i >= 0; i--)
                {
                    stack.push(rhs.get(i));
                    nodeStack.push(children.get(i));
                }
            }

            step++;
        }

        if (safety >= 10000)
        {
            result.errors.add("Line " + lineNumber + ": Parser safety stop triggered.");
        }

        result.traceLog = traceLog.toString();
        result.accepted = accepted && result.errors.isEmpty();
        result.errorCount = result.errors.size();
        result.parseTree = accepted ? tree : null;

        return result;
    }

    public List<ParseResult> parseFile(String filename)
    {
        List<String> inputs = readInputFile(filename);
        List<ParseResult> results = new ArrayList<>();

        for (int i = 0; i < inputs.size(); i++)
        {
            System.out.println("\n" + "=".repeat(80));
            System.out.println("Parsing input string #" + (i + 1) + ": " + inputs.get(i));
            System.out.println("=".repeat(80));

            ParseResult r = parse(inputs.get(i), i + 1);
            results.add(r);

            System.out.println(r.traceLog);

            if (r.accepted)
            {
                System.out.println("Result: String ACCEPTED successfully!");
                if (r.parseTree != null)
                {
                    System.out.println("\nParse Tree:");
                    r.parseTree.printIndented();
                }
            }
            else
            {
                System.out.println("Result: REJECTED with " + r.errorCount + " error(s).");
                for (String err : r.errors)
                {
                    System.out.println("  " + err);
                }
            }
        }

        return results;
    }

    public void saveTraces(List<ParseResult> results, String filename)
    {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename)))
        {
            for (int i = 0; i < results.size(); i++)
            {
                ParseResult r = results.get(i);
                pw.println("=".repeat(80));
                pw.println("Input #" + (i + 1) + ": " + r.inputString);
                pw.println("=".repeat(80));
                pw.println(r.traceLog);
                if (r.accepted)
                {
                    pw.println("Result: ACCEPTED");
                }
                else
                {
                    pw.println("Result: REJECTED with " + r.errorCount + " error(s)");
                    for (String err : r.errors)
                    {
                        pw.println("  " + err);
                    }
                }
                pw.println();
            }
        }
        catch (IOException e)
        {
            System.err.println("Error saving traces: " + e.getMessage());
        }
    }

    public void saveParseTrees(List<ParseResult> results, String filename)
    {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename)))
        {
            for (int i = 0; i < results.size(); i++)
            {
                ParseResult r = results.get(i);
                pw.println("=".repeat(60));
                pw.println("Parse Tree for input #" + (i + 1) + ": " + r.inputString);
                pw.println("=".repeat(60));

                if (r.accepted && r.parseTree != null)
                {
                    r.parseTree.saveToFile(pw, "");
                }
                else
                {
                    pw.println("  (No parse tree - string was rejected)");
                    for (String err : r.errors)
                    {
                        pw.println("  " + err);
                    }
                }
                pw.println();
            }
        }
        catch (IOException e)
        {
            System.err.println("Error saving parse trees: " + e.getMessage());
        }
    }
}
