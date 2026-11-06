package net.rai.scp_unleashed.scp217.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp217.Scp217Pulse;

import java.util.List;

public record Scp217PulsePayload(double X, double Y, double Z, float Radius, List<Integer> Targets) implements CustomPacketPayload {
    public static final Type<Scp217PulsePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp217_pulse"));
    public static final StreamCodec<ByteBuf, Scp217PulsePayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, Scp217PulsePayload::X,
            ByteBufCodecs.DOUBLE, Scp217PulsePayload::Y,
            ByteBufCodecs.DOUBLE, Scp217PulsePayload::Z,
            ByteBufCodecs.FLOAT, Scp217PulsePayload::Radius,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(Scp217Pulse.MaxMarked)), Scp217PulsePayload::Targets,
            Scp217PulsePayload::new);

    public Vec3 origin() {
        return new Vec3(X, Y, Z);
    }

    @Override
    public Type<Scp217PulsePayload> type() {
        return PayloadType;
    }
}
