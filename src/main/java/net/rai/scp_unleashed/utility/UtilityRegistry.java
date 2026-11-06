package net.rai.scp_unleashed.utility;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class UtilityRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    private static final List<DeferredItem<BlockItem>> CreativeOrder = new ArrayList<>();

    public static final DeferredBlock<CatwalkBlock> Catwalk = BlockRegister.register("catwalk", () -> new CatwalkBlock(grating()));
    public static final DeferredBlock<MetalGrateBlock> MetalGrate = BlockRegister.register("metal_grate", () -> new MetalGrateBlock(grating()));
    public static final DeferredBlock<CatwalkSlabBlock> CatwalkSlab = BlockRegister.register("catwalk_slab", () -> new CatwalkSlabBlock(grating()));
    public static final DeferredBlock<CatwalkStairsBlock> CatwalkStairs = BlockRegister.register("catwalk_stairs", () -> new CatwalkStairsBlock(MetalGrate.get().defaultBlockState(), grating()));
    public static final DeferredBlock<SafetyRailingBlock> SafetyRailing = BlockRegister.register("safety_railing", () -> new SafetyRailingBlock(steel(MapColor.METAL, 3.0f)));
    public static final DeferredBlock<SafetyRailingBlock> SafetyRailingYellow = BlockRegister.register("safety_railing_yellow", () -> new SafetyRailingBlock(steel(MapColor.COLOR_YELLOW, 3.0f)));
    public static final DeferredBlock<IndustrialLadderBlock> IndustrialLadder = BlockRegister.register("industrial_ladder", () -> new IndustrialLadderBlock(steel(MapColor.METAL, 2.0f).pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<CagedLadderBlock> CagedLadder = BlockRegister.register("caged_ladder", () -> new CagedLadderBlock(steel(MapColor.COLOR_YELLOW, 2.0f).pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<CagedLadderTopBlock> CagedLadderTop = BlockRegister.register("caged_ladder_top", () -> new CagedLadderTopBlock(steel(MapColor.COLOR_YELLOW, 2.0f).pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<SupportBeamBlock> SupportBeam = BlockRegister.register("support_beam", () -> new SupportBeamBlock(steel(MapColor.METAL, 5.0f).sound(SoundType.NETHERITE_BLOCK)));
    public static final DeferredBlock<SupportColumnBlock> SupportColumn = BlockRegister.register("support_column", () -> new SupportColumnBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(4.0f, 9.0f).sound(SoundType.STONE).noOcclusion()));
    public static final DeferredBlock<DecorPipeBlock> DecorPipe = BlockRegister.register("decor_pipe", () -> new DecorPipeBlock(steel(MapColor.COLOR_GRAY, 2.5f)));
    public static final DeferredBlock<DecorPipeElbowBlock> DecorPipeElbow = BlockRegister.register("decor_pipe_elbow", () -> new DecorPipeElbowBlock(steel(MapColor.COLOR_GRAY, 2.5f)));
    public static final DeferredBlock<DecorPipeValveBlock> DecorPipeValve = BlockRegister.register("decor_pipe_valve", () -> new DecorPipeValveBlock(steel(MapColor.COLOR_GRAY, 2.5f)));
    public static final DeferredBlock<CableTrayBlock> CableTray = BlockRegister.register("cable_tray", () -> new CableTrayBlock(steel(MapColor.METAL, 2.0f)));
    public static final DeferredBlock<ChainLinkFenceBlock> ChainLinkFence = BlockRegister.register("chain_link_fence", () -> new ChainLinkFenceBlock(steel(MapColor.METAL, 3.0f).sound(SoundType.CHAIN)));
    public static final DeferredBlock<ChainLinkGateBlock> ChainLinkGate = BlockRegister.register("chain_link_gate", () -> new ChainLinkGateBlock(steel(MapColor.METAL, 3.0f).sound(SoundType.CHAIN)));
    public static final DeferredBlock<CautionBarrierBlock> CautionBarrier = BlockRegister.register("caution_barrier", () -> new CautionBarrierBlock(steel(MapColor.COLOR_YELLOW, 1.5f)));
    public static final DeferredBlock<BollardBlock> Bollard = BlockRegister.register("bollard", () -> new BollardBlock(steel(MapColor.COLOR_YELLOW, 4.0f)));
    public static final DeferredBlock<JerseyBarrierBlock> JerseyBarrier = BlockRegister.register("jersey_barrier", () -> new JerseyBarrierBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_LIGHT_GRAY).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0f, 9.0f).sound(SoundType.STONE).noOcclusion()));
    public static final DeferredBlock<WarningBeaconBlock> WarningBeacon = BlockRegister.register("warning_beacon", () -> new WarningBeaconBlock(BlockBehaviour.Properties.of()
            .mapColor(State -> State.getValue(WarningBeaconBlock.Lit) ? MapColor.COLOR_ORANGE : MapColor.COLOR_BLACK).requiresCorrectToolForDrops().strength(2.0f, 6.0f)
            .sound(SoundType.LANTERN).noOcclusion().pushReaction(PushReaction.DESTROY)
            .lightLevel(State -> State.getValue(WarningBeaconBlock.Lit) ? WarningBeaconBlock.Brightness : 0)));

    public static final DeferredItem<BlockItem> CatwalkItem = item("catwalk", Catwalk);
    public static final DeferredItem<BlockItem> CatwalkSlabItem = item("catwalk_slab", CatwalkSlab);
    public static final DeferredItem<BlockItem> CatwalkStairsItem = item("catwalk_stairs", CatwalkStairs);
    public static final DeferredItem<BlockItem> MetalGrateItem = item("metal_grate", MetalGrate);
    public static final DeferredItem<BlockItem> SafetyRailingItem = item("safety_railing", SafetyRailing);
    public static final DeferredItem<BlockItem> SafetyRailingYellowItem = item("safety_railing_yellow", SafetyRailingYellow);
    public static final DeferredItem<BlockItem> IndustrialLadderItem = item("industrial_ladder", IndustrialLadder);
    public static final DeferredItem<BlockItem> CagedLadderItem = item("caged_ladder", CagedLadder);
    public static final DeferredItem<BlockItem> CagedLadderTopItem = item("caged_ladder_top", CagedLadderTop);
    public static final DeferredItem<BlockItem> SupportBeamItem = item("support_beam", SupportBeam);
    public static final DeferredItem<BlockItem> SupportColumnItem = item("support_column", SupportColumn);
    public static final DeferredItem<BlockItem> DecorPipeItem = item("decor_pipe", DecorPipe);
    public static final DeferredItem<BlockItem> DecorPipeElbowItem = item("decor_pipe_elbow", DecorPipeElbow);
    public static final DeferredItem<BlockItem> DecorPipeValveItem = item("decor_pipe_valve", DecorPipeValve);
    public static final DeferredItem<BlockItem> CableTrayItem = item("cable_tray", CableTray);
    public static final DeferredItem<BlockItem> ChainLinkFenceItem = item("chain_link_fence", ChainLinkFence);
    public static final DeferredItem<BlockItem> ChainLinkGateItem = item("chain_link_gate", ChainLinkGate);
    public static final DeferredItem<BlockItem> CautionBarrierItem = item("caution_barrier", CautionBarrier);
    public static final DeferredItem<BlockItem> BollardItem = item("bollard", Bollard);
    public static final DeferredItem<BlockItem> JerseyBarrierItem = item("jersey_barrier", JerseyBarrier);
    public static final DeferredItem<BlockItem> WarningBeaconItem = item("warning_beacon", WarningBeacon);

    private UtilityRegistry() {
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
    }

    public static List<DeferredItem<BlockItem>> creativeOrder() {
        return Collections.unmodifiableList(CreativeOrder);
    }

    private static DeferredItem<BlockItem> item(String Name, DeferredBlock<? extends Block> Placed) {
        DeferredItem<BlockItem> Registered = ItemRegister.registerSimpleBlockItem(Name, Placed);
        CreativeOrder.add(Registered);
        return Registered;
    }

    private static BlockBehaviour.Properties grating() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.0f, 6.0f).sound(SoundType.COPPER_GRATE).noOcclusion()
                .isValidSpawn((State, World, Pos, Type) -> false).isRedstoneConductor((State, World, Pos) -> false)
                .isSuffocating((State, World, Pos) -> false).isViewBlocking((State, World, Pos) -> false);
    }

    private static BlockBehaviour.Properties steel(MapColor Colour, float Hardness) {
        return BlockBehaviour.Properties.of().mapColor(Colour).requiresCorrectToolForDrops().strength(Hardness, 6.0f).sound(SoundType.METAL).noOcclusion();
    }
}
