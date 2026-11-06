package net.rai.scp_unleashed.scp1730.rooms;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.ArrayList;
import java.util.List;

public record Scp1730RoomsPayload(String Dimension, List<Scp1730SpecialRoom> Rooms) implements CustomPacketPayload {
    public static final int MaxRooms = 256;
    public static final Type<Scp1730RoomsPayload> PayloadType = new Type<>(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp1730_rooms"));
    public static final StreamCodec<FriendlyByteBuf, Scp1730RoomsPayload> PayloadCodec = StreamCodec.of((Buffer, Payload) -> Payload.write(Buffer), Scp1730RoomsPayload::read);

    private void write(FriendlyByteBuf Buffer) {
        Buffer.writeUtf(Dimension);
        int Count = Math.min(MaxRooms, Rooms.size());
        Buffer.writeVarInt(Count);
        for (int Index = 0; Index < Count; Index++) {
            Scp1730SpecialRoom Room = Rooms.get(Index);
            Buffer.writeVarInt(Room.Kind().ordinal());
            Buffer.writeInt(Room.MinX());
            Buffer.writeInt(Room.MinZ());
            Buffer.writeInt(Room.MaxX());
            Buffer.writeInt(Room.MaxZ());
            Buffer.writeVarInt(Room.Floor());
            Buffer.writeVarInt(Room.Height());
            Buffer.writeVarInt(Room.Doors().size());
            for (Scp1730SpecialRoom.Door Door : Room.Doors()) {
                Buffer.writeInt(Door.X());
                Buffer.writeInt(Door.Z());
                Buffer.writeByte(Door.Dir());
            }
        }
    }

    private static Scp1730RoomsPayload read(FriendlyByteBuf Buffer) {
        String Dimension = Buffer.readUtf();
        int Count = Math.min(MaxRooms, Buffer.readVarInt());
        List<Scp1730SpecialRoom> Rooms = new ArrayList<>();
        Scp1730RoomKind[] Kinds = Scp1730RoomKind.values();
        for (int Index = 0; Index < Count; Index++) {
            int Kind = Buffer.readVarInt();
            int MinX = Buffer.readInt();
            int MinZ = Buffer.readInt();
            int MaxX = Buffer.readInt();
            int MaxZ = Buffer.readInt();
            int Floor = Buffer.readVarInt();
            int Height = Buffer.readVarInt();
            int Doors = Math.min(16, Buffer.readVarInt());
            List<Scp1730SpecialRoom.Door> Made = new ArrayList<>();
            for (int Door = 0; Door < Doors; Door++) {
                Made.add(new Scp1730SpecialRoom.Door(Buffer.readInt(), Buffer.readInt(), Buffer.readByte() & 3));
            }
            if (Kind >= 0 && Kind < Kinds.length) {
                Rooms.add(new Scp1730SpecialRoom(Kinds[Kind], MinX, MinZ, MaxX, MaxZ, Floor, Height, List.copyOf(Made)));
            }
        }
        return new Scp1730RoomsPayload(Dimension, Rooms);
    }

    public ResourceKey<Level> dimension() {
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(Dimension));
    }

    @Override
    public Type<Scp1730RoomsPayload> type() {
        return PayloadType;
    }
}
