package net.rai.scp_unleashed.scp096.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp096.Scp096ShyGuy;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class Scp096GeoModel extends GeoModel<Scp096ShyGuy> {
    public static final ResourceLocation ModelPath = location("geo/entity/scp096/shy_guy.geo.json");
    public static final ResourceLocation TexturePath = location("textures/entity/scp096/shy_guy.png");
    public static final ResourceLocation AnimationPath = location("animations/entity/scp096/shy_guy.animation.json");
    public static final String LookBone = "look";
    public static final String BagBone = "bag";
    public static final String JawBone = "jaw";
    public static final String MawBone = "maw";
    public static final String FaceBone = "face";
    public static final String ChinBone = "chin";

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    @Override
    public ResourceLocation getModelResource(Scp096ShyGuy Guy) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp096ShyGuy Guy) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp096ShyGuy Guy) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(Scp096ShyGuy Guy, long InstanceId, AnimationState<Scp096ShyGuy> Animated) {
        GeoBone Bag = getAnimationProcessor().getBone(BagBone);
        if (Bag != null) {
            Bag.setHidden(!Guy.isBagged());
        }
        if (Guy.isBagged()) {
            GeoBone Jaw = getAnimationProcessor().getBone(JawBone);
            if (Jaw != null) {
                Jaw.setPosY(0.0f);
            }
            GeoBone Maw = getAnimationProcessor().getBone(MawBone);
            if (Maw != null) {
                Maw.setScaleY(1.0f);
            }
        }
        GeoBone Look = getAnimationProcessor().getBone(LookBone);
        EntityModelData Data = Animated.getData(DataTickets.ENTITY_MODEL_DATA);
        if (Look == null || Data == null || Guy.isDeadOrDying() || !Guy.looksAround()) {
            return;
        }
        Look.setRotX(Mth.clamp(Data.headPitch(), -Scp096ShyGuy.LookDownLimit, Guy.isBagged() ? Scp096ShyGuy.HoodPitchLimit : Scp096ShyGuy.PitchLimit) * Mth.DEG_TO_RAD);
        Look.setRotY(Mth.clamp(Data.netHeadYaw(), -Scp096ShyGuy.YawLimit, Scp096ShyGuy.YawLimit) * Mth.DEG_TO_RAD);
    }
}
