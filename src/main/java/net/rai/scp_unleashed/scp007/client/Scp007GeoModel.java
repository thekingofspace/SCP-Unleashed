package net.rai.scp_unleashed.scp007.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.client.DClassGeoModel;
import net.rai.scp_unleashed.scp007.Scp007Entity;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.data.EntityModelData;

public class Scp007GeoModel extends DClassGeoModel<Scp007Entity> {
    public static final ResourceLocation SubjectModel = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "geo/entity/scp007/scp007.geo.json");
    public static final ResourceLocation SubjectTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/entity/scp007/scp007.png");
    public static final ResourceLocation SubjectAnimation = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "animations/entity/scp007/scp007.animation.json");
    public static final String PlanetBone = "planet";
    public static final String CloudBone = "clouds";
    public static final String StormBone = "storm";
    public static final float Tilt = 10.0f * Mth.DEG_TO_RAD;
    public static final float PlanetSpin = 0.6f;
    public static final float CloudSpin = 1.0f;
    public static final float CloudPhase = 7.0f;
    public static final float RainShown = 0.2f;

    @Override
    public ResourceLocation getModelResource(Scp007Entity Owner) {
        return SubjectModel;
    }

    @Override
    public ResourceLocation getTextureResource(Scp007Entity Owner) {
        return SubjectTexture;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp007Entity Owner) {
        return SubjectAnimation;
    }

    @Nullable
    private GeoBone part(String Name) {
        return getAnimationProcessor().getBone(Name);
    }

    @Override
    public void setCustomAnimations(Scp007Entity Owner, long InstanceId, AnimationState<Scp007Entity> Animated) {
        super.setCustomAnimations(Owner, InstanceId, Animated);
        float Partial = Animated.getPartialTick();
        float Time = Owner.tickCount + Partial;
        float Phase = (Owner.getId() * 47) % 360;
        spin(PlanetBone, Time * PlanetSpin + Phase);
        spin(CloudBone, Time * CloudSpin + Phase + CloudPhase);
        GeoBone Storm = part(StormBone);
        if (Storm != null) {
            Storm.setHidden(Owner.level().getRainLevel(Partial) < RainShown);
        }
        if (Owner.isSettled() && !Owner.isPuppet()) {
            steady(Owner, Animated);
        }
    }

    private void spin(String Name, float Degrees) {
        GeoBone Found = part(Name);
        if (Found != null) {
            Found.setRotZ(Tilt);
            Found.setRotY(-Mth.wrapDegrees(Degrees) * Mth.DEG_TO_RAD);
        }
    }

    private void steady(Scp007Entity Owner, AnimationState<Scp007Entity> Animated) {
        GeoBone Head = part("head");
        EntityModelData Data = Animated.getData(DataTickets.ENTITY_MODEL_DATA);
        if (Head == null || Data == null || Owner.isSwimmingPose()) {
            return;
        }
        float HeadX = Mth.clamp(Data.headPitch(), -HeadPitchLimit, HeadPitchLimit) * Mth.DEG_TO_RAD;
        float HeadY = Mth.clamp(Mth.wrapDegrees(Data.netHeadYaw()), -HeadYawLimit, HeadYawLimit) * Mth.DEG_TO_RAD;
        Head.setRotX(Head.getRotX() - HeadX);
        Head.setRotY(Head.getRotY() - HeadY);
    }
}
