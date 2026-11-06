package net.rai.scp_unleashed.scp035.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp035.Scp035Host;
import net.rai.scp_unleashed.storage.client.TintedBuffers;
import org.jetbrains.annotations.Nullable;

public class Scp035HostRenderer extends HumanoidMobRenderer<Scp035Host, Scp035HostModel> {
    public static final float MaxPallor = 0.32f;
    public static final int PallorRed = 0xA8;
    public static final int PallorGreen = 0xB4;
    public static final int PallorBlue = 0xA4;

    private final Scp035HostModel Wide;
    private final Scp035HostModel Slim;
    @Nullable
    private MultiBufferSource Plain;

    public Scp035HostRenderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp035HostModel(Context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        this.Wide = model;
        this.Slim = new Scp035HostModel(Context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        layers.removeIf(Layer -> Layer instanceof CustomHeadLayer);
        addLayer(new MaskLayer(this, Context.getItemInHandRenderer()));
    }

    @Override
    public void render(Scp035Host Host, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        LivingEntity Dummy = Scp035HostLook.dummyFor(Host);
        if (Dummy != null) {
            boolean Drawn = false;
            Pose.pushPose();
            try {
                Scp035HostLook.pose(Host, Dummy);
                drawDummy(Dummy, Yaw, PartialTick, Pose, Buffers, Light);
                Drawn = true;
            } catch (RuntimeException Problem) {
                Host.markDummyFailed();
            } finally {
                Pose.popPose();
            }
            if (Drawn) {
                return;
            }
        }
        PlayerSkin Skin = Scp035Skins.skinOf(Host);
        model = Skin.model() == PlayerSkin.Model.SLIM ? Slim : Wide;
        float Decay = Host.decay();
        Plain = Buffers;
        try {
            super.render(Host, Yaw, PartialTick, Pose, Decay > 0.01f ? new TintedBuffers(Buffers, pallor(Decay)) : Buffers, Light);
        } finally {
            Plain = null;
        }
    }

    private static <E extends LivingEntity> void drawDummy(E Dummy, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        EntityRenderer<? super E> Renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(Dummy);
        Vec3 Offset = Renderer.getRenderOffset(Dummy, PartialTick);
        Pose.translate(Offset.x, Offset.y, Offset.z);
        Renderer.render(Dummy, Yaw, PartialTick, Pose, Buffers, Light);
    }

    public static int pallor(float Decay) {
        float Amount = Mth.clamp(Decay, 0.0f, 1.0f) * MaxPallor;
        int Red = Math.round(Mth.lerp(Amount, 255.0f, PallorRed));
        int Green = Math.round(Mth.lerp(Amount, 255.0f, PallorGreen));
        int Blue = Math.round(Mth.lerp(Amount, 255.0f, PallorBlue));
        return 0xFF000000 | Red << 16 | Green << 8 | Blue;
    }

    @Override
    public ResourceLocation getTextureLocation(Scp035Host Host) {
        return Scp035Skins.skinOf(Host).texture();
    }

    private final class MaskLayer extends RenderLayer<Scp035Host, Scp035HostModel> {
        private final ItemInHandRenderer Hands;

        private MaskLayer(RenderLayerParent<Scp035Host, Scp035HostModel> Parent, ItemInHandRenderer Hands) {
            super(Parent);
            this.Hands = Hands;
        }

        @Override
        public void render(PoseStack Pose, MultiBufferSource Buffers, int Light, Scp035Host Host, float LimbSwing, float LimbSwingAmount, float PartialTick,
                           float AgeInTicks, float HeadYaw, float HeadPitch) {
            ItemStack Worn = Host.getItemBySlot(EquipmentSlot.HEAD);
            if (Worn.isEmpty()) {
                return;
            }
            Pose.pushPose();
            getParentModel().getHead().translateAndRotate(Pose);
            CustomHeadLayer.translateToHead(Pose, false);
            Hands.renderItem(Host, Worn, ItemDisplayContext.HEAD, false, Pose, Plain != null ? Plain : Buffers, Light);
            Pose.popPose();
        }
    }
}
