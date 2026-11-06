package net.rai.scp_unleashed.guards;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;

public final class GuardsRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<MenuType<?>> MenuRegister = DeferredRegister.create(Registries.MENU, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents ComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Scp_unleashed.ModId);

    public static final DeferredBlock<PatrolNodeBlock> PatrolNode = BlockRegister.register("patrol_node", () -> new PatrolNodeBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).strength(1.5f, 6.0f).sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<PatrolManagerBlock> PatrolManager = BlockRegister.register("patrol_manager", () -> new PatrolManagerBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.5f, 6.0f).sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.BLOCK)
            .lightLevel(State -> 4)));

    public static final DeferredItem<BlockItem> PatrolNodeItem = ItemRegister.registerSimpleBlockItem("patrol_node", PatrolNode, new Item.Properties());
    public static final DeferredItem<BlockItem> PatrolManagerItem = ItemRegister.registerSimpleBlockItem("patrol_manager", PatrolManager, new Item.Properties());
    public static final DeferredItem<PatrolLinkerItem> PatrolLinker = ItemRegister.register("patrol_linker", () -> new PatrolLinkerItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<PatrolOrdersItem> PatrolOrders = ItemRegister.register("patrol_orders", () -> new PatrolOrdersItem(new Item.Properties().stacksTo(16)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PatrolManagerBlockEntity>> PatrolManagerEntity = BlockEntityTypeRegister.register("patrol_manager",
            () -> BlockEntityType.Builder.of(PatrolManagerBlockEntity::new, PatrolManager.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PatrolNodeBlockEntity>> PatrolNodeEntity = BlockEntityTypeRegister.register("patrol_node",
            () -> BlockEntityType.Builder.of(PatrolNodeBlockEntity::new, PatrolNode.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<PatrolManagerMenu>> PatrolManagerMenuType = MenuRegister.register("patrol_manager",
            () -> IMenuTypeExtension.create(PatrolManagerMenu::new));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PatrolRoute>> Route =
            ComponentRegister.registerComponentType("guards_route", Builder -> Builder.persistent(PatrolRoute.RouteCodec).networkSynchronized(PatrolRoute.RouteStreamCodec));

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<GuardOrders>> Orders = AttachmentRegister.register("guard_orders",
            () -> AttachmentType.builder(GuardOrders::blank).serialize(GuardOrders.OrdersCodec, Saved -> Saved.valid()).build());

    public static final DeferredHolder<SoundEvent, SoundEvent> LinkClick = sound("guards_link_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> LinkLock = sound("guards_link_lock");
    public static final DeferredHolder<SoundEvent, SoundEvent> LinkTone = sound("guards_link_tone");
    public static final DeferredHolder<SoundEvent, SoundEvent> UnlinkTone = sound("guards_unlink_tone");
    public static final DeferredHolder<SoundEvent, SoundEvent> OrdersWrite = sound("guards_orders_write");
    public static final DeferredHolder<SoundEvent, SoundEvent> OrdersStamp = sound("guards_orders_stamp");
    public static final DeferredHolder<SoundEvent, SoundEvent> OrdersHand = sound("guards_orders_hand");
    public static final DeferredHolder<SoundEvent, SoundEvent> OrdersClear = sound("guards_orders_clear");
    public static final DeferredHolder<SoundEvent, SoundEvent> AlertTone = sound("guards_alert_tone");
    public static final DeferredHolder<SoundEvent, SoundEvent> AlertKey = sound("guards_alert_key");
    public static final DeferredHolder<SoundEvent, SoundEvent> TerminalBoot = sound("guards_terminal_boot");

    private GuardsRegistry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        MenuRegister.register(ModEventBus);
        SoundRegister.register(ModEventBus);
        ComponentRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
    }
}
