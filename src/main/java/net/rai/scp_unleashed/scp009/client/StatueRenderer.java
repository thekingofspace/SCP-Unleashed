package net.rai.scp_unleashed.scp009.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp009.Scp009Registry;
import net.rai.scp_unleashed.scp009.Scp009Statue;

public class StatueRenderer extends EntityRenderer<Scp009Statue> {
    public static final ResourceLocation Crystal = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/entity/scp009/statue.png");
    public static final int Glow = 7;

    public StatueRenderer(EntityRendererProvider.Context Context) {
        super(Context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public ResourceLocation getTextureLocation(Scp009Statue Statue) {
        return Crystal;
    }

    @Override
    public void render(Scp009Statue Statue, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        int Lit = LightTexture.pack(Math.max(LightTexture.block(Light), Glow), LightTexture.sky(Light));
        Entity Dummy = StatueDummies.dummyFor(Statue);
        if (Dummy == null) {
            fallback(Statue, Pose, Buffers, Lit);
            return;
        }
        Pose.pushPose();
        try {
            draw(Dummy, Pose, new CrystalBuffers(Buffers, RenderType.entityTranslucent(Crystal), Glow), Lit);
        } catch (RuntimeException Problem) {
            Statue.markDummyFailed();
        } finally {
            Pose.popPose();
        }
    }

    private static <E extends Entity> void draw(E Dummy, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        EntityRenderer<? super E> Renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(Dummy);
        Vec3 Offset = Renderer.getRenderOffset(Dummy, 1.0f);
        Pose.translate(Offset.x, Offset.y, Offset.z);
        Renderer.render(Dummy, Dummy.getYRot(), 1.0f, Pose, Buffers, Light);
    }

    private static void fallback(Scp009Statue Statue, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        float Width = Statue.getBbWidth();
        float Height = Statue.getBbHeight();
        Pose.pushPose();
        Pose.translate(-Width * 0.5f, 0.0f, -Width * 0.5f);
        Pose.scale(Width, Height, Width);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Scp009Registry.RedIce.get().defaultBlockState(), Pose, Buffers, Light, OverlayTexture.NO_OVERLAY);
        Pose.popPose();
    }
}
