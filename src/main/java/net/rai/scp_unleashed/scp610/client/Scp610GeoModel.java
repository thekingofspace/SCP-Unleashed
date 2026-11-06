package net.rai.scp_unleashed.scp610.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp610.Scp610Controller;
import net.rai.scp_unleashed.scp610.Scp610Creature;
import net.rai.scp_unleashed.scp610.Scp610HiveLord;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.state.BoneSnapshot;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

public class Scp610GeoModel<T extends Scp610Creature> extends GeoModel<T> {
    public static final String RootBone = "root";
    public static final String LookBone = "look";
    public static final String EyeBone = "eye";
    public static final String PupilBone = "pupil";
    public static final String BodyController = "body";
    public static final float EyeLimit = 35.0f;
    public static final float EyeFollow = 0.12f;
    private final ResourceLocation ModelPath;
    private final ResourceLocation TexturePath;
    private final ResourceLocation AnimationPath;
    private float EyeYaw;
    private float EyePitch;
    private float Dilation = 1.0f;

    public Scp610GeoModel(String Name) {
        ModelPath = location("geo/entity/scp610/" + Name + ".geo.json");
        TexturePath = texture(Name);
        AnimationPath = location("animations/entity/scp610/" + Name + ".animation.json");
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    public static ResourceLocation texture(String Name) {
        return location("textures/entity/scp610/" + Name + ".png");
    }

    public static ResourceLocation glow(String Name) {
        return location("textures/entity/scp610/" + Name + "_glow.png");
    }

    @Override
    public ResourceLocation getModelResource(T Creature) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(T Creature) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(T Creature) {
        return AnimationPath;
    }

    public static boolean readyToDraw(Scp610Creature Creature, long InstanceId) {
        if (!Creature.isEmerging()) {
            return true;
        }
        String Clip = Creature.spawnClip();
        AnimationController<?> Controller = Creature.getAnimatableInstanceCache().getManagerForId(InstanceId).getAnimationControllers().get(BodyController);
        return Clip == null || Controller instanceof Scp610Controller Body && Body.playing(Clip);
    }

    @Override
    public void setCustomAnimations(T Creature, long InstanceId, AnimationState<T> Animated) {
        float Partial = Animated.getPartialTick();
        GeoBone Root = getAnimationProcessor().getBone(RootBone);
        if (Root != null) {
            Root.setHidden(!readyToDraw(Creature, InstanceId));
        }
        GeoBone Look = getAnimationProcessor().getBone(LookBone);
        if (Look != null) {
            place(Look, Creature.lookPitch(Partial), Creature.lookYaw(Partial), 0.0f);
        }
        if (Creature instanceof Scp610HiveLord Lord) {
            aim(Lord, Partial);
        }
    }

    private void aim(Scp610HiveLord Lord, float Partial) {
        GeoBone Eye = getAnimationProcessor().getBone(EyeBone);
        float WantYaw = 0.0f;
        float WantPitch = 0.0f;
        Entity Seen = Lord.watched();
        if (Seen != null && Lord.deathTime <= 0) {
            Vec3 From = Lord.getPosition(Partial).add(0.0, Lord.getBbHeight() * 0.5, 0.0);
            Vec3 To = Seen.getEyePosition(Partial);
            Vec3 Gap = To.subtract(From);
            float Facing = Mth.rotLerp(Partial, Lord.yBodyRotO, Lord.yBodyRot);
            float Heading = (float) (Mth.atan2(Gap.z, Gap.x) * Mth.RAD_TO_DEG) - 90.0f;
            WantYaw = Mth.clamp(Mth.wrapDegrees(Heading - Facing), -EyeLimit, EyeLimit);
            WantPitch = Mth.clamp((float) -(Mth.atan2(Gap.y, Math.sqrt(Gap.x * Gap.x + Gap.z * Gap.z)) * Mth.RAD_TO_DEG), -EyeLimit, EyeLimit);
        }
        EyeYaw += (WantYaw - EyeYaw) * EyeFollow;
        EyePitch += (WantPitch - EyePitch) * EyeFollow;
        if (Eye != null) {
            place(Eye, EyePitch, EyeYaw, 0.0f);
        }
        Dilation += (Lord.dilation(Partial) - Dilation) * 0.08f;
        GeoBone Pupil = getAnimationProcessor().getBone(PupilBone);
        if (Pupil != null) {
            float Scale = Mth.clamp(Dilation, 0.6f, 1.4f);
            Pupil.setScaleX(Scale);
            Pupil.setScaleY(Scale);
        }
    }

    public static void place(GeoBone Bone, float Pitch, float Yaw, float Roll) {
        BoneSnapshot Rest = Bone.getInitialSnapshot();
        Bone.setRotX(Rest.getRotX() - Pitch * Mth.DEG_TO_RAD);
        Bone.setRotY(Rest.getRotY() - Yaw * Mth.DEG_TO_RAD);
        Bone.setRotZ(Rest.getRotZ() + Roll * Mth.DEG_TO_RAD);
    }
}
