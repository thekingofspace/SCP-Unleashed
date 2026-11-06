package net.rai.scp_unleashed.containmentzone;

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
import net.rai.scp_unleashed.containmentzone.shutter.ShutterRegistry;
import net.rai.scp_unleashed.decor.DecorRegistry;
import net.rai.scp_unleashed.decor.FacilityFloorMarkBlock;
import net.rai.scp_unleashed.decor.FacilityLampBlock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public final class ContainmentZoneRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final String Prefix = "containment_room_";
    public static final int AlarmBrightness = 12;
    public static final int PanelBrightness = 3;
    public static final int BreachBrightness = 6;

    private static final List<DeferredItem<BlockItem>> CreativeOrder = new ArrayList<>();

    public static final DeferredBlock<FacilityFloorMarkBlock> FloorGrate = block("floor_grate", () -> new FacilityFloorMarkBlock(zone(MapColor.STONE, SoundType.METAL)));
    public static final DeferredBlock<FacilityLampBlock> RedLamp = block("lamp_red", () -> new FacilityLampBlock(zone(MapColor.COLOR_BLACK, SoundType.GLASS)
            .mapColor(State -> State.getValue(FacilityLampBlock.Lit) ? MapColor.COLOR_RED : MapColor.COLOR_BLACK)
            .instrument(NoteBlockInstrument.HAT).lightLevel(State -> State.getValue(FacilityLampBlock.Lit) ? FacilityLampBlock.Brightness : 0)));
    public static final DeferredBlock<CellStackBlock> ObservationBay = block("observation_bay", () -> new CellStackBlock(CellStackKind.ObservationBay, prop(MapColor.COLOR_GRAY, 3.0f, SoundType.METAL)));
    public static final DeferredBlock<CellPlaqueBlock> CellPlaque = block("cell_plaque", () -> new CellPlaqueBlock(prop(MapColor.COLOR_YELLOW, 1.0f, SoundType.METAL).pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<CellPropBlock> RestraintAnchor = block("restraint_anchor", () -> new CellPropBlock(CellPropKind.RestraintAnchor, prop(MapColor.METAL, 3.0f, SoundType.CHAIN)));
    public static final DeferredBlock<CellPropBlock> SedationVent = block("sedation_vent", () -> new CellPropBlock(CellPropKind.SedationVent, prop(MapColor.COLOR_GRAY, 2.0f, SoundType.METAL).pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<CellSignalBlock> StatusPanel = block("status_panel", () -> new CellSignalBlock(CellPropKind.StatusPanel, prop(MapColor.COLOR_GRAY, 1.5f, SoundType.METAL)
            .pushReaction(PushReaction.DESTROY).lightLevel(State -> State.getValue(CellSignalBlock.Powered) ? BreachBrightness : PanelBrightness)));
    public static final DeferredBlock<CellStackBlock> SpecimenTank = block("specimen_tank", () -> new CellStackBlock(CellStackKind.SpecimenTank, prop(MapColor.COLOR_CYAN, 2.0f, SoundType.GLASS)));
    public static final DeferredBlock<CellStackBlock> PaddedPanel = block("padded_panel", () -> new CellStackBlock(CellStackKind.PaddedPanel, prop(MapColor.COLOR_LIGHT_GRAY, 1.2f, SoundType.WOOL)));
    public static final DeferredBlock<CellSignBlock> HazardSign = block("hazard_sign", () -> new CellSignBlock(prop(MapColor.COLOR_RED, 1.0f, SoundType.METAL).pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<CellSignalBlock> AlarmLight = block("alarm_light", () -> new CellSignalBlock(CellPropKind.AlarmLight, prop(MapColor.COLOR_RED, 1.5f, SoundType.METAL)
            .pushReaction(PushReaction.DESTROY).lightLevel(State -> State.getValue(CellSignalBlock.Powered) ? AlarmBrightness : 0)));
    public static final DeferredBlock<CellShutterBlock> LockdownShutter = block("lockdown_shutter", () -> new CellShutterBlock(prop(MapColor.METAL, 3.0f, SoundType.METAL).pushReaction(PushReaction.BLOCK)
            .dynamicShape().isRedstoneConductor((State, World, Pos) -> false).isSuffocating((State, World, Pos) -> false).isViewBlocking((State, World, Pos) -> false)));

    private ContainmentZoneRegistry() {
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        ShutterRegistry.register(ModEventBus);
    }

    public static List<DeferredItem<BlockItem>> creativeOrder() {
        return Collections.unmodifiableList(CreativeOrder);
    }

    private static <B extends Block> DeferredBlock<B> block(String Name, Supplier<B> Factory) {
        DeferredBlock<B> Registered = BlockRegister.register(Prefix + Name, Factory);
        CreativeOrder.add(ItemRegister.registerSimpleBlockItem(Prefix + Name, Registered));
        return Registered;
    }

    private static BlockBehaviour.Properties zone(MapColor Colour, SoundType Sound) {
        return BlockBehaviour.Properties.of().mapColor(Colour).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops()
                .strength(DecorRegistry.Hardness, DecorRegistry.BlastResistance).sound(Sound);
    }

    private static BlockBehaviour.Properties prop(MapColor Colour, float Hardness, SoundType Sound) {
        return BlockBehaviour.Properties.of().mapColor(Colour).strength(Hardness, Hardness * 2.0f).sound(Sound).noOcclusion();
    }
}
