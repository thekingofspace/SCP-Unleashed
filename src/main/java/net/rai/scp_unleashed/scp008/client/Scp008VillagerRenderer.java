package net.rai.scp_unleashed.scp008.client;

import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.ZombieVillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp008.Scp008Zombie;

public class Scp008VillagerRenderer extends HumanoidMobRenderer<Scp008Zombie, ZombieVillagerModel<Scp008Zombie>> {
    private static final ResourceLocation Skin = ResourceLocation.withDefaultNamespace("textures/entity/zombie_villager/zombie_villager.png");

    public Scp008VillagerRenderer(EntityRendererProvider.Context Context) {
        super(Context, new ZombieVillagerModel<>(Context.bakeLayer(ModelLayers.ZOMBIE_VILLAGER)), 0.5f);
        addLayer(new HumanoidArmorLayer<>(this, new HumanoidArmorModel<>(Context.bakeLayer(ModelLayers.ZOMBIE_VILLAGER_INNER_ARMOR)),
                new HumanoidArmorModel<>(Context.bakeLayer(ModelLayers.ZOMBIE_VILLAGER_OUTER_ARMOR)), Context.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(Scp008Zombie Risen) {
        return Skin;
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
