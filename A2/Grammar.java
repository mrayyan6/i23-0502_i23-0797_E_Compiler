import java.io.*;
import java.util.*;

public class Grammar 
{
    private LinkedHashMap<String, List<List<String>>> prod; // productions
    private ArrayList<String> ntOrder; // nonTerminalOrder
    private String start; // startSymbol

    public Grammar(String path) throws IOException 
    {
        prod = new LinkedHashMap<>();
        ntOrder = new ArrayList<>();
        start = null;

        readGrammar(path);
    }

    public LinkedHashMap<String, List<List<String>>> getProd() 
    {
        return prod;
    }

    public ArrayList<String> getNtOrder() 
    {
        return ntOrder;
    }

    public String getStart() 
    {
        return start;
    }

    public Set<String> getTerms() 
    {
        Set<String> terms = new LinkedHashSet<>();
        for (List<List<String>> alts : prod.values()) 
        {
            for (List<String> alt : alts) 
            {
                for (String sym : alt) 
                {
                    if (!prod.containsKey(sym) && !sym.equals("epsilon")) 
                    {
                        terms.add(sym);
                    }
                }
            }
        }
        return terms;
    }

    public boolean isNt(String sym) 
    {
        return prod.containsKey(sym);
    }

    private void readGrammar(String path) throws IOException
    {
        BufferedReader br = new BufferedReader(new FileReader(path));
        String line;

        while ((line = br.readLine()) != null) 
        {
            line = line.trim();
            if (line.isEmpty())
            {
                continue;
            }

            String[] parts = line.split("\\s*->\\s*", 2);
            if (parts.length < 2) 
            {
                //  skip if ghalat format 
                System.err.println("WARNING: Skipping malformed line: " + line);
                continue;
            }

            String nt = parts[0].trim();
            String rhs = parts[1].trim();

            if (!prod.containsKey(nt)) 
            {
                prod.put(nt, new ArrayList<>());
                ntOrder.add(nt);
            }

            if (start == null)
            {
                start = nt;
            }

            String[] alts = rhs.split("\\s*\\|\\s*");

            for (String alt : alts)
            {
                List<String> syms = tokAlt(alt.trim());
                prod.get(nt).add(syms);
            }
        }

        br.close();
    }

    private List<String> tokAlt(String alt)
    {
        List<String> syms = new ArrayList<>();
        String[] toks = alt.split("\\s+");
        for (String tok : toks)
        {
            if (tok.isEmpty())
            {
                continue;
            }

            if (tok.equals("@"))
            {
                syms.add("epsilon");
            }
            else
            {
                syms.add(tok);
            }
        }
        return syms;
    }

    public void leftFactor()
    {
        boolean changed = true;

        while (changed)
        {
            changed = false;
            List<String> nts = new ArrayList<>(ntOrder);

            for (String nt : nts)
            {
                if (!prod.containsKey(nt))
                {
                    continue;
                }

                List<List<String>> alts = prod.get(nt);
                if (alts == null || alts.isEmpty())
                {
                    continue;
                }

                Map<String, List<List<String>>> groups = new LinkedHashMap<>();
                for (List<String> alt : alts)
                {
                    if (alt.isEmpty())
                    {
                        continue;
                    }
                    String firstSym = alt.get(0);
                    groups.computeIfAbsent(firstSym, k -> new ArrayList<>()).add(alt);
                }

                boolean needsFactoring = false;
                for (List<List<String>> grp : groups.values())
                {
                    if (grp.size() > 1)
                    {
                        needsFactoring = true;
                        break;
                    }
                }

                if (!needsFactoring)
                {
                    continue;
                }

                changed = true;
                List<List<String>> outAlts = new ArrayList<>();

                for (Map.Entry<String, List<List<String>>> entry : groups.entrySet()) 
                {
                    List<List<String>> grpAlts = entry.getValue();

                    if (grpAlts.size() == 1)
                    {
                        outAlts.add(grpAlts.get(0));
                    }
                    else
                    {
                        List<String> pfx = longestPrefix(grpAlts);
                        String newNt = makePrime(nt);

                        List<String> factAlt = new ArrayList<>(pfx);
                        factAlt.add(newNt);
                        outAlts.add(factAlt);

                        List<List<String>> sfxs = new ArrayList<>();
                        for (List<String> gAlt : grpAlts)
                        {
                            List<String> sfx = new ArrayList<>(gAlt.subList(pfx.size(), gAlt.size()));
                            if (sfx.isEmpty())
                            {
                                sfx.add("epsilon");
                            }
                            sfxs.add(sfx);
                        }

                        prod.put(newNt, sfxs);
                        if (!ntOrder.contains(newNt))
                        {
                            int idx = ntOrder.indexOf(nt);
                            ntOrder.add(idx + 1, newNt);
                        }
                    }
                }

                prod.put(nt, outAlts);
            }
        }
    }

    private List<String> longestPrefix(List<List<String>> alts)
    {
        List<String> pfx = new ArrayList<>();
        if (alts.isEmpty())
        {
            return pfx;
        }

        int min = alts.stream().mapToInt(List::size).min().orElse(0);

        for (int i = 0; i < min; i++)
        {
            String sym = alts.get(0).get(i);
            boolean allSame = true;

            for (int j = 1; j < alts.size(); j++)
            {
                if (!alts.get(j).get(i).equals(sym))
                {
                    allSame = false;
                    break;
                }
            }

            if (allSame)
            {
                pfx.add(sym);
            }
            else
            {
                break;
            }
        }

        return pfx;
    }

    private String makePrime(String base)// adds altaf prime
    {
        String candidate = base + "'";
        while (prod.containsKey(candidate))
        {
            candidate = candidate + "'";
        }
        return candidate;
    }

    public void removeLeftRec()
    {
        List<String> nts = new ArrayList<>(ntOrder);
        int n = nts.size();

        for (int i = 0; i < n; i++)
        {
            String ai = nts.get(i);

            for (int j = 0; j < i; j++)
            {
                String aj = nts.get(j);
                subProd(ai, aj);
            }

            removeDirectLeftRec(ai);
        }
    }

    private void subProd(String ai, String aj)
    {
        List<List<String>> aiAlts = prod.get(ai);
        if (aiAlts == null)
        {
            return;
        }

        List<List<String>> outAlts = new ArrayList<>();

        for (List<String> alt : aiAlts)
        {
            if (!alt.isEmpty() && alt.get(0).equals(aj))
            {
                List<String> gamma = new ArrayList<>(alt.subList(1, alt.size()));
                List<List<String>> ajAlts = prod.get(aj);

                if (ajAlts != null)
                {
                    for (List<String> ajAlt : ajAlts)
                    {
                        List<String> sub = new ArrayList<>();
                        if (ajAlt.size() == 1 && ajAlt.get(0).equals("epsilon"))
                        {
                            if (gamma.isEmpty())
                            {
                                sub.add("epsilon");
                            }
                            else
                            {
                                sub.addAll(gamma);
                            }
                        }
                        else
                        {
                            sub.addAll(ajAlt);
                            sub.addAll(gamma);
                        }
                        outAlts.add(sub);
                    }
                }
            }
            else
            {
                outAlts.add(alt);
            }
        }

        prod.put(ai, outAlts);
    }

    private void removeDirectLeftRec(String nt)
    {
        List<List<String>> alts = prod.get(nt);
        if (alts == null)
        {
            return;
        }

        List<List<String>> rec = new ArrayList<>();
        List<List<String>> nonRec = new ArrayList<>();

        for (List<String> alt : alts)
        {
            if (!alt.isEmpty() && alt.get(0).equals(nt))
            {
                rec.add(new ArrayList<>(alt.subList(1, alt.size())));
            }
            else
            {
                nonRec.add(alt);
            }
        }

        if (rec.isEmpty())
        {
            return;
        }

        String newNt = makePrime(nt);
        List<List<String>> outAlts = new ArrayList<>();

        for (List<String> beta : nonRec)
        {
            List<String> alt = new ArrayList<>();
            if (beta.size() == 1 && beta.get(0).equals("epsilon"))
            {
                alt.add(newNt);
            }
            else
            {
                alt.addAll(beta);
                alt.add(newNt);
            }
            outAlts.add(alt);
        }
        prod.put(nt, outAlts);

        List<List<String>> pAlts = new ArrayList<>();
        for (List<String> alpha : rec)
        {
            List<String> pAlt = new ArrayList<>(alpha);
            pAlt.add(newNt);
            pAlts.add(pAlt);
        }

        List<String> epsAlt = new ArrayList<>();
        epsAlt.add("epsilon");
        pAlts.add(epsAlt);

        prod.put(newNt, pAlts);

        if (!ntOrder.contains(newNt))
        {
            int idx = ntOrder.indexOf(nt);
            ntOrder.add(idx + 1, newNt);
        }
    }

    @Override
    public String toString()
    {
        StringBuilder out = new StringBuilder();
        for (String nt : ntOrder)
        {
            List<List<String>> alts = prod.get(nt);
            if (alts == null)
            {
                continue;
            }

            out.append(nt).append(" -> ");
            for (int i = 0; i < alts.size(); i++)
            {
                out.append(String.join(" ", alts.get(i)));
                if (i < alts.size() - 1)
                {
                    out.append(" | ");
                }
            }
            out.append("\n");
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
