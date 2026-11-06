package net.rai.scp_unleashed.voice.gametest;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.config.ScpClientConfig;
import net.rai.scp_unleashed.integration.voicechat.VoiceGate;
import net.rai.scp_unleashed.radio.gametest.RadioTestPlayer;
import net.rai.scp_unleashed.voice.VoiceArchive;
import net.rai.scp_unleashed.voice.VoiceClip;
import net.rai.scp_unleashed.voice.VoiceEvents;
import net.rai.scp_unleashed.voice.VoiceFiles;
import net.rai.scp_unleashed.voice.VoicePlayback;
import net.rai.scp_unleashed.voice.VoicePolicy;
import net.rai.scp_unleashed.voice.VoiceProfile;
import net.rai.scp_unleashed.voice.VoiceProfiles;
import net.rai.scp_unleashed.voice.VoiceRecorder;
import net.rai.scp_unleashed.voice.VoiceStore;
import net.rai.scp_unleashed.voice.network.VoicePreferencePayload;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class VoiceArchiveGameTests {
    public static final String Arena = "radio_arena";
    public static final long Day = 86_400_000L;

    private VoiceArchiveGameTests() {
    }

    public static final class FakePlayback implements VoicePlayback {
        public final List<List<byte[]>> Played = new ArrayList<>();
        public final List<VoiceProfile> Profiles = new ArrayList<>();
        public final List<Float> Scales = new ArrayList<>();

        @Override
        public boolean play(ServerLevel World, Entity Source, List<byte[]> Frames, VoiceProfile Profile, float PitchScale, long Seed) {
            Played.add(Frames);
            Profiles.add(Profile);
            Scales.add(PitchScale);
            return true;
        }
    }

    public static List<byte[]> frames(int Count, int Mark) {
        List<byte[]> Made = new ArrayList<>();
        for (int Index = 0; Index < Count; Index++) {
            byte[] Frame = new byte[30 + Index % 7];
            Frame[0] = (byte) Mark;
            Frame[1] = (byte) Index;
            Frame[2] = (byte) (Index >> 8);
            Made.add(Frame);
        }
        return Made;
    }

    public static VoiceRecorder.Finished clip(String Anomaly, UUID Speaker, String Name, long Recorded, int Count, int Mark) {
        return new VoiceRecorder.Finished(Anomaly, Speaker, Name, null, Recorded, frames(Count, Mark));
    }

    private static boolean same(List<byte[]> First, List<byte[]> Second) {
        if (First == null || Second == null || First.size() != Second.size()) {
            return false;
        }
        for (int Index = 0; Index < First.size(); Index++) {
            if (!Arrays.equals(First.get(Index), Second.get(Index))) {
                return false;
            }
        }
        return true;
    }

    private static int run(MinecraftServer Server, CommandSourceStack Source, String Command) {
        try {
            return Server.getCommands().getDispatcher().execute(Command, Source);
        } catch (CommandSyntaxException Problem) {
            return -1;
        }
    }

    private static CommandSourceStack console(MinecraftServer Server) {
        return Server.createCommandSourceStack().withSuppressedOutput().withPermission(4);
    }

    private static RadioTestPlayer player(GameTestHelper Helper, String Name) {
        ServerLevel World = Helper.getLevel();
        RadioTestPlayer User = new RadioTestPlayer(World, Name);
        Vec3 Where = Helper.absoluteVec(new Vec3(8.5, 1.0, 8.5));
        User.moveTo(Where.x, Where.y, Where.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        User.setGameMode(GameType.SURVIVAL);
        return User;
    }

    private static void dismiss(GameTestHelper Helper, RadioTestPlayer User) {
        if (!User.isRemoved()) {
            Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
        }
    }

    private static VoiceArchive.Heard heard(UUID Speaker, ServerLevel World, boolean Spectator, boolean Disabled, boolean Whispering, boolean Grouped) {
        return new VoiceArchive.Heard(Speaker, "voice_test", World.dimension(), 0.0, 64.0, 0.0, Spectator, Disabled, Whispering, Grouped);
    }

    private static short[] sine(double Hertz, double Seconds, double Amplitude) {
        short[] Made = new short[(int) (Seconds * VoiceProfiles.SampleRate)];
        for (int Index = 0; Index < Made.length; Index++) {
            Made[Index] = (short) Math.round(Amplitude * Math.sin(2.0 * Math.PI * Hertz * Index / VoiceProfiles.SampleRate));
        }
        return Made;
    }

    private static double pitch(short[] Pcm) {
        int Crossings = 0;
        for (int Index = 1; Index < Pcm.length; Index++) {
            if (Pcm[Index - 1] < 0 && Pcm[Index] >= 0) {
                Crossings++;
            }
        }
        return Crossings / (Pcm.length / (double) VoiceProfiles.SampleRate);
    }

    private static double power(short[] Pcm, double Hertz) {
        double Coefficient = 2.0 * Math.cos(2.0 * Math.PI * Hertz / VoiceProfiles.SampleRate);
        double Previous = 0.0;
        double Older = 0.0;
        for (short Sample : Pcm) {
            double Next = Sample + Coefficient * Previous - Older;
            Older = Previous;
            Previous = Next;
        }
        return (Previous * Previous + Older * Older - Coefficient * Previous * Older) / Pcm.length;
    }

    private static double bandRatio(short[] Pcm) {
        double Low = power(Pcm, 350.0) + power(Pcm, 450.0) + power(Pcm, 550.0);
        double High = power(Pcm, 5000.0) + power(Pcm, 6500.0) + power(Pcm, 8000.0);
        return Low / Math.max(1e-9, High);
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "voice_profiles")
    public static void profiles(GameTestHelper Helper) {
        short[] Tone = sine(1000.0, 4.0, 12000.0);
        double Lure = pitch(VoiceProfiles.scp939(Tone, 5L, 1.0f));
        Helper.assertTrue(Lure > 930.0 && Lure < 990.0, "The SCP-939 profile should drop a 1 kHz tone to about 960 Hz, got " + Lure);
        double Young = pitch(VoiceProfiles.scp939(Tone, 5L, 1.25f));
        Helper.assertTrue(Young > 1160.0 && Young < 1240.0, "A young SCP-939 should sound higher, got " + Young);
        VoiceProfiles.Shaped Shifted = VoiceProfiles.scp001Day(Tone, 9L, 1.0f);
        double Dropped = pitch(Shifted.Pcm());
        Helper.assertTrue(Dropped > 945.0 && Dropped < 975.0, "The SCP-001 profile should drop a 1 kHz tone by about 4 percent, got " + Dropped);
        Random Noise = new Random(1L);
        short[] Hiss = new short[2 * VoiceProfiles.SampleRate];
        for (int Index = 0; Index < Hiss.length; Index++) {
            Hiss[Index] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(Noise.nextGaussian() * 6000.0)));
        }
        double Before = bandRatio(Hiss);
        double After = bandRatio(VoiceProfiles.scp001Day(Hiss, 3L, 1.0f).Pcm());
        Helper.assertTrue(After > Before * 1.8, "The SCP-001 profile should lift 300 to 600 Hz over the highs: before " + Before + ", after " + After);
        VoiceProfiles.Shaped Lengthy = VoiceProfiles.scp001Day(sine(300.0, 10.0, 9000.0), 11L, 1.0f);
        Helper.assertTrue(Lengthy.Stutters() >= 1 && Lengthy.Stutters() <= 4, "Ten seconds should stutter one to four times, got " + Lengthy.Stutters());
        int Base = (int) (10.0 * VoiceProfiles.SampleRate / VoiceProfiles.DayPitch);
        int Extra = Lengthy.Pcm().length - Base;
        Helper.assertTrue(Extra >= Lengthy.Stutters() * 40 * 48 - 2000 && Extra <= Lengthy.Stutters() * 80 * 48 + 2000, "Each stutter should repeat 40 to 80 ms, extra length " + Extra);
        Helper.assertTrue(VoiceProfiles.scp001Day(sine(300.0, 0.5, 9000.0), 11L, 1.0f).Stutters() == 0, "Half a second should never stutter");
        int Stuttered = 0;
        for (long Seed = 1L; Seed <= 20L; Seed++) {
            if (VoiceProfiles.scp001Day(sine(300.0, 3.0, 9000.0), Seed, 1.0f).Stutters() > 0) {
                Stuttered++;
            }
        }
        Helper.assertTrue(Stuttered >= 5 && Stuttered <= 20, "Three second clips should stutter now and then, got " + Stuttered + " of 20");
        short[] Click = new short[VoiceProfiles.SampleRate];
        Click[VoiceProfiles.SampleRate / 4] = 20000;
        short[] Dry = VoiceProfiles.scp001Day(Click, 4L, 1.0f).Pcm();
        double Total = 0.0;
        double Tail = 0.0;
        int Peak = 0;
        for (int Index = 0; Index < Dry.length; Index++) {
            if (Math.abs(Dry[Index]) > Math.abs(Dry[Peak])) {
                Peak = Index;
            }
        }
        for (int Index = 0; Index < Dry.length; Index++) {
            double Energy = Dry[Index] * (double) Dry[Index];
            Total += Energy;
            if (Index > Peak + VoiceProfiles.SampleRate / 20) {
                Tail += Energy;
            }
        }
        Helper.assertTrue(Tail < Total * 0.01, "The SCP-001 profile should stay dry with no reverb tail: " + Tail / Math.max(1.0, Total));
        Helper.assertTrue(VoiceProfiles.forAnomaly(VoicePolicy.Scp001Day) == VoiceProfiles.Day && VoiceProfiles.forAnomaly(VoicePolicy.Scp939) == VoiceProfiles.Lure,
                "Profiles are not registered next to their anomaly ids");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "voice_recorder")
    public static void recorder(GameTestHelper Helper) {
        VoiceRecorder Recorder = new VoiceRecorder();
        UUID Speaker = UUID.randomUUID();
        List<byte[]> Some = frames(400, 1);
        for (int Index = 0; Index < 30; Index++) {
            Recorder.offer("voicetest_rec", Speaker, "rec", null, Some.get(Index), Index * 20L);
        }
        Recorder.sweep(10_000L);
        Helper.assertTrue(Recorder.drain().isEmpty(), "A clip shorter than a second was kept");
        for (int Index = 0; Index < 200; Index++) {
            Recorder.offer("voicetest_rec", Speaker, "rec", null, Some.get(Index), 20_000L + Index * 20L);
        }
        Recorder.sweep(40_000L);
        List<VoiceRecorder.Finished> Split = Recorder.drain();
        Helper.assertTrue(Split.size() == 2 && Split.get(0).Frames().size() == VoiceRecorder.MaxFrames && Split.get(1).Frames().size() == 50,
                "Four seconds of speech should split into a three second and a one second clip");
        for (int Index = 0; Index < 60; Index++) {
            Recorder.offer("voicetest_rec", Speaker, "rec", null, Some.get(Index), 50_000L + Index * 20L);
        }
        for (int Index = 0; Index < 60; Index++) {
            Recorder.offer("voicetest_rec", Speaker, "rec", null, Some.get(Index), 52_000L + Index * 20L);
        }
        Recorder.sweep(60_000L);
        Helper.assertTrue(Recorder.drain().size() == 2, "A pause should split speech into two clips");
        for (int Index = 0; Index < VoiceRecorder.PendingCap + 10; Index++) {
            Recorder.offer("voicetest_rec", UUID.randomUUID(), "rec", null, Some.get(0), 70_000L);
        }
        Helper.assertTrue(Recorder.pending() <= VoiceRecorder.PendingCap, "Open recordings are not capped");
        Helper.assertFalse(Recorder.offer("voicetest_rec", Speaker, "rec", null, new byte[VoiceRecorder.MaxFrameBytes + 1], 70_000L), "An oversized frame was taken");
        Recorder.clear();
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "voice_policyGate")
    public static void policyGate(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        String Id = "voicetest_gate";
        FakePlayback Fake = new FakePlayback();
        VoicePlayback Before = VoiceArchive.playback();
        RadioTestPlayer Talker = player(Helper, "voice_gate_talk");
        ArmorStand Source = EntityType.ARMOR_STAND.create(World);
        Vec3 Spot = Helper.absoluteVec(new Vec3(4.5, 1.0, 4.5));
        Source.moveTo(Spot.x, Spot.y, Spot.z);
        UUID Other = UUID.randomUUID();
        try {
            VoiceArchive.setPlayback(Fake);
            VoicePolicy.forceMaster(true);
            VoiceArchive.clear(Id);
            VoiceArchive.clear(VoicePolicy.Scp001Day);
            VoiceClip Mine = VoiceArchive.accept(clip(Id, Talker.getUUID(), "talker", System.currentTimeMillis() - 5000L, 60, 7));
            VoiceArchive.accept(clip(Id, Other, "other", System.currentTimeMillis() - 4000L, 60, 9));
            Helper.assertTrue(Mine != null, "A clip could not be stored");
            VoicePolicy.forceMaster(false);
            Helper.assertFalse(VoiceArchive.capture(Id, Talker), "Capture started with the master switch off");
            Helper.assertFalse(VoiceArchive.replay(Id, Source, Optional.empty()), "A clip replayed with the master switch off");
            Helper.assertTrue(VoiceArchive.hear(heard(Talker.getUUID(), World, false, false, false, false), new byte[40], System.currentTimeMillis()) == 0,
                    "Voice was taken with the master switch off");
            Helper.assertTrue(Fake.Played.isEmpty(), "The backend played with the master switch off");
            VoicePolicy.forceMaster(true);
            Helper.assertTrue(VoiceArchive.capture(Id, Talker), "Capture did not start with the master switch on");
            Helper.assertTrue(VoiceArchive.isCapturing(Id, Talker.getUUID()), "The capture window is not open");
            Helper.assertTrue(VoiceArchive.hear(heard(Talker.getUUID(), World, false, false, false, false), new byte[40], System.currentTimeMillis()) == 1,
                    "A captured player's voice was not taken");
            UUID Stranger = UUID.randomUUID();
            Helper.assertTrue(VoiceArchive.hear(heard(Stranger, World, false, false, false, false), new byte[40], System.currentTimeMillis()) == 0,
                    "A player outside any capture window was taken");
            Helper.assertTrue(VoiceArchive.hear(heard(Talker.getUUID(), World, true, false, false, false), new byte[40], System.currentTimeMillis()) == 0, "A spectator was taken");
            Helper.assertTrue(VoiceArchive.hear(heard(Talker.getUUID(), World, false, true, false, false), new byte[40], System.currentTimeMillis()) == 0,
                    "A player with voice chat disabled was taken");
            Helper.assertTrue(VoiceArchive.hear(heard(Talker.getUUID(), World, false, false, true, false), new byte[40], System.currentTimeMillis()) == 0, "A whisper was taken");
            Helper.assertTrue(VoiceArchive.hear(heard(Talker.getUUID(), World, false, false, false, true), new byte[40], System.currentTimeMillis()) == 0,
                    "A voice chat group member was taken");
            VoiceGate.mark("voice_test", Talker.getUUID(), true);
            Helper.assertTrue(VoiceArchive.hear(heard(Talker.getUUID(), World, false, false, false, false), new byte[40], System.currentTimeMillis()) == 0, "A silenced player was taken");
            VoiceGate.mark("voice_test", Talker.getUUID(), false);
            Talker.setGameMode(GameType.SPECTATOR);
            Helper.assertFalse(VoiceArchive.capture(Id, Talker), "Capture started on a spectator");
            Talker.setGameMode(GameType.SURVIVAL);
            Helper.assertTrue(VoiceArchive.replay(Id, Source, Optional.of(Other)) && Fake.Played.size() == 1, "A stored clip did not replay");
            Helper.assertTrue(Fake.Played.get(0).get(0)[0] == (byte) 9, "The preferred speaker's clip was not chosen");
            Helper.assertTrue(VoiceArchive.replay(Id, Source, Optional.of(UUID.randomUUID())), "An unknown preferred speaker should fall back to any clip");
            VoicePolicy.forceSwitch(VoicePolicy.Scp939, false);
            Helper.assertFalse(VoicePolicy.allows(VoicePolicy.Scp939), "The SCP-939 switch did not turn its recording off");
            Helper.assertFalse(VoiceArchive.capture(VoicePolicy.Scp939, Talker), "SCP-939 capture started with its switch off");
            Helper.assertTrue(VoicePolicy.allows(VoicePolicy.Scp001Day) && VoiceArchive.capture(VoicePolicy.Scp001Day, Talker), "SCP-001 capture should not depend on the SCP-939 switch");
            VoiceArchive.accept(clip(VoicePolicy.Scp001Day, Talker.getUUID(), "talker", System.currentTimeMillis(), 60, 3));
            Helper.assertTrue(VoiceArchive.replay(VoicePolicy.Scp001Day, Source, Optional.of(Talker.getUUID())), "The SCP-001 store did not replay");
            Helper.assertTrue(Fake.Profiles.get(Fake.Profiles.size() - 1) == VoiceProfiles.Day, "The SCP-001 replay did not use its own distortion profile");
            VoicePolicy.forceSwitch(VoicePolicy.Scp001Day, false);
            Helper.assertFalse(VoiceArchive.replay(VoicePolicy.Scp001Day, Source, Optional.empty()), "SCP-001 replayed with its switch off");
            VoicePolicy.forceSwitch(VoicePolicy.Scp001Day, null);
            VoicePolicy.forceMaster(false);
            Helper.assertTrue(VoicePolicy.allows(VoicePolicy.Scp001Day) == false, "A per anomaly switch worked without the master switch");
        } finally {
            VoiceGate.mark("voice_test", Talker.getUUID(), false);
            VoicePolicy.resetForces();
            VoiceArchive.clear(Id);
            VoiceArchive.clear(VoicePolicy.Scp001Day);
            VoiceArchive.stopAll(Talker.getUUID());
            VoiceArchive.setPlayback(Before);
            dismiss(Helper, Talker);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "voice_persistence")
    public static void persistence(GameTestHelper Helper) {
        String Id = "voicetest_persist";
        UUID Alpha = UUID.randomUUID();
        UUID Beta = UUID.randomUUID();
        try {
            VoicePolicy.forceMaster(true);
            VoiceArchive.clear(Id);
            VoiceArchive.flush();
            long Now = System.currentTimeMillis();
            VoiceRecorder.Finished First = clip(Id, Alpha, "alpha", Now - 2000L, 60, 1);
            VoiceRecorder.Finished Second = clip(Id, Beta, "beta", Now - 1000L, 90, 2);
            VoiceClip A = VoiceArchive.accept(First);
            VoiceClip B = VoiceArchive.accept(Second);
            Helper.assertTrue(VoiceArchive.flush(), "Voice writes did not finish");
            Path Folder = VoiceArchive.root().resolve(Id);
            Helper.assertTrue(Files.isRegularFile(Folder.resolve(VoiceFiles.IndexName)), "The index was not written to " + Folder);
            Helper.assertTrue(Files.isRegularFile(VoiceFiles.clipPath(Folder, A.Id())) && Files.isRegularFile(VoiceFiles.clipPath(Folder, B.Id())), "Clip files were not written");
            VoiceArchive.reload();
            VoiceStore Again = VoiceArchive.store(Id);
            Helper.assertTrue(Again.size() == 2, "Clips did not survive a reload, found " + Again.size());
            Helper.assertTrue(same(Again.frames(A.Id()), First.Frames()) && same(Again.frames(B.Id()), Second.Frames()), "Reloaded frames differ from the recorded ones");
            VoiceClip Read = Again.clips().get(0);
            Helper.assertTrue(Read.SpeakerName().equals("alpha") && Read.Speaker().equals(Alpha) && Read.Recorded() == First.Recorded() && Read.Frames() == 60,
                    "Reloaded clip metadata differs");
            Files.deleteIfExists(Folder.resolve(VoiceFiles.IndexName));
            VoiceArchive.reload();
            Helper.assertTrue(VoiceArchive.store(Id).size() == 2, "Clips were not recovered without the index");
            VoiceArchive.flush();
            Helper.assertTrue(Files.isRegularFile(Folder.resolve(VoiceFiles.IndexName)), "The index was not rebuilt");
            Files.write(VoiceFiles.clipPath(Folder, B.Id()), new byte[]{1, 2, 3});
            VoiceArchive.reload();
            Helper.assertTrue(VoiceArchive.store(Id).frames(B.Id()) == null && VoiceArchive.store(Id).size() == 1, "A damaged clip was not dropped");
            VoiceArchive.clear(Id);
            VoiceArchive.flush();
            Helper.assertFalse(Files.exists(Folder), "Clearing did not remove the folder");
        } catch (IOException Problem) {
            throw new IllegalStateException(Problem);
        } finally {
            VoicePolicy.resetForces();
            VoiceArchive.clear(Id);
            VoiceArchive.flush();
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "voice_caps")
    public static void caps(GameTestHelper Helper) {
        String Id = "voicetest_caps";
        String Aged = "voicetest_aged";
        UUID Alpha = UUID.randomUUID();
        try {
            VoicePolicy.forceMaster(true);
            VoicePolicy.forceCaps(new int[]{5, 2, 14});
            VoiceArchive.clear(Id);
            VoiceArchive.clear(Aged);
            long Now = System.currentTimeMillis();
            for (int Index = 0; Index < 3; Index++) {
                VoiceArchive.accept(clip(Id, Alpha, "alpha", Now - 100_000L + Index, 60, Index));
            }
            List<VoiceClip> Kept = VoiceArchive.store(Id).clips();
            Helper.assertTrue(Kept.size() == 2 && Kept.stream().noneMatch(Each -> Each.Recorded() == Now - 100_000L), "The per speaker cap did not drop the oldest clip");
            for (int Index = 0; Index < 5; Index++) {
                VoiceArchive.accept(clip(Id, UUID.randomUUID(), "other", Now - 50_000L + Index, 60, 20 + Index));
            }
            Helper.assertTrue(VoiceArchive.store(Id).size() == 5 && !VoiceArchive.store(Id).holdsSpeaker(Alpha), "The per anomaly cap did not drop the oldest clips");
            VoiceArchive.accept(clip(Aged, Alpha, "alpha", Now - 20 * Day, 60, 1));
            VoiceArchive.accept(clip(Aged, Alpha, "alpha", Now - Day, 60, 2));
            Helper.assertTrue(VoiceArchive.prune(Now) >= 1 && VoiceArchive.store(Aged).size() == 1, "The daily prune did not drop a 20 day old clip");
            VoiceArchive.accept(clip(Aged, UUID.randomUUID(), "old", Now - 30 * Day, 60, 3));
            VoiceArchive.reload();
            Helper.assertTrue(VoiceArchive.store(Aged).size() == 1, "Loading did not prune an old clip");
            VoicePolicy.forceMaster(false);
            VoiceArchive.accept(clip(Aged, UUID.randomUUID(), "old", Now - 40 * Day, 60, 4));
            VoiceArchive.reload();
            Helper.assertTrue(VoiceArchive.store(Aged).size() == 2, "Files were pruned while the master switch was off");
        } finally {
            VoicePolicy.resetForces();
            VoiceArchive.clear(Id);
            VoiceArchive.clear(Aged);
            VoiceArchive.flush();
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "voice_clear")
    public static void clear(GameTestHelper Helper) {
        MinecraftServer Server = Helper.getLevel().getServer();
        RadioTestPlayer Named = player(Helper, "voice_clear_p");
        Named.enlist(true);
        UUID Other = UUID.randomUUID();
        try {
            VoicePolicy.forceMaster(true);
            long Now = System.currentTimeMillis();
            VoiceArchive.accept(clip("voicetest_one", Named.getUUID(), "voice_clear_p", Now - 3000L, 60, 1));
            VoiceArchive.accept(clip("voicetest_one", Named.getUUID(), "voice_clear_p", Now - 2000L, 60, 2));
            VoiceArchive.accept(clip("voicetest_one", Other, "other", Now - 1000L, 60, 3));
            VoiceArchive.accept(clip("voicetest_two", Named.getUUID(), "voice_clear_p", Now - 3000L, 60, 4));
            VoiceArchive.accept(clip("voicetest_two", Other, "other", Now - 2000L, 60, 5));
            VoiceArchive.accept(clip("voicetest_two", Other, "other", Now - 1000L, 60, 6));
            VoicePolicy.forceMaster(false);
            Helper.assertTrue(run(Server, console(Server), "scp_unleashed voice clear player @a[name=voice_clear_p]") == 3, "Clearing a player removed the wrong number of clips");
            Helper.assertTrue(VoiceArchive.store("voicetest_one").size() == 1 && VoiceArchive.store("voicetest_two").size() == 2, "Clearing a player touched other speakers");
            Helper.assertTrue(run(Server, console(Server), "scp_unleashed voice clear voicetest_one") == 1, "Clearing one anomaly removed the wrong number of clips");
            VoiceArchive.flush();
            Helper.assertFalse(Files.exists(VoiceArchive.root().resolve("voicetest_one")), "Clearing one anomaly left its folder");
            Helper.assertTrue(VoiceArchive.store("voicetest_two").size() == 2, "Clearing one anomaly touched another");
            Helper.assertTrue(run(Server, console(Server), "scp_unleashed voice clear voicetest_missing") == 0, "Clearing an unknown anomaly did not fail");
            Helper.assertTrue(run(Server, console(Server), "scp_unleashed voice status") == 2, "Status counted the wrong number of clips");
            Helper.assertTrue(run(Server, console(Server), "scp_unleashed voice clear") >= 2, "Clearing everything removed too few clips");
            VoiceArchive.flush();
            Helper.assertTrue(VoiceArchive.anomalies().stream().allMatch(Anomaly -> VoiceArchive.store(Anomaly).size() == 0), "Clearing everything left clips in memory");
            Helper.assertFalse(Files.exists(VoiceArchive.root()), "Clearing everything left files on disk");
            Helper.assertTrue(run(Server, Named.createCommandSourceStack(), "scp_unleashed voice clear") == -1, "A player without permission could clear every clip");
        } finally {
            VoicePolicy.resetForces();
            VoiceArchive.clear("voicetest_one");
            VoiceArchive.clear("voicetest_two");
            VoiceArchive.flush();
            Named.enlist(false);
            dismiss(Helper, Named);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "voice_forgetme")
    public static void forgetMe(GameTestHelper Helper) {
        MinecraftServer Server = Helper.getLevel().getServer();
        RadioTestPlayer Me = player(Helper, "voice_forget_me");
        UUID Other = UUID.randomUUID();
        try {
            VoicePolicy.forceMaster(true);
            long Now = System.currentTimeMillis();
            VoiceArchive.accept(clip("voicetest_fa", Me.getUUID(), "voice_forget_me", Now - 3000L, 60, 1));
            VoiceArchive.accept(clip("voicetest_fb", Me.getUUID(), "voice_forget_me", Now - 2000L, 60, 2));
            VoiceArchive.accept(clip("voicetest_fb", Other, "other", Now - 1000L, 60, 3));
            VoicePolicy.forceMaster(false);
            Helper.assertTrue(run(Server, Me.createCommandSourceStack(), "scp_unleashed voice forgetme") == 2, "forgetme did not delete both of the player's clips");
            Helper.assertFalse(VoiceArchive.store("voicetest_fa").holdsSpeaker(Me.getUUID()) || VoiceArchive.store("voicetest_fb").holdsSpeaker(Me.getUUID()),
                    "The player's clips survived forgetme");
            Helper.assertTrue(VoiceArchive.store("voicetest_fb").holdsSpeaker(Other), "forgetme deleted another player's clip");
            VoiceArchive.reload();
            Helper.assertFalse(VoiceArchive.store("voicetest_fb").holdsSpeaker(Me.getUUID()), "The player's clips came back from disk");
        } finally {
            VoicePolicy.resetForces();
            VoiceArchive.clear("voicetest_fa");
            VoiceArchive.clear("voicetest_fb");
            VoiceArchive.flush();
            dismiss(Helper, Me);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "voice_joinNotice")
    public static void joinNotice(GameTestHelper Helper) {
        RadioTestPlayer Quiet = player(Helper, "voice_notice_off");
        RadioTestPlayer Told = player(Helper, "voice_notice_on");
        try {
            VoicePolicy.forceMaster(false);
            VoiceEvents.loggedIn(new PlayerEvent.PlayerLoggedInEvent(Quiet));
            Helper.assertFalse(Quiet.heard(VoiceEvents.NoticeKey), "The recording notice was shown with the switch off");
            VoicePolicy.forceMaster(true);
            VoiceEvents.loggedIn(new PlayerEvent.PlayerLoggedInEvent(Told));
            Helper.assertTrue(Told.heard(VoiceEvents.NoticeKey), "The recording notice was not shown with the switch on");
            Helper.assertTrue(Told.heard("voice forgetme"), "The notice does not say how to remove clips");
        } finally {
            VoicePolicy.resetForces();
            dismiss(Helper, Quiet);
            dismiss(Helper, Told);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "voice_optOut")
    public static void optOut(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        String Id = "voicetest_optout";
        FakePlayback Fake = new FakePlayback();
        VoicePlayback Before = VoiceArchive.playback();
        RadioTestPlayer Shy = player(Helper, "voice_opt_out");
        UUID Other = UUID.randomUUID();
        ArmorStand Source = EntityType.ARMOR_STAND.create(World);
        Vec3 Spot = Helper.absoluteVec(new Vec3(4.5, 1.0, 4.5));
        Source.moveTo(Spot.x, Spot.y, Spot.z);
        try {
            Helper.assertTrue(ScpClientConfig.AllowVoiceRecording.getDefault(), "The client option should default to allow");
            Helper.assertTrue(VoiceArchive.allowsSpeaker(Shy.getUUID()), "A player who never reported should count as allowing");
            VoiceArchive.setPlayback(Fake);
            VoicePolicy.forceMaster(true);
            VoiceArchive.clear(Id);
            VoiceArchive.accept(clip(Id, Shy.getUUID(), "voice_opt_out", System.currentTimeMillis() - 3000L, 60, 1));
            Helper.assertTrue(VoiceArchive.capture(Id, Shy), "Capture did not start for an allowing player");
            VoiceEvents.preference(Shy, new VoicePreferencePayload(false));
            Helper.assertFalse(VoiceArchive.allowsSpeaker(Shy.getUUID()), "The opt out was not recorded");
            Helper.assertFalse(VoiceArchive.isCapturing(Id, Shy.getUUID()), "Opting out did not end the open capture");
            Helper.assertFalse(VoiceArchive.capture(Id, Shy), "Capture started for a player who opted out");
            Helper.assertTrue(VoiceArchive.hear(heard(Shy.getUUID(), World, false, false, false, false), new byte[40], System.currentTimeMillis()) == 0,
                    "An opted out player's voice was taken");
            Helper.assertFalse(VoiceArchive.replay(Id, Source, Optional.of(Shy.getUUID())), "An opted out player's only clip was replayed");
            VoiceArchive.accept(clip(Id, Other, "other", System.currentTimeMillis() - 2000L, 60, 2));
            Helper.assertTrue(VoiceArchive.replay(Id, Source, Optional.of(Shy.getUUID())) && Fake.Played.get(Fake.Played.size() - 1).get(0)[0] == (byte) 2,
                    "Replay did not skip the opted out player's clip");
            Helper.assertTrue(VoiceArchive.store(Id).holdsSpeaker(Shy.getUUID()), "Opting out deleted clips that should wait for forgetme or clear");
            VoiceArchive.flush();
            Helper.assertTrue(VoiceFiles.readOptOuts(VoiceArchive.optOutFile(VoiceArchive.root())).contains(Shy.getUUID()), "The opt out was not saved");
            VoiceEvents.preference(Shy, new VoicePreferencePayload(true));
            Helper.assertTrue(VoiceArchive.capture(Id, Shy), "Capture did not resume after opting back in");
            Helper.assertTrue(VoiceArchive.hear(heard(Shy.getUUID(), World, false, false, false, false), new byte[40], System.currentTimeMillis()) == 1,
                    "Voice was not taken after opting back in");
            VoiceArchive.clear(Id);
            VoiceArchive.accept(clip(Id, Shy.getUUID(), "voice_opt_out", System.currentTimeMillis(), 60, 5));
            Helper.assertTrue(VoiceArchive.replay(Id, Source, Optional.empty()), "The player's clip was not replayable after opting back in");
            VoiceArchive.flush();
            Helper.assertFalse(VoiceFiles.readOptOuts(VoiceArchive.optOutFile(VoiceArchive.root())).contains(Shy.getUUID()), "Opting back in was not saved");
        } finally {
            VoiceArchive.setAllowed(Shy.getUUID(), true);
            VoicePolicy.resetForces();
            VoiceArchive.clear(Id);
            VoiceArchive.stopAll(Shy.getUUID());
            VoiceArchive.flush();
            VoiceArchive.setPlayback(Before);
            dismiss(Helper, Shy);
        }
        Helper.succeed();
    }
}
