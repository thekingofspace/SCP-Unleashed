package net.rai.scp_unleashed.scp2317;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.scp001scarlet.ScarletMeter;
import net.rai.scp_unleashed.scp2317.note.Scp2317RiteNoteRegistry;
import net.rai.scp_unleashed.scp2317.world.Scp2317ChunkGenerator;
import net.rai.scp_unleashed.stranger.StrangerEncounters;

import java.util.List;

public final class Scp2317Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> GeneratorRegister = DeferredRegister.create(Registries.CHUNK_GENERATOR, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_2317");
    public static final float DoorHardness = 4.0f;
    public static final float DoorResistance = 1200.0f;
    public static final float SealedResistance = 3600000.0f;
    public static final float FarRange = 64.0f;

    public static final DeferredBlock<Scp2317DoorBlock> Door = BlockRegister.register("scp2317_door",
            () -> new Scp2317DoorBlock(doorProperties().strength(DoorHardness, DoorResistance).requiresCorrectToolForDrops(), false));
    public static final DeferredBlock<Scp2317DoorBlock> FarDoor = BlockRegister.register("scp2317_far_door",
            () -> new Scp2317DoorBlock(doorProperties().strength(-1.0f, SealedResistance).noLootTable(), true));
    public static final DeferredBlock<Scp2317VaseBlock> Vase = BlockRegister.register("scp2317_vase",
            () -> new Scp2317VaseBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_RED).sound(SoundType.DECORATED_POT).strength(-1.0f, SealedResistance)
                    .noLootTable().noOcclusion().pushReaction(PushReaction.BLOCK).lightLevel(Scp2317VaseBlock::lightOf)));
    public static final DeferredBlock<Scp2317KingBlock> King = BlockRegister.register("scp2317_dormant_king",
            () -> new Scp2317KingBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).sound(SoundType.STONE).strength(-1.0f, SealedResistance)
                    .noLootTable().noOcclusion().noCollission().pushReaction(PushReaction.BLOCK).lightLevel(State -> 3)));
    public static final DeferredItem<BlockItem> DoorItem = ItemRegister.register("scp2317_door",
            () -> new BlockItem(Door.get(), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp2317DoorBlockEntity>> DoorEntity = BlockEntityTypeRegister.register("scp2317_door",
            () -> BlockEntityType.Builder.of(Scp2317DoorBlockEntity::new, Door.get(), FarDoor.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp2317KingBlockEntity>> KingEntity = BlockEntityTypeRegister.register("scp2317_dormant_king",
            () -> BlockEntityType.Builder.of(Scp2317KingBlockEntity::new, King.get()).build(null));

    public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<Scp2317ChunkGenerator>> Generator = GeneratorRegister.register("scp2317_desert",
            () -> Scp2317ChunkGenerator.GeneratorCodec);

    public static final DeferredHolder<SoundEvent, SoundEvent> Rattle = sound("scp2317_door_rattle");
    public static final DeferredHolder<SoundEvent, SoundEvent> RattleIron = sound("scp2317_door_rattle_iron");
    public static final DeferredHolder<SoundEvent, SoundEvent> Open = sound("scp2317_door_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> OpenStone = sound("scp2317_door_open_stone");
    public static final DeferredHolder<SoundEvent, SoundEvent> Shut = sound("scp2317_door_shut");
    public static final DeferredHolder<SoundEvent, SoundEvent> ShutThud = sound("scp2317_door_shut_thud");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hum = sound("scp2317_door_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> Pass = sound("scp2317_pass");
    public static final DeferredHolder<SoundEvent, SoundEvent> PassSand = sound("scp2317_pass_sand");
    public static final DeferredHolder<SoundEvent, SoundEvent> Wind = sound("scp2317_wind");
    public static final DeferredHolder<SoundEvent, SoundEvent> Accept = sound("scp2317_vase_accept");
    public static final DeferredHolder<SoundEvent, SoundEvent> AcceptDrip = sound("scp2317_vase_drip");
    public static final DeferredHolder<SoundEvent, SoundEvent> Refuse = sound("scp2317_vase_refuse");
    public static final DeferredHolder<SoundEvent, SoundEvent> Turn = sound("scp2317_rite_turn");
    public static final DeferredHolder<SoundEvent, SoundEvent> Exhale = farSound("scp2317_rite_exhale");
    public static final DeferredHolder<SoundEvent, SoundEvent> ExhaleRumble = farSound("scp2317_rite_rumble");
    public static final DeferredHolder<SoundEvent, SoundEvent> Snap = sound("scp2317_chain_snap");
    public static final DeferredHolder<SoundEvent, SoundEvent> SnapRing = sound("scp2317_chain_ring");
    public static final DeferredHolder<SoundEvent, SoundEvent> Stir = sound("scp2317_king_stir");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> RattleLayers = List.of(Rattle, RattleIron);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> OpenLayers = List.of(Open, OpenStone);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ShutLayers = List.of(Shut, ShutThud);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> PassLayers = List.of(Pass, PassSand);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> AcceptLayers = List.of(Accept, AcceptDrip);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ExhaleLayers = List.of(Exhale, ExhaleRumble);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SnapLayers = List.of(Snap, SnapRing);

    private Scp2317Registry() {
    }

    private static BlockBehaviour.Properties doorProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.STONE).noOcclusion()
                .pushReaction(PushReaction.BLOCK).isValidSpawn((State, Getter, Pos, Type) -> false).isRedstoneConductor((State, Getter, Pos) -> false)
                .isSuffocating((State, Getter, Pos) -> false).isViewBlocking((State, Getter, Pos) -> false).lightLevel(Scp2317DoorBlock::lightOf);
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> farSound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createFixedRangeEvent(location(Name), FarRange));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        GeneratorRegister.register(ModEventBus);
        Scp2317RiteNoteRegistry.register(ModEventBus);
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
        ScarletMeter.listen(Scp2317DoorBlockEntity::snapped);
    }
}
