package net.rai.scp_unleashed.scp902;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class Scp902BoxBlockEntity extends BlockEntity {
    private long Toggled = Long.MIN_VALUE / 2;
    private boolean Primed;
    private boolean Showing;
    private float From;
    private float Ticks;
    private float LastTicks;
    private int Beats;

    public Scp902BoxBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp902Registry.BoxEntity.get(), Pos, State);
    }

    public boolean settling(long Now) {
        return Now >= Toggled && Now - Toggled < Scp902Lid.RestTicks;
    }

    public void toggled(long Now) {
        Toggled = Now;
    }

    public int beats() {
        return Beats;
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, Scp902BoxBlockEntity Self) {
        if (World instanceof ServerLevel Server && Scp902Box.onBeat(World.getGameTime())) {
            Self.Beats++;
            Scp902Box.tick(Server, Vec3.atCenterOf(Pos).subtract(0.0, 0.2, 0.0));
        }
    }

    public static void clientTick(Level World, BlockPos Pos, BlockState State, Scp902BoxBlockEntity Self) {
        boolean Target = Scp902BoxBlock.isOpen(State);
        if (!Self.Primed) {
            Self.Primed = true;
            Self.Showing = Target;
            Self.From = Scp902Lid.settled(Target);
            Self.Ticks = Scp902Lid.duration(Target);
            Self.LastTicks = Self.Ticks;
            return;
        }
        if (Target != Self.Showing) {
            Self.From = Scp902Lid.angle(Self.Showing, Self.From, Self.Ticks);
            Self.Showing = Target;
            Self.Ticks = 0.0f;
            Self.LastTicks = 0.0f;
            return;
        }
        Self.LastTicks = Self.Ticks;
        Self.Ticks = Math.min(Self.Ticks + 1.0f, Scp902Lid.duration(Target));
    }

    public float lidAngle(float Partial) {
        if (!Primed) {
            return Scp902Lid.settled(Scp902BoxBlock.isOpen(getBlockState()));
        }
        return Scp902Lid.angle(Showing, From, Mth.lerp(Partial, LastTicks, Ticks));
    }
}
