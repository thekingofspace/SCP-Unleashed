package net.rai.scp_unleashed.space;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

public record SpaceSuitTank(boolean Fitted, int Air) {
    public static final SpaceSuitTank Unfitted = new SpaceSuitTank(false, 0);
    public static final Codec<SpaceSuitTank> TankCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            Codec.BOOL.fieldOf("fitted").forGetter(SpaceSuitTank::Fitted),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("air").forGetter(SpaceSuitTank::Air)
    ).apply(Instance, SpaceSuitTank::new));
    public static final StreamCodec<ByteBuf, SpaceSuitTank> TankStreamCodec = StreamCodec.composite(
            ByteBufCodecs.BOOL, SpaceSuitTank::Fitted,
            ByteBufCodecs.VAR_INT, SpaceSuitTank::Air,
            SpaceSuitTank::new);

    public SpaceSuitTank {
        Air = Fitted ? Math.max(0, Math.min(AirCanisterItem.Capacity, Air)) : 0;
    }

    public SpaceSuitTank withAir(int Amount) {
        return new SpaceSuitTank(Fitted, Amount);
    }

    public float fraction() {
        return Air / (float) AirCanisterItem.Capacity;
    }
}
