package net.rai.scp_unleashed.scp005.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.List;

public record HoldsPayload(List<HeldDoor> Doors) implements CustomPacketPayload {
    public static final Type<HoldsPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp005_holds"));
    public static final StreamCodec<ByteBuf, HoldsPayload> PayloadCodec = StreamCodec.composite(
            HeldDoor.DoorCodec.apply(ByteBufCodecs.list()), HoldsPayload::Doors,
            HoldsPayload::new);

    @Override
    public Type<HoldsPayload> type() {
        return PayloadType;
    }

    public record HeldDoor(BlockPos Anchor, long Until) {
        public static final StreamCodec<ByteBuf, HeldDoor> DoorCodec = StreamCodec.composite(
                BlockPos.STREAM_CODEC, HeldDoor::Anchor,
                ByteBufCodecs.VAR_LONG, HeldDoor::Until,
                HeldDoor::new);
    }
}
