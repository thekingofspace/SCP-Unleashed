package net.rai.scp_unleashed.guns.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guns.GunFiring;

public record GunFirePayload(int Kind, int Sequence) implements CustomPacketPayload {
    public static final Type<GunFirePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "guns_fire"));
    public static final StreamCodec<ByteBuf, GunFirePayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, GunFirePayload::Kind,
            ByteBufCodecs.VAR_INT, GunFirePayload::Sequence,
            GunFirePayload::new);

    @Override
    public Type<GunFirePayload> type() {
        return PayloadType;
    }

    public static void handle(GunFirePayload Payload, IPayloadContext Context) {
        if (Context.player() instanceof ServerPlayer User) {
            GunFiring.requestFire(User, Payload.Kind(), Payload.Sequence());
        }
    }
}
