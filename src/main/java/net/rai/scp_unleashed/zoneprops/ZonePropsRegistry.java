package net.rai.scp_unleashed.zoneprops;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.decor.PillarPart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public final class ZonePropsRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final int LampGlow = 14;
    public static final int VentGlow = 7;
    public static final int ScreenGlow = 5;
    public static final int CaseGlow = 10;

    private static final List<DeferredItem<BlockItem>> CreativeOrder = new ArrayList<>();

    public static final DeferredBlock<Block> LczIntercom = prop(ZonePropKind.LczIntercom, () -> metal(MapColor.QUARTZ, 1.0f).pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> LczNoticeBoard = prop(ZonePropKind.LczNoticeBoard, () -> wood(MapColor.TERRACOTTA_WHITE, 0.8f).pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> LczWallPipes = prop(ZonePropKind.LczWallPipes, () -> metal(MapColor.COLOR_LIGHT_GRAY, 1.5f).pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> LczSupplyLocker = prop(ZonePropKind.LczSupplyLocker, () -> metal(MapColor.QUARTZ, 2.0f));
    public static final DeferredBlock<Block> LczCheckpointBarrier = prop(ZonePropKind.LczCheckpointBarrier, () -> metal(MapColor.COLOR_YELLOW, 1.5f).pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> LczCellBunk = prop(ZonePropKind.LczCellBunk, () -> metal(MapColor.COLOR_LIGHT_GRAY, 1.5f).pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> HczValvePanel = prop(ZonePropKind.HczValvePanel, () -> metal(MapColor.COLOR_GRAY, 2.0f).pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> HczPipeManifold = prop(ZonePropKind.HczPipeManifold, () -> metal(MapColor.COLOR_GRAY, 2.5f));
    public static final DeferredBlock<Block> HczSteamVent = prop(ZonePropKind.HczSteamVent, () -> metal(MapColor.COLOR_GRAY, 2.5f).lightLevel(State -> VentGlow));
    public static final DeferredBlock<Block> HczCagedLamp = prop(ZonePropKind.HczCagedLamp, () -> metal(MapColor.COLOR_ORANGE, 1.5f).pushReaction(PushReaction.DESTROY)
            .lightLevel(State -> LampGlow));
    public static final DeferredBlock<Block> HczHazardCrate = prop(ZonePropKind.HczHazardCrate, () -> metal(MapColor.COLOR_YELLOW, 2.0f));
    public static final DeferredBlock<Block> HczMonitorConsole = prop(ZonePropKind.HczMonitorConsole, () -> metal(MapColor.COLOR_BLACK, 2.0f).lightLevel(State -> ScreenGlow));
    public static final DeferredBlock<Block> EzDirectorySign = prop(ZonePropKind.EzDirectorySign, () -> wood(MapColor.COLOR_BROWN, 1.0f).pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> EzPlanter = prop(ZonePropKind.EzPlanter, () -> wood(MapColor.PLANT, 1.0f));
    public static final DeferredBlock<Block> EzWaitingChairs = prop(ZonePropKind.EzWaitingChairs, () -> metal(MapColor.TERRACOTTA_RED, 1.5f));
    public static final DeferredBlock<Block> EzTurnstile = prop(ZonePropKind.EzTurnstile, () -> metal(MapColor.TERRACOTTA_WHITE, 2.0f));
    public static final DeferredBlock<Block> EzDisplayCase = prop(ZonePropKind.EzDisplayCase, () -> prop(MapColor.COLOR_BROWN, 1.0f, SoundType.GLASS)
            .lightLevel(State -> lit(State.getValue(ZoneStackPropBlock.Part)) ? CaseGlow : 0));
    public static final DeferredBlock<Block> EzDummyCameraDome = prop(ZonePropKind.EzDummyCameraDome, () -> metal(MapColor.SNOW, 1.0f).pushReaction(PushReaction.DESTROY));

    private ZonePropsRegistry() {
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
    }

    public static List<DeferredItem<BlockItem>> creativeOrder() {
        return Collections.unmodifiableList(CreativeOrder);
    }

    private static boolean lit(PillarPart Piece) {
        return Piece == PillarPart.Top || Piece == PillarPart.Single;
    }

    private static DeferredBlock<Block> prop(ZonePropKind Kind, Supplier<BlockBehaviour.Properties> Settings) {
        String Name = Kind.getSerializedName();
        DeferredBlock<Block> Registered = BlockRegister.register(Name, () -> create(Kind, Settings.get()));
        CreativeOrder.add(ItemRegister.registerSimpleBlockItem(Name, Registered));
        return Registered;
    }

    private static Block create(ZonePropKind Kind, BlockBehaviour.Properties Settings) {
        return switch (Kind.getMount()) {
            case Floor -> new ZonePropBlock(Kind, Settings);
            case Wall -> new ZoneWallPropBlock(Kind, Settings);
            case Ceiling -> new ZoneCeilingPropBlock(Kind, Settings);
            case Stack, WallStack -> Kind == ZonePropKind.HczSteamVent ? new ZoneVentBlock(Kind, Settings) : new ZoneStackPropBlock(Kind, Settings);
            case Pair, SidePair -> new ZonePairedPropBlock(Kind, Settings);
        };
    }

    private static BlockBehaviour.Properties prop(MapColor Colour, float Hardness, SoundType Sound) {
        return BlockBehaviour.Properties.of().mapColor(Colour).strength(Hardness, Hardness * 2.0f).sound(Sound).noOcclusion();
    }

    private static BlockBehaviour.Properties metal(MapColor Colour, float Hardness) {
        return prop(Colour, Hardness, SoundType.METAL);
    }

    private static BlockBehaviour.Properties wood(MapColor Colour, float Hardness) {
        return prop(Colour, Hardness, SoundType.WOOD).ignitedByLava();
    }
}
