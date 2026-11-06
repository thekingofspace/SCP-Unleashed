package net.rai.scp_unleashed.storage.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.NeoForgeRenderTypes;
import net.rai.scp_unleashed.storage.StorageContainerBlockEntity;
import net.rai.scp_unleashed.storage.StorageGeometry;
import net.rai.scp_unleashed.storage.StorageTags;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class StorageLayoutAudit {
    public static final float MinExtent = 1.5f;
    public static final float MinFace = 3.0f;
    public static final float InsideShare = 0.5f;
    public static final float Slack = 0.75f;
    public static final float ClipLimit = 1.0f;
    public static final int AuditLight = 0xF000F0;
    public static final Set<String> KnownRenderTypes = Set.of("", "minecraft:solid", "minecraft:cutout", "minecraft:cutout_mipped", "minecraft:translucent", "solid", "cutout",
            "cutout_mipped", "translucent");
    private static final Map<String, JsonObject> Parsed = new HashMap<>();

    private StorageLayoutAudit() {
    }

    public record Draw(String Kind, String Subject, ItemStack Shown, BlockState Block, ItemDisplayContext Display, Matrix4f Matrix, boolean Centred) {
    }

    public record Shape(List<float[]> Boxes, String RenderType, String Note) {
    }

    private static final class Recorder implements StorageCanvas {
        private final Function<String, InputStream> Resources;
        private final List<Draw> Draws = new ArrayList<>();

        private Recorder(Function<String, InputStream> Resources) {
            this.Resources = Resources;
        }

        @Override
        public boolean isBlockLike(StorageLayoutContext Context, ItemStack Shown) {
            JsonObject Model = itemModel(Resources, Shown);
            return Model == null || !"generated".equals(kind(Resources, Model));
        }

        @Override
        public float halfHeight(StorageLayoutContext Context, ItemStack Shown, ItemDisplayContext Display) {
            Matrix4f Transform = displayMatrix(displayEntry(Resources, itemModel(Resources, Shown), Display));
            return StorageLayoutContext.PixelHalf * Transform.transformDirection(new Vector3f(0.0f, 1.0f, 0.0f)).length();
        }

        @Override
        public void drawModel(StorageLayoutContext Context, ModelResourceLocation Location, boolean Centred) {
            Draws.add(new Draw("model", Location.id().toString(), ItemStack.EMPTY, null, ItemDisplayContext.NONE, new Matrix4f(Context.Pose().last().pose()), Centred));
        }

        @Override
        public void drawBlock(StorageLayoutContext Context, BlockState Shown) {
            Draws.add(new Draw("block", BuiltInRegistries.BLOCK.getKey(Shown.getBlock()).toString(), ItemStack.EMPTY, Shown, ItemDisplayContext.NONE,
                    new Matrix4f(Context.Pose().last().pose()), true));
        }

        @Override
        public void drawItem(StorageLayoutContext Context, ItemStack Shown, ItemDisplayContext Display) {
            Draws.add(new Draw("item", BuiltInRegistries.ITEM.getKey(Shown.getItem()).toString(), Shown.copy(), null, Display, new Matrix4f(Context.Pose().last().pose()), true));
        }
    }

    private static final class Sink implements VertexConsumer {
        @Override
        public VertexConsumer addVertex(float X, float Y, float Z) {
            return this;
        }

        @Override
        public VertexConsumer setColor(int R, int G, int B, int A) {
            return this;
        }

        @Override
        public VertexConsumer setUv(float U, float V) {
            return this;
        }

        @Override
        public VertexConsumer setUv1(int U, int V) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int U, int V) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float X, float Y, float Z) {
            return this;
        }
    }

    public static List<String> registerAll(List<String> Registrars) {
        List<String> Problems = new ArrayList<>();
        for (String Name : Registrars) {
            try {
                Class<?> Found = Class.forName(Name, true, StorageLayoutAudit.class.getClassLoader());
                if (Found.getClassLoader() != StorageLayoutAudit.class.getClassLoader()) {
                    Problems.add("PROBLEM layout registrar " + Name + " is not client code, so the audit cannot see its layouts");
                    continue;
                }
                Method Register = Found.getMethod("register");
                if (!Modifier.isStatic(Register.getModifiers())) {
                    Problems.add("PROBLEM layout registrar " + Name + " has no static register()");
                    continue;
                }
                Register.invoke(null);
            } catch (NoSuchMethodException Missing) {
                Problems.add("PROBLEM layout registrar " + Name + " has no static register(), so the audit cannot load its layouts");
            } catch (Throwable Problem) {
                Problems.add("PROBLEM layout registrar " + Name + " failed to register: " + describe(Problem));
            }
        }
        return Problems;
    }

    public static List<String> run(List<ItemStack> Stacks, Level World, StorageContainerBlockEntity Small, StorageContainerBlockEntity Tall, List<String> Registrars,
                                   Function<String, InputStream> Resources, List<String> Dump) {
        List<String> Lines = new ArrayList<>(registerAll(Registrars));
        Boolean GlassSafe = glassPath(Lines);
        Recorder Canvas = new Recorder(Resources);
        StorageLayoutContext.useCanvas(Canvas);
        try {
            for (ItemStack Stack : Stacks) {
                for (boolean IsTall : new boolean[]{false, true}) {
                    if (!IsTall && Stack.is(StorageTags.TallOnly)) {
                        continue;
                    }
                    Lines.addAll(audit(Stack, IsTall ? Tall : Small, IsTall, World, Canvas, Resources, Dump, GlassSafe));
                }
            }
        } finally {
            StorageLayoutContext.useCanvas(null);
        }
        return Lines;
    }

    private static Boolean glassPath(List<String> Lines) {
        try {
            RenderType Glass = Sheets.translucentCullBlockSheet();
            boolean Safe = true;
            for (RenderType Loose : List.of(NeoForgeRenderTypes.ITEM_LAYERED_TRANSLUCENT.get(), NeoForgeRenderTypes.ITEM_UNSORTED_TRANSLUCENT.get(), Sheets.translucentItemSheet())) {
                Safe &= LockerItemBuffers.redirect(Loose) == Glass;
            }
            Lines.add(Safe ? "ok translucent item render types share the door glass batch" : "note translucent item render types are drawn apart from the door glass batch");
            return Safe;
        } catch (Throwable Problem) {
            Lines.add("note render types could not be built here (" + describe(Problem) + "), the glass batch check was skipped");
            return null;
        }
    }

    private static List<String> audit(ItemStack Stack, StorageContainerBlockEntity Locker, boolean IsTall, Level World, Recorder Canvas, Function<String, InputStream> Resources,
                                      List<String> Dump, Boolean GlassSafe) {
        List<String> Lines = new ArrayList<>();
        String Name = BuiltInRegistries.ITEM.getKey(Stack.getItem()).toString() + (IsTall ? " tall" : " small");
        Canvas.Draws.clear();
        StorageLayout Layout = StorageLayouts.forStack(Stack, IsTall);
        try {
            Layout.render(new StorageLayoutContext(Locker, Stack, IsTall, new PoseStack(), Type -> new Sink(), AuditLight, 0, 0.0f, World));
        } catch (Throwable Problem) {
            Lines.add("PROBLEM " + Name + ": the layout threw " + describe(Problem));
            return Lines;
        }
        String Self = BuiltInRegistries.ITEM.getKey(Stack.getItem()).toString();
        List<Draw> Content = new ArrayList<>();
        for (Draw Each : Canvas.Draws) {
            if (Each.Kind().equals("item") && Each.Subject().equals(Self)) {
                Content.add(Each);
            }
        }
        if (Content.isEmpty()) {
            for (Draw Each : Canvas.Draws) {
                if (Each.Kind().equals("block") && Each.Subject().equals(Self)) {
                    Content.add(Each);
                }
            }
        }
        if (Content.isEmpty()) {
            for (Draw Each : Canvas.Draws) {
                if (!Each.Kind().equals("item") && !isPedestal(Each.Subject())) {
                    Content.add(Each);
                }
            }
        }
        StringBuilder Record = new StringBuilder();
        Record.append("{\"item\": \"").append(Self).append("\", \"tall\": ").append(IsTall).append(", \"draws\": [");
        for (int Index = 0; Index < Canvas.Draws.size(); Index++) {
            Draw Each = Canvas.Draws.get(Index);
            boolean IsContent = false;
            for (Draw Shown : Content) {
                IsContent |= Shown == Each;
            }
            Record.append(Index == 0 ? "" : ", ").append(drawJson(Each, IsContent));
        }
        Record.append("]}");
        Dump.add(Record.toString());
        if (Content.isEmpty()) {
            Lines.add("PROBLEM " + Name + ": nothing is drawn for the stored item, so it is invisible");
            return Lines;
        }
        float[] Bounds = null;
        List<String> Notes = new ArrayList<>();
        for (Draw Each : Content) {
            Shape Form = shape(Resources, Each);
            if (Form.Note() != null) {
                Notes.add(Form.Note());
            }
            if (Each.Kind().equals("item") && !KnownRenderTypes.contains(Form.RenderType())) {
                Lines.add("PROBLEM " + Name + ": render type " + Form.RenderType() + " has no locker path and may not draw");
            }
            if (Each.Kind().equals("item") && Form.RenderType().endsWith("translucent") && !Boolean.TRUE.equals(GlassSafe)) {
                Lines.add("PROBLEM " + Name + " behind a closed glass door: its translucent model is drawn in a separate batch from the door glass, so the glass can hide it");
            }
            Matrix4f Full = fullMatrix(Resources, Each);
            for (float[] Box : Form.Boxes()) {
                Bounds = grow(Bounds, transformed(Full, Box));
            }
        }
        if (Bounds == null) {
            Lines.add("PROBLEM " + Name + ": the stored item has no geometry, so it is invisible");
            return Lines;
        }
        float Ceiling = StorageGeometry.cavityTop(IsTall);
        float[] Cavity = {StorageGeometry.CavityLeft - Slack, StorageGeometry.CavityFloor - Slack, StorageGeometry.CavityFront - Slack, StorageGeometry.CavityRight + Slack,
                Ceiling + Slack, StorageGeometry.CavityBack + Slack};
        float SizeX = Bounds[3] - Bounds[0];
        float SizeY = Bounds[4] - Bounds[1];
        float SizeZ = Bounds[5] - Bounds[2];
        float Largest = Math.max(SizeX, Math.max(SizeY, SizeZ));
        float Face = Math.max(SizeX * SizeY, Math.max(SizeX * SizeZ, SizeY * SizeZ));
        float Inside = insideShare(Bounds, Cavity);
        String Size = String.format(Locale.ROOT, "x %.2f..%.2f y %.2f..%.2f z %.2f..%.2f", Bounds[0], Bounds[3], Bounds[1], Bounds[4], Bounds[2], Bounds[5]);
        if (Largest < MinExtent || Face < MinFace) {
            Lines.add("PROBLEM " + Name + ": drawn too small to see (" + Size + ")");
        } else if (Inside < InsideShare) {
            Lines.add("PROBLEM " + Name + ": mostly outside the cavity, hidden in the shell (" + Size + ", " + Math.round(Inside * 100.0f) + "% inside)");
        } else if (Bounds[2] >= StorageGeometry.CavityBack || Bounds[4] <= StorageGeometry.CavityFloor) {
            Lines.add("PROBLEM " + Name + ": behind the back panel or under the floor (" + Size + ")");
        } else if (hiddenInPedestal(Bounds, IsTall, Canvas.Draws)) {
            Lines.add("PROBLEM " + Name + ": buried inside the pedestal (" + Size + ")");
        } else if (overflow(Bounds, Ceiling) > ClipLimit) {
            Lines.add("PROBLEM " + Name + ": clips " + String.format(Locale.ROOT, "%.2f", overflow(Bounds, Ceiling)) + " px into the shell or door, so part of it is hidden (" + Size + ")");
        } else {
            Lines.add("ok " + Name + ": " + Size + (Notes.isEmpty() ? "" : " (" + String.join(", ", Notes) + ")"));
        }
        return Lines;
    }

    private static float overflow(float[] Bounds, float Ceiling) {
        return Math.max(Math.max(Math.max(StorageGeometry.CavityLeft - Bounds[0], Bounds[3] - StorageGeometry.CavityRight),
                Math.max(StorageGeometry.CavityFront - Bounds[2], Bounds[5] - StorageGeometry.CavityBack)), Math.max(Bounds[4] - Ceiling, StorageGeometry.CavityFloor - Bounds[1]));
    }

    private static String describe(Throwable Problem) {
        Throwable Root = Problem;
        while (Root.getCause() != null && Root.getCause() != Root) {
            Root = Root.getCause();
        }
        return Root.getClass().getSimpleName() + ": " + Root.getMessage();
    }

    private static boolean isPedestal(String Subject) {
        return Subject.equals(StorageModels.Pedestal.id().toString()) || Subject.equals(StorageModels.TallPedestal.id().toString());
    }

    private static boolean hiddenInPedestal(float[] Bounds, boolean IsTall, List<Draw> Draws) {
        boolean Platform = false;
        for (Draw Each : Draws) {
            Platform |= isPedestal(Each.Subject());
        }
        if (!Platform) {
            return false;
        }
        float Top = InteriorLayout.platformTop(IsTall);
        return Bounds[0] >= 4.0f && Bounds[3] <= 12.0f && Bounds[2] >= 5.0f && Bounds[5] <= 13.0f && Bounds[4] <= Top;
    }

    private static String drawJson(Draw Each, boolean IsContent) {
        float[] Values = new float[16];
        Each.Matrix().get(Values);
        StringBuilder Text = new StringBuilder();
        Text.append("{\"kind\": \"").append(Each.Kind()).append("\", \"subject\": \"").append(Each.Subject()).append("\", \"display\": \"")
                .append(Each.Display().getSerializedName()).append("\", \"centred\": ").append(Each.Centred()).append(", \"content\": ").append(IsContent).append(", \"matrix\": [");
        for (int Index = 0; Index < 16; Index++) {
            Text.append(Index == 0 ? "" : ", ").append(String.format(Locale.ROOT, "%.6f", Values[Index]));
        }
        return Text.append("]}").toString();
    }

    private static float insideShare(float[] Box, float[] Cavity) {
        float Share = 1.0f;
        for (int Axis = 0; Axis < 3; Axis++) {
            float Size = Box[Axis + 3] - Box[Axis];
            if (Size < 1.0e-3f) {
                Share *= Box[Axis] >= Cavity[Axis] && Box[Axis] <= Cavity[Axis + 3] ? 1.0f : 0.0f;
            } else {
                Share *= Math.max(0.0f, Math.min(Box[Axis + 3], Cavity[Axis + 3]) - Math.max(Box[Axis], Cavity[Axis])) / Size;
            }
        }
        return Share;
    }

    private static float[] grow(float[] Bounds, float[] Box) {
        if (Bounds == null) {
            return Box.clone();
        }
        return new float[]{Math.min(Bounds[0], Box[0]), Math.min(Bounds[1], Box[1]), Math.min(Bounds[2], Box[2]), Math.max(Bounds[3], Box[3]), Math.max(Bounds[4], Box[4]),
                Math.max(Bounds[5], Box[5])};
    }

    private static float[] transformed(Matrix4f Full, float[] Box) {
        float[] Result = null;
        for (int Corner = 0; Corner < 8; Corner++) {
            Vector3f Point = new Vector3f((Corner & 1) == 0 ? Box[0] : Box[3], (Corner & 2) == 0 ? Box[1] : Box[4], (Corner & 4) == 0 ? Box[2] : Box[5]);
            Full.transformPosition(Point.div(16.0f));
            Point.mul(16.0f);
            float[] Single = {Point.x, Point.y, Point.z, Point.x, Point.y, Point.z};
            Result = grow(Result, Single);
        }
        return Result;
    }

    private static Matrix4f fullMatrix(Function<String, InputStream> Resources, Draw Each) {
        Matrix4f Full = new Matrix4f(Each.Matrix());
        if (Each.Kind().equals("item")) {
            Full.mul(displayMatrix(displayEntry(Resources, itemModel(Resources, Each.Shown()), Each.Display())));
            Full.translate(-0.5f, -0.5f, -0.5f);
        } else if (Each.Centred()) {
            Full.translate(-0.5f, 0.0f, -0.5f);
        }
        return Full;
    }

    public static JsonObject read(Function<String, InputStream> Resources, String Location) {
        if (Parsed.containsKey(Location)) {
            return Parsed.get(Location);
        }
        ResourceLocation Id = ResourceLocation.parse(Location);
        JsonObject Found = null;
        try (InputStream Stream = Resources.apply("assets/" + Id.getNamespace() + "/models/" + Id.getPath() + ".json")) {
            if (Stream != null) {
                Found = JsonParser.parseReader(new InputStreamReader(Stream, StandardCharsets.UTF_8)).getAsJsonObject();
            }
        } catch (Exception Problem) {
            Found = null;
        }
        Parsed.put(Location, Found);
        return Found;
    }

    private static JsonObject itemModel(Function<String, InputStream> Resources, ItemStack Shown) {
        ResourceLocation Id = BuiltInRegistries.ITEM.getKey(Shown.getItem());
        return read(Resources, Id.getNamespace() + ":item/" + Id.getPath());
    }

    private static String normal(String Parent) {
        return Parent.contains(":") ? Parent : "minecraft:" + Parent;
    }

    private static String kind(Function<String, InputStream> Resources, JsonObject Model) {
        JsonObject Current = base(Model);
        for (int Depth = 0; Current != null && Depth < 12; Depth++) {
            if (Current.has("elements")) {
                return "elements";
            }
            if (!Current.has("parent")) {
                return "empty";
            }
            String Parent = normal(Current.get("parent").getAsString());
            if (Parent.equals("minecraft:item/generated") || Parent.equals("minecraft:builtin/generated") || Parent.equals("minecraft:item/handheld")) {
                return "generated";
            }
            if (Parent.equals("minecraft:builtin/entity")) {
                return "builtin";
            }
            Current = base(read(Resources, Parent));
        }
        return "missing";
    }

    private static JsonObject base(JsonObject Model) {
        if (Model != null && Model.has("loader") && Model.get("loader").getAsString().endsWith("separate_transforms") && Model.has("base")) {
            JsonObject Base = Model.getAsJsonObject("base").deepCopy();
            if (Model.has("display") && !Base.has("display")) {
                Base.add("display", Model.get("display"));
            }
            return Base;
        }
        return Model;
    }

    private static JsonObject vanillaDisplay(String Parent) {
        String Text = switch (Parent) {
            case "minecraft:block/block" -> "{\"gui\":{\"rotation\":[30,225,0],\"translation\":[0,0,0],\"scale\":[0.625,0.625,0.625]},"
                    + "\"ground\":{\"rotation\":[0,0,0],\"translation\":[0,3,0],\"scale\":[0.25,0.25,0.25]},\"fixed\":{\"rotation\":[0,0,0],\"translation\":[0,0,0],\"scale\":[0.5,0.5,0.5]}}";
            case "minecraft:item/generated", "minecraft:item/handheld" -> "{\"ground\":{\"rotation\":[0,0,0],\"translation\":[0,2,0],\"scale\":[0.5,0.5,0.5]},"
                    + "\"fixed\":{\"rotation\":[0,180,0],\"translation\":[0,0,0],\"scale\":[1,1,1]}}";
            default -> "{}";
        };
        return JsonParser.parseString(Text).getAsJsonObject();
    }

    private static JsonObject displayEntry(Function<String, InputStream> Resources, JsonObject Model, ItemDisplayContext Display) {
        if (Display == ItemDisplayContext.NONE || Model == null) {
            return null;
        }
        String Key = Display.getSerializedName();
        JsonObject Current = Model;
        for (int Depth = 0; Current != null && Depth < 12; Depth++) {
            if (Current.has("display") && Current.getAsJsonObject("display").has(Key)) {
                return Current.getAsJsonObject("display").getAsJsonObject(Key);
            }
            JsonObject Base = base(Current);
            if (Base != Current && Base.has("display") && Base.getAsJsonObject("display").has(Key)) {
                return Base.getAsJsonObject("display").getAsJsonObject(Key);
            }
            if (!Base.has("parent")) {
                return null;
            }
            String Parent = normal(Base.get("parent").getAsString());
            JsonObject Next = read(Resources, Parent);
            if (Next == null) {
                JsonObject Vanilla = vanillaDisplay(Parent);
                return Vanilla.has(Key) ? Vanilla.getAsJsonObject(Key) : null;
            }
            Current = Next;
        }
        return null;
    }

    private static float[] triple(JsonObject Entry, String Key, float Fallback) {
        if (Entry == null || !Entry.has(Key)) {
            return new float[]{Fallback, Fallback, Fallback};
        }
        JsonArray Values = Entry.getAsJsonArray(Key);
        return new float[]{Values.get(0).getAsFloat(), Values.get(1).getAsFloat(), Values.get(2).getAsFloat()};
    }

    public static Matrix4f displayMatrix(JsonObject Entry) {
        Matrix4f Result = new Matrix4f();
        if (Entry == null) {
            return Result;
        }
        float[] Move = triple(Entry, "translation", 0.0f);
        float[] Turn = triple(Entry, "rotation", 0.0f);
        float[] Size = triple(Entry, "scale", 1.0f);
        for (int Index = 0; Index < 3; Index++) {
            Move[Index] = Math.max(-80.0f, Math.min(80.0f, Move[Index])) / 16.0f;
        }
        Result.translate(Move[0], Move[1], Move[2]);
        Result.rotate(new Quaternionf().rotationXYZ((float) Math.toRadians(Turn[0]), (float) Math.toRadians(Turn[1]), (float) Math.toRadians(Turn[2])));
        Result.scale(Size[0], Size[1], Size[2]);
        return Result;
    }

    private static Shape shape(Function<String, InputStream> Resources, Draw Each) {
        if (Each.Kind().equals("block")) {
            Shape Modelled = blockShape(Resources, Each.Block());
            if (Modelled != null) {
                return Modelled;
            }
            AABB Bounds = Each.Block().getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).isEmpty() ? new AABB(0, 0, 0, 1, 1, 1)
                    : Each.Block().getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).bounds();
            return new Shape(List.of(new float[]{(float) Bounds.minX * 16.0f, (float) Bounds.minY * 16.0f, (float) Bounds.minZ * 16.0f, (float) Bounds.maxX * 16.0f,
                    (float) Bounds.maxY * 16.0f, (float) Bounds.maxZ * 16.0f}), "", "block shape");
        }
        JsonObject Model = Each.Kind().equals("item") ? itemModel(Resources, Each.Shown()) : read(Resources, Each.Subject());
        if (Model == null) {
            return new Shape(List.of(), "", "model file missing");
        }
        String Type = renderType(Resources, Model);
        String Kind = kind(Resources, Model);
        if (Kind.equals("generated")) {
            return new Shape(List.of(new float[]{0, 0, 7.5f, 16, 16, 8.5f}), Type, null);
        }
        if (Kind.equals("builtin")) {
            return new Shape(List.of(new float[]{4, 8, 4, 12, 16, 12}), Type, "custom renderer, approximate shape");
        }
        List<float[]> Boxes = new ArrayList<>();
        JsonObject Current = base(Model);
        for (int Depth = 0; Current != null && Depth < 12; Depth++) {
            if (Current.has("elements")) {
                for (JsonElement Element : Current.getAsJsonArray("elements")) {
                    Boxes.add(elementBox(Element.getAsJsonObject()));
                }
                break;
            }
            if (!Current.has("parent")) {
                break;
            }
            Current = base(read(Resources, normal(Current.get("parent").getAsString())));
        }
        return new Shape(Boxes, Type, Boxes.isEmpty() ? "no elements" : null);
    }

    private static Shape blockShape(Function<String, InputStream> Resources, BlockState State) {
        ResourceLocation Id = BuiltInRegistries.BLOCK.getKey(State.getBlock());
        JsonObject Variants;
        try (InputStream Stream = Resources.apply("assets/" + Id.getNamespace() + "/blockstates/" + Id.getPath() + ".json")) {
            if (Stream == null) {
                return null;
            }
            JsonObject Root = JsonParser.parseReader(new InputStreamReader(Stream, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!Root.has("variants")) {
                return null;
            }
            Variants = Root.getAsJsonObject("variants");
        } catch (Exception Problem) {
            return null;
        }
        for (Map.Entry<String, JsonElement> Variant : Variants.entrySet()) {
            if (!matches(State, Variant.getKey())) {
                continue;
            }
            JsonElement Choice = Variant.getValue().isJsonArray() ? Variant.getValue().getAsJsonArray().get(0) : Variant.getValue();
            JsonObject Picked = Choice.getAsJsonObject();
            JsonObject Model = read(Resources, normal(Picked.get("model").getAsString()));
            if (Model == null) {
                return null;
            }
            float Turn = Picked.has("y") ? Picked.get("y").getAsFloat() : 0.0f;
            Matrix4f Spin = new Matrix4f().translate(8.0f, 0.0f, 8.0f).rotateY((float) Math.toRadians(-Turn)).translate(-8.0f, 0.0f, -8.0f);
            List<float[]> Boxes = new ArrayList<>();
            JsonObject Current = Model;
            for (int Depth = 0; Current != null && Depth < 12; Depth++) {
                if (Current.has("elements")) {
                    for (JsonElement Element : Current.getAsJsonArray("elements")) {
                        float[] Box = elementBox(Element.getAsJsonObject());
                        float[] Turned = null;
                        for (int Corner = 0; Corner < 8; Corner++) {
                            Vector3f Point = new Vector3f((Corner & 1) == 0 ? Box[0] : Box[3], (Corner & 2) == 0 ? Box[1] : Box[4], (Corner & 4) == 0 ? Box[2] : Box[5]);
                            Spin.transformPosition(Point);
                            Turned = grow(Turned, new float[]{Point.x, Point.y, Point.z, Point.x, Point.y, Point.z});
                        }
                        Boxes.add(Turned);
                    }
                    break;
                }
                if (!Current.has("parent")) {
                    break;
                }
                Current = read(Resources, normal(Current.get("parent").getAsString()));
            }
            return Boxes.isEmpty() ? null : new Shape(Boxes, "", null);
        }
        return null;
    }

    private static boolean matches(BlockState State, String Key) {
        if (Key.isEmpty()) {
            return true;
        }
        for (String Pair : Key.split(",")) {
            String[] Parts = Pair.split("=", 2);
            if (Parts.length != 2) {
                return false;
            }
            boolean Found = false;
            for (Property<?> Value : State.getProperties()) {
                if (Value.getName().equals(Parts[0])) {
                    Found = valueName(State, Value).equals(Parts[1]);
                }
            }
            if (!Found) {
                return false;
            }
        }
        return true;
    }

    private static <T extends Comparable<T>> String valueName(BlockState State, Property<T> Value) {
        return Value.getName(State.getValue(Value));
    }

    private static String renderType(Function<String, InputStream> Resources, JsonObject Model) {
        JsonObject Current = Model;
        for (int Depth = 0; Current != null && Depth < 12; Depth++) {
            if (Current.has("render_type")) {
                return Current.get("render_type").getAsString();
            }
            JsonObject Base = base(Current);
            if (Base != Current && Base.has("render_type")) {
                return Base.get("render_type").getAsString();
            }
            if (!Base.has("parent")) {
                return "";
            }
            Current = read(Resources, normal(Base.get("parent").getAsString()));
        }
        return "";
    }

    private static float[] elementBox(JsonObject Element) {
        float[] From = triple(Element, "from", 0.0f);
        float[] To = triple(Element, "to", 16.0f);
        float[] Box = {Math.min(From[0], To[0]), Math.min(From[1], To[1]), Math.min(From[2], To[2]), Math.max(From[0], To[0]), Math.max(From[1], To[1]), Math.max(From[2], To[2])};
        if (!Element.has("rotation")) {
            return Box;
        }
        JsonObject Turn = Element.getAsJsonObject("rotation");
        float Angle = Turn.has("angle") ? Turn.get("angle").getAsFloat() : 0.0f;
        if (Angle == 0.0f) {
            return Box;
        }
        float[] Origin = triple(Turn, "origin", 8.0f);
        String Axis = Turn.has("axis") ? Turn.get("axis").getAsString() : "y";
        Matrix4f Spin = new Matrix4f().translate(Origin[0], Origin[1], Origin[2]);
        float Radians = (float) Math.toRadians(Angle);
        switch (Axis) {
            case "x" -> Spin.rotateX(Radians);
            case "z" -> Spin.rotateZ(Radians);
            default -> Spin.rotateY(Radians);
        }
        if (Turn.has("rescale") && Turn.get("rescale").getAsBoolean()) {
            float Factor = 1.0f / (float) Math.cos(Math.abs(Radians));
            Spin.scale(Axis.equals("x") ? 1.0f : Factor, Axis.equals("y") ? 1.0f : Factor, Axis.equals("z") ? 1.0f : Factor);
        }
        Spin.translate(-Origin[0], -Origin[1], -Origin[2]);
        float[] Result = null;
        for (int Corner = 0; Corner < 8; Corner++) {
            Vector3f Point = new Vector3f((Corner & 1) == 0 ? Box[0] : Box[3], (Corner & 2) == 0 ? Box[1] : Box[4], (Corner & 4) == 0 ? Box[2] : Box[5]);
            Spin.transformPosition(Point);
            Result = grow(Result, new float[]{Point.x, Point.y, Point.z, Point.x, Point.y, Point.z});
        }
        return Result;
    }
}
