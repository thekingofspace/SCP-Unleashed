package net.rai.scp_unleashed.archive.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.rai.scp_unleashed.Scp_unleashed;

public record OpenDossierPayload(InteractionHand Hand, boolean Reveal) implements CustomPacketPayload {
    public static final Type<OpenDossierPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "archive_open_dossier"));
    public static final StreamCodec<ByteBuf, OpenDossierPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.BOOL, Payload -> Payload.Hand() == InteractionHand.OFF_HAND,
            ByteBufCodecs.BOOL, OpenDossierPayload::Reveal,
            (OffHand, Reveal) -> new OpenDossierPayload(OffHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, Reveal));

    @Override
    public Type<OpenDossierPayload> type() {
        return PayloadType;
    }
}
