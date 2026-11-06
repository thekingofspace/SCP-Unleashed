package net.rai.scp_unleashed.scp105.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.client.DClassGeoModel;
import net.rai.scp_unleashed.scp105.Scp105Iris;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.data.EntityModelData;

public class Scp105GeoModel extends DClassGeoModel<Scp105Iris> {
    public static final ResourceLocation IrisModel = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "geo/entity/scp105/iris.geo.json");
    public static final ResourceLocation IrisTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/entity/scp105/iris.png");
    public static final ResourceLocation IrisAnimation = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "animations/entity/scp105/iris.animation.json");
    public static final int PhotoShown = 4;
    public static final int PhotoHidden = 13;
    public static final int MouthRate = 3;

    @Override
    public ResourceLocation getModelResource(Scp105Iris Owner) {
        return IrisModel;
    }

    @Override
    public ResourceLocation getTextureResource(Scp105Iris Owner) {
        return IrisTexture;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp105Iris Owner) {
        return IrisAnimation;
    }

    @Nullable
    private GeoBone part(String Name) {
        return getAnimationProcessor().getBone(Name);
    }

    @Override
    public void setCustomAnimations(Scp105Iris Owner, long InstanceId, AnimationState<Scp105Iris> Animated) {
        super.setCustomAnimations(Owner, InstanceId, Animated);
        int Age = Owner.actAge();
        GeoBone Photo = part("photo");
        if (Photo != null) {
            Photo.setHidden(!(Owner.isGiving() && Age >= PhotoShown && Age < PhotoHidden));
        }
        GeoBone Mouth = part("mouth");
        if (Mouth != null && Owner.isAlive() && (Owner.isTalking() || Owner.isGiving())) {
            Mouth.setHidden((Owner.tickCount / MouthRate) % 2 == 0);
        }
        if (Owner.isSnapping()) {
            steady(Owner, Animated);
        }
    }

    private void steady(Scp105Iris Owner, AnimationState<Scp105Iris> Animated) {
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
