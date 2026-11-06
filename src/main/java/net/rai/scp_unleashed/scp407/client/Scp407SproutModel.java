package net.rai.scp_unleashed.scp407.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp407.Scp407Registry;
import net.rai.scp_unleashed.scp407.Scp407Sprout;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;

public class Scp407SproutModel extends GeoModel<Scp407Sprout> {
    public static final String HeadBone = "head";
    public static final float YawLimit = 50.0f;
    public static final float PitchLimit = 30.0f;
    public static final float LookEasePerTick = 0.2f;
    public static final float LookCatchUpTicks = 5.0f;
    private static final ResourceLocation ModelPath = Scp407Registry.location("geo/entity/scp407/scp407_sprout.geo.json");
    private static final ResourceLocation TexturePath = Scp407Registry.location("textures/entity/scp407/scp407_sprout.png");
    private static final ResourceLocation AnimationPath = Scp407Registry.location("animations/entity/scp407/scp407_sprout.animation.json");

    private final Map<Scp407Sprout, float[]> Looks = new WeakHashMap<>();
    private final Map<GeoBone, float[]> Nudged = new IdentityHashMap<>();

    @Override
    public ResourceLocation getModelResource(Scp407Sprout Creature) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp407Sprout Creature) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp407Sprout Creature) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(Scp407Sprout Creature, long InstanceId, AnimationState<Scp407Sprout> Animated) {
        GeoBone Head = getAnimationProcessor().getBone(HeadBone);
        EntityModelData Data = Animated.getData(DataTickets.ENTITY_MODEL_DATA);
        if (Head == null || Data == null) {
            return;
        }
        float Weight = lookWeight(Creature, Animated.getPartialTick());
        nudge(Head, Mth.clamp(Data.headPitch(), -PitchLimit, PitchLimit) * Weight, Mth.clamp(Data.netHeadYaw(), -YawLimit, YawLimit) * Weight);
    }

    private float lookWeight(Scp407Sprout Creature, float Partial) {
        float Now = Creature.tickCount + Partial;
        float Target = Creature.isPerforming() ? 0.0f : 1.0f;
        float[] Look = Looks.computeIfAbsent(Creature, Key -> new float[]{Target, Now});
        float Step = Mth.clamp(Now - Look[1], 0.0f, LookCatchUpTicks) * LookEasePerTick;
        Look[0] = Look[0] < Target ? Math.min(Target, Look[0] + Step) : Math.max(Target, Look[0] - Step);
        Look[1] = Now;
        return Look[0] * Look[0] * (3.0f - 2.0f * Look[0]);
    }

    private void nudge(GeoBone Bone, float Pitch, float Yaw) {
        float BaseX = Bone.getRotX();
        float BaseY = Bone.getRotY();
        float[] Last = Nudged.get(Bone);
        if (Last != null && BaseX == Last[0] && BaseY == Last[1]) {
            BaseX -= Last[2];
            BaseY -= Last[3];
        }
        float AddX = Pitch * Mth.DEG_TO_RAD;
        float AddY = Yaw * Mth.DEG_TO_RAD;
        Bone.setRotX(BaseX + AddX);
        Bone.setRotY(BaseY + AddY);
        Nudged.put(Bone, new float[]{Bone.getRotX(), Bone.getRotY(), AddX, AddY});
    }
}
