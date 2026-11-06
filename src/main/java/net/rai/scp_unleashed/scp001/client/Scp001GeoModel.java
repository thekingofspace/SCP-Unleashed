package net.rai.scp_unleashed.scp001.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp001.Scp001Jitter;
import net.rai.scp_unleashed.scp001.Scp001Prototype;
import net.rai.scp_unleashed.scp001.Scp001Registry;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

import java.util.ArrayList;
import java.util.List;

public class Scp001GeoModel extends GeoModel<Scp001Prototype> {
    public static final ResourceLocation ModelPath = Scp001Registry.location("geo/entity/scp001/prototype.geo.json");
    public static final ResourceLocation TexturePath = Scp001Registry.location("textures/entity/scp001/prototype.png");
    public static final ResourceLocation GlowPath = Scp001Registry.location("textures/entity/scp001/prototype_glow.png");
    public static final ResourceLocation AnimationPath = Scp001Registry.location("animations/entity/scp001/prototype.animation.json");
    public static final String LookBone = "look";
    public static final String[] CordBones = {"cord_1", "cord_2", "cord_3", "cord_4"};
    public static final float PitchLimit = 30.0f;
    public static final float YawLimit = 50.0f;
    public static final float Trail = 0.22f;

    @Override
    public ResourceLocation getModelResource(Scp001Prototype Creature) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp001Prototype Creature) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp001Prototype Creature) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(Scp001Prototype Creature, long InstanceId, AnimationState<Scp001Prototype> Animated) {
        EntityModelData Data = Animated.getData(DataTickets.ENTITY_MODEL_DATA);
        GeoBone Look = getAnimationProcessor().getBone(LookBone);
        if (Look != null && Data != null && !Creature.isLocked() && !Creature.busy()) {
            Look.setRotX(-Mth.clamp(Data.headPitch(), -PitchLimit, PitchLimit) * Mth.DEG_TO_RAD);
            Look.setRotY(-Mth.clamp(Data.netHeadYaw(), -YawLimit, YawLimit) * Mth.DEG_TO_RAD);
        }
        if (!Creature.isDeadOrDying()) {
            shiver(Creature.jitter());
        }
        if (Creature.hangsCord()) {
            hang(Creature, Animated.getPartialTick());
        }
    }

    private void shiver(Scp001Jitter Jitter) {
        float[][] Offsets = Jitter.offsets();
        for (int Index = 0; Index < Scp001Jitter.Bones.length; Index++) {
            nudge(Scp001Jitter.Bones[Index], Offsets[Index]);
        }
        nudge("head", Jitter.snap());
        float[][] Arms = Jitter.armOffsets();
        for (int Index = 0; Index < Scp001Jitter.Arms.length; Index++) {
            nudge(Scp001Jitter.Arms[Index], Arms[Index]);
        }
        float[] Blades = Jitter.bladeOffsets();
        String[] Names = Jitter.blades();
        for (int Index = 0; Index < Names.length; Index++) {
            GeoBone Blade = getAnimationProcessor().getBone(Names[Index]);
            if (Blade != null && Blades[Index] != 0.0f) {
                Blade.setRotX(Blade.getRotX() + Blades[Index] * Mth.DEG_TO_RAD);
            }
        }
    }

    private void nudge(String Name, float[] Degrees) {
        GeoBone Bone = getAnimationProcessor().getBone(Name);
        if (Bone == null || Degrees[0] == 0.0f && Degrees[1] == 0.0f && Degrees[2] == 0.0f) {
            return;
        }
        Bone.setRotX(Bone.getRotX() + Degrees[0] * Mth.DEG_TO_RAD);
        Bone.setRotY(Bone.getRotY() + Degrees[1] * Mth.DEG_TO_RAD);
        Bone.setRotZ(Bone.getRotZ() + Degrees[2] * Mth.DEG_TO_RAD);
    }

    private void hang(Scp001Prototype Creature, float Partial) {
        GeoBone First = getAnimationProcessor().getBone(CordBones[0]);
        if (First == null || First.getParent() == null) {
            return;
        }
        float SwingX = Creature.swingX(Partial);
        float SwingZ = Creature.swingZ(Partial);
        Matrix3f Parent = world(First.getParent());
        Vector3f Down = new Matrix3f().rotationZYX(SwingZ, 0.0f, SwingX).transform(new Vector3f(0.0f, -1.0f, 0.0f));
        Vector3f Local = Parent.transpose(new Matrix3f()).transform(Down);
        First.setRotX((float) Math.asin(Mth.clamp(-Local.z, -1.0f, 1.0f)));
        First.setRotY(0.0f);
        First.setRotZ((float) Mth.atan2(Local.x, -Local.y));
        for (int Index = 1; Index < CordBones.length; Index++) {
            GeoBone Next = getAnimationProcessor().getBone(CordBones[Index]);
            if (Next != null) {
                Next.setRotX(-SwingX * Trail);
                Next.setRotY(0.0f);
                Next.setRotZ(-SwingZ * Trail);
            }
        }
    }

    private static Matrix3f world(GeoBone Bone) {
        List<GeoBone> Chain = new ArrayList<>();
        GeoBone Current = Bone;
        while (Current != null) {
            Chain.add(Current);
            Current = Current.getParent();
        }
        Matrix3f Result = new Matrix3f();
        for (int Index = Chain.size() - 1; Index >= 0; Index--) {
            GeoBone Item = Chain.get(Index);
            Result.mul(new Matrix3f().rotationZYX(Item.getRotZ(), Item.getRotY(), Item.getRotX()));
        }
        return Result;
    }
}
