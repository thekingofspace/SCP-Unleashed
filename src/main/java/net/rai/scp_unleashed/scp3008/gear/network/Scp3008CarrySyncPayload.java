package net.rai.scp_unleashed.scp3008.gear.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.scp3008.gear.Scp3008GearRegistry;

import java.util.List;

public record Scp3008CarrySyncPayload(int EntityId, List<Shown> Parts, int Facing, int Turns) implements CustomPacketPayload {
    public static final Type<Scp3008CarrySyncPayload> PayloadType = new Type<>(Scp3008GearRegistry.location("scp3008_gear_carry"));

    public record Shown(BlockPos Offset, BlockState State) {
        public static final StreamCodec<ByteBuf, Shown> ShownCodec = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Shown::Offset,
                ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY), Shown::State,
                Shown::new);
    }

    public static final StreamCodec<ByteBuf, Scp3008CarrySyncPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Scp3008CarrySyncPayload::EntityId,
            Shown.ShownCodec.apply(ByteBufCodecs.list()), Scp3008CarrySyncPayload::Parts,
            ByteBufCodecs.INT, Scp3008CarrySyncPayload::Facing,
            ByteBufCodecs.VAR_INT, Scp3008CarrySyncPayload::Turns,
            Scp3008CarrySyncPayload::new);

    @Override
    public Type<Scp3008CarrySyncPayload> type() {
        return PayloadType;
    }
}
