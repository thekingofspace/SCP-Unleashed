package net.rai.scp_unleashed.scp409.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.dclass.DClassLike;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Scp409GeoLayer<T extends GeoAnimatable> extends GeoRenderLayer<T> {
    @Nullable
    private Scp409GeoFaces Table;
    @Nullable
    private Map<GeoBone, List<Scp409Anchors.Anchor>> Anchors;
    @Nullable
    private Scp409ClientState.Entry Current;
    private boolean WithCore;
    @Nullable
    private BakedGeoModel Learning;
    private final Set<GeoBone> Shown = Collections.newSetFromMap(new IdentityHashMap<>());

    public Scp409GeoLayer(GeoRenderer<T> Renderer) {
        super(Renderer);
    }

    @Nullable
    public static LivingEntity source(Object Animatable) {
        if (!(Animatable instanceof LivingEntity Living)) {
            return null;
        }
        if (Living instanceof DClassLike Body) {
            if (Body.isSpecimen()) {
                return null;
            }
            if (Body.isPuppet() && Body.puppetOwner() != null) {
                return Body.puppetOwner();
            }
        }
        return Living;
    }

    @Override
    public void preRender(PoseStack Pose, T Animatable, BakedGeoModel Model, RenderType Type, MultiBufferSource Buffers, VertexConsumer Buffer, float Partial, int Light, int Overlay) {
        Table = null;
        Anchors = null;
        Current = null;
        Learning = null;
        LivingEntity Source = source(Animatable);
        if (Source == null || ((LivingEntity) Animatable).isInvisible()) {
            return;
        }
        Scp409ClientState.Entry Found = Scp409ClientState.get(Source.getId());
        if (Found == null || !Found.visible()) {
            return;
        }
        Scp409GeoFaces Faces = Scp409GeoFaces.find(Model);
        if (Faces == null) {
            Learning = Model;
            Shown.clear();
            return;
        }
        if (Faces.isEmpty()) {
            return;
        }
        Table = Faces;
        Anchors = Faces.anchors(Source.getUUID());
        Current = Found;
        WithCore = Scp409CrystalLayer.nearCamera((LivingEntity) Animatable);
    }

    @Override
    public void renderForBone(PoseStack Pose, T Animatable, GeoBone Bone, RenderType Type, MultiBufferSource Buffers, VertexConsumer Buffer, float Partial, int Light, int Overlay) {
        if (Learning != null && !Bone.isHidden()) {
            Shown.add(Bone);
            return;
        }
        Scp409GeoFaces Faces = Table;
        Map<GeoBone, List<Scp409Anchors.Anchor>> Picked = Anchors;
        Scp409ClientState.Entry Found = Current;
        if (Faces == null || Picked == null || Found == null || Bone.isHidden()) {
            return;
        }
        List<Scp409Anchors.Anchor> Here = Picked.get(Bone);
        if (Here == null || Here.isEmpty()) {
            return;
        }
        boolean Core = WithCore;
        Faces.place(Pose, Here, Found.Growth(), Found.Stage(), (Where, Variant, Size) -> Scp409Crystals.cluster(Where, Buffers, Light, Variant, Size, Core));
    }

    @Override
    public void render(PoseStack Pose, T Animatable, BakedGeoModel Model, RenderType Type, MultiBufferSource Buffers, VertexConsumer Buffer, float Partial, int Light, int Overlay) {
        if (Learning != null) {
            Scp409GeoFaces.remember(Learning, Shown);
            Learning = null;
            Shown.clear();
            return;
        }
        if (Table != null) {
            Table = null;
            Anchors = null;
            Current = null;
            return;
        }
        LivingEntity Source = source(Animatable);
        if (Source == null || !(Animatable instanceof LivingEntity Living) || Living.isInvisible()) {
            return;
        }
        Scp409ClientState.Entry Found = Scp409ClientState.get(Source.getId());
        Scp409GeoFaces Learned = Scp409GeoFaces.find(Model);
        if (Found == null || !Found.visible() || Learned == null || !Learned.isEmpty()) {
            return;
        }
        float Scale = Math.max(0.01f, Living.getScale());
        Scp409GeoFaces Box = Scp409GeoFaces.box(Living.getBbWidth() / Scale, Living.getBbHeight() / Scale);
        boolean Core = Scp409CrystalLayer.nearCamera(Living);
        Box.place(Pose, Box.allAnchors(Source.getUUID()), Found.Growth(), Found.Stage(), (Where, Variant, Size) -> Scp409Crystals.cluster(Where, Buffers, Light, Variant, Size, Core));
    }
}
