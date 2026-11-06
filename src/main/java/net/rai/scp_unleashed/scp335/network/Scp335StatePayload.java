package net.rai.scp_unleashed.scp335.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp335.Scp335Phase;
import net.rai.scp_unleashed.scp335.Scp335Registry;

public record Scp335StatePayload(int EntityId, Scp335Phase Phase, int Tick, Vec3 Approach, Vec3 Screen, boolean Assisted, boolean Lit) implements CustomPacketPayload {
    public static final Type<Scp335StatePayload> PayloadType = new Type<>(Scp335Registry.location("scp335_state"));
    public static final StreamCodec<FriendlyByteBuf, Scp335StatePayload> PayloadCodec = StreamCodec.of((Buffer, Payload) -> Payload.write(Buffer), Scp335StatePayload::read);

    public static Scp335StatePayload cleared(int EntityId) {
        return new Scp335StatePayload(EntityId, Scp335Phase.None, 0, Vec3.ZERO, Vec3.ZERO, false, false);
    }

    private void write(FriendlyByteBuf Buffer) {
        Buffer.writeVarInt(EntityId);
        Buffer.writeByte(Phase.ordinal());
        Buffer.writeVarInt(Tick);
        writeVec(Buffer, Approach);
        writeVec(Buffer, Screen);
        Buffer.writeByte((Assisted ? 1 : 0) | (Lit ? 2 : 0));
    }

    private static Scp335StatePayload read(FriendlyByteBuf Buffer) {
        int EntityId = Buffer.readVarInt();
        Scp335Phase Phase = Scp335Phase.byId(Buffer.readByte());
        int Tick = Buffer.readVarInt();
        Vec3 Approach = readVec(Buffer);
        Vec3 Screen = readVec(Buffer);
        int Flags = Buffer.readByte();
        return new Scp335StatePayload(EntityId, Phase, Tick, Approach, Screen, (Flags & 1) != 0, (Flags & 2) != 0);
    }

    private static void writeVec(FriendlyByteBuf Buffer, Vec3 Point) {
        Buffer.writeDouble(Point.x);
        Buffer.writeDouble(Point.y);
        Buffer.writeDouble(Point.z);
    }

    private static Vec3 readVec(FriendlyByteBuf Buffer) {
        return new Vec3(Buffer.readDouble(), Buffer.readDouble(), Buffer.readDouble());
    }

    @Override
    public Type<Scp335StatePayload> type() {
        return PayloadType;
    }
}
