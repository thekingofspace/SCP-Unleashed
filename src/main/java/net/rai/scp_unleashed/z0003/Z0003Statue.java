package net.rai.scp_unleashed.z0003;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

public record Z0003Statue(float BodyYaw, float HeadYaw, float Pitch, float WalkPosition, float WalkSpeed, float Swing, boolean Crouching, boolean Running,
                          double X, double Y, double Z, long Since, long CrackStart) {
    public static final long NotCracking = 0L;
    public static final Codec<Z0003Statue> StatueCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            Codec.FLOAT.fieldOf("body_yaw").forGetter(Z0003Statue::BodyYaw),
            Codec.FLOAT.fieldOf("head_yaw").forGetter(Z0003Statue::HeadYaw),
            Codec.FLOAT.fieldOf("pitch").forGetter(Z0003Statue::Pitch),
            Codec.FLOAT.fieldOf("walk_position").forGetter(Z0003Statue::WalkPosition),
            Codec.FLOAT.fieldOf("walk_speed").forGetter(Z0003Statue::WalkSpeed),
            Codec.FLOAT.fieldOf("swing").forGetter(Z0003Statue::Swing),
            Codec.BOOL.fieldOf("crouching").forGetter(Z0003Statue::Crouching),
            Codec.BOOL.optionalFieldOf("running", false).forGetter(Z0003Statue::Running),
            Codec.DOUBLE.fieldOf("x").forGetter(Z0003Statue::X),
            Codec.DOUBLE.fieldOf("y").forGetter(Z0003Statue::Y),
            Codec.DOUBLE.fieldOf("z").forGetter(Z0003Statue::Z),
            Codec.LONG.fieldOf("since").forGetter(Z0003Statue::Since),
            Codec.LONG.optionalFieldOf("crack_start", NotCracking).forGetter(Z0003Statue::CrackStart)
    ).apply(Instance, Z0003Statue::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, Z0003Statue> StatueStreamCodec = StreamCodec.of(Z0003Statue::write, Z0003Statue::read);

    private static void write(RegistryFriendlyByteBuf Buffer, Z0003Statue Statue) {
        Buffer.writeFloat(Statue.BodyYaw);
        Buffer.writeFloat(Statue.HeadYaw);
        Buffer.writeFloat(Statue.Pitch);
        Buffer.writeFloat(Statue.WalkPosition);
        Buffer.writeFloat(Statue.WalkSpeed);
        Buffer.writeFloat(Statue.Swing);
        Buffer.writeBoolean(Statue.Crouching);
        Buffer.writeBoolean(Statue.Running);
        Buffer.writeDouble(Statue.X);
        Buffer.writeDouble(Statue.Y);
        Buffer.writeDouble(Statue.Z);
        Buffer.writeVarLong(Statue.Since);
        Buffer.writeVarLong(Statue.CrackStart);
    }

    private static Z0003Statue read(RegistryFriendlyByteBuf Buffer) {
        return new Z0003Statue(Buffer.readFloat(), Buffer.readFloat(), Buffer.readFloat(), Buffer.readFloat(), Buffer.readFloat(), Buffer.readFloat(),
                Buffer.readBoolean(), Buffer.readBoolean(), Buffer.readDouble(), Buffer.readDouble(), Buffer.readDouble(), Buffer.readVarLong(), Buffer.readVarLong());
    }

    public boolean cracking() {
        return CrackStart != NotCracking;
    }

    public Z0003Statue withCrack(long Now) {
        return new Z0003Statue(BodyYaw, HeadYaw, Pitch, WalkPosition, WalkSpeed, Swing, Crouching, Running, X, Y, Z, Since, Math.max(1L, Now));
    }

    public Z0003Statue at(double NewX, double NewY, double NewZ) {
        return new Z0003Statue(BodyYaw, HeadYaw, Pitch, WalkPosition, WalkSpeed, Swing, Crouching, Running, NewX, NewY, NewZ, Since, CrackStart);
    }

    public Z0003Statue since(long Value) {
        return new Z0003Statue(BodyYaw, HeadYaw, Pitch, WalkPosition, WalkSpeed, Swing, Crouching, Running, X, Y, Z, Value, CrackStart);
    }

    public float crackProgress(long Now, float Partial, int Ticks) {
        if (!cracking()) {
            return 0.0f;
        }
        return Mth.clamp((Now - CrackStart + Partial) / Math.max(1, Ticks), 0.0f, 1.0f);
    }

    public long age(long Now) {
        return Math.max(0L, Now - Since);
    }
}
