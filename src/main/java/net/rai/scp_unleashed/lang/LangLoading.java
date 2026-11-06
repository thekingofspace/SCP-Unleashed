package net.rai.scp_unleashed.lang;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

public final class LangLoading {
    public static final String Directory = "lang";
    public static final String Suffix = ".json";
    public static final int MaxLoggedDuplicates = 40;
    private static final Logger Log = LogUtils.getLogger();
    private static final ThreadLocal<Session> Active = new ThreadLocal<>();

    private LangLoading() {
    }

    @FunctionalInterface
    public interface MainLoader {
        void load(Resource Source, BiConsumer<String, String> Output) throws Exception;
    }

    public static final class Session {
        private final List<String> Codes;
        private final Map<String, Integer> PackOrder = new HashMap<>();
        private final Map<String, Set<String>> KeysByCode = new LinkedHashMap<>();
        private final Map<String, Map<String, Map<ResourceLocation, List<Resource>>>> Listed = new HashMap<>();
        private LangMerge.Report Total = LangMerge.Report.Empty;

        private Session(ResourceManager Manager, List<String> Codes) {
            this.Codes = List.copyOf(Codes);
            int Order = 0;
            for (PackResources Pack : Manager.listPacks().toList()) {
                PackOrder.putIfAbsent(Pack.packId(), Order++);
            }
            for (String Code : this.Codes) {
                KeysByCode.computeIfAbsent(Code, Ignored -> new LinkedHashSet<>());
            }
        }

        private int orderOf(String PackId) {
            return PackOrder.computeIfAbsent(PackId, Ignored -> PackOrder.size());
        }

        public Map<String, Set<String>> keysByCode() {
            return KeysByCode;
        }

        public LangMerge.Report report() {
            return Total;
        }
    }

    public static Session begin(ResourceManager Manager, List<String> Codes) {
        Session Started = new Session(Manager, Codes);
        Active.set(Started);
        return Started;
    }

    @Nullable
    public static Session active() {
        return Active.get();
    }

    public static String mainPath(String Code) {
        return Directory + "/" + Code + Suffix;
    }

    private static Map<ResourceLocation, List<Resource>> folderFiles(Session Running, ResourceManager Manager, String Namespace, String Code) {
        Map<String, Map<ResourceLocation, List<Resource>>> ByNamespace = Running.Listed.computeIfAbsent(Code, Ignored -> {
            Map<String, Map<ResourceLocation, List<Resource>>> Grouped = new HashMap<>();
            try {
                Manager.listResourceStacks(Directory + "/" + Code, Location -> Location.getPath().endsWith(Suffix))
                        .forEach((Location, Stack) -> Grouped.computeIfAbsent(Location.getNamespace(), Space -> new LinkedHashMap<>()).put(Location, Stack));
            } catch (RuntimeException Problem) {
                Log.error("Could not list the language folders for {}: {}", Code, LangMerge.describe(Problem));
            }
            return Grouped;
        });
        return ByNamespace.getOrDefault(Namespace, Map.of());
    }

    public static void appendNamespace(Session Running, ResourceManager Manager, String Namespace, String Code, List<Resource> MainStack,
                                       Map<String, String> Strings, Map<String, Component> Components, MainLoader Main) {
        List<LangMerge.Part> Parts = new ArrayList<>();
        String MainPath = mainPath(Code);
        for (Resource Source : MainStack) {
            Parts.add(new LangMerge.Part(Running.orderOf(Source.sourcePackId()), Source.sourcePackId(), Namespace + ":" + MainPath, true,
                    Output -> Main.load(Source, Output)));
        }
        for (Map.Entry<ResourceLocation, List<Resource>> Found : folderFiles(Running, Manager, Namespace, Code).entrySet()) {
            for (Resource Source : Found.getValue()) {
                Parts.add(new LangMerge.Part(Running.orderOf(Source.sourcePackId()), Source.sourcePackId(), Found.getKey().toString(), false,
                        Output -> readFolderFile(Source, Output)));
            }
        }
        Set<String> Keys = Running.KeysByCode.computeIfAbsent(Code, Ignored -> new LinkedHashSet<>());
        LangMerge.Report Done = LangMerge.apply(Parts, Strings::put, (Entry, Key) -> {
            Keys.add(Key);
            if (!Entry.Main()) {
                Components.remove(Key);
            }
        });
        Running.Total = Running.Total.plus(Done);
        for (String Broken : Done.Broken()) {
            Log.error("Skipped broken language file {} for {}", Broken, Code);
        }
    }

    public static void readFolderFile(Resource Source, BiConsumer<String, String> Output) throws Exception {
        try (Reader In = Source.openAsReader()) {
            LangTree.flatten(LangTree.read(In), Output);
        }
    }

    public static LangIndex finish(Session Running) {
        if (Active.get() == Running) {
            Active.remove();
        }
        LangMerge.Report Total = Running.Total;
        List<String> Duplicates = Total.Duplicates();
        for (int Index = 0; Index < Math.min(MaxLoggedDuplicates, Duplicates.size()); Index++) {
            Log.warn("Duplicate translation key {}", Duplicates.get(Index));
        }
        if (Duplicates.size() > MaxLoggedDuplicates) {
            Log.warn("{} more duplicate translation keys were not listed", Duplicates.size() - MaxLoggedDuplicates);
        }
        LangIndex Built = LangIndex.build(Running.Codes, Running.KeysByCode);
        LangIndex.publish(Built);
        Log.info("Merged {} language files ({} keys) for {}{}", Total.Files(), Total.Keys(), Running.Codes,
                Total.Broken().isEmpty() ? "" : ", skipped " + Total.Broken().size() + " broken");
        return Built;
    }
}
