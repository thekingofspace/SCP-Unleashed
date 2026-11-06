package net.rai.scp_unleashed.scp093;

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
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.stranger.StrangerEncounters;

import java.util.List;

public final class Scp093Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_093");
    public static final ResourceKey<DamageType> TearsDamage = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp093_tears"));
    public static final ResourceKey<DamageType> TorsoDamage = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp093_torso"));
    public static final ResourceKey<Structure> City = ResourceKey.create(Registries.STRUCTURE, location("scp093_city"));
    public static final ResourceKey<Structure> Shack = ResourceKey.create(Registries.STRUCTURE, location("scp093_shack"));

    public static final DeferredHolder<SoundEvent, SoundEvent> DiscSeat = sound("scp093_disc_seat");
    public static final DeferredHolder<SoundEvent, SoundEvent> DiscSeatChime = sound("scp093_disc_seat_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> DiscRemove = sound("scp093_disc_remove");
    public static final DeferredHolder<SoundEvent, SoundEvent> MirrorHum = sound("scp093_mirror_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> MirrorHumLow = sound("scp093_mirror_hum_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> MirrorBlank = sound("scp093_mirror_blank");
    public static final DeferredHolder<SoundEvent, SoundEvent> Transit = sound("scp093_transit");
    public static final DeferredHolder<SoundEvent, SoundEvent> TransitRush = sound("scp093_transit_rush");
    public static final DeferredHolder<SoundEvent, SoundEvent> Arrive = sound("scp093_arrive");
    public static final DeferredHolder<SoundEvent, SoundEvent> ArriveWind = sound("scp093_arrive_wind");
    public static final DeferredHolder<SoundEvent, SoundEvent> Drink = sound("scp093_drink");
    public static final DeferredHolder<SoundEvent, SoundEvent> DrinkChime = sound("scp093_drink_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> Whisper = sound("scp093_whisper");
    public static final DeferredHolder<SoundEvent, SoundEvent> VisionSting = sound("scp093_vision_sting");
    public static final DeferredHolder<SoundEvent, SoundEvent> Claimed = sound("scp093_claimed");
    public static final DeferredHolder<SoundEvent, SoundEvent> ClaimedChoir = sound("scp093_claimed_choir");
    public static final DeferredHolder<SoundEvent, SoundEvent> TorsoAmbient = sound("scp093_torso_ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> TorsoBreath = sound("scp093_torso_breath");
    public static final DeferredHolder<SoundEvent, SoundEvent> TorsoDrag = sound("scp093_torso_drag");
    public static final DeferredHolder<SoundEvent, SoundEvent> TorsoDragThud = sound("scp093_torso_drag_thud");
    public static final DeferredHolder<SoundEvent, SoundEvent> TorsoThud = sound("scp093_torso_thud");
    public static final DeferredHolder<SoundEvent, SoundEvent> TorsoThudBoom = sound("scp093_torso_thud_boom");
    public static final DeferredHolder<SoundEvent, SoundEvent> TorsoThudClank = sound("scp093_torso_thud_clank");
    public static final DeferredHolder<SoundEvent, SoundEvent> TorsoThudThump = sound("scp093_torso_thud_thump");
    public static final DeferredHolder<SoundEvent, SoundEvent> TorsoSwipe = sound("scp093_torso_swipe");
    public static final DeferredHolder<SoundEvent, SoundEvent> TorsoSwipeHeavy = sound("scp093_torso_swipe_heavy");
    public static final DeferredHolder<SoundEvent, SoundEvent> TorsoSlam = sound("scp093_torso_slam");
    public static final DeferredHolder<SoundEvent, SoundEvent> TorsoSlamImpact = sound("scp093_torso_slam_impact");
    public static final DeferredHolder<SoundEvent, SoundEvent> TorsoHurt = sound("scp093_torso_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> TorsoHurtGroan = sound("scp093_torso_hurt_groan");
    public static final DeferredHolder<SoundEvent, SoundEvent> TorsoDeath = sound("scp093_torso_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> TorsoDeathCollapse = sound("scp093_torso_death_collapse");
    public static final DeferredHolder<SoundEvent, SoundEvent> TorsoRise = sound("scp093_torso_rise");
    public static final DeferredHolder<SoundEvent, SoundEvent> TorsoRiseGrind = sound("scp093_torso_rise_grind");
    public static final DeferredHolder<SoundEvent, SoundEvent> AmbientLoop = sound("scp093_ambient_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> Wind = sound("scp093_wind");
    public static final DeferredHolder<SoundEvent, SoundEvent> Mood = sound("scp093_mood");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SeatLayers = List.of(DiscSeat, DiscSeatChime);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HumLayers = List.of(MirrorHum, MirrorHumLow);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> TransitLayers = List.of(Transit, TransitRush);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ArriveLayers = List.of(Arrive, ArriveWind);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> DrinkLayers = List.of(Drink, DrinkChime);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ClaimLayers = List.of(Claimed, ClaimedChoir);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> AmbientLayers = List.of(TorsoAmbient, TorsoBreath);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SwipeLayers = List.of(TorsoSwipe, TorsoSwipeHeavy);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SlamLayers = List.of(TorsoSlam, TorsoSlamImpact);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HurtLayers = List.of(TorsoHurt, TorsoHurtGroan);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> DeathLayers = List.of(TorsoDeath, TorsoDeathCollapse);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> RiseLayers = List.of(TorsoRise, TorsoRiseGrind);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ThudLayers = List.of(TorsoThud, TorsoThudBoom, TorsoThudClank, TorsoThudThump, TorsoDragThud, TorsoDrag);

    public static final DeferredBlock<Scp093MirrorBlock> StandingMirror = BlockRegister.register("scp093_mirror",
            () -> new Scp093MirrorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(2.0f, 6.0f).sound(SoundType.WOOD).noOcclusion()
                    .pushReaction(PushReaction.BLOCK).lightLevel(State -> State.getValue(Scp093MirrorBlock.Seated) ? Scp093MirrorBlock.SeatedLight : 0)
                    .isRedstoneConductor((State, World, Pos) -> false).isSuffocating((State, World, Pos) -> false).isViewBlocking((State, World, Pos) -> false)));
    public static final DeferredBlock<Scp093ReturnMirrorBlock> ReturnMirror = BlockRegister.register("scp093_return_mirror",
            () -> new Scp093ReturnMirrorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BROWN).strength(-1.0f, 3600000.0f).sound(SoundType.WOOD)
                    .noOcclusion().noLootTable().pushReaction(PushReaction.BLOCK).lightLevel(State -> Scp093ReturnMirrorBlock.Light)
                    .isValidSpawn((State, World, Pos, Kind) -> false).isRedstoneConductor((State, World, Pos) -> false)
                    .isSuffocating((State, World, Pos) -> false).isViewBlocking((State, World, Pos) -> false)));

    public static final DeferredItem<Scp093DiscItem> Disc = ItemRegister.register("scp093_disc",
            () -> new Scp093DiscItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredItem<BlockItem> MirrorItem = ItemRegister.register("scp093_mirror",
            () -> new Scp093MirrorItem(StandingMirror.get(), new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Scp093TearsItem> Tears = ItemRegister.register("scp093_tears",
            () -> new Scp093TearsItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp093MirrorBlockEntity>> MirrorEntity = BlockEntityTypeRegister.register("scp093_mirror",
            () -> BlockEntityType.Builder.of(Scp093MirrorBlockEntity::new, StandingMirror.get()).build(null));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp093Torso>> Torso = EntityTypeRegister.register("scp093_torso",
            () -> EntityType.Builder.<Scp093Torso>of(Scp093Torso::new, MobCategory.MONSTER).sized(Scp093Torso.Width, Scp093Torso.Height).eyeHeight(Scp093Torso.EyeHeight)
                    .clientTrackingRange(12).build("scp093_torso"));

    public static final DeferredItem<DeferredSpawnEggItem> TorsoEgg = ItemRegister.register("scp093_torso_spawn_egg",
            () -> new DeferredSpawnEggItem(Torso, 0xFFFFFF, 0xFFFFFF, new Item.Properties()));

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Scp093Doses>> Doses = AttachmentRegister.register("scp093_doses",
            () -> AttachmentType.builder(() -> Scp093Doses.None).serialize(Scp093Doses.DoseCodec, Saved -> Saved.Amount() > 0.0f || Saved.ClaimAt() > 0L).build());

    private Scp093Registry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
    }
}
