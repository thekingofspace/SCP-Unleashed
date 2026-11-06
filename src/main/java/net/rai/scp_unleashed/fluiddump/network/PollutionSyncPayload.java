package net.rai.scp_unleashed.fluiddump.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.fluiddump.PollutionField;

import java.util.List;

public record PollutionSyncPayload(List<PollutionPatch> Patches) implements CustomPacketPayload {
    public static final Type<PollutionSyncPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "fluid_dump_pollution"));
    public static final StreamCodec<ByteBuf, PollutionSyncPayload> PayloadCodec = StreamCodec.composite(
            PollutionPatch.PatchCodec.apply(ByteBufCodecs.list(PollutionField.MaxSources)), PollutionSyncPayload::Patches,
            PollutionSyncPayload::new);

    @Override
    public Type<PollutionSyncPayload> type() {
        return PayloadType;
    }
}
