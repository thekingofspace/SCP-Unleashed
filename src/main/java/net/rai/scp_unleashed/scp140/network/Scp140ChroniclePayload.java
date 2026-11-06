package net.rai.scp_unleashed.scp140.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp140.api.Scp140Pages;

import java.util.ArrayList;
import java.util.List;

public record Scp140ChroniclePayload(boolean Open, int Written, float Progress, long SeenMask, int Mood, List<PageEntry> Pages) implements CustomPacketPayload {
    public static final Type<Scp140ChroniclePayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp140_chronicle"));
    public static final StreamCodec<FriendlyByteBuf, Scp140ChroniclePayload> PayloadCodec = StreamCodec.ofMember(Scp140ChroniclePayload::write, Scp140ChroniclePayload::read);

    public static Scp140ChroniclePayload read(FriendlyByteBuf Buffer) {
        boolean Open = Buffer.readBoolean();
        int Written = Math.max(0, Math.min(Scp140Pages.Total, Buffer.readVarInt()));
        float Progress = Buffer.readFloat();
        long SeenMask = Buffer.readLong();
        int Mood = Buffer.readVarInt();
        int Count = Math.max(0, Math.min(Scp140Pages.Total, Buffer.readVarInt()));
        List<PageEntry> Pages = new ArrayList<>(Count);
        for (int Index = 0; Index < Count; Index++) {
            Pages.add(new PageEntry(Buffer.readVarInt(), Buffer.readBoolean(), Buffer.readBoolean(), Buffer.readVarInt(), Buffer.readVarInt(), Buffer.readVarLong()));
        }
        return new Scp140ChroniclePayload(Open, Written, Progress, SeenMask, Mood, Pages);
    }

    public void write(FriendlyByteBuf Buffer) {
        Buffer.writeBoolean(Open);
        Buffer.writeVarInt(Written);
        Buffer.writeFloat(Progress);
        Buffer.writeLong(SeenMask);
        Buffer.writeVarInt(Mood);
        Buffer.writeVarInt(Pages.size());
        for (PageEntry Entry : Pages) {
            Buffer.writeVarInt(Entry.Page());
            Buffer.writeBoolean(Entry.HasSite());
            Buffer.writeBoolean(Entry.SameRealm());
            Buffer.writeVarInt(Entry.OffsetX());
            Buffer.writeVarInt(Entry.OffsetZ());
            Buffer.writeVarLong(Entry.Day());
        }
    }

    @Override
    public Type<Scp140ChroniclePayload> type() {
        return PayloadType;
    }

    public record PageEntry(int Page, boolean HasSite, boolean SameRealm, int OffsetX, int OffsetZ, long Day) {
    }
}
