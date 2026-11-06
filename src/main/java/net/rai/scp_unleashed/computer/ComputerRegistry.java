package net.rai.scp_unleashed.computer;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.computer.data.FloppyContents;

public final class ComputerRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents ComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);

    public static final DeferredBlock<ComputerBlock> Computer = BlockRegister.register("computer", () -> new ComputerBlock(device(6)));
    public static final DeferredBlock<ChatServerBlock> ChatServer = BlockRegister.register("chat_server", () -> new ChatServerBlock(device(4)));
    public static final DeferredBlock<MessageDatabaseBlock> MessageDatabase = BlockRegister.register("message_database", () -> new MessageDatabaseBlock(device(3)));
    public static final DeferredBlock<UploadPortBlock> UploadPort = BlockRegister.register("network_upload_port", () -> new UploadPortBlock(device(3)));

    public static final DeferredItem<BlockItem> ComputerItem = ItemRegister.registerSimpleBlockItem("computer", Computer);
    public static final DeferredItem<BlockItem> ChatServerItem = ItemRegister.registerSimpleBlockItem("chat_server", ChatServer);
    public static final DeferredItem<BlockItem> MessageDatabaseItem = ItemRegister.registerSimpleBlockItem("message_database", MessageDatabase);
    public static final DeferredItem<BlockItem> UploadPortItem = ItemRegister.registerSimpleBlockItem("network_upload_port", UploadPort);
    public static final DeferredItem<FloppyDiscItem> FloppyDisc = ItemRegister.register("floppy_disc", () -> new FloppyDiscItem(new Item.Properties().stacksTo(16)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ComputerBlockEntity>> ComputerEntity = BlockEntityTypeRegister.register("computer",
            () -> BlockEntityType.Builder.of(ComputerBlockEntity::new, Computer.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChatServerBlockEntity>> ChatServerEntity = BlockEntityTypeRegister.register("chat_server",
            () -> BlockEntityType.Builder.of(ChatServerBlockEntity::new, ChatServer.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MessageDatabaseBlockEntity>> MessageDatabaseEntity = BlockEntityTypeRegister.register("message_database",
            () -> BlockEntityType.Builder.of(MessageDatabaseBlockEntity::new, MessageDatabase.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<UploadPortBlockEntity>> UploadPortEntity = BlockEntityTypeRegister.register("network_upload_port",
            () -> BlockEntityType.Builder.of(UploadPortBlockEntity::new, UploadPort.get()).build(null));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FloppyContents>> FloppyComponent = ComponentRegister.registerComponentType("computer_floppy",
            Builder -> Builder.persistent(FloppyContents.ContentsCodec).networkSynchronized(FloppyContents.ContentsStreamCodec));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CustomData>> StorageComponent = ComponentRegister.registerComponentType("computer_storage",
            Builder -> Builder.persistent(CustomData.CODEC).networkSynchronized(CustomData.STREAM_CODEC));

    public static final DeferredHolder<SoundEvent, SoundEvent> Key = sound("computer_key");
    public static final DeferredHolder<SoundEvent, SoundEvent> Beep = sound("computer_beep");
    public static final DeferredHolder<SoundEvent, SoundEvent> Error = sound("computer_error");
    public static final DeferredHolder<SoundEvent, SoundEvent> Boot = sound("computer_boot");
    public static final DeferredHolder<SoundEvent, SoundEvent> Shutdown = sound("computer_shutdown");
    public static final DeferredHolder<SoundEvent, SoundEvent> DiskInsert = sound("computer_disk_insert");
    public static final DeferredHolder<SoundEvent, SoundEvent> DiskEject = sound("computer_disk_eject");
    public static final DeferredHolder<SoundEvent, SoundEvent> DiskSeek = sound("computer_disk_seek");
    public static final DeferredHolder<SoundEvent, SoundEvent> Message = sound("computer_message");
    public static final DeferredHolder<SoundEvent, SoundEvent> Toggle = sound("computer_toggle");

    private ComputerRegistry() {
    }

    private static BlockBehaviour.Properties device(int Light) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.0f, 6.0f).sound(SoundType.METAL).noOcclusion()
                .lightLevel(State -> ComputerDeviceBlock.lightOf(State, Light)).isRedstoneConductor((State, World, Pos) -> false)
                .isSuffocating((State, World, Pos) -> false).isViewBlocking((State, World, Pos) -> false);
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
    }
}
