package net.rai.scp_unleashed.scp031.client;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp031.Scp031Polyp;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

public class Scp031PolypModel extends GeoModel<Scp031Polyp> {
    public static final ResourceLocation ModelPath = Scp031FleshModel.location("geo/entity/scp031/polyp.geo.json");
    public static final ResourceLocation TexturePath = Scp031FleshModel.location("textures/entity/scp031/polyp.png");
    public static final ResourceLocation AnimationPath = Scp031FleshModel.location("animations/entity/scp031/polyp.animation.json");
    public static final String StagePrefix = "stage_";

    @Override
    public ResourceLocation getModelResource(Scp031Polyp Polyp) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp031Polyp Polyp) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp031Polyp Polyp) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(Scp031Polyp Polyp, long InstanceId, AnimationState<Scp031Polyp> Animated) {
        int Stage = Polyp.stage();
        for (int Index = 0; Index < Scp031Polyp.Stages; Index++) {
            GeoBone Bone = getAnimationProcessor().getBone(StagePrefix + Index);
            if (Bone != null) {
                Bone.setHidden(Index != Stage);
            }
        }
    }
}
