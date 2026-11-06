package net.rai.scp_unleashed.lang;

import net.minecraft.locale.Language;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class LangIndex {
    public static final LangIndex Empty = new LangIndex(0, List.of(), Map.of());
    private static final AtomicInteger Generations = new AtomicInteger();
    private static final int MaxIndexDigits = 6;
    private static final Map<Object, LangIndex> ByLanguage = Collections.synchronizedMap(new WeakHashMap<>());
    private static volatile LangIndex Current = Empty;

    private final int Generation;
    private final List<String> Codes;
    private final Map<String, Map<String, List<Integer>>> Children;

    private LangIndex(int Generation, List<String> Codes, Map<String, Map<String, List<Integer>>> Children) {
        this.Generation = Generation;
        this.Codes = Codes;
        this.Children = Children;
    }

    public static LangIndex current() {
        LangIndex Attached = ByLanguage.get(Language.getInstance());
        return Attached != null ? Attached : Current;
    }

    public static void publish(LangIndex Index) {
        Current = Index;
    }

    public static void attach(Object Owner, LangIndex Index) {
        if (Owner != null) {
            ByLanguage.put(Owner, Index);
        }
    }

    public static LangIndex build(List<String> Codes, Map<String, ? extends Collection<String>> KeysByCode) {
        Map<String, Map<String, List<Integer>>> Children = new HashMap<>();
        for (String Code : Codes) {
            Collection<String> Keys = KeysByCode.get(Code);
            if (Keys == null || Children.containsKey(Code)) {
                continue;
            }
            Map<String, TreeSet<Integer>> Found = new HashMap<>();
            for (String Key : Keys) {
                collect(Key, Found);
            }
            Map<String, List<Integer>> Sorted = new HashMap<>();
            Found.forEach((Parent, Indices) -> Sorted.put(Parent, List.copyOf(Indices)));
            Children.put(Code, Sorted);
        }
        return new LangIndex(Generations.incrementAndGet(), List.copyOf(Codes), Children);
    }

    private static void collect(String Key, Map<String, TreeSet<Integer>> Found) {
        int Start = 0;
        while (Start < Key.length()) {
            int End = Key.indexOf('.', Start);
            if (End < 0) {
                End = Key.length();
            }
            if (Start > 0 && isIndex(Key, Start, End)) {
                Found.computeIfAbsent(Key.substring(0, Start - 1), Parent -> new TreeSet<>()).add(Integer.parseInt(Key, Start, End, 10));
            }
            Start = End + 1;
        }
    }

    private static boolean isIndex(String Key, int Start, int End) {
        if (End <= Start || End - Start > MaxIndexDigits) {
            return false;
        }
        for (int Position = Start; Position < End; Position++) {
            char Letter = Key.charAt(Position);
            if (Letter < '0' || Letter > '9') {
                return false;
            }
        }
        return true;
    }

    public int generation() {
        return Generation;
    }

    public List<String> codes() {
        return Codes;
    }

    public List<Integer> indices(String Parent) {
        for (int Position = Codes.size() - 1; Position >= 0; Position--) {
            Map<String, List<Integer>> Table = Children.get(Codes.get(Position));
            List<Integer> Found = Table == null ? null : Table.get(Parent);
            if (Found != null && !Found.isEmpty()) {
                return Found;
            }
        }
        return List.of();
    }

    public String providerOf(String Parent) {
        for (int Position = Codes.size() - 1; Position >= 0; Position--) {
            Map<String, List<Integer>> Table = Children.get(Codes.get(Position));
            if (Table != null && Table.containsKey(Parent)) {
                return Codes.get(Position);
            }
        }
        return "";
    }

    public List<Integer> indicesOr(String Parent, int FallbackCount) {
        List<Integer> Found = indices(Parent);
        if (!Found.isEmpty()) {
            return Found;
        }
        List<Integer> Counted = new ArrayList<>();
        for (int Index = 0; Index < FallbackCount; Index++) {
            Counted.add(Index);
        }
        return Counted;
    }
}
