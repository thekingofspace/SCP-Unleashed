package net.rai.scp_unleashed.scp008.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.control.client.ControlBodyLooks;
import net.rai.scp_unleashed.scp008.Scp008Zombie;

public class Scp008ZombieRenderer extends HumanoidMobRenderer<Scp008Zombie, Scp008ZombieModel> {
    private static final ResourceLocation Fallback = ResourceLocation.withDefaultNamespace("textures/entity/zombie/zombie.png");

    private final Scp008ZombieModel Wide;
    private final Scp008ZombieModel Slim;
    private final Scp008VillagerRenderer VillagerLook;
    private final Scp008PlainRenderer PlainLook;

    public Scp008ZombieRenderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp008ZombieModel(Context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        this.Wide = model;
        this.Slim = new Scp008ZombieModel(Context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        this.VillagerLook = new Scp008VillagerRenderer(Context);
        this.PlainLook = new Scp008PlainRenderer(Context);
        addLayer(new HumanoidArmorLayer<>(this, new HumanoidArmorModel<>(Context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel<>(Context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), Context.getModelManager()));
    }

    @Override
    public void render(Scp008Zombie Risen, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight) {
        if (ControlBodyLooks.render(Risen, Risen.getBodyLook(), Yaw, PartialTick, Pose, Buffers, PackedLight)) {
            return;
        }
        if (Risen.isFromVillager()) {
            VillagerLook.render(Risen, Yaw, PartialTick, Pose, Buffers, PackedLight);
            return;
        }
        PlayerSkin Skin = Scp008Skins.skinOf(Risen);
        if (Skin == null) {
            PlainLook.render(Risen, Yaw, PartialTick, Pose, Buffers, PackedLight);
            return;
        }
        model = Skin.model() == PlayerSkin.Model.SLIM ? Slim : Wide;
        super.render(Risen, Yaw, PartialTick, Pose, Buffers, PackedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(Scp008Zombie Risen) {
        PlayerSkin Skin = Scp008Skins.skinOf(Risen);
        return Skin == null ? Fallback : Skin.texture();
    }

    @Override
    protected boolean shouldShowName(Scp008Zombie Risen) {
        return !Risen.hasAdoptedLook() && super.shouldShowName(Risen);
    }

    @Override
    protected boolean isShaking(Scp008Zombie Risen) {
        return super.isShaking(Risen) || Risen.isCollapsing();
    }
}
