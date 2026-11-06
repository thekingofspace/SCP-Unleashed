package net.rai.scp_unleashed.guidebook;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

public final class GuidebookRegistry {
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final DeferredItem<FacilityTabletItem> FacilityTablet = ItemRegister.register("facility_tablet",
            () -> new FacilityTabletItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<SoundEvent, SoundEvent> TabletBoot = sound("facility_tablet_boot");
    public static final DeferredHolder<SoundEvent, SoundEvent> TabletPower = sound("facility_tablet_power");
    public static final DeferredHolder<SoundEvent, SoundEvent> TabletChime = sound("facility_tablet_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> TabletTap = sound("facility_tablet_tap");
    public static final DeferredHolder<SoundEvent, SoundEvent> TabletTapClick = sound("facility_tablet_tap_click");

    private GuidebookRegistry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        ItemRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
    }
}
