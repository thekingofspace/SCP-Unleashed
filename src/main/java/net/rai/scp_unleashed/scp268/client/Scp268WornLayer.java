package net.rai.scp_unleashed.scp268.client;

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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.scp268.Scp268CapItem;
import net.rai.scp_unleashed.scp268.Scp268Registry;
import net.rai.scp_unleashed.senses.Senses;

public class Scp268WornLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final float HelmetWidth = 1.25F;
    public static final float HelmetHeight = 1.125F;

    public Scp268WornLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> Parent) {
        super(Parent);
    }

    @Override
    public void render(PoseStack Pose, MultiBufferSource Buffers, int PackedLight, AbstractClientPlayer Wearer, float LimbSwing, float LimbSwingAmount,
                       float PartialTick, float AgeInTicks, float HeadYaw, float HeadPitch) {
        if (Wearer.isInvisible() || Wearer.getItemBySlot(EquipmentSlot.HEAD).is(Scp268Registry.Cap.get()) || !Senses.showsWorn(Wearer, Scp268Registry.Cap.get())) {
            return;
        }
        HumanoidModel<LivingEntity> Cap = Scp268CapLayer.model();
        ModelPart Head = Cap.head;
        Head.copyFrom(getParentModel().head);
        if (!Wearer.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            Head.xScale = HelmetWidth;
            Head.yScale = HelmetHeight;
            Head.zScale = HelmetWidth;
        }
        Head.render(Pose, Buffers.getBuffer(RenderType.armorCutoutNoCull(Scp268CapItem.ModelTexture)), PackedLight, OverlayTexture.NO_OVERLAY);
        Head.xScale = 1.0F;
        Head.yScale = 1.0F;
        Head.zScale = 1.0F;
    }
}
