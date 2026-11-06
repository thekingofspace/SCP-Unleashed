package net.rai.scp_unleashed.scp002;

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
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.DeferredSoundType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.ReaderTargets;
import net.rai.scp_unleashed.archive.investigation.StartConditions;

public final class Scp002Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);

    public static final float FarSoundRange = 256.0f;
    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_002");
    public static final ResourceKey<DamageType> ConsumedDamage = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp002_consumed"));

    public static final DeferredHolder<SoundEvent, SoundEvent> MeteorRush = sound("scp002_meteor_rush");
    public static final DeferredHolder<SoundEvent, SoundEvent> MeteorRoar = sound("scp002_meteor_roar");
    public static final DeferredHolder<SoundEvent, SoundEvent> MeteorBurn = sound("scp002_meteor_burn");
    public static final DeferredHolder<SoundEvent, SoundEvent> MeteorSwoop = sound("scp002_meteor_swoop");
    public static final DeferredHolder<SoundEvent, SoundEvent> ImpactBoom = farSound("scp002_impact_boom");
    public static final DeferredHolder<SoundEvent, SoundEvent> ImpactCrack = farSound("scp002_impact_crack");
    public static final DeferredHolder<SoundEvent, SoundEvent> ImpactRumble = farSound("scp002_impact_rumble");
    public static final DeferredHolder<SoundEvent, SoundEvent> ImpactSub = farSound("scp002_impact_sub");
    public static final DeferredHolder<SoundEvent, SoundEvent> Debris = farSound("scp002_debris");
    public static final DeferredHolder<SoundEvent, SoundEvent> CraterSizzle = sound("scp002_crater_sizzle");
    public static final DeferredHolder<SoundEvent, SoundEvent> DoorOpen = sound("scp002_door_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> DoorGrind = sound("scp002_door_grind");
    public static final DeferredHolder<SoundEvent, SoundEvent> DoorClang = sound("scp002_door_clang");
    public static final DeferredHolder<SoundEvent, SoundEvent> DoorClose = sound("scp002_door_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> DoorDeny = sound("scp002_door_deny");
    public static final DeferredHolder<SoundEvent, SoundEvent> Heartbeat = sound("scp002_heartbeat");
    public static final DeferredHolder<SoundEvent, SoundEvent> FleshSquelch = sound("scp002_flesh_squelch");
    public static final DeferredHolder<SoundEvent, SoundEvent> FleshStep = sound("scp002_flesh_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> RoomGroan = sound("scp002_room_groan");
    public static final DeferredHolder<SoundEvent, SoundEvent> RoomBreath = sound("scp002_room_breath");
    public static final DeferredHolder<SoundEvent, SoundEvent> ConsumeTear = sound("scp002_consume_tear");

    public static final SoundType FleshSound = new DeferredSoundType(1.0f, 0.9f, FleshSquelch, FleshStep, FleshSquelch, FleshStep, FleshStep);

    public static final DeferredBlock<MeteoricShellBlock> Shell = BlockRegister.register("scp002_shell", () -> new MeteoricShellBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.DEEPSLATE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.5f, 9.0f).sound(SoundType.ANCIENT_DEBRIS)));
    public static final DeferredBlock<FleshBlock> Flesh = BlockRegister.register("scp002_flesh", () -> new FleshBlock(flesh(MapColor.CRIMSON_NYLIUM)));
    public static final DeferredBlock<FleshSurfaceBlock> FleshWall = BlockRegister.register("scp002_flesh_wall", () -> new FleshSurfaceBlock(flesh(MapColor.TERRACOTTA_PINK)));
    public static final DeferredBlock<FleshSurfaceBlock> FleshFloor = BlockRegister.register("scp002_flesh_floor", () -> new FleshSurfaceBlock(flesh(MapColor.TERRACOTTA_RED)));
    public static final DeferredBlock<BulkheadDoorBlock> Door = BlockRegister.register("scp002_door", () -> new BulkheadDoorBlock(flesh(MapColor.METAL).sound(SoundType.NETHERITE_BLOCK).noOcclusion()));
    public static final DeferredBlock<FleshBedBlock> Bed = BlockRegister.register("scp002_bed", () -> new FleshBedBlock(flesh(MapColor.TERRACOTTA_PINK).noOcclusion()));
    public static final DeferredBlock<FleshFixtureBlock> Desk = BlockRegister.register("scp002_desk", () -> new FleshFixtureBlock(flesh(MapColor.TERRACOTTA_RED).noOcclusion(), Fixture.Desk));
    public static final DeferredBlock<FleshFixtureBlock> Chair = BlockRegister.register("scp002_chair", () -> new FleshFixtureBlock(flesh(MapColor.TERRACOTTA_RED).noOcclusion(), Fixture.Chair));
    public static final DeferredBlock<FleshFixtureBlock> Dresser = BlockRegister.register("scp002_dresser", () -> new FleshFixtureBlock(flesh(MapColor.TERRACOTTA_RED).noOcclusion(), Fixture.Dresser));
    public static final DeferredBlock<BoneLampBlock> Lamp = BlockRegister.register("scp002_lamp", () -> new BoneLampBlock(flesh(MapColor.SAND).noOcclusion()
            .lightLevel(State -> State.getValue(BoneLampBlock.Lit) ? BoneLampBlock.LitLevel : BoneLampBlock.DimLevel)));
    public static final DeferredBlock<FleshRugBlock> Rug = BlockRegister.register("scp002_rug", () -> new FleshRugBlock(flesh(MapColor.TERRACOTTA_WHITE).noOcclusion()));

    public static final DeferredItem<BlockItem> ShellItem = ItemRegister.registerSimpleBlockItem("scp002_shell", Shell);
    public static final DeferredItem<Scp002SpecimenItem> Specimen = ItemRegister.register("scp002_specimen",
            () -> new Scp002SpecimenItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BulkheadDoorBlockEntity>> DoorEntity = BlockEntityTypeRegister.register("scp002_door",
            () -> BlockEntityType.Builder.of(BulkheadDoorBlockEntity::new, Door.get()).build(null));

    public static final DeferredHolder<EntityType<?>, EntityType<MeteorEntity>> Meteor = EntityTypeRegister.register("scp002_meteor",
            () -> EntityType.Builder.<MeteorEntity>of(MeteorEntity::new, MobCategory.MISC)
                    .sized(1.6f, 1.6f)
                    .fireImmune()
                    .noSummon()
                    .clientTrackingRange(16)
                    .updateInterval(10)
                    .build("scp002_meteor"));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Ember = ParticleRegister.register("scp002_ember", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Tendril = ParticleRegister.register("scp002_tendril", () -> new SimpleParticleType(false));

    private Scp002Registry() {
    }

    private static BlockBehaviour.Properties flesh(MapColor Colour) {
        return BlockBehaviour.Properties.of().mapColor(Colour).strength(-1.0f, 3600000.0f).sound(FleshSound).pushReaction(PushReaction.BLOCK)
                .isValidSpawn((State, World, Pos, Kind) -> false);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> farSound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createFixedRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name), FarSoundRange));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        StartConditions.register(MeteorCondition.TypeName, MeteorCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp002Strikes::spawn);
        ReaderTargets.onHandoff((Server, CaseId, From, To) -> Scp002Strikes.get(Server).caseMoved(CaseId, To));
    }
}
