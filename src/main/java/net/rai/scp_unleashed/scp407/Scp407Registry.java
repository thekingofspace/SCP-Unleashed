package net.rai.scp_unleashed.scp407;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.speaker.SpeakerRegistry;
import net.rai.scp_unleashed.speaker.cassette.CassetteRecording;
import net.rai.scp_unleashed.stasis.StasisPods;
import net.rai.scp_unleashed.stranger.StrangerEncounters;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class Scp407Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_407");
    public static final ResourceLocation SongId = location("scp407_song");
    public static final String TrackKey = "item.scp_unleashed.scp407_cassette.track";
    public static final int LabelColour = 0x9CC27A;
    public static final ResourceKey<DamageType> Withered = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp407_withered"));

    public static final DeferredBlock<Scp407MatBlock> Mat = BlockRegister.register("scp407_mat", () -> new Scp407MatBlock(growth(MapColor.COLOR_GREEN)
            .sound(SoundType.MOSS_CARPET).strength(0.2f).noCollission().noOcclusion()));
    public static final DeferredBlock<Scp407SprigBlock> Sprig = BlockRegister.register("scp407_sprig", () -> new Scp407SprigBlock(growth(MapColor.PLANT)
            .sound(SoundType.AZALEA).strength(0.1f).noCollission().noOcclusion().lightLevel(State -> Scp407GrowthBlock.alive(State) ? 3 : 0)));
    public static final DeferredBlock<Scp407TrunkBlock> Trunk = BlockRegister.register("scp407_trunk", () -> new Scp407TrunkBlock(growth(MapColor.TERRACOTTA_GREEN)
            .sound(SoundType.MANGROVE_ROOTS).strength(1.6f).noOcclusion()));
    public static final DeferredBlock<Scp407BloomBlock> Bloom = BlockRegister.register("scp407_bloom", () -> new Scp407BloomBlock(growth(MapColor.COLOR_YELLOW)
            .sound(SoundType.SPORE_BLOSSOM).strength(0.4f).noOcclusion().lightLevel(State -> Scp407GrowthBlock.alive(State) ? 9 : 0)));
    public static final DeferredBlock<Scp407PodBlock> Pod = BlockRegister.register("scp407_pod", () -> new Scp407PodBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.TERRACOTTA_GREEN).sound(SoundType.MANGROVE_ROOTS).strength(-1.0f, 3600000.0f).noOcclusion().pushReaction(PushReaction.BLOCK)
            .lightLevel(State -> Scp407GrowthBlock.alive(State) ? 4 : 0).randomTicks()));

    public static final DeferredBlock<Scp407PlantBlock> Reeds = BlockRegister.register("scp407_reeds", () -> new Scp407PlantBlock(growth(MapColor.COLOR_LIGHT_GREEN)
            .sound(SoundType.GRASS).strength(0.3f).noOcclusion().dynamicShape(), Scp407PlantBlock.Traits.of(box(3, 0, 3, 13, 16, 13), box(5, 0, 5, 11, 15, 11), 0.15f, 0.04f, 0.0f, 0.0f)));
    public static final DeferredBlock<Scp407PlantBlock> Fern = BlockRegister.register("scp407_fern", () -> new Scp407PlantBlock(growth(MapColor.PLANT)
            .sound(SoundType.AZALEA).strength(0.1f).noCollission().noOcclusion(), Scp407PlantBlock.Traits.of(box(2, 0, 2, 14, 11, 14), null, 0.18f, 0.0f, 0.03f, 0.0f)));
    public static final DeferredBlock<Scp407PlantBlock> Puffball = BlockRegister.register("scp407_puffball", () -> new Scp407PlantBlock(growth(MapColor.TERRACOTTA_WHITE)
            .sound(SoundType.FUNGUS).strength(0.1f).noCollission().noOcclusion().lightLevel(State -> Scp407GrowthBlock.alive(State) ? 7 : 0),
            Scp407PlantBlock.Traits.of(box(2, 0, 2, 14, 9, 14), null, 0.05f, 0.35f, 0.06f, 0.02f)));
    public static final DeferredBlock<Scp407PlantBlock> Coral = BlockRegister.register("scp407_coral", () -> new Scp407PlantBlock(growth(MapColor.TERRACOTTA_PINK)
            .sound(SoundType.CORAL_BLOCK).strength(0.4f).noOcclusion().dynamicShape().lightLevel(State -> Scp407GrowthBlock.alive(State) ? 3 : 0),
            Scp407PlantBlock.Traits.of(box(2, 0, 2, 14, 15, 14), box(5, 0, 5, 11, 13, 11), 0.08f, 0.12f, 0.0f, 0.0f)));
    public static final DeferredBlock<Scp407PlantBlock> Roots = BlockRegister.register("scp407_roots", () -> new Scp407PlantBlock(growth(MapColor.TERRACOTTA_BROWN)
            .sound(SoundType.ROOTS).strength(0.2f).noCollission().noOcclusion(), Scp407PlantBlock.Traits.of(box(1, 0, 1, 15, 5, 15), null, 0.08f, 0.0f, 0.0f, 0.0f)));
    public static final DeferredBlock<Scp407PlantBlock> Creeper = BlockRegister.register("scp407_creeper", () -> new Scp407PlantBlock(growth(MapColor.PLANT)
            .sound(SoundType.CAVE_VINES).strength(0.1f).noCollission().noOcclusion(), Scp407PlantBlock.Traits.of(box(0, 0, 0, 16, 3, 16), null, 0.14f, 0.02f, 0.0f, 0.0f)));
    public static final DeferredBlock<Scp407HangingBlock> Tendril = BlockRegister.register("scp407_tendril", () -> new Scp407HangingBlock(growth(MapColor.PLANT)
            .sound(SoundType.CAVE_VINES).strength(0.1f).noCollission().noOcclusion(), Scp407PlantBlock.Traits.of(box(3, 1, 3, 13, 16, 13), null, 0.12f, 0.0f, 0.05f, 0.0f)));
    public static final DeferredBlock<Scp407HangingBlock> Bulbs = BlockRegister.register("scp407_bulbs", () -> new Scp407HangingBlock(growth(MapColor.COLOR_YELLOW)
            .sound(SoundType.CAVE_VINES).strength(0.1f).noCollission().noOcclusion().lightLevel(State -> Scp407GrowthBlock.alive(State) ? 5 : 0),
            Scp407PlantBlock.Traits.of(box(4, 1, 4, 12, 16, 12), null, 0.04f, 0.3f, 0.0f, 0.0f)));
    public static final DeferredBlock<Scp407ShelfBlock> Shelf = BlockRegister.register("scp407_shelf", () -> new Scp407ShelfBlock(growth(MapColor.TERRACOTTA_ORANGE)
            .sound(SoundType.FUNGUS).strength(0.2f).noCollission().noOcclusion().lightLevel(State -> Scp407GrowthBlock.alive(State) ? 3 : 0),
            Scp407PlantBlock.Traits.of(box(1, 2, 0, 15, 14, 7), null, 0.1f, 0.06f, 0.08f, 0.0f)));
    public static final List<DeferredBlock<? extends Scp407PlantBlock>> Plants = List.of(Reeds, Fern, Puffball, Coral, Roots, Creeper, Tendril, Bulbs, Shelf);

    public static final DeferredHolder<EntityType<?>, EntityType<Scp407Sprout>> Sprout = EntityTypeRegister.register("scp407_sprout",
            () -> EntityType.Builder.<Scp407Sprout>of(Scp407Sprout::new, MobCategory.CREATURE)
                    .sized(Scp407Sprout.Width, Scp407Sprout.Height)
                    .eyeHeight(Scp407Sprout.EyeHeight)
                    .clientTrackingRange(8)
                    .build("scp407_sprout"));
    public static final DeferredHolder<EntityType<?>, EntityType<Scp407Titan>> Titan = EntityTypeRegister.register("scp407_titan",
            () -> EntityType.Builder.<Scp407Titan>of(Scp407Titan::new, MobCategory.MONSTER)
                    .sized(Scp407Titan.Width, Scp407Titan.Height)
                    .eyeHeight(Scp407Titan.EyeHeight)
                    .clientTrackingRange(10)
                    .build("scp407_titan"));

    public static final DeferredItem<Scp407CassetteItem> Cassette = ItemRegister.register("scp407_cassette",
            () -> new Scp407CassetteItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
                    .component(SpeakerRegistry.CassetteComponent.get(), recording())));
    public static final DeferredItem<DeferredSpawnEggItem> SproutEgg = ItemRegister.register("scp407_sprout_spawn_egg",
            () -> new DeferredSpawnEggItem(Sprout, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<DeferredSpawnEggItem> TitanEgg = ItemRegister.register("scp407_titan_spawn_egg",
            () -> new DeferredSpawnEggItem(Titan, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Spore = ParticleRegister.register("scp407_spore", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Leaf = ParticleRegister.register("scp407_leaf", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Ash = ParticleRegister.register("scp407_ash", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Mote = ParticleRegister.register("scp407_mote", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Glint = ParticleRegister.register("scp407_glint", () -> new SimpleParticleType(false));

    public static final DeferredHolder<SoundEvent, SoundEvent> Song = sound("scp407_song");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> Voices = voices();
    public static final DeferredHolder<SoundEvent, SoundEvent> GrowthCreak = sound("scp407_growth_creak");
    public static final DeferredHolder<SoundEvent, SoundEvent> GrowthRustle = sound("scp407_growth_rustle");
    public static final DeferredHolder<SoundEvent, SoundEvent> SporePuff = sound("scp407_spore_puff");
    public static final DeferredHolder<SoundEvent, SoundEvent> PodClose = sound("scp407_pod_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> SproutChirp = sound("scp407_sprout_chirp");
    public static final DeferredHolder<SoundEvent, SoundEvent> SproutHurt = sound("scp407_sprout_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SproutDeath = sound("scp407_sprout_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> SproutStep = sound("scp407_sprout_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> SproutHop = sound("scp407_sprout_hop");
    public static final DeferredHolder<SoundEvent, SoundEvent> TitanStep = sound("scp407_titan_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> TitanStepCreak = sound("scp407_titan_step_creak");
    public static final DeferredHolder<SoundEvent, SoundEvent> TitanGroan = sound("scp407_titan_groan");
    public static final DeferredHolder<SoundEvent, SoundEvent> TitanSwipe = sound("scp407_titan_swipe");
    public static final DeferredHolder<SoundEvent, SoundEvent> TitanGrab = sound("scp407_titan_grab");
    public static final DeferredHolder<SoundEvent, SoundEvent> TitanAbsorb = sound("scp407_titan_absorb");
    public static final DeferredHolder<SoundEvent, SoundEvent> TitanHurt = sound("scp407_titan_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> TitanDeath = sound("scp407_titan_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> TitanEmerge = sound("scp407_titan_emerge");
    public static final DeferredHolder<SoundEvent, SoundEvent> WitherCrackle = sound("scp407_wither_crackle");
    public static final DeferredHolder<SoundEvent, SoundEvent> WitherCrumble = sound("scp407_wither_crumble");

    private Scp407Registry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    public static CassetteRecording recording() {
        return new CassetteRecording(SongId, Component.translatable(TrackKey), Optional.of(LabelColour));
    }

    private static VoxelShape box(double X0, double Y0, double Z0, double X1, double Y1, double Z1) {
        return Block.box(X0, Y0, Z0, X1, Y1, Z1);
    }

    private static BlockBehaviour.Properties growth(MapColor Colour) {
        return BlockBehaviour.Properties.of().mapColor(Colour).pushReaction(PushReaction.DESTROY).randomTicks().isValidSpawn((State, World, Pos, Type) -> false)
                .isRedstoneConductor((State, World, Pos) -> false).isSuffocating((State, World, Pos) -> false).isViewBlocking((State, World, Pos) -> false);
    }

    private static List<DeferredHolder<SoundEvent, SoundEvent>> voices() {
        List<DeferredHolder<SoundEvent, SoundEvent>> Found = new ArrayList<>();
        for (String Name : Scp407Score.Voices) {
            Found.add(sound("scp407_song_" + Name));
        }
        return List.copyOf(Found);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
        StasisPods.addCaptureVeto(Scp407Fusion::refusesStasis);
    }
}
