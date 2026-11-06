package net.rai.scp_unleashed.archive;

import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.database.DatabaseBlock;
import net.rai.scp_unleashed.archive.database.DatabaseCopyItem;
import net.rai.scp_unleashed.archive.database.DatabaseTerminalBlock;
import net.rai.scp_unleashed.archive.document.DocumentStamp;
import net.rai.scp_unleashed.archive.document.FieldNotes;

import java.util.UUID;

public final class ArchiveRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<MenuType<?>> MenuRegister = DeferredRegister.create(Registries.MENU, Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents ComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final DeferredBlock<ArchiveBlock> Archive = BlockRegister.register("archive", () -> new ArchiveBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.5f, 6.0f).sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.BLOCK)
            .lightLevel(State -> State.getValue(ArchiveBlock.Half) == DoubleBlockHalf.UPPER ? 6 : 0)
            .isRedstoneConductor((State, World, Pos) -> false).isSuffocating((State, World, Pos) -> false).isViewBlocking((State, World, Pos) -> false)));

    public static final DeferredItem<BlockItem> ArchiveItem = ItemRegister.registerSimpleBlockItem("archive", Archive);
    public static final DeferredItem<SealedDocumentItem> SealedDocument = ItemRegister.register("sealed_document",
            () -> new SealedDocumentItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<AnomalyDocumentItem> OpenedDocument = ItemRegister.register("anomaly_document",
            () -> new AnomalyDocumentItem(new Item.Properties().stacksTo(1)));

    public static final DeferredBlock<DatabaseBlock> Database = BlockRegister.register("archive_database", () -> new DatabaseBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GRAY).requiresCorrectToolForDrops().strength(5.0f, 9.0f).sound(SoundType.NETHERITE_BLOCK).noOcclusion().pushReaction(PushReaction.BLOCK)
            .lightLevel(State -> State.getValue(DatabaseBlock.Half) == DoubleBlockHalf.UPPER ? 7 : 4)
            .isRedstoneConductor((State, World, Pos) -> false).isSuffocating((State, World, Pos) -> false).isViewBlocking((State, World, Pos) -> false)));
    public static final DeferredBlock<DatabaseTerminalBlock> DatabaseTerminal = BlockRegister.register("archive_database_terminal", () -> new DatabaseTerminalBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.0f, 6.0f).sound(SoundType.METAL).noOcclusion().lightLevel(State -> 6)
            .isRedstoneConductor((State, World, Pos) -> false).isSuffocating((State, World, Pos) -> false).isViewBlocking((State, World, Pos) -> false)));
    public static final DeferredItem<BlockItem> DatabaseItem = ItemRegister.registerSimpleBlockItem("archive_database", Database, new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> DatabaseTerminalItem = ItemRegister.registerSimpleBlockItem("archive_database_terminal", DatabaseTerminal);
    public static final DeferredItem<DatabaseCopyItem> DatabaseCopy = ItemRegister.register("archive_database_copy",
            () -> new DatabaseCopyItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ArchiveBlockEntity>> ArchiveEntity = BlockEntityRegister.register("archive",
            () -> BlockEntityType.Builder.of(ArchiveBlockEntity::new, Archive.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<ArchiveMenu>> ArchiveMenuType = MenuRegister.register("archive",
            () -> IMenuTypeExtension.create(ArchiveMenu::new));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DocumentStamp>> StampComponent = ComponentRegister.registerComponentType("archive_document",
            Builder -> Builder.persistent(DocumentStamp.StampCodec).networkSynchronized(DocumentStamp.StampStreamCodec));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FieldNotes>> NotesComponent = ComponentRegister.registerComponentType("archive_notes",
            Builder -> Builder.persistent(FieldNotes.NotesCodec).networkSynchronized(FieldNotes.NotesStreamCodec));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> ClaimantComponent = ComponentRegister.registerComponentType("archive_claimant",
            Builder -> Builder.persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC));

    public static final DeferredHolder<SoundEvent, SoundEvent> Boot = sound("archive_boot");
    public static final DeferredHolder<SoundEvent, SoundEvent> Keystroke = sound("archive_type");
    public static final DeferredHolder<SoundEvent, SoundEvent> Roll = sound("archive_roll");
    public static final DeferredHolder<SoundEvent, SoundEvent> Deal = sound("archive_deal");
    public static final DeferredHolder<SoundEvent, SoundEvent> StampSound = sound("archive_stamp");
    public static final DeferredHolder<SoundEvent, SoundEvent> Pick = sound("archive_pick");
    public static final DeferredHolder<SoundEvent, SoundEvent> Print = sound("archive_print");
    public static final DeferredHolder<SoundEvent, SoundEvent> Shred = sound("archive_shred");
    public static final DeferredHolder<SoundEvent, SoundEvent> Deny = sound("archive_deny");
    public static final DeferredHolder<SoundEvent, SoundEvent> Page = sound("archive_page");
    public static final DeferredHolder<SoundEvent, SoundEvent> Seal = sound("archive_seal");
    public static final DeferredHolder<SoundEvent, SoundEvent> StaticBurst = sound("archive_static");
    public static final DeferredHolder<SoundEvent, SoundEvent> DiscoverySting = sound("archive_discovery");
    public static final DeferredHolder<SoundEvent, SoundEvent> Omen = sound("archive_omen");
    public static final DeferredHolder<SoundEvent, SoundEvent> DatabaseUpload = sound("archive_database_upload");
    public static final DeferredHolder<SoundEvent, SoundEvent> DatabaseWhir = sound("archive_database_whir");
    public static final DeferredHolder<SoundEvent, SoundEvent> DatabasePrint = sound("archive_database_print");
    public static final DeferredHolder<SoundEvent, SoundEvent> DatabasePaper = sound("archive_database_paper");
    public static final DeferredHolder<SoundEvent, SoundEvent> DatabaseShred = sound("archive_database_shred");
    public static final DeferredHolder<SoundEvent, SoundEvent> DatabaseDeny = sound("archive_database_deny");
    public static final DeferredHolder<SoundEvent, SoundEvent> DatabaseOpen = sound("archive_database_open");

    private ArchiveRegistry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityRegister.register(ModEventBus);
        MenuRegister.register(ModEventBus);
        ComponentRegister.register(ModEventBus);
        SoundRegister.register(ModEventBus);
        ArchiveRules.register(ModEventBus);
    }
}
