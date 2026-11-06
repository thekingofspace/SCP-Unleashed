package net.rai.scp_unleashed.scp140;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.storage.StorageBehaviours;
import net.rai.scp_unleashed.stranger.StrangerEncounters;

import java.util.List;

public final class Scp140Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_140");
    public static final float BookHardness = 1.0f;
    public static final float BookResistance = 3600000.0f;

    public static final DeferredBlock<Scp140BookBlock> Book = BlockRegister.register("scp140_chronicle",
            () -> new Scp140BookBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(BookHardness, BookResistance)
                    .sound(SoundType.CHISELED_BOOKSHELF).noOcclusion().forceSolidOn().pushReaction(PushReaction.BLOCK)
                    .isValidSpawn((State, Getter, Pos, Type) -> false).isRedstoneConductor((State, Getter, Pos) -> false)));
    public static final DeferredItem<Scp140BookItem> BookItem = ItemRegister.register("scp140_chronicle",
            () -> new Scp140BookItem(Book.get(), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp140BookBlockEntity>> BookEntity = BlockEntityTypeRegister.register("scp140_chronicle",
            () -> BlockEntityType.Builder.of(Scp140BookBlockEntity::new, Book.get()).build(null));

    public static final DeferredHolder<SoundEvent, SoundEvent> BookOpen = sound("scp140_book_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> BookClose = sound("scp140_book_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> BookLeather = sound("scp140_book_leather");
    public static final DeferredHolder<SoundEvent, SoundEvent> BookThump = sound("scp140_book_thump");
    public static final DeferredHolder<SoundEvent, SoundEvent> PageTurn = sound("scp140_page_turn");
    public static final DeferredHolder<SoundEvent, SoundEvent> PageRustle = sound("scp140_page_rustle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Quill = sound("scp140_quill");
    public static final DeferredHolder<SoundEvent, SoundEvent> QuillScratch = sound("scp140_quill_scratch");
    public static final DeferredHolder<SoundEvent, SoundEvent> InkSeep = sound("scp140_ink_seep");
    public static final DeferredHolder<SoundEvent, SoundEvent> InkSeepWet = sound("scp140_ink_seep_wet");
    public static final DeferredHolder<SoundEvent, SoundEvent> PageWritten = sound("scp140_page_written");
    public static final DeferredHolder<SoundEvent, SoundEvent> PageChant = sound("scp140_page_chant");
    public static final DeferredHolder<SoundEvent, SoundEvent> PageFlutter = sound("scp140_page_flutter");
    public static final DeferredHolder<SoundEvent, SoundEvent> Whisper = sound("scp140_whisper");
    public static final DeferredHolder<SoundEvent, SoundEvent> WhisperMurmur = sound("scp140_whisper_murmur");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> OpenLayers = List.of(BookOpen, BookLeather);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CloseLayers = List.of(BookClose, BookThump, BookLeather);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> TurnLayers = List.of(PageTurn, PageRustle);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> QuillLayers = List.of(Quill, QuillScratch);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SeepLayers = List.of(InkSeep, InkSeepWet);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> WrittenLayers = List.of(PageWritten, QuillScratch, PageChant, PageFlutter, InkSeepWet);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> WhisperLayers = List.of(Whisper, WhisperMurmur);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Ink = ParticleRegister.register("scp140_ink", () -> new SimpleParticleType(false));

    private Scp140Registry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        StorageBehaviours.register(Scp140BookItem::is, Scp140Containment.Instance);
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
    }
}
