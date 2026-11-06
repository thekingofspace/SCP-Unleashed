package net.rai.scp_unleashed.ultralaser.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.ultralaser.UltralaserPriming;

public record UltralaserTriggerPayload(boolean Pressed) implements CustomPacketPayload {
    public static final Type<UltralaserTriggerPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "ultralaser_trigger"));
    public static final StreamCodec<ByteBuf, UltralaserTriggerPayload> PayloadCodec = ByteBufCodecs.BOOL.map(UltralaserTriggerPayload::new, UltralaserTriggerPayload::Pressed);

    @Override
    public Type<UltralaserTriggerPayload> type() {
        return PayloadType;
    }

    public static void handle(UltralaserTriggerPayload Payload, IPayloadContext Context) {
        if (!(Context.player() instanceof ServerPlayer User)) {
            return;
        }
        if (Payload.Pressed()) {
            UltralaserPriming.press(User);
        } else {
            UltralaserPriming.release(User);
        }
    }
}
