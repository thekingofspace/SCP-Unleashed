package net.rai.scp_unleashed.scp001day.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
import net.rai.scp_unleashed.scp001day.Scp001DayRegistry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp001DayGearClient {
    public static final ModelLayerLocation HatLayer = new ModelLayerLocation(Scp001DayRegistry.location("scp001_day_sun_hat"), "main");
    public static final ModelLayerLocation WrapsLayer = new ModelLayerLocation(Scp001DayRegistry.location("scp001_day_sun_wraps"), "main");
    public static final ModelLayerLocation LegsLayer = new ModelLayerLocation(Scp001DayRegistry.location("scp001_day_sun_leg_wraps"), "main");
    public static final ModelLayerLocation BootsLayer = new ModelLayerLocation(Scp001DayRegistry.location("scp001_day_sun_boots"), "main");
    private static final Map<ModelLayerLocation, GearArmorModel> Baked = new ConcurrentHashMap<>();

    private Scp001DayGearClient() {
    }

    private record Worn(ModelLayerLocation Layer) implements IClientItemExtensions {
        @Override
        public HumanoidModel<?> getHumanoidArmorModel(LivingEntity Wearer, ItemStack Stack, EquipmentSlot Slot, HumanoidModel<?> Original) {
            return Baked.computeIfAbsent(Layer, Key -> new GearArmorModel(Minecraft.getInstance().getEntityModels().bakeLayer(Key)));
        }
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions Event) {
        Event.registerLayerDefinition(HatLayer, Scp001DayGearLayers::scp001DaySunHat);
        Event.registerLayerDefinition(WrapsLayer, Scp001DayGearLayers::scp001DaySunWraps);
        Event.registerLayerDefinition(LegsLayer, Scp001DayGearLayers::scp001DaySunLegWraps);
        Event.registerLayerDefinition(BootsLayer, Scp001DayGearLayers::scp001DaySunBoots);
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers Event) {
        Baked.clear();
    }

    @SubscribeEvent
    public static void registerExtensions(RegisterClientExtensionsEvent Event) {
        Event.registerItem(new Worn(HatLayer), Scp001DayRegistry.SunHat.get());
        Event.registerItem(new Worn(WrapsLayer), Scp001DayRegistry.SunWraps.get());
        Event.registerItem(new Worn(LegsLayer), Scp001DayRegistry.SunLegWraps.get());
        Event.registerItem(new Worn(BootsLayer), Scp001DayRegistry.SunBoots.get());
    }
}
