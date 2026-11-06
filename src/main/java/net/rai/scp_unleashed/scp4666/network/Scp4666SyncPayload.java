package net.rai.scp_unleashed.scp4666.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.List;

public record Scp4666SyncPayload(int Night, long Clock, float Rate, int Flags, int CentreX, int CentreZ, List<Integer> Bounds, List<Integer> Tasks) implements CustomPacketPayload {
    public static final int MaxTasks = 12;
    public static final Type<Scp4666SyncPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp4666_sync"));
    private static final StreamCodec<ByteBuf, List<Integer>> BoundsCodec = ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(6));
    private static final StreamCodec<ByteBuf, List<Integer>> TasksCodec = ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(MaxTasks));
    public static final StreamCodec<ByteBuf, Scp4666SyncPayload> PayloadCodec = StreamCodec.of(Scp4666SyncPayload::write, Scp4666SyncPayload::read);

    private static void write(ByteBuf Buffer, Scp4666SyncPayload Payload) {
        ByteBufCodecs.VAR_INT.encode(Buffer, Payload.Night());
        ByteBufCodecs.VAR_LONG.encode(Buffer, Payload.Clock());
        ByteBufCodecs.FLOAT.encode(Buffer, Payload.Rate());
        ByteBufCodecs.VAR_INT.encode(Buffer, Payload.Flags());
        ByteBufCodecs.VAR_INT.encode(Buffer, Payload.CentreX());
        ByteBufCodecs.VAR_INT.encode(Buffer, Payload.CentreZ());
        BoundsCodec.encode(Buffer, Payload.Bounds());
        TasksCodec.encode(Buffer, Payload.Tasks().size() > MaxTasks ? Payload.Tasks().subList(0, MaxTasks) : Payload.Tasks());
    }

    private static Scp4666SyncPayload read(ByteBuf Buffer) {
        int Night = ByteBufCodecs.VAR_INT.decode(Buffer);
        long Clock = ByteBufCodecs.VAR_LONG.decode(Buffer);
        float Rate = ByteBufCodecs.FLOAT.decode(Buffer);
        int Flags = ByteBufCodecs.VAR_INT.decode(Buffer);
        int CentreX = ByteBufCodecs.VAR_INT.decode(Buffer);
        int CentreZ = ByteBufCodecs.VAR_INT.decode(Buffer);
        List<Integer> Bounds = BoundsCodec.decode(Buffer);
        List<Integer> Tasks = TasksCodec.decode(Buffer);
        return new Scp4666SyncPayload(Night, Clock, Rate, Flags, CentreX, CentreZ, Bounds, Tasks);
    }

    public static int pack(int Phase, int Tree, boolean LightsDead, boolean Running, boolean Started) {
        return (Phase & 15) | (Tree & 15) << 4 | (LightsDead ? 1 : 0) << 8 | (Running ? 1 : 0) << 9 | (Started ? 1 : 0) << 10;
    }

    public int phase() {
        return Flags & 15;
    }

    public int tree() {
        return Flags >> 4 & 15;
    }

    public boolean lightsDead() {
        return (Flags >> 8 & 1) != 0;
    }

    public boolean running() {
        return (Flags >> 9 & 1) != 0;
    }

    public boolean started() {
        return (Flags >> 10 & 1) != 0;
    }

    @Override
    public Type<Scp4666SyncPayload> type() {
        return PayloadType;
    }
}
