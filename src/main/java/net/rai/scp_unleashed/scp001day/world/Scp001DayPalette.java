package net.rai.scp_unleashed.scp001day.world;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.properties.Property;
import net.rai.scp_unleashed.Scp_unleashed;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.rai.scp_unleashed.scp001day.plan.Scp001DayCode;

public final class Scp001DayPalette {
    private static volatile BlockState[] States;

    private Scp001DayPalette() {
    }

    public static BlockState state(int Code) {
        BlockState[] Known = States;
        if (Known == null) {
            Known = build();
            States = Known;
        }
        return Code >= 0 && Code < Known.length && Known[Code] != null ? Known[Code] : Blocks.AIR.defaultBlockState();
    }

    private static BlockState[] build() {
        BlockState[] Made = new BlockState[Scp001DayCode.Count];
        Made[Scp001DayCode.Air] = Blocks.AIR.defaultBlockState();
        Made[Scp001DayCode.Bedrock] = Blocks.BEDROCK.defaultBlockState();
        Made[Scp001DayCode.Stone] = Blocks.STONE.defaultBlockState();
        Made[Scp001DayCode.Road] = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
        Made[Scp001DayCode.Sidewalk] = Blocks.SMOOTH_STONE.defaultBlockState();
        Made[Scp001DayCode.Curb] = Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        Made[Scp001DayCode.Plaza] = Blocks.CALCITE.defaultBlockState();
        Made[Scp001DayCode.Facade] = Blocks.WHITE_CONCRETE.defaultBlockState();
        Made[Scp001DayCode.Facade + 1] = Blocks.SMOOTH_QUARTZ.defaultBlockState();
        Made[Scp001DayCode.Facade + 2] = Blocks.SMOOTH_SANDSTONE.defaultBlockState();
        Made[Scp001DayCode.Facade + 3] = Blocks.POLISHED_DIORITE.defaultBlockState();
        Made[Scp001DayCode.Facade + 4] = Blocks.LIGHT_GRAY_TERRACOTTA.defaultBlockState();
        Made[Scp001DayCode.FloorSlab] = Blocks.SMOOTH_STONE.defaultBlockState();
        Made[Scp001DayCode.InteriorWall] = Blocks.QUARTZ_BRICKS.defaultBlockState();
        Made[Scp001DayCode.CoreWall] = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
        Made[Scp001DayCode.Glass] = Blocks.GLASS.defaultBlockState();
        Made[Scp001DayCode.Boards] = Blocks.BIRCH_PLANKS.defaultBlockState();
        Made[Scp001DayCode.Bars] = Blocks.IRON_BARS.defaultBlockState();
        Made[Scp001DayCode.Rebar] = Blocks.IRON_BARS.defaultBlockState();
        Made[Scp001DayCode.StairSouth] = Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);
        Made[Scp001DayCode.StairNorth] = Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
        Made[Scp001DayCode.LadderEast] = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST);
        Made[Scp001DayCode.LadderSouth] = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH);
        Made[Scp001DayCode.ManholeCover] = Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.TOP).setValue(TrapDoorBlock.OPEN, false);
        Made[Scp001DayCode.SewerBrick] = Blocks.STONE_BRICKS.defaultBlockState();
        Made[Scp001DayCode.SewerStained] = Blocks.TUFF_BRICKS.defaultBlockState();
        Made[Scp001DayCode.SewerCracked] = Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
        Made[Scp001DayCode.Water] = Blocks.WATER.defaultBlockState();
        Made[Scp001DayCode.Rubble] = Blocks.COBBLESTONE.defaultBlockState();
        Made[Scp001DayCode.Debris] = Blocks.TUFF.defaultBlockState();
        Made[Scp001DayCode.Stain] = Blocks.BROWN_TERRACOTTA.defaultBlockState();
        Made[Scp001DayCode.Residue] = Blocks.BLACK_TERRACOTTA.defaultBlockState();
        Made[Scp001DayCode.DeadLamp] = mod("fluorescent_light", Blocks.REDSTONE_LAMP.defaultBlockState(), "face", "ceiling", "lit", "false", "facing", "north");
        Made[Scp001DayCode.FlickerLamp] = mod("fluorescent_light", Blocks.AIR.defaultBlockState(), "face", "ceiling", "lit", "true", "facing", "north");
        Made[Scp001DayCode.EmergencyLamp] = mod("emergency_light", Blocks.AIR.defaultBlockState(), "face", "ceiling", "powered", "true", "facing", "north");
        Made[Scp001DayCode.StormDrain] = mod("metal_grate", Blocks.IRON_BARS.defaultBlockState());
        Made[Scp001DayCode.RubbleSlab] = Blocks.COBBLESTONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        Made[Scp001DayCode.RubbleWall] = Blocks.COBBLESTONE_WALL.defaultBlockState();
        Made[Scp001DayCode.Gravel] = Blocks.GRAVEL.defaultBlockState();
        Made[Scp001DayCode.WallPanel] = Blocks.SPRUCE_PLANKS.defaultBlockState();
        Made[Scp001DayCode.WallPaperBlue] = Blocks.LIGHT_BLUE_TERRACOTTA.defaultBlockState();
        Made[Scp001DayCode.WallPaperYellow] = Blocks.YELLOW_TERRACOTTA.defaultBlockState();
        for (int Facing = 0; Facing < 4; Facing++) {
            String Name = Facings[Facing];
            Direction Way = Direction.from2DDataValue(Facing == 0 ? 2 : (Facing == 1 ? 3 : (Facing == 2 ? 0 : 1)));
            Made[Scp001DayCode.Desk + Facing] = mod("office_desk", Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP), "facing", Name);
            Made[Scp001DayCode.Chair + Facing] = mod("office_chair", Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Way), "facing", Name);
            Made[Scp001DayCode.Cabinet + Facing] = mod("filing_cabinet", Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), "facing", Name, "open", "false");
            Made[Scp001DayCode.Papers + Facing] = mod("paper_stack", Blocks.AIR.defaultBlockState(), "facing", Name, "variant", Integer.toString((Facing * 2 + 1) % 6));
            Made[Scp001DayCode.TippedChair + Facing] = Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Way).setValue(StairBlock.HALF, Half.TOP);
            Made[Scp001DayCode.Bench + Facing] = Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Way);
            Made[Scp001DayCode.RubbleStair + Facing] = Blocks.COBBLESTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Way);
        }
        Made[Scp001DayCode.Plant] = Blocks.FLOWER_POT.defaultBlockState();
        Made[Scp001DayCode.Copier] = mod("office_photocopier", Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), "facing", "north");
        Made[Scp001DayCode.StainDark] = Blocks.SOUL_SOIL.defaultBlockState();
        Made[Scp001DayCode.FallenTile] = Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        Made[Scp001DayCode.CarBody] = Blocks.RED_CONCRETE.defaultBlockState();
        Made[Scp001DayCode.CarBody + 1] = Blocks.BLUE_CONCRETE.defaultBlockState();
        Made[Scp001DayCode.CarBody + 2] = Blocks.WHITE_CONCRETE.defaultBlockState();
        Made[Scp001DayCode.CarBody + 3] = Blocks.YELLOW_CONCRETE.defaultBlockState();
        Made[Scp001DayCode.CarBody + 4] = Blocks.GRAY_CONCRETE.defaultBlockState();
        Made[Scp001DayCode.CarGlass] = Blocks.LIGHT_GRAY_STAINED_GLASS.defaultBlockState();
        Made[Scp001DayCode.Wheel] = Blocks.BLACK_CONCRETE.defaultBlockState();
        Made[Scp001DayCode.LampPole] = Blocks.ANDESITE_WALL.defaultBlockState();
        Made[Scp001DayCode.LampArm] = Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        Made[Scp001DayCode.LampHead] = Blocks.REDSTONE_LAMP.defaultBlockState();
        Made[Scp001DayCode.SignalHead] = Blocks.BLACK_CONCRETE.defaultBlockState();
        Made[Scp001DayCode.ShelterRoof] = Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        Made[Scp001DayCode.ShelterPost] = Blocks.IRON_BARS.defaultBlockState();
        Made[Scp001DayCode.ShelterGlass] = Blocks.GLASS.defaultBlockState();
        Made[Scp001DayCode.PaperDrift] = mod("paper_stack", Blocks.AIR.defaultBlockState(), "facing", "north", "variant", "2");
        Made[Scp001DayCode.PipeX] = mod("decor_pipe", Blocks.CHAIN.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X), "axis", "x");
        Made[Scp001DayCode.PipeY] = mod("decor_pipe", Blocks.CHAIN.defaultBlockState(), "axis", "y");
        Made[Scp001DayCode.PipeZ] = mod("decor_pipe", Blocks.CHAIN.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z), "axis", "z");
        Made[Scp001DayCode.Valve] = mod("decor_pipe_valve", Blocks.CHAIN.defaultBlockState());
        Made[Scp001DayCode.Grate] = mod("metal_grate", Blocks.IRON_BARS.defaultBlockState());
        Made[Scp001DayCode.Bedroll] = Blocks.SPRUCE_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.BOTTOM).setValue(TrapDoorBlock.OPEN, false);
        Made[Scp001DayCode.Lantern] = Blocks.LANTERN.defaultBlockState();
        Made[Scp001DayCode.EmptyTin] = Blocks.FLOWER_POT.defaultBlockState();
        Made[Scp001DayCode.BusBody] = Blocks.YELLOW_CONCRETE.defaultBlockState();
        Made[Scp001DayCode.BusTrim] = Blocks.WHITE_CONCRETE.defaultBlockState();
        return Made;
    }

    private static final String[] Facings = {"north", "east", "south", "west"};

    public static BlockState mod(String Name, BlockState Fallback, String... Settings) {
        Block Found = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name));
        if (Found == null || Found == Blocks.AIR) {
            return Fallback;
        }
        BlockState State = Found.defaultBlockState();
        for (int Index = 0; Index + 1 < Settings.length; Index += 2) {
            Property<?> Setting = State.getBlock().getStateDefinition().getProperty(Settings[Index]);
            if (Setting != null) {
                State = apply(State, Setting, Settings[Index + 1]);
            }
        }
        return State;
    }

    private static <T extends Comparable<T>> BlockState apply(BlockState State, Property<T> Setting, String Value) {
        return Setting.getValue(Value).map(Parsed -> State.setValue(Setting, Parsed)).orElse(State);
    }
}
