package net.rai.scp_unleashed.scp143;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.StartConditions;

import java.util.Optional;
import java.util.function.Supplier;

public final class Scp143Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<MobEffect> MobEffectRegister = DeferredRegister.create(Registries.MOB_EFFECT, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_143");
    public static final ResourceKey<DamageType> PetalDamage = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp143_petal"));
    public static final ResourceKey<DamageType> BleedingDamage = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp143_bleeding"));

    public static final SoundType WoodSound = new SoundType(1.0f, 0.82f, SoundEvents.CHERRY_WOOD_BREAK, SoundEvents.CHERRY_WOOD_STEP, SoundEvents.CHERRY_WOOD_PLACE,
            SoundEvents.CHERRY_WOOD_HIT, SoundEvents.CHERRY_WOOD_FALL);
    public static final SoundType HangingSignSound = new SoundType(1.0f, 0.82f, SoundEvents.CHERRY_WOOD_HANGING_SIGN_BREAK, SoundEvents.CHERRY_WOOD_HANGING_SIGN_STEP,
            SoundEvents.CHERRY_WOOD_HANGING_SIGN_PLACE, SoundEvents.CHERRY_WOOD_HANGING_SIGN_HIT, SoundEvents.CHERRY_WOOD_HANGING_SIGN_FALL);
    public static final SoundType LeavesSound = new SoundType(1.0f, 1.12f, SoundEvents.CHERRY_LEAVES_BREAK, SoundEvents.CHERRY_LEAVES_STEP, SoundEvents.CHERRY_LEAVES_PLACE,
            SoundEvents.CHERRY_LEAVES_HIT, SoundEvents.CHERRY_LEAVES_FALL);
    public static final BlockSetType BladewoodSet = BlockSetType.register(new BlockSetType("scp_unleashed:scp143_bladewood", true, true, true,
            BlockSetType.PressurePlateSensitivity.EVERYTHING, WoodSound, SoundEvents.CHERRY_WOOD_DOOR_CLOSE, SoundEvents.CHERRY_WOOD_DOOR_OPEN,
            SoundEvents.CHERRY_WOOD_TRAPDOOR_CLOSE, SoundEvents.CHERRY_WOOD_TRAPDOOR_OPEN, SoundEvents.CHERRY_WOOD_PRESSURE_PLATE_CLICK_OFF,
            SoundEvents.CHERRY_WOOD_PRESSURE_PLATE_CLICK_ON, SoundEvents.CHERRY_WOOD_BUTTON_CLICK_OFF, SoundEvents.CHERRY_WOOD_BUTTON_CLICK_ON));
    public static final WoodType Bladewood = WoodType.register(new WoodType("scp_unleashed:scp143_bladewood", BladewoodSet, WoodSound, HangingSignSound,
            SoundEvents.CHERRY_WOOD_FENCE_GATE_CLOSE, SoundEvents.CHERRY_WOOD_FENCE_GATE_OPEN));
    public static final TreeGrower Grower = new TreeGrower("scp_unleashed:scp143_bladewood", Optional.empty(), Optional.empty(), Optional.empty());

    public static final float LogHardness = 3.0f;
    public static final float LogResistance = 6.0f;

    public static final DeferredBlock<Scp143LogBlock> Log = BlockRegister.register("scp143_log", () -> new Scp143LogBlock(log(MapColor.TERRACOTTA_BLACK, MapColor.COLOR_BLACK)));
    public static final DeferredBlock<Scp143LogBlock> Wood = BlockRegister.register("scp143_wood", () -> new Scp143LogBlock(log(MapColor.COLOR_BLACK, MapColor.COLOR_BLACK)));
    public static final DeferredBlock<Scp143LogBlock> StrippedLog = BlockRegister.register("scp143_stripped_log",
            () -> new Scp143LogBlock(log(MapColor.TERRACOTTA_BLACK, MapColor.TERRACOTTA_GRAY)));
    public static final DeferredBlock<Scp143LogBlock> StrippedWood = BlockRegister.register("scp143_stripped_wood",
            () -> new Scp143LogBlock(log(MapColor.TERRACOTTA_GRAY, MapColor.TERRACOTTA_GRAY)));
    public static final DeferredBlock<Block> Planks = BlockRegister.register("scp143_planks", () -> new Block(wood(MapColor.TERRACOTTA_BLACK).strength(LogHardness, LogResistance)));
    public static final DeferredBlock<StairBlock> Stairs = BlockRegister.register("scp143_stairs",
            () -> new StairBlock(Planks.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(Planks.get())));
    public static final DeferredBlock<SlabBlock> Slab = BlockRegister.register("scp143_slab", () -> new SlabBlock(wood(MapColor.TERRACOTTA_BLACK).strength(LogHardness, LogResistance)));
    public static final DeferredBlock<FenceBlock> Fence = BlockRegister.register("scp143_fence", () -> new FenceBlock(wood(MapColor.TERRACOTTA_BLACK).strength(LogHardness, LogResistance)));
    public static final DeferredBlock<FenceGateBlock> FenceGate = BlockRegister.register("scp143_fence_gate",
            () -> new FenceGateBlock(Bladewood, wood(MapColor.TERRACOTTA_BLACK).forceSolidOn().strength(LogHardness, LogResistance)));
    public static final DeferredBlock<DoorBlock> Door = BlockRegister.register("scp143_door",
            () -> new DoorBlock(BladewoodSet, wood(MapColor.TERRACOTTA_BLACK).strength(4.5f, LogResistance).noOcclusion().pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<TrapDoorBlock> Trapdoor = BlockRegister.register("scp143_trapdoor",
            () -> new TrapDoorBlock(BladewoodSet, wood(MapColor.TERRACOTTA_BLACK).strength(4.5f, LogResistance).noOcclusion().isValidSpawn((State, World, Pos, Kind) -> false)));
    public static final DeferredBlock<PressurePlateBlock> PressurePlate = BlockRegister.register("scp143_pressure_plate",
            () -> new PressurePlateBlock(BladewoodSet, wood(MapColor.TERRACOTTA_BLACK).forceSolidOn().noCollission().strength(0.75f, 1.0f).pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<ButtonBlock> Button = BlockRegister.register("scp143_button",
            () -> new ButtonBlock(BladewoodSet, 30, BlockBehaviour.Properties.of().noCollission().strength(0.75f, 1.0f).pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<StandingSignBlock> Sign = BlockRegister.register("scp143_sign",
            () -> new StandingSignBlock(Bladewood, sign(MapColor.TERRACOTTA_BLACK)));
    public static final DeferredBlock<WallSignBlock> WallSign = BlockRegister.register("scp143_wall_sign",
            () -> new WallSignBlock(Bladewood, sign(MapColor.TERRACOTTA_BLACK)));
    public static final DeferredBlock<CeilingHangingSignBlock> HangingSign = BlockRegister.register("scp143_hanging_sign",
            () -> new CeilingHangingSignBlock(Bladewood, sign(MapColor.TERRACOTTA_GRAY)));
    public static final DeferredBlock<WallHangingSignBlock> WallHangingSign = BlockRegister.register("scp143_wall_hanging_sign",
            () -> new WallHangingSignBlock(Bladewood, sign(MapColor.TERRACOTTA_GRAY)));
    public static final DeferredBlock<Scp143LeavesBlock> Leaves = BlockRegister.register("scp143_leaves", () -> new Scp143LeavesBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.TERRACOTTA_WHITE)
            .strength(0.3f)
            .randomTicks()
            .sound(LeavesSound)
            .noOcclusion()
            .isValidSpawn((State, World, Pos, Kind) -> false)
            .isSuffocating((State, World, Pos) -> false)
            .isViewBlocking((State, World, Pos) -> false)
            .ignitedByLava()
            .pushReaction(PushReaction.DESTROY)
            .isRedstoneConductor((State, World, Pos) -> false)));
    public static final DeferredBlock<Scp143SaplingBlock> Sapling = BlockRegister.register("scp143_sapling", () -> new Scp143SaplingBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.TERRACOTTA_WHITE)
            .noCollission()
            .randomTicks()
            .instabreak()
            .sound(SoundType.CHERRY_SAPLING)
            .pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<FlowerPotBlock> PottedSapling = BlockRegister.register("scp143_potted_sapling",
            () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, Sapling, BlockBehaviour.Properties.of().instabreak().noOcclusion().pushReaction(PushReaction.DESTROY)));

    public static final DeferredItem<Scp143BlockItem> LogItem = ItemRegister.register("scp143_log", () -> new Scp143BlockItem(Log.get(), new Item.Properties(), "block.scp_unleashed.scp143_log"));
    public static final DeferredItem<BlockItem> WoodItem = blockItem("scp143_wood", Wood);
    public static final DeferredItem<BlockItem> StrippedLogItem = blockItem("scp143_stripped_log", StrippedLog);
    public static final DeferredItem<BlockItem> StrippedWoodItem = blockItem("scp143_stripped_wood", StrippedWood);
    public static final DeferredItem<BlockItem> PlanksItem = blockItem("scp143_planks", Planks);
    public static final DeferredItem<BlockItem> StairsItem = blockItem("scp143_stairs", Stairs);
    public static final DeferredItem<BlockItem> SlabItem = blockItem("scp143_slab", Slab);
    public static final DeferredItem<BlockItem> FenceItem = blockItem("scp143_fence", Fence);
    public static final DeferredItem<BlockItem> FenceGateItem = blockItem("scp143_fence_gate", FenceGate);
    public static final DeferredItem<DoubleHighBlockItem> DoorItem = ItemRegister.register("scp143_door", () -> new DoubleHighBlockItem(Door.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> TrapdoorItem = blockItem("scp143_trapdoor", Trapdoor);
    public static final DeferredItem<BlockItem> PressurePlateItem = blockItem("scp143_pressure_plate", PressurePlate);
    public static final DeferredItem<BlockItem> ButtonItem = blockItem("scp143_button", Button);
    public static final DeferredItem<SignItem> BladewoodSign = ItemRegister.register("scp143_sign", () -> new SignItem(new Item.Properties().stacksTo(16), Sign.get(), WallSign.get()));
    public static final DeferredItem<HangingSignItem> BladewoodHangingSign = ItemRegister.register("scp143_hanging_sign",
            () -> new HangingSignItem(HangingSign.get(), WallHangingSign.get(), new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Scp143BlockItem> LeavesItem = ItemRegister.register("scp143_leaves",
            () -> new Scp143BlockItem(Leaves.get(), new Item.Properties(), "block.scp_unleashed.scp143_leaves"));
    public static final DeferredItem<Scp143BlockItem> SaplingItem = ItemRegister.register("scp143_sapling",
            () -> new Scp143BlockItem(Sapling.get(), new Item.Properties(), "block.scp_unleashed.scp143_sapling"));
    public static final DeferredItem<Scp143BoatItem> BladewoodBoat = ItemRegister.register("scp143_boat", () -> new Scp143BoatItem(false, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Scp143BoatItem> BladewoodChestBoat = ItemRegister.register("scp143_chest_boat", () -> new Scp143BoatItem(true, new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp143Boat>> BoatType = EntityTypeRegister.register("scp143_boat",
            () -> EntityType.Builder.<Scp143Boat>of(Scp143Boat::new, MobCategory.MISC).sized(1.375f, 0.5625f).eyeHeight(0.5625f).clientTrackingRange(10).build("scp143_boat"));
    public static final DeferredHolder<EntityType<?>, EntityType<Scp143ChestBoat>> ChestBoatType = EntityTypeRegister.register("scp143_chest_boat",
            () -> EntityType.Builder.<Scp143ChestBoat>of(Scp143ChestBoat::new, MobCategory.MISC).sized(1.375f, 0.5625f).eyeHeight(0.5625f).clientTrackingRange(10)
                    .build("scp143_chest_boat"));

    public static final DeferredHolder<MobEffect, Scp143BleedingEffect> Bleeding = MobEffectRegister.register("scp143_bleeding", Scp143BleedingEffect::new);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Petal = ParticleRegister.register("scp143_petal", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Drip = ParticleRegister.register("scp143_drip", () -> new SimpleParticleType(false));

    public static final DeferredHolder<SoundEvent, SoundEvent> Rustle = sound("scp143_rustle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Chime = sound("scp143_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> Slice = sound("scp143_slice");
    public static final DeferredHolder<SoundEvent, SoundEvent> DripSound = sound("scp143_drip");
    public static final DeferredHolder<SoundEvent, SoundEvent> Grow = sound("scp143_grow");

    private Scp143Registry() {
    }

    private static BlockBehaviour.Properties log(MapColor Top, MapColor Side) {
        return BlockBehaviour.Properties.of()
                .mapColor(State -> State.getValue(RotatedPillarBlock.AXIS) == net.minecraft.core.Direction.Axis.Y ? Top : Side)
                .instrument(NoteBlockInstrument.BASS)
                .strength(LogHardness, LogResistance)
                .sound(WoodSound)
                .ignitedByLava();
    }

    private static BlockBehaviour.Properties wood(MapColor Colour) {
        return BlockBehaviour.Properties.of().mapColor(Colour).instrument(NoteBlockInstrument.BASS).sound(WoodSound).ignitedByLava();
    }

    private static BlockBehaviour.Properties sign(MapColor Colour) {
        return BlockBehaviour.Properties.of().mapColor(Colour).forceSolidOn().instrument(NoteBlockInstrument.BASS).noCollission().strength(1.5f, 2.0f).ignitedByLava();
    }

    private static <T extends Block> DeferredItem<BlockItem> blockItem(String Name, Supplier<T> Placed) {
        return ItemRegister.register(Name, () -> new BlockItem(Placed.get(), new Item.Properties()));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static Block strippedOf(Block Original) {
        if (Original == Log.get()) {
            return StrippedLog.get();
        }
        if (Original == Wood.get()) {
            return StrippedWood.get();
        }
        return null;
    }

    public static boolean isBladewood(Block Found) {
        return Found == Log.get() || Found == Wood.get() || Found == StrippedLog.get() || Found == StrippedWood.get();
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        MobEffectRegister.register(ModEventBus);
        StartConditions.register(Scp143GroveCondition.TypeName, Scp143GroveCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp143Sites::confirm);
    }
}
