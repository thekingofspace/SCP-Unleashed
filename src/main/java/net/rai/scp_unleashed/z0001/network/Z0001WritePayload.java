package net.rai.scp_unleashed.z0001.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.z0001.Z0001Ink;

public record Z0001WritePayload(InteractionHand Hand, String Name) implements CustomPacketPayload {
    public static final int MaxLength = 32;
    public static final Type<Z0001WritePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "z0001_write"));
    public static final StreamCodec<ByteBuf, Z0001WritePayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.BOOL, Payload -> Payload.Hand() == InteractionHand.OFF_HAND,
            ByteBufCodecs.stringUtf8(MaxLength), Z0001WritePayload::Name,
            (OffHand, Name) -> new Z0001WritePayload(OffHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, Name));

    @Override
    public Type<Z0001WritePayload> type() {
        return PayloadType;
    }

    public static void handle(Z0001WritePayload Payload, IPayloadContext Context) {
        if (!(Context.player() instanceof ServerPlayer Writer)) {
            return;
        }
        Z0001Ink.Written Result = Z0001Ink.write(Writer, Payload.Hand(), Payload.Name());
        if (Writer.connection.hasChannel(Z0001ResultPayload.PayloadType)) {
            PacketDistributor.sendToPlayer(Writer, new Z0001ResultPayload(Result.Outcome().ordinal(), Result.Name()));
        }
    }
}
