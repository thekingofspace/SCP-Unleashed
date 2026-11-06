package net.rai.scp_unleashed.scp024;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp024.plan.Scp024Plan;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class Scp024Lights {
    public static final int PoolLevel = 11;
    public static final int Warm = 0xFFEFD2;
    public static final int Wash = 0xFF2A1E;
    public static final int Gloom = 0x7A1620;
    public static final int[] Party = {0xFF4A4A, 0xFFD23C, 0x4CFF6A, 0x4CC8FF, 0xC46AFF, 0xFF6AC8};
    public static final int FollowEvery = 220;
    public static final int FollowFor = 90;

    public enum Mood {
        Sweep,
        Converge,
        Flash,
        Red
    }

    private final ServerLevel World;
    private final List<Scp024Spotlight> Beams = new ArrayList<>();
    private final List<BlockPos> Pools = new ArrayList<>();
    private final double MinX;
    private final double MaxX;
    private final double MinZ;
    private final double MaxZ;
    private final double FloorY;
    private final boolean Glow;
    private final int Base;
    private Mood Current = Mood.Sweep;
    private int MoodTicks;
    private boolean Tense;
    private int Ticks;
    @Nullable
    private Entity Host;

    private Scp024Lights(ServerLevel World, double MinX, double MaxX, double MinZ, double MaxZ, double FloorY, boolean Glow, int Base) {
        this.World = World;
        this.MinX = MinX;
        this.MaxX = MaxX;
        this.MinZ = MinZ;
        this.MaxZ = MaxZ;
        this.FloorY = FloorY;
        this.Glow = Glow;
        this.Base = Base;
    }

    public static Scp024Lights rig(ServerLevel World, UUID Token, BlockPos Origin, Scp024Plan Plan, double FloorY, boolean Glow, int Colour) {
        int[] Low = Plan.low();
        int[] High = Plan.high();
        Scp024Lights Lights = new Scp024Lights(World, Origin.getX() + Low[0] + 2.0, Origin.getX() + High[0] - 1.0, Origin.getZ() + Low[2] + 2.0,
                Origin.getZ() + High[2] - 1.0, Origin.getY() + FloorY, Glow, Colour);
        for (int[] Mark : Plan.marks("rig")) {
            Scp024Spotlight Beam = Scp024Registry.Spotlight.get().create(World);
            if (Beam == null) {
                continue;
            }
            Beam.moveTo(Origin.getX() + Mark[0] + 0.5, Origin.getY() + Mark[1] + 0.4, Origin.getZ() + Mark[2] + 0.5, 0.0f, 0.0f);
            Beam.aim(new Vec3((Lights.MinX + Lights.MaxX) * 0.5, Lights.FloorY, (Lights.MinZ + Lights.MaxZ) * 0.5));
            Beam.tint(Colour);
            Beam.spread(1.3f);
            Scp024Stage.own(Beam, Token);
            World.addFreshEntity(Beam);
            Lights.Beams.add(Beam);
            Lights.Pools.add(null);
        }
        return Lights;
    }

    public void host(@Nullable Entity Figure) {
        Host = Figure;
    }

    public void mood(Mood Next, int Duration) {
        Current = Next;
        MoodTicks = Duration;
    }

    public void tense(boolean Enabled) {
        Tense = Enabled;
    }

    public boolean isTense() {
        return Tense;
    }

    private Vec3 sweep(int Index, double Speed) {
        double Phase = Index * 1.7 + Index * Index * 0.31;
        double T = Ticks * 0.012 * Speed;
        double X = MinX + (MaxX - MinX) * (0.5 + 0.45 * Math.sin(T * (1.0 + Index * 0.23) + Phase));
        double Z = MinZ + (MaxZ - MinZ) * (0.5 + 0.45 * Math.sin(T * (0.77 + Index * 0.17) + Phase * 1.3));
        return new Vec3(X, FloorY, Z);
    }

    private static Vec3 feet(Entity Target) {
        return new Vec3(Target.getX(), Target.getY() + 0.05, Target.getZ());
    }

    public void tick(@Nullable Entity Focus) {
        Ticks++;
        if (MoodTicks > 0 && --MoodTicks == 0) {
            Current = Mood.Sweep;
        }
        boolean Following = Focus != null && (Tense || Ticks % FollowEvery < FollowFor);
        double Speed = Tense ? 2.6 : 1.0;
        for (int Index = 0; Index < Beams.size(); Index++) {
            Scp024Spotlight Beam = Beams.get(Index);
            if (Beam.isRemoved()) {
                continue;
            }
            Vec3 Spot;
            int Colour = Base;
            float Width = 1.3f;
            float Power = 1.0f;
            switch (Current) {
                case Converge -> {
                    Entity Pick = Host != null && Index % 2 == 1 ? Host : Focus;
                    Spot = Pick != null ? feet(Pick) : sweep(Index, Speed);
                    Width = 1.1f;
                }
                case Flash -> {
                    Spot = Focus != null ? feet(Focus) : sweep(Index, 3.0);
                    Colour = Party[(Ticks / 3 + Index * 2) % Party.length];
                    Width = 1.0f + 0.4f * Mth.sin(Ticks * 0.6f + Index);
                    Power = 1.2f;
                }
                case Red -> {
                    Spot = Focus != null ? feet(Focus) : sweep(Index, 2.0);
                    Colour = Wash;
                    Width = 2.0f;
                    Power = 0.9f + 0.2f * Mth.sin(Ticks * 0.9f);
                }
                default -> {
                    boolean Chase = Following && Index == 0 && Focus != null;
                    Spot = Chase ? feet(Focus) : sweep(Index, Speed);
                    if (Tense && Ticks % 10 < 5 && Index > 0) {
                        Power = 0.75f;
                    }
                }
            }
            Vec3 Was = Beam.target();
            Beam.aim(Was.lerp(Spot, Current == Mood.Sweep && !Following ? 0.35 : 0.5));
            if (Beam.colour() != (Colour & 0xFFFFFF)) {
                Beam.tint(Colour);
            }
            if (Math.abs(Beam.width() - Width) > 0.01f) {
                Beam.spread(Width);
            }
            if (Math.abs(Beam.power() - Power) > 0.01f) {
                Beam.power(Power);
            }
            if (Glow && Ticks % 4 == Index % 4) {
                pool(Index, Beam.target());
            }
        }
    }

    private void pool(int Index, Vec3 Spot) {
        BlockPos Cell = BlockPos.containing(Spot.x, Spot.y + 0.3, Spot.z);
        BlockPos Old = Pools.get(Index);
        if (Cell.equals(Old)) {
            return;
        }
        release(Old);
        Pools.set(Index, null);
        if (!World.isLoaded(Cell) || !World.getBlockState(Cell).isAir() || World.getBlockState(Cell.below()).isAir()) {
            return;
        }
        for (BlockPos Taken : Pools) {
            if (Cell.equals(Taken)) {
                return;
            }
        }
        World.setBlock(Cell, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, PoolLevel), Scp024Palette.Quiet);
        Pools.set(Index, Cell);
    }

    private void release(@Nullable BlockPos Cell) {
        if (Cell == null || !World.isLoaded(Cell)) {
            return;
        }
        BlockState State = World.getBlockState(Cell);
        if (State.is(Blocks.LIGHT) && State.getValue(LightBlock.LEVEL) == PoolLevel) {
            World.setBlock(Cell, Blocks.AIR.defaultBlockState(), Scp024Palette.Quiet);
        }
    }

    public void remove() {
        for (int Index = 0; Index < Beams.size(); Index++) {
            Beams.get(Index).discard();
            release(Pools.get(Index));
            Pools.set(Index, null);
        }
        Beams.clear();
        Pools.clear();
    }

    public static boolean isPool(BlockState State) {
        return State.is(Blocks.LIGHT) && State.getValue(LightBlock.LEVEL) == PoolLevel;
    }
}
