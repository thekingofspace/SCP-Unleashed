package net.rai.scp_unleashed.scp093.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp093FlashPayload(int Phase, int Ticks) implements CustomPacketPayload {
    public static final int FadeIn = 0;
    public static final int FadeOut = 1;
    public static final int Cancel = 2;
    public static final Type<Scp093FlashPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp093_flash"));
    public static final StreamCodec<ByteBuf, Scp093FlashPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Scp093FlashPayload::Phase,
            ByteBufCodecs.VAR_INT, Scp093FlashPayload::Ticks,
            Scp093FlashPayload::new);

    @Override
    public Type<Scp093FlashPayload> type() {
        return PayloadType;
    }
}
