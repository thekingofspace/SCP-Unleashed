package net.rai.scp_unleashed.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.block.entity.PortHolder;

public record SetPortPayload(BlockPos Pos, int Slot, int Port) implements CustomPacketPayload {
    public static final Type<SetPortPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "set_port"));
    public static final StreamCodec<ByteBuf, SetPortPayload> PayloadCodec = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SetPortPayload::Pos,
            ByteBufCodecs.VAR_INT, SetPortPayload::Slot,
            ByteBufCodecs.VAR_INT, SetPortPayload::Port,
            SetPortPayload::new);

    @Override
    public Type<SetPortPayload> type() {
        return PayloadType;
    }

    public static void handle(SetPortPayload Payload, IPayloadContext Context) {
        if (Context.player() instanceof ServerPlayer Player && Player.level().isLoaded(Payload.Pos()) && Player.canInteractWithBlock(Payload.Pos(), 1.0)
                && Player.level().getBlockEntity(Payload.Pos()) instanceof PortHolder Device
                && Payload.Slot() >= 0 && Payload.Slot() < Device.getSlotCount()) {
            Device.setSlot(Payload.Slot(), Payload.Port());
        }
    }
}
