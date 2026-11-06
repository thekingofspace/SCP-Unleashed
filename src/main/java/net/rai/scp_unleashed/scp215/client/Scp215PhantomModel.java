package net.rai.scp_unleashed.scp215.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp215.Scp215Phantom;
import net.rai.scp_unleashed.scp215.Scp215Variant;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

public class Scp215PhantomModel extends GeoModel<Scp215Phantom> {
    public static final String HeadBone = "head";
    public static final float PitchShare = 0.8f;
    private static final ResourceLocation[] Models = new ResourceLocation[Scp215Variant.values().length];
    private static final ResourceLocation[] Textures = new ResourceLocation[Scp215Variant.values().length];
    private static final ResourceLocation[] Animations = new ResourceLocation[Scp215Variant.values().length];

    static {
        for (Scp215Variant Kind : Scp215Variant.values()) {
            Models[Kind.ordinal()] = location("geo/entity/scp215/" + Kind.Name + ".geo.json");
            Textures[Kind.ordinal()] = location("textures/entity/scp215/" + Kind.Name + ".png");
            Animations[Kind.ordinal()] = location("animations/entity/scp215/" + Kind.Name + ".animation.json");
        }
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    @Override
    public ResourceLocation getModelResource(Scp215Phantom Phantom) {
        return Models[Phantom.variant().ordinal()];
    }

    @Override
    public ResourceLocation getTextureResource(Scp215Phantom Phantom) {
        return Textures[Phantom.variant().ordinal()];
    }

    @Override
    public ResourceLocation getAnimationResource(Scp215Phantom Phantom) {
        return Animations[Phantom.variant().ordinal()];
    }

    @Override
    public void setCustomAnimations(Scp215Phantom Phantom, long InstanceId, AnimationState<Scp215Phantom> Animated) {
        GeoBone Head = getAnimationProcessor().getBone(HeadBone);
        if (Head == null) {
            return;
        }
        float Partial = Animated.getPartialTick();
        Head.setRotY(Head.getRotY() - Phantom.netHeadYaw(Partial) * Mth.DEG_TO_RAD);
        Head.setRotX(Head.getRotX() - Phantom.headPitch(Partial) * PitchShare * Mth.DEG_TO_RAD);
    }
}
