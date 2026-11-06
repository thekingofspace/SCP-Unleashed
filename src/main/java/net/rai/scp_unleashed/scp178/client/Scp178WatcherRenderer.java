package net.rai.scp_unleashed.scp178.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp178.Scp178Watcher;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

public class Scp178WatcherRenderer extends GeoEntityRenderer<Scp178Watcher> {
    public static final float Shadow = 0.42f;

    public Scp178WatcherRenderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp178WatcherModel());
        shadowRadius = Shadow;
    }

    @Override
    public boolean shouldRender(Scp178Watcher Watcher, Frustum View, double CameraX, double CameraY, double CameraZ) {
        return Scp178Vision.sees(Watcher) && super.shouldRender(Watcher, View, CameraX, CameraY, CameraZ);
    }

    @Override
    public void render(Scp178Watcher Watcher, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Watcher.setRenderPartial(PartialTick);
        float Alpha = Scp178Vision.alpha(Watcher, PartialTick);
        if (Alpha <= 0.0f) {
            return;
        }
        shadowRadius = Shadow * Alpha;
        super.render(Watcher, Yaw, PartialTick, Pose, Buffers, Light);
    }

    @Nullable
    @Override
    public RenderType getRenderType(Scp178Watcher Watcher, ResourceLocation Texture, @Nullable MultiBufferSource Buffers, float PartialTick) {
        float Alpha = Scp178Vision.alpha(Watcher, PartialTick);
        if (Alpha < 0.5f && ShaderCompat.renderingShadowPass()) {
            return null;
        }
        return Alpha < 0.999f ? RenderType.entityTranslucent(Texture) : RenderType.entityCutoutNoCull(Texture);
    }

    @Override
    public Color getRenderColor(Scp178Watcher Watcher, float PartialTick, int PackedLight) {
        return Color.ofRGBA(1.0f, 1.0f, 1.0f, Scp178Vision.alpha(Watcher, PartialTick));
    }

    @Override
    protected float getDeathMaxRotation(Scp178Watcher Watcher) {
        return 0.0f;
    }
}
