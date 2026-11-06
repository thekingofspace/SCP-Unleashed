package net.rai.scp_unleashed.guards;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

public record PatrolPoint(BlockPos Pos, int Wait, Direction Facing) {
    public static final int DefaultWait = 60;
    public static final int MaxWait = 1200;
    public static final int WaitStep = 20;
    public static final Codec<PatrolPoint> PointCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            BlockPos.CODEC.fieldOf("Pos").forGetter(PatrolPoint::Pos),
            Codec.INT.optionalFieldOf("Wait", DefaultWait).forGetter(PatrolPoint::Wait),
            Direction.CODEC.optionalFieldOf("Facing", Direction.NORTH).forGetter(PatrolPoint::Facing)
    ).apply(Instance, PatrolPoint::new));
    public static final StreamCodec<ByteBuf, PatrolPoint> PointStreamCodec = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PatrolPoint::Pos,
            ByteBufCodecs.VAR_INT, PatrolPoint::Wait,
            Direction.STREAM_CODEC, PatrolPoint::Facing,
            PatrolPoint::new);

    public PatrolPoint {
        Pos = Pos.immutable();
        Wait = Mth.clamp(Wait, 0, MaxWait);
        Facing = Facing.getAxis().isHorizontal() ? Facing : Direction.NORTH;
    }

    public PatrolPoint withWait(int Ticks) {
        return new PatrolPoint(Pos, Ticks, Facing);
    }
}
