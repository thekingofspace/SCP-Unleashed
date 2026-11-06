package net.rai.scp_unleashed.scp409.client;

import com.mojang.blaze3d.vertex.PoseStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.util.RenderUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public final class Scp409GeoFaces {
    public static final float PixelArea = 256.0f;
    public static final float GroundBand = 2.5f / 16.0f;
    public static final float SmallestFace = 0.75f;
    public static final int CachedEntities = 256;
    private static final Map<BakedGeoModel, Scp409GeoFaces> Tables = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Long, Scp409GeoFaces> Boxes = Collections.synchronizedMap(new LinkedHashMap<>());

    private final List<Face> Faces;
    private final float TotalArea;
    private final float Scale;
    private final Map<UUID, Map<GeoBone, List<Scp409Anchors.Anchor>>> Picks = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, Map<GeoBone, List<Scp409Anchors.Anchor>>> Eldest) {
            return size() > CachedEntities;
        }
    };

    public record Face(@Nullable GeoBone Bone, @Nullable GeoCube Cube, Vector3f Corner, Vector3f AxisS, Vector3f AxisT, Vector3f Normal, float Area) {
    }

    private Scp409GeoFaces(List<Face> Faces) {
        this.Faces = List.copyOf(Faces);
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

    public float scale() {
        return Scale;
    }

    @Nullable
    public static Scp409GeoFaces find(BakedGeoModel Model) {
        return Tables.get(Model);
    }

    public static Scp409GeoFaces remember(BakedGeoModel Model, Set<GeoBone> Shown) {
        Scp409GeoFaces Found = Tables.get(Model);
        if (Found == null) {
            Found = build(Model, Shown);
            Tables.put(Model, Found);
        }
        return Found;
    }

    public static void clear() {
        Tables.clear();
        Boxes.clear();
    }

    public static Scp409GeoFaces box(float Width, float Height) {
        long Key = (long) Math.round(Width * 64.0f) << 32 | Math.round(Height * 64.0f);
        synchronized (Boxes) {
            Scp409GeoFaces Found = Boxes.get(Key);
            if (Found == null) {
                float Half = Width * 0.5f;
                float Side = Width * PixelArea;
                float Tall = Height * 16.0f * Width * 16.0f;
                List<Face> Made = new ArrayList<>();
                Made.add(new Face(null, null, new Vector3f(-Half, 0, -Half), new Vector3f(0, Height, 0), new Vector3f(0, 0, Width), new Vector3f(-1, 0, 0), Tall));
                Made.add(new Face(null, null, new Vector3f(Half, 0, -Half), new Vector3f(0, Height, 0), new Vector3f(0, 0, Width), new Vector3f(1, 0, 0), Tall));
                Made.add(new Face(null, null, new Vector3f(-Half, Height, -Half), new Vector3f(Width, 0, 0), new Vector3f(0, 0, Width), new Vector3f(0, 1, 0), Side * Width));
                Made.add(new Face(null, null, new Vector3f(-Half, 0, -Half), new Vector3f(Width, 0, 0), new Vector3f(0, Height, 0), new Vector3f(0, 0, -1), Tall));
                Made.add(new Face(null, null, new Vector3f(-Half, 0, Half), new Vector3f(Width, 0, 0), new Vector3f(0, Height, 0), new Vector3f(0, 0, 1), Tall));
                Found = new Scp409GeoFaces(Made);
                Boxes.put(Key, Found);
            }
            return Found;
        }
    }

    public synchronized List<Scp409Anchors.Anchor> allAnchors(UUID Id) {
        List<Scp409Anchors.Anchor> Every = new ArrayList<>();
        for (List<Scp409Anchors.Anchor> Group : anchors(Id).values()) {
            Every.addAll(Group);
        }
        return Every;
    }

    public static Scp409GeoFaces build(BakedGeoModel Model, @Nullable Set<GeoBone> Shown) {
        List<Face> Made = new ArrayList<>();
        float[] Lowest = {Float.MAX_VALUE};
        for (GeoBone Bone : Model.topLevelBones()) {
            lowest(Bone, Lowest);
        }
        for (GeoBone Bone : Model.topLevelBones()) {
            walk(Bone, Made, Lowest[0], Shown);
        }
        return new Scp409GeoFaces(Made);
    }

    private static void lowest(GeoBone Bone, float[] Lowest) {
        for (GeoCube Cube : Bone.getCubes()) {
            for (GeoQuad Quad : Cube.quads()) {
                if (Quad == null) {
                    continue;
                }
                for (GeoVertex Vertex : Quad.vertices()) {
                    Lowest[0] = Math.min(Lowest[0], Vertex.position().y());
                }
            }
        }
        for (GeoBone Child : Bone.getChildBones()) {
            lowest(Child, Lowest);
        }
    }

    private static void walk(GeoBone Bone, List<Face> Made, float Lowest, @Nullable Set<GeoBone> Shown) {
        if (!Boolean.TRUE.equals(Bone.shouldNeverRender()) && (Shown == null || Shown.contains(Bone))) {
            for (GeoCube Cube : Bone.getCubes()) {
                for (GeoQuad Quad : Cube.quads()) {
                    if (Quad == null || Quad.vertices().length < 4) {
                        continue;
                    }
                    Vector3f Corner = new Vector3f(Quad.vertices()[0].position());
                    Vector3f AxisS = new Vector3f(Quad.vertices()[1].position()).sub(Corner);
                    Vector3f AxisT = new Vector3f(Quad.vertices()[3].position()).sub(Corner);
                    Vector3f Cross = new Vector3f(AxisS).cross(AxisT);
                    float Area = Cross.length() * PixelArea;
                    if (Area < SmallestFace) {
                        continue;
                    }
                    Vector3f Normal = new Vector3f(Quad.normal());
                    if (Normal.lengthSquared() < 1.0e-6f) {
                        Normal = new Vector3f(Cross);
                    }
                    Normal.normalize();
                    if (Normal.y < -0.7f && Corner.y <= Lowest + GroundBand) {
                        continue;
                    }
                    Made.add(new Face(Bone, Cube, Corner, AxisS, AxisT, Normal, Area));
                }
            }
        }
        for (GeoBone Child : Bone.getChildBones()) {
            walk(Child, Made, Lowest, Shown);
        }
    }

    public synchronized Map<GeoBone, List<Scp409Anchors.Anchor>> anchors(UUID Id) {
        Map<GeoBone, List<Scp409Anchors.Anchor>> Found = Picks.get(Id);
        if (Found == null) {
            float[] Areas = new float[Faces.size()];
            for (int Index = 0; Index < Areas.length; Index++) {
                Areas[Index] = Faces.get(Index).Area();
            }
            Found = new IdentityHashMap<>();
            for (Scp409Anchors.Anchor Item : Scp409Anchors.pick(Areas, TotalArea, Scp409Anchors.seed(Id))) {
                Found.computeIfAbsent(Faces.get(Item.Face()).Bone(), Key -> new ArrayList<>()).add(Item);
            }
            Picks.put(Id, Found);
        }
        return Found;
    }

    public void place(PoseStack Pose, List<Scp409Anchors.Anchor> Anchors, float Growth, int Stage, Scp409Anchors.Sink Draw) {
        for (Scp409Anchors.Anchor Item : Anchors) {
            float Size = Scp409Anchors.size(Item, Growth, Stage, Scale);
            if (Size <= 0.0f || Item.Face() >= Faces.size()) {
                continue;
            }
            Face Where = Faces.get(Item.Face());
            Pose.pushPose();
            if (Where.Cube() != null) {
                RenderUtil.translateToPivotPoint(Pose, Where.Cube());
                RenderUtil.rotateMatrixAroundCube(Pose, Where.Cube());
                RenderUtil.translateAwayFromPivotPoint(Pose, Where.Cube());
            }
            float X = Where.Corner().x + Where.AxisS().x * Item.S() + Where.AxisT().x * Item.T();
            float Y = Where.Corner().y + Where.AxisS().y * Item.S() + Where.AxisT().y * Item.T();
            float Z = Where.Corner().z + Where.AxisS().z * Item.S() + Where.AxisT().z * Item.T();
            Pose.translate(X, Y, Z);
            Scp409Anchors.orient(Pose, Where.Normal().x, Where.Normal().y, Where.Normal().z, Item);
            Draw.cluster(Pose, Item.Variant(), Size);
            Pose.popPose();
        }
    }
}
