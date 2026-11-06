package net.rai.scp_unleashed.scp4666.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.gear.client.GearArmorModel;
import net.rai.scp_unleashed.scp4666.Scp4666Registry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp4666GearClient {
    public static final ModelLayerLocation HatLayer = new ModelLayerLocation(Scp4666Registry.location("scp4666_winter_hat"), "main");
    public static final ModelLayerLocation CoatLayer = new ModelLayerLocation(Scp4666Registry.location("scp4666_winter_coat"), "main");
    public static final ModelLayerLocation BootsLayer = new ModelLayerLocation(Scp4666Registry.location("scp4666_winter_boots"), "main");
    private static final Map<ModelLayerLocation, GearArmorModel> Baked = new ConcurrentHashMap<>();

    private Scp4666GearClient() {
    }

    private record Worn(ModelLayerLocation Layer) implements IClientItemExtensions {
        @Override
        public HumanoidModel<?> getHumanoidArmorModel(LivingEntity Wearer, ItemStack Stack, EquipmentSlot Slot, HumanoidModel<?> Original) {
            return Baked.computeIfAbsent(Layer, Key -> new GearArmorModel(Minecraft.getInstance().getEntityModels().bakeLayer(Key)));
        }
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions Event) {
        Event.registerLayerDefinition(HatLayer, Scp4666GearLayers::scp4666WinterHat);
        Event.registerLayerDefinition(CoatLayer, Scp4666GearLayers::scp4666WinterCoat);
        Event.registerLayerDefinition(BootsLayer, Scp4666GearLayers::scp4666WinterBoots);
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers Event) {
        Baked.clear();
    }

    @SubscribeEvent
    public static void registerExtensions(RegisterClientExtensionsEvent Event) {
        Event.registerItem(new Worn(HatLayer), Scp4666Registry.WinterHat.get());
        Event.registerItem(new Worn(CoatLayer), Scp4666Registry.WinterCoat.get());
        Event.registerItem(new Worn(BootsLayer), Scp4666Registry.WinterBoots.get());
    }
}
