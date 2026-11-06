package net.rai.scp_unleashed.steam;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
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

public final class SteamRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<MenuType<?>> MenuRegister = DeferredRegister.create(Registries.MENU, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);

    public static final DeferredBlock<WaterBoilerBlock> WaterBoiler = BlockRegister.register("water_boiler", () -> new WaterBoilerBlock(machine()
            .lightLevel(WaterBoilerBlock::lightOf)));
    public static final DeferredItem<BlockItem> WaterBoilerItem = ItemRegister.registerSimpleBlockItem("water_boiler", WaterBoiler);
    public static final DeferredBlock<SteamTurbineBlock> SteamTurbine = BlockRegister.register("steam_turbine", () -> new SteamTurbineBlock(machine()));
    public static final DeferredItem<BlockItem> SteamTurbineItem = ItemRegister.registerSimpleBlockItem("steam_turbine", SteamTurbine);
    public static final DeferredBlock<IndustrialTurbineBlock> IndustrialTurbine = BlockRegister.register("industrial_turbine", () -> new IndustrialTurbineBlock(machine()));
    public static final DeferredItem<BlockItem> IndustrialTurbineItem = ItemRegister.registerSimpleBlockItem("industrial_turbine", IndustrialTurbine);
    public static final DeferredBlock<CoolingTowerBlock> CoolingTower = BlockRegister.register("cooling_tower", () -> new CoolingTowerBlock(machine()
            .mapColor(MapColor.STONE).sound(SoundType.STONE)));
    public static final DeferredItem<BlockItem> CoolingTowerItem = ItemRegister.registerSimpleBlockItem("cooling_tower", CoolingTower);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WaterBoilerBlockEntity>> WaterBoilerEntity = BlockEntityRegister.register("water_boiler",
            () -> BlockEntityType.Builder.of(WaterBoilerBlockEntity::new, WaterBoiler.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SteamTurbineBlockEntity>> SteamTurbineEntity = BlockEntityRegister.register("steam_turbine",
            () -> BlockEntityType.Builder.of(SteamTurbineBlockEntity::new, SteamTurbine.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IndustrialTurbineBlockEntity>> IndustrialTurbineEntity = BlockEntityRegister.register("industrial_turbine",
            () -> BlockEntityType.Builder.of(IndustrialTurbineBlockEntity::new, IndustrialTurbine.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CoolingTowerBlockEntity>> CoolingTowerEntity = BlockEntityRegister.register("cooling_tower",
            () -> BlockEntityType.Builder.of(CoolingTowerBlockEntity::new, CoolingTower.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<WaterBoilerMenu>> WaterBoilerMenuType = MenuRegister.register("water_boiler",
            () -> new MenuType<>(WaterBoilerMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SteamPlume = ParticleRegister.register("steam_plume", () -> new SimpleParticleType(false));

    public static final DeferredHolder<SoundEvent, SoundEvent> BoilerBoil = sound("water_boiler_boil");
    public static final DeferredHolder<SoundEvent, SoundEvent> BoilerFire = sound("water_boiler_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> BoilerValve = sound("water_boiler_valve");
    public static final DeferredHolder<SoundEvent, SoundEvent> TurbineWhine = sound("steam_turbine_whine");
    public static final DeferredHolder<SoundEvent, SoundEvent> TurbineRush = sound("steam_turbine_rush");
    public static final DeferredHolder<SoundEvent, SoundEvent> TurbineRumble = sound("steam_turbine_rumble");
    public static final DeferredHolder<SoundEvent, SoundEvent> IndustrialTurbineHum = sound("industrial_turbine_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> IndustrialTurbineRush = sound("industrial_turbine_rush");
    public static final DeferredHolder<SoundEvent, SoundEvent> IndustrialTurbineRumble = sound("industrial_turbine_rumble");
    public static final DeferredHolder<SoundEvent, SoundEvent> IndustrialTurbineDrone = sound("industrial_turbine_drone");
    public static final DeferredHolder<SoundEvent, SoundEvent> TowerRain = sound("cooling_tower_rain");

    private SteamRegistry() {
    }

    private static BlockBehaviour.Properties machine() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.5f, 6.0f).sound(SoundType.METAL).noOcclusion()
                .pushReaction(PushReaction.BLOCK).isValidSpawn((State, World, Pos, Kind) -> false).isRedstoneConductor((State, World, Pos) -> false)
                .isSuffocating((State, World, Pos) -> false).isViewBlocking((State, World, Pos) -> false);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityRegister.register(ModEventBus);
        MenuRegister.register(ModEventBus);
        SoundRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
    }
}
