package net.rai.scp_unleashed.scp469.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.client.geo.SharedGeoModel;
import net.rai.scp_unleashed.scp469.Scp469Angel;
import net.rai.scp_unleashed.scp469.Scp469Registry;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.List;

public class Scp469GeoModel extends SharedGeoModel<Scp469Angel> {
    public static final ResourceLocation ModelPath = Scp469Registry.location("geo/entity/scp469/many_winged_angel.geo.json");
    public static final ResourceLocation TexturePath = Scp469Registry.location("textures/entity/scp469/many_winged_angel.png");
    public static final ResourceLocation AnimationPath = Scp469Registry.location("animations/entity/scp469/many_winged_angel.animation.json");
    public static final List<String> Wings = List.of("crown", "back", "high", "side", "low");
    public static final float CalmShudder = 1.6f;
    public static final float HuntShudder = 3.2f;
    public static final float HeldShudder = 4.5f;

    @Override
    public ResourceLocation getModelResource(Scp469Angel Creature) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp469Angel Creature) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp469Angel Creature) {
        return AnimationPath;
    }

    @Override
    public RenderType getRenderType(Scp469Angel Creature, ResourceLocation Texture) {
        return RenderType.entityCutoutNoCull(Texture);
    }

    public static float shudder(float Seconds, int Index) {
        float Fast = Mth.sin(Seconds * 23.0f + Index * 1.7f) * 0.55f;
        float Mid = Mth.sin(Seconds * 13.0f + Index * 2.9f) * 0.3f;
        float Slow = Mth.sin(Seconds * 5.0f + Index * 0.8f) * 0.15f;
        return Fast + Mid + Slow;
    }

    @Override
    public void setCustomAnimations(Scp469Angel Creature, long InstanceId, AnimationState<Scp469Angel> Animated) {
        if (Creature.isDeadOrDying() || Creature.isReforming()) {
            return;
        }
        float Seconds = (Creature.tickCount + Animated.getPartialTick()) / 20.0f + Math.floorMod(Creature.getId(), 97) * 0.37f;
        float Amount = Creature.isEmbracing() ? HeldShudder : Creature.isHunting() ? HuntShudder : CalmShudder;
        if (Creature.isFolded()) {
            Amount *= 0.5f;
        }
        int Index = 0;
        for (String Side : new String[]{"l", "r"}) {
            for (String Wing : Wings) {
                GeoBone Hand = getAnimationProcessor().getBone("wing_" + Wing + "_" + Side + "_hand");
                GeoBone Arm = getAnimationProcessor().getBone("wing_" + Wing + "_" + Side + "_arm");
                float Wave = shudder(Seconds, Index) * Amount * Mth.DEG_TO_RAD;
                if (Hand != null) {
                    Hand.setRotZ(Hand.getRotZ() + Wave);
                    Hand.setRotX(Hand.getRotX() + shudder(Seconds + 0.31f, Index + 5) * Amount * 0.5f * Mth.DEG_TO_RAD);
                }
                if (Arm != null) {
                    Arm.setRotZ(Arm.getRotZ() + Wave * 0.4f);
                }
                Index++;
            }
        }
    }
}
