package net.rai.scp_unleashed.lang;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class LangMerge {
    public static final Comparator<Part> PartOrder = Comparator.comparingInt(Part::Pack)
            .thenComparing(Part::Main)
            .thenComparing(Part::Path);

    private LangMerge() {
    }

    @FunctionalInterface
    public interface Loader {
        void load(BiConsumer<String, String> Output) throws Exception;
    }

    public record Part(int Pack, String PackId, String Path, boolean Main, Loader Source) {
    }

    public record Report(int Files, int Keys, List<String> Duplicates, List<String> Broken) {
        public static final Report Empty = new Report(0, 0, List.of(), List.of());

        public Report plus(Report Other) {
            List<String> AllDuplicates = new ArrayList<>(Duplicates);
            AllDuplicates.addAll(Other.Duplicates());
            List<String> AllBroken = new ArrayList<>(Broken);
            AllBroken.addAll(Other.Broken());
            return new Report(Files + Other.Files(), Keys + Other.Keys(), AllDuplicates, AllBroken);
        }
    }

    public static Report apply(List<Part> Parts, BiConsumer<String, String> Output, BiConsumer<Part, String> Defined) {
        List<Part> Ordered = new ArrayList<>(Parts);
        Ordered.sort(PartOrder);
        List<String> Duplicates = new ArrayList<>();
        List<String> Broken = new ArrayList<>();
        Map<String, String> SeenInPack = new HashMap<>();
        int CurrentPack = Integer.MIN_VALUE;
        int Files = 0;
        int Keys = 0;
        for (Part Entry : Ordered) {
            if (Entry.Pack() != CurrentPack) {
                CurrentPack = Entry.Pack();
                SeenInPack.clear();
            }
            Map<String, String> Loaded = new LinkedHashMap<>();
            List<String> Repeated = new ArrayList<>();
            try {
                Entry.Source().load((Key, Value) -> {
                    if (Loaded.put(Key, Value) != null) {
                        Repeated.add(Key);
                    }
                });
            } catch (Exception Problem) {
                Broken.add(Entry.PackId() + ":" + Entry.Path() + " (" + describe(Problem) + ")");
                continue;
            }
            Files++;
            for (String Key : Repeated) {
                Duplicates.add(Key + " twice in " + Entry.PackId() + ":" + Entry.Path() + ", using the later value");
            }
            for (Map.Entry<String, String> Pair : Loaded.entrySet()) {
                String Before = SeenInPack.put(Pair.getKey(), Entry.Path());
                if (Before != null && !Before.equals(Entry.Path())) {
                    Duplicates.add(Pair.getKey() + " in pack " + Entry.PackId() + ": " + Before + " and " + Entry.Path() + ", using " + Entry.Path());
                }
                Output.accept(Pair.getKey(), Pair.getValue());
                Defined.accept(Entry, Pair.getKey());
                Keys++;
            }
        }
        return new Report(Files, Keys, Duplicates, Broken);
    }

    public record Built(Map<String, String> Table, Map<String, Set<String>> KeysByCode, Report Done) {
    }

    public static Built build(List<String> Codes, Function<String, List<Part>> PartsFor) {
        Map<String, String> Table = new HashMap<>();
        Map<String, Set<String>> KeysByCode = new LinkedHashMap<>();
        Report Total = Report.Empty;
        for (String Code : Codes) {
            Set<String> Keys = KeysByCode.computeIfAbsent(Code, Ignored -> new LinkedHashSet<>());
            Total = Total.plus(apply(PartsFor.apply(Code), Table::put, (Entry, Key) -> Keys.add(Key)));
        }
        return new Built(Table, KeysByCode, Total);
    }

    public static String describe(Throwable Problem) {
        Throwable Root = Problem;
        while (Root.getCause() != null && Root.getCause() != Root) {
            Root = Root.getCause();
        }
        String Message = Root.getMessage();
        return Root.getClass().getSimpleName() + (Message == null ? "" : ": " + Message);
    }
}
