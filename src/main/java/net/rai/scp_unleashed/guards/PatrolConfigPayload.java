package net.rai.scp_unleashed.guards;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.rai.scp_unleashed.security.TurretRules;

public record PatrolConfigPayload(BlockPos Pos, int Action, int Index, int Value, String Text) implements CustomPacketPayload {
    public static final int ActionToggle = 0;
    public static final int ActionAllow = 1;
    public static final int ActionDeny = 2;
    public static final int ActionRemove = 3;
    public static final int ActionFlip = 4;
    public static final int ActionMode = 5;
    public static final int ActionFire = 6;
    public static final int ActionMelee = 7;
    public static final int ActionWait = 8;
    public static final int ActionMove = 9;
    public static final int ActionUnlink = 10;
    public static final Type<PatrolConfigPayload> PayloadType = new Type<>(GuardsRegistry.location("guards_patrol_config"));
    public static final StreamCodec<ByteBuf, PatrolConfigPayload> PayloadCodec = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PatrolConfigPayload::Pos,
            ByteBufCodecs.VAR_INT, PatrolConfigPayload::Action,
            ByteBufCodecs.VAR_INT, PatrolConfigPayload::Index,
            ByteBufCodecs.VAR_INT, PatrolConfigPayload::Value,
            ByteBufCodecs.stringUtf8(TurretRules.MaxLength), PatrolConfigPayload::Text,
            PatrolConfigPayload::new);

    @Override
    public Type<PatrolConfigPayload> type() {
        return PayloadType;
    }

    public static void handle(PatrolConfigPayload Payload, IPayloadContext Context) {
        if (Context.player() instanceof ServerPlayer User && User.level().isLoaded(Payload.Pos()) && User.level().getBlockEntity(Payload.Pos()) instanceof PatrolManagerBlockEntity Manager
                && Manager.isUsableBy(User)) {
            if (!Manager.canEdit(User)) {
                Manager.denied(User);
                return;
            }
            Manager.applyConfig(Payload.Action(), Payload.Index(), Payload.Value(), Payload.Text());
        }
    }
}
