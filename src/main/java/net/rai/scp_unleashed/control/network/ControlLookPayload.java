package net.rai.scp_unleashed.control.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.UUID;

public record ControlLookPayload(UUID Player, boolean Active, ResourceLocation Look, CompoundTag Identity) implements CustomPacketPayload {
    public static final Type<ControlLookPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "control_look"));
    public static final StreamCodec<ByteBuf, ControlLookPayload> PayloadCodec = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, ControlLookPayload::Player,
            ByteBufCodecs.BOOL, ControlLookPayload::Active,
            ResourceLocation.STREAM_CODEC, ControlLookPayload::Look,
            ByteBufCodecs.COMPOUND_TAG, ControlLookPayload::Identity,
            ControlLookPayload::new);

    @Override
    public Type<ControlLookPayload> type() {
        return PayloadType;
    }
}
