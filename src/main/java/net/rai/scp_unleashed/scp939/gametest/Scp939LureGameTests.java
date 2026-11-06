package net.rai.scp_unleashed.scp939.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.integration.voicechat.VoiceGate;
import net.rai.scp_unleashed.radio.VoiceConfig;
import net.rai.scp_unleashed.radio.gametest.RadioTestPlayer;
import net.rai.scp_unleashed.scp939.Scp939;
import net.rai.scp_unleashed.scp939.Scp939LureCadence;
import net.rai.scp_unleashed.scp939.Scp939Registry;
import net.rai.scp_unleashed.scp939.Scp939VoiceAttraction;
import net.rai.scp_unleashed.scp939.Scp939VoiceLure;
import net.rai.scp_unleashed.voice.VoiceArchive;
import net.rai.scp_unleashed.voice.VoicePolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp939LureGameTests {
    public static final String Arena = "radio_arena";

    private Scp939LureGameTests() {
    }

    private static Scp939 hunter(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp939 Made = Scp939Registry.Scp939Type.get().create(World);
        Vec3 Spot = Helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(8, 1, 8)));
        Made.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        Made.setNoAi(true);
        Made.setPersistenceRequired();
        World.addFreshEntity(Made);
        return Made;
    }

    private static RadioTestPlayer player(GameTestHelper Helper, Vec3 Relative, String Name) {
        ServerLevel World = Helper.getLevel();
        RadioTestPlayer User = new RadioTestPlayer(World, Name);
        Vec3 Where = Helper.absoluteVec(Relative);
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

    private static VoiceArchive.Heard spoken(UUID Speaker, ServerLevel World, Vec3 Where, boolean Whispering, boolean Spectator) {
        return new VoiceArchive.Heard(Speaker, "scp939_attract", World.dimension(), Where.x, Where.y, Where.z, Spectator, false, Whispering, false);
    }

    private static boolean drawn(MinecraftServer Server, Scp939 Hunter, VoiceArchive.Heard Sample) {
        Scp939VoiceAttraction.forget(Hunter.getUUID());
        Scp939VoiceAttraction.tick(Server);
        Scp939VoiceAttraction.voiceHeard(Sample, System.currentTimeMillis());
        Scp939VoiceAttraction.tick(Server);
        Scp939VoiceAttraction.Memory Known = Scp939VoiceAttraction.remembered(Hunter, Hunter.level().getGameTime());
        return Known != null && Known.Where().distanceToSqr(new Vec3(Sample.X(), Sample.Y(), Sample.Z())) < 1.0e-6;
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp939_voiceAttraction")
    public static void voiceAttraction(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        MinecraftServer Server = World.getServer();
        Scp939 Hunter = hunter(Helper);
        Vec3 Centre = Hunter.position();
        RadioTestPlayer Talker = player(Helper, new Vec3(18.5, 1.0, 8.5), "scp939_attract_t");
        try {
            VoicePolicy.forceMaster(true);
            Scp939VoiceAttraction.force(null);
            Helper.assertTrue(VoiceConfig.Scp939VoiceAttraction.getDefault(), "Voice attraction should default to on");
            Helper.assertTrue(drawn(Server, Hunter, spoken(UUID.randomUUID(), World, Centre.add(20.0, 1.0, 0.0), false, false)), "Speech 20 blocks away did not draw SCP-939");
            Helper.assertTrue(Scp939LureCadence.state(World, Hunter) == Scp939LureCadence.State.Stalking, "A heard voice should make SCP-939 stalk");
            Helper.assertFalse(drawn(Server, Hunter, spoken(UUID.randomUUID(), World, Centre.add(40.0, 1.0, 0.0), false, false)), "Speech 40 blocks away drew SCP-939");
            Helper.assertFalse(drawn(Server, Hunter, spoken(UUID.randomUUID(), World, Centre.add(10.0, 1.0, 0.0), true, false)), "A whisper 10 blocks away drew SCP-939");
            Helper.assertTrue(drawn(Server, Hunter, spoken(UUID.randomUUID(), World, Centre.add(4.0, 1.0, 0.0), true, false)), "A whisper 4 blocks away did not draw SCP-939");
            Helper.assertFalse(drawn(Server, Hunter, spoken(UUID.randomUUID(), World, Centre.add(10.0, 1.0, 0.0), false, true)), "A spectator drew SCP-939");
            UUID Hushed = UUID.randomUUID();
            VoiceGate.mark("scp939_attract", Hushed, true);
            Helper.assertFalse(drawn(Server, Hunter, spoken(Hushed, World, Centre.add(10.0, 1.0, 0.0), false, false)), "A silenced player drew SCP-939");
            VoiceGate.mark("scp939_attract", Hushed, false);
            UUID Shy = UUID.randomUUID();
            VoiceArchive.setAllowed(Shy, false);
            Helper.assertTrue(drawn(Server, Hunter, spoken(Shy, World, Centre.add(12.0, 1.0, 0.0), false, false)), "A player who opted out of recording did not draw SCP-939");
            VoiceArchive.setAllowed(Shy, true);
            UUID Chatty = UUID.randomUUID();
            Scp939VoiceAttraction.tick(Server);
            long Now = System.currentTimeMillis();
            Helper.assertTrue(Scp939VoiceAttraction.voiceHeard(spoken(Chatty, World, Centre.add(5.0, 1.0, 0.0), false, false), Now), "The first word was not heard");
            Helper.assertFalse(Scp939VoiceAttraction.voiceHeard(spoken(Chatty, World, Centre.add(5.0, 1.0, 0.0), false, false), Now + 200L), "Speech refreshed faster than once a second");
            Helper.assertTrue(Scp939VoiceAttraction.voiceHeard(spoken(Chatty, World, Centre.add(6.0, 1.0, 0.0), false, false), Now + 1100L), "Continuous talking did not refresh the position");
            Scp939VoiceAttraction.tick(Server);
            Scp939VoiceAttraction.forget(Hunter.getUUID());
            Hunter.setTarget(null);
            Helper.assertTrue(drawn(Server, Hunter, spoken(Talker.getUUID(), World, Talker.position(), false, false)) && Hunter.getTarget() == Talker,
                    "A nearby talker was not hunted");
            Hunter.setTarget(null);
            Scp939VoiceAttraction.force(false);
            Helper.assertFalse(drawn(Server, Hunter, spoken(UUID.randomUUID(), World, Centre.add(10.0, 1.0, 0.0), false, false)), "SCP-939 was drawn with attraction off");
            Scp939VoiceAttraction.force(null);
            VoicePolicy.forceMaster(false);
            Helper.assertFalse(drawn(Server, Hunter, spoken(UUID.randomUUID(), World, Centre.add(10.0, 1.0, 0.0), false, false)), "SCP-939 was drawn with recording off");
        } finally {
            VoicePolicy.resetForces();
            Scp939VoiceAttraction.force(null);
            Scp939VoiceAttraction.clear();
            dismiss(Helper, Talker);
            Hunter.discard();
        }
        Helper.succeed();
    }

    private static List<Long> calls(ServerLevel World, Scp939 Hunter, long Start, int Ticks) {
        Scp939LureCadence.forget(Hunter);
        List<Long> Times = new ArrayList<>();
        for (long Now = Start; Now < Start + Ticks; Now++) {
            if (Scp939LureCadence.tick(World, Hunter, Now)) {
                Times.add(Now);
            }
        }
        return Times;
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp939_lureCadence")
    public static void lureCadence(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp939 Hunter = hunter(Helper);
        RadioTestPlayer Prey = player(Helper, new Vec3(8.5 + 20.0, 1.0, 8.5), "scp939_cadence");
        try {
            Hunter.setTarget(Prey);
            Helper.assertTrue(Scp939LureCadence.state(World, Hunter) == Scp939LureCadence.State.Stalking, "SCP-939 with prey 20 blocks off is not stalking");
            long Start = World.getGameTime();
            List<Long> Stalking = calls(World, Hunter, Start, 20 * 180);
            int Min = 20 * VoiceConfig.lureMinSeconds();
            int Max = 20 * VoiceConfig.lureMaxSeconds();
            Helper.assertTrue(Stalking.size() >= 180 / VoiceConfig.lureMaxSeconds(), "SCP-939 called only " + Stalking.size() + " times in three minutes of stalking");
            Helper.assertTrue(Stalking.get(0) - Start <= Max, "The first call came too late: " + (Stalking.get(0) - Start));
            for (int Index = 1; Index < Stalking.size(); Index++) {
                long Gap = Stalking.get(Index) - Stalking.get(Index - 1);
                Helper.assertTrue(Gap >= Min && Gap <= Max, "A stalking call gap of " + Gap + " ticks is outside " + Min + " to " + Max);
            }
            Hunter.setTarget(null);
            Helper.assertTrue(Scp939LureCadence.state(World, Hunter) == Scp939LureCadence.State.Idle, "SCP-939 with a player near but no prey is not idling");
            List<Long> Idle = calls(World, Hunter, Start + 100_000L, 20 * 300);
            int IdleMin = 20 * VoiceConfig.idleLureMinSeconds();
            int IdleMax = 20 * VoiceConfig.idleLureMaxSeconds();
            Helper.assertTrue(Idle.size() >= 300 / VoiceConfig.idleLureMaxSeconds(), "SCP-939 called only " + Idle.size() + " times in five idle minutes");
            for (int Index = 1; Index < Idle.size(); Index++) {
                long Gap = Idle.get(Index) - Idle.get(Index - 1);
                Helper.assertTrue(Gap >= IdleMin && Gap <= IdleMax, "An idle call gap of " + Gap + " ticks is outside " + IdleMin + " to " + IdleMax);
            }
            Prey.moveTo(Prey.getX() + 200.0, Prey.getY(), Prey.getZ());
            Helper.assertTrue(Scp939LureCadence.state(World, Hunter) == Scp939LureCadence.State.Quiet, "SCP-939 with nobody near should stay quiet");
            Helper.assertTrue(calls(World, Hunter, Start + 300_000L, 20 * 120).isEmpty(), "SCP-939 called with nobody near");
            Helper.assertTrue(Scp939VoiceLure.ReplayChance >= 0.85f, "The voice replay share was not raised");
        } finally {
            Scp939LureCadence.forget(Hunter);
            dismiss(Helper, Prey);
            Hunter.discard();
        }
        Helper.succeed();
    }
}
