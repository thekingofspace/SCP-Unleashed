package net.rai.scp_unleashed.stasis.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.rift.RiftParticleOptions;
import net.rai.scp_unleashed.rift.RiftRegistry;
import net.rai.scp_unleashed.stasis.StasisRegistry;
import net.rai.scp_unleashed.stasis.network.StasisEffectPayload;

public final class StasisClientEffects {
    public static final double SparkPull = 0.2;
    public static final double MotePull = 0.065;
    public static final int StreamMotes = 14;

    private StasisClientEffects() {
    }

    public static void play(StasisEffectPayload Payload) {
        ClientLevel World = Minecraft.getInstance().level;
        if (World == null) {
            return;
        }
        FragmentRarity Grade = FragmentRarity.byTier(Payload.Tier());
        int Colour = Grade.getColour();
        Vec3 Base = new Vec3(Payload.X(), Payload.Y(), Payload.Z());
        float Width = Math.max(0.3f, Payload.Width());
        float Height = Math.max(0.3f, Payload.Height());
        switch (Payload.Kind()) {
            case StasisEffectPayload.Captured -> capture(World, Base, Width, Height, Colour, Payload.Holder());
            case StasisEffectPayload.Released -> release(World, Base, Width, Height, Colour);
            case StasisEffectPayload.Burst -> burst(World, Base, Width, Height, Colour, Grade);
            case StasisEffectPayload.Denied -> deny(World, Base, Colour);
            case StasisEffectPayload.Rattled -> rattle(World, Base, Colour);
            default -> {
            }
        }
    }

    private static Vec3 inside(RandomSource Random, Vec3 Base, float Width, float Height) {
        return new Vec3(Base.x + (Random.nextDouble() - 0.5) * Width, Base.y + Random.nextDouble() * Height, Base.z + (Random.nextDouble() - 0.5) * Width);
    }

    private static Vec3 heading(RandomSource Random) {
        Vec3 Way = new Vec3(Random.nextGaussian(), Random.nextGaussian() * 0.8, Random.nextGaussian());
        return Way.lengthSqr() < 1.0e-6 ? new Vec3(0.0, 1.0, 0.0) : Way.normalize();
    }

    private static void spark(ClientLevel World, int Colour, float Scale, Vec3 At, Vec3 Motion) {
        World.addParticle(new RiftParticleOptions(RiftRegistry.RiftSpark.get(), Colour, -1, Scale), At.x, At.y, At.z, Motion.x, Motion.y, Motion.z);
    }

    private static void mote(ClientLevel World, int Colour, float Scale, Vec3 At, Vec3 Motion) {
        World.addParticle(new RiftParticleOptions(RiftRegistry.RiftMote.get(), Colour, -1, Scale), At.x, At.y, At.z, Motion.x, Motion.y, Motion.z);
    }

    private static void shockwave(ClientLevel World, int Colour, float Scale, Vec3 At) {
        World.addParticle(new RiftParticleOptions(RiftRegistry.RiftShockwave.get(), Colour, -1, Scale), At.x, At.y, At.z, 0.0, 0.0, 0.0);
    }

    private static void capture(ClientLevel World, Vec3 Base, float Width, float Height, int Colour, int HolderId) {
        RandomSource Random = World.getRandom();
        Vec3 Centre = Base.add(0.0, Height * 0.5, 0.0);
        int Count = Mth.clamp(Math.round(16 + Width * Height * 10), 16, 44);
        for (int Index = 0; Index < Count; Index++) {
            Vec3 From = inside(Random, Base, Width * 1.2f, Height * 1.1f);
            spark(World, Colour, 0.9f, From, Centre.subtract(From).scale(SparkPull));
            if (Index % 3 == 0) {
                mote(World, Colour, 0.8f, From, Centre.subtract(From).scale(MotePull));
            }
        }
        shockwave(World, Colour, 0.22f + 0.12f * Math.max(Width, Height), Centre);
        Entity Holder = HolderId >= 0 ? World.getEntity(HolderId) : null;
        if (Holder == null) {
            return;
        }
        Vec3 Hand = Holder.getEyePosition().add(Holder.getLookAngle().scale(0.45)).add(0.0, -0.45, 0.0);
        Vec3 Span = Hand.subtract(Centre);
        for (int Index = 0; Index < StreamMotes; Index++) {
            double Along = (double) Index / StreamMotes;
            Vec3 At = Centre.add(Span.scale(Along * 0.6)).add((Random.nextDouble() - 0.5) * 0.2, (Random.nextDouble() - 0.5) * 0.2, (Random.nextDouble() - 0.5) * 0.2);
            mote(World, Colour, 0.7f, At, Span.scale(MotePull * (1.0 - Along * 0.5)));
        }
    }

    private static void release(ClientLevel World, Vec3 Base, float Width, float Height, int Colour) {
        RandomSource Random = World.getRandom();
        Vec3 Centre = Base.add(0.0, Height * 0.5, 0.0);
        shockwave(World, Colour, 0.25f + 0.15f * Math.max(Width, Height), Centre);
        int Count = Mth.clamp(Math.round(14 + Width * Height * 8), 14, 40);
        for (int Index = 0; Index < Count; Index++) {
            Vec3 Edge = inside(Random, Base, Width * 1.3f, Height * 1.2f);
            spark(World, Colour, 0.8f, Centre, Edge.subtract(Centre).scale(SparkPull));
            if (Index % 2 == 0) {
                mote(World, Colour, 0.75f, Centre, Edge.subtract(Centre).scale(MotePull));
            }
        }
    }

    private static void burst(ClientLevel World, Vec3 Base, float Width, float Height, int Colour, FragmentRarity Grade) {
        RandomSource Random = World.getRandom();
        Vec3 Centre = Base.add(0.0, Math.min(Height, 2.0f) * 0.5, 0.0);
        shockwave(World, Colour, 0.55f + 0.2f * Math.max(Width, Height), Centre);
        shockwave(World, Colour, 0.3f, Centre);
        BlockParticleOption Glass = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GLASS.defaultBlockState());
        ItemParticleOption Shell = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(StasisRegistry.podFor(Grade)));
        for (int Index = 0; Index < 30; Index++) {
            Vec3 Way = heading(Random);
            double Speed = 0.12 + Random.nextDouble() * 0.2;
            World.addParticle(Glass, Centre.x, Centre.y, Centre.z, Way.x * Speed, Math.abs(Way.y) * Speed + 0.12, Way.z * Speed);
        }
        for (int Index = 0; Index < 14; Index++) {
            Vec3 Way = heading(Random);
            double Speed = 0.1 + Random.nextDouble() * 0.15;
            World.addParticle(Shell, Centre.x, Centre.y, Centre.z, Way.x * Speed, Math.abs(Way.y) * Speed + 0.15, Way.z * Speed);
        }
        for (int Index = 0; Index < 28; Index++) {
            Vec3 Way = heading(Random);
            spark(World, Colour, 1.1f, Centre, Way.scale(0.18 + Random.nextDouble() * 0.25));
        }
        for (int Index = 0; Index < 18; Index++) {
            Vec3 Edge = inside(Random, Base, Width * 1.8f + 0.6f, Height * 1.4f + 0.4f);
            mote(World, Colour, 1.0f, Centre, Edge.subtract(Centre).scale(MotePull));
        }
    }

    private static void deny(ClientLevel World, Vec3 At, int Colour) {
        RandomSource Random = World.getRandom();
        for (int Index = 0; Index < 6; Index++) {
            spark(World, Colour, 0.6f, At, heading(Random).scale(0.08));
        }
        World.addParticle(ParticleTypes.SMOKE, At.x, At.y, At.z, 0.0, 0.02, 0.0);
    }

    private static void rattle(ClientLevel World, Vec3 At, int Colour) {
        RandomSource Random = World.getRandom();
        for (int Index = 0; Index < 3; Index++) {
            spark(World, Colour, 0.5f, At.add((Random.nextDouble() - 0.5) * 0.3, (Random.nextDouble() - 0.5) * 0.3, (Random.nextDouble() - 0.5) * 0.3),
                    heading(Random).scale(0.05));
        }
    }
}
