package net.rai.scp_unleashed.scp409;

import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.util.DeferredSoundType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.stranger.StrangerEncounters;

import java.util.List;

public final class Scp409Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_409");
    public static final ResourceKey<DamageType> OvergrownDamage = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp409_overgrown"));
    public static final TagKey<EntityType<?>> Immune = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp409_immune"));

    public static final DeferredHolder<SoundEvent, SoundEvent> CaseOpen = sound("scp409_case_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> CaseOpenScrape = sound("scp409_case_open_scrape");
    public static final DeferredHolder<SoundEvent, SoundEvent> CaseClose = sound("scp409_case_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> CaseCloseThud = sound("scp409_case_close_thud");
    public static final DeferredHolder<SoundEvent, SoundEvent> Growth = sound("scp409_growth");
    public static final DeferredHolder<SoundEvent, SoundEvent> GrowthTick = sound("scp409_growth_tick");
    public static final DeferredHolder<SoundEvent, SoundEvent> Infect = sound("scp409_infect");
    public static final DeferredHolder<SoundEvent, SoundEvent> InfectRing = sound("scp409_infect_ring");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hum = sound("scp409_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> Shatter = sound("scp409_shatter");
    public static final DeferredHolder<SoundEvent, SoundEvent> ShatterCluster = sound("scp409_shatter_cluster");
    public static final DeferredHolder<SoundEvent, SoundEvent> ShatterChime = sound("scp409_shatter_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> CaseStep = sound("scp409_case_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> CaseBreak = sound("scp409_case_break");
    public static final DeferredHolder<SoundEvent, SoundEvent> CasePlace = sound("scp409_case_place");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> OpenLayers = List.of(CaseOpen, CaseOpenScrape);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CloseLayers = List.of(CaseClose, CaseCloseThud);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> GrowthLayers = List.of(Growth, GrowthTick);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> InfectLayers = List.of(Infect, InfectRing);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ShatterLayers = List.of(Shatter, ShatterCluster, ShatterChime);

    public static final DeferredSoundType CaseSounds = new DeferredSoundType(1.0f, 1.0f, CaseBreak, CaseStep, CasePlace, CaseStep, CaseStep);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ShardParticle = ParticleRegister.register("scp409_shard", () -> new SimpleParticleType(false));

    public static final DeferredBlock<Scp409CaseBlock> Case = BlockRegister.register("scp409_case", () -> new Scp409CaseBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.DIRT).instrument(NoteBlockInstrument.BASEDRUM).strength(Scp409CaseBlock.Hardness, Scp409CaseBlock.Resistance)
            .sound(CaseSounds)
            .noOcclusion().pushReaction(PushReaction.BLOCK).isValidSpawn((State, Getter, Pos, Type) -> false)
            .isRedstoneConductor((State, Getter, Pos) -> false).lightLevel(Scp409CaseBlock::glow)));
    public static final DeferredItem<Scp409CaseItem> CaseItem = ItemRegister.register("scp409_case",
            () -> new Scp409CaseItem(Case.get(), new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp409CaseBlockEntity>> CaseEntity = BlockEntityTypeRegister.register("scp409_case",
            () -> BlockEntityType.Builder.of(Scp409CaseBlockEntity::new, Case.get()).build(null));

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> Crystal = AttachmentRegister.register("scp409_crystal",
            () -> AttachmentType.builder(() -> 0).serialize(Codec.INT, Saved -> Saved >= 0).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Scp409Trace>> Trace = AttachmentRegister.register("scp409_trace",
            () -> AttachmentType.builder(() -> new Scp409Trace()).build());

    public static GameRules.Key<GameRules.BooleanValue> MobSpread;

    private Scp409Registry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
        ModEventBus.addListener(FMLCommonSetupEvent.class, Event -> Event.enqueueWork(Scp409Registry::registerRules));
    }

    private static void registerRules() {
        if (MobSpread == null) {
            MobSpread = GameRules.register("scp409MobSpread", GameRules.Category.MOBS, GameRules.BooleanValue.create(true));
        }
    }

    public static boolean mobSpread(GameRules Rules) {
        if (MobSpread == null) {
            return true;
        }
        GameRules.BooleanValue Rule = Rules.getRule(MobSpread);
        return Rule == null || Rule.get();
    }
}
