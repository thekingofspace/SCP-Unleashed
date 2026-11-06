package net.rai.scp_unleashed.dclass.theodore.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.theodore.AmalgamationEyes;
import net.rai.scp_unleashed.dclass.theodore.TheodoreAmalgamation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

public class AmalgamationGeoModel extends GeoModel<TheodoreAmalgamation> {
    public static final ResourceLocation ModelPath = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "geo/entity/dclass/theodore_amalgamation.geo.json");
    public static final ResourceLocation AnimationPath = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "animations/entity/dclass/theodore_amalgamation.animation.json");
    public static final ResourceLocation TexturePath = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/entity/dclass/theodore_amalgamation.png");
    public static final float PupilTravel = 0.5f;
    public static final String[] Sides = {"r", "l"};

    @Override
    public ResourceLocation getModelResource(TheodoreAmalgamation Owner) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(TheodoreAmalgamation Owner) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(TheodoreAmalgamation Owner) {
        return AnimationPath;
    }

    @Override
    public RenderType getRenderType(TheodoreAmalgamation Owner, ResourceLocation Texture) {
        return RenderType.entityCutoutNoCull(Texture);
    }

    @Override
    public void setCustomAnimations(TheodoreAmalgamation Owner, long InstanceId, AnimationState<TheodoreAmalgamation> Animated) {
        super.setCustomAnimations(Owner, InstanceId, Animated);
        AmalgamationEyes Eyes = Owner.eyes();
        if (Eyes == null) {
            return;
        }
        float Partial = Animated.getPartialTick();
        boolean Dead = Owner.isDeadOrDying();
        for (int Head = 0; Head < Eyes.count(); Head++) {
            float LookX = Dead ? 0.0f : Eyes.lookX(Head, Partial);
            float LookY = Dead ? -1.0f : Eyes.lookY(Head, Partial);
            for (String Side : Sides) {
                GeoBone Pupil = getAnimationProcessor().getBone("eye_" + Head + "_" + Side);
                if (Pupil != null) {
                    Pupil.setPosX(-LookX * PupilTravel);
                    Pupil.setPosY(-LookY * PupilTravel);
                }
            }
        }
    }
}
