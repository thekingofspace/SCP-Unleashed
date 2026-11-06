package net.rai.scp_unleashed.scp076.client;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.client.DClassGeoModel;
import net.rai.scp_unleashed.scp076.Scp076Abel;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

public class Scp076AbelModel extends DClassGeoModel<Scp076Abel> {
    public static final ResourceLocation GeoPath = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "geo/entity/scp076/abel.geo.json");
    public static final ResourceLocation ClipPath = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "animations/entity/scp076/abel.animation.json");
    public static final ResourceLocation SkinPath = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/entity/scp076/abel.png");

    @Override
    public ResourceLocation getModelResource(Scp076Abel Owner) {
        return GeoPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp076Abel Owner) {
        return SkinPath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp076Abel Owner) {
        return ClipPath;
    }

    @Nullable
    private GeoBone part(String Name) {
        return getAnimationProcessor().getBone(Name);
    }

    @Override
    public void setCustomAnimations(Scp076Abel Owner, long InstanceId, AnimationState<Scp076Abel> Animated) {
        super.setCustomAnimations(Owner, InstanceId, Animated);
        GeoBone Knife = part("left_knife");
        if (Knife != null) {
            Knife.setHidden(!Owner.knifeInHand());
        }
        GeoBone Back = part("skirt_back");
        GeoBone Front = part("skirt_front");
        GeoBone Right = part("right_leg");
        GeoBone Left = part("left_leg");
        if (Back == null || Front == null || Right == null || Left == null) {
            return;
        }
        boolean Seated = Owner.isSittingPose();
        Back.setHidden(Seated);
        Front.setHidden(Seated);
        if (Seated) {
            return;
        }
        Back.setRotX(Math.min(Right.getRotX(), Left.getRotX()));
        Back.setRotY(0.0f);
        Back.setRotZ(0.0f);
    }
}
