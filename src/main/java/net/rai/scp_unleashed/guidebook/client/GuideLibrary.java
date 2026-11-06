package net.rai.scp_unleashed.guidebook.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GuideLibrary {
    public static final String Directory = "guidebook";
    public static final String ChapterFolder = "chapters/";
    public static final String PageFolder = "pages/";
    public static final int LooseChapterOrder = 100000;
    private static final Logger Log = LogUtils.getLogger();
    private static final Gson Parser = new GsonBuilder().create();
    private static List<GuideChapter> Chapters = List.of();
    private static List<GuidePage> Pages = List.of();

    private GuideLibrary() {
    }

    public static List<GuideChapter> chapters() {
        return Chapters;
    }

    public static List<GuidePage> pages() {
        return Pages;
    }

    public static int indexOf(@Nullable ResourceLocation Id) {
        for (int Index = 0; Index < Pages.size(); Index++) {
            if (Pages.get(Index).Id().equals(Id)) {
                return Index;
            }
        }
        return -1;
    }

    @Nullable
    public static GuideChapter chapterOf(GuidePage Page) {
        for (GuideChapter Chapter : Chapters) {
            if (Chapter.Id().equals(Page.Chapter())) {
                return Chapter;
            }
        }
        return null;
    }

    static void accept(Map<ResourceLocation, JsonElement> Files) {
        Map<ResourceLocation, GuideChapter> Declared = new HashMap<>();
        List<GuidePage> Loaded = new ArrayList<>();
        for (Map.Entry<ResourceLocation, JsonElement> Source : Files.entrySet()) {
            ResourceLocation Location = Source.getKey();
            try {
                JsonObject Root = GsonHelper.convertToJsonObject(Source.getValue(), "guidebook file");
                if (Location.getPath().startsWith(ChapterFolder)) {
                    ResourceLocation Id = Location.withPath(Location.getPath().substring(ChapterFolder.length()));
                    Declared.put(Id, new GuideChapter(Id, GsonHelper.getAsInt(Root, "order", 0), text(Root, "title", ""), optionalId(Root, "icon"), List.of()));
                } else if (Location.getPath().startsWith(PageFolder)) {
                    Loaded.add(page(Location.withPath(Location.getPath().substring(PageFolder.length())), Root));
                } else {
                    Log.warn("Ignoring guidebook file {}: it belongs in chapters/ or pages/", Location);
                }
            } catch (RuntimeException Problem) {
                Log.error("Skipping guidebook file {}: {}", Location, Problem.getMessage());
            }
        }
        Map<ResourceLocation, List<GuidePage>> Grouped = new LinkedHashMap<>();
        for (GuidePage Page : Loaded) {
            if (!Declared.containsKey(Page.Chapter())) {
                Log.warn("Guidebook page {} names unknown chapter {}, listing it under that id", Page.Id(), Page.Chapter());
                Declared.put(Page.Chapter(), new GuideChapter(Page.Chapter(), LooseChapterOrder, new GuideText(Page.Chapter().getPath(), List.of()), null, List.of()));
            }
            Grouped.computeIfAbsent(Page.Chapter(), Key -> new ArrayList<>()).add(Page);
        }
        Comparator<GuidePage> PageOrder = Comparator.comparingInt(GuidePage::Order).thenComparing(Page -> Page.Id().toString());
        List<GuideChapter> Built = new ArrayList<>();
        for (GuideChapter Chapter : Declared.values()) {
            List<GuidePage> Members = Grouped.getOrDefault(Chapter.Id(), List.of());
            if (!Members.isEmpty()) {
                Built.add(new GuideChapter(Chapter.Id(), Chapter.Order(), Chapter.Title(), Chapter.Icon(), Members.stream().sorted(PageOrder).toList()));
            }
        }
        Built.sort(Comparator.comparingInt(GuideChapter::Order).thenComparing(Chapter -> Chapter.Id().toString()));
        List<GuidePage> Flat = new ArrayList<>();
        for (GuideChapter Chapter : Built) {
            Flat.addAll(Chapter.Pages());
        }
        Chapters = List.copyOf(Built);
        Pages = List.copyOf(Flat);
        Log.info("Loaded {} guidebook pages in {} chapters", Pages.size(), Chapters.size());
    }

    private static GuidePage page(ResourceLocation Id, JsonObject Root) {
        ResourceLocation Chapter = ResourceLocation.parse(GsonHelper.getAsString(Root, "chapter"));
        List<GuideBlock> Blocks = new ArrayList<>();
        JsonArray Content = GsonHelper.getAsJsonArray(Root, "content", new JsonArray());
        for (int Index = 0; Index < Content.size(); Index++) {
            try {
                Blocks.add(block(GsonHelper.convertToJsonObject(Content.get(Index), "content block")));
            } catch (RuntimeException Problem) {
                Log.error("Skipping block {} of guidebook page {}: {}", Index, Id, Problem.getMessage());
            }
        }
        return new GuidePage(Id, Chapter, GsonHelper.getAsInt(Root, "order", 0), text(Root, "title", ""), optionalId(Root, "icon"), List.copyOf(Blocks));
    }

    private static GuideBlock block(JsonObject Node) {
        String Type = GsonHelper.getAsString(Node, "type", "text");
        return switch (Type) {
            case "text" -> new GuideBlock.Paragraph(text(Node, "text", "args"));
            case "heading" -> new GuideBlock.Heading(text(Node, "text", "args"));
            case "note" -> new GuideBlock.Note(text(Node, "text", "args"));
            case "step" -> new GuideBlock.Step(optionalId(Node, "icon"), text(Node, "text", "args"), GsonHelper.getAsBoolean(Node, "bullet", false));
            case "items" -> new GuideBlock.Icons(ids(Node, "items"), links(Node, "links"), "flow".equals(GsonHelper.getAsString(Node, "style", "row")),
                    optionalText(Node, "caption", "caption_args"));
            case "image" -> {
                int Width = GsonHelper.getAsInt(Node, "width");
                int Height = GsonHelper.getAsInt(Node, "height");
                yield new GuideBlock.Picture(ResourceLocation.parse(GsonHelper.getAsString(Node, "texture")), Width, Height, GsonHelper.getAsInt(Node, "u", 0),
                        GsonHelper.getAsInt(Node, "v", 0), GsonHelper.getAsInt(Node, "texture_width", Width), GsonHelper.getAsInt(Node, "texture_height", Height),
                        optionalText(Node, "caption", "caption_args"));
            }
            default -> throw new JsonParseException("unknown block type '" + Type + "'");
        };
    }

    private static GuideText text(JsonObject Node, String Field, String ArgsField) {
        List<String> Args = new ArrayList<>();
        if (!ArgsField.isEmpty() && Node.has(ArgsField)) {
            for (JsonElement Arg : GsonHelper.getAsJsonArray(Node, ArgsField)) {
                Args.add(GsonHelper.convertToString(Arg, ArgsField));
            }
        }
        return new GuideText(GsonHelper.getAsString(Node, Field), List.copyOf(Args));
    }

    @Nullable
    private static GuideText optionalText(JsonObject Node, String Field, String ArgsField) {
        return Node.has(Field) ? text(Node, Field, ArgsField) : null;
    }

    @Nullable
    private static ResourceLocation optionalId(JsonObject Node, String Field) {
        return Node.has(Field) ? ResourceLocation.parse(GsonHelper.getAsString(Node, Field)) : null;
    }

    private static List<ResourceLocation> links(JsonObject Node, String Field) {
        List<ResourceLocation> Found = new ArrayList<>();
        if (Node.has(Field)) {
            for (JsonElement Entry : GsonHelper.getAsJsonArray(Node, Field)) {
                String Target = GsonHelper.convertToString(Entry, Field);
                Found.add(Target.isEmpty() ? null : ResourceLocation.parse(Target));
            }
        }
        return Collections.unmodifiableList(Found);
    }

    private static List<ResourceLocation> ids(JsonObject Node, String Field) {
        List<ResourceLocation> Found = new ArrayList<>();
        for (JsonElement Entry : GsonHelper.getAsJsonArray(Node, Field)) {
            Found.add(ResourceLocation.parse(GsonHelper.convertToString(Entry, Field)));
        }
        return List.copyOf(Found);
    }

    public static class Loader extends SimpleJsonResourceReloadListener {
        public Loader() {
            super(Parser, Directory);
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> Files, ResourceManager Resources, ProfilerFiller Profiler) {
            accept(Files);
        }
    }
}
