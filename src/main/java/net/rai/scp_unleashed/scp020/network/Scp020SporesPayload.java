package net.rai.scp_unleashed.scp020.network;

import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public record Scp020SporesPayload(boolean Active, boolean Camera, long[] Hosts, byte[] Masks) implements CustomPacketPayload {
    public static final int MaxHosts = 1024;
    public static final Type<Scp020SporesPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp020_spores"));
    public static final Scp020SporesPayload Off = new Scp020SporesPayload(false, false, new long[0], new byte[0]);
    public static final StreamCodec<FriendlyByteBuf, Scp020SporesPayload> PayloadCodec = StreamCodec.of(Scp020SporesPayload::write, Scp020SporesPayload::read);

    public int size() {
        return Math.min(Hosts.length, Masks.length);
    }

    private static void write(FriendlyByteBuf Buffer, Scp020SporesPayload Payload) {
        Buffer.writeBoolean(Payload.Active());
        Buffer.writeBoolean(Payload.Camera());
        int Count = Math.min(MaxHosts, Payload.size());
        Buffer.writeVarInt(Count);
        for (int Index = 0; Index < Count; Index++) {
            Buffer.writeLong(Payload.Hosts()[Index]);
            Buffer.writeByte(Payload.Masks()[Index]);
        }
    }

    private static Scp020SporesPayload read(FriendlyByteBuf Buffer) {
        boolean Active = Buffer.readBoolean();
        boolean Camera = Buffer.readBoolean();
        int Count = Buffer.readVarInt();
        if (Count < 0 || Count > MaxHosts) {
            throw new DecoderException("Too many SCP-020 hosts: " + Count);
        }
        long[] Hosts = new long[Count];
        byte[] Masks = new byte[Count];
        for (int Index = 0; Index < Count; Index++) {
            Hosts[Index] = Buffer.readLong();
            Masks[Index] = Buffer.readByte();
        }
        return new Scp020SporesPayload(Active, Camera, Hosts, Masks);
    }

    @Override
    public Type<Scp020SporesPayload> type() {
        return PayloadType;
    }
}
