package net.rai.scp_unleashed.z0004.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.client.geo.SharedGeoModel;
import net.rai.scp_unleashed.z0004.Z0004Pirate;
import net.rai.scp_unleashed.z0004.Z0004Registry;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.data.EntityModelData;

public class Z0004PirateGeoModel extends SharedGeoModel<Z0004Pirate> {
    public static final ResourceLocation[] Models = new ResourceLocation[Z0004Pirate.Variants];
    public static final ResourceLocation[] Textures = new ResourceLocation[Z0004Pirate.Variants];
    public static final ResourceLocation CaptainModel = Z0004Registry.location("geo/entity/z0004/z0004_captain.geo.json");
    public static final ResourceLocation CaptainTexture = Z0004Registry.location("textures/entity/z0004/z0004_captain.png");
    public static final ResourceLocation CaptainAnimation = Z0004Registry.location("animations/entity/z0004/z0004_captain.animation.json");
    public static final ResourceLocation[] Animations = new ResourceLocation[Z0004Pirate.Variants];
    public static final float YawLimit = 50.0f;
    public static final float PitchLimit = 30.0f;

    static {
        for (int Index = 0; Index < Z0004Pirate.Variants; Index++) {
            String Name = "z0004_crew_" + Z0004Pirate.VariantNames[Index];
            Models[Index] = Z0004Registry.location("geo/entity/z0004/" + Name + ".geo.json");
            Textures[Index] = Z0004Registry.location("textures/entity/z0004/" + Name + ".png");
            Animations[Index] = Z0004Registry.location("animations/entity/z0004/" + Name + ".animation.json");
        }
    }

    @Override
    public ResourceLocation getModelResource(Z0004Pirate Owner) {
        return Owner.captain() ? CaptainModel : Models[Owner.variant()];
    }

    @Override
    public ResourceLocation getTextureResource(Z0004Pirate Owner) {
        return Owner.captain() ? CaptainTexture : Textures[Owner.variant()];
    }

    @Override
    public ResourceLocation getAnimationResource(Z0004Pirate Owner) {
        return Owner.captain() ? CaptainAnimation : Animations[Owner.variant()];
    }

    @Override
    public void setCustomAnimations(Z0004Pirate Owner, long InstanceId, AnimationState<Z0004Pirate> Animated) {
        super.setCustomAnimations(Owner, InstanceId, Animated);
        for (String Name : Z0004DeckGeoModel.Props) {
            GeoBone Prop = getAnimationProcessor().getBone(Name);
            if (Prop != null) {
                Prop.setHidden(true);
            }
        }
        GeoBone Look = getAnimationProcessor().getBone("look");
        if (Look == null) {
            return;
        }
        EntityModelData Data = Animated.getData(DataTickets.ENTITY_MODEL_DATA);
        float Yaw = 0.0f;
        float Pitch = 0.0f;
        if (Data != null && !Owner.swimShown()) {
            Yaw = Mth.clamp(Data.netHeadYaw(), -YawLimit, YawLimit);
            Pitch = Mth.clamp(Data.headPitch(), -PitchLimit, PitchLimit);
        }
        Look.setRotY(Look.getInitialSnapshot().getRotY() + Yaw * Mth.DEG_TO_RAD);
        Look.setRotX(Look.getInitialSnapshot().getRotX() - Pitch * Mth.DEG_TO_RAD);
    }
}
