package net.rai.scp_unleashed.scp5001.xk.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp5001.core.Scp5001Duel;
import net.rai.scp_unleashed.scp5001.xk.YaldabaothFlight;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class YaldabaothFlightGameTests {
    public static final String Arena = "scp5001mekhane_arena";
    public static final int RoamTicks = 12000;
    public static final double Slack = 1.0E-6;

    private YaldabaothFlightGameTests() {
    }

    private static void dump(String Name, StringBuilder Lines) {
        String Folder = System.getenv("SCP_MEKHANE_PLOT");
        if (Folder == null || Folder.isBlank()) {
            return;
        }
        try {
            Files.writeString(Path.of(Folder, Name), Lines.toString());
        } catch (IOException Problem) {
            throw new IllegalStateException(Problem);
        }
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp5001xk_flight")
    public static void roamingTurnsStayCapped(GameTestHelper Helper) {
        YaldabaothFlight Flight = new YaldabaothFlight();
        Flight.manifest(new Vec3(0.0, 180.0, 0.0), new Vec3(1.0, 0.0, 0.0), false, 180.0);
        RandomSource Random = RandomSource.create(5001L);
        StringBuilder Lines = new StringBuilder("tick,x,y,z,mode\n");
        double Worst = 0.0;
        double Fastest = 0.0;
        Vec3 Previous = Flight.head();
        YaldabaothFlight.Mode Earlier = Flight.mode();
        for (int Tick = 0; Tick < RoamTicks; Tick++) {
            switch (Tick) {
                case 2000 -> Flight.retarget(new Vec3(600.0, 0.0, 300.0), 220.0, 200.0);
                case 3200 -> Flight.shape(YaldabaothFlight.Pattern.Eight, 0.6);
                case 4300 -> Flight.startDive(new Vec3(560.0, 70.0, 330.0));
                case 6200 -> Flight.retarget(new Vec3(-250.0, 0.0, -180.0), 170.0, 175.0);
                case 7600 -> Flight.shape(YaldabaothFlight.Pattern.Spiral, 1.2);
                case 9000 -> Flight.startDive(new Vec3(-230.0, 66.0, -150.0));
                case 10600 -> Flight.shape(YaldabaothFlight.Pattern.Ellipse, 2.0);
                default -> {
                }
            }
            Flight.tick(null, Random);
            YaldabaothFlight.Mode Mode = Flight.mode();
            double Cap = Mode == YaldabaothFlight.Mode.Dive || Earlier == YaldabaothFlight.Mode.Dive ? YaldabaothFlight.DiveTurn : YaldabaothFlight.RoamTurn;
            Earlier = Mode;
            Helper.assertTrue(Flight.lastCap() <= Cap + Slack, "roaming used a sharper turn cap " + Flight.lastCap() + " in " + Mode);
            Helper.assertTrue(Flight.lastTurn() <= Flight.lastCap() + Slack, "heading turned " + Flight.lastTurn() + " past the cap " + Flight.lastCap() + " at tick " + Tick);
            Worst = Math.max(Worst, Flight.lastTurn());
            Vec3 Here = Flight.head();
            Fastest = Math.max(Fastest, Here.distanceTo(Previous));
            Previous = Here;
            Lines.append(String.format(Locale.ROOT, "%d,%.3f,%.3f,%.3f,%s%n", Tick, Here.x, Here.y, Here.z, Mode.name()));
        }
        Helper.assertTrue(Fastest <= 1.2, "the head jumped " + Fastest + " blocks in one tick");
        Helper.assertTrue(Worst > 0.0, "the head never turned");
        dump("yaldabaoth_roam_after.csv", Lines);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp5001xk_flight")
    public static void duelManoeuvresStayCapped(GameTestHelper Helper) {
        YaldabaothFlight Flight = new YaldabaothFlight();
        Vec3 Centre = new Vec3(0.0, 180.0, 0.0);
        Flight.manifest(Centre.add(90.0, 10.0, 0.0), new Vec3(0.0, 0.0, 1.0), false, 190.0);
        Flight.engage(-1, Centre);
        RandomSource Random = RandomSource.create(5002L);
        StringBuilder Lines = new StringBuilder("tick,x,y,z,mode\n");
        Vec3 Start = Flight.head();
        double Travel = 0.0;
        Vec3 Previous = Start;
        for (int Tick = 0; Tick < 1600; Tick++) {
            switch (Tick) {
                case 100 -> Flight.attack(Scp5001Duel.Lunge, Centre);
                case 300 -> Flight.attack(Scp5001Duel.Bite, Centre.add(6.0, 4.0, 0.0));
                case 500 -> Flight.attack(Scp5001Duel.Sweep, Centre.subtract(0.0, 18.0, 0.0));
                case 700 -> Flight.attack(Scp5001Duel.Roar, Centre.add(0.0, 10.0, 0.0));
                case 900 -> Flight.attack(Scp5001Duel.Coil, Centre);
                case 1150 -> Flight.react(Centre, 1.6f);
                default -> {
                }
            }
            if (Tick >= 1300 && Tick < 1500) {
                Flight.bind(Centre.add(0.0, 20.0, 90.0), (Tick - 1300) / 200.0f);
            }
            if (Tick == 1500) {
                Flight.banish();
            }
            Flight.tick(null, Random);
            Helper.assertTrue(Flight.lastTurn() <= Flight.lastCap() + Slack, "duel heading turned " + Flight.lastTurn() + " past the cap " + Flight.lastCap() + " at tick " + Tick);
            Vec3 Here = Flight.head();
            Travel += Here.distanceTo(Previous);
            Previous = Here;
            if (Tick < 1300) {
                Vec3 Flat = new Vec3(Here.x - Centre.x, 0.0, Here.z - Centre.z);
                Helper.assertTrue(Flat.length() <= YaldabaothFlight.DuelReach + 40.0, "the serpent left the duel area at tick " + Tick);
            }
            Lines.append(String.format(Locale.ROOT, "%d,%.3f,%.3f,%.3f,%s%n", Tick, Here.x, Here.y, Here.z, Flight.mode().name()));
        }
        Helper.assertTrue(Travel > 800.0, "the serpent barely moved during the duel: " + Travel);
        dump("yaldabaoth_duel_moves.csv", Lines);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp5001xk_voice")
    public static void voiceFollowsTheMoves(GameTestHelper Helper) {
        YaldabaothFlight Flight = new YaldabaothFlight();
        Vec3 Centre = new Vec3(0.0, 180.0, 0.0);
        Flight.manifest(Centre.add(90.0, 10.0, 0.0), new Vec3(0.0, 0.0, 1.0), false, 190.0);
        Flight.engage(-1, Centre);
        RandomSource Random = RandomSource.create(5003L);
        int Lunged = -1;
        int Snapped = -1;
        int Roared = -1;
        int Flinched = -1;
        int Dragged = -1;
        int Banished = -1;
        for (int Tick = 0; Tick < 800; Tick++) {
            if (Tick == 100) {
                Flight.attack(Scp5001Duel.Bite, Centre.add(6.0, 4.0, 0.0));
            }
            if (Tick == 300) {
                Flight.react(Centre, 0.5f);
            }
            if (Tick == 400) {
                Flight.attack(Scp5001Duel.Roar, Centre.add(0.0, 10.0, 0.0));
            }
            if (Tick >= 600 && Tick < 700) {
                Flight.bind(Centre.add(0.0, 20.0, 90.0), (Tick - 600) / 100.0f);
            }
            if (Tick == 700) {
                Flight.banish();
            }
            Flight.tick(null, Random);
            int Said = Flight.voice();
            if ((Said & YaldabaothFlight.VoiceLunge) != 0 && Lunged < 0) {
                Lunged = Tick;
            }
            if ((Said & YaldabaothFlight.VoiceSnap) != 0 && Snapped < 0) {
                Snapped = Tick;
            }
            if ((Said & YaldabaothFlight.VoiceRoar) != 0 && Roared < 0) {
                Roared = Tick;
            }
            if ((Said & YaldabaothFlight.VoiceFlinch) != 0 && Flinched < 0) {
                Flinched = Tick;
            }
            if ((Said & YaldabaothFlight.VoiceDrag) != 0 && Dragged < 0) {
                Dragged = Tick;
            }
            if ((Said & YaldabaothFlight.VoiceBanish) != 0 && Banished < 0) {
                Banished = Tick;
            }
        }
        Helper.assertTrue(Lunged > 100 && Lunged < Snapped, "the lunge must be heard before the jaws close: " + Lunged + " / " + Snapped);
        Helper.assertTrue(Snapped == 100 + YaldabaothFlight.ImpactAt - 4, "the jaws must snap at the strike, not when the move starts: " + Snapped);
        Helper.assertTrue(Flinched == 300, "a light hit must draw a flinch: " + Flinched);
        Helper.assertTrue(Roared > 400 && Roared <= 420, "a small hit must not roar, the roar move must: " + Roared);
        Helper.assertTrue(Dragged == 600 && Banished == 700, "the portal drag and banish must be voiced: " + Dragged + " / " + Banished);
        Helper.succeed();
    }
}
