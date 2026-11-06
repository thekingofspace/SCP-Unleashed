package net.rai.scp_unleashed.npc.doors;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.door.DoorButtonBlock;
import net.rai.scp_unleashed.door.DoorLeverBlock;
import net.rai.scp_unleashed.door.KeycardScannerBlock;
import net.rai.scp_unleashed.door.PanelDoorBlock;
import net.rai.scp_unleashed.door.PanelDoorBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class NpcDoorAccess {
    public static final double Plane = 0.5;
    public static final double StandOff = 1.0;
    public static final float OpenEnough = 0.98f;

    public enum Way {
        Manual, Opener, Scanner
    }

    public record Plan(Way Kind, BlockPos Device, ItemStack Card) {
    }

    private NpcDoorAccess() {
    }

    public static boolean isGate(BlockState State) {
        if (State.getBlock() instanceof PanelDoorBlock) {
            return true;
        }
        return State.getBlock() instanceof DoorBlock Door && !Door.type().canOpenByHand();
    }

    public static boolean isOpen(BlockState State) {
        return State.hasProperty(BlockStateProperties.OPEN) && State.getValue(BlockStateProperties.OPEN);
    }

    public static boolean isClosedGate(BlockState State) {
        return isGate(State) && !isOpen(State);
    }

    public static boolean isOpenPanel(BlockState State) {
        return State.getBlock() instanceof PanelDoorBlock && isOpen(State);
    }

    public static Vec3 normal(BlockState State) {
        Direction Front = Direction.NORTH;
        if (State.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            Front = State.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        return new Vec3(Front.getStepX(), 0.0, Front.getStepZ());
    }

    public static BlockPos anchor(List<BlockPos> Cells) {
        BlockPos Best = Cells.get(0);
        for (BlockPos Cell : Cells) {
            if (Cell.asLong() < Best.asLong()) {
                Best = Cell;
            }
        }
        return Best.immutable();
    }

    public static Vec3 middle(List<BlockPos> Cells) {
        int Bottom = Integer.MAX_VALUE;
        for (BlockPos Cell : Cells) {
            Bottom = Math.min(Bottom, Cell.getY());
        }
        double SumX = 0.0;
        double SumZ = 0.0;
        int Count = 0;
        for (BlockPos Cell : Cells) {
            if (Cell.getY() == Bottom) {
                SumX += Cell.getX() + 0.5;
                SumZ += Cell.getZ() + 0.5;
                Count++;
            }
        }
        return new Vec3(SumX / Math.max(1, Count), Bottom, SumZ / Math.max(1, Count));
    }

    public static double offset(Vec3 Point, Vec3 Middle, Vec3 Normal) {
        return (Point.x - Middle.x) * Normal.x + (Point.z - Middle.z) * Normal.z;
    }

    public static boolean fullyOpen(Level World, List<BlockPos> Cells) {
        for (BlockPos Cell : Cells) {
            BlockState State = World.getBlockState(Cell);
            if (!isGate(State) || !isOpen(State)) {
                return false;
            }
            if (State.getBlock() instanceof PanelDoorBlock Frame) {
                PanelDoorBlockEntity Door = Frame.masterEntity(World, Cell, State);
                if (Door != null && Door.getEased() < OpenEnough) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean anyOpen(Level World, List<BlockPos> Cells) {
        for (BlockPos Cell : Cells) {
            if (isOpen(World.getBlockState(Cell))) {
                return true;
            }
        }
        return false;
    }

    public static List<ItemStack> carried(LivingEntity User) {
        List<ItemStack> Pack = new ArrayList<>();
        if (User instanceof DClass Body) {
            Pack.addAll(Body.main());
        }
        Pack.add(User.getItemBySlot(EquipmentSlot.MAINHAND));
        Pack.add(User.getItemBySlot(EquipmentSlot.OFFHAND));
        return Pack;
    }

    @Nullable
    public static Plan plan(LivingEntity User, Level World, BlockPos Door, double Side) {
        List<BlockPos> Cells = NpcDoors.extent(World, Door);
        if (Cells.isEmpty()) {
            return null;
        }
        BlockState State = World.getBlockState(Door);
        if (State.getBlock() instanceof PanelDoorBlock Frame && Frame.getKind().isManual()) {
            return new Plan(Way.Manual, anchor(Cells), ItemStack.EMPTY);
        }
        Vec3 Middle = middle(Cells);
        Vec3 Normal = normal(State);
        Vec3 Stand = Middle.add(Normal.scale(Side * StandOff)).add(0.0, User.getEyeHeight(), 0.0);
        BlockPos Opener = nearest(devices(World, Cells, Middle, Normal, Side, true), Stand);
        if (Opener != null) {
            return new Plan(Way.Opener, Opener, ItemStack.EMPTY);
        }
        for (BlockPos Scanner : sorted(devices(World, Cells, Middle, Normal, Side, false), Stand)) {
            Optional<ItemStack> Card = NpcDoors.pickCard(World, Scanner, carried(User));
            if (Card.isPresent()) {
                return new Plan(Way.Scanner, Scanner, Card.get());
            }
        }
        return null;
    }

    private static List<BlockPos> devices(Level World, List<BlockPos> Cells, Vec3 Middle, Vec3 Normal, double Side, boolean Openers) {
        Set<BlockPos> Inside = new LinkedHashSet<>(Cells);
        Set<BlockPos> Found = new LinkedHashSet<>();
        for (BlockPos Cell : Cells) {
            for (BlockPos Spot : BlockPos.betweenClosed(Cell.offset(-NpcDoors.Search, -NpcDoors.Search, -NpcDoors.Search), Cell.offset(NpcDoors.Search, NpcDoors.Search, NpcDoors.Search))) {
                if (Inside.contains(Spot) || Found.contains(Spot)) {
                    continue;
                }
                BlockState State = World.getBlockState(Spot);
                boolean Wanted = Openers ? usableOpener(State) : State.getBlock() instanceof KeycardScannerBlock;
                if (Wanted && onSide(State, Spot, Middle, Normal, Side)) {
                    Found.add(Spot.immutable());
                }
            }
        }
        return new ArrayList<>(Found);
    }

    private static boolean usableOpener(BlockState State) {
        if (State.getBlock() instanceof ButtonBlock || State.getBlock() instanceof DoorButtonBlock) {
            return true;
        }
        boolean Lever = State.getBlock() instanceof LeverBlock || State.getBlock() instanceof DoorLeverBlock;
        return Lever && !(State.hasProperty(BlockStateProperties.POWERED) && State.getValue(BlockStateProperties.POWERED));
    }

    private static boolean onSide(BlockState State, BlockPos Spot, Vec3 Middle, Vec3 Normal, double Side) {
        double Offset = offset(Vec3.atCenterOf(Spot), Middle, Normal);
        if (Offset * Side >= Plane) {
            return true;
        }
        if (Math.abs(Offset) >= Plane || !State.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return false;
        }
        Direction Facing = State.getValue(BlockStateProperties.HORIZONTAL_FACING);
        return (Facing.getStepX() * Normal.x + Facing.getStepZ() * Normal.z) * Side > 0.0;
    }

    @Nullable
    private static BlockPos nearest(List<BlockPos> Found, Vec3 Stand) {
        List<BlockPos> Order = sorted(Found, Stand);
        return Order.isEmpty() ? null : Order.get(0);
    }

    private static List<BlockPos> sorted(List<BlockPos> Found, Vec3 Stand) {
        List<BlockPos> Order = new ArrayList<>(Found);
        Order.sort((First, Second) -> Double.compare(Vec3.atCenterOf(First).distanceToSqr(Stand), Vec3.atCenterOf(Second).distanceToSqr(Stand)));
        return Order;
    }
}
