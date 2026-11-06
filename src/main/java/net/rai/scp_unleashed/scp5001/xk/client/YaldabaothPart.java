package net.rai.scp_unleashed.scp5001.xk.client;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp5001.xk.Scp5001XkRegistry;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class YaldabaothPart implements GeoAnimatable {
    public enum Kind {
        Head("head"),
        Ring("ring"),
        RingEye("ring_eye"),
        RingMaw("ring_maw"),
        RingRib("ring_rib"),
        Tail("tail");

        private final String Key;

        Kind(String Key) {
            this.Key = Key;
        }

        public String key() {
            return Key;
        }
    }

    private final Kind Shape;
    private final ResourceLocation Model;
    private final ResourceLocation Texture;
    private final ResourceLocation Glow;
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);

    public YaldabaothPart(Kind Shape) {
        this.Shape = Shape;
        Model = Scp5001XkRegistry.location("geo/entity/yaldabaoth/yaldabaoth_" + Shape.key() + ".geo.json");
        Texture = Scp5001XkRegistry.location("textures/entity/yaldabaoth/yaldabaoth_" + Shape.key() + ".png");
        Glow = Scp5001XkRegistry.location("textures/entity/yaldabaoth/yaldabaoth_" + Shape.key() + "_glowmask.png");
    }

    public Kind shape() {
        return Shape;
    }

    public ResourceLocation model() {
        return Model;
    }

    public ResourceLocation texture() {
        return Texture;
    }

    public ResourceLocation glow() {
        return Glow;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }

    @Override
    public double getTick(Object Owner) {
        return 0.0;
    }
}
