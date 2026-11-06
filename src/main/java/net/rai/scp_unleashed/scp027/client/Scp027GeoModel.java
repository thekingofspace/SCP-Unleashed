package net.rai.scp_unleashed.scp027.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.client.DClassGeoModel;
import net.rai.scp_unleashed.scp027.Scp027Entity;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.data.EntityModelData;

public class Scp027GeoModel extends DClassGeoModel<Scp027Entity> {
    public static final ResourceLocation SubjectModel = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "geo/entity/scp027/scp027.geo.json");
    public static final ResourceLocation SubjectTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/entity/scp027/scp027.png");
    public static final ResourceLocation SubjectAnimation = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "animations/entity/scp027/scp027.animation.json");
    public static final int FlyCount = 12;
    public static final float FlyPivot = 28.0f;
    public static final float HeavyLids = 0.5f;
    public static final float DisperseTicks = 18.0f;
    public static final float TiredYaw = 25.0f;

    @Override
    public ResourceLocation getModelResource(Scp027Entity Owner) {
        return SubjectModel;
    }

    @Override
    public ResourceLocation getTextureResource(Scp027Entity Owner) {
        return SubjectTexture;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp027Entity Owner) {
        return SubjectAnimation;
    }

    @Nullable
    private GeoBone part(String Name) {
        return getAnimationProcessor().getBone(Name);
    }

    @Override
    public void setCustomAnimations(Scp027Entity Owner, long InstanceId, AnimationState<Scp027Entity> Animated) {
        super.setCustomAnimations(Owner, InstanceId, Animated);
        float Time = (Owner.tickCount + Animated.getPartialTick()) / 20.0f;
        stiffen(Owner, Animated);
        if (!Owner.eyesShut()) {
            heavy("right_lid");
            heavy("left_lid");
        }
        float Dying = Owner.isDeadOrDying() ? (Owner.deathTime + Animated.getPartialTick()) / DisperseTicks : 0.0f;
        for (int Index = 0; Index < FlyCount; Index++) {
            fly(Index, Time + (Owner.getId() % 97) * 0.37f, Dying);
        }
        rat(Time);
    }

    private void stiffen(Scp027Entity Owner, AnimationState<Scp027Entity> Animated) {
        GeoBone Head = part("head");
        EntityModelData Data = Animated.getData(DataTickets.ENTITY_MODEL_DATA);
        if (Head == null || Data == null || Owner.isPuppet() || Owner.isSpecimen() || Owner.isRestingPose()) {
            return;
        }
        float Applied = Mth.clamp(Mth.wrapDegrees(Data.netHeadYaw()), -HeadYawLimit, HeadYawLimit);
        float Wanted = Mth.clamp(Applied, -TiredYaw, TiredYaw);
        Head.setRotY(Head.getRotY() - (Applied - Wanted) * Mth.DEG_TO_RAD);
    }

    private void heavy(String Name) {
        GeoBone Found = part(Name);
        if (Found != null && (Found.isHidden() || Found.getScaleY() < HeavyLids)) {
            Found.setHidden(false);
            Found.setScaleY(HeavyLids);
        }
    }

    public static float[] flyOffset(int Index, float Time, float Dying) {
        float Sign = Index % 2 == 0 ? 1.0f : -1.0f;
        float Speed = 1.7f + (Index % 3) * 0.55f;
        float Radius = 6.4f + (Index * 7 % 5) * 0.65f;
        float Height = 25.5f + (Index * 13 % 12) * 0.8f;
        float Phase = Index * 2.39996f;
        float Angle = Phase + Sign * Speed * Time + 0.5f * Mth.sin(Time * 3.1f + Index * 1.7f);
        float Wobble = 1.0f + 0.18f * Mth.sin(Time * 1.9f + Index);
        float Spread = 1.0f + Dying * 3.5f;
        float X = Mth.cos(Angle) * Radius * Wobble * Spread + Mth.sin(Time * 17.0f + Index * 5.0f) * 0.35f;
        float Z = Mth.sin(Angle) * Radius * Spread + Mth.cos(Time * 15.0f + Index * 3.0f) * 0.35f;
        float Y = Height + Mth.sin(Time * 2.3f + Index * 0.9f) * 1.0f + Dying * 10.0f;
        return new float[]{X, Y, Z};
    }

    private void fly(int Index, float Time, float Dying) {
        GeoBone Found = part("fly_" + Index);
        if (Found == null) {
            return;
        }
        if (Dying >= 1.0f) {
            Found.setHidden(true);
            return;
        }
        Found.setHidden(false);
        float[] Offset = flyOffset(Index, Time, Dying);
        Found.setPosX(Offset[0]);
        Found.setPosY(Offset[1] - FlyPivot);
        Found.setPosZ(Offset[2]);
    }

    private void rat(float Time) {
        GeoBone Head = part("shoulder_rat_head");
        if (Head != null) {
            float Sniff = Mth.sin(Time * 9.0f) * (Mth.sin(Time * 0.7f) > 0.3f ? 0.07f : 0.0f);
            Head.setRotY(Mth.sin(Time * 1.3f) * 0.35f);
            Head.setRotX(Sniff + Mth.sin(Time * 0.9f) * 0.08f);
        }
        GeoBone Tail = part("shoulder_rat_tail");
        if (Tail != null) {
            Tail.setRotZ(Mth.sin(Time * 1.6f) * 0.18f);
        }
    }
}
