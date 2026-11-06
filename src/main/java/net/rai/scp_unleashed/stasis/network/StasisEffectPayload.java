package net.rai.scp_unleashed.stasis.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record StasisEffectPayload(int Kind, double X, double Y, double Z, float Width, float Height, int Tier, int Holder) implements CustomPacketPayload {
    public static final int Captured = 0;
    public static final int Released = 1;
    public static final int Burst = 2;
    public static final int Denied = 3;
    public static final int Rattled = 4;
    public static final Type<StasisEffectPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "stasis_effect"));
    public static final StreamCodec<FriendlyByteBuf, StasisEffectPayload> PayloadCodec = CustomPacketPayload.codec(StasisEffectPayload::write, StasisEffectPayload::read);

    private static StasisEffectPayload read(FriendlyByteBuf Buffer) {
        return new StasisEffectPayload(Buffer.readVarInt(), Buffer.readDouble(), Buffer.readDouble(), Buffer.readDouble(), Buffer.readFloat(), Buffer.readFloat(),
                Buffer.readVarInt(), Buffer.readInt());
    }

    private void write(FriendlyByteBuf Buffer) {
        Buffer.writeVarInt(Kind);
        Buffer.writeDouble(X);
        Buffer.writeDouble(Y);
        Buffer.writeDouble(Z);
        Buffer.writeFloat(Width);
        Buffer.writeFloat(Height);
        Buffer.writeVarInt(Tier);
        Buffer.writeInt(Holder);
    }

    @Override
    public Type<StasisEffectPayload> type() {
        return PayloadType;
    }
}
