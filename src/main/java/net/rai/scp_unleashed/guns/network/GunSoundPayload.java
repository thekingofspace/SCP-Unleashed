package net.rai.scp_unleashed.guns.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record GunSoundPayload(int Cue, int Kind, float Scale) implements CustomPacketPayload {
    public static final Type<GunSoundPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "guns_sound"));
    public static final StreamCodec<ByteBuf, GunSoundPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, GunSoundPayload::Cue,
            ByteBufCodecs.VAR_INT, GunSoundPayload::Kind,
            ByteBufCodecs.FLOAT, GunSoundPayload::Scale,
            GunSoundPayload::new);

    @Override
    public Type<GunSoundPayload> type() {
        return PayloadType;
    }
}
