package net.rai.scp_unleashed.reactor.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record ReactorSnapshotPayload(int ContainerId, CompoundTag Data) implements CustomPacketPayload {
    public static final Type<ReactorSnapshotPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "fission_reactor_snapshot"));
    public static final StreamCodec<ByteBuf, ReactorSnapshotPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ReactorSnapshotPayload::ContainerId,
            ByteBufCodecs.COMPOUND_TAG, ReactorSnapshotPayload::Data,
            ReactorSnapshotPayload::new);

    @Override
    public Type<ReactorSnapshotPayload> type() {
        return PayloadType;
    }
}
