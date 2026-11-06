package net.rai.scp_unleashed.scp003;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.util.DeferredSoundType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.stranger.StrangerEncounters;

public final class Scp003Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_003");

    public static final DeferredHolder<SoundEvent, SoundEvent> BoardSeat = sound("scp003_board_seat");
    public static final DeferredHolder<SoundEvent, SoundEvent> BoardPulse = sound("scp003_board_pulse");
    public static final DeferredHolder<SoundEvent, SoundEvent> BoardTwitch = sound("scp003_board_twitch");
    public static final DeferredHolder<SoundEvent, SoundEvent> BoardCrack = sound("scp003_board_crack");
    public static final DeferredHolder<SoundEvent, SoundEvent> BoardHatch = sound("scp003_board_hatch");
    public static final DeferredHolder<SoundEvent, SoundEvent> BoardHatchLow = sound("scp003_board_hatch_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> FleshSquelch = sound("scp003_flesh_squelch");
    public static final DeferredHolder<SoundEvent, SoundEvent> FleshStep = sound("scp003_flesh_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> PolypGrow = sound("scp003_polyp_grow");
    public static final DeferredHolder<SoundEvent, SoundEvent> PolypWither = sound("scp003_polyp_wither");
    public static final DeferredHolder<SoundEvent, SoundEvent> LarvaAmbient = sound("scp003_larva_ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> LarvaHurt = sound("scp003_larva_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> LarvaDeath = sound("scp003_larva_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> LarvaStep = sound("scp003_larva_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> Moult = sound("scp003_moult");
    public static final DeferredHolder<SoundEvent, SoundEvent> MoultBurst = sound("scp003_moult_burst");
    public static final DeferredHolder<SoundEvent, SoundEvent> CrawlerAmbient = sound("scp003_crawler_ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> CrawlerHurt = sound("scp003_crawler_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> CrawlerDeath = sound("scp003_crawler_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> CrawlerStep = sound("scp003_crawler_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> CrawlerAttack = sound("scp003_crawler_attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> HostAmbient = sound("scp003_host_ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> HostHurt = sound("scp003_host_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> HostDeath = sound("scp003_host_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> HostStep = sound("scp003_host_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> HostAttack = sound("scp003_host_attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> HostPlace = sound("scp003_host_place");
    public static final DeferredHolder<SoundEvent, SoundEvent> Ritual = sound("scp003_ritual");
    public static final DeferredHolder<SoundEvent, SoundEvent> RitualDrone = sound("scp003_ritual_drone");
    public static final DeferredHolder<SoundEvent, SoundEvent> BoardForm = sound("scp003_board_form");
    public static final DeferredHolder<SoundEvent, SoundEvent> AlphaAmbient = sound("scp003_alpha_ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> AlphaHurt = sound("scp003_alpha_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> AlphaDeath = sound("scp003_alpha_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> AlphaStep = sound("scp003_alpha_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> AlphaHeal = sound("scp003_alpha_heal");
    public static final DeferredHolder<SoundEvent, SoundEvent> AlphaHealTarget = sound("scp003_alpha_heal_target");
    public static final DeferredHolder<SoundEvent, SoundEvent> Promote = sound("scp003_promote");
    public static final DeferredHolder<SoundEvent, SoundEvent> PromoteLow = sound("scp003_promote_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> HostThud = sound("scp003_host_thud");
    public static final DeferredHolder<SoundEvent, SoundEvent> AlphaThud = sound("scp003_alpha_thud");
    public static final DeferredHolder<SoundEvent, SoundEvent> AlphaAttack = sound("scp003_alpha_attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> BarkCreak = sound("scp003_bark_creak");
    public static final DeferredHolder<SoundEvent, SoundEvent> RootShuffle = sound("scp003_root_shuffle");
    public static final DeferredHolder<SoundEvent, SoundEvent> CrawlerWet = sound("scp003_crawler_wet");

    public static final SoundType PolypSound = new DeferredSoundType(0.8f, 1.0f, FleshSquelch, FleshStep, FleshSquelch, FleshStep, FleshStep);
    public static final SoundType BoardSound = new DeferredSoundType(0.7f, 1.1f, FleshSquelch, FleshStep, BoardSeat, FleshStep, FleshStep);

    public static final DeferredBlock<Scp003PlateBlock> Plate = BlockRegister.register("scp003_plate", () -> new Scp003PlateBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).strength(1.5f, 6.0f).sound(SoundType.STONE).noOcclusion().pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<Scp003BoardBlock> Board = BlockRegister.register("scp003_board", () -> new Scp003BoardBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_CYAN).strength(0.6f, 1.0f).sound(BoardSound).noOcclusion().pushReaction(PushReaction.DESTROY)
            .isValidSpawn((State, World, Pos, Kind) -> false)));
    public static final DeferredBlock<Scp003PolypBlock> Polyp = BlockRegister.register("scp003_polyp", () -> new Scp003PolypBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.CRIMSON_STEM).strength(0.5f, 0.5f).sound(PolypSound).noOcclusion().noCollission().pushReaction(PushReaction.DESTROY)
            .lightLevel(State -> !State.getValue(Scp003PolypBlock.Withered) && State.getValue(Scp003PolypBlock.Age) >= Scp003PolypBlock.MaxAge ? 5 : 0)));

    public static final DeferredItem<Scp003PlateItem> PlateItem = ItemRegister.register("scp003_plate",
            () -> new Scp003PlateItem(Plate.get(), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<Scp003BoardItem> BoardItem = ItemRegister.register("scp003_board",
            () -> new Scp003BoardItem(Board.get(), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp003BoardBlockEntity>> BoardEntity = BlockEntityTypeRegister.register("scp003_board",
            () -> BlockEntityType.Builder.of(Scp003BoardBlockEntity::new, Board.get()).build(null));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp003Larva>> Larva = EntityTypeRegister.register("scp003_larva",
            () -> EntityType.Builder.<Scp003Larva>of(Scp003Larva::new, MobCategory.MONSTER).sized(0.6f, 0.35f).eyeHeight(0.22f).clientTrackingRange(10).build("scp003_larva"));
    public static final DeferredHolder<EntityType<?>, EntityType<Scp003Crawler>> Crawler = EntityTypeRegister.register("scp003_crawler",
            () -> EntityType.Builder.<Scp003Crawler>of(Scp003Crawler::new, MobCategory.MONSTER).sized(1.4f, 1.45f).eyeHeight(0.78f).clientTrackingRange(10).build("scp003_crawler"));
    public static final DeferredHolder<EntityType<?>, EntityType<Scp003Host>> Host = EntityTypeRegister.register("scp003_host",
            () -> EntityType.Builder.<Scp003Host>of(Scp003Host::new, MobCategory.MONSTER).sized(0.9f, 2.95f).eyeHeight(2.78f).clientTrackingRange(10).build("scp003_host"));
    public static final DeferredHolder<EntityType<?>, EntityType<Scp003Alpha>> Alpha = EntityTypeRegister.register("scp003_alpha",
            () -> EntityType.Builder.<Scp003Alpha>of(Scp003Alpha::new, MobCategory.MONSTER).sized(1.3f, 3.7f).eyeHeight(3.52f).clientTrackingRange(10).build("scp003_alpha"));

    public static final DeferredItem<DeferredSpawnEggItem> LarvaEgg = ItemRegister.register("scp003_larva_spawn_egg",
            () -> new DeferredSpawnEggItem(Larva, 0xFFFFFF, 0xFFFFFF, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> CrawlerEgg = ItemRegister.register("scp003_crawler_spawn_egg",
            () -> new DeferredSpawnEggItem(Crawler, 0xFFFFFF, 0xFFFFFF, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> HostEgg = ItemRegister.register("scp003_host_spawn_egg",
            () -> new DeferredSpawnEggItem(Host, 0xFFFFFF, 0xFFFFFF, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> AlphaEgg = ItemRegister.register("scp003_alpha_spawn_egg",
            () -> new DeferredSpawnEggItem(Alpha, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.RARE)));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Flesh = ParticleRegister.register("scp003_flesh", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Heal = ParticleRegister.register("scp003_heal", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Spore = ParticleRegister.register("scp003_spore", () -> new SimpleParticleType(false));

    private Scp003Registry() {
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
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
    }
}
