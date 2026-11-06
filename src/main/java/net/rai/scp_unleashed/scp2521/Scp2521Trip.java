package net.rai.scp_unleashed.scp2521;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public record Scp2521Trip(boolean Active, ResourceLocation Dimension, double X, double Y, double Z, float Yaw, float Pitch, int Plot, int Stage, int Ticks) {
    public static final Scp2521Trip None = new Scp2521Trip(false, Level.OVERWORLD.location(), 0.0, 0.0, 0.0, 0.0f, 0.0f, -1, 0, 0);
    public static final Codec<Scp2521Trip> TripCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            Codec.BOOL.optionalFieldOf("active", false).forGetter(Scp2521Trip::Active),
            ResourceLocation.CODEC.optionalFieldOf("dimension", Level.OVERWORLD.location()).forGetter(Scp2521Trip::Dimension),
            Codec.DOUBLE.optionalFieldOf("x", 0.0).forGetter(Scp2521Trip::X),
            Codec.DOUBLE.optionalFieldOf("y", 0.0).forGetter(Scp2521Trip::Y),
            Codec.DOUBLE.optionalFieldOf("z", 0.0).forGetter(Scp2521Trip::Z),
            Codec.FLOAT.optionalFieldOf("yaw", 0.0f).forGetter(Scp2521Trip::Yaw),
            Codec.FLOAT.optionalFieldOf("pitch", 0.0f).forGetter(Scp2521Trip::Pitch),
            Codec.INT.optionalFieldOf("plot", -1).forGetter(Scp2521Trip::Plot),
            Codec.INT.optionalFieldOf("stage", 0).forGetter(Scp2521Trip::Stage),
            Codec.INT.optionalFieldOf("ticks", 0).forGetter(Scp2521Trip::Ticks)
    ).apply(Instance, Scp2521Trip::new));

    public boolean active() {
        return Active;
    }

    public ResourceKey<Level> dimension() {
        return ResourceKey.create(Registries.DIMENSION, Dimension);
    }

    public Vec3 spot() {
        return new Vec3(X, Y, Z);
    }

    public Scp2521Trip at(int NewStage, int NewTicks) {
        return new Scp2521Trip(Active, Dimension, X, Y, Z, Yaw, Pitch, Plot, NewStage, NewTicks);
    }
}
