package net.rai.scp_unleashed.scp5001.xk.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp5001.xk.Scp5001XkRegistry;

import java.util.ArrayList;
import java.util.List;

public record YaldabaothPayload(int State, Vec3 Head, Vec3 Facing, float Jaw, int Flinch, int Mode, Vec3 Portal, Vec3 BindDirection, float BindProgress,
                                List<Vec3> Trail, long Stamp) implements CustomPacketPayload {
    public static final int Gone = 0;
    public static final int Present = 1;
    public static final int Banishing = 2;
    public static final Type<YaldabaothPayload> PayloadType = new Type<>(Scp5001XkRegistry.location("scp5001_xk_yaldabaoth"));
    public static final StreamCodec<FriendlyByteBuf, YaldabaothPayload> PayloadCodec = StreamCodec.ofMember(YaldabaothPayload::write, YaldabaothPayload::read);

    private static Vec3 vector(FriendlyByteBuf Buffer) {
        return new Vec3(Buffer.readDouble(), Buffer.readDouble(), Buffer.readDouble());
    }

    private static void vector(FriendlyByteBuf Buffer, Vec3 Value) {
        Buffer.writeDouble(Value.x);
        Buffer.writeDouble(Value.y);
        Buffer.writeDouble(Value.z);
    }

    private static YaldabaothPayload read(FriendlyByteBuf Buffer) {
        int State = Buffer.readByte();
        Vec3 Head = vector(Buffer);
        Vec3 Facing = new Vec3(Buffer.readFloat(), Buffer.readFloat(), Buffer.readFloat());
        float Jaw = Buffer.readFloat();
        int Flinch = Buffer.readByte();
        int Mode = Buffer.readByte();
        Vec3 Portal = vector(Buffer);
        Vec3 BindDirection = new Vec3(Buffer.readFloat(), Buffer.readFloat(), Buffer.readFloat());
        float BindProgress = Buffer.readFloat();
        int Count = Math.min(256, Buffer.readVarInt());
        List<Vec3> Trail = new ArrayList<>(Count);
        for (int Index = 0; Index < Count; Index++) {
            Trail.add(new Vec3(Buffer.readFloat(), Buffer.readFloat(), Buffer.readFloat()).add(Head));
        }
        long Stamp = Buffer.readVarLong();
        return new YaldabaothPayload(State, Head, Facing, Jaw, Flinch, Mode, Portal, BindDirection, BindProgress, Trail, Stamp);
    }

    private void write(FriendlyByteBuf Buffer) {
        Buffer.writeByte(State);
        vector(Buffer, Head);
        Buffer.writeFloat((float) Facing.x);
        Buffer.writeFloat((float) Facing.y);
        Buffer.writeFloat((float) Facing.z);
        Buffer.writeFloat(Jaw);
        Buffer.writeByte(Flinch);
        Buffer.writeByte(Mode);
        vector(Buffer, Portal);
        Buffer.writeFloat((float) BindDirection.x);
        Buffer.writeFloat((float) BindDirection.y);
        Buffer.writeFloat((float) BindDirection.z);
        Buffer.writeFloat(BindProgress);
        Buffer.writeVarInt(Trail.size());
        for (Vec3 Point : Trail) {
            Buffer.writeFloat((float) (Point.x - Head.x));
            Buffer.writeFloat((float) (Point.y - Head.y));
            Buffer.writeFloat((float) (Point.z - Head.z));
        }
        Buffer.writeVarLong(Stamp);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PayloadType;
    }
}
