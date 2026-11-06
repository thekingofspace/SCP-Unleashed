package net.rai.scp_unleashed.scp1762.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.client.geo.SharedGeoModel;
import net.rai.scp_unleashed.scp1762.Scp1762Dragon;
import net.rai.scp_unleashed.scp1762.Scp1762Kind;
import net.rai.scp_unleashed.scp1762.Scp1762Registry;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.List;

public class Scp1762DragonGeoModel extends SharedGeoModel<Scp1762Dragon> {
    public static final List<String> Legs = List.of("leg", "leg_back", "leg_front");
    public static final List<String> Airborne = List.of("fly", "glide", "swoop");
    public static final float DangleRatio = 0.55f;
    public static final float DangleLimit = 18.0f;
    public static final float DangleFloor = 4.0f;

    private final ResourceLocation ModelPath;
    private final ResourceLocation TexturePath;
    private final ResourceLocation AnimationPath;

    public Scp1762DragonGeoModel(Scp1762Kind Kind) {
        ModelPath = Scp1762Registry.location("geo/entity/scp1762/" + Kind.id() + ".geo.json");
        TexturePath = Scp1762Registry.location("textures/entity/scp1762/" + Kind.id() + ".png");
        AnimationPath = Scp1762Registry.location("animations/entity/scp1762/" + Kind.id() + ".animation.json");
    }

    @Override
    public ResourceLocation getModelResource(Scp1762Dragon Dragon) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp1762Dragon Dragon) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp1762Dragon Dragon) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(Scp1762Dragon Dragon, long InstanceId, AnimationState<Scp1762Dragon> State) {
        super.setCustomAnimations(Dragon, InstanceId, State);
        if (Dragon.holdingClip() || !Airborne.contains(Dragon.clip())) {
            return;
        }
        float Roll = Dragon.roll(State.getPartialTick());
        if (Math.abs(Roll) < DangleFloor) {
            return;
        }
        String Outer = Roll > 0.0f ? "_r" : "_l";
        float Dangle = (float) Math.toRadians(Math.min(DangleLimit, Math.abs(Roll) * DangleRatio));
        for (String Leg : Legs) {
            GeoBone Bone = getAnimationProcessor().getBone(Leg + Outer);
            if (Bone != null) {
                Bone.setRotX(Bone.getRotX() - Dangle);
            }
        }
    }

    @Override
    public RenderType getRenderType(Scp1762Dragon Dragon, ResourceLocation Texture) {
        return RenderType.entityCutoutNoCull(Texture);
    }
}
