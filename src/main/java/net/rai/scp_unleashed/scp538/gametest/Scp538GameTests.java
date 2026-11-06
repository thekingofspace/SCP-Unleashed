package net.rai.scp_unleashed.scp538.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.EffectCures;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.furnishing.FurnishingRegistry;
import net.rai.scp_unleashed.scp500.Scp500Panacea;
import net.rai.scp_unleashed.scp538.Scp538Infection;
import net.rai.scp_unleashed.scp538.Scp538Light;
import net.rai.scp_unleashed.scp538.Scp538Registry;
import net.rai.scp_unleashed.scp538.Scp538Spider;
import net.rai.scp_unleashed.scp538.Scp538Surfaces;
import net.rai.scp_unleashed.scp538.Scp538Swarm;
import net.rai.scp_unleashed.stasis.StasisPodItem;
import net.rai.scp_unleashed.stasis.StasisPods;
import net.rai.scp_unleashed.stasis.StasisRegistry;
import net.rai.scp_unleashed.stranger.StrangerEncounterCondition;
import net.rai.scp_unleashed.stranger.StrangerGift;
import net.rai.scp_unleashed.stranger.StrangerPlan;
import net.rai.scp_unleashed.transferpod.TransferPodContent;
import net.rai.scp_unleashed.transferpod.TransferPodRegistry;
import net.rai.scp_unleashed.transferpod.TransferPods;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp538GameTests {
    public static final String Arena = "scp538_arena";
    public static final int Low = 1;
    public static final int High = 13;
    public static final int Floor = 1;
    public static final BlockPos Middle = new BlockPos(7, Floor + 1, 7);

    private Scp538GameTests() {
    }

    private static void lightRoom(GameTestHelper Helper, int Level) {
        for (int X = Low; X <= High; X++) {
            for (int Z = Low; Z <= High; Z++) {
                for (int Y = Floor + 1; Y <= Floor + 2; Y++) {
                    Helper.setBlock(new BlockPos(X, Y, Z), Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, Level));
                }
            }
        }
    }

    private static void lightStrip(GameTestHelper Helper, int Level, int ToX) {
        for (int X = Low; X <= ToX; X++) {
            for (int Z = Low; Z <= High; Z++) {
                Helper.setBlock(new BlockPos(X, Floor + 1, Z), Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, Level));
            }
        }
    }

    private static void settled(GameTestHelper Helper, int Level) {
        for (BlockPos Spot : new BlockPos[]{Middle, new BlockPos(Low, Floor + 1, Low), new BlockPos(High, Floor + 1, High), new BlockPos(Low, Floor + 2, High)}) {
            Helper.assertTrue(light(Helper, Spot) == Level, "The room light has not settled at " + Spot + ": " + light(Helper, Spot) + " instead of " + Level);
        }
    }

    private static int light(GameTestHelper Helper, BlockPos Relative) {
        return Scp538Light.at(Helper.getLevel(), Helper.absolutePos(Relative));
    }

    private static Scp538Spider spider(GameTestHelper Helper, BlockPos Relative) {
        return spider(Helper, Helper.absoluteVec(Vec3.atBottomCenterOf(Relative)));
    }

    private static Scp538Spider spider(GameTestHelper Helper, Vec3 Where) {
        ServerLevel World = Helper.getLevel();
        Scp538Spider Spider = Scp538Registry.Spider.get().create(World);
        Spider.moveTo(Where.x, Where.y, Where.z, 0.0f, 0.0f);
        Spider.setPersistenceRequired();
        World.addFreshEntity(Spider);
        return Spider;
    }

    private static Scp538TestPlayer player(GameTestHelper Helper, BlockPos Relative, String Name, GameType Mode) {
        ServerLevel World = Helper.getLevel();
        Scp538TestPlayer User = new Scp538TestPlayer(World, Name);
        Vec3 Where = Helper.absoluteVec(Vec3.atBottomCenterOf(Relative));
        User.moveTo(Where.x, Where.y, Where.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        User.setGameMode(Mode);
        return User;
    }

    private static void dismiss(GameTestHelper Helper, ServerPlayer User) {
        if (!User.isRemoved()) {
            Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
        }
    }

    private static List<Scp538Spider> spiders(GameTestHelper Helper) {
        AABB Box = new AABB(Helper.absolutePos(BlockPos.ZERO)).expandTowards(16.0, 8.0, 16.0).inflate(2.0);
        return Helper.getLevel().getEntitiesOfClass(Scp538Spider.class, Box, Entity::isAlive);
    }

    private static String describe(GameTestHelper Helper, Scp538Spider Spider) {
        return "[alive " + Spider.isAlive() + ", mood " + Spider.mood() + ", fade " + Spider.fade() + ", light " + Scp538Light.at(Spider) + ", crawling "
                + Spider.isCrawling() + ", surface " + Spider.surface() + ", at " + Spider.position().subtract(Helper.absoluteVec(Vec3.ZERO)) + ", target "
                + Spider.getTarget() + ", bites " + Spider.bites() + "]";
    }

    private static void sweep(GameTestHelper Helper) {
        for (Scp538Spider Spider : spiders(Helper)) {
            Spider.discard();
        }
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp538_fadesInDarkness")
    public static void fadesInDarkness(GameTestHelper Helper) {
        List<Scp538Spider> Placed = new ArrayList<>();
        Helper.startSequence().thenWaitUntil(() -> Helper.assertTrue(light(Helper, Middle) < Scp538Light.Fading, "The sealed arena is not dark yet: light "
                + light(Helper, Middle))).thenExecute(() -> Placed.add(spider(Helper, Middle))).thenIdle(5).thenExecute(() -> {
            Helper.assertTrue(Placed.get(0).fade() > 0, "A spider in darkness is not fading");
        }).thenExecuteAfter(Scp538Spider.FadeTicks + 10, () -> {
            Helper.assertFalse(Placed.get(0).isAlive(), "A spider left in darkness did not fade away " + describe(Helper, Placed.get(0)));
            sweep(Helper);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp538_lightThresholds")
    public static void lightThresholds(GameTestHelper Helper) {
        Helper.assertTrue(!Scp538Light.bearable(7) && Scp538Light.bearable(8), "Spiders must not bear light 7 but bear light 8");
        Helper.assertTrue(Scp538Light.restless(8) && Scp538Light.restless(11) && !Scp538Light.restless(12) && !Scp538Light.restless(7),
                "The restless band is not 8 to 11");
        Helper.assertTrue(!Scp538Light.calming(11) && Scp538Light.calming(12) && Scp538Light.calming(15), "Light 12 and above must be calm");
        lightStrip(Helper, 15, 4);
        List<Scp538Spider> Placed = new ArrayList<>();
        int[] Wanted = {12, 11, 8, 7};
        BlockPos[] Spots = new BlockPos[Wanted.length];
        Helper.startSequence().thenIdle(20).thenWaitUntil(() -> {
            for (int Index = 0; Index < Wanted.length; Index++) {
                Spots[Index] = null;
                for (int X = 5; X <= High; X++) {
                    BlockPos Spot = new BlockPos(X, Floor + 1, 3 + Index * 3);
                    if (light(Helper, Spot) == Wanted[Index]) {
                        Spots[Index] = Spot;
                        break;
                    }
                }
                Helper.assertTrue(Spots[Index] != null, "No cell with light " + Wanted[Index] + " was found in the gradient yet");
            }
        }).thenExecute(() -> {
            for (BlockPos Spot : Spots) {
                Placed.add(spider(Helper, Spot));
            }
        }).thenIdle(6).thenExecute(() -> {
            Helper.assertTrue(Placed.get(0).mood() == Scp538Spider.Calm, "A spider in light 12 is not calm: " + Placed.get(0).mood());
            Helper.assertTrue(Placed.get(1).mood() == Scp538Spider.Restless, "A spider in light 11 is not restless: " + Placed.get(1).mood());
            Helper.assertTrue(Placed.get(2).mood() == Scp538Spider.Restless && Placed.get(2).fade() == 0, "A spider in light 8 is not restless or is fading");
            Helper.assertTrue(Placed.get(3).fade() > 0, "A spider in light 7 is not fading");
            sweep(Helper);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp538_litPathsOnly")
    public static void litPathsOnly(GameTestHelper Helper) {
        lightStrip(Helper, 15, 4);
        Helper.startSequence().thenWaitUntil(() -> {
            Helper.assertTrue(light(Helper, new BlockPos(2, Floor + 1, 7)) == 15, "The lit end is not lit yet");
            Helper.assertTrue(light(Helper, new BlockPos(13, Floor + 1, 7)) == 6, "The dark end has not settled yet: " + light(Helper, new BlockPos(13, Floor + 1, 7)));
        }).thenExecute(() -> {
            ServerLevel World = Helper.getLevel();
            Scp538Spider Spider = spider(Helper, new BlockPos(2, Floor + 1, 7));
            Spider.moveTo(Helper.absoluteVec(new Vec3(2.5, Floor + 1.0, 7.5)));
            Spider.setOnGround(true);
            Path Route = Spider.getNavigation().createPath(Helper.absolutePos(new BlockPos(13, Floor + 1, 7)), 0);
            Helper.assertTrue(Route != null, "No ground path was built toward the dark end");
            for (int Index = 0; Index < Route.getNodeCount(); Index++) {
                Node Step = Route.getNode(Index);
                int Light = Scp538Light.at(World, Step.asBlockPos());
                Helper.assertTrue(Scp538Light.bearable(Light), "The ground path steps into light " + Light + " at " + Step.asBlockPos());
            }
            Helper.assertFalse(Route.canReach(), "The ground path claims to reach a cell in darkness");
            Scp538Surfaces.Search Search = new Scp538Surfaces.Search(World, Spider, List.of());
            Scp538Surfaces.Node Start = Search.node(Helper.absolutePos(new BlockPos(2, Floor + 1, 7)), Direction.DOWN);
            Helper.assertTrue(Start != null, "No surface node under the spider");
            for (int MinLight : new int[]{Scp538Light.Fading, Scp538Light.Calming}) {
                List<Scp538Surfaces.Node> Crawl = Search.plan(Start, MinLight, Scp538Surfaces.Goal.Flee, Helper.absoluteVec(new Vec3(1.5, Floor + 1.0, 7.5)));
                for (Scp538Surfaces.Node Each : Crawl) {
                    Helper.assertTrue(Search.light(Each.Cell()) >= MinLight, "A crawl route with floor " + MinLight + " enters light " + Search.light(Each.Cell()));
                }
            }
            sweep(Helper);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp538_daylightDoesNotCalm")
    public static void daylightDoesNotCalm(GameTestHelper Helper) {
        Helper.assertTrue(Scp538Light.bearable(15), "Full daylight is not bearable");
        Helper.assertFalse(Scp538Light.calmed(15, 0), "Daylight alone calmed the spiders");
        Helper.assertFalse(Scp538Light.calmed(14, 11), "Dim lamps in daylight calmed the spiders");
        Helper.assertTrue(Scp538Light.calmed(15, 12), "Bright lamps did not calm the spiders");
        Helper.assertTrue(Scp538Light.calmed(12, 12), "Bright lamps at night did not calm the spiders");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 600, batch = "scp538_spreadOut")
    public static void spidersSpreadOut(GameTestHelper Helper) {
        lightRoom(Helper, 15);
        List<Scp538Spider> Placed = new ArrayList<>();
        Helper.startSequence().thenWaitUntil(() -> settled(Helper, 15)).thenExecute(() -> {
            for (int Index = 0; Index < 4; Index++) {
                Placed.add(spider(Helper, Middle));
            }
        }).thenIdle(450).thenExecute(() -> {
            try {
                int Close = 0;
                for (int First = 0; First < Placed.size(); First++) {
                    for (int Second = First + 1; Second < Placed.size(); Second++) {
                        if (Placed.get(First).distanceTo(Placed.get(Second)) < 0.35) {
                            Close++;
                        }
                    }
                }
                Helper.assertTrue(Close == 0, Close + " pairs of spiders are still clumped together");
            } finally {
                sweep(Helper);
            }
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp538_calmInBrightLight")
    public static void calmInBrightLight(GameTestHelper Helper) {
        lightRoom(Helper, 15);
        List<ServerPlayer> Users = new ArrayList<>();
        List<Scp538Spider> Placed = new ArrayList<>();
        Helper.startSequence().thenWaitUntil(() -> settled(Helper, 15)).thenExecute(() -> {
            Users.add(player(Helper, Middle, "scp538_calm", GameType.SURVIVAL));
            Placed.add(spider(Helper, Middle.east(2)));
            Placed.add(spider(Helper, Middle.west(2)));
        }).thenIdle(100).thenExecute(() -> {
            try {
                Helper.assertTrue(Scp538Light.calming(light(Helper, Middle)), "The bright room is not bright: " + light(Helper, Middle));
                for (Scp538Spider Spider : Placed) {
                    Helper.assertTrue(Spider.isAlive() && Spider.bites() == 0, "A spider bit someone in bright light");
                    Helper.assertTrue(Spider.mood() == Scp538Spider.Calm, "A spider in bright light is not calm: " + Spider.mood());
                }
                Helper.assertFalse(Scp538Infection.isInfected(Users.get(0)), "A player in bright light was infected");
            } finally {
                Users.forEach(User -> dismiss(Helper, User));
                sweep(Helper);
            }
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp538_huntsInDimLight")
    public static void huntsInDimLight(GameTestHelper Helper) {
        lightRoom(Helper, 10);
        List<ServerPlayer> Users = new ArrayList<>();
        List<Scp538Spider> Placed = new ArrayList<>();
        Helper.startSequence().thenWaitUntil(() -> settled(Helper, 10)).thenExecute(() -> {
            Users.add(player(Helper, Middle, "scp538_prey", GameType.SURVIVAL));
            Placed.add(spider(Helper, Middle.east(3)));
            Helper.assertTrue(Scp538Light.restless(light(Helper, Middle)), "The dim room is not dim: " + light(Helper, Middle));
        }).thenWaitUntil(() -> Helper.assertTrue(Scp538Infection.isInfected(Users.get(0)), "The spider has not bitten the player yet " + describe(Helper, Placed.get(0))
                + " player at " + Users.get(0).position().subtract(Helper.absoluteVec(Vec3.ZERO)))).thenExecute(() -> {
            try {
                Helper.assertTrue(Placed.get(0).bites() >= 1, "The infection did not come from a bite");
                Helper.assertTrue(Scp538Infection.stage(Users.get(0)) == 1, "A fresh bite is not stage 1");
            } finally {
                Users.forEach(User -> dismiss(Helper, User));
                sweep(Helper);
            }
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 1000, batch = "scp538_seeksShadow")
    public static void seeksShadow(GameTestHelper Helper) {
        lightRoom(Helper, 15);
        BlockPos Table = new BlockPos(9, Floor + 1, 7);
        Helper.setBlock(Table, FurnishingRegistry.CafeteriaTable.get().defaultBlockState());
        List<Scp538Spider> Placed = new ArrayList<>();
        Helper.startSequence().thenWaitUntil(() -> {
            settled(Helper, 15);
            Helper.assertTrue(Scp538Light.calming(light(Helper, new BlockPos(1, Floor + 5, 1))), "The ceiling corner is not lit yet");
        }).thenExecute(() -> {
            ServerLevel World = Helper.getLevel();
            Scp538Spider Probe = spider(Helper, Middle);
            Placed.add(Probe);
            Scp538Surfaces.Search Search = new Scp538Surfaces.Search(World, Probe, List.of());
            Scp538Surfaces.Node Open = Search.node(Helper.absolutePos(Middle), Direction.DOWN);
            Scp538Surfaces.Node Under = Search.node(Helper.absolutePos(Table), Direction.UP);
            Scp538Surfaces.Node Corner = Search.node(Helper.absolutePos(new BlockPos(1, Floor + 1, 1)), Direction.DOWN);
            Scp538Surfaces.Node Ceiling = Search.node(Helper.absolutePos(new BlockPos(1, Floor + 5, 1)), Direction.UP);
            Helper.assertTrue(Open != null && Under != null && Corner != null && Ceiling != null,
                    "Expected surface nodes are missing: open " + Open + ", under " + Under + ", corner " + Corner + ", ceiling " + Ceiling);
            Helper.assertTrue(Search.score(Under) > Search.score(Open) + 2.0, "Under the table does not score as shade: " + Search.score(Under) + " vs " + Search.score(Open));
            Helper.assertTrue(Search.score(Corner) > Search.score(Open) + 2.0, "A floor corner does not score as shade");
            Helper.assertTrue(Search.score(Ceiling) > Search.score(Corner), "A ceiling corner does not outrank a floor corner");
            Helper.assertTrue(Search.score(Open) < Scp538Surfaces.MinScore, "Open floor scores as a shadow spot");
        }).thenWaitUntil(() -> {
            Helper.assertTrue(!Placed.isEmpty(), "No spider was placed");
            Scp538Spider Spider = Placed.get(0);
            Helper.assertTrue(Spider.isAlive() && Spider.isCrawling() && Spider.route() == null && Spider.anchor() != null, "The spider has not settled yet " + describe(Helper, Spider));
            Scp538Surfaces.Search Search = new Scp538Surfaces.Search(Helper.getLevel(), Spider, List.of());
            Helper.assertTrue(Search.score(Spider.anchor()) >= Scp538Surfaces.MinScore, "The spider settled in the open");
        }).thenExecute(() -> {
            Scp538Spider Spider = Placed.get(0);
            Helper.assertTrue(Spider.anchor().Side() == Direction.UP || Spider.anchor().Side() == Direction.DOWN, "The spider settled on a wall");
            Helper.assertTrue(Spider.position().distanceToSqr(Helper.absoluteVec(Vec3.atBottomCenterOf(Middle))) > 0.25, "The spider never left the open floor");
            sweep(Helper);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "scp538_biteScatters")
    public static void biteScatters(GameTestHelper Helper) {
        lightRoom(Helper, 10);
        List<ServerPlayer> Users = new ArrayList<>();
        List<Scp538Spider> Placed = new ArrayList<>();
        double[] Before = new double[1];
        Helper.startSequence().thenWaitUntil(() -> settled(Helper, 10)).thenExecute(() -> {
            Users.add(player(Helper, Middle, "scp538_bitten", GameType.SURVIVAL));
            for (BlockPos Spot : new BlockPos[]{Middle.east(2), Middle.west(2), Middle.north(2), Middle.south(3)}) {
                Placed.add(spider(Helper, Spot));
            }
        }).thenIdle(2).thenExecute(() -> {
            ServerLevel World = Helper.getLevel();
            Helper.assertTrue(Placed.get(0).bite(World, Users.get(0)), "A bite did not infect the player");
            Helper.assertTrue(Scp538Infection.isInfected(Users.get(0)), "The bitten player is not infected");
            Helper.assertFalse(Scp538Swarm.canBite(Users.get(0)), "An infected player can still be bitten");
            Before[0] = Placed.stream().mapToDouble(Spider -> Spider.distanceTo(Users.get(0))).average().orElse(0.0);
        }).thenIdle(10).thenExecute(() -> {
            for (Scp538Spider Spider : Placed) {
                Helper.assertTrue(Spider.mood() == Scp538Spider.Fleeing && Spider.threat() != null, "A spider did not flee from the bitten player");
                Helper.assertTrue(Spider.getTarget() == null, "A spider still targets the bitten player");
            }
        }).thenIdle(80).thenExecute(() -> {
            try {
                double After = Placed.stream().mapToDouble(Spider -> Spider.distanceTo(Users.get(0))).average().orElse(0.0);
                Helper.assertTrue(After > Before[0] + 0.5, "The spiders did not move away (" + Before[0] + " to " + After + ")");
                int Bites = Placed.stream().mapToInt(Scp538Spider::bites).sum();
                Helper.assertTrue(Bites == 1, "The spiders bit " + Bites + " times");
            } finally {
                Users.forEach(User -> dismiss(Helper, User));
                sweep(Helper);
            }
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp538_deathBurst")
    public static void deathBurst(GameTestHelper Helper) {
        lightRoom(Helper, 15);
        List<ServerPlayer> Users = new ArrayList<>();
        Helper.startSequence().thenIdle(15).thenExecute(() -> {
            Scp538TestPlayer User = player(Helper, Middle, "scp538_doomed", GameType.SURVIVAL);
            Users.add(User);
            Helper.assertTrue(Scp538Infection.infect(User), "The player could not be infected");
            Scp538Infection.setElapsed(User, Scp538Infection.Duration - 3);
        }).thenIdle(10).thenExecute(() -> {
            try {
                ServerPlayer User = Users.get(0);
                Helper.assertTrue(User.isDeadOrDying(), "The infection did not end in death");
                Helper.assertFalse(Scp538Infection.isInfected(User), "The dead player is still infected");
                int Count = spiders(Helper).size();
                Helper.assertTrue(Count == Scp538Swarm.BurstMax, "The death burst made " + Count + " spiders, not " + Scp538Swarm.BurstMax);
            } finally {
                Users.forEach(User -> dismiss(Helper, User));
                sweep(Helper);
            }
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp538_burstCap")
    public static void burstCap(GameTestHelper Helper) {
        lightRoom(Helper, 15);
        Helper.assertTrue(Scp538Swarm.allowedFor(20, 0, 0) == 20, "An empty area does not allow a full burst");
        Helper.assertTrue(Scp538Swarm.allowedFor(20, Scp538Swarm.AreaCap - 5, 0) == 5, "The area cap is not respected");
        Helper.assertTrue(Scp538Swarm.allowedFor(20, 0, Scp538Swarm.LevelCap - 3) == 3, "The world cap is not respected");
        Helper.assertTrue(Scp538Swarm.allowedFor(20, Scp538Swarm.AreaCap, 0) == 0 && Scp538Swarm.allowedFor(20, 0, Scp538Swarm.LevelCap + 4) == 0,
                "A full area or world still allows spiders");
        Helper.startSequence().thenIdle(15).thenExecute(() -> {
            ServerLevel World = Helper.getLevel();
            Vec3 Where = Helper.absoluteVec(Vec3.atBottomCenterOf(Middle));
            int Base = Scp538Swarm.countNear(World, Where, Scp538Swarm.AreaRadius);
            int First = Scp538Swarm.spawn(World, Where, Scp538Swarm.AreaCap - Base - 5, 0.0f);
            Helper.assertTrue(First == Scp538Swarm.AreaCap - Base - 5, "The pre-fill made " + First + " spiders");
            DClass Body = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), Middle.east(2));
            Helper.assertTrue(Scp538Infection.infect(Body), "The D-Class could not be infected");
            int Burst = Scp538Swarm.burst(World, Body);
            Helper.assertTrue(Burst == 5, "The burst ignored the area cap: " + Burst);
            Helper.assertTrue(Scp538Swarm.countNear(World, Where, Scp538Swarm.AreaRadius) == Scp538Swarm.AreaCap, "The area is not exactly at its cap");
            Body.discard();
            sweep(Helper);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp538_dclassBitten")
    public static void dclassBitten(GameTestHelper Helper) {
        lightRoom(Helper, 15);
        Helper.startSequence().thenIdle(15).thenExecute(() -> {
            ServerLevel World = Helper.getLevel();
            DClass Body = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), Middle);
            Scp538Spider Spider = spider(Helper, Middle.east(1));
            Helper.assertTrue(Scp538Swarm.canBite(Body), "D-Class cannot be bitten");
            Helper.assertTrue(Spider.bite(World, Body), "A bite did not infect the D-Class");
            Helper.assertTrue(Scp538Infection.isInfected(Body), "The D-Class is not infected");
            Scp538Infection.setElapsed(Body, Scp538Infection.Duration - 2);
        }).thenIdle(8).thenExecute(() -> {
            List<DClass> Bodies = Helper.getLevel().getEntitiesOfClass(DClass.class, new AABB(Helper.absolutePos(Middle)).inflate(3.0), Entity::isAlive);
            Helper.assertTrue(Bodies.isEmpty(), "The infected D-Class did not die");
            Helper.assertTrue(spiders(Helper).size() >= Scp538Swarm.BurstMax, "The D-Class death made no burst");
            sweep(Helper);
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp538_creativeSpared")
    public static void creativeSpared(GameTestHelper Helper) {
        Scp538TestPlayer Creative = player(Helper, Middle, "scp538_creative", GameType.CREATIVE);
        Scp538TestPlayer Watcher = player(Helper, Middle.east(2), "scp538_spectator", GameType.SPECTATOR);
        try {
            Helper.assertFalse(Scp538Swarm.canBite(Creative) || Scp538Swarm.canBite(Watcher), "Creative or spectator players can be bitten");
            Helper.assertFalse(Scp538Infection.infect(Creative), "A creative player was infected");
        } finally {
            dismiss(Helper, Creative);
            dismiss(Helper, Watcher);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp538_bladesPassThrough")
    public static void bladesPassThrough(GameTestHelper Helper) {
        lightRoom(Helper, 15);
        Helper.startSequence().thenIdle(10).thenExecute(() -> {
            Scp538Spider Spider = spider(Helper, Middle);
            Scp538TestPlayer User = player(Helper, Middle.east(1), "scp538_swinger", GameType.SURVIVAL);
            try {
                Helper.assertFalse(Spider.hurt(Spider.damageSources().playerAttack(User), 20.0f), "A blade hurt the spider");
                Helper.assertFalse(Spider.hurt(Spider.damageSources().onFire(), 20.0f), "Fire hurt the spider");
                Helper.assertTrue(Spider.isAlive(), "The spider died to an ordinary blow");
                Helper.assertTrue(Spider.hurt(Spider.damageSources().genericKill(), Float.MAX_VALUE), "The kill command did not reach the spider");
                Helper.assertFalse(Spider.isAlive(), "The kill command did not remove the spider");
                Helper.assertTrue(Scp538Registry.Spider.get().is(StasisRegistry.Immune), "Ordinary stasis pods can hold the spiders");
            } finally {
                dismiss(Helper, User);
                sweep(Helper);
            }
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp538_cureAndMilk")
    public static void cureAndMilk(GameTestHelper Helper) {
        Scp538TestPlayer User = player(Helper, Middle, "scp538_patient", GameType.SURVIVAL);
        try {
            Helper.assertTrue(Scp538Infection.infect(User), "The player could not be infected");
            Scp538Infection.setElapsed(User, Scp538Infection.StageStarts[2] + 10);
            User.removeEffectsCuredBy(EffectCures.MILK);
            Helper.assertTrue(Scp538Infection.isInfected(User), "Milk cured the infection");
            Helper.assertTrue(Scp500Panacea.dose(User, null), "SCP-500 could not be given");
            Helper.assertFalse(Scp538Infection.isInfected(User), "SCP-500 did not cure the infection");
            Helper.assertTrue(Scp538Swarm.canBite(User), "A cured player is still treated as infected");
        } finally {
            dismiss(Helper, User);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp538_infectionStages")
    public static void infectionStages(GameTestHelper Helper) {
        Helper.assertTrue(Scp538Infection.stageOf(0) == 1 && Scp538Infection.stageOf(Scp538Infection.StageStarts[1]) == 2
                && Scp538Infection.stageOf(Scp538Infection.StageStarts[3]) == 4 && Scp538Infection.stageOf(Scp538Infection.Duration) == 4, "Stages do not follow the timeline");
        Scp538TestPlayer User = player(Helper, Middle, "scp538_timeline", GameType.SURVIVAL);
        Helper.startSequence().thenExecute(() -> Helper.assertTrue(Scp538Infection.infect(User), "The player could not be infected"))
                .thenIdle(20).thenExecute(() -> {
                    try {
                        int Elapsed = Scp538Infection.elapsed(User);
                        Helper.assertTrue(Elapsed >= 15 && Elapsed <= 25, "The server clock did not advance the infection: " + Elapsed);
                        Helper.assertTrue(User.isAlive(), "The player died early");
                    } finally {
                        dismiss(Helper, User);
                    }
                }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "scp538_podNeverExpires")
    public static void podNeverExpires(GameTestHelper Helper) {
        ItemStack Pod = TransferPods.loaded(Scp538Registry.PodContent);
        Helper.assertTrue(TransferPods.isLoaded(Pod), "A fresh Transfer Pod is not loaded");
        Helper.assertFalse(Pod.has(StasisRegistry.Contents.get()) || Pod.has(StasisRegistry.Charge.get()), "The Transfer Pod carries a draining stasis field");
        Helper.assertFalse(StasisPodItem.isPod(Pod) || StasisPodItem.isExpired(Pod, Long.MAX_VALUE), "The Transfer Pod can expire like a stasis pod");
        Scp538TestPlayer User = player(Helper, Middle, "scp538_carrier", GameType.SURVIVAL);
        User.getInventory().add(Pod);
        Helper.startSequence().thenIdle(240).thenExecute(() -> {
            try {
                ItemStack Held = User.getInventory().getItem(User.getInventory().findSlotMatchingItem(TransferPods.loaded(Scp538Registry.PodContent)));
                Helper.assertTrue(TransferPods.isLoaded(Held), "The Transfer Pod lost its contents over time");
                Helper.assertTrue(spiders(Helper).isEmpty(), "The Transfer Pod let something out on its own");
            } finally {
                dismiss(Helper, User);
                sweep(Helper);
            }
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp538_podSingleUse")
    public static void podSingleUse(GameTestHelper Helper) {
        lightRoom(Helper, 15);
        Helper.startSequence().thenIdle(10).thenExecute(() -> {
            ServerLevel World = Helper.getLevel();
            Scp538TestPlayer User = player(Helper, Middle.west(3), "scp538_opener", GameType.SURVIVAL);
            try {
                User.setItemInHand(InteractionHand.MAIN_HAND, TransferPods.loaded(Scp538Registry.PodContent));
                BlockPos Floor = Helper.absolutePos(Middle.below());
                BlockHitResult Hit = new BlockHitResult(Vec3.atCenterOf(Floor).add(0.0, 0.5, 0.0), Direction.UP, Floor, false);
                ItemStack Stack = User.getItemInHand(InteractionHand.MAIN_HAND);
                InteractionResult First = Stack.useOn(new UseOnContext(User, InteractionHand.MAIN_HAND, Hit));
                Helper.assertTrue(First.consumesAction(), "Opening the Transfer Pod failed: " + First);
                Helper.assertTrue(spiders(Helper).size() == Scp538Swarm.PodSwarm, "The pod released " + spiders(Helper).size() + " spiders");
                Helper.assertTrue(User.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(), "The opened pod was not destroyed");
                User.setItemInHand(InteractionHand.MAIN_HAND, TransferPods.spent());
                ItemStack After = User.getItemInHand(InteractionHand.MAIN_HAND);
                InteractionResult Second = After.useOn(new UseOnContext(User, InteractionHand.MAIN_HAND, Hit));
                Helper.assertFalse(Second.consumesAction(), "A spent pod opened again");
                Helper.assertTrue(spiders(Helper).size() == Scp538Swarm.PodSwarm, "A spent pod released more spiders");
                DClass Body = Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), Middle.west(2));
                InteractionResult Refill = CommonHooks.onInteractEntity(User, Body, InteractionHand.MAIN_HAND);
                Helper.assertTrue(Refill != null, "Using a spent pod on a mob was not refused");
                Helper.assertFalse(TransferPods.isLoaded(User.getItemInHand(InteractionHand.MAIN_HAND)), "A spent pod was refilled");
                Helper.assertTrue(Body.isAlive() && !Body.isRemoved(), "The refill attempt captured the mob");
                Body.discard();
                ItemStack Floored = TransferPods.loaded(Scp538Registry.PodContent);
                Helper.assertTrue(TransferPods.breakOut(World, Floored, Helper.absoluteVec(Vec3.atBottomCenterOf(Middle.east(3)))), "A destroyed pod did not break open");
                Helper.assertTrue(spiders(Helper).size() == Scp538Swarm.PodSwarm * 2, "A destroyed pod released the wrong number of spiders");
            } finally {
                dismiss(Helper, User);
                sweep(Helper);
            }
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp538_podContentApi")
    public static void podContentApi(GameTestHelper Helper) {
        TransferPodContent Held = TransferPods.content(Scp538Registry.PodContent).orElse(null);
        Helper.assertTrue(Held != null && Held.LabelKey().equals(Scp538Registry.PodLabel), "SCP-538 is not a registered Transfer Pod content");
        Helper.assertTrue(TransferPods.all().contains(Held), "The content list does not include SCP-538");
        ItemStack Pod = TransferPods.loaded(Scp538Registry.PodContent);
        Helper.assertTrue(TransferPods.contentOf(Pod) == Held, "A loaded pod does not resolve to its content");
        Helper.assertTrue(TransferPods.labelOf(Pod).getString().equals(Held.label().getString()), "The pod is not labelled by its content");
        boolean Refused = false;
        try {
            TransferPods.register(new TransferPodContent(Scp538Registry.PodContent, "x", 0, (World, Where, Yaw, User) -> 0));
        } catch (IllegalStateException Problem) {
            Refused = true;
        }
        Helper.assertTrue(Refused, "A second content with the same id was accepted");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp538_strangerBringsPod")
    public static void strangerBringsPod(GameTestHelper Helper) {
        AnomalyDocument Document = AnomalyDocuments.server().get(Scp538Registry.DocumentId);
        Helper.assertTrue(Document != null, "The SCP-538 dossier did not load");
        Helper.assertTrue(Document.Classification() == DocumentClass.Keter, "SCP-538 is not filed as Keter");
        Helper.assertTrue(Document.Condition() instanceof StrangerEncounterCondition, "SCP-538 does not arrive with the stranger");
        StrangerPlan Plan = ((StrangerEncounterCondition) Document.Condition()).plan();
        Helper.assertTrue(Plan.Gifts().size() == 1, "The stranger brings " + Plan.Gifts().size() + " gifts");
        StrangerGift Gift = Plan.Gifts().get(0);
        ItemStack Stack = Gift.toStack();
        Helper.assertTrue(Stack.is(TransferPodRegistry.Pod.get()) && Scp538Registry.PodContent.equals(TransferPods.contentId(Stack)),
                "The stranger's gift is not a Transfer Pod holding SCP-538: " + Stack);
        Helper.assertTrue(AnomalySpawners.spawnerFor(Scp538Registry.DocumentId).isPresent(), "No spawner is registered for SCP-538");
        Helper.assertTrue(spiders(Helper).isEmpty(), "Spiders exist before the pod is opened");
        Helper.succeed();
    }
}
