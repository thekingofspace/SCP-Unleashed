package net.rai.scp_unleashed.wire;

import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.block.entity.OutletBlockEntity;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class Wires {
    public static final int BlocksPerCoil = 8;
    public static final int MaxLinksPerOutlet = 8;
    public static final int BurnTicks = 20;
    public static final int TripTicks = 5;
    public static final double OutletReach = -0.5 / 16.0;
    public static final double BlockReach = 7.5 / 16.0;
    public static final double SlotThinning = 0.04 / 16.0;
    public static final int MaxSlot = 15;
    public static final double JitterRange = 0.03 / 16.0;
    public static final int MaxQueuedMessages = 4096;
    public static final int MessagesPerTick = 1024;
    public static final int ValidationsPerTick = 64;

    private Wires() {
    }

    public static double halfWidth(WireTier Tier, int Slot, BlockPos First, BlockPos Second) {
        long Low = Math.min(First.asLong(), Second.asLong());
        long High = Math.max(First.asLong(), Second.asLong());
        double Jitter = (HashCommon.mix(Low * 31L + High) >>> 11) * 0x1.0p-53 * JitterRange;
        return Tier.getHalfWidth() - Math.min(Math.max(Slot, 0), MaxSlot) * SlotThinning - Jitter;
    }

    public static Vec3 attachPoint(BlockPos Pos, Direction Face, boolean FullBlock) {
        double Reach = FullBlock ? BlockReach : OutletReach;
        return Vec3.atCenterOf(Pos).add(Face.getStepX() * Reach, Face.getStepY() * Reach, Face.getStepZ() * Reach);
    }

    public static Direction settledFace(OutletBlockEntity Outlet, Direction Face, Vec3 Toward) {
        return Outlet.isFullBlock() ? exitFace(Outlet.getBlockPos(), Face, Toward) : Face;
    }

    public static Direction exitFace(BlockPos Pos, Direction Clicked, Vec3 Toward) {
        Vec3 Anchor = attachPoint(Pos, Clicked, true);
        if (leavesThrough(Pos, Anchor, Toward.subtract(Anchor)) == Clicked) {
            return Clicked;
        }
        Vec3 Centre = Vec3.atCenterOf(Pos);
        Direction Best = Direction.getNearest(Toward.x - Centre.x, Toward.y - Centre.y, Toward.z - Centre.z);
        return leavesThrough(Pos, attachPoint(Pos, Best, true), Toward.subtract(attachPoint(Pos, Best, true))) == Best ? Best : Clicked;
    }

    private static Direction leavesThrough(BlockPos Pos, Vec3 From, Vec3 Heading) {
        double[] Start = {From.x - Pos.getX(), From.y - Pos.getY(), From.z - Pos.getZ()};
        double[] Step = {Heading.x, Heading.y, Heading.z};
        Direction.Axis[] Axes = {Direction.Axis.X, Direction.Axis.Y, Direction.Axis.Z};
        double Nearest = Double.MAX_VALUE;
        Direction Exit = Direction.UP;
        for (int Axis = 0; Axis < 3; Axis++) {
            if (Math.abs(Step[Axis]) < 1.0E-9) {
                continue;
            }
            double Time = ((Step[Axis] > 0 ? 1.0 : 0.0) - Start[Axis]) / Step[Axis];
            if (Time < Nearest) {
                Nearest = Time;
                Exit = Direction.fromAxisAndDirection(Axes[Axis], Step[Axis] > 0 ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
            }
        }
        return Exit;
    }

    public static BlockPos hostOf(BlockState State, BlockPos Pos) {
        return State.getBlock() instanceof WireConnector Connector ? Connector.hostPos(State, Pos) : Pos;
    }

    public static int coilCost(double Length) {
        return Math.max(1, (int) Math.ceil(Length / BlocksPerCoil));
    }

    public static void playSound(Level World, BlockPos Pos, SoundEvent Sound) {
        World.playSound(null, Pos, Sound, SoundSource.BLOCKS, 1.0f, 0.92f + World.getRandom().nextFloat() * 0.16f);
    }

    public static void dropCoils(Level World, BlockPos Pos, WireTier Tier, int Coils) {
        while (Coils > 0) {
            int Count = Math.min(Coils, 64);
            Block.popResource(World, Pos, new ItemStack(Tier.getCoil(), Count));
            Coils -= Count;
        }
    }

    public static void detachAll(ServerLevel World, BlockPos Pos, OutletBlockEntity Connector) {
        Map<WireTier, Integer> Refunds = new EnumMap<>(WireTier.class);
        for (WireLink Link : List.copyOf(Connector.getLinks())) {
            Refunds.merge(Link.Tier(), coilCost(Connector.attachFor(Link).distanceTo(Link.targetAttach())), Integer::sum);
            if (World.isLoaded(Link.Target()) && World.getBlockEntity(Link.Target()) instanceof OutletBlockEntity Other) {
                Other.removeLink(Pos);
            }
            Connector.removeLink(Link.Target());
        }
        Refunds.forEach((Tier, Coils) -> dropCoils(World, Pos, Tier, Coils));
    }
}
