package net.rai.scp_unleashed.scp049.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp049.Scp049Cured;

public class Scp049CuredPlayerRenderer extends HumanoidMobRenderer<Scp049Cured, Scp049CuredModel> {
    private static final ResourceLocation Fallback = ResourceLocation.withDefaultNamespace("textures/entity/zombie/zombie.png");

    private final Scp049CuredModel Wide;
    private final Scp049CuredModel Slim;

    public Scp049CuredPlayerRenderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp049CuredModel(Context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        this.Wide = model;
        this.Slim = new Scp049CuredModel(Context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        addLayer(new HumanoidArmorLayer<>(this, new HumanoidArmorModel<>(Context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel<>(Context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), Context.getModelManager()));
    }

    public boolean canDraw(Scp049Cured Risen) {
        return Scp049Skins.skinOf(Risen) != null;
    }

    @Override
    public void render(Scp049Cured Risen, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight) {
        PlayerSkin Skin = Scp049Skins.skinOf(Risen);
        model = Skin != null && Skin.model() == PlayerSkin.Model.SLIM ? Slim : Wide;
        super.render(Risen, Yaw, PartialTick, Pose, Buffers, PackedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(Scp049Cured Risen) {
        PlayerSkin Skin = Scp049Skins.skinOf(Risen);
        return Skin == null ? Fallback : Skin.texture();
    }

    @Override
    protected boolean shouldShowName(Scp049Cured Risen) {
        return false;
    }
}
