package net.rai.scp_unleashed.z0004.client;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.client.geo.SharedGeoModel;
import net.rai.scp_unleashed.z0004.Z0004DeckSlot;
import net.rai.scp_unleashed.z0004.Z0004Registry;
import net.rai.scp_unleashed.z0004.Z0004Ship;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

public class Z0004ShipGeoModel extends SharedGeoModel<Z0004Ship> {
    public static final ResourceLocation ModelPath = Z0004Registry.location("geo/entity/z0004/z0004_ship.geo.json");
    public static final ResourceLocation TexturePath = Z0004Registry.location("textures/entity/z0004/z0004_ship.png");
    public static final ResourceLocation AnimationPath = Z0004Registry.location("animations/entity/z0004/z0004_ship.animation.json");

    @Override
    public ResourceLocation getModelResource(Z0004Ship Owner) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Z0004Ship Owner) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Z0004Ship Owner) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(Z0004Ship Owner, long InstanceId, AnimationState<Z0004Ship> Animated) {
        super.setCustomAnimations(Owner, InstanceId, Animated);
        for (Z0004DeckSlot Slot : Z0004DeckSlot.All) {
            GeoBone Found = getAnimationProcessor().getBone(Slot.Bone());
            if (Found != null) {
                Found.setTrackingMatrices(true);
            }
        }
    }
}
