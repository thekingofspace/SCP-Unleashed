package net.rai.scp_unleashed.scp860;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
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
import net.rai.scp_unleashed.stasis.StasisPods;
import net.rai.scp_unleashed.stranger.StrangerEncounters;

import java.util.List;

public final class Scp860Registry {
    public static final float RoarRange = 96.0f;
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_860");
    public static final ResourceKey<Level> Forest = ResourceKey.create(Registries.DIMENSION, location("scp860_forest"));
    public static final float DoorHardness = 5.0f;
    public static final float DoorResistance = 1200.0f;
    public static final int PortalLight = 6;

    public static final DeferredBlock<Scp860DoorBlock> Door = BlockRegister.register("scp860_door",
            () -> new Scp860DoorBlock(doorProperties().strength(DoorHardness, DoorResistance).requiresCorrectToolForDrops(), false));
    public static final DeferredBlock<Scp860DoorBlock> FarDoor = BlockRegister.register("scp860_far_door",
            () -> new Scp860DoorBlock(doorProperties().strength(-1.0f, 3600000.0f).noLootTable(), true));
    public static final DeferredItem<BlockItem> DoorItem = ItemRegister.register("scp860_door",
            () -> new BlockItem(Door.get(), new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<Scp860KeyItem> Key = ItemRegister.register("scp860_key",
            () -> new Scp860KeyItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp860DoorBlockEntity>> DoorEntity = BlockEntityTypeRegister.register("scp860_door",
            () -> BlockEntityType.Builder.of(Scp860DoorBlockEntity::new, Door.get(), FarDoor.get()).build(null));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp860Watcher>> Watcher = EntityTypeRegister.register("scp860_2",
            () -> EntityType.Builder.<Scp860Watcher>of(Scp860Watcher::new, MobCategory.MONSTER)
                    .sized(Scp860Watcher.Width, Scp860Watcher.Height)
                    .eyeHeight(Scp860Watcher.EyeHeight)
                    .clientTrackingRange(10)
                    .build("scp860_2"));
    public static final DeferredItem<DeferredSpawnEggItem> Egg = ItemRegister.register("scp860_2_spawn_egg",
            () -> new DeferredSpawnEggItem(Watcher, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.RARE)));

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Scp860Record>> Ledger = AttachmentRegister.register("scp860_trip",
            () -> AttachmentType.serializable(Scp860Record::new).copyOnDeath().build());

    public static final DeferredHolder<SoundEvent, SoundEvent> Rattle = sound("scp860_door_rattle");
    public static final DeferredHolder<SoundEvent, SoundEvent> RattleChain = sound("scp860_door_rattle_chain");
    public static final DeferredHolder<SoundEvent, SoundEvent> KeyIn = sound("scp860_key_in");
    public static final DeferredHolder<SoundEvent, SoundEvent> KeyTurn = sound("scp860_key_turn");
    public static final DeferredHolder<SoundEvent, SoundEvent> KeyOut = sound("scp860_key_out");
    public static final DeferredHolder<SoundEvent, SoundEvent> DoorOpen = sound("scp860_door_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> DoorCreak = sound("scp860_door_creak");
    public static final DeferredHolder<SoundEvent, SoundEvent> DoorShut = sound("scp860_door_shut");
    public static final DeferredHolder<SoundEvent, SoundEvent> DoorThud = sound("scp860_door_thud");
    public static final DeferredHolder<SoundEvent, SoundEvent> PortalHum = sound("scp860_portal_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> Enter = sound("scp860_enter");
    public static final DeferredHolder<SoundEvent, SoundEvent> EnterLeaves = sound("scp860_enter_leaves");
    public static final DeferredHolder<SoundEvent, SoundEvent> Leave = sound("scp860_leave");
    public static final DeferredHolder<SoundEvent, SoundEvent> LeaveDoor = sound("scp860_leave_door");
    public static final DeferredHolder<SoundEvent, SoundEvent> Rustle = sound("scp860_rustle");
    public static final DeferredHolder<SoundEvent, SoundEvent> RustleTwig = sound("scp860_rustle_twig");
    public static final DeferredHolder<SoundEvent, SoundEvent> Growl = sound("scp860_growl");
    public static final DeferredHolder<SoundEvent, SoundEvent> GrowlRumble = sound("scp860_growl_rumble");
    public static final DeferredHolder<SoundEvent, SoundEvent> Roar = farSound("scp860_roar");
    public static final DeferredHolder<SoundEvent, SoundEvent> RoarBellow = farSound("scp860_roar_bellow");
    public static final DeferredHolder<SoundEvent, SoundEvent> RoarRasp = farSound("scp860_roar_rasp");
    public static final DeferredHolder<SoundEvent, SoundEvent> Voice = sound("scp860_voice");
    public static final DeferredHolder<SoundEvent, SoundEvent> VoiceCreak = sound("scp860_voice_creak");
    public static final DeferredHolder<SoundEvent, SoundEvent> Step = sound("scp860_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> StepPad = sound("scp860_step_pad");
    public static final DeferredHolder<SoundEvent, SoundEvent> Attack = sound("scp860_attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> AttackSnap = sound("scp860_attack_snap");
    public static final DeferredHolder<SoundEvent, SoundEvent> Pounce = sound("scp860_pounce");
    public static final DeferredHolder<SoundEvent, SoundEvent> PounceRush = sound("scp860_pounce_rush");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hurt = sound("scp860_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> HurtCrack = sound("scp860_hurt_crack");
    public static final DeferredHolder<SoundEvent, SoundEvent> Death = sound("scp860_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> DeathCrack = sound("scp860_death_crack");
    public static final DeferredHolder<SoundEvent, SoundEvent> Wind = sound("scp860_wind");
    public static final DeferredHolder<SoundEvent, SoundEvent> Creak = sound("scp860_creak");
    public static final DeferredHolder<SoundEvent, SoundEvent> Snap = sound("scp860_snap");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> RattleLayers = List.of(Rattle, RattleChain);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> KeyInLayers = List.of(KeyIn, KeyTurn);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> KeyOutLayers = List.of(KeyOut, KeyTurn);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> OpenLayers = List.of(DoorOpen, DoorCreak);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ShutLayers = List.of(DoorShut, DoorThud);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> EnterLayers = List.of(Enter, EnterLeaves);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> LeaveLayers = List.of(Leave, LeaveDoor);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> RustleLayers = List.of(Rustle, RustleTwig);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> GrowlLayers = List.of(Growl, GrowlRumble);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> RoarLayers = List.of(Roar, RoarBellow, RoarRasp);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> VoiceLayers = List.of(Voice, VoiceCreak);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> StepLayers = List.of(Step, StepPad);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> AttackLayers = List.of(Attack, AttackSnap);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> PounceLayers = List.of(Pounce, PounceRush);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HurtLayers = List.of(Hurt, HurtCrack);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> DeathLayers = List.of(Death, DeathCrack);

    private Scp860Registry() {
    }

    private static BlockBehaviour.Properties doorProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.STONE).noOcclusion()
                .pushReaction(PushReaction.BLOCK).isValidSpawn((State, Getter, Pos, Type) -> false).isRedstoneConductor((State, Getter, Pos) -> false)
                .isSuffocating((State, Getter, Pos) -> false).isViewBlocking((State, Getter, Pos) -> false).lightLevel(Scp860DoorBlock::lightOf);
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> farSound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createFixedRangeEvent(location(Name), RoarRange));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
        Scp860Stranger.register();
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
        StasisPods.addReleaseHook((World, Spawned) -> {
            if (Spawned instanceof Scp860Watcher Creature) {
                Creature.released(World);
            }
        });
    }
}
