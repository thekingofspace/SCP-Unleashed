package net.rai.scp_unleashed.scp001scarlet.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp001scarlet.Scp001ScarletRegistry;
import net.rai.scp_unleashed.scp001scarlet.cult.ScarletCultist;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class ScarletCultistGeoModel extends GeoModel<ScarletCultist> {
    public static final ResourceLocation ModelPath = Scp001ScarletRegistry.location("geo/entity/scp001_scarlet/scarlet_cultist.geo.json");
    public static final ResourceLocation AnimationPath = Scp001ScarletRegistry.location("animations/entity/scp001_scarlet/scarlet_cultist.animation.json");
    public static final ResourceLocation Texture = Scp001ScarletRegistry.location("textures/entity/scp001_scarlet/scarlet_cultist.png");
    public static final ResourceLocation LeaderTexture = Scp001ScarletRegistry.location("textures/entity/scp001_scarlet/scarlet_cultist_leader.png");
    public static final float PitchLimit = 25.0f;
    public static final float YawLimit = 45.0f;

    @Override
    public ResourceLocation getModelResource(ScarletCultist Owner) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(ScarletCultist Owner) {
        return Owner.leader() ? LeaderTexture : Texture;
    }

    @Override
    public ResourceLocation getAnimationResource(ScarletCultist Owner) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(ScarletCultist Owner, long InstanceId, AnimationState<ScarletCultist> Animated) {
        GeoBone Dagger = getAnimationProcessor().getBone("dagger");
        GeoBone Staff = getAnimationProcessor().getBone("staff");
        if (Dagger != null) {
            Dagger.setHidden(Owner.leader());
        }
        if (Staff != null) {
            Staff.setHidden(!Owner.leader());
        }
        GeoBone Look = getAnimationProcessor().getBone("look");
        EntityModelData Data = Animated.getData(DataTickets.ENTITY_MODEL_DATA);
        if (Look == null || Data == null || Owner.kneeling()) {
            if (Look != null) {
                Look.setRotX(0.0f);
                Look.setRotY(0.0f);
            }
            return;
        }
        Look.setRotX(Mth.clamp(Data.headPitch(), -PitchLimit, PitchLimit) * Mth.DEG_TO_RAD);
        Look.setRotY(Mth.clamp(Mth.wrapDegrees(Data.netHeadYaw()), -YawLimit, YawLimit) * Mth.DEG_TO_RAD);
    }
}
