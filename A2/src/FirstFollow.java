import java.util.*;

public class FirstFollow
{
    private Grammar g;
    private Map<String, Set<String>> first;
    private Map<String, Set<String>> follow;

    public FirstFollow(Grammar g)
    {
        this.g = g;
        this.first = new LinkedHashMap<>();
        this.follow = new LinkedHashMap<>();

        for (String nt : g.getNtOrder())
        {
            first.put(nt, new LinkedHashSet<>());
            follow.put(nt, new LinkedHashSet<>());
        }

        buildFirst();
        buildFollow();
    }

    public Map<String, Set<String>> getFirst()
    {
        return first;
    }

    public Map<String, Set<String>> getFollow()
    {
        return follow;
    }

    private void buildFirst()
    {
        boolean changed = true;

        while (changed)
        {
            changed = false;

            for (String nt : g.getNtOrder())
            {
                List<List<String>> alts = g.getProd().get(nt);
                if (alts == null)
                {
                    continue;
                }

                for (List<String> alt : alts)
                {
                    Set<String> altFirst = firstOfSeq(alt);

                    if (first.get(nt).addAll(altFirst))
                    {
                        changed = true;
                    }
                }
            }
        }
    }

    public Set<String> firstOfSeq(List<String> seq)
    {
        Set<String> result = new LinkedHashSet<>();

        if (seq.size() == 1 && seq.get(0).equals("epsilon"))
        {
            result.add("epsilon");
            return result;
        }

        boolean allEps = true;

        for (String sym : seq)
        {
            if (sym.equals("epsilon"))
            {
                continue;
            }

            if (!g.isNt(sym))
            {
                result.add(sym);
                allEps = false;
                break;
            }
            else
            {
                Set<String> symFirst = first.get(sym);
                if (symFirst != null)
                {
                    for (String s : symFirst)
                    {
                        if (!s.equals("epsilon"))
                        {
                            result.add(s);
                        }
                    }

                    if (!symFirst.contains("epsilon"))
                    {
                        allEps = false;
                        break;
                    }
                }
                else
                {
                    allEps = false;
                    break;
                }
            }
        }

        if (allEps)
        {
            result.add("epsilon");
        }

        return result;
    }

    private void buildFollow()
    {
        String s = g.getStart();
        if (s != null)
        {
            follow.get(s).add("$");
        }

        boolean changed = true;

        while (changed)
        {
            changed = false;

            for (String nt : g.getNtOrder())
            {
                List<List<String>> alts = g.getProd().get(nt);
                if (alts == null)
                {
                    continue;
                }

                for (List<String> alt : alts)
                {
                    for (int i = 0; i < alt.size(); i++)
                    {
                        String sym = alt.get(i);

                        if (!g.isNt(sym))
                        {
                            continue;
                        }

                        List<String> beta = alt.subList(i + 1, alt.size());
                        Set<String> firstBeta;

                        if (beta.isEmpty())
                        {
                            firstBeta = new LinkedHashSet<>();
                            firstBeta.add("epsilon");
                        }
                        else
                        {
                            firstBeta = firstOfSeq(beta);
                        }

                        for (String t : firstBeta)
                        {
                            if (!t.equals("epsilon"))
                            {
                                if (follow.get(sym).add(t))
                                {
                                    changed = true;
                                }
                            }
                        }

                        if (firstBeta.contains("epsilon"))
                        {
                            if (follow.get(sym).addAll(follow.get(nt)))
                            {
                                changed = true;
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public String toString()
    {
        StringBuilder out = new StringBuilder();

        int w = 12;
        for (String nt : g.getNtOrder())
        {
            if (nt.length() > w)
            {
                w = nt.length();
            }
        }
        w += 2;

        String head = String.format("%-" + w + "s %-30s %-30s", "Non-Terminal", "FIRST", "FOLLOW");
        out.append(head).append("\n");
        out.append("-".repeat(head.length())).append("\n");

        for (String nt : g.getNtOrder())
        {
            String firstStr = "{   " + String.join(", ", first.getOrDefault(nt, Collections.emptySet())) + "     }";
            String followStr = "{   " + String.join(", ", follow.getOrDefault(nt, Collections.emptySet())) + "   }";
            out.append(String.format("%-" + w + "s %-30s %-30s", nt, firstStr, followStr)).append("\n");
        }

        return out.toString();
    }
}
