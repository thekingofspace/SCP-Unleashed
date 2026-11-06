package net.rai.scp_unleashed.scp012;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.storage.StorageBehaviours;
import net.rai.scp_unleashed.stranger.StrangerEncounters;

public final class Scp012Registry {
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents DataComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_012");
    public static final ResourceKey<DamageType> CompositionDamage = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp012_composition"));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> Completeness = DataComponentRegister.registerComponentType("scp012_completeness",
            Builder -> Builder.persistent(Codec.intRange(0, Scp012CompositionItem.Complete)).networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> Composer = AttachmentRegister.register("scp012_composer",
            () -> AttachmentType.builder(() -> Boolean.FALSE).serialize(Codec.BOOL, Saved -> Saved).build());

    public static final DeferredItem<Scp012CompositionItem> Composition = ItemRegister.register("scp012_composition",
            () -> new Scp012CompositionItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final DeferredHolder<SoundEvent, SoundEvent> Note = sound("scp012_note");
    public static final DeferredHolder<SoundEvent, SoundEvent> NoteBell = sound("scp012_note_bell");
    public static final DeferredHolder<SoundEvent, SoundEvent> Toll = sound("scp012_toll");
    public static final DeferredHolder<SoundEvent, SoundEvent> TollRing = sound("scp012_toll_ring");
    public static final DeferredHolder<SoundEvent, SoundEvent> Page = sound("scp012_page");
    public static final DeferredHolder<SoundEvent, SoundEvent> Complete = sound("scp012_complete");
    public static final DeferredHolder<SoundEvent, SoundEvent> Copy = sound("scp012_copy");
    public static final DeferredHolder<SoundEvent, SoundEvent> Unveiled = sound("scp012_unveiled");

    private Scp012Registry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        ItemRegister.register(ModEventBus);
        DataComponentRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
        StorageBehaviours.register(Scp012CompositionItem::is, Scp012Containment.Instance);
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
    }
}
