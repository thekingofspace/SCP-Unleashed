package net.rai.scp_unleashed.daevite.client;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.daevite.entity.DaeviteKnight;
import net.rai.scp_unleashed.dclass.client.DClassGeoModel;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

public class DaeviteKnightModel extends DClassGeoModel<DaeviteKnight> {
    public static final ResourceLocation GeoPath = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "geo/entity/daevite/knight.geo.json");
    public static final ResourceLocation SpearClips = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "animations/entity/daevite/knight_spear.animation.json");
    public static final ResourceLocation SwordClips = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "animations/entity/daevite/knight_sword.animation.json");
    public static final ResourceLocation SkinPath = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/entity/daevite/knight.png");

    @Override
    public ResourceLocation getModelResource(DaeviteKnight Owner) {
        return GeoPath;
    }

    @Override
    public ResourceLocation getTextureResource(DaeviteKnight Owner) {
        return SkinPath;
    }

    @Override
    public ResourceLocation getAnimationResource(DaeviteKnight Owner) {
        return Owner.spearman() ? SpearClips : SwordClips;
    }

    @Nullable
    private GeoBone part(String Name) {
        return getAnimationProcessor().getBone(Name);
    }

    @Override
    public void setCustomAnimations(DaeviteKnight Owner, long InstanceId, AnimationState<DaeviteKnight> Animated) {
        super.setCustomAnimations(Owner, InstanceId, Animated);
        GeoBone Spear = part("spear");
        GeoBone Sword = part("sword");
        if (Spear != null) {
            Spear.setHidden(!Owner.spearman());
        }
        if (Sword != null) {
            Sword.setHidden(Owner.spearman());
        }
        GeoBone Front = part("tabard_front");
        GeoBone Back = part("tabard_back");
        GeoBone Right = part("right_leg");
        GeoBone Left = part("left_leg");
        if (Front == null || Back == null || Right == null || Left == null) {
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
