package net.rai.scp_unleashed.scp1730.rooms.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp1730.rooms.Scp1730RoomsRegistry;
import net.rai.scp_unleashed.scp1730.rooms.Scp1730SpecialRoom;
import net.rai.scp_unleashed.scp1730.rooms.Scp1730TelevisionBlockEntity;
import net.rai.scp_unleashed.scp1730.rooms.Scp1730TelevisionTune;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp1730TelevisionClient {
    public static final int ConfettiCap = 160;
    public static final int StreamerCap = 40;
    public static final int Burst = 36;
    private static final Map<BlockPos, Long> Played = new ConcurrentHashMap<>();

    private Scp1730TelevisionClient() {
    }

    public static Holder<SoundEvent> instrument(int Kind) {
        return switch (Kind) {
            case Scp1730TelevisionTune.Pling -> SoundEvents.NOTE_BLOCK_PLING;
            case Scp1730TelevisionTune.Bit -> SoundEvents.NOTE_BLOCK_BIT;
            case Scp1730TelevisionTune.Bell -> SoundEvents.NOTE_BLOCK_BELL;
            case Scp1730TelevisionTune.Xylophone -> SoundEvents.NOTE_BLOCK_XYLOPHONE;
            case Scp1730TelevisionTune.IronXylophone -> SoundEvents.NOTE_BLOCK_IRON_XYLOPHONE;
            case Scp1730TelevisionTune.Harp -> SoundEvents.NOTE_BLOCK_HARP;
            default -> SoundEvents.NOTE_BLOCK_BANJO;
        };
    }

    public static void tick(Scp1730TelevisionBlockEntity Entity) {
        Minecraft Client = Minecraft.getInstance();
        ClientLevel World = Client.level;
        if (World == null || Entity.getLevel() != World) {
            return;
        }
        BlockPos Pos = Entity.getBlockPos();
        if (Entity.phase() != Scp1730TelevisionBlockEntity.Showing) {
            Played.remove(Pos);
            return;
        }
        long Age = World.getGameTime() - Entity.phaseStart();
        if (Age < 0 || Age > Scp1730TelevisionBlockEntity.ShowTicks + 2) {
            return;
        }
        Long Before = Played.get(Pos);
        long From = Before == null ? Math.max(-1L, Age - 2L) : Before;
        Played.put(Pos, Age);
        Direction Front = Entity.facing();
        Vec3 Speaker = Vec3.atCenterOf(Pos).add(Front.getStepX() * 0.4, 0.0, Front.getStepZ() * 0.4);
        if (!Client.isPaused()) {
            List<Scp1730TelevisionTune.Note> Score = Scp1730TelevisionTune.score();
            for (Scp1730TelevisionTune.Note Note : Score) {
                if (Note.Tick() <= From) {
                    continue;
                }
                if (Note.Tick() > Age) {
                    break;
                }
                World.playLocalSound(Speaker.x, Speaker.y, Speaker.z, instrument(Note.Instrument()).value(), SoundSource.RECORDS, Note.Volume(),
                        Scp1730TelevisionTune.pitch(Note.Semitone()), false);
            }
        }
        if (Before == null && Age < 6) {
            for (int Index = 0; Index < Burst; Index++) {
                confetti(World, Speaker, World.random, 0.18, Index % 3 == 0);
            }
        }
        Scp1730SpecialRoom Room = Entity.room();
        if (Room == null || Age > Scp1730TelevisionBlockEntity.ShowTicks - Scp1730TelevisionBlockEntity.WindTicks) {
            return;
        }
        ParticleStatus Status = Client.options.particles().get();
        int Tries = Status == ParticleStatus.ALL ? 3 : Status == ParticleStatus.DECREASED ? 1 : 0;
        RandomSource Random = World.random;
        for (int Try = 0; Try < Tries; Try++) {
            boolean Ribbon = Random.nextInt(10) < 3;
            int Style = Ribbon ? Scp1730RoomParticles.Streamer : Scp1730RoomParticles.Confetti;
            if (Scp1730RoomParticles.alive(Style) >= (Ribbon ? StreamerCap : ConfettiCap)) {
                continue;
            }
            double X = Room.MinX() + 1.2 + Random.nextDouble() * (Room.MaxX() - Room.MinX() - 1.4);
            double Z = Room.MinZ() + 1.2 + Random.nextDouble() * (Room.MaxZ() - Room.MinZ() - 1.4);
            double Y = Room.Floor() + Room.Height() - 0.2 - Random.nextDouble() * 0.6;
            World.addParticle(Ribbon ? Scp1730RoomsRegistry.StreamerParticle.get() : Scp1730RoomsRegistry.ConfettiParticle.get(), X, Y, Z, (Random.nextDouble() - 0.5) * 0.02, -0.01,
                    (Random.nextDouble() - 0.5) * 0.02);
        }
    }

    static void confetti(ClientLevel World, Vec3 From, RandomSource Random, double Speed, boolean Ribbon) {
        if (Scp1730RoomParticles.alive(Ribbon ? Scp1730RoomParticles.Streamer : Scp1730RoomParticles.Confetti) >= (Ribbon ? StreamerCap : ConfettiCap)) {
            return;
        }
        double Dx = (Random.nextDouble() - 0.5) * Speed;
        double Dy = Random.nextDouble() * Speed * 0.9 + 0.05;
        double Dz = (Random.nextDouble() - 0.5) * Speed;
        World.addParticle(Ribbon ? Scp1730RoomsRegistry.StreamerParticle.get() : Scp1730RoomsRegistry.ConfettiParticle.get(), From.x, From.y + 0.3, From.z, Dx, Dy, Dz);
    }

    public static void reset() {
        Played.clear();
    }
}
