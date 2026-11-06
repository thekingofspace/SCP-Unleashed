package net.rai.scp_unleashed.guns;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.flamethrower.FlamerSounds;
import net.rai.scp_unleashed.guns.network.GunSoundPayload;
import net.rai.scp_unleashed.scp001scarlet.king.loot.ScarletRifleSounds;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

public final class GunSounds {
    public static final float PitchJitter = 0.06F;
    public static final float BurstJitter = 0.025F;
    public static final int TailEvery = 3;
    public static final int CueShot = 0;
    public static final int CueReloadStart = 1;
    public static final int CueReloadFinish = 2;
    public static final int CueCharge = 3;
    public static final int CueAbort = 4;
    public static final int CueBurnout = 5;
    public static final int CueOverheat = 6;
    public static final int CueCooled = 7;
    private static final Map<Level, List<Pending>> Queues = new WeakHashMap<>();

    private GunSounds() {
    }

    public record Layer(Supplier<SoundEvent> Sound, float Volume, float Pitch, int Delay, boolean Tail) {
    }

    private record Pending(@Nullable Player Except, double X, double Y, double Z, Layer Part, float Pitch, long Due) {
    }

    private static Layer layer(Supplier<SoundEvent> Sound, float Volume, float Pitch, int Delay) {
        return new Layer(Sound, Volume, Pitch, Delay, false);
    }

    private static Layer tail(Supplier<SoundEvent> Sound, float Volume, float Pitch, int Delay) {
        return new Layer(Sound, Volume, Pitch, Delay, true);
    }

    public static List<Layer> shot(GunKind Kind) {
        return switch (Kind) {
            case Pistol -> List.of(layer(GunsRegistry.ShotPunch, 0.51F, 1.45F, 0), layer(GunsRegistry.ShotWhump, 0.28F, 1.9F, 0), layer(GunsRegistry.ShotBoom, 0.15F, 2.0F, 0),
                    layer(GunsRegistry.ShotCrack, 0.08F, 1.5F, 0), layer(GunsRegistry.MechanismClose, 0.18F, 1.9F, 1), layer(GunsRegistry.ShotEcho, 0.18F, 1.6F, 1));
            case Revolver -> List.of(layer(GunsRegistry.ShotHeavy, 0.45F, 1.3F, 0), layer(GunsRegistry.ShotPunch, 0.5F, 1.1F, 0), layer(GunsRegistry.ShotBoom, 0.25F, 1.7F, 0),
                    layer(GunsRegistry.ShotCrack, 0.06F, 1.25F, 0), layer(GunsRegistry.ShotEcho, 0.25F, 1.15F, 1));
            case AssaultRifle -> List.of(layer(GunsRegistry.ShotPunch, 0.47F, 1.6F, 0), tail(GunsRegistry.ShotWhump, 0.16F, 2.0F, 0), layer(GunsRegistry.ShotCrack, 0.05F, 1.7F, 0),
                    layer(GunsRegistry.MechanismClose, 0.11F, 2.0F, 1), tail(GunsRegistry.ShotEcho, 0.07F, 1.8F, 1));
            case Shotgun -> List.of(layer(GunsRegistry.ShotHeavy, 0.45F, 1.0F, 0), layer(GunsRegistry.ShotPunch, 0.45F, 0.95F, 0), layer(GunsRegistry.ShotBoom, 0.32F, 1.45F, 0),
                    layer(GunsRegistry.ShotCrack, 0.05F, 1.15F, 0), layer(GunsRegistry.ShotEcho, 0.27F, 0.95F, 1), layer(GunsRegistry.MechanismOpen, 0.45F, 1.5F, 9),
                    layer(GunsRegistry.MechanismClose, 0.5F, 1.4F, 12));
            case SniperRifle -> List.of(layer(GunsRegistry.ShotHeavy, 0.47F, 0.95F, 0), layer(GunsRegistry.ShotBoom, 0.38F, 1.3F, 0), layer(GunsRegistry.ShotPunch, 0.43F, 0.85F, 0),
                    layer(GunsRegistry.ShotCrack, 0.06F, 1.0F, 0), layer(GunsRegistry.ShotThunder, 0.14F, 1.7F, 1), layer(GunsRegistry.ShotEcho, 0.33F, 0.8F, 2),
                    layer(GunsRegistry.MechanismClick, 0.4F, 1.3F, 14), layer(GunsRegistry.MechanismOpen, 0.45F, 1.4F, 16), layer(GunsRegistry.MechanismClose, 0.5F, 1.5F, 20));
            case ParticleDisruptor -> List.of(layer(GunsRegistry.DisruptorDischarge, 1.0F, 1.1F, 0), layer(GunsRegistry.ShotThunder, 0.6F, 1.4F, 0),
                    layer(GunsRegistry.ShotBoom, 0.5F, 0.8F, 0), layer(GunsRegistry.DisruptorFade, 0.5F, 1.8F, 4));
            case DoubleBarrel -> List.of(layer(GunsRegistry.ShotHeavy, 0.48F, 1.08F, 0), layer(GunsRegistry.ShotPunch, 0.42F, 1.0F, 0), layer(GunsRegistry.ShotBoom, 0.3F, 1.55F, 0),
                    layer(GunsRegistry.ShotCrack, 0.05F, 1.2F, 0), layer(GunsRegistry.ShotEcho, 0.26F, 1.0F, 1));
            case Spiker -> List.of(layer(GunsRegistry.Pneumatic, 0.45F, 1.6F, 0), tail(GunsRegistry.PneumaticVent, 0.3F, 1.7F, 0), tail(GunsRegistry.MechanismClose, 0.1F, 2.0F, 0));
            case Ultralaser -> List.of();
            case MicroHid -> List.of();
            case Flamethrower -> List.of();
            case ScarletRifle -> ScarletRifleSounds.shot();
            case Scp127 -> List.of(layer(GunsRegistry.ShotPunch, 0.42F, 1.78F, 0), tail(GunsRegistry.ShotWhump, 0.14F, 2.0F, 0), layer(GunsRegistry.ShotCrack, 0.05F, 1.9F, 0),
                    layer(GunsRegistry.MechanismClose, 0.12F, 2.0F, 1), tail(GunsRegistry.ShotEcho, 0.06F, 1.95F, 1));
            case CombatShotgun -> List.of(layer(GunsRegistry.ShotHeavy, 0.42F, 1.1F, 0), layer(GunsRegistry.ShotPunch, 0.45F, 1.05F, 0), layer(GunsRegistry.ShotBoom, 0.27F, 1.6F, 0),
                    layer(GunsRegistry.ShotCrack, 0.05F, 1.25F, 0), layer(GunsRegistry.ShotEcho, 0.24F, 1.05F, 1), layer(GunsRegistry.MechanismClose, 0.22F, 1.75F, 2));
            case MachineGun -> List.of(layer(GunsRegistry.ShotPunch, 0.5F, 1.35F, 0), tail(GunsRegistry.ShotHeavy, 0.2F, 1.5F, 0), layer(GunsRegistry.ShotCrack, 0.05F, 1.45F, 0),
                    layer(GunsRegistry.MechanismClose, 0.12F, 1.6F, 1), tail(GunsRegistry.ShotWhump, 0.16F, 1.7F, 0), tail(GunsRegistry.ShotEcho, 0.09F, 1.5F, 1));
        };
    }

    public static List<Layer> reloadStart(GunKind Kind) {
        return switch (Kind) {
            case Revolver -> List.of(layer(GunsRegistry.MechanismDoor, 0.4F, 1.9F, 0), layer(GunsRegistry.MechanismChain, 0.5F, 1.6F, 6));
            case DoubleBarrel -> List.of(layer(GunsRegistry.MechanismDoor, 0.45F, 1.55F, 0), layer(GunsRegistry.MechanismClick, 0.4F, 1.3F, 4), layer(GunsRegistry.MechanismClick, 0.35F, 1.5F, 5),
                    layer(GunsRegistry.MechanismLoad, 0.55F, 1.25F, 13), layer(GunsRegistry.MechanismLoad, 0.55F, 1.3F, 22));
            case Spiker -> List.of(layer(GunsRegistry.MechanismChain, 0.5F, 1.45F, 0), layer(GunsRegistry.MechanismOpen, 0.3F, 1.9F, 3));
            case Shotgun -> List.of(layer(GunsRegistry.MechanismLoad, 0.6F, 1.2F, 0), layer(GunsRegistry.MechanismLoad, 0.6F, 1.25F, 14),
                    layer(GunsRegistry.MechanismLoad, 0.6F, 1.2F, 28), layer(GunsRegistry.MechanismLoad, 0.6F, 1.3F, 42));
            case CombatShotgun -> List.of(layer(GunsRegistry.MechanismClick, 0.5F, 1.6F, 0), layer(GunsRegistry.MechanismOpen, 0.35F, 1.7F, 2),
                    layer(GunsRegistry.MechanismChain, 0.45F, 1.5F, 8), layer(GunsRegistry.MechanismClunk, 0.5F, 1.5F, 33), layer(GunsRegistry.MechanismClose, 0.45F, 1.7F, 35));
            case MachineGun -> List.of(layer(GunsRegistry.MechanismDoor, 0.45F, 1.3F, 0), layer(GunsRegistry.MechanismOpen, 0.4F, 1.2F, 2),
                    layer(GunsRegistry.MechanismClunk, 0.45F, 1.3F, 19), layer(GunsRegistry.MechanismChain, 0.5F, 1.2F, 21), layer(GunsRegistry.MechanismLoad, 0.5F, 1.05F, 54),
                    layer(GunsRegistry.MechanismClunk, 0.5F, 1.35F, 57), layer(GunsRegistry.MechanismChain, 0.55F, 1.45F, 66), layer(GunsRegistry.MechanismChain, 0.5F, 1.55F, 72),
                    layer(GunsRegistry.MechanismClose, 0.6F, 1.0F, 86), layer(GunsRegistry.MechanismDoor, 0.4F, 1.6F, 87));
            case Flamethrower -> FlamerSounds.refuelStart();
            case ScarletRifle -> ScarletRifleSounds.rechargeStart();
            default -> List.of(layer(GunsRegistry.MechanismChain, 0.6F, 1.3F, 0), layer(GunsRegistry.MechanismOpen, 0.35F, 1.8F, 2));
        };
    }

    public static List<Layer> reloadFinish(GunKind Kind) {
        return switch (Kind) {
            case Revolver -> List.of(layer(GunsRegistry.MechanismClose, 0.5F, 1.9F, 0), layer(GunsRegistry.MechanismClick, 0.5F, 1.7F, 2));
            case DoubleBarrel -> List.of(layer(GunsRegistry.MechanismClose, 0.6F, 1.15F, 0), layer(GunsRegistry.MechanismClick, 0.45F, 1.7F, 1));
            case Spiker -> List.of(layer(GunsRegistry.MechanismClose, 0.5F, 1.8F, 0), layer(GunsRegistry.MechanismClunk, 0.3F, 1.9F, 2));
            case Shotgun -> List.of(layer(GunsRegistry.MechanismClunk, 0.6F, 1.5F, 0), layer(GunsRegistry.MechanismClose, 0.45F, 1.6F, 3));
            case CombatShotgun -> List.of(layer(GunsRegistry.MechanismClose, 0.55F, 1.3F, 0), layer(GunsRegistry.MechanismClunk, 0.45F, 1.6F, 1),
                    layer(GunsRegistry.MechanismClick, 0.4F, 1.4F, 3));
            case MachineGun -> List.of(layer(GunsRegistry.MechanismClunk, 0.6F, 1.15F, 0), layer(GunsRegistry.MechanismClose, 0.45F, 1.5F, 3),
                    layer(GunsRegistry.MechanismClick, 0.45F, 1.3F, 5));
            case SniperRifle -> List.of(layer(GunsRegistry.MechanismClose, 0.55F, 1.5F, 0), layer(GunsRegistry.MechanismClick, 0.5F, 1.3F, 4),
                    layer(GunsRegistry.MechanismClunk, 0.55F, 1.4F, 6), layer(GunsRegistry.MechanismClunk, 0.55F, 1.55F, 10));
            case Flamethrower -> FlamerSounds.refuelFinish();
            case ScarletRifle -> ScarletRifleSounds.rechargeFinish();
            default -> List.of(layer(GunsRegistry.MechanismClose, 0.6F, 1.6F, 0), layer(GunsRegistry.MechanismClunk, 0.4F, 1.7F, 5), layer(GunsRegistry.MechanismClick, 0.4F, 1.4F, 6));
        };
    }

    public static List<Layer> dryFire() {
        return List.of(layer(GunsRegistry.MechanismClick, 0.7F, 2.0F, 0), layer(GunsRegistry.MechanismClose, 0.15F, 2.0F, 0));
    }

    public static List<Layer> dryFire(GunKind Kind) {
        return Kind == GunKind.Flamethrower ? FlamerSounds.sputter() : dryFire();
    }

    public static List<Layer> disruptorCharge() {
        return List.of(layer(GunsRegistry.DisruptorCharge, 1.0F, 1.35F, 0), layer(GunsRegistry.DisruptorPower, 0.7F, 1.6F, 0), layer(GunsRegistry.DisruptorWhine, 0.6F, 1.0F, 0));
    }

    public static List<Layer> disruptorBurnout() {
        return List.of(layer(GunsRegistry.DisruptorBurnout, 1.0F, 1.2F, 0), layer(GunsRegistry.Fizz, 0.8F, 0.7F, 2), layer(GunsRegistry.DisruptorFade, 0.6F, 0.9F, 3));
    }

    public static List<Layer> disruptorAbort() {
        return List.of(layer(GunsRegistry.DisruptorFade, 0.6F, 1.6F, 0));
    }

    public static List<Layer> overheat() {
        return List.of(layer(GunsRegistry.PneumaticVent, 0.55F, 0.75F, 0), layer(GunsRegistry.Fizz, 0.6F, 1.35F, 0), layer(GunsRegistry.MechanismClunk, 0.35F, 1.05F, 1),
                layer(GunsRegistry.Fizz, 0.35F, 0.9F, 12));
    }

    public static List<Layer> cooled() {
        return List.of(layer(GunsRegistry.MechanismClick, 0.45F, 1.25F, 0), layer(GunsRegistry.PneumaticVent, 0.2F, 1.4F, 0));
    }

    public static List<Layer> hitMarker(boolean Kill) {
        return List.of(layer(GunsRegistry.HitMarker, Kill ? 0.55F : 0.35F, Kill ? 1.25F : 1.6F, 0));
    }

    public static List<Layer> benchAssemble() {
        return List.of(layer(GunsRegistry.BenchAnvil, 0.5F, 1.3F, 0), layer(GunsRegistry.BenchSmithing, 0.8F, 1.0F, 3));
    }

    public static List<Layer> cue(int Cue, GunKind Kind) {
        return switch (Cue) {
            case CueShot -> shot(Kind);
            case CueReloadStart -> reloadStart(Kind);
            case CueReloadFinish -> reloadFinish(Kind);
            case CueCharge -> disruptorCharge();
            case CueAbort -> disruptorAbort();
            case CueBurnout -> disruptorBurnout();
            case CueOverheat -> overheat();
            case CueCooled -> cooled();
            default -> List.of();
        };
    }

    public static int tailEvery(GunKind Kind) {
        return Kind == GunKind.MachineGun ? 5 : TailEvery;
    }

    public static List<Layer> burst(GunKind Kind, int Streak) {
        return burst(shot(Kind), Streak, tailEvery(Kind));
    }

    public static List<Layer> burst(List<Layer> Layers, int Streak) {
        return burst(Layers, Streak, TailEvery);
    }

    private static List<Layer> burst(List<Layer> Layers, int Streak, int Every) {
        if (Streak <= 0 || Streak % Every == 0) {
            return Layers;
        }
        List<Layer> Core = new ArrayList<>();
        for (Layer Part : Layers) {
            if (!Part.Tail()) {
                Core.add(Part);
            }
        }
        return Core;
    }

    public static float jitter(int Streak) {
        return Streak > 0 ? BurstJitter : PitchJitter;
    }

    public static void playAround(ServerPlayer User, int Cue, GunKind Kind, float Scale) {
        play(User.level(), User, User.getX(), User.getEyeY(), User.getZ(), cue(Cue, Kind), Scale, PitchJitter);
        PacketDistributor.sendToPlayer(User, new GunSoundPayload(Cue, Kind.ordinal(), Scale));
    }

    public static void playNear(Entity User, int Cue, GunKind Kind, float Scale) {
        if (User instanceof ServerPlayer Gunner) {
            playAround(Gunner, Cue, Kind, Scale);
            return;
        }
        play(User.level(), null, User.getX(), User.getEyeY(), User.getZ(), cue(Cue, Kind), Scale, PitchJitter);
    }

    public static void play(Level World, @Nullable Player Except, double X, double Y, double Z, List<Layer> Layers, float Scale) {
        play(World, Except, X, Y, Z, Layers, Scale, PitchJitter);
    }

    public static void play(Level World, @Nullable Player Except, double X, double Y, double Z, List<Layer> Layers, float Scale, float Jitter) {
        RandomSource Random = World.getRandom();
        float Shift = 1.0F + (Random.nextFloat() * 2.0F - 1.0F) * Jitter;
        for (Layer Part : Layers) {
            float Pitch = Part.Pitch() * Shift;
            Layer Scaled = new Layer(Part.Sound(), Part.Volume() * Scale, Part.Pitch(), Part.Delay(), Part.Tail());
            if (Part.Delay() <= 0) {
                sound(World, Except, X, Y, Z, Scaled, Pitch);
            } else {
                Pending Entry = new Pending(Except, X, Y, Z, Scaled, Pitch, World.getGameTime() + Part.Delay());
                synchronized (Queues) {
                    Queues.computeIfAbsent(World, Key -> new ArrayList<>()).add(Entry);
                }
            }
        }
    }

    private static void sound(Level World, @Nullable Player Except, double X, double Y, double Z, Layer Part, float Pitch) {
        GunSoundMixer.emit(World, Except, X, Y, Z, Part.Sound().get(), SoundSource.PLAYERS, Part.Volume(), Pitch);
    }

    public static void tick(Level World) {
        List<Pending> Ready = new ArrayList<>();
        synchronized (Queues) {
            List<Pending> Waiting = Queues.get(World);
            if (Waiting != null && !Waiting.isEmpty()) {
                long Now = World.getGameTime();
                Iterator<Pending> Entries = Waiting.iterator();
                while (Entries.hasNext()) {
                    Pending Entry = Entries.next();
                    if (Now >= Entry.Due()) {
                        Ready.add(Entry);
                        Entries.remove();
                    } else if (Entry.Due() - Now > 400L) {
                        Entries.remove();
                    }
                }
            }
        }
        for (Pending Entry : Ready) {
            sound(World, Entry.Except(), Entry.X(), Entry.Y(), Entry.Z(), Entry.Part(), Entry.Pitch());
        }
        GunSoundMixer.flush(World);
    }

    public static void clear() {
        synchronized (Queues) {
            Queues.clear();
        }
        GunSoundMixer.clear();
    }
}
