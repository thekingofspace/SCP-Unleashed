package net.rai.scp_unleashed.scp5001.mekhane.client;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp5001.mekhane.Scp5001MekhaneRegistry;
import net.rai.scp_unleashed.scp5001.mekhane.entity.Mekhane;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

public class MekhaneGeoModel extends GeoModel<Mekhane> {
    public static final ResourceLocation ModelPath = Scp5001MekhaneRegistry.location("geo/entity/mekhane/mekhane.geo.json");
    public static final ResourceLocation TexturePath = Scp5001MekhaneRegistry.location("textures/entity/mekhane/mekhane.png");
    public static final ResourceLocation AnimationPath = Scp5001MekhaneRegistry.location("animations/entity/mekhane/mekhane.animation.json");
    public static final String[] Tracked = {"hand_ll", "axe", "chest"};
    public static final String[][] Breakage = {
            {"plate_chest_l"},
            {"plate_pauldron_ur"},
            {"plate_chest_r"},
            {"plate_belly", "feather_l6"},
    };

    @Override
    public ResourceLocation getModelResource(Mekhane Owner) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Mekhane Owner) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Mekhane Owner) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(Mekhane Owner, long InstanceId, AnimationState<Mekhane> Animated) {
        super.setCustomAnimations(Owner, InstanceId, Animated);
        for (String Name : Tracked) {
            GeoBone Found = getAnimationProcessor().getBone(Name);
            if (Found != null) {
                Found.setTrackingMatrices(true);
            }
        }
        int Damage = Owner.damage();
        for (int Stage = 0; Stage < Breakage.length; Stage++) {
            for (String Name : Breakage[Stage]) {
                GeoBone Found = getAnimationProcessor().getBone(Name);
                if (Found != null) {
                    Found.setHidden(Damage > Stage);
                }
            }
        }
    }
}
