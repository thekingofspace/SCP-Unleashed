package net.rai.scp_unleashed.scp001scarlet.court.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp001scarlet.court.CourtDaughter;
import net.rai.scp_unleashed.scp001scarlet.court.CourtMinion;
import net.rai.scp_unleashed.scp001scarlet.court.DaughterEyes;
import net.rai.scp_unleashed.scp001scarlet.court.DaughterThorns;
import net.rai.scp_unleashed.scp001scarlet.court.EyesImage;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.state.BoneSnapshot;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

import java.util.Map;
import java.util.WeakHashMap;

public class CourtGeoModel<T extends Entity & GeoAnimatable> extends GeoModel<T> {
    public static final String Folder = "scarlet_court";
    public static final String HeadBone = "head";
    public static final String BloomBone = "bloom";
    public static final String LidBone = "great_lid";
    public static final String SigilBone = "sigil";
    public static final float YawLimit = 40.0f;
    public static final float PitchLimit = 30.0f;
    public static final float LookTicks = 6.0f;
    private final ResourceLocation ModelPath;
    private final ResourceLocation TexturePath;
    private final ResourceLocation AnimationPath;
    private final Map<Entity, float[]> Looks = new WeakHashMap<>();

    public CourtGeoModel(String Name) {
        ModelPath = location("geo/entity/" + Folder + "/" + Name + ".geo.json");
        TexturePath = texture(Name, "");
        AnimationPath = location("animations/entity/" + Folder + "/" + Name + ".animation.json");
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    public static ResourceLocation texture(String Name, String Suffix) {
        return location("textures/entity/" + Folder + "/" + Name + Suffix + ".png");
    }

    @Override
    public ResourceLocation getModelResource(T Subject) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(T Subject) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(T Subject) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(T Subject, long InstanceId, AnimationState<T> Animated) {
        GeoBone Bloom = getAnimationProcessor().getBone(BloomBone);
        if (Bloom != null) {
            Bloom.setHidden(!(Subject instanceof DaughterThorns Thorns && Thorns.phaseTwo()));
        }
        GeoBone Lid = getAnimationProcessor().getBone(LidBone);
        if (Lid != null) {
            Lid.setHidden(Subject instanceof DaughterEyes Eyes && Eyes.phaseTwo());
        }
        GeoBone Sigil = getAnimationProcessor().getBone(SigilBone);
        if (Sigil != null) {
            Sigil.setHidden(Subject instanceof EyesImage);
        }
        GeoBone Head = getAnimationProcessor().getBone(HeadBone);
        if (Head == null || !(Subject instanceof LivingEntity Living) || Living.isDeadOrDying()) {
            return;
        }
        float Now = Subject.tickCount + (float) Animated.getPartialTick();
        float[] Look = Looks.computeIfAbsent(Subject, Key -> new float[]{0.0f, Now});
        float Step = Mth.clamp(Now - Look[1], 0.0f, 4.0f) / LookTicks;
        Look[1] = Now;
        Look[0] = Mth.clamp(Look[0] + (busy(Subject) ? -Step : Step), 0.0f, 1.0f);
        if (Look[0] <= 0.0f) {
            return;
        }
        EntityModelData Data = Animated.getData(DataTickets.ENTITY_MODEL_DATA);
        if (Data == null) {
            return;
        }
        blend(Head, Mth.clamp(Data.headPitch(), -PitchLimit, PitchLimit), Mth.clamp(Mth.wrapDegrees(Data.netHeadYaw()), -YawLimit, YawLimit),
                Look[0] * Look[0] * (3.0f - 2.0f * Look[0]));
    }

    public static void blend(GeoBone Bone, float Pitch, float Yaw, float Weight) {
        BoneSnapshot Rest = Bone.getInitialSnapshot();
        Bone.setRotX(Mth.lerp(Weight, Bone.getRotX(), Rest.getRotX() - Pitch * Mth.DEG_TO_RAD));
        Bone.setRotY(Mth.lerp(Weight, Bone.getRotY(), Rest.getRotY() - Yaw * Mth.DEG_TO_RAD));
    }

    private static boolean busy(Entity Subject) {
        if (Subject instanceof CourtMinion Minion) {
            return Minion.busy() || Minion.rising();
        }
        if (Subject instanceof CourtDaughter Daughter) {
            return Daughter.action() != CourtDaughter.None;
        }
        return false;
    }
}
