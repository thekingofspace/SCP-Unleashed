package net.rai.scp_unleashed.scp002;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.Scp_unleashed;

public final class Scp002Sounds {
    private Scp002Sounds() {
    }

    private static void play(ServerLevel World, Vec3 At, Holder<SoundEvent> Sound, SoundSource Channel, float Volume, float Pitch) {
        World.playSound(null, At.x, At.y, At.z, Sound.value(), Channel, Volume, Pitch);
    }

    private static float spread(RandomSource Random, float Base, float Range) {
        return Base + (Random.nextFloat() - 0.5f) * Range;
    }

    private static Holder<SoundEvent> door(String Name, Holder<SoundEvent> Fallback) {
        return BuiltInRegistries.SOUND_EVENT.getHolder(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)).<Holder<SoundEvent>>map(Found -> Found).orElse(Fallback);
    }

    private static Vec3 doorCentre(BlockPos Door) {
        return Vec3.atBottomCenterOf(Door).add(0.0, 1.0, 0.0);
    }

    public static void doorOpen(ServerLevel World, BlockPos Door) {
        Vec3 At = doorCentre(Door);
        play(World, At, door("door_containment_unlatch", Scp002Registry.DoorOpen), SoundSource.BLOCKS, 0.9f, spread(World.random, 1.2f, 0.06f));
        play(World, At, door("door_gate_rattle", Scp002Registry.DoorGrind), SoundSource.BLOCKS, 0.45f, spread(World.random, 1.0f, 0.08f));
    }

    public static void doorHiss(ServerLevel World, BlockPos Door) {
        Vec3 At = doorCentre(Door);
        play(World, At, door("door_containment_hiss", Scp002Registry.DoorGrind), SoundSource.BLOCKS, 0.4f, spread(World.random, 1.0f, 0.1f));
    }

    public static void doorSwing(ServerLevel World, BlockPos Door) {
        Vec3 At = doorCentre(Door);
        play(World, At, door("door_containment_grind", Scp002Registry.DoorGrind), SoundSource.BLOCKS, 0.8f, spread(World.random, 1.2f, 0.08f));
        play(World, At, door("door_gate_engage", Scp002Registry.DoorOpen), SoundSource.BLOCKS, 0.7f, spread(World.random, 1.0f, 0.06f));
    }

    public static void doorSwingBack(ServerLevel World, BlockPos Door) {
        Vec3 At = doorCentre(Door);
        play(World, At, door("door_containment_grind", Scp002Registry.DoorGrind), SoundSource.BLOCKS, 0.65f, spread(World.random, 1.45f, 0.08f));
        play(World, At, door("door_gate_rattle", Scp002Registry.DoorGrind), SoundSource.BLOCKS, 0.25f, spread(World.random, 1.1f, 0.08f));
    }

    public static void doorClose(ServerLevel World, BlockPos Door) {
        Vec3 At = doorCentre(Door);
        play(World, At, door("door_containment_seal", Scp002Registry.DoorClose), SoundSource.BLOCKS, 0.55f, spread(World.random, 1.0f, 0.06f));
        play(World, At, door("door_containment_chunk", Scp002Registry.DoorClang), SoundSource.BLOCKS, 0.42f, spread(World.random, 1.0f, 0.06f));
        play(World, At, door("door_gate_clang", Scp002Registry.DoorClang), SoundSource.BLOCKS, 0.26f, spread(World.random, 1.0f, 0.06f));
        play(World, At, door("door_containment_thump", Scp002Registry.DoorClose), SoundSource.BLOCKS, 0.36f, 1.0f);
    }

    public static void doorLatch(ServerLevel World, BlockPos Door) {
        Vec3 At = doorCentre(Door);
        play(World, At, door("door_containment_unlatch", Scp002Registry.DoorOpen), SoundSource.BLOCKS, 0.6f, spread(World.random, 1.15f, 0.06f));
        play(World, At, door("door_gate_rattle", Scp002Registry.DoorGrind), SoundSource.BLOCKS, 0.35f, spread(World.random, 1.05f, 0.08f));
    }

    public static void doorDeny(ServerLevel World, BlockPos Door) {
        Vec3 At = doorCentre(Door);
        play(World, At, door("door_gate_clank", Scp002Registry.DoorDeny), SoundSource.BLOCKS, 0.7f, spread(World.random, 0.95f, 0.1f));
    }

    public static void heartbeat(ServerLevel World, Vec3 At, float Volume, float Pitch) {
        play(World, At, Scp002Registry.Heartbeat, SoundSource.HOSTILE, Volume, Pitch);
    }

    public static void tear(ServerLevel World, Vec3 At, RandomSource Random) {
        play(World, At, Scp002Registry.ConsumeTear, SoundSource.HOSTILE, 0.8f, spread(Random, 0.75f, 0.15f));
        play(World, At, Scp002Registry.FleshSquelch, SoundSource.HOSTILE, 0.55f, spread(Random, 0.6f, 0.12f));
    }

    public static void groan(ServerLevel World, Vec3 At, RandomSource Random) {
        play(World, At, Scp002Registry.RoomGroan, SoundSource.HOSTILE, 1.3f, spread(Random, 0.62f, 0.06f));
        play(World, At, Scp002Registry.Heartbeat, SoundSource.HOSTILE, 1.0f, 0.7f);
        play(World, At, Scp002Registry.FleshSquelch, SoundSource.HOSTILE, 0.8f, 0.5f);
    }

    public static void ambience(ServerLevel World, Vec3 At, boolean Awake, RandomSource Random) {
        if (Awake) {
            play(World, At, Scp002Registry.FleshSquelch, SoundSource.HOSTILE, spread(Random, 0.55f, 0.2f), spread(Random, 0.6f, 0.15f));
            if (Random.nextInt(3) == 0) {
                play(World, At, Scp002Registry.RoomGroan, SoundSource.HOSTILE, 0.7f, spread(Random, 0.58f, 0.08f));
            }
            return;
        }
        if (Random.nextBoolean()) {
            play(World, At, Scp002Registry.RoomBreath, SoundSource.AMBIENT, 0.35f, spread(Random, 0.62f, 0.08f));
        } else {
            play(World, At, Scp002Registry.FleshSquelch, SoundSource.AMBIENT, 0.22f, spread(Random, 0.55f, 0.1f));
        }
    }

    public static void impact(ServerLevel World, Vec3 At, RandomSource Random) {
        play(World, At, Scp002Registry.ImpactBoom, SoundSource.BLOCKS, 1.0f, spread(Random, 1.0f, 0.06f));
        play(World, At, Scp002Registry.ImpactCrack, SoundSource.BLOCKS, 1.0f, spread(Random, 1.0f, 0.06f));
        play(World, At, Scp002Registry.ImpactRumble, SoundSource.BLOCKS, 1.0f, spread(Random, 1.0f, 0.05f));
        play(World, At, Scp002Registry.ImpactSub, SoundSource.BLOCKS, 1.0f, 1.0f);
    }

    public static void debris(ServerLevel World, Vec3 At, RandomSource Random) {
        play(World, At, Scp002Registry.Debris, SoundSource.BLOCKS, spread(Random, 0.8f, 0.3f), spread(Random, 1.0f, 0.25f));
    }

    public static void sizzle(ServerLevel World, Vec3 At, RandomSource Random) {
        play(World, At, Scp002Registry.CraterSizzle, SoundSource.BLOCKS, spread(Random, 0.7f, 0.3f), spread(Random, 0.62f, 0.12f));
    }
}
