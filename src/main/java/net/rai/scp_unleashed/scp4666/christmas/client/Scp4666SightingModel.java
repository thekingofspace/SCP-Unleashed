package net.rai.scp_unleashed.scp4666.christmas.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp4666.christmas.Scp4666Sighting;
import net.rai.scp_unleashed.scp4666.client.Scp4666GeoModel;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

public class Scp4666SightingModel extends GeoModel<Scp4666Sighting> {
    public static final String[] HiddenBones = {"sack", "sack_body", "sack_bottom"};

    @Override
    public ResourceLocation getModelResource(Scp4666Sighting Figure) {
        return Scp4666GeoModel.ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp4666Sighting Figure) {
        return Scp4666GeoModel.TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp4666Sighting Figure) {
        return Scp4666GeoModel.AnimationPath;
    }

    @Override
    public RenderType getRenderType(Scp4666Sighting Figure, ResourceLocation Texture) {
        return RenderType.entityCutoutNoCull(Texture);
    }

    @Override
    public void setCustomAnimations(Scp4666Sighting Figure, long InstanceId, AnimationState<Scp4666Sighting> Animated) {
        for (String Name : HiddenBones) {
            GeoBone Found = getAnimationProcessor().getBone(Name);
            if (Found != null) {
                Found.setHidden(true);
            }
        }
    }
}
