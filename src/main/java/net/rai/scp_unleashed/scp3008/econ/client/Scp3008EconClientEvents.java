package net.rai.scp_unleashed.scp3008.econ.client;

import net.minecraft.client.model.HumanoidModel;
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
import net.rai.scp_unleashed.scp3008.econ.Scp3008EconRegistry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp3008EconClientEvents {
    private Scp3008EconClientEvents() {
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions Event) {
        for (Scp3008EconArmorModels.Piece Each : Scp3008EconArmorModels.Piece.values()) {
            Event.registerLayerDefinition(Each.Location, Each.Definition);
        }
    }

    @SubscribeEvent
    public static void bakeLayers(EntityRenderersEvent.AddLayers Event) {
        Scp3008EconArmorModels.bake(Event.getEntityModels());
    }

    private static IClientItemExtensions worn(Scp3008EconArmorModels.Piece Each) {
        return new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity Wearer, ItemStack Stack, EquipmentSlot Slot, HumanoidModel<?> Original) {
                return Scp3008EconArmorModels.model(Each);
            }
        };
    }

    @SubscribeEvent
    public static void registerExtensions(RegisterClientExtensionsEvent Event) {
        Event.registerItem(worn(Scp3008EconArmorModels.Piece.Colander), Scp3008EconRegistry.ColanderHelmet.get());
        Event.registerItem(worn(Scp3008EconArmorModels.Piece.Vest), Scp3008EconRegistry.CushionVest.get());
        Event.registerItem(worn(Scp3008EconArmorModels.Piece.Slippers), Scp3008EconRegistry.FeltSlippers.get());
    }
}
