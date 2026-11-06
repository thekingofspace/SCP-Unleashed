package net.rai.scp_unleashed.scp049.client;

import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.ZombieVillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp049.Scp049Cured;

public class Scp049CuredVillagerRenderer extends HumanoidMobRenderer<Scp049Cured, ZombieVillagerModel<Scp049Cured>> {
    private static final ResourceLocation Skin = ResourceLocation.withDefaultNamespace("textures/entity/zombie_villager/zombie_villager.png");

    public Scp049CuredVillagerRenderer(EntityRendererProvider.Context Context) {
        super(Context, new ZombieVillagerModel<>(Context.bakeLayer(ModelLayers.ZOMBIE_VILLAGER)), 0.5f);
        addLayer(new HumanoidArmorLayer<>(this, new HumanoidArmorModel<>(Context.bakeLayer(ModelLayers.ZOMBIE_VILLAGER_INNER_ARMOR)),
                new HumanoidArmorModel<>(Context.bakeLayer(ModelLayers.ZOMBIE_VILLAGER_OUTER_ARMOR)), Context.getModelManager()));
        addLayer(new VillagerProfessionLayer<>(this, Context.getResourceManager(), "zombie_villager"));
    }

    @Override
    public ResourceLocation getTextureLocation(Scp049Cured Risen) {
        return Skin;
    }

    @Override
    protected boolean shouldShowName(Scp049Cured Risen) {
        return false;
    }
}
