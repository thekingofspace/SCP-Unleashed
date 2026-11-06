package net.rai.scp_unleashed.scp409.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public final class Scp409Faces {
    public static final int Head = 0;
    public static final int Body = 1;
    public static final int Plain = 2;
    public static final float GroundBand = 2.5f;
    public static final float SmallestFace = 0.75f;
    public static final int CachedEntities = 256;
    private static final Logger Log = LogUtils.getLogger();
    private static final Map<Object, Scp409Faces> Tables = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Long, Scp409Faces> Boxes = Collections.synchronizedMap(new LinkedHashMap<>());
    @Nullable
    private static Field CubesField;
    @Nullable
    private static Field ChildrenField;
    private static boolean Resolved;

    private final List<Face> Faces;
    private final float TotalArea;
    private final float Scale;
    @Nullable
    private final Youth Baby;
    private final Map<UUID, List<Scp409Anchors.Anchor>> Picks = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, List<Scp409Anchors.Anchor>> Eldest) {
            return size() > CachedEntities;
        }
    };

    public record Face(ModelPart[] Path, int Group, Vector3f Corner, Vector3f AxisS, Vector3f AxisT, Vector3f Normal, float Area) {
    }

    public record Youth(boolean ScaleHead, float HeadYOffset, float HeadZOffset, float HeadScale, float BodyScale, float BodyYOffset) {
    }

    private record Root(ModelPart Part, int Group) {
    }

    private record Pending(ModelPart[] Path, ModelPart.Cube Cube, float Bottom, Matrix4f Rest, int Group) {
    }

    private Scp409Faces(List<Face> Faces, @Nullable Youth Baby) {
        this.Faces = List.copyOf(Faces);
        this.Baby = Baby;
        float Sum = 0.0f;
        for (Face Item : Faces) {
            Sum += Item.Area();
        }
        this.TotalArea = Sum;
        this.Scale = Scp409Anchors.sizeScale(Scp409Anchors.body(Sum));
    }

    public List<Face> faces() {
        return Faces;
    }

    public boolean isEmpty() {
        return Faces.isEmpty();
    }

    public float totalArea() {
        return TotalArea;
    }

    public float scale() {
        return Scale;
    }

    public synchronized List<Scp409Anchors.Anchor> anchors(UUID Id) {
        List<Scp409Anchors.Anchor> Found = Picks.get(Id);
        if (Found == null) {
            float[] Areas = new float[Faces.size()];
            for (int Index = 0; Index < Areas.length; Index++) {
                Areas[Index] = Faces.get(Index).Area();
            }
            Found = Scp409Anchors.pick(Areas, TotalArea, Scp409Anchors.seed(Id));
            Picks.put(Id, Found);
        }
        return Found;
    }

    public static Scp409Faces of(EntityModel<?> Model) {
        Scp409Faces Found = Tables.get(Model);
        if (Found == null) {
            Found = build(Model);
            Tables.put(Model, Found);
        }
        return Found;
    }

    public static Scp409Faces box(float Width, float Height) {
        long Key = (long) Math.round(Width * 64.0f) << 32 | Math.round(Height * 64.0f);
        synchronized (Boxes) {
            Scp409Faces Found = Boxes.get(Key);
            if (Found == null) {
                Found = buildBox(Width, Height);
                Boxes.put(Key, Found);
            }
            return Found;
        }
    }

    public static void clear() {
        Tables.clear();
        Boxes.clear();
    }

    private static void resolve() {
        if (Resolved) {
            return;
        }
        Resolved = true;
        try {
            Field Cubes = ModelPart.class.getDeclaredField("cubes");
            Field Children = ModelPart.class.getDeclaredField("children");
            Cubes.setAccessible(true);
            Children.setAccessible(true);
            CubesField = Cubes;
            ChildrenField = Children;
        } catch (Throwable Problem) {
            Log.warn("SCP-409 crystals cannot read model parts; growths fall back to the bounding box", Problem);
        }
    }

    @SuppressWarnings("unchecked")
    public static List<ModelPart.Cube> cubes(ModelPart Part) {
        resolve();
        if (CubesField == null) {
            return List.of();
        }
        try {
            return (List<ModelPart.Cube>) CubesField.get(Part);
        } catch (Throwable Problem) {
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    public static Collection<ModelPart> children(ModelPart Part) {
        resolve();
        if (ChildrenField == null) {
            return List.of();
        }
        try {
            return ((Map<String, ModelPart>) ChildrenField.get(Part)).values();
        } catch (Throwable Problem) {
            return List.of();
        }
    }

    public static Scp409Faces build(EntityModel<?> Model) {
        List<Root> Roots = roots(Model);
        Youth Baby = Model instanceof AgeableListModel<?> ? youth(Model) : null;
        List<Pending> Waiting = new ArrayList<>();
        Set<String> Seen = new HashSet<>();
        for (Root Start : Roots) {
            walk(Start.Part(), new ArrayList<>(), new Matrix4f(), Start.Group(), Waiting, Seen, Collections.newSetFromMap(new IdentityHashMap<>()));
        }
        float Lowest = -Float.MAX_VALUE;
        for (Pending Entry : Waiting) {
            Lowest = Math.max(Lowest, Entry.Bottom());
        }
        List<Face> Made = new ArrayList<>();
        for (Pending Entry : Waiting) {
            addFaces(Made, Entry.Path(), Entry.Group(), Entry.Cube(), Entry.Rest(), Lowest);
        }
        return new Scp409Faces(Made, Baby);
    }

    private static void walk(ModelPart Part, List<ModelPart> Chain, Matrix4f Parent, int Group, List<Pending> Waiting, Set<String> Seen, Set<ModelPart> Visited) {
        if (!Visited.add(Part) || !Part.visible) {
            return;
        }
        List<ModelPart> Here = new ArrayList<>(Chain);
        Here.add(Part);
        Matrix4f Rest = new Matrix4f(Parent);
        PartPose Initial = Part.getInitialPose();
        Rest.translate(Initial.x, Initial.y, Initial.z);
        if (Initial.xRot != 0.0f || Initial.yRot != 0.0f || Initial.zRot != 0.0f) {
            Rest.rotateZYX(Initial.zRot, Initial.yRot, Initial.xRot);
        }
        ModelPart[] Path = Here.toArray(new ModelPart[0]);
        for (ModelPart.Cube Cube : Part.skipDraw ? List.<ModelPart.Cube>of() : cubes(Part)) {
            Vector3f Low = Rest.transformPosition(new Vector3f(Cube.minX, Cube.minY, Cube.minZ));
            Vector3f High = Rest.transformPosition(new Vector3f(Cube.maxX, Cube.maxY, Cube.maxZ));
            String Key = Group + ":" + Math.round(Math.min(Low.x, High.x) * 10) + "," + Math.round(Math.min(Low.y, High.y) * 10) + "," + Math.round(Math.min(Low.z, High.z) * 10)
                    + ":" + Math.round(Math.max(Low.x, High.x) * 10) + "," + Math.round(Math.max(Low.y, High.y) * 10) + "," + Math.round(Math.max(Low.z, High.z) * 10);
            if (!Seen.add(Key)) {
                continue;
            }
            Waiting.add(new Pending(Path, Cube, Math.max(Low.y, High.y), Rest, Group));
        }
        for (ModelPart Child : children(Part)) {
            walk(Child, Here, Rest, Group, Waiting, Seen, Visited);
        }
    }

    private static void addFaces(List<Face> Made, ModelPart[] Path, int Group, ModelPart.Cube Cube, Matrix4f Rest, float Lowest) {
        float X0 = Cube.minX;
        float Y0 = Cube.minY;
        float Z0 = Cube.minZ;
        float X1 = Cube.maxX;
        float Y1 = Cube.maxY;
        float Z1 = Cube.maxZ;
        add(Made, Path, Group, Rest, Lowest, new Vector3f(X0, Y0, Z0), new Vector3f(0, Y1 - Y0, 0), new Vector3f(0, 0, Z1 - Z0), new Vector3f(-1, 0, 0));
        add(Made, Path, Group, Rest, Lowest, new Vector3f(X1, Y0, Z0), new Vector3f(0, Y1 - Y0, 0), new Vector3f(0, 0, Z1 - Z0), new Vector3f(1, 0, 0));
        add(Made, Path, Group, Rest, Lowest, new Vector3f(X0, Y0, Z0), new Vector3f(X1 - X0, 0, 0), new Vector3f(0, 0, Z1 - Z0), new Vector3f(0, -1, 0));
        add(Made, Path, Group, Rest, Lowest, new Vector3f(X0, Y1, Z0), new Vector3f(X1 - X0, 0, 0), new Vector3f(0, 0, Z1 - Z0), new Vector3f(0, 1, 0));
        add(Made, Path, Group, Rest, Lowest, new Vector3f(X0, Y0, Z0), new Vector3f(X1 - X0, 0, 0), new Vector3f(0, Y1 - Y0, 0), new Vector3f(0, 0, -1));
        add(Made, Path, Group, Rest, Lowest, new Vector3f(X0, Y0, Z1), new Vector3f(X1 - X0, 0, 0), new Vector3f(0, Y1 - Y0, 0), new Vector3f(0, 0, 1));
    }

    private static void add(List<Face> Made, ModelPart[] Path, int Group, Matrix4f Rest, float Lowest, Vector3f Corner, Vector3f AxisS, Vector3f AxisT, Vector3f Normal) {
        float Area = AxisS.length() * AxisT.length();
        if (Area < SmallestFace) {
            return;
        }
        Vector3f RestNormal = Rest.transformDirection(new Vector3f(Normal)).normalize();
        if (RestNormal.y > 0.7f) {
            Vector3f Middle = Rest.transformPosition(new Vector3f(Corner).add(new Vector3f(AxisS).mul(0.5f)).add(new Vector3f(AxisT).mul(0.5f)));
            if (Middle.y >= Lowest - GroundBand) {
                return;
            }
        }
        Made.add(new Face(Path, Group, Corner, AxisS, AxisT, Normal, Area));
    }

    private static Scp409Faces buildBox(float Width, float Height) {
        float Half = Width * 8.0f;
        float Bottom = 24.0f;
        float Top = Bottom - Height * 16.0f;
        List<Face> Made = new ArrayList<>();
        ModelPart[] Path = new ModelPart[0];
        float Side = Width * 16.0f;
        float Tall = Height * 16.0f;
        Made.add(new Face(Path, Plain, new Vector3f(-Half, Top, -Half), new Vector3f(0, Tall, 0), new Vector3f(0, 0, Side), new Vector3f(-1, 0, 0), Tall * Side));
        Made.add(new Face(Path, Plain, new Vector3f(Half, Top, -Half), new Vector3f(0, Tall, 0), new Vector3f(0, 0, Side), new Vector3f(1, 0, 0), Tall * Side));
        Made.add(new Face(Path, Plain, new Vector3f(-Half, Top, -Half), new Vector3f(Side, 0, 0), new Vector3f(0, 0, Side), new Vector3f(0, -1, 0), Side * Side));
        Made.add(new Face(Path, Plain, new Vector3f(-Half, Top, -Half), new Vector3f(Side, 0, 0), new Vector3f(0, Tall, 0), new Vector3f(0, 0, -1), Tall * Side));
        Made.add(new Face(Path, Plain, new Vector3f(-Half, Top, Half), new Vector3f(Side, 0, 0), new Vector3f(0, Tall, 0), new Vector3f(0, 0, 1), Tall * Side));
        return new Scp409Faces(Made, null);
    }

    private static List<Root> roots(EntityModel<?> Model) {
        List<Root> Found = new ArrayList<>();
        try {
            if (Model instanceof HierarchicalModel<?> Tree) {
                Found.add(new Root(Tree.root(), Plain));
                return Found;
            }
            if (Model instanceof AgeableListModel<?>) {
                for (ModelPart Part : invokeParts(Model, "headParts")) {
                    Found.add(new Root(Part, Head));
                }
                for (ModelPart Part : invokeParts(Model, "bodyParts")) {
                    Found.add(new Root(Part, Body));
                }
                if (!Found.isEmpty()) {
                    return Found;
                }
            }
            if (Model instanceof ListModel<?> Listed) {
                for (ModelPart Part : Listed.parts()) {
                    Found.add(new Root(Part, Plain));
                }
                return Found;
            }
        } catch (Throwable Problem) {
            Log.debug("SCP-409 crystals could not list the parts of {}", Model.getClass().getName(), Problem);
            Found.clear();
        }
        return fieldRoots(Model);
    }

    private static Iterable<ModelPart> invokeParts(EntityModel<?> Model, String Name) throws ReflectiveOperationException {
        Class<?> Kind = Model.getClass();
        while (Kind != null && Kind != Object.class) {
            try {
                Method Found = Kind.getDeclaredMethod(Name);
                Found.setAccessible(true);
                Object Result = Found.invoke(Model);
                List<ModelPart> Parts = new ArrayList<>();
                if (Result instanceof Iterable<?> Many) {
                    for (Object Item : Many) {
                        if (Item instanceof ModelPart Part) {
                            Parts.add(Part);
                        }
                    }
                }
                return Parts;
            } catch (NoSuchMethodException Missing) {
                Kind = Kind.getSuperclass();
            }
        }
        return List.of();
    }

    private static List<Root> fieldRoots(EntityModel<?> Model) {
        List<ModelPart> Candidates = new ArrayList<>();
        Class<?> Kind = Model.getClass();
        while (Kind != null && Kind != EntityModel.class && Kind != Object.class) {
            for (Field Item : Kind.getDeclaredFields()) {
                try {
                    if (ModelPart.class.isAssignableFrom(Item.getType())) {
                        Item.setAccessible(true);
                        Object Value = Item.get(Model);
                        if (Value instanceof ModelPart Part) {
                            Candidates.add(Part);
                        }
                    } else if (Item.getType().isArray() && ModelPart.class.isAssignableFrom(Item.getType().getComponentType())) {
                        Item.setAccessible(true);
                        Object Value = Item.get(Model);
                        if (Value instanceof ModelPart[] Parts) {
                            Collections.addAll(Candidates, Parts);
                        }
                    }
                } catch (Throwable Problem) {
                    Log.debug("SCP-409 crystals skipped field {}", Item.getName());
                }
            }
            Kind = Kind.getSuperclass();
        }
        Set<ModelPart> Descendants = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ModelPart Part : Candidates) {
            collect(Part, Descendants);
        }
        List<Root> Found = new ArrayList<>();
        Set<ModelPart> Used = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ModelPart Part : Candidates) {
            if (!Descendants.contains(Part) && Used.add(Part)) {
                Found.add(new Root(Part, Plain));
            }
        }
        return Found;
    }

    private static void collect(ModelPart Part, Set<ModelPart> Into) {
        for (ModelPart Child : children(Part)) {
            if (Into.add(Child)) {
                collect(Child, Into);
            }
        }
    }

    @Nullable
    private static Youth youth(EntityModel<?> Model) {
        try {
            Class<?> Kind = AgeableListModel.class;
            return new Youth(bool(Kind, Model, "scaleHead"), number(Kind, Model, "babyYHeadOffset"), number(Kind, Model, "babyZHeadOffset"),
                    number(Kind, Model, "babyHeadScale"), number(Kind, Model, "babyBodyScale"), number(Kind, Model, "bodyYOffset"));
        } catch (Throwable Problem) {
            return null;
        }
    }

    private static boolean bool(Class<?> Kind, Object Owner, String Name) throws ReflectiveOperationException {
        Field Found = Kind.getDeclaredField(Name);
        Found.setAccessible(true);
        return Found.getBoolean(Owner);
    }

    private static float number(Class<?> Kind, Object Owner, String Name) throws ReflectiveOperationException {
        Field Found = Kind.getDeclaredField(Name);
        Found.setAccessible(true);
        return Found.getFloat(Owner);
    }

    public void place(PoseStack Pose, boolean Young, List<Scp409Anchors.Anchor> Anchors, float Growth, int Stage, @Nullable ModelPart Only, Scp409Anchors.Sink Draw) {
        for (Scp409Anchors.Anchor Item : Anchors) {
            float Size = Scp409Anchors.size(Item, Growth, Stage, Scale);
            if (Size <= 0.0f || Item.Face() >= Faces.size()) {
                continue;
            }
            Face Where = Faces.get(Item.Face());
            if (Only != null && (Where.Path().length == 0 || Where.Path()[Where.Path().length - 1] != Only)) {
                continue;
            }
            if (!visible(Where.Path())) {
                continue;
            }
            Pose.pushPose();
            if (Only == null && Young && Baby != null && Where.Group() != Plain) {
                youngPose(Pose, Where.Group());
            }
            for (ModelPart Part : Where.Path()) {
                Part.translateAndRotate(Pose);
            }
            float X = Where.Corner().x + Where.AxisS().x * Item.S() + Where.AxisT().x * Item.T();
            float Y = Where.Corner().y + Where.AxisS().y * Item.S() + Where.AxisT().y * Item.T();
            float Z = Where.Corner().z + Where.AxisS().z * Item.S() + Where.AxisT().z * Item.T();
            Pose.translate(X / 16.0f, Y / 16.0f, Z / 16.0f);
            Scp409Anchors.orient(Pose, Where.Normal().x, Where.Normal().y, Where.Normal().z, Item);
            Draw.cluster(Pose, Item.Variant(), Size);
            Pose.popPose();
        }
    }

    private void youngPose(PoseStack Pose, int Group) {
        Youth Young = Baby;
        if (Young == null) {
            return;
        }
        if (Group == Head) {
            if (Young.ScaleHead()) {
                float Factor = 1.5f / Young.HeadScale();
                Pose.scale(Factor, Factor, Factor);
            }
            Pose.translate(0.0f, Young.HeadYOffset() / 16.0f, Young.HeadZOffset() / 16.0f);
        } else {
            float Factor = 1.0f / Young.BodyScale();
            Pose.scale(Factor, Factor, Factor);
            Pose.translate(0.0f, Young.BodyYOffset() / 16.0f, 0.0f);
        }
    }

    private static boolean visible(ModelPart[] Path) {
        for (ModelPart Part : Path) {
            if (!Part.visible) {
                return false;
            }
        }
        return Path.length == 0 || !Path[Path.length - 1].skipDraw;
    }
}
