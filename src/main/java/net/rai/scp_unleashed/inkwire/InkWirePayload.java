package net.rai.scp_unleashed.inkwire;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.rai.scp_unleashed.Scp_unleashed;

public record InkWirePayload(BlockPos Owner, BlockPos Target, boolean MainHand) implements CustomPacketPayload {
    public static final Type<InkWirePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "invisible_ink_wire"));
    public static final StreamCodec<ByteBuf, InkWirePayload> PayloadCodec = StreamCodec.composite(
            BlockPos.STREAM_CODEC, InkWirePayload::Owner,
            BlockPos.STREAM_CODEC, InkWirePayload::Target,
            ByteBufCodecs.BOOL, InkWirePayload::MainHand,
            InkWirePayload::new);

    @Override
    public Type<InkWirePayload> type() {
        return PayloadType;
    }

    public static void handle(InkWirePayload Payload, IPayloadContext Context) {
        if (!(Context.player() instanceof ServerPlayer User)) {
            return;
        }
        InvisibleInkItem.inkWire(User, Payload.MainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, Payload.Owner(), Payload.Target());
    }
}
