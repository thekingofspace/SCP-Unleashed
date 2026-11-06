package net.rai.scp_unleashed.scp539.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.client.geo.SharedGeoModel;
import net.rai.scp_unleashed.scp539.Scp539Helicopter;
import net.rai.scp_unleashed.scp539.Scp539Registry;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;

public class Scp539HelicopterGeoModel extends SharedGeoModel<Scp539Helicopter> {
    public static final ResourceLocation ModelPath = Scp539Registry.location("geo/entity/scp539/scp539_helicopter.geo.json");
    public static final ResourceLocation TexturePath = Scp539Registry.location("textures/entity/scp539/scp539_helicopter.png");
    public static final ResourceLocation AnimationPath = Scp539Registry.location("animations/entity/scp539/scp539_helicopter.animation.json");
    public static final String MainRotor = "main_rotor";
    public static final String TailRotor = "tail_rotor";
    public static final float MainSpin = 1.1f;
    public static final float TailSpin = 1.9f;

    @Override
    public ResourceLocation getModelResource(Scp539Helicopter Craft) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp539Helicopter Craft) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp539Helicopter Craft) {
        return AnimationPath;
    }

    @Override
    public RenderType getRenderType(Scp539Helicopter Craft, ResourceLocation Texture) {
        return RenderType.entityCutoutNoCull(Texture);
    }

    @Nullable
    private GeoBone find(String Name) {
        return getAnimationProcessor().getBone(Name);
    }

    @Override
    public void setCustomAnimations(Scp539Helicopter Craft, long InstanceId, AnimationState<Scp539Helicopter> Animated) {
        float Time = Craft.tickCount + Animated.getPartialTick();
        GeoBone Main = find(MainRotor);
        if (Main != null) {
            Main.setRotY(-(Time * MainSpin) % Mth.TWO_PI);
        }
        GeoBone Tail = find(TailRotor);
        if (Tail != null) {
            Tail.setRotX((Time * TailSpin) % Mth.TWO_PI);
        }
    }
}
