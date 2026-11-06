package net.rai.scp_unleashed.decal;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class DecalRegistry {
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents DataComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> DecalVariant = DataComponentRegister.registerComponentType("decal_variant",
            Builder -> Builder.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredHolder<SoundEvent, SoundEvent> DecalPlace = sound("decal_place");
    public static final DeferredHolder<SoundEvent, SoundEvent> DecalScrape = sound("decal_scrape");
    public static final DeferredHolder<SoundEvent, SoundEvent> DecalCycle = sound("decal_cycle");
    public static final DeferredHolder<SoundEvent, SoundEvent> DecalMount = sound("decal_mount");

    public static final Map<DecalKind, DeferredItem<DecalItem>> Decals = registerDecals();
    public static final DeferredItem<DecalScraperItem> DecalScraper = ItemRegister.register("decal_scraper",
            () -> new DecalScraperItem(new Item.Properties().durability(DecalScraperItem.Durability)));

    private DecalRegistry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    private static Map<DecalKind, DeferredItem<DecalItem>> registerDecals() {
        Map<DecalKind, DeferredItem<DecalItem>> Registered = new EnumMap<>(DecalKind.class);
        for (DecalKind Kind : DecalKind.values()) {
            Registered.put(Kind, ItemRegister.register("decal_" + Kind.getName(), () -> new DecalItem(Kind, new Item.Properties())));
        }
        return Collections.unmodifiableMap(Registered);
    }

    public static void register(IEventBus ModEventBus) {
        ItemRegister.register(ModEventBus);
        DataComponentRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
    }
}
