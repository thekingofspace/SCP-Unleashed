package net.rai.scp_unleashed.scp131.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class Scp131GlassLayer<T extends GeoAnimatable> extends GeoRenderLayer<T> {
    public static final ResourceLocation GlassTexture = Scp131GeoModel.location("textures/entity/scp131/" + Scp131GeoModel.Name + "_glass.png");
    public static final int Clear = 0xFFFFFFFF;

    public Scp131GlassLayer(GeoRenderer<T> Renderer) {
        super(Renderer);
    }

    @Override
    public void render(PoseStack Pose, T Animatable, BakedGeoModel Model, @Nullable RenderType Type, MultiBufferSource Buffers, @Nullable VertexConsumer Buffer,
                       float PartialTick, int PackedLight, int PackedOverlay) {
        if (Buffers == null || ShaderCompat.renderingShadowPass()) {
            return;
        }
        if (Animatable instanceof Entity Owner && Owner.isInvisible()) {
            return;
        }
        RenderType Glass = RenderType.entityTranslucent(GlassTexture);
        getRenderer().reRender(Model, Pose, Buffers, Animatable, Glass, Buffers.getBuffer(Glass), PartialTick, PackedLight, PackedOverlay, Clear);
    }
}
