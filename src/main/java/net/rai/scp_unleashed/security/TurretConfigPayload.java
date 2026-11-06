package net.rai.scp_unleashed.security;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TurretConfigPayload(BlockPos Pos, int Action, int Index, String Text) implements CustomPacketPayload {
    public static final int ActionToggle = 0;
    public static final int ActionAllow = 1;
    public static final int ActionDeny = 2;
    public static final int ActionRemove = 3;
    public static final int ActionFlip = 4;
    public static final Type<TurretConfigPayload> PayloadType = new Type<>(SecurityRegistry.location("security_turret_config"));
    public static final StreamCodec<ByteBuf, TurretConfigPayload> PayloadCodec = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TurretConfigPayload::Pos,
            ByteBufCodecs.VAR_INT, TurretConfigPayload::Action,
            ByteBufCodecs.VAR_INT, TurretConfigPayload::Index,
            ByteBufCodecs.stringUtf8(TurretRules.MaxLength), TurretConfigPayload::Text,
            TurretConfigPayload::new);

    @Override
    public Type<TurretConfigPayload> type() {
        return PayloadType;
    }

    public static void handle(TurretConfigPayload Payload, IPayloadContext Context) {
        if (Context.player() instanceof ServerPlayer User && User.level().isLoaded(Payload.Pos()) && User.canInteractWithBlock(Payload.Pos(), 1.0)
                && User.level().getBlockEntity(Payload.Pos()) instanceof SentryTurretBlockEntity Turret && Turret.canEdit(User)) {
            Turret.applyConfig(Payload.Action(), Payload.Index(), Payload.Text());
        }
    }
}
