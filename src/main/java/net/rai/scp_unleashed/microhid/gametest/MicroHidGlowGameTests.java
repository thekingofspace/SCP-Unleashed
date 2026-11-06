package net.rai.scp_unleashed.microhid.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.microhid.MicroHidGlow;

import java.util.List;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class MicroHidGlowGameTests {
    public static final String Arena = "micro_hid_arena";
    public static final double Tolerance = 1.0E-9;
    public static final List<Vec3> Samples = List.of(new Vec3(0.0, 0.0, -3.0), new Vec3(1.5, -0.4, -6.0), new Vec3(-7.0, 2.0, 11.0), new Vec3(0.2, 0.1, -0.4),
            new Vec3(0.01, 0.0, -0.03), new Vec3(30.0, -12.0, 4.0));
    public static final double[] Pulls = {0.0, 0.07, 0.24, 0.35, 0.9, 1.7, 40.0};
    public static final int Lane = 5;
    public static final int FromX = 2;
    public static final int ToX = 10;

    private MicroHidGlowGameTests() {
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "micro_hid_glow")
    public static void glowPullKeepsTheScreenPosition(GameTestHelper Helper) {
        for (Vec3 Local : Samples) {
            double Distance = Local.length();
            for (double Pull : Pulls) {
                Vec3 Moved = MicroHidGlow.pulled(Local, Pull);
                double Kept = Moved.length();
                Helper.assertTrue(Moved.normalize().dot(Local.normalize()) > 1.0 - Tolerance, "a pulled glow vertex left its sight line: " + Local + " -> " + Moved);
                Helper.assertTrue(Kept <= Distance + Tolerance, "a pulled glow vertex moved away from the eye: " + Local + " pull " + Pull);
                Helper.assertTrue(Kept >= Math.min(Distance, MicroHidGlow.NearPlane) - Tolerance, "a pulled glow vertex crossed the near plane: " + Local + " pull " + Pull);
                double Expected = Math.max(Distance - Pull, Math.min(Distance, MicroHidGlow.NearPlane));
                Helper.assertTrue(Math.abs(Kept - Expected) < 1.0E-7, "glow pull " + Pull + " at distance " + Distance + " kept " + Kept + " instead of " + Expected);
            }
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "micro_hid_glow")
    public static void onlyThingsInFrontHideTheGlow(GameTestHelper Helper) {
        double[] Widths = {0.022, 0.05, 0.07, 0.16, 0.24, 0.34, 0.6};
        for (double Width : Widths) {
            double Pull = MicroHidGlow.ribbonPull(Width);
            Helper.assertTrue(Pull > 0.0 && Pull <= Width && Pull <= MicroHidGlow.RibbonPullLimit, "ribbon pull " + Pull + " for half width " + Width);
            for (double Core = 1.0; Core < 40.0; Core *= 1.7) {
                Vec3 Point = new Vec3(0.3, -0.2, -1.0).normalize().scale(Core);
                double Drawn = MicroHidGlow.pulled(Point, Pull).length();
                Helper.assertTrue(Drawn <= Core - Pull + Tolerance || Drawn <= MicroHidGlow.NearPlane + Tolerance, "glow at " + Core + " drawn at depth " + Drawn);
                double Behind = Core + 0.01;
                Helper.assertTrue(Drawn < Behind, "a block behind the beam core at " + Behind + " would cover the glow drawn at " + Drawn);
                double Grazing = Core - Pull * 0.5;
                Helper.assertTrue(Drawn <= Grazing || Grazing <= MicroHidGlow.NearPlane, "a block inside the glow radius at " + Grazing + " would cover the glow");
                double Front = Core - Pull - 0.05;
                if (Front > MicroHidGlow.NearPlane) {
                    Helper.assertTrue(Front < Drawn, "a block in front of the glow at " + Front + " no longer hides it");
                }
            }
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "micro_hid_glow")
    public static void impactGlowClearsTheSurfaceItHits(GameTestHelper Helper) {
        Vec3 Normal = new Vec3(0.0, 1.0, 0.0);
        Vec3 Across = new Vec3(1.0, 0.0, 0.0);
        double[] Radii = {0.35, 0.9, 1.7};
        int Lifted = 0;
        int Checked = 0;
        for (double Height = 0.3; Height < 12.0; Height *= 1.6) {
            for (double Back = 0.0; Back < 40.0; Back = Back * 1.8 + 0.4) {
                Vec3 Impact = new Vec3(0.2, -Height, -Back);
                Vec3 Facing = Impact.scale(-1.0).normalize();
                Vec3 Side = Facing.cross(Across).normalize();
                Vec3 Up = Side.cross(Facing).normalize();
                for (double Radius : Radii) {
                    for (int Step = 0; Step < 24; Step++) {
                        double Angle = Step * Math.PI * 2.0 / 24.0;
                        for (double Share : new double[]{0.0, 0.5, 1.0}) {
                            Vec3 Corner = Impact.add(Side.scale(Math.cos(Angle) * Radius * Share)).add(Up.scale(Math.sin(Angle) * Radius * Share));
                            Vec3 Drawn = MicroHidGlow.cleared(MicroHidGlow.pulled(Corner, Radius), Impact, Normal);
                            double Above = Drawn.subtract(Impact).dot(Normal);
                            Helper.assertTrue(Drawn.normalize().dot(Corner.normalize()) > 1.0 - Tolerance, "clearing moved the glow off its sight line");
                            Helper.assertTrue(Drawn.length() <= Corner.length() + Tolerance, "clearing pushed the glow away from the eye");
                            Helper.assertTrue(Above >= MicroHidGlow.SurfaceMargin - 1.0E-9 || Drawn.length() <= MicroHidGlow.NearPlane + Tolerance,
                                    "part of the impact glow stays under the surface it hit: height " + Height + " back " + Back + " radius " + Radius + " above " + Above);
                            if (Corner.subtract(Impact).dot(Normal) < 0.0) {
                                Lifted++;
                            }
                            Checked++;
                        }
                    }
                }
            }
        }
        Helper.assertTrue(Checked > 1000 && Lifted > 100, "too few impact glow points were checked: " + Checked + " lifted " + Lifted);
        Vec3 Clear = new Vec3(0.1, 0.5, -3.0);
        Helper.assertTrue(MicroHidGlow.cleared(Clear, new Vec3(0.0, -1.0, -3.0), Normal).equals(Clear), "glow already above the surface was moved");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "micro_hid_glow")
    public static void handZoneSplitsAtTheEdge(GameTestHelper Helper) {
        double Muzzle = 1.2;
        double Edge = MicroHidGlow.handEdge(Muzzle);
        Helper.assertTrue(Edge > Muzzle && Edge - Muzzle <= 1.5, "the first person hand zone ends at " + Edge + " for a muzzle at " + Muzzle);
        Helper.assertTrue(MicroHidGlow.inHand(Muzzle, Edge) && !MicroHidGlow.inHand(Edge + 0.01, Edge), "the hand zone does not cover the muzzle");
        Helper.assertTrue(!MicroHidGlow.inHand(0.5, -1.0), "a third person beam has no hand zone");
        double Cut = MicroHidGlow.crossing(Muzzle, Muzzle + 4.0, Edge);
        Helper.assertTrue(Math.abs(Muzzle + Cut * 4.0 - Edge) < 1.0E-9, "the hand zone split lands at " + (Muzzle + Cut * 4.0) + " instead of " + Edge);
        Helper.assertTrue(MicroHidGlow.crossing(5.0, 1.0, Edge) > 0.0 && MicroHidGlow.crossing(5.0, 1.0, Edge) < 1.0, "a beam pointing back at the eye does not split");
        Helper.assertTrue(MicroHidGlow.crossing(1.0, 1.0, Edge) == 0.5, "a flat segment split is not centred");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "micro_hid_glow")
    public static void impactGlowStaysBehindWalls(GameTestHelper Helper) {
        int Floor = MicroHidGameTests.stand(Helper);
        for (int X = FromX; X <= ToX; X++) {
            for (int Y = Floor; Y <= Floor + 2; Y++) {
                Helper.setBlock(new BlockPos(X, Y, Lane), Blocks.AIR);
            }
        }
        Helper.setBlock(new BlockPos(ToX, Floor + 1, Lane), Blocks.STONE);
        Vec3 Eye = Helper.absoluteVec(new Vec3(FromX + 0.5, Floor + 1.62, Lane + 0.5));
        Vec3 Impact = Helper.absoluteVec(new Vec3(ToX, Floor + 1.5, Lane + 0.5));
        Helper.assertTrue(MicroHidGlow.seen(Helper.getLevel(), Eye, Impact), "the wall the beam hits hides its own impact glow");
        Helper.setBlock(new BlockPos(6, Floor + 1, Lane), Blocks.STONE);
        Helper.assertTrue(!MicroHidGlow.seen(Helper.getLevel(), Eye, Impact), "an impact behind a wall is treated as visible, its glow would show through the wall");
        Helper.setBlock(new BlockPos(6, Floor, Lane), Blocks.SAND);
        Helper.setBlock(new BlockPos(6, Floor + 1, Lane), Blocks.DEAD_BUSH);
        Helper.assertTrue(MicroHidGlow.seen(Helper.getLevel(), Eye, Impact), "a dead bush in front of the impact hides its glow");
        Helper.succeed();
    }
}
