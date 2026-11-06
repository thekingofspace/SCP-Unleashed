package net.rai.scp_unleashed.mtf.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.mtf.MtfHelicopter;
import net.rai.scp_unleashed.mtf.MtfRegistry;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

public class MtfHelicopterGeoModel extends GeoModel<MtfHelicopter> {
    public static final ResourceLocation ModelPath = MtfRegistry.location("geo/entity/mtf/mtf_helicopter.geo.json");
    public static final ResourceLocation TexturePath = MtfRegistry.location("textures/entity/mtf/mtf_helicopter.png");
    public static final ResourceLocation AnimationPath = MtfRegistry.location("animations/entity/mtf/mtf_helicopter.animation.json");
    public static final float MainSpin = 0.9f;
    public static final float TailSpin = 1.7f;
    public static final int StrobePeriod = 26;
    public static final int StrobeOn = 3;

    @Override
    public ResourceLocation getModelResource(MtfHelicopter Owner) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(MtfHelicopter Owner) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(MtfHelicopter Owner) {
        return AnimationPath;
    }

    @Override
    public RenderType getRenderType(MtfHelicopter Owner, ResourceLocation Texture) {
        return RenderType.entityCutoutNoCull(Texture);
    }

    @Nullable
    private GeoBone find(String Name) {
        return getAnimationProcessor().getBone(Name);
    }

    @Override
    public void setCustomAnimations(MtfHelicopter Owner, long InstanceId, AnimationState<MtfHelicopter> Animated) {
        float Time = Owner.tickCount + Animated.getPartialTick();
        GeoBone Main = find("main_rotor");
        if (Main != null) {
            Main.setRotY(-(Time * MainSpin) % Mth.TWO_PI);
        }
        GeoBone Tail = find("tail_rotor");
        if (Tail != null) {
            Tail.setRotX((Time * TailSpin) % Mth.TWO_PI);
        }
        int Beat = Owner.tickCount % StrobePeriod;
        GeoBone Top = find("strobe_top");
        if (Top != null) {
            Top.setHidden(Beat >= StrobeOn);
        }
        GeoBone Belly = find("strobe_belly");
        if (Belly != null) {
            int Offset = (Beat + StrobePeriod / 2) % StrobePeriod;
            Belly.setHidden(Offset >= StrobeOn);
        }
    }
}
