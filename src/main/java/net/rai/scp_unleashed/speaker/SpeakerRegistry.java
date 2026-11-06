package net.rai.scp_unleashed.speaker;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.speaker.cassette.CassetteItem;
import net.rai.scp_unleashed.speaker.cassette.CassetteLibrary;
import net.rai.scp_unleashed.speaker.cassette.CassetteRecording;

import java.util.LinkedHashMap;
import java.util.Map;

public final class SpeakerRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents ComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);

    public static final DeferredBlock<SpeakerBlock> Speaker = BlockRegister.register("speaker", () -> new SpeakerBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).strength(1.5f, 6.0f).sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<SoundConsoleBlock> SoundConsole = BlockRegister.register("sound_console", () -> new SoundConsoleBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.0f, 6.0f).sound(SoundType.METAL).noOcclusion()));

    public static final DeferredItem<BlockItem> SpeakerItem = ItemRegister.registerSimpleBlockItem("speaker", Speaker);
    public static final DeferredItem<BlockItem> SoundConsoleItem = ItemRegister.registerSimpleBlockItem("sound_console", SoundConsole);
    public static final DeferredItem<CassetteItem> CassetteTape = ItemRegister.register("cassette_tape", () -> new CassetteItem(new Item.Properties().stacksTo(16)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SpeakerBlockEntity>> SpeakerEntity = BlockEntityTypeRegister.register("speaker",
            () -> BlockEntityType.Builder.of(SpeakerBlockEntity::new, Speaker.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SoundConsoleBlockEntity>> SoundConsoleEntity = BlockEntityTypeRegister.register("sound_console",
            () -> BlockEntityType.Builder.of(SoundConsoleBlockEntity::new, SoundConsole.get()).build(null));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CassetteRecording>> CassetteComponent = ComponentRegister.registerComponentType("speaker_cassette",
            Builder -> Builder.persistent(CassetteRecording.RecordingCodec).networkSynchronized(CassetteRecording.RecordingStreamCodec));

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<CassetteLibrary>> Library = AttachmentRegister.register("speaker_library",
            () -> AttachmentType.builder(() -> CassetteLibrary.Empty).serialize(CassetteLibrary.LibraryCodec, Saved -> !Saved.Recordings().isEmpty()).copyOnDeath().build());

    public static final Map<ResourceLocation, DeferredHolder<SoundEvent, SoundEvent>> Sounds = registerSounds();
    public static final DeferredHolder<SoundEvent, SoundEvent> CassetteInsert = sound("speaker.ui.cassette_insert");
    public static final DeferredHolder<SoundEvent, SoundEvent> LatchOn = sound("speaker.ui.latch_on");
    public static final DeferredHolder<SoundEvent, SoundEvent> LatchOff = sound("speaker.ui.latch_off");

    private SpeakerRegistry() {
    }

    private static Map<ResourceLocation, DeferredHolder<SoundEvent, SoundEvent>> registerSounds() {
        Map<ResourceLocation, DeferredHolder<SoundEvent, SoundEvent>> Registered = new LinkedHashMap<>();
        for (SoundCatalog.Entry Entry : SoundCatalog.Entries) {
            Registered.put(Entry.Id(), sound(Entry.getRegistryName()));
        }
        return Map.copyOf(Registered);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ComponentRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
    }
}
