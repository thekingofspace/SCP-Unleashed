package net.rai.scp_unleashed.fluiddump.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.rai.scp_unleashed.fluiddump.PollutionMath;

public record PollutionPatch(int X, int Y, int Z, float Radius, float Density) {
    public static final StreamCodec<ByteBuf, PollutionPatch> PatchCodec = StreamCodec.composite(
            ByteBufCodecs.INT, PollutionPatch::X,
            ByteBufCodecs.INT, PollutionPatch::Y,
            ByteBufCodecs.INT, PollutionPatch::Z,
            ByteBufCodecs.FLOAT, PollutionPatch::Radius,
            ByteBufCodecs.FLOAT, PollutionPatch::Density,
            PollutionPatch::new);

    public double densityAt(double PointX, double PointY, double PointZ) {
        return PollutionMath.local(PointX - (X + 0.5), PointY - (Y + 0.5), PointZ - (Z + 0.5), Radius, Density);
    }

    public boolean samePlace(PollutionPatch Other) {
        return X == Other.X && Y == Other.Y && Z == Other.Z;
    }

    public boolean looksLike(PollutionPatch Other) {
        return samePlace(Other) && Math.abs(Radius - Other.Radius) < 0.01f && Math.abs(Density - Other.Density) < 0.001f;
    }
}
