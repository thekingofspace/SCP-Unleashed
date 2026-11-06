package net.rai.scp_unleashed.scp346.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp346.Scp346Pterosaur;
import net.rai.scp_unleashed.scp346.Scp346Variant;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

import java.util.EnumMap;
import java.util.Map;

public class Scp346GeoModel extends GeoModel<Scp346Pterosaur> {
    public static final String Name = "scp346";
    public static final String LookBone = "look";
    public static final float PitchLimit = 30.0f;
    public static final float YawLimit = 50.0f;
    public static final float FlightPitchLimit = 12.0f;
    public static final ResourceLocation ModelPath = location("geo/entity/scp346/" + Name + ".geo.json");
    public static final ResourceLocation AnimationPath = location("animations/entity/scp346/" + Name + ".animation.json");
    private static final Map<Scp346Variant, ResourceLocation> Textures = new EnumMap<>(Scp346Variant.class);

    static {
        for (Scp346Variant Variant : Scp346Variant.values()) {
            Textures.put(Variant, location("textures/entity/scp346/" + Name + "_" + Variant.getSerializedName() + ".png"));
        }
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    public static ResourceLocation texture(Scp346Variant Variant) {
        return Textures.get(Variant);
    }

    @Override
    public ResourceLocation getModelResource(Scp346Pterosaur Bird) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp346Pterosaur Bird) {
        return texture(Bird.getVariant());
    }

    @Override
    public ResourceLocation getAnimationResource(Scp346Pterosaur Bird) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(Scp346Pterosaur Bird, long InstanceId, AnimationState<Scp346Pterosaur> Animated) {
        GeoBone Look = getAnimationProcessor().getBone(LookBone);
        if (Look == null) {
            return;
        }
        float Flinch = Bird.flinchAmount(Animated.getPartialTick());
        float Pitch = 0.0f;
        float Yaw = 0.0f;
        EntityModelData Data = Animated.getData(DataTickets.ENTITY_MODEL_DATA);
        if (Data != null && !Bird.isRoosting() && !Bird.isActing()) {
            float Limit = Bird.isFlying() ? FlightPitchLimit : PitchLimit;
            Pitch = Mth.clamp(Data.headPitch(), -Limit, Limit);
            Yaw = Mth.clamp(Data.netHeadYaw(), -YawLimit, YawLimit);
        }
        Look.setRotX((-Pitch - Scp346Pterosaur.FlinchPitch * Flinch) * Mth.DEG_TO_RAD);
        Look.setRotY(-Yaw * Mth.DEG_TO_RAD);
        Look.setRotZ(Scp346Pterosaur.FlinchRoll * Flinch * Mth.DEG_TO_RAD);
    }
}
