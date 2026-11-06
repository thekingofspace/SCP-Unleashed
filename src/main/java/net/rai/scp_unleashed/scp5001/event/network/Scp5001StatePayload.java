package net.rai.scp_unleashed.scp5001.event.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.rai.scp_unleashed.scp5001.event.Scp5001EventRegistry;

import java.util.ArrayList;
import java.util.List;

public record Scp5001StatePayload(int Phase, long PhaseTick, float Lift, int Overdriven, int Generators, int Rods, boolean RodsSpent, long CooldownEnd,
                                  int SwarmWave, int SwarmWaves, int SwarmAlive, BlockPos Core, boolean Located, List<BlockPos> Hot, long RiseStart,
                                  long ResetStart, int RiseTicks, int LatchTicks) implements CustomPacketPayload {
    public static final Type<Scp5001StatePayload> PayloadType = new Type<>(Scp5001EventRegistry.location("scp5001_event_state"));
    public static final StreamCodec<FriendlyByteBuf, Scp5001StatePayload> PayloadCodec = StreamCodec.ofMember(Scp5001StatePayload::write, Scp5001StatePayload::read);

    private void write(FriendlyByteBuf Buffer) {
        Buffer.writeVarInt(Phase);
        Buffer.writeLong(PhaseTick);
        Buffer.writeFloat(Lift);
        Buffer.writeVarInt(Overdriven);
        Buffer.writeVarInt(Generators);
        Buffer.writeVarInt(Rods);
        Buffer.writeBoolean(RodsSpent);
        Buffer.writeLong(CooldownEnd);
        Buffer.writeVarInt(SwarmWave);
        Buffer.writeVarInt(SwarmWaves);
        Buffer.writeVarInt(SwarmAlive);
        Buffer.writeBlockPos(Core);
        Buffer.writeBoolean(Located);
        Buffer.writeVarInt(Hot.size());
        for (BlockPos Pos : Hot) {
            Buffer.writeBlockPos(Pos);
        }
        Buffer.writeLong(RiseStart);
        Buffer.writeLong(ResetStart);
        Buffer.writeVarInt(RiseTicks);
        Buffer.writeVarInt(LatchTicks);
    }

    private static Scp5001StatePayload read(FriendlyByteBuf Buffer) {
        int Phase = Buffer.readVarInt();
        long PhaseTick = Buffer.readLong();
        float Lift = Buffer.readFloat();
        int Overdriven = Buffer.readVarInt();
        int Generators = Buffer.readVarInt();
        int Rods = Buffer.readVarInt();
        boolean Spent = Buffer.readBoolean();
        long Cooldown = Buffer.readLong();
        int Wave = Buffer.readVarInt();
        int Waves = Buffer.readVarInt();
        int Alive = Buffer.readVarInt();
        BlockPos Core = Buffer.readBlockPos();
        boolean Located = Buffer.readBoolean();
        int Count = Math.min(64, Buffer.readVarInt());
        List<BlockPos> Hot = new ArrayList<>();
        for (int Index = 0; Index < Count; Index++) {
            Hot.add(Buffer.readBlockPos());
        }
        long RiseStart = Buffer.readLong();
        long ResetStart = Buffer.readLong();
        int RiseTicks = Buffer.readVarInt();
        int LatchTicks = Buffer.readVarInt();
        return new Scp5001StatePayload(Phase, PhaseTick, Lift, Overdriven, Generators, Rods, Spent, Cooldown, Wave, Waves, Alive, Core, Located, Hot, RiseStart,
                ResetStart, RiseTicks, LatchTicks);
    }

    @Override
    public Type<Scp5001StatePayload> type() {
        return PayloadType;
    }
}
