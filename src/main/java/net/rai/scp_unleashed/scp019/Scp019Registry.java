package net.rai.scp_unleashed.scp019;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
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
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.stranger.StrangerEncounters;

public final class Scp019Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_019");

    public static final DeferredHolder<SoundEvent, SoundEvent> PotOpen = sound("scp019_pot_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> PotOpenScrape = sound("scp019_pot_open_scrape");
    public static final DeferredHolder<SoundEvent, SoundEvent> PotClose = sound("scp019_pot_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> PotCloseSeat = sound("scp019_pot_close_seat");
    public static final DeferredHolder<SoundEvent, SoundEvent> Puff = sound("scp019_puff");
    public static final DeferredHolder<SoundEvent, SoundEvent> PuffHiss = sound("scp019_puff_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> Emerge = sound("scp019_emerge");
    public static final DeferredHolder<SoundEvent, SoundEvent> EmergeScrabble = sound("scp019_emerge_scrabble");
    public static final DeferredHolder<SoundEvent, SoundEvent> Enter = sound("scp019_enter");
    public static final DeferredHolder<SoundEvent, SoundEvent> EnterScrabble = sound("scp019_enter_scrabble");
    public static final DeferredHolder<SoundEvent, SoundEvent> DroneSkitter = sound("scp019_drone_skitter");
    public static final DeferredHolder<SoundEvent, SoundEvent> DronePatter = sound("scp019_drone_patter");
    public static final DeferredHolder<SoundEvent, SoundEvent> DroneChitter = sound("scp019_drone_chitter");
    public static final DeferredHolder<SoundEvent, SoundEvent> DroneHurt = sound("scp019_drone_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> DroneDeath = sound("scp019_drone_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> DroneSqueal = sound("scp019_drone_squeal");
    public static final DeferredHolder<SoundEvent, SoundEvent> DroneSquealShrill = sound("scp019_drone_squeal_shrill");
    public static final DeferredHolder<SoundEvent, SoundEvent> DroneSquealCry = sound("scp019_drone_squeal_cry");
    public static final DeferredHolder<SoundEvent, SoundEvent> BruteStep = sound("scp019_brute_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> BruteThud = sound("scp019_brute_thud");
    public static final DeferredHolder<SoundEvent, SoundEvent> BruteGrunt = sound("scp019_brute_grunt");
    public static final DeferredHolder<SoundEvent, SoundEvent> BruteRoar = sound("scp019_brute_roar");
    public static final DeferredHolder<SoundEvent, SoundEvent> BruteRoarLow = sound("scp019_brute_roar_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> BruteAttack = sound("scp019_brute_attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> BruteAttackSwing = sound("scp019_brute_attack_swing");
    public static final DeferredHolder<SoundEvent, SoundEvent> BruteHurt = sound("scp019_brute_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> BruteDeath = sound("scp019_brute_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> BruteEmerge = sound("scp019_brute_emerge");
    public static final DeferredHolder<SoundEvent, SoundEvent> BruteEmergeLow = sound("scp019_brute_emerge_low");

    public static final DeferredBlock<Scp019PotBlock> Pot = BlockRegister.register("scp019_pot", () -> new Scp019PotBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLACK).instrument(NoteBlockInstrument.BASEDRUM).strength(3.0f, 1200.0f).sound(SoundType.DECORATED_POT)
            .requiresCorrectToolForDrops().noOcclusion().pushReaction(PushReaction.BLOCK).isValidSpawn((State, World, Pos, Kind) -> false)
            .isRedstoneConductor((State, World, Pos) -> false)));
    public static final DeferredItem<BlockItem> PotItem = ItemRegister.register("scp019_pot",
            () -> new BlockItem(Pot.get(), new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp019PotBlockEntity>> PotEntity = BlockEntityTypeRegister.register("scp019_pot",
            () -> BlockEntityType.Builder.of(Scp019PotBlockEntity::new, Pot.get()).build(null));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp019Drone>> Drone = EntityTypeRegister.register("scp019_drone",
            () -> EntityType.Builder.<Scp019Drone>of(Scp019Drone::new, MobCategory.MONSTER).sized(0.45f, 0.65f).eyeHeight(0.5f).clientTrackingRange(10).build("scp019_drone"));
    public static final DeferredHolder<EntityType<?>, EntityType<Scp019Brute>> Brute = EntityTypeRegister.register("scp019_brute",
            () -> EntityType.Builder.<Scp019Brute>of(Scp019Brute::new, MobCategory.MONSTER).sized(1.4f, 1.4f).eyeHeight(1.25f).clientTrackingRange(10).build("scp019_brute"));

    public static final DeferredItem<DeferredSpawnEggItem> DroneEgg = ItemRegister.register("scp019_drone_spawn_egg",
            () -> new DeferredSpawnEggItem(Drone, 0xFFFFFF, 0xFFFFFF, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> BruteEgg = ItemRegister.register("scp019_brute_spawn_egg",
            () -> new DeferredSpawnEggItem(Brute, 0xFFFFFF, 0xFFFFFF, new Item.Properties()));

    private Scp019Registry() {
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
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
    }
}
