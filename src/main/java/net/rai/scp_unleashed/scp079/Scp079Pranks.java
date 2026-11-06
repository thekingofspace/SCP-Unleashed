package net.rai.scp_unleashed.scp079;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.block.entity.BreakerBlockEntity;
import net.rai.scp_unleashed.block.entity.RedstoneReceiverBlockEntity;
import net.rai.scp_unleashed.computer.ComputerScp079;
import net.rai.scp_unleashed.speaker.SoundConsoleBlockEntity;
import net.rai.scp_unleashed.speaker.SoundMessages;
import net.rai.scp_unleashed.speaker.SpeakerBlockEntity;
import net.rai.scp_unleashed.wire.WireMessage;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class Scp079Pranks {
    public static final int MaxSpeakers = 3;
    public static final int PhantomMin = 40;
    public static final int PhantomMax = 140;
    public static final int FlickerTicks = 50;
    public static final int JamMin = 60;
    public static final int JamMax = 160;
    public static final float PrankVolume = 1.0f;
    public static final float SignalVolume = 1.3f;
    public static final List<ResourceLocation> PrankSounds = List.of(
            sound("speaker.facility.alarm"),
            sound("speaker.facility.klaxon"),
            sound("speaker.facility.pa_chime"),
            sound("speaker.facility.door_buzzer"),
            sound("speaker.facility.announcement_start"),
            sound("speaker.ambience.static"),
            sound("scp079_speaker_laugh"),
            sound("scp079_speaker_voice"),
            sound("scp079_speaker_static"),
            sound("scp079_speaker_voice"));
    public static final ResourceLocation SignalSound = sound("scp079_signal");

    private Scp079Pranks() {
    }

    private static ResourceLocation sound(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    @Nullable
    public static Result attempt(ServerLevel World, Scp079BlockEntity Computer, RandomSource Random, @Nullable Kind Forced) {
        Scp079Grid.Reach Reach = Scp079Grid.reach(World, Computer);
        List<RedstoneReceiverBlockEntity> Receivers = Reach.receivers();
        List<RedstoneReceiverBlockEntity> Lit = new ArrayList<>();
        for (RedstoneReceiverBlockEntity Receiver : Receivers) {
            if (Receiver.getSignal() > 0) {
                Lit.add(Receiver);
            }
        }
        List<SpeakerBlockEntity> Speakers = new ArrayList<>();
        for (SpeakerBlockEntity Speaker : Reach.speakers()) {
            if (!Speaker.isLooping()) {
                Speakers.add(Speaker);
            }
        }
        List<SoundConsoleBlockEntity> Consoles = new ArrayList<>();
        for (SoundConsoleBlockEntity Console : Reach.consoles()) {
            if (Console.isPowered() && (Console.isDriving() || !Console.isLoop())) {
                Consoles.add(Console);
            }
        }
        List<BreakerBlockEntity> Breakers = new ArrayList<>();
        for (BreakerBlockEntity Breaker : Reach.breakers(World)) {
            if (!Breaker.isTripped()) {
                Breakers.add(Breaker);
            }
        }
        List<Kind> Options = new ArrayList<>();
        for (Kind Candidate : Kind.values()) {
            boolean Possible = switch (Candidate) {
                case Phantom, Flicker -> !Receivers.isEmpty();
                case Jam -> !Lit.isEmpty();
                case Speaker -> !Speakers.isEmpty();
                case Console -> !Consoles.isEmpty();
                case Breaker -> !Breakers.isEmpty();
                case Chat -> ComputerScp079.canChat(Reach.Devices().values());
            };
            if (Possible && (Forced == null || Forced == Candidate)) {
                for (int Weight = 0; Weight < Candidate.Weight; Weight++) {
                    Options.add(Candidate);
                }
            }
        }
        if (Options.isEmpty()) {
            return null;
        }
        Kind Chosen = Options.get(Random.nextInt(Options.size()));
        BlockPos Self = Computer.getBlockPos();
        int Touched = 0;
        switch (Chosen) {
            case Phantom -> {
                RedstoneReceiverBlockEntity Target = Receivers.get(Random.nextInt(Receivers.size()));
                Scp079Signals.phantom(World, Target.getBlockPos(), Self, Mth.nextInt(Random, PhantomMin, PhantomMax));
                Touched = 1;
            }
            case Flicker -> {
                RedstoneReceiverBlockEntity Target = Receivers.get(Random.nextInt(Receivers.size()));
                Scp079Signals.flicker(World, Target.getBlockPos(), Self, FlickerTicks);
                Touched = 1;
            }
            case Jam -> {
                RedstoneReceiverBlockEntity Target = Lit.get(Random.nextInt(Lit.size()));
                List<BlockPos> Sources = new ArrayList<>(Reach.Devices().keySet());
                Sources.remove(Target.getBlockPos());
                Scp079Signals.jam(World, Target.getBlockPos(), Self, Sources, Mth.nextInt(Random, JamMin, JamMax));
                Touched = 1;
            }
            case Speaker -> {
                int Count = 1 + Random.nextInt(Math.min(MaxSpeakers, Speakers.size()));
                ResourceLocation Noise = PrankSounds.get(Random.nextInt(PrankSounds.size()));
                float Pitch = 0.7f + Random.nextFloat() * 0.6f;
                for (int Index = 0; Index < Count && !Speakers.isEmpty(); Index++) {
                    SpeakerBlockEntity Speaker = Speakers.remove(Random.nextInt(Speakers.size()));
                    Speaker.receiveMessage(new WireMessage(Speaker.getChannel(), SoundMessages.payload(Noise, PrankVolume, Pitch), Self));
                    Touched++;
                }
            }
            case Console -> {
                SoundConsoleBlockEntity Console = Consoles.get(Random.nextInt(Consoles.size()));
                if (Console.isDriving()) {
                    Console.stopLoop(World);
                } else {
                    Console.transmit(World);
                }
                Touched = 1;
            }
            case Breaker -> {
                BreakerBlockEntity Breaker = Breakers.get(Random.nextInt(Breakers.size()));
                Breaker.trip(World);
                Touched = 1;
            }
            case Chat -> Touched = ComputerScp079.post(World, Self, Reach.Devices().values(), Computer.isUpgraded(), Random);
        }
        return new Result(Chosen, Touched);
    }

    public static int broadcast(ServerLevel World, Scp079BlockEntity Computer, long Until) {
        Scp079Grid.Reach Reach = Scp079Grid.reach(World, Computer);
        BlockPos Self = Computer.getBlockPos();
        int Count = 0;
        RandomSource Random = World.getRandom();
        for (SpeakerBlockEntity Speaker : Reach.speakers()) {
            float Pitch = 0.85f + Random.nextFloat() * 0.3f;
            if (Speaker.isLooping()) {
                SoundMessages.play(World, Speaker.mouth(), new SoundMessages.Request(SignalSound, SignalVolume, Pitch, false));
            } else {
                Speaker.receiveMessage(new WireMessage(Speaker.getChannel(), SoundMessages.payload(SignalSound, SignalVolume, Pitch), Self));
            }
            Scp079Exposure.addSpeaker(World, Speaker.getBlockPos(), Speaker.mouth(), Until);
            Count++;
        }
        return Count;
    }

    public enum Kind implements StringRepresentable {
        Phantom("phantom", 3),
        Flicker("flicker", 3),
        Jam("jam", 2),
        Speaker("speaker", 4),
        Console("console", 1),
        Breaker("breaker", 1),
        Chat("chat", 3);

        private final String Name;
        private final int Weight;

        Kind(String Name, int Weight) {
            this.Name = Name;
            this.Weight = Weight;
        }

        @Nullable
        public static Kind byName(String Wanted) {
            for (Kind Candidate : values()) {
                if (Candidate.Name.equals(Wanted)) {
                    return Candidate;
                }
            }
            return null;
        }

        public int getWeight() {
            return Weight;
        }

        public Component getDescription() {
            return Component.translatable("dialogue.scp_unleashed.scp079.prank." + Name);
        }

        @Override
        public String getSerializedName() {
            return Name;
        }
    }

    public record Result(Kind Chosen, int Touched) {
    }
}
