package net.rai.scp_unleashed.extraction;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.FragmentRarity;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class ExtractionRegistry {
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents ComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ConjurerSelection = ComponentRegister.registerComponentType("rift_conjurer_selection",
            Builder -> Builder.persistent(Codec.intRange(0, RiftConjurerItem.Choices - 1)).networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredItem<RiftExtractorItem> RiftExtractor = ItemRegister.register("rift_extractor",
            () -> new RiftExtractorItem(new Item.Properties().durability(RiftExtractorItem.Durability)));
    public static final DeferredItem<RiftDetectorItem> RiftDetector = ItemRegister.register("rift_detector",
            () -> new RiftDetectorItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<RiftConjurerItem> RiftConjurer = ItemRegister.register("rift_conjurer",
            () -> new RiftConjurerItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final Map<FragmentRarity, DeferredItem<UnstableFragmentItem>> Fragments = registerFragments();

    public static final DeferredHolder<SoundEvent, SoundEvent> ExtractionLoop = registerSound("extraction_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> ExtractionSuccess = registerSound("extraction_success");
    public static final DeferredHolder<SoundEvent, SoundEvent> ExtractionFizzle = registerSound("extraction_fizzle");
    public static final DeferredHolder<SoundEvent, SoundEvent> DetectorClick = registerSound("extraction_detector_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> DetectorPing = registerSound("extraction_detector_ping");
    public static final DeferredHolder<SoundEvent, SoundEvent> Conjure = registerSound("extraction_conjure");
    public static final DeferredHolder<SoundEvent, SoundEvent> ConjureCycle = registerSound("extraction_conjure_cycle");

    private ExtractionRegistry() {
    }

    public static void register(IEventBus ModEventBus) {
        ItemRegister.register(ModEventBus);
        SoundRegister.register(ModEventBus);
        ComponentRegister.register(ModEventBus);
    }

    public static UnstableFragmentItem fragmentFor(FragmentRarity Grade) {
        return Fragments.get(Grade).get();
    }

    private static Map<FragmentRarity, DeferredItem<UnstableFragmentItem>> registerFragments() {
        Map<FragmentRarity, DeferredItem<UnstableFragmentItem>> Result = new EnumMap<>(FragmentRarity.class);
        for (FragmentRarity Grade : FragmentRarity.values()) {
            Result.put(Grade, ItemRegister.register("unstable_fragment_" + Grade.getSerializedName(),
                    () -> new UnstableFragmentItem(Grade, new Item.Properties().stacksTo(UnstableFragmentItem.StackSize))));
        }
        return Collections.unmodifiableMap(Result);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> registerSound(String Name) {
        return SoundRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }
}
