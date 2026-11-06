package net.rai.scp_unleashed.upgrade;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.ArrayList;
import java.util.List;

public record MufflerPayload(boolean Replace, List<Machine> Added, long[] Removed) implements CustomPacketPayload {
    public static final Type<MufflerPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "sound_mufflers"));
    public static final StreamCodec<FriendlyByteBuf, MufflerPayload> PayloadCodec = CustomPacketPayload.codec(MufflerPayload::write, MufflerPayload::read);
    public static final int MaxMachines = 65536;

    public record Machine(long Core, long[] Cells) {
    }

    public static MufflerPayload replace(List<MufflerData.Entry> Entries) {
        List<Machine> Added = new ArrayList<>(Entries.size());
        for (MufflerData.Entry Known : Entries) {
            Added.add(new Machine(Known.Core(), Known.Cells()));
        }
        return new MufflerPayload(true, Added, new long[0]);
    }

    public static MufflerPayload added(MufflerData.Entry Known) {
        return new MufflerPayload(false, List.of(new Machine(Known.Core(), Known.Cells())), new long[0]);
    }

    public static MufflerPayload removed(long Core) {
        return new MufflerPayload(false, List.of(), new long[]{Core});
    }

    private static MufflerPayload read(FriendlyByteBuf Buffer) {
        boolean Replace = Buffer.readBoolean();
        int Count = Math.min(Buffer.readVarInt(), MaxMachines);
        List<Machine> Added = new ArrayList<>(Count);
        for (int Index = 0; Index < Count; Index++) {
            Added.add(new Machine(Buffer.readLong(), Buffer.readLongArray()));
        }
        return new MufflerPayload(Replace, Added, Buffer.readLongArray());
    }

    private void write(FriendlyByteBuf Buffer) {
        Buffer.writeBoolean(Replace);
        Buffer.writeVarInt(Added.size());
        for (Machine Sent : Added) {
            Buffer.writeLong(Sent.Core());
            Buffer.writeLongArray(Sent.Cells());
        }
        Buffer.writeLongArray(Removed);
    }

    @Override
    public Type<MufflerPayload> type() {
        return PayloadType;
    }
}
