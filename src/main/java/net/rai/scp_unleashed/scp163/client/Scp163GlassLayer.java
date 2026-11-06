package net.rai.scp_unleashed.scp163.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp163.Scp163Castaway;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class Scp163GlassLayer extends GeoRenderLayer<Scp163Castaway> {
    public static final ResourceLocation GlassTexture = Scp163GeoModel.location("textures/entity/scp163/" + Scp163GeoModel.Name + "_glass.png");
    public static final int Clear = 0xFFFFFFFF;

    public Scp163GlassLayer(GeoRenderer<Scp163Castaway> Renderer) {
        super(Renderer);
    }

    @Override
    public void render(PoseStack Pose, Scp163Castaway Creature, BakedGeoModel Model, @Nullable RenderType Type, MultiBufferSource Buffers, @Nullable VertexConsumer Buffer,
                       float PartialTick, int PackedLight, int PackedOverlay) {
        if (Buffers == null || !Creature.isSuited() || Creature.isInvisible() || ShaderCompat.renderingShadowPass()) {
            return;
        }
        if (Model.getBone(Scp163GeoModel.VisorBone).isEmpty()) {
            return;
        }
        RenderType Glass = RenderType.entityTranslucentCull(GlassTexture);
        getRenderer().reRender(Model, Pose, Buffers, Creature, Glass, Buffers.getBuffer(Glass), PartialTick, PackedLight, PackedOverlay, Clear);
    }
}
