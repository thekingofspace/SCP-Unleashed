package net.rai.scp_unleashed.rocketry;

import com.mojang.serialization.Codec;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
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
import net.rai.scp_unleashed.rocketry.platform.LaunchPlatformKind;
import net.rai.scp_unleashed.rocketry.platform.LaunchPlatformMenu;
import net.rai.scp_unleashed.rocketry.platform.PortableLaunchPadBlock;
import net.rai.scp_unleashed.rocketry.platform.RocketPlatformBlock;
import net.rai.scp_unleashed.rocketry.platform.RocketPlatformBlockEntity;
import net.rai.scp_unleashed.rocketry.platform.SatellitePlatformBlock;
import net.rai.scp_unleashed.rocketry.platform.SatellitePlatformBlockEntity;
import net.rai.scp_unleashed.rocketry.rocket.DropPodEntity;
import net.rai.scp_unleashed.rocketry.rocket.LaunchRecord;
import net.rai.scp_unleashed.rocketry.rocket.RocketEntity;
import net.rai.scp_unleashed.rocketry.rocket.RocketItem;
import net.rai.scp_unleashed.rocketry.satellite.SatelliteBoosterEntity;
import net.rai.scp_unleashed.rocketry.satellite.SatelliteItem;

import java.util.List;

public final class RocketryRegistry {
    public static final ResourceKey<Level> Moon = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("scp_unleashed", "moon"));
    public static final TagKey<EntityType<?>> PressurizedVehicles = TagKey.create(Registries.ENTITY_TYPE, id("pressurized_vehicles"));

    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<MenuType<?>> MenuRegister = DeferredRegister.create(Registries.MENU, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents ComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> RocketFuel = ComponentRegister.registerComponentType("rocket_fuel",
            Builder -> Builder.persistent(Codec.intRange(0, Integer.MAX_VALUE)).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GlobalPos>> ReturnSite = ComponentRegister.registerComponentType("rocket_return_site",
            Builder -> Builder.persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC));

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<LaunchRecord>> LastLaunch = AttachmentRegister.register("rocketry_last_launch",
            () -> AttachmentType.builder(() -> LaunchRecord.None).serialize(LaunchRecord.RecordCodec, Saved -> Saved.Site().isPresent()).copyOnDeath().build());

    public static final DeferredBlock<RocketPlatformBlock> RocketPlatform = BlockRegister.register("rocket_platform", () -> new RocketPlatformBlock(machine(MapColor.METAL)
            .lightLevel(RocketPlatformBlock::lightOf)));
    public static final DeferredItem<BlockItem> RocketPlatformItem = ItemRegister.register("rocket_platform", () -> new BlockItem(RocketPlatform.get(),
            new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredBlock<SatellitePlatformBlock> SatellitePlatform = BlockRegister.register("satellite_platform", () -> new SatellitePlatformBlock(machine(MapColor.METAL)
            .lightLevel(SatellitePlatformBlock::lightOf)));
    public static final DeferredItem<BlockItem> SatellitePlatformItem = ItemRegister.register("satellite_platform", () -> new BlockItem(SatellitePlatform.get(),
            new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredBlock<PortableLaunchPadBlock> PortableLaunchPad = BlockRegister.register("portable_launch_pad", () -> new PortableLaunchPadBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GRAY).requiresCorrectToolForDrops().strength(2.5f, 6.0f).sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.BLOCK)));
    public static final DeferredItem<BlockItem> PortableLaunchPadItem = ItemRegister.register("portable_launch_pad", () -> new BlockItem(PortableLaunchPad.get(),
            new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<Item> RocketEngine = ItemRegister.register("rocket_engine", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<RocketItem> Rocket = ItemRegister.register("rocket", () -> new RocketItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
            .fireResistant()));
    public static final DeferredItem<SatelliteItem> Satellite = ItemRegister.register("satellite", () -> new SatelliteItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RocketPlatformBlockEntity>> RocketPlatformEntity = BlockEntityRegister.register("rocket_platform",
            () -> BlockEntityType.Builder.of(RocketPlatformBlockEntity::new, RocketPlatform.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SatellitePlatformBlockEntity>> SatellitePlatformEntity = BlockEntityRegister.register("satellite_platform",
            () -> BlockEntityType.Builder.of(SatellitePlatformBlockEntity::new, SatellitePlatform.get()).build(null));

    public static final DeferredHolder<EntityType<?>, EntityType<RocketEntity>> RocketType = EntityTypeRegister.register("rocket",
            () -> EntityType.Builder.<RocketEntity>of(RocketEntity::new, MobCategory.MISC)
                    .sized(RocketEntity.Width, RocketEntity.Height)
                    .eyeHeight(RocketEntity.Height * 0.5f)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .fireImmune()
                    .build("rocket"));
    public static final DeferredHolder<EntityType<?>, EntityType<DropPodEntity>> DropPodType = EntityTypeRegister.register("drop_pod",
            () -> EntityType.Builder.<DropPodEntity>of(DropPodEntity::new, MobCategory.MISC)
                    .sized(DropPodEntity.Width, DropPodEntity.Height)
                    .eyeHeight(DropPodEntity.Height * 0.5f)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .fireImmune()
                    .build("drop_pod"));
    public static final DeferredHolder<EntityType<?>, EntityType<SatelliteBoosterEntity>> SatelliteBoosterType = EntityTypeRegister.register("satellite_booster",
            () -> EntityType.Builder.<SatelliteBoosterEntity>of(SatelliteBoosterEntity::new, MobCategory.MISC)
                    .sized(SatelliteBoosterEntity.Width, SatelliteBoosterEntity.Height)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .fireImmune()
                    .build("satellite_booster"));

    public static final DeferredHolder<MenuType<?>, MenuType<LaunchPlatformMenu>> RocketPlatformMenu = menu(LaunchPlatformKind.Rocket);
    public static final DeferredHolder<MenuType<?>, MenuType<LaunchPlatformMenu>> SatellitePlatformMenu = menu(LaunchPlatformKind.Satellite);

    public static final DeferredHolder<SoundEvent, SoundEvent> Ignition = sound("rocketry_ignition");
    public static final DeferredHolder<SoundEvent, SoundEvent> IgnitionHiss = sound("rocketry_ignition_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> Liftoff = sound("rocketry_liftoff");
    public static final DeferredHolder<SoundEvent, SoundEvent> LiftoffThunder = sound("rocketry_liftoff_thunder");
    public static final DeferredHolder<SoundEvent, SoundEvent> LiftoffDeep = sound("rocketry_liftoff_deep");
    public static final DeferredHolder<SoundEvent, SoundEvent> Roar = sound("rocketry_roar");
    public static final DeferredHolder<SoundEvent, SoundEvent> RoarFire = sound("rocketry_roar_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> RoarRumble = sound("rocketry_roar_rumble");
    public static final DeferredHolder<SoundEvent, SoundEvent> Countdown = sound("rocketry_countdown");
    public static final DeferredHolder<SoundEvent, SoundEvent> Clamps = sound("rocketry_clamps");
    public static final DeferredHolder<SoundEvent, SoundEvent> Refused = sound("rocketry_refused");
    public static final DeferredHolder<SoundEvent, SoundEvent> Board = sound("rocketry_board");
    public static final DeferredHolder<SoundEvent, SoundEvent> Thruster = sound("rocketry_thruster");
    public static final DeferredHolder<SoundEvent, SoundEvent> ThrusterHiss = sound("rocketry_thruster_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> Reentry = sound("rocketry_reentry");
    public static final DeferredHolder<SoundEvent, SoundEvent> PodLand = sound("rocketry_pod_land");
    public static final DeferredHolder<SoundEvent, SoundEvent> PodLandCrunch = sound("rocketry_pod_land_crunch");
    public static final DeferredHolder<SoundEvent, SoundEvent> PodOpen = sound("rocketry_pod_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> PodBreak = sound("rocketry_pod_break");
    public static final DeferredHolder<SoundEvent, SoundEvent> BoosterLaunch = sound("rocketry_booster_launch");
    public static final DeferredHolder<SoundEvent, SoundEvent> BoosterBurn = sound("rocketry_booster_burn");
    public static final DeferredHolder<SoundEvent, SoundEvent> Deployed = sound("rocketry_satellite_deployed");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> IgnitionLayers = List.of(Ignition, IgnitionHiss);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> LiftoffLayers = List.of(Liftoff, LiftoffThunder, LiftoffDeep);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> LandLayers = List.of(PodLand, PodLandCrunch);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BoosterLayers = List.of(BoosterLaunch, IgnitionHiss);

    private RocketryRegistry() {
    }

    public static ResourceLocation id(String Name) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name);
    }

    private static BlockBehaviour.Properties machine(MapColor Colour) {
        return BlockBehaviour.Properties.of().mapColor(Colour).requiresCorrectToolForDrops().strength(3.5f, 6.0f).sound(SoundType.METAL).noOcclusion()
                .pushReaction(PushReaction.BLOCK);
    }

    private static DeferredHolder<MenuType<?>, MenuType<LaunchPlatformMenu>> menu(LaunchPlatformKind Kind) {
        return MenuRegister.register(Kind.getName(), () -> new MenuType<>((ContainerId, PlayerInventory) -> new LaunchPlatformMenu(Kind, ContainerId, PlayerInventory),
                FeatureFlags.DEFAULT_FLAGS));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(id(Name)));
    }

    public static void register(IEventBus ModEventBus) {
        ComponentRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        MenuRegister.register(ModEventBus);
        SoundRegister.register(ModEventBus);
    }
}
