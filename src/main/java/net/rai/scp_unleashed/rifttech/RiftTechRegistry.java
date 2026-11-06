package net.rai.scp_unleashed.rifttech;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
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
import net.rai.scp_unleashed.item.UpgradeItem;
import net.rai.scp_unleashed.upgrade.UpgradeType;

public final class RiftTechRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<MenuType<?>> MenuRegister = DeferredRegister.create(Registries.MENU, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final DeferredBlock<RiftCollectorBlock> RiftCollector = BlockRegister.register("rift_collector", () -> new RiftCollectorBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(5.0f, 18.0f).sound(SoundType.NETHERITE_BLOCK).noOcclusion().pushReaction(PushReaction.BLOCK)
            .lightLevel(RiftCollectorBlock::lightOf)));
    public static final DeferredItem<BlockItem> RiftCollectorItem = ItemRegister.registerSimpleBlockItem("rift_collector", RiftCollector,
            new Item.Properties().rarity(Rarity.RARE));
    public static final DeferredBlock<InstabilityPylonBlock> InstabilityPylon = BlockRegister.register("instability_pylon", () -> new InstabilityPylonBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(6.0f, 24.0f).sound(SoundType.NETHERITE_BLOCK).noOcclusion().pushReaction(PushReaction.BLOCK)
            .lightLevel(InstabilityPylonBlock::lightOf)));
    public static final DeferredItem<BlockItem> InstabilityPylonItem = ItemRegister.registerSimpleBlockItem("instability_pylon", InstabilityPylon,
            new Item.Properties().rarity(Rarity.RARE));
    public static final DeferredItem<ApocalypticFragmentItem> ApocalypticFragment = ItemRegister.register("apocalyptic_rift_fragment",
            () -> new ApocalypticFragmentItem(new Item.Properties().stacksTo(ApocalypticFragmentItem.StackSize).rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredItem<UpgradeItem> RangeUpgrade = ItemRegister.register("range_upgrade",
            () -> new UpgradeItem(UpgradeType.Range, new Item.Properties().stacksTo(16)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RiftCollectorBlockEntity>> RiftCollectorEntity = BlockEntityRegister.register("rift_collector",
            () -> BlockEntityType.Builder.of(RiftCollectorBlockEntity::new, RiftCollector.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InstabilityPylonBlockEntity>> InstabilityPylonEntity = BlockEntityRegister.register("instability_pylon",
            () -> BlockEntityType.Builder.of(InstabilityPylonBlockEntity::new, InstabilityPylon.get()).build(null));

    public static final DeferredHolder<EntityType<?>, EntityType<ApocalypticRiftEntity>> ApocalypticRift = EntityRegister.register("apocalyptic_rift",
            () -> EntityType.Builder.<ApocalypticRiftEntity>of(ApocalypticRiftEntity::new, MobCategory.MISC)
                    .sized(1.2f, ApocalypticRiftEntity.Height)
                    .fireImmune()
                    .clientTrackingRange(12)
                    .updateInterval(20)
                    .build("apocalyptic_rift"));

    public static final DeferredHolder<MenuType<?>, MenuType<RiftCollectorMenu>> RiftCollectorMenuType = MenuRegister.register("rift_collector",
            () -> new MenuType<>(RiftCollectorMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>, MenuType<PylonMenu>> PylonMenuType = MenuRegister.register("instability_pylon",
            () -> new MenuType<>(PylonMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<SoundEvent, SoundEvent> CollectorServo = sound("rift_collector_servo");
    public static final DeferredHolder<SoundEvent, SoundEvent> CollectorServoClick = sound("rift_collector_servo_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> CollectorBeam = sound("rift_collector_beam");
    public static final DeferredHolder<SoundEvent, SoundEvent> CollectorBeamBed = sound("rift_collector_beam_bed");
    public static final DeferredHolder<SoundEvent, SoundEvent> CollectorLock = sound("rift_collector_lock");
    public static final DeferredHolder<SoundEvent, SoundEvent> PylonHum = sound("instability_pylon_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> PylonHumBed = sound("instability_pylon_hum_bed");
    public static final DeferredHolder<SoundEvent, SoundEvent> PylonArc = sound("instability_pylon_arc");
    public static final DeferredHolder<SoundEvent, SoundEvent> PylonArcSnap = sound("instability_pylon_arc_snap");
    public static final DeferredHolder<SoundEvent, SoundEvent> PylonLink = sound("instability_pylon_link");
    public static final DeferredHolder<SoundEvent, SoundEvent> PylonUnlink = sound("instability_pylon_unlink");
    public static final DeferredHolder<SoundEvent, SoundEvent> ApocalypticDrone = sound("apocalyptic_drone");
    public static final DeferredHolder<SoundEvent, SoundEvent> ApocalypticDroneBed = sound("apocalyptic_drone_bed");
    public static final DeferredHolder<SoundEvent, SoundEvent> ApocalypticBoom = sound("apocalyptic_boom");
    public static final DeferredHolder<SoundEvent, SoundEvent> ApocalypticBoomTail = sound("apocalyptic_boom_tail");
    public static final DeferredHolder<SoundEvent, SoundEvent> ApocalypticBoomPortal = sound("apocalyptic_boom_portal");
    public static final DeferredHolder<SoundEvent, SoundEvent> GoldenStrike = sound("apocalyptic_strike");
    public static final DeferredHolder<SoundEvent, SoundEvent> GoldenStrikeCrack = sound("apocalyptic_strike_crack");
    public static final DeferredHolder<SoundEvent, SoundEvent> GoldenStrikeImpact = sound("apocalyptic_strike_impact");

    private RiftTechRegistry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityRegister.register(ModEventBus);
        EntityRegister.register(ModEventBus);
        MenuRegister.register(ModEventBus);
        SoundRegister.register(ModEventBus);
    }
}
