package net.rai.scp_unleashed.scp001gate.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.rai.scp_unleashed.scp001gate.Scp001GateRegistry;

public record Scp001GateWarnPayload(int Tier, int Variant, float Closeness) implements CustomPacketPayload {
    public static final Type<Scp001GateWarnPayload> PayloadType = new Type<>(Scp001GateRegistry.location("scp001_gate_warn"));
    public static final StreamCodec<ByteBuf, Scp001GateWarnPayload> PayloadCodec = StreamCodec.composite(ByteBufCodecs.VAR_INT, Scp001GateWarnPayload::Tier, ByteBufCodecs.VAR_INT,
            Scp001GateWarnPayload::Variant, ByteBufCodecs.FLOAT, Scp001GateWarnPayload::Closeness, Scp001GateWarnPayload::new);

    @Override
    public Type<Scp001GateWarnPayload> type() {
        return PayloadType;
    }
}
