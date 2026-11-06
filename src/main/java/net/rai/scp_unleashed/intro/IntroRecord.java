package net.rai.scp_unleashed.intro;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public record IntroRecord(boolean Done, boolean Started, Optional<Spot> Return) {
    public static final IntroRecord Fresh = new IntroRecord(false, false, Optional.empty());

    public record Spot(ResourceKey<Level> Dimension, double X, double Y, double Z, float Yaw, float Pitch, float Fall) {
        public static final Codec<Spot> SpotCodec = RecordCodecBuilder.create(Instance -> Instance.group(
                ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(Spot::Dimension),
                Codec.DOUBLE.fieldOf("x").forGetter(Spot::X),
                Codec.DOUBLE.fieldOf("y").forGetter(Spot::Y),
                Codec.DOUBLE.fieldOf("z").forGetter(Spot::Z),
                Codec.FLOAT.fieldOf("yaw").forGetter(Spot::Yaw),
                Codec.FLOAT.fieldOf("pitch").forGetter(Spot::Pitch),
                Codec.FLOAT.optionalFieldOf("fall", 0.0f).forGetter(Spot::Fall)
        ).apply(Instance, Spot::new));

        public Vec3 position() {
            return new Vec3(X, Y, Z);
        }
    }

    public static final Codec<IntroRecord> RecordCodec = RecordCodecBuilder.create(Instance -> Instance.group(
            Codec.BOOL.optionalFieldOf("done", false).forGetter(IntroRecord::Done),
            Codec.BOOL.optionalFieldOf("started", false).forGetter(IntroRecord::Started),
            Spot.SpotCodec.optionalFieldOf("return").forGetter(IntroRecord::Return)
    ).apply(Instance, IntroRecord::new));

    public boolean active() {
        return Return.isPresent();
    }

    public IntroRecord finished() {
        return new IntroRecord(true, true, Optional.empty());
    }

    public IntroRecord begun(Spot Back) {
        return new IntroRecord(Done, true, Optional.of(Back));
    }

    public IntroRecord resumed() {
        return new IntroRecord(Done, true, Return);
    }

    public IntroRecord cleared() {
        return new IntroRecord(Done, Started, Optional.empty());
    }

    public IntroRecord reset() {
        return new IntroRecord(false, false, Return);
    }
}
