package net.rai.scp_unleashed.scp939.gametest;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.integration.voicechat.VoiceGate;
import net.rai.scp_unleashed.scp939.Scp939;
import net.rai.scp_unleashed.scp939.Scp939Registry;
import net.rai.scp_unleashed.scp939.Scp939VoiceLure;
import net.rai.scp_unleashed.voice.VoiceArchive;
import net.rai.scp_unleashed.voice.VoicePlayback;
import net.rai.scp_unleashed.voice.VoicePolicy;
import net.rai.scp_unleashed.voice.VoiceProfiles;
import net.rai.scp_unleashed.voice.VoiceRecorder;
import net.rai.scp_unleashed.voice.gametest.VoiceArchiveGameTests;

import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp939VoiceGameTests {
    public static final String Arena = "radio_arena";
    public static final String SilenceReason = "scp939_voice_test";

    private Scp939VoiceGameTests() {
    }

    private static int speak(VoiceArchive.Heard Sample, int Count, long Start) {
        int Heard = 0;
        for (int Index = 0; Index < Count; Index++) {
            byte[] Frame = new byte[40];
            Frame[0] = (byte) Index;
            Heard += Scp939VoiceLure.hear(Sample, Frame, Start + Index * 20L) > 0 ? 1 : 0;
        }
        return Heard;
    }

    private static VoiceArchive.Heard at(UUID Speaker, ServerLevel World, Vec3 Where, boolean Spectator, boolean Disabled, boolean Whispering, boolean Grouped) {
        return new VoiceArchive.Heard(Speaker, "scp939_talker", World.dimension(), Where.x, Where.y, Where.z, Spectator, Disabled, Whispering, Grouped);
    }

    private static int stored() {
        return VoiceArchive.store(Scp939VoiceLure.Anomaly).size();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp939_voiceGate")
    public static void voiceGate(GameTestHelper Helper) throws CommandSyntaxException {
        ServerLevel World = Helper.getLevel();
        MinecraftServer Server = World.getServer();
        Scp939 Hunter = Scp939Registry.Scp939Type.get().create(World);
        Vec3 Spot = Helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(8, 1, 8)));
        Hunter.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        Hunter.setNoAi(true);
        Hunter.setPersistenceRequired();
        World.addFreshEntity(Hunter);
        VoiceArchiveGameTests.FakePlayback Fake = new VoiceArchiveGameTests.FakePlayback();
        VoicePlayback Before = VoiceArchive.playback();
        UUID Speaker = UUID.randomUUID();
        Vec3 Near = Spot.add(6.0, 1.5, 0.0);
        long Past = System.currentTimeMillis() - 10_000L;
        try {
            VoiceArchive.setPlayback(Fake);
            VoicePolicy.forceMaster(true);
            VoiceArchive.clear(Scp939VoiceLure.Anomaly);
            VoicePolicy.forceMaster(false);
            Scp939VoiceLure.tick(Server);
            Scp939VoiceLure.ear(Hunter);
            Helper.assertFalse(Scp939VoiceLure.isListening(), "SCP-939 listens with the master switch off");
            Helper.assertTrue(speak(at(Speaker, World, Near, false, false, false, false), 60, Past) == 0, "SCP-939 recorded a voice with the master switch off");
            Helper.assertFalse(Scp939VoiceLure.replay(World, Hunter, null), "SCP-939 replayed with the master switch off");
            VoicePolicy.forceMaster(true);
            VoicePolicy.forceSwitch(VoicePolicy.Scp939, false);
            Scp939VoiceLure.tick(Server);
            Helper.assertFalse(Scp939VoiceLure.isListening(), "SCP-939 listens with its own switch off");
            VoicePolicy.forceSwitch(VoicePolicy.Scp939, null);
            Scp939VoiceLure.tick(Server);
            Scp939VoiceLure.ear(Hunter);
            Helper.assertTrue(Scp939VoiceLure.isListening(), "SCP-939 does not listen with recording on");
            Helper.assertTrue(speak(at(Speaker, World, Near, true, false, false, false), 60, Past) == 0, "A spectator was recorded");
            Helper.assertTrue(speak(at(Speaker, World, Near, false, true, false, false), 60, Past) == 0, "A player with voice chat disabled was recorded");
            Helper.assertTrue(speak(at(Speaker, World, Near, false, false, true, false), 60, Past) == 0, "A whispering player was recorded");
            Helper.assertTrue(speak(at(Speaker, World, Near, false, false, false, true), 60, Past) == 0, "A player in a voice chat group was recorded");
            Helper.assertTrue(speak(at(Speaker, World, Spot.add(30.0, 1.0, 0.0), false, false, false, false), 60, Past) == 0, "A player 30 blocks away was recorded");
            VoiceArchive.Heard Elsewhere = new VoiceArchive.Heard(Speaker, "scp939_talker", Level.NETHER, Near.x, Near.y, Near.z, false, false, false, false);
            Helper.assertTrue(speak(Elsewhere, 60, Past) == 0, "A player in another dimension was recorded");
            VoiceGate.mark(SilenceReason, Speaker, true);
            Helper.assertTrue(speak(at(Speaker, World, Near, false, false, false, false), 60, Past) == 0, "A silenced player was recorded");
            VoiceGate.mark(SilenceReason, Speaker, false);
            Helper.assertTrue(stored() == 0 && VoiceArchive.recorder().pending() == 0, "A refused voice left data behind");
            Helper.assertTrue(speak(at(Speaker, World, Near, false, false, false, false), 60, Past) == 60, "A nearby talker was not heard");
            VoiceArchive.tick(Server);
            Helper.assertTrue(stored() == 1, "The heard clip was not stored, found " + stored());
            Helper.assertTrue(Hunter.getUUID().equals(VoiceArchive.store(Scp939VoiceLure.Anomaly).clips().get(0).Listener()), "The clip does not remember which SCP-939 heard it");
            Helper.assertTrue(Scp939VoiceLure.replay(World, Hunter, null) && Fake.Played.size() == 1 && Fake.Played.get(0).size() == 60,
                    "SCP-939 did not replay the clip through the voice backend");
            Helper.assertTrue(Fake.Profiles.get(0) == VoiceProfiles.Lure && Fake.Scales.get(0) == Scp939VoiceLure.AdultScale, "SCP-939 lost its own distortion");
            VoicePolicy.forceMaster(false);
            Scp939VoiceLure.tick(Server);
            int Calls = Hunter.calls();
            Hunter.lureCall(World);
            Helper.assertTrue(Hunter.calls() == Calls + 1 && Fake.Played.size() == 1, "With recording off SCP-939 must use its normal lure call");
            Helper.assertTrue(stored() == 1, "Turning recording off touched stored clips");
            VoicePolicy.forceMaster(true);
            Scp939VoiceLure.tick(Server);
            Hunter.discard();
            Helper.assertTrue(Scp939VoiceLure.ears() == 0, "A removed SCP-939 kept listening");
            VoiceArchive.reload();
            Helper.assertTrue(stored() == 1, "SCP-939 clips did not persist through a reload");
            Helper.assertTrue(Server.getCommands().getDispatcher().execute("scp_unleashed voice clear scp939", Server.createCommandSourceStack().withSuppressedOutput()) == 1,
                    "The clear command did not remove the SCP-939 clip");
            Helper.assertTrue(stored() == 0, "SCP-939 clips survived the clear command");
            VoiceArchive.accept(new VoiceRecorder.Finished(Scp939VoiceLure.Anomaly, Speaker, "old", null, System.currentTimeMillis(), VoiceArchiveGameTests.frames(60, 1)));
            Helper.assertTrue(stored() == 1, "The store did not take a clip after clearing");
        } finally {
            VoiceGate.mark(SilenceReason, Speaker, false);
            VoicePolicy.forceMaster(true);
            VoiceArchive.clear(Scp939VoiceLure.Anomaly);
            VoicePolicy.resetForces();
            VoiceArchive.flush();
            VoiceArchive.setPlayback(Before);
            Scp939VoiceLure.clear();
            if (!Hunter.isRemoved()) {
                Hunter.discard();
            }
        }
        Helper.succeed();
    }
}
