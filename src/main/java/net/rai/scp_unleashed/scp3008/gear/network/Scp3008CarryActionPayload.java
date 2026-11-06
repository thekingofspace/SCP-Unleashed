package net.rai.scp_unleashed.scp3008.gear.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.rai.scp_unleashed.scp3008.gear.Scp3008GearRegistry;

public record Scp3008CarryActionPayload(int Action) implements CustomPacketPayload {
    public static final int Use = 0;
    public static final int Drop = 1;
    public static final Type<Scp3008CarryActionPayload> PayloadType = new Type<>(Scp3008GearRegistry.location("scp3008_gear_carry_action"));
    public static final StreamCodec<ByteBuf, Scp3008CarryActionPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Scp3008CarryActionPayload::Action,
            Scp3008CarryActionPayload::new);

    @Override
    public Type<Scp3008CarryActionPayload> type() {
        return PayloadType;
    }
}
