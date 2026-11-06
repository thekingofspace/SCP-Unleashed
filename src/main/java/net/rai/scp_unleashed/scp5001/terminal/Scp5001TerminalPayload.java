package net.rai.scp_unleashed.scp5001.terminal;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp5001.core.Scp5001State;

public record Scp5001TerminalPayload(int Pool, String Phase, float Lift, int Overdriven, long Seed, long Time) implements CustomPacketPayload {
    public static final Type<Scp5001TerminalPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp5001_terminal"));
    public static final StreamCodec<ByteBuf, Scp5001TerminalPayload> PayloadCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Scp5001TerminalPayload::Pool,
            ByteBufCodecs.STRING_UTF8, Scp5001TerminalPayload::Phase,
            ByteBufCodecs.FLOAT, Scp5001TerminalPayload::Lift,
            ByteBufCodecs.VAR_INT, Scp5001TerminalPayload::Overdriven,
            ByteBufCodecs.VAR_LONG, Scp5001TerminalPayload::Seed,
            ByteBufCodecs.VAR_LONG, Scp5001TerminalPayload::Time,
            Scp5001TerminalPayload::new);

    public static Scp5001TerminalPayload read(MinecraftServer Server, BlockPos Pos, int Pool) {
        Scp5001State State = Scp5001State.get(Server);
        return new Scp5001TerminalPayload(Pool, State.phase().name(), State.lift(), State.overdriven(), Pos.asLong(), Server.overworld().getGameTime());
    }

    @Override
    public Type<Scp5001TerminalPayload> type() {
        return PayloadType;
    }
}
