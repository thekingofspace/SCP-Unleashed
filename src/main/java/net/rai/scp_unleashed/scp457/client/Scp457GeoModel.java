package net.rai.scp_unleashed.scp457.client;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.dclass.client.DClassGeoModel;
import net.rai.scp_unleashed.scp457.Scp457;
import net.rai.scp_unleashed.scp457.Scp457Effects;
import net.rai.scp_unleashed.scp457.Scp457Registry;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

public class Scp457GeoModel extends DClassGeoModel<Scp457> {
    public static final ResourceLocation GeoPath = Scp457Registry.location("geo/entity/scp457/scp457.geo.json");
    public static final ResourceLocation ClipPath = Scp457Registry.location("animations/entity/scp457/scp457.animation.json");
    public static final ResourceLocation SkinPath = Scp457Registry.location("textures/entity/scp457/scp457.png");
    public static final ResourceLocation AshPath = Scp457Registry.location("textures/entity/scp457/scp457_ash.png");
    public static final int AshAfter = Scp457.CollapseTicks / 2;

    @Override
    public ResourceLocation getModelResource(Scp457 Owner) {
        return GeoPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp457 Owner) {
        return Owner.deathTime > AshAfter ? AshPath : SkinPath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp457 Owner) {
        return ClipPath;
    }

    @Override
    public void setCustomAnimations(Scp457 Owner, long InstanceId, AnimationState<Scp457> Animated) {
        super.setCustomAnimations(Owner, InstanceId, Animated);
        for (String Name : Scp457Effects.TrackedBones) {
            GeoBone Found = getAnimationProcessor().getBone(Name);
            if (Found != null) {
                Found.setTrackingMatrices(true);
            }
        }
    }
}
