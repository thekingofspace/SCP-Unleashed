package net.rai.scp_unleashed.scp1730;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.gateway.GatewayDestinations;
import net.rai.scp_unleashed.scp1730.block.RealityFolderBlock;
import net.rai.scp_unleashed.scp1730.block.RealityFolderBlockEntity;
import net.rai.scp_unleashed.scp1730.block.Scp1730CardBlock;
import net.rai.scp_unleashed.scp1730.block.Scp1730ControlBlock;
import net.rai.scp_unleashed.scp1730.block.Scp1730CrackBlock;
import net.rai.scp_unleashed.scp1730.block.Scp1730CrackBlockEntity;
import net.rai.scp_unleashed.scp1730.block.Scp1730Decor;
import net.rai.scp_unleashed.scp1730.block.Scp1730DecorBlock;
import net.rai.scp_unleashed.scp1730.block.Scp1730LaserBlock;
import net.rai.scp_unleashed.scp1730.block.Scp1730LaserBlockEntity;
import net.rai.scp_unleashed.scp1730.block.Scp1730SealBlock;
import net.rai.scp_unleashed.scp1730.block.Scp1730SymbolBlock;
import net.rai.scp_unleashed.scp1730.block.Scp1730SymbolBlockEntity;
import net.rai.scp_unleashed.scp1730.block.Scp1730ThresherBlock;
import net.rai.scp_unleashed.scp1730.block.Scp1730ThresherBlockEntity;
import net.rai.scp_unleashed.scp1730.block.Scp1730TurretBlock;
import net.rai.scp_unleashed.scp1730.block.Scp1730TurretBlockEntity;
import net.rai.scp_unleashed.scp1730.entity.RealityAnchorEntity;
import net.rai.scp_unleashed.scp1730.entity.RealityAnchorField;
import net.rai.scp_unleashed.scp1730.entity.Scp1730Guardian;
import net.rai.scp_unleashed.scp1730.entity.Scp1730Leech;
import net.rai.scp_unleashed.scp1730.entity.Scp1730Painter;
import net.rai.scp_unleashed.scp1730.entity.Scp1730Tentacle;
import net.rai.scp_unleashed.scp1730.item.RealityAnchorItem;
import net.rai.scp_unleashed.scp1730.item.Scp1730CardItem;
import net.rai.scp_unleashed.scp1730.item.Scp1730CoreItem;
import net.rai.scp_unleashed.scp1730.item.Scp1730FragmentItem;
import net.rai.scp_unleashed.scp1730.world.Scp1730ChunkGenerator;
import net.rai.scp_unleashed.stasis.StasisPods;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class Scp1730Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> GeneratorRegister = DeferredRegister.create(Registries.CHUNK_GENERATOR, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_1730");
    private static final List<DeferredItem<? extends Item>> FacilityItems = new ArrayList<>();
    private static final Map<Scp1730Decor, DeferredBlock<Block>> Decor = new EnumMap<>(Scp1730Decor.class);

    public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<Scp1730ChunkGenerator>> Generator = GeneratorRegister.register("scp1730_facility",
            () -> Scp1730ChunkGenerator.GeneratorCodec);

    public static final DeferredBlock<Scp1730SymbolBlock> Symbol = BlockRegister.register("scp1730_symbol",
            () -> new Scp1730SymbolBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).noCollission().noOcclusion().replaceable().strength(0.6f)
                    .sound(SoundType.WOOL).pushReaction(PushReaction.DESTROY).noLootTable().isValidSpawn((State, World, Pos, Kind) -> false)));
    public static final DeferredBlock<Scp1730CrackBlock> Crack = BlockRegister.register("scp1730_crack",
            () -> new Scp1730CrackBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(-1.0f, 3600000.0f).noLootTable()
                    .sound(SoundType.DEEPSLATE).pushReaction(PushReaction.BLOCK).isValidSpawn((State, World, Pos, Kind) -> false)));
    public static final DeferredBlock<Block> PitVoid = BlockRegister.register("scp1730_pit_void",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(-1.0f, 3600000.0f).noLootTable().sound(SoundType.DEEPSLATE)
                    .pushReaction(PushReaction.BLOCK).isValidSpawn((State, World, Pos, Kind) -> false)));
    public static final DeferredBlock<Scp1730TurretBlock> Turret = BlockRegister.register("scp1730_turret",
            () -> new Scp1730TurretBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 8.0f).requiresCorrectToolForDrops().noOcclusion()
                    .sound(SoundType.NETHERITE_BLOCK).pushReaction(PushReaction.BLOCK)));
    public static final DeferredBlock<Scp1730LaserBlock> Laser = BlockRegister.register("scp1730_laser_emitter",
            () -> new Scp1730LaserBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 6.0f).requiresCorrectToolForDrops().noOcclusion()
                    .sound(SoundType.METAL).lightLevel(State -> State.getValue(Scp1730LaserBlock.Powered) && !State.getValue(Scp1730LaserBlock.Broken) ? 4 : 0)));
    public static final DeferredBlock<Scp1730ControlBlock> Control = BlockRegister.register("scp1730_control",
            () -> new Scp1730ControlBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 6.0f).requiresCorrectToolForDrops().noOcclusion()
                    .sound(SoundType.METAL).lightLevel(State -> 3)));
    public static final DeferredBlock<Scp1730SealBlock> Seal = BlockRegister.register("scp1730_seal",
            () -> new Scp1730SealBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(-1.0f, 3600000.0f).noLootTable()
                    .sound(SoundType.NETHERITE_BLOCK).pushReaction(PushReaction.BLOCK).isValidSpawn((State, World, Pos, Kind) -> false)));
    public static final DeferredBlock<Scp1730CardBlock> Card = BlockRegister.register("scp1730_card",
            () -> new Scp1730CardBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).noCollission().noOcclusion().instabreak().noLootTable()
                    .sound(SoundType.WOOL).pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<Scp1730CardBlock> Clearance = BlockRegister.register("scp1730_clearance",
            () -> new Scp1730CardBlock.Clearance(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).noCollission().noOcclusion().instabreak().noLootTable()
                    .sound(SoundType.WOOL).pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<Scp1730ThresherBlock> Thresher = BlockRegister.register("scp1730_thresher",
            () -> new Scp1730ThresherBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(-1.0f, 3600000.0f).noLootTable().noOcclusion()
                    .sound(SoundType.NETHERITE_BLOCK).pushReaction(PushReaction.BLOCK).lightLevel(State -> 9)));
    public static final DeferredBlock<RealityFolderBlock> Folder = BlockRegister.register("reality_folder",
            () -> new RealityFolderBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(6.0f, 1200.0f).requiresCorrectToolForDrops().noOcclusion()
                    .sound(SoundType.NETHERITE_BLOCK).pushReaction(PushReaction.BLOCK).isValidSpawn((State, World, Pos, Kind) -> false)
                    .lightLevel(State -> State.getValue(RealityFolderBlock.Powered) ? 7 : 0)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp1730SymbolBlockEntity>> SymbolEntity = BlockEntityTypeRegister.register("scp1730_symbol",
            () -> BlockEntityType.Builder.of(Scp1730SymbolBlockEntity::new, Symbol.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp1730CrackBlockEntity>> CrackEntity = BlockEntityTypeRegister.register("scp1730_crack",
            () -> BlockEntityType.Builder.of(Scp1730CrackBlockEntity::new, Crack.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp1730TurretBlockEntity>> TurretEntity = BlockEntityTypeRegister.register("scp1730_turret",
            () -> BlockEntityType.Builder.of(Scp1730TurretBlockEntity::new, Turret.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp1730LaserBlockEntity>> LaserEntity = BlockEntityTypeRegister.register("scp1730_laser_emitter",
            () -> BlockEntityType.Builder.of(Scp1730LaserBlockEntity::new, Laser.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp1730ThresherBlockEntity>> ThresherEntity = BlockEntityTypeRegister.register("scp1730_thresher",
            () -> BlockEntityType.Builder.of(Scp1730ThresherBlockEntity::new, Thresher.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RealityFolderBlockEntity>> FolderEntity = BlockEntityTypeRegister.register("reality_folder",
            () -> BlockEntityType.Builder.of(RealityFolderBlockEntity::new, Folder.get()).build(null));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp1730Painter>> PainterType = EntityTypeRegister.register("scp1730_1",
            () -> EntityType.Builder.<Scp1730Painter>of(Scp1730Painter::new, MobCategory.CREATURE).sized(Scp1730Painter.Width, Scp1730Painter.Height)
                    .eyeHeight(Scp1730Painter.EyeHeight).fireImmune().clientTrackingRange(10).build("scp1730_1"));
    public static final DeferredHolder<EntityType<?>, EntityType<Scp1730Guardian>> GuardianType = EntityTypeRegister.register("scp1730_2",
            () -> EntityType.Builder.<Scp1730Guardian>of(Scp1730Guardian::new, MobCategory.MONSTER).sized(Scp1730Guardian.Width, Scp1730Guardian.Height)
                    .eyeHeight(Scp1730Guardian.EyeHeight).fireImmune().clientTrackingRange(16).build("scp1730_2"));
    public static final DeferredHolder<EntityType<?>, EntityType<Scp1730Leech>> LeechType = EntityTypeRegister.register("scp1730_leech",
            () -> EntityType.Builder.<Scp1730Leech>of(Scp1730Leech::new, MobCategory.MONSTER).sized(Scp1730Leech.Width, Scp1730Leech.Height)
                    .eyeHeight(0.14f).clientTrackingRange(8).build("scp1730_leech"));
    public static final DeferredHolder<EntityType<?>, EntityType<Scp1730Tentacle>> TentacleType = EntityTypeRegister.register("scp1730_tentacle",
            () -> EntityType.Builder.<Scp1730Tentacle>of(Scp1730Tentacle::new, MobCategory.MISC).sized(Scp1730Tentacle.Width, Scp1730Tentacle.Height)
                    .fireImmune().noSummon().clientTrackingRange(10).updateInterval(1).build("scp1730_tentacle"));
    public static final DeferredHolder<EntityType<?>, EntityType<RealityAnchorEntity>> AnchorType = EntityTypeRegister.register("scp1730_anchor",
            () -> EntityType.Builder.<RealityAnchorEntity>of(RealityAnchorEntity::new, MobCategory.MISC).sized(0.25f, 0.25f).fireImmune()
                    .clientTrackingRange(6).updateInterval(4).build("scp1730_anchor"));
    public static final DeferredHolder<EntityType<?>, EntityType<RealityAnchorField>> FieldType = EntityTypeRegister.register("scp1730_anchor_field",
            () -> EntityType.Builder.<RealityAnchorField>of(RealityAnchorField::new, MobCategory.MISC).sized(0.5f, 0.5f).fireImmune().noSummon()
                    .clientTrackingRange(8).updateInterval(20).build("scp1730_anchor_field"));

    public static final DeferredItem<RealityAnchorItem> Anchor = ItemRegister.register("scranton_reality_anchor",
            () -> new RealityAnchorItem(new Item.Properties().stacksTo(8).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Scp1730FragmentItem> Fragment = ItemRegister.register("scp1730_contained_fragment",
            () -> new Scp1730FragmentItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant()));
    public static final DeferredItem<Scp1730CoreItem> Core = ItemRegister.register("scp1730_thresher_core",
            () -> new Scp1730CoreItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredItem<Scp1730CardItem> AccessCard = ItemRegister.register("scp1730_access_card",
            () -> new Scp1730CardItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Scp1730CardItem> ClearanceCard = ItemRegister.register("scp1730_clearance_card",
            () -> new Scp1730CardItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<BlockItem> FolderItem = ItemRegister.registerSimpleBlockItem("reality_folder", Folder, new Item.Properties().rarity(Rarity.EPIC));
    public static final DeferredItem<BlockItem> SymbolItem = facility("scp1730_symbol", Symbol);
    public static final DeferredItem<BlockItem> CrackItem = facility("scp1730_crack", Crack);
    public static final DeferredItem<BlockItem> PitVoidItem = facility("scp1730_pit_void", PitVoid);
    public static final DeferredItem<BlockItem> TurretItem = facility("scp1730_turret", Turret);
    public static final DeferredItem<BlockItem> LaserItem = facility("scp1730_laser_emitter", Laser);
    public static final DeferredItem<BlockItem> ControlItem = facility("scp1730_control", Control);
    public static final DeferredItem<BlockItem> SealItem = facility("scp1730_seal", Seal);
    public static final DeferredItem<BlockItem> CardItem = facility("scp1730_card", Card);
    public static final DeferredItem<BlockItem> ClearanceItem = facility("scp1730_clearance", Clearance);
    public static final DeferredItem<BlockItem> ThresherItem = facility("scp1730_thresher", Thresher);

    public static final DeferredItem<DeferredSpawnEggItem> PainterEgg = ItemRegister.register("scp1730_1_spawn_egg",
            () -> new DeferredSpawnEggItem(PainterType, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<DeferredSpawnEggItem> GuardianEgg = ItemRegister.register("scp1730_2_spawn_egg",
            () -> new DeferredSpawnEggItem(GuardianType, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<DeferredSpawnEggItem> LeechEgg = ItemRegister.register("scp1730_leech_spawn_egg",
            () -> new DeferredSpawnEggItem(LeechType, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GlyphParticle = ParticleRegister.register("scp1730_glyph", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> AnchorParticle = ParticleRegister.register("scp1730_anchor", () -> new SimpleParticleType(false));

    public static final DeferredHolder<SoundEvent, SoundEvent> AmbienceAlarm = sound("scp1730_ambience_alarm");
    public static final DeferredHolder<SoundEvent, SoundEvent> AmbiencePa = sound("scp1730_ambience_pa");
    public static final DeferredHolder<SoundEvent, SoundEvent> AmbienceDrip = sound("scp1730_ambience_drip");
    public static final DeferredHolder<SoundEvent, SoundEvent> AmbienceGroan = sound("scp1730_ambience_groan");
    public static final DeferredHolder<SoundEvent, SoundEvent> AmbienceVent = sound("scp1730_ambience_vent");
    public static final DeferredHolder<SoundEvent, SoundEvent> SymbolGaze = sound("scp1730_symbol_gaze");
    public static final DeferredHolder<SoundEvent, SoundEvent> SymbolBurn = sound("scp1730_symbol_burn");
    public static final DeferredHolder<SoundEvent, SoundEvent> Paint = sound("scp1730_paint");
    public static final DeferredHolder<SoundEvent, SoundEvent> Scrub = sound("scp1730_scrub");
    public static final DeferredHolder<SoundEvent, SoundEvent> PainterIdle = sound("scp1730_painter_idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> PainterAnchored = sound("scp1730_painter_anchored");
    public static final DeferredHolder<SoundEvent, SoundEvent> GuardianHum = sound("scp1730_guardian_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> GuardianCast = sound("scp1730_guardian_cast");
    public static final DeferredHolder<SoundEvent, SoundEvent> GuardianSwipe = sound("scp1730_guardian_swipe");
    public static final DeferredHolder<SoundEvent, SoundEvent> GuardianGrab = sound("scp1730_guardian_grab");
    public static final DeferredHolder<SoundEvent, SoundEvent> GuardianSlam = sound("scp1730_guardian_slam");
    public static final DeferredHolder<SoundEvent, SoundEvent> GuardianPush = sound("scp1730_guardian_push");
    public static final DeferredHolder<SoundEvent, SoundEvent> GuardianHurt = sound("scp1730_guardian_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> GuardianDeath = sound("scp1730_guardian_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> GuardianChains = sound("scp1730_guardian_chains");
    public static final DeferredHolder<SoundEvent, SoundEvent> GuardianWard = sound("scp1730_guardian_ward");
    public static final DeferredHolder<SoundEvent, SoundEvent> LeechCrawl = sound("scp1730_leech_crawl");
    public static final DeferredHolder<SoundEvent, SoundEvent> LeechBite = sound("scp1730_leech_bite");
    public static final DeferredHolder<SoundEvent, SoundEvent> LeechDeath = sound("scp1730_leech_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> LeechEmerge = sound("scp1730_leech_emerge");
    public static final DeferredHolder<SoundEvent, SoundEvent> TentacleRumble = sound("scp1730_tentacle_rumble");
    public static final DeferredHolder<SoundEvent, SoundEvent> TentacleStrike = sound("scp1730_tentacle_strike");
    public static final DeferredHolder<SoundEvent, SoundEvent> TentacleWithdraw = sound("scp1730_tentacle_withdraw");
    public static final DeferredHolder<SoundEvent, SoundEvent> PitFall = sound("scp1730_pit_fall");
    public static final DeferredHolder<SoundEvent, SoundEvent> TurretFire = sound("scp1730_turret_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> TurretLock = sound("scp1730_turret_lock");
    public static final DeferredHolder<SoundEvent, SoundEvent> LaserTrip = sound("scp1730_laser_trip");
    public static final DeferredHolder<SoundEvent, SoundEvent> Alarm = fixed("scp1730_alarm", 64.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> Breaker = sound("scp1730_breaker");
    public static final DeferredHolder<SoundEvent, SoundEvent> ReaderAccept = sound("scp1730_reader_accept");
    public static final DeferredHolder<SoundEvent, SoundEvent> ReaderDeny = sound("scp1730_reader_deny");
    public static final DeferredHolder<SoundEvent, SoundEvent> Release = sound("scp1730_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> SealOpen = sound("scp1730_seal_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> AnchorBurst = sound("scp1730_anchor_burst");
    public static final DeferredHolder<SoundEvent, SoundEvent> AnchorHum = sound("scp1730_anchor_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> FolderSpinup = sound("reality_folder_spinup");
    public static final DeferredHolder<SoundEvent, SoundEvent> FolderRunning = sound("reality_folder_running");
    public static final DeferredHolder<SoundEvent, SoundEvent> FolderShutdown = fixed("reality_folder_shutdown", 48.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> ThresherHum = sound("scp1730_thresher_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> ThresherPull = fixed("scp1730_thresher_pull", 64.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> CollapseRumble = fixed("scp1730_collapse_rumble", 48.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> CollapseDebris = sound("scp1730_collapse_debris");
    public static final DeferredHolder<SoundEvent, SoundEvent> FragmentUse = sound("scp1730_fragment_use");
    public static final DeferredHolder<SoundEvent, SoundEvent> CoreHum = sound("scp1730_core_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> TurretCrunch = sound("scp1730_turret_crunch");
    public static final DeferredHolder<SoundEvent, SoundEvent> TurretFizz = sound("scp1730_turret_fizz");
    public static final DeferredHolder<SoundEvent, SoundEvent> TurretSpark = sound("scp1730_turret_spark");
    public static final DeferredHolder<SoundEvent, SoundEvent> TurretHit = sound("scp1730_turret_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> EmitterBreak = sound("scp1730_emitter_break");
    public static final DeferredHolder<SoundEvent, SoundEvent> EmitterFizz = sound("scp1730_emitter_fizz");
    public static final DeferredHolder<SoundEvent, SoundEvent> AnchorBoom = fixed("scp1730_anchor_boom", 40.0f);
    public static final DeferredHolder<SoundEvent, SoundEvent> AnchorThump = sound("scp1730_anchor_thump");
    public static final DeferredHolder<SoundEvent, SoundEvent> AnchorRing = sound("scp1730_anchor_ring");

    public static final ResourceKey<DamageType> SymbolDamage = damage("scp1730_symbol");
    public static final ResourceKey<DamageType> PitDamage = damage("scp1730_pit");
    public static final ResourceKey<DamageType> TentacleDamage = damage("scp1730_tentacle");
    public static final ResourceKey<DamageType> GuardianDamage = damage("scp1730_guardian");
    public static final ResourceKey<DamageType> LeechDamage = damage("scp1730_leech");
    public static final ResourceKey<DamageType> TurretDamage = damage("scp1730_turret");
    public static final ResourceKey<DamageType> DebrisDamage = damage("scp1730_debris");

    static {
        for (Scp1730Decor Entry : Scp1730Decor.values()) {
            DeferredBlock<Block> Made = BlockRegister.register(Entry.id(), () -> Scp1730DecorBlock.make(Entry, decorProperties(Entry)));
            Decor.put(Entry, Made);
            FacilityItems.add(ItemRegister.registerSimpleBlockItem(Entry.id(), Made));
        }
    }

    private Scp1730Registry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static BlockBehaviour.Properties decorProperties(Scp1730Decor Entry) {
        BlockBehaviour.Properties Props = BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.0f, 6.0f).sound(SoundType.STONE);
        int Glow = Entry.glow();
        if (Glow > 0) {
            Props = Props.lightLevel(State -> Glow);
        }
        return switch (Entry.kind()) {
            case Cube, FacingCube -> Props;
            case Prop -> Props.noOcclusion().sound(SoundType.METAL).strength(1.5f, 4.0f);
            case Decal, Light -> Props.noOcclusion().noCollission().replaceable().instabreak().sound(SoundType.METAL).pushReaction(PushReaction.DESTROY);
        };
    }

    private static <B extends Block> DeferredItem<BlockItem> facility(String Name, Supplier<B> Made) {
        DeferredItem<BlockItem> Item0 = ItemRegister.register(Name, () -> new BlockItem(Made.get(), new Item.Properties()));
        FacilityItems.add(Item0);
        return Item0;
    }

    public static List<DeferredItem<? extends Item>> facilityItems() {
        return Collections.unmodifiableList(FacilityItems);
    }

    public static Map<Scp1730Decor, DeferredBlock<Block>> decor() {
        return Collections.unmodifiableMap(Decor);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> fixed(String Name, float Range) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createFixedRangeEvent(location(Name), Range));
    }

    private static ResourceKey<DamageType> damage(String Name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, location(Name));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        GeneratorRegister.register(ModEventBus);
        net.rai.scp_unleashed.scp1730.rooms.Scp1730RoomsRegistry.register(ModEventBus);
        net.rai.scp_unleashed.scp1730.puzzle.Scp1730PuzzleRegistry.register(ModEventBus);
        net.rai.scp_unleashed.scp1730.guardian.Scp1730GuardianRegistry.register(ModEventBus);
        net.rai.scp_unleashed.scp1730.pit.Scp1730PitRegistry.register(ModEventBus);
        GatewayDestinations.register(Scp1730Destination.make());
        StasisPods.addCaptureVeto(Target -> Target instanceof Scp1730Guardian Boss && !net.rai.scp_unleashed.scp1730.guardian.Scp1730GuardianCapture.capturable(Boss) || Target instanceof Scp1730Tentacle
                || Target instanceof Scp1730Painter Painter && !Painter.anchored());
    }
}
