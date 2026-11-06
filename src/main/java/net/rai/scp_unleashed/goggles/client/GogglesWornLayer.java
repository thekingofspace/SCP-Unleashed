package net.rai.scp_unleashed.goggles.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.goggles.Goggles;
import net.rai.scp_unleashed.scp020.Scp020GogglesItem;
import net.rai.scp_unleashed.scp020.client.Scp020GogglesLayer;
import net.rai.scp_unleashed.scp096.Scp096GogglesItem;
import net.rai.scp_unleashed.scp096.client.Scp096GogglesLayer;
import net.rai.scp_unleashed.scp268.client.Scp268WornLayer;
import net.rai.scp_unleashed.senses.Senses;

import java.util.List;

public class GogglesWornLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final List<Goggles.Kind> Shown = List.of(Goggles.Kind.Scramble, Goggles.Kind.Hyperspectral);

    public GogglesWornLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> Parent) {
        super(Parent);
    }

    @Override
    public void render(PoseStack Pose, MultiBufferSource Buffers, int PackedLight, AbstractClientPlayer Wearer, float LimbSwing, float LimbSwingAmount,
                       float PartialTick, float AgeInTicks, float HeadYaw, float HeadPitch) {
        if (Wearer.isInvisible() || !Senses.curiosLoaded()) {
            return;
        }
        ItemStack Head = Wearer.getItemBySlot(EquipmentSlot.HEAD);
        for (Goggles.Kind Kind : Shown) {
            if (Head.is(Kind.item()) || !Senses.showsWorn(Wearer, Kind.item())) {
                continue;
            }
            ModelPart Part = model(Kind).head;
            Part.copyFrom(getParentModel().head);
            if (!Head.isEmpty()) {
                Part.xScale = Scp268WornLayer.HelmetWidth;
                Part.yScale = Scp268WornLayer.HelmetHeight;
                Part.zScale = Scp268WornLayer.HelmetWidth;
            }
            Part.render(Pose, Buffers.getBuffer(RenderType.armorCutoutNoCull(texture(Kind))), PackedLight, OverlayTexture.NO_OVERLAY);
            Part.xScale = 1.0F;
            Part.yScale = 1.0F;
            Part.zScale = 1.0F;
        }
    }

    private static HumanoidModel<LivingEntity> model(Goggles.Kind Kind) {
        return Kind == Goggles.Kind.Scramble ? Scp096GogglesLayer.model() : Scp020GogglesLayer.model();
    }

    private static ResourceLocation texture(Goggles.Kind Kind) {
        return Kind == Goggles.Kind.Scramble ? Scp096GogglesItem.ModelTexture : Scp020GogglesItem.ModelTexture;
    }
}
