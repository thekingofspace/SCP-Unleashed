package net.rai.scp_unleashed.greenhouse;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TransparentBlock;
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
import java.util.function.Supplier;

public final class GreenhouseRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final float GlassHardness = 0.3f;
    public static final float GlassWallHardness = 1.5f;
    public static final float GlassWallResistance = 6.0f;
    public static final int GrowRackGlow = 6;
    public static final int PanelGlow = 3;

    private static final List<DeferredItem<BlockItem>> CreativeOrder = new ArrayList<>();

    public static final DeferredBlock<TransparentBlock> Glass = block("greenhouse_glass", () -> new TransparentBlock(glass()));
    public static final DeferredBlock<IronBarsBlock> GlassPane = block("greenhouse_glass_pane", () -> new IronBarsBlock(glass()));
    public static final DeferredBlock<GreenhouseGlassWallBlock> GlassWallBottom = block("greenhouse_glass_wall_bottom", () -> new GreenhouseGlassWallBlock(GlassWallKind.Bottom, glassWall()));
    public static final DeferredBlock<GreenhouseGlassWallBlock> GlassWallMiddle = block("greenhouse_glass_wall_middle", () -> new GreenhouseGlassWallBlock(GlassWallKind.Middle, glassWall()));
    public static final DeferredBlock<GreenhouseGlassWallBlock> GlassWallTop = block("greenhouse_glass_wall_top", () -> new GreenhouseGlassWallBlock(GlassWallKind.Top, glassWall()));

    public static final DeferredBlock<GreenhouseStackBlock> GrowRack = block("greenhouse_grow_rack", () -> new GreenhouseStackBlock(StackKind.GrowRack,
            prop(MapColor.SNOW, 1.5f, SoundType.METAL).lightLevel(State -> GrowRackGlow)));
    public static final DeferredBlock<GreenhouseStackBlock> HydroponicWall = block("greenhouse_hydroponic_wall", () -> new GreenhouseStackBlock(StackKind.HydroponicWall,
            prop(MapColor.SNOW, 1.5f, SoundType.METAL)));
    public static final DeferredBlock<GreenhouseStackBlock> WaterTank = block("greenhouse_water_tank", () -> new GreenhouseStackBlock(StackKind.WaterTank,
            prop(MapColor.COLOR_LIGHT_GREEN, 2.0f, SoundType.METAL)));
    public static final DeferredBlock<GreenhouseStackBlock> SeedCabinet = block("greenhouse_seed_cabinet", () -> new GreenhouseStackBlock(StackKind.SeedCabinet,
            prop(MapColor.SNOW, 2.0f, SoundType.METAL)));
    public static final DeferredBlock<GreenhouseTallPlantBlock> TallFern = block("greenhouse_tall_fern", () -> new GreenhouseTallPlantBlock(
            prop(MapColor.PLANT, 0.5f, SoundType.DECORATED_POT).pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<GreenhousePropBlock> PottingBench = block("greenhouse_potting_bench", () -> new GreenhousePropBlock(GreenhouseKind.PottingBench,
            prop(MapColor.SNOW, 1.5f, SoundType.METAL)));
    public static final DeferredBlock<GreenhousePropBlock> HangingPlanter = block("greenhouse_hanging_planter", () -> new GreenhousePropBlock(GreenhouseKind.HangingPlanter,
            prop(MapColor.PLANT, 0.5f, SoundType.AZALEA).pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<GreenhousePropBlock> IrrigationPanel = block("greenhouse_irrigation_panel", () -> new GreenhousePropBlock(GreenhouseKind.IrrigationPanel,
            prop(MapColor.SNOW, 1.5f, SoundType.METAL).pushReaction(PushReaction.DESTROY).lightLevel(State -> PanelGlow)));

    private GreenhouseRegistry() {
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
    }

    public static List<DeferredItem<BlockItem>> creativeOrder() {
        return Collections.unmodifiableList(CreativeOrder);
    }

    private static <B extends Block> DeferredBlock<B> block(String Name, Supplier<B> Factory) {
        DeferredBlock<B> Registered = BlockRegister.register(Name, Factory);
        CreativeOrder.add(ItemRegister.registerSimpleBlockItem(Name, Registered));
        return Registered;
    }

    private static BlockBehaviour.Properties glass() {
        return BlockBehaviour.Properties.of()
                .instrument(NoteBlockInstrument.HAT)
                .strength(GlassHardness)
                .sound(SoundType.GLASS)
                .noOcclusion()
                .isValidSpawn((State, World, Pos, Type) -> false)
                .isRedstoneConductor((State, World, Pos) -> false)
                .isSuffocating((State, World, Pos) -> false)
                .isViewBlocking((State, World, Pos) -> false);
    }

    private static BlockBehaviour.Properties glassWall() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.SNOW)
                .instrument(NoteBlockInstrument.HAT)
                .requiresCorrectToolForDrops()
                .strength(GlassWallHardness, GlassWallResistance)
                .sound(SoundType.GLASS)
                .noOcclusion()
                .isValidSpawn((State, World, Pos, Type) -> false)
                .isRedstoneConductor((State, World, Pos) -> false)
                .isSuffocating((State, World, Pos) -> false)
                .isViewBlocking((State, World, Pos) -> false);
    }

    private static BlockBehaviour.Properties prop(MapColor Colour, float Hardness, SoundType Sound) {
        return BlockBehaviour.Properties.of().mapColor(Colour).strength(Hardness, Hardness * 2.0f).sound(Sound).noOcclusion();
    }
}
