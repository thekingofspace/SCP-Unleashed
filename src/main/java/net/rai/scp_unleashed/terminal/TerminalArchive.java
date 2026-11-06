package net.rai.scp_unleashed.terminal;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.DocumentClass;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public final class TerminalArchive {
    public static final String Folder = "anomaly_document";

    public record Census(int Total, Map<DocumentClass, Integer> ByClass) {
        public int of(DocumentClass Which) {
            return ByClass.getOrDefault(Which, 0);
        }

        public int sum() {
            int Sum = 0;
            for (int Each : ByClass.values()) {
                Sum += Each;
            }
            return Sum;
        }
    }

    private static volatile Optional<Census> Cached;
    private static volatile Map<ResourceLocation, AnomalyDocument> Documents;

    private TerminalArchive() {
    }

    public static Optional<Census> census() {
        Optional<Census> Known = Cached;
        if (Known == null) {
            Known = Optional.ofNullable(read(folder()));
            Cached = Known;
        }
        return Known;
    }

    public static Map<ResourceLocation, AnomalyDocument> documents() {
        Map<ResourceLocation, AnomalyDocument> Known = Documents;
        if (Known == null) {
            Known = load(folder());
            Documents = Known;
        }
        return Known;
    }

    public static Map<ResourceLocation, AnomalyDocument> load(Path Root) {
        if (Root == null || !Files.isDirectory(Root)) {
            return Map.of();
        }
        Map<ResourceLocation, AnomalyDocument> Loaded = new LinkedHashMap<>();
        try (Stream<Path> Listing = Files.list(Root)) {
            List<Path> Entries = Listing.filter(Each -> Each.getFileName().toString().endsWith(".json")).sorted().toList();
            for (Path Each : Entries) {
                String Name = Each.getFileName().toString();
                ResourceLocation Id = ResourceLocation.tryBuild(Scp_unleashed.ModId, Name.substring(0, Name.length() - ".json".length()));
                if (Id == null) {
                    continue;
                }
                try (Reader Input = Files.newBufferedReader(Each)) {
                    AnomalyDocument.DocumentCodec.parse(JsonOps.INSTANCE, JsonParser.parseReader(Input)).result()
                            .map(Parsed -> Parsed.Lang().isEmpty() ? Parsed.withLang(AnomalyDocument.defaultLang(Id)) : Parsed)
                            .ifPresent(Parsed -> Loaded.put(Id, Parsed));
                } catch (Exception Failure) {
                    continue;
                }
            }
        } catch (Exception Failure) {
            return Map.of();
        }
        return Collections.unmodifiableMap(Loaded);
    }

    public static Path folder() {
        try {
            IModFileInfo Info = ModList.get().getModFileById(Scp_unleashed.ModId);
            return Info == null ? null : Info.getFile().findResource("data", Scp_unleashed.ModId, Folder);
        } catch (RuntimeException Failure) {
            return null;
        }
    }

    public static Census read(Path Root) {
        if (Root == null || !Files.isDirectory(Root)) {
            return null;
        }
        List<String> Classes = new ArrayList<>();
        try (Stream<Path> Listing = Files.list(Root)) {
            List<Path> Entries = Listing.filter(Each -> Each.getFileName().toString().endsWith(".json")).sorted().toList();
            for (Path Each : Entries) {
                try (Reader Input = Files.newBufferedReader(Each)) {
                    JsonElement Parsed = JsonParser.parseReader(Input);
                    JsonObject Body = Parsed.isJsonObject() ? Parsed.getAsJsonObject() : new JsonObject();
                    Classes.add(Body.has("classification") ? Body.get("classification").getAsString() : "");
                }
            }
        } catch (Exception Failure) {
            return null;
        }
        return Classes.isEmpty() ? null : count(Classes);
    }

    public static Census count(List<String> Classes) {
        Map<DocumentClass, Integer> ByClass = new EnumMap<>(DocumentClass.class);
        for (String Each : Classes) {
            DocumentClass Which = classOf(Each);
            ByClass.merge(Which, 1, Integer::sum);
        }
        return new Census(Classes.size(), Collections.unmodifiableMap(ByClass));
    }

    public static DocumentClass classOf(String Name) {
        String Clean = Name == null ? "" : Name.trim().toLowerCase(Locale.ROOT);
        for (DocumentClass Each : DocumentClass.values()) {
            if (Each.getSerializedName().equals(Clean)) {
                return Each;
            }
        }
        return DocumentClass.Unknown;
    }
}
