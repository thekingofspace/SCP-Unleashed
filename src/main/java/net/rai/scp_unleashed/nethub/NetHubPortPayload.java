package net.rai.scp_unleashed.nethub;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.rai.scp_unleashed.Scp_unleashed;

public record NetHubPortPayload(BlockPos Pos, int Port) implements CustomPacketPayload {
    public static final double ReachSquared = 144.0;
    public static final Type<NetHubPortPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "nethub_port"));
    public static final StreamCodec<ByteBuf, NetHubPortPayload> PayloadCodec = StreamCodec.composite(
            BlockPos.STREAM_CODEC, NetHubPortPayload::Pos,
            ByteBufCodecs.VAR_INT, NetHubPortPayload::Port,
            NetHubPortPayload::new);

    @Override
    public Type<NetHubPortPayload> type() {
        return PayloadType;
    }

    public static void handle(NetHubPortPayload Payload, IPayloadContext Context) {
        if (Context.player() instanceof ServerPlayer User && !User.isSpectator() && User.level().isLoaded(Payload.Pos())
                && User.level().getBlockEntity(Payload.Pos()) instanceof NetworkHubBlockEntity Hub && User.distanceToSqr(Hub.getCentre()) <= ReachSquared) {
            Hub.setPort(Payload.Port());
        }
    }

    public static boolean nearEnough(Vec3 Eye, Vec3 Centre) {
        return Eye.distanceToSqr(Centre) <= ReachSquared;
    }
}
