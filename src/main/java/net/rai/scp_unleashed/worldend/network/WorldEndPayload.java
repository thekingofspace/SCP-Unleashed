package net.rai.scp_unleashed.worldend.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.worldend.WorldEndRegistry;

public record WorldEndPayload(int Stage, int Elapsed, int Duration, boolean Focused, Vec3 Focus, Component Message) implements CustomPacketPayload {
    public static final int Idle = 0;
    public static final int Freezing = 1;
    public static final int Released = 2;
    public static final int Final = 3;
    public static final Type<WorldEndPayload> PayloadType = new Type<>(WorldEndRegistry.location("worldend_stage"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WorldEndPayload> PayloadCodec = StreamCodec.ofMember(WorldEndPayload::write, WorldEndPayload::read);

    private static WorldEndPayload read(RegistryFriendlyByteBuf Buffer) {
        int Stage = Buffer.readVarInt();
        int Elapsed = Buffer.readVarInt();
        int Duration = Buffer.readVarInt();
        boolean Focused = Buffer.readBoolean();
        Vec3 Focus = new Vec3(Buffer.readDouble(), Buffer.readDouble(), Buffer.readDouble());
        Component Message = ComponentSerialization.TRUSTED_STREAM_CODEC.decode(Buffer);
        return new WorldEndPayload(Stage, Elapsed, Duration, Focused, Focus, Message);
    }

    private void write(RegistryFriendlyByteBuf Buffer) {
        Buffer.writeVarInt(Stage);
        Buffer.writeVarInt(Elapsed);
        Buffer.writeVarInt(Duration);
        Buffer.writeBoolean(Focused);
        Buffer.writeDouble(Focus.x);
        Buffer.writeDouble(Focus.y);
        Buffer.writeDouble(Focus.z);
        ComponentSerialization.TRUSTED_STREAM_CODEC.encode(Buffer, Message);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PayloadType;
    }
}
