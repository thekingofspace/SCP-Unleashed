package net.rai.scp_unleashed.reactor;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
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

public final class ReactorRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<MenuType<?>> MenuRegister = DeferredRegister.create(Registries.MENU, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents ComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GlobalPos>> ReactorLink = ComponentRegister.registerComponentType("reactor_link",
            Builder -> Builder.persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC));

    public static final DeferredBlock<FuelTowerBlock> FuelTower = BlockRegister.register("fuel_tower", () -> new FuelTowerBlock(towerProperties(MapColor.COLOR_GRAY)
            .lightLevel(FuelTowerBlock::lightOf)));
    public static final DeferredBlock<ControlRodTowerBlock> ControlRodTower = BlockRegister.register("control_rod_tower",
            () -> new ControlRodTowerBlock(towerProperties(MapColor.COLOR_YELLOW)));
    public static final DeferredBlock<CoolantTowerBlock> CoolantTower = BlockRegister.register("coolant_tower",
            () -> new CoolantTowerBlock(towerProperties(MapColor.COLOR_BLUE)));
    public static final DeferredBlock<SteamChannelBlock> SteamChannel = BlockRegister.register("steam_channel",
            () -> new SteamChannelBlock(towerProperties(MapColor.COLOR_ORANGE)));
    public static final DeferredBlock<BoosterTowerBlock> BoosterTower = BlockRegister.register("booster_tower",
            () -> new BoosterTowerBlock(towerProperties(MapColor.COLOR_CYAN).lightLevel(BoosterTowerBlock::lightOf)));
    public static final DeferredBlock<ReactorControllerBlock> Controller = BlockRegister.register("reactor_controller", () -> new ReactorControllerBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.5f, 6.0f).sound(SoundType.METAL).lightLevel(ReactorControllerBlock::lightOf)));
    public static final DeferredBlock<IrradiationChamberBlock> Chamber = BlockRegister.register("irradiation_chamber", () -> new IrradiationChamberBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(4.0f, 8.0f).sound(SoundType.METAL)
            .lightLevel(State -> State.getValue(IrradiationChamberBlock.Active) ? IrradiationChamberBlock.ActiveLight : 0)));

    public static final DeferredItem<BlockItem> FuelTowerItem = ItemRegister.register("fuel_tower", () -> new BlockItem(FuelTower.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<BlockItem> ControlRodTowerItem = ItemRegister.registerSimpleBlockItem("control_rod_tower", ControlRodTower);
    public static final DeferredItem<BlockItem> CoolantTowerItem = ItemRegister.registerSimpleBlockItem("coolant_tower", CoolantTower);
    public static final DeferredItem<BlockItem> SteamChannelItem = ItemRegister.registerSimpleBlockItem("steam_channel", SteamChannel);
    public static final DeferredItem<BlockItem> BoosterTowerItem = ItemRegister.register("booster_tower", () -> new BlockItem(BoosterTower.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<ReactorControllerItem> ControllerItem = ItemRegister.register("reactor_controller", () -> new ReactorControllerItem(Controller.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> ChamberItem = ItemRegister.registerSimpleBlockItem("irradiation_chamber", Chamber);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TowerSegmentBlockEntity>> SegmentEntity = BlockEntityRegister.register("reactor_tower",
            () -> BlockEntityType.Builder.of(TowerSegmentBlockEntity::new, FuelTower.get(), ControlRodTower.get(), CoolantTower.get(), SteamChannel.get(),
                    BoosterTower.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ReactorControllerBlockEntity>> ControllerEntity = BlockEntityRegister.register("reactor_controller",
            () -> BlockEntityType.Builder.of(ReactorControllerBlockEntity::new, Controller.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IrradiationChamberBlockEntity>> ChamberEntity = BlockEntityRegister.register("irradiation_chamber",
            () -> BlockEntityType.Builder.of(IrradiationChamberBlockEntity::new, Chamber.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<ReactorControllerMenu>> ControllerMenu = MenuRegister.register("reactor_controller",
            () -> new MenuType<>(ReactorControllerMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>, MenuType<IrradiationChamberMenu>> ChamberMenu = MenuRegister.register("irradiation_chamber",
            () -> new MenuType<>(IrradiationChamberMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<SoundEvent, SoundEvent> RodDrive = sound("fission_rod_drive");
    public static final DeferredHolder<SoundEvent, SoundEvent> TowerCreak = sound("fission_tower_creak");
    public static final DeferredHolder<SoundEvent, SoundEvent> TowerVent = sound("fission_tower_vent");
    public static final DeferredHolder<SoundEvent, SoundEvent> TowerBoil = sound("fission_tower_boil");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChamberHum = sound("fission_chamber_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> BoosterHum = sound("fission_booster_hum");

    private ReactorRegistry() {
    }

    private static BlockBehaviour.Properties towerProperties(MapColor Colour) {
        return BlockBehaviour.Properties.of().mapColor(Colour).requiresCorrectToolForDrops().strength(5.0f, 12.0f).sound(SoundType.NETHERITE_BLOCK)
                .pushReaction(PushReaction.BLOCK);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        ComponentRegister.register(ModEventBus);
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityRegister.register(ModEventBus);
        MenuRegister.register(ModEventBus);
        SoundRegister.register(ModEventBus);
    }
}
