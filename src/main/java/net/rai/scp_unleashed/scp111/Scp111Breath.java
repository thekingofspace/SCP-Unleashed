package net.rai.scp_unleashed.scp111;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public record Scp111Breath(int Style, int Core, int Edge, float Size) implements ParticleOptions {
    public static final int Flame = 0;
    public static final int Smoke = 1;
    public static final int Spark = 2;
    public static final int Ember = 3;
    public static final int WispCore = 0x9C9C9C;
    public static final int WispEdge = 0x4A4A4E;
    public static final float FlameSize = 0.05f;
    public static final float SmokeSize = 0.06f;
    public static final float SparkSize = 0.035f;
    public static final float EmberSize = 0.025f;
    public static final double PuffSpeed = 0.035;
    public static final double JetFriction = 0.9;
    public static final int JetLife = 12;
    public static final float JetScale = 1.5f;
    public static final int JetCount = 4;
    public static final MapCodec<Scp111Breath> BreathCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            Codec.intRange(0, 3).fieldOf("style").forGetter(Scp111Breath::Style),
            Codec.INT.fieldOf("core").forGetter(Scp111Breath::Core),
            Codec.INT.fieldOf("edge").forGetter(Scp111Breath::Edge),
            Codec.FLOAT.fieldOf("size").forGetter(Scp111Breath::Size)
    ).apply(Instance, Scp111Breath::new));
    public static final StreamCodec<ByteBuf, Scp111Breath> BreathStreamCodec = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Scp111Breath::Style,
            ByteBufCodecs.INT, Scp111Breath::Core,
            ByteBufCodecs.INT, Scp111Breath::Edge,
            ByteBufCodecs.FLOAT, Scp111Breath::Size,
            Scp111Breath::new);

    @Override
    public ParticleType<?> getType() {
        return Scp111Registry.Breath.get();
    }

    public static Scp111Breath pick(Scp111Variant Variant, RandomSource Random, float Scale) {
        float Roll = Random.nextFloat();
        return switch (Variant.getStyle()) {
            case Smoke -> Roll < 0.8f ? smoke(Variant.getCore(), Variant.getEdge(), Scale) : ember(Variant, Scale);
            case Spark -> Roll < 0.7f ? new Scp111Breath(Spark, Variant.getCore(), Variant.getEdge(), SparkSize * Scale) : flame(Variant, Scale);
            case Ember -> Roll < 0.6f ? flame(Variant, Scale) : ember(Variant, Scale);
            default -> Roll < 0.8f ? flame(Variant, Scale) : smoke(WispCore, WispEdge, Scale * 0.8f);
        };
    }

    private static Scp111Breath flame(Scp111Variant Variant, float Scale) {
        return new Scp111Breath(Flame, Variant.getCore(), Variant.getEdge(), FlameSize * Scale);
    }

    private static Scp111Breath smoke(int Core, int Edge, float Scale) {
        return new Scp111Breath(Smoke, Core, Edge, SmokeSize * Scale);
    }

    private static Scp111Breath ember(Scp111Variant Variant, float Scale) {
        int Hot = Variant.getStyle() == Scp111Variant.Breath.Smoke ? 0xFF8A3A : Variant.getEdge();
        return new Scp111Breath(Ember, 0xFFF2C0, Hot, EmberSize * Scale);
    }

    public static void puff(ServerLevel World, Vec3 Mouth, Vec3 Facing, Scp111Variant Variant, int Count, float Scale) {
        RandomSource Random = World.random;
        for (int Index = 0; Index < Count; Index++) {
            double Speed = PuffSpeed * (0.6 + Random.nextDouble() * 0.8) * Math.max(0.5, Scale);
            Vec3 Motion = Facing.scale(Speed).add((Random.nextDouble() - 0.5) * 0.012, 0.004 + Random.nextDouble() * 0.008, (Random.nextDouble() - 0.5) * 0.012);
            send(World, pick(Variant, Random, Scale), Mouth, Motion);
        }
    }

    public static void jet(ServerLevel World, Vec3 Mouth, Vec3 Direction, Scp111Variant Variant, int Count, float Scale, double Reach) {
        RandomSource Random = World.random;
        double Launch = Reach * (1.0 - JetFriction) / (1.0 - Math.pow(JetFriction, JetLife));
        for (int Index = 0; Index < Count; Index++) {
            double Speed = Launch * (0.75 + Random.nextDouble() * 0.4);
            Vec3 Spread = new Vec3((Random.nextDouble() - 0.5) * 0.09, (Random.nextDouble() - 0.5) * 0.06, (Random.nextDouble() - 0.5) * 0.09).scale(Speed * 4.0);
            Vec3 Motion = Direction.scale(Speed).add(Spread);
            send(World, pick(Variant, Random, Scale * JetScale), Mouth.add(Motion.scale(Random.nextDouble())), Motion);
        }
    }

    private static void send(ServerLevel World, Scp111Breath Options, Vec3 From, Vec3 Motion) {
        World.sendParticles(Options, From.x, From.y, From.z, 0, Motion.x, Motion.y, Motion.z, 1.0);
    }
}
