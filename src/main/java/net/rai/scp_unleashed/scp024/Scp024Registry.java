package net.rai.scp_unleashed.scp024;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
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
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.StartConditions;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class Scp024Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents ComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);
    public static final List<DeferredItem<? extends Item>> BlockItems = new ArrayList<>();

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_024");
    public static final ResourceKey<DamageType> EliminatedDamage = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp024_eliminated"));
    public static final ResourceKey<DamageType> BlastDamage = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp024_blast"));

    public static final DeferredBlock<Scp024SolidBlock> Siding = solid("scp024_siding", MapColor.COLOR_LIGHT_GRAY, SoundType.METAL);
    public static final DeferredBlock<Scp024SolidBlock> Concrete = solid("scp024_concrete", MapColor.STONE, SoundType.STONE);
    public static final DeferredBlock<Scp024SolidBlock> Roof = solid("scp024_roof", MapColor.COLOR_BLACK, SoundType.STONE);
    public static final DeferredBlock<Scp024SolidBlock> Trim = solid("scp024_trim", MapColor.COLOR_GRAY, SoundType.METAL);
    public static final DeferredBlock<Scp024SolidBlock> DockDoor = solid("scp024_dock_door", MapColor.COLOR_LIGHT_GRAY, SoundType.METAL);
    public static final DeferredBlock<Scp024MountBlock> Bumper = block("scp024_bumper", () -> new Scp024MountBlock(Scp024SolidBlock.sealed(MapColor.COLOR_BLACK)
            .sound(SoundType.WOOL).noOcclusion(), 3.0, 3.0, 12.0, 13.0, 13.0, 16.0));
    public static final DeferredBlock<Scp024SignBlock> Sign = block("scp024_sign", () -> new Scp024SignBlock(Scp024SolidBlock.sealed(MapColor.TERRACOTTA_WHITE)
            .sound(SoundType.METAL).noOcclusion()));
    public static final DeferredBlock<Scp024MountBlock> Lamp = block("scp024_lamp", () -> new Scp024MountBlock(Scp024SolidBlock.sealed(MapColor.METAL)
            .sound(SoundType.LANTERN).noOcclusion().lightLevel(State -> 11), 5.0, 3.0, 9.0, 11.0, 13.0, 16.0));
    public static final DeferredBlock<Scp024DoorBlock> StageDoor = BlockRegister.register("scp024_stage_door", () -> new Scp024DoorBlock(Scp024SolidBlock.sealed(MapColor.COLOR_BLACK)
            .sound(SoundType.NETHERITE_BLOCK).noOcclusion().isViewBlocking((State, World, Pos) -> true)));
    public static final DeferredBlock<Scp024ExitDoorBlock> ExitDoor = BlockRegister.register("scp024_exit_door", () -> new Scp024ExitDoorBlock(Scp024SolidBlock.sealed(MapColor.SNOW)
            .sound(SoundType.NETHERITE_BLOCK).noOcclusion().lightLevel(State -> 13)));
    public static final DeferredBlock<Scp024SolidBlock> StageFloor = solid("scp024_stage_floor", MapColor.COLOR_BLACK, SoundType.STONE);
    public static final DeferredBlock<Scp024SolidBlock> StageWall = solid("scp024_stage_wall", MapColor.COLOR_RED, SoundType.WOOL);
    public static final DeferredBlock<Scp024SolidBlock> Marquee = block("scp024_marquee", () -> new Scp024SolidBlock(Scp024SolidBlock.sealed(MapColor.GOLD)
            .sound(SoundType.GLASS).lightLevel(State -> 15)));
    public static final DeferredBlock<Scp024SolidBlock> Curtain = solid("scp024_curtain", MapColor.COLOR_RED, SoundType.WOOL);

    public static final DeferredItem<Scp024DoorItem> StageDoorItem = ItemRegister.register("scp024_stage_door",
            () -> new Scp024DoorItem(StageDoor.get(), new Item.Properties().rarity(Rarity.EPIC), "block.scp_unleashed.scp024_stage_door"));
    public static final DeferredItem<Scp024DoorItem> ExitDoorItem = ItemRegister.register("scp024_exit_door",
            () -> new Scp024DoorItem(ExitDoor.get(), new Item.Properties().rarity(Rarity.RARE), "block.scp_unleashed.scp024_exit_door"));
    public static final DeferredItem<Scp024PrizeItem> DollHairs = ItemRegister.register("scp024_doll_hairs",
            () -> new Scp024PrizeItem(new Item.Properties().stacksTo(64), "item.scp_unleashed.scp024_doll_hairs", 2));
    public static final DeferredItem<Scp024PrizeItem> Ribbon = ItemRegister.register("scp024_participation_ribbon",
            () -> new Scp024PrizeItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON), "item.scp_unleashed.scp024_participation_ribbon", 2));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> PropComponent = ComponentRegister.registerComponentType("scp024_prop",
            Builder -> Builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp024Host>> Host = EntityTypeRegister.register("scp024_host",
            () -> EntityType.Builder.<Scp024Host>of(Scp024Host::new, MobCategory.MISC)
                    .sized(0.6f, 1.95f)
                    .eyeHeight(1.7f)
                    .fireImmune()
                    .noSummon()
                    .clientTrackingRange(8)
                    .build("scp024_host"));
    public static final DeferredHolder<EntityType<?>, EntityType<Scp024Spotlight>> Spotlight = EntityTypeRegister.register("scp024_spotlight",
            () -> EntityType.Builder.<Scp024Spotlight>of(Scp024Spotlight::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .fireImmune()
                    .noSummon()
                    .noSave()
                    .clientTrackingRange(6)
                    .updateInterval(2)
                    .build("scp024_spotlight"));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Confetti = ParticleRegister.register("scp024_confetti", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Streamer = ParticleRegister.register("scp024_streamer", () -> new SimpleParticleType(false));

    public static final DeferredHolder<SoundEvent, SoundEvent> DoorOpen = sound("scp024_door_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> DoorWhoosh = sound("scp024_door_whoosh");
    public static final DeferredHolder<SoundEvent, SoundEvent> Blast = sound("scp024_blast");
    public static final DeferredHolder<SoundEvent, SoundEvent> BlastFar = sound("scp024_blast_far");
    public static final DeferredHolder<SoundEvent, SoundEvent> Fuse = sound("scp024_fuse");
    public static final DeferredHolder<SoundEvent, SoundEvent> AnvilLand = sound("scp024_anvil");
    public static final DeferredHolder<SoundEvent, SoundEvent> Crumble = sound("scp024_crumble");
    public static final DeferredHolder<SoundEvent, SoundEvent> Trapdoor = sound("scp024_trapdoor");
    public static final DeferredHolder<SoundEvent, SoundEvent> Click = sound("scp024_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> SpotOn = sound("scp024_spot_on");
    public static final DeferredHolder<SoundEvent, SoundEvent> ConfettiPop = sound("scp024_confetti");
    public static final DeferredHolder<SoundEvent, SoundEvent> HostLaugh = sound("scp024_host_laugh");
    public static final DeferredHolder<SoundEvent, SoundEvent> Voice = sound("scp024_voice");
    public static final DeferredHolder<SoundEvent, SoundEvent> NoteBell = sound("scp024_note_bell");
    public static final DeferredHolder<SoundEvent, SoundEvent> NotePling = sound("scp024_note_pling");
    public static final DeferredHolder<SoundEvent, SoundEvent> NoteBit = sound("scp024_note_bit");
    public static final DeferredHolder<SoundEvent, SoundEvent> NoteBass = sound("scp024_note_bass");
    public static final DeferredHolder<SoundEvent, SoundEvent> NoteChime = sound("scp024_note_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> NoteXylophone = sound("scp024_note_xylophone");
    public static final DeferredHolder<SoundEvent, SoundEvent> NoteBuzzer = sound("scp024_note_buzzer");
    public static final DeferredHolder<SoundEvent, SoundEvent> NoteFlute = sound("scp024_note_flute");
    public static final DeferredHolder<SoundEvent, SoundEvent> NoteCowbell = sound("scp024_note_cowbell");
    public static final DeferredHolder<SoundEvent, SoundEvent> ClapScaffold = sound("scp024_crowd_clap_scaffold");
    public static final DeferredHolder<SoundEvent, SoundEvent> ClapBamboo = sound("scp024_crowd_clap_bamboo");
    public static final DeferredHolder<SoundEvent, SoundEvent> ClapFrame = sound("scp024_crowd_clap_frame");
    public static final DeferredHolder<SoundEvent, SoundEvent> ClapBook = sound("scp024_crowd_clap_book");
    public static final DeferredHolder<SoundEvent, SoundEvent> CheerVillager = sound("scp024_crowd_cheer_villager");
    public static final DeferredHolder<SoundEvent, SoundEvent> CheerPillager = sound("scp024_crowd_cheer_pillager");
    public static final DeferredHolder<SoundEvent, SoundEvent> CheerPiglin = sound("scp024_crowd_cheer_piglin");
    public static final DeferredHolder<SoundEvent, SoundEvent> CheerWitch = sound("scp024_crowd_cheer_witch");
    public static final DeferredHolder<SoundEvent, SoundEvent> CheerVindicator = sound("scp024_crowd_cheer_vindicator");
    public static final DeferredHolder<SoundEvent, SoundEvent> OohVillager = sound("scp024_crowd_ooh_villager");
    public static final DeferredHolder<SoundEvent, SoundEvent> OohWitch = sound("scp024_crowd_ooh_witch");
    public static final DeferredHolder<SoundEvent, SoundEvent> LaughWitch = sound("scp024_crowd_laugh_witch");
    public static final DeferredHolder<SoundEvent, SoundEvent> LaughPiglin = sound("scp024_crowd_laugh_piglin");
    public static final DeferredHolder<SoundEvent, SoundEvent> LaughVillager = sound("scp024_crowd_laugh_villager");
    public static final DeferredHolder<SoundEvent, SoundEvent> DrumSnare = sound("scp024_drum_snare");
    public static final DeferredHolder<SoundEvent, SoundEvent> DrumKick = sound("scp024_drum_kick");
    public static final DeferredHolder<SoundEvent, SoundEvent> DrumCrash = sound("scp024_drum_crash");
    public static final DeferredHolder<SoundEvent, SoundEvent> MusicFlute = sound("scp024_music_flute");
    public static final DeferredHolder<SoundEvent, SoundEvent> MusicBell = sound("scp024_music_bell");
    public static final DeferredHolder<SoundEvent, SoundEvent> MusicBit = sound("scp024_music_bit");
    public static final DeferredHolder<SoundEvent, SoundEvent> MusicChime = sound("scp024_music_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> MusicXylophone = sound("scp024_music_xylophone");
    public static final DeferredHolder<SoundEvent, SoundEvent> MusicBass = sound("scp024_music_bass");
    public static final DeferredHolder<SoundEvent, SoundEvent> MusicDrum = sound("scp024_music_drum");

    private Scp024Registry() {
    }

    private static <T extends Block> DeferredBlock<T> block(String Name, Supplier<T> Factory) {
        DeferredBlock<T> Registered = BlockRegister.register(Name, Factory);
        BlockItems.add(ItemRegister.register(Name, () -> new BlockItem(Registered.get(), new Item.Properties())));
        return Registered;
    }

    private static DeferredBlock<Scp024SolidBlock> solid(String Name, MapColor Colour, SoundType Sound) {
        return block(Name, () -> new Scp024SolidBlock(Scp024SolidBlock.sealed(Colour).sound(Sound)));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        ComponentRegister.register(ModEventBus);
        StartConditions.register(Scp024WanderCondition.TypeName, Scp024WanderCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp024Warehouse::spawn);
        Scp024Games.bootstrap();
    }
}
