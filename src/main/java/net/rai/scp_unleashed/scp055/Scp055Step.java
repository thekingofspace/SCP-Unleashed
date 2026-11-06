package net.rai.scp_unleashed.scp055;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

public record Scp055Step(int Kind, int Ticks, List<BlockPos> Path, float Yaw, float Pitch) {
    public static final int Walk = 0;
    public static final int Look = 1;
    public static final int Fiddle = 2;
    public static final int Pause = 3;
    public static final int MaxPath = 16;
    public static final StreamCodec<ByteBuf, Scp055Step> StepCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Scp055Step::Kind,
            ByteBufCodecs.VAR_INT, Scp055Step::Ticks,
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(MaxPath)), Scp055Step::Path,
            ByteBufCodecs.FLOAT, Scp055Step::Yaw,
            ByteBufCodecs.FLOAT, Scp055Step::Pitch,
            Scp055Step::new);

    public static Scp055Step walk(int Ticks, List<BlockPos> Path) {
        return new Scp055Step(Walk, Ticks, List.copyOf(Path), 0.0f, 0.0f);
    }

    public static Scp055Step look(int Ticks, float Yaw, float Pitch) {
        return new Scp055Step(Look, Ticks, List.of(), Yaw, Pitch);
    }

    public static Scp055Step fiddle(int Ticks, float Yaw, float Pitch) {
        return new Scp055Step(Fiddle, Ticks, List.of(), Yaw, Pitch);
    }

    public static Scp055Step pause(int Ticks) {
        return new Scp055Step(Pause, Ticks, List.of(), 0.0f, 0.0f);
    }

    public static int total(List<Scp055Step> Steps) {
        int Sum = 0;
        for (Scp055Step Each : Steps) {
            Sum += Each.Ticks();
        }
        return Sum;
    }
}
