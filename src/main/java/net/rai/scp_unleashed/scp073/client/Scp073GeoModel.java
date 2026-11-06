package net.rai.scp_unleashed.scp073.client;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.client.DClassGeoModel;
import net.rai.scp_unleashed.scp073.Scp073Cain;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

public class Scp073GeoModel extends DClassGeoModel<Scp073Cain> {
    public static final ResourceLocation CainModel = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "geo/entity/scp073/scp073_cain.geo.json");
    public static final ResourceLocation CainTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/entity/scp073/cain.png");

    @Override
    public ResourceLocation getModelResource(Scp073Cain Owner) {
        return CainModel;
    }

    @Override
    public ResourceLocation getTextureResource(Scp073Cain Owner) {
        return CainTexture;
    }

    @Nullable
    private GeoBone part(String Name) {
        return getAnimationProcessor().getBone(Name);
    }

    @Override
    public void setCustomAnimations(Scp073Cain Owner, long InstanceId, AnimationState<Scp073Cain> Animated) {
        super.setCustomAnimations(Owner, InstanceId, Animated);
        GeoBone Front = part("robe_front");
        GeoBone Back = part("robe_back");
        GeoBone Right = part("right_leg");
        GeoBone Left = part("left_leg");
        if (Front == null || Back == null || Right == null || Left == null) {
            return;
        }
        boolean Seated = Owner.isSittingPose();
        Front.setHidden(Seated);
        Back.setHidden(Seated);
        if (Seated) {
            return;
        }
        Front.setRotX(Math.max(Right.getRotX(), Left.getRotX()));
        Back.setRotX(Math.min(Right.getRotX(), Left.getRotX()));
        Front.setRotY(0.0f);
        Back.setRotY(0.0f);
        Front.setRotZ(0.0f);
        Back.setRotZ(0.0f);
    }
}
