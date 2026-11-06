package net.rai.scp_unleashed.scp2317.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.gateway.gametest.GatewayGameTests;
import net.rai.scp_unleashed.scp001scarlet.ScarletConfig;
import net.rai.scp_unleashed.scp001scarlet.core.ScarletState;
import net.rai.scp_unleashed.scp2317.Scp2317Builder;
import net.rai.scp_unleashed.scp2317.Scp2317Chains;
import net.rai.scp_unleashed.scp2317.Scp2317Config;
import net.rai.scp_unleashed.scp2317.Scp2317DoorBlock;
import net.rai.scp_unleashed.scp2317.Scp2317DoorBlockEntity;
import net.rai.scp_unleashed.scp2317.Scp2317KingBlockEntity;
import net.rai.scp_unleashed.scp2317.Scp2317Plan;
import net.rai.scp_unleashed.scp2317.Scp2317Registry;
import net.rai.scp_unleashed.scp2317.Scp2317Ritual;
import net.rai.scp_unleashed.scp2317.Scp2317RitualGuide;
import net.rai.scp_unleashed.scp2317.Scp2317Travel;
import net.rai.scp_unleashed.scp2317.Scp2317World;

import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp2317GameTests {
    public static final String Arena = GatewayGameTests.Arena;
    public static final String StateName = "scp_unleashed_scarlet";

    private Scp2317GameTests() {
    }

    private static ScarletState swap(GameTestHelper Helper, boolean Read, ScarletState.Phase Phase, float Manifestation) {
        MinecraftServer Server = Helper.getLevel().getServer();
        ScarletState Before = ScarletState.get(Server);
        ScarletState Fresh = new ScarletState();
        if (Read) {
            Fresh.markRead();
        }
        Fresh.phase(Phase, Server.overworld().getGameTime());
        Fresh.manifestation(Manifestation);
        Server.overworld().getDataStorage().set(StateName, Fresh);
        GatewayGameTests.whenDone(Helper, () -> Server.overworld().getDataStorage().set(StateName, Before));
        return Fresh;
    }

    private static void standIn(GameTestHelper Helper) {
        Scp2317World.standIn(Level.END);
        GatewayGameTests.whenDone(Helper, () -> {
            Scp2317World.standIn(null);
            Scp2317Ritual.forgetAll();
        });
    }

    private static Scp2317DoorBlockEntity door(GameTestHelper Helper, BlockPos Local) {
        BlockPos Master = Helper.absolutePos(Local);
        Scp2317DoorBlock.placeWhole(Helper.getLevel(), Master, Direction.SOUTH, Scp2317Registry.Door.get(), Block.UPDATE_ALL);
        Helper.assertTrue(Helper.getLevel().getBlockEntity(Master) instanceof Scp2317DoorBlockEntity, "the door has no block entity");
        return (Scp2317DoorBlockEntity) Helper.getLevel().getBlockEntity(Master);
    }

    private static FakePlayer visitor(ServerLevel World, Vec3 Spot, String Name, GameTestHelper Helper) {
        FakePlayer User = new FakePlayer(World, new GameProfile(UUID.randomUUID(), Name));
        World.getChunkAt(BlockPos.containing(Spot));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        User.setGameMode(GameType.SURVIVAL);
        GatewayGameTests.whenDone(Helper, () -> {
            if (!User.isRemoved()) {
                User.serverLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
            }
        });
        return User;
    }

    private static ServerLevel end(GameTestHelper Helper) {
        ServerLevel End = Helper.getLevel().getServer().getLevel(Level.END);
        Helper.assertTrue(End != null, "the End is missing");
        return End;
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp2317_door_lock")
    public static void doorLockedUntilRead(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            ScarletState State = swap(Helper, false, ScarletState.Phase.Asleep, 0.0f);
            Scp2317DoorBlockEntity Frame = door(Helper, new BlockPos(3, 2, 3));
            FakePlayer User = GatewayGameTests.person(Helper, 4.5, 6.5, "scp2317_reader");
            Frame.use(User);
            Helper.assertTrue(!Frame.opened(), "the door opened before the SCP-001 file was read");
            State.markRead();
            Frame.use(User);
            Helper.assertTrue(Frame.opened(), "the door stayed shut after the file was read");
            Helper.assertTrue(Helper.getLevel().getBlockState(Frame.getBlockPos()).getValue(Scp2317DoorBlock.Open), "the open state did not reach the blocks");
            Frame.use(User);
            Helper.assertTrue(!Frame.opened(), "the door did not close again");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp2317_door_seal")
    public static void doorUnbreakableWhileOccupied(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            standIn(Helper);
            Scp2317DoorBlockEntity Frame = door(Helper, new BlockPos(3, 2, 3));
            BlockPos Cell = Frame.getBlockPos().above();
            BlockState State = Helper.getLevel().getBlockState(Cell);
            FakePlayer Miner = GatewayGameTests.person(Helper, 4.5, 6.5, "scp2317_miner");
            Helper.assertTrue(!Frame.sealed(), "the door is sealed with nobody inside");
            Helper.assertTrue(State.getDestroyProgress(Miner, Helper.getLevel(), Cell) > 0.0f, "the door cannot be broken with nobody inside");
            FakePlayer Inside = visitor(end(Helper), Scp2317World.arrival(0), "scp2317_inside", Helper);
            Helper.assertTrue(Scp2317World.occupied(Helper.getLevel().getServer()), "the stand-in desert does not count its visitor");
            Helper.assertTrue(Frame.sealed(), "the door is not sealed while someone is inside");
            Helper.assertTrue(State.getDestroyProgress(Miner, Helper.getLevel(), Cell) == 0.0f, "the door can be mined while someone is inside");
            Helper.assertTrue(State.getBlock().getExplosionResistance(State, Helper.getLevel(), Cell, null) >= Scp2317Registry.SealedResistance, "explosions can break the door while someone is inside");
            Inside.serverLevel().removePlayerImmediately(Inside, Entity.RemovalReason.DISCARDED);
            Helper.assertTrue(!Frame.sealed(), "the door stays sealed after the visitor left");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp2317_offering")
    public static void offeringMustFallNearTheVase(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            standIn(Helper);
            swap(Helper, true, ScarletState.Phase.Stirring, 0.5f);
            ServerLevel End = end(Helper);
            Scp2317Builder.build(End);
            Scp2317Ritual.resetCooldown(Helper.getLevel().getServer());
            FakePlayer User = visitor(End, Scp2317World.vase().add(2.0, -1.5, 0.0), "scp2317_priest", Helper);
            Chicken Far = chicken(End, Scp2317World.vase().add(18.0, -1.5, 0.0), Helper);
            Far.hurt(End.damageSources().playerAttack(User), 100.0f);
            Helper.assertTrue(Far.isDeadOrDying(), "the far chicken survived");
            Helper.assertTrue(Scp2317Ritual.attempt(User.getUUID()) == null, "a chicken killed far from the vase armed the rite");
            Chicken Near = chicken(End, Scp2317World.vase().add(1.5, -1.5, 0.5), Helper);
            Near.hurt(End.damageSources().playerAttack(User), 100.0f);
            Helper.assertTrue(Scp2317Ritual.attempt(User.getUUID()) != null, "a chicken killed beside the vase did not arm the rite");
            Helper.assertTrue(End.getBlockState(Scp2317World.vasePos()).is(Scp2317Registry.Vase.get()), "the vase was not built");
            Helper.succeed();
        });
    }

    private static Chicken chicken(ServerLevel World, Vec3 Spot, GameTestHelper Helper) {
        Chicken Made = EntityType.CHICKEN.create(World);
        Helper.assertTrue(Made != null, "no chicken");
        Made.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addFreshEntity(Made);
        GatewayGameTests.whenDone(Helper, () -> {
            if (!Made.isRemoved()) {
                Made.discard();
            }
        });
        return Made;
    }

    private static Scp2317Ritual.Outcome walk(FakePlayer User, double From, double To, double Radius) {
        Vec3 Centre = Scp2317World.centre();
        Scp2317Ritual.Outcome Last = Scp2317Ritual.Outcome.None;
        int Steps = Math.max(1, (int) Math.ceil(Math.abs(To - From) / 8.0));
        for (int Step = 0; Step <= Steps; Step++) {
            double Angle = Math.toRadians(From + (To - From) * Step / Steps);
            Vec3 Spot = new Vec3(Centre.x + Math.cos(Angle) * Radius, Centre.y, Centre.z + Math.sin(Angle) * Radius);
            Scp2317Ritual.Outcome Now = Scp2317Ritual.observe(User, Spot);
            if (Now != Scp2317Ritual.Outcome.None) {
                Last = Now;
            }
            if (Now == Scp2317Ritual.Outcome.Done || Now == Scp2317Ritual.Outcome.Failed) {
                return Now;
            }
        }
        return Last;
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp2317_rite")
    public static void clockwiseThenCounterLowersManifestation(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            standIn(Helper);
            MinecraftServer Server = Helper.getLevel().getServer();
            ScarletState State = swap(Helper, true, ScarletState.Phase.Stirring, 0.5f);
            ServerLevel End = end(Helper);
            Scp2317Ritual.resetCooldown(Server);
            FakePlayer User = visitor(End, Scp2317World.centre().add(16.0, 0.0, 0.0), "scp2317_walker", Helper);
            Helper.assertTrue(Scp2317Ritual.offer(User, End) == Scp2317Ritual.Outcome.Armed, "the rite was not armed");
            Helper.assertTrue(walk(User, 0.0, 370.0, 16.0) == Scp2317Ritual.Outcome.Turned, "a clockwise lap did not complete the first circle");
            Helper.assertTrue(walk(User, 370.0, -12.0, 16.0) == Scp2317Ritual.Outcome.Done, "the counter-clockwise lap did not finish the rite");
            float Expected = 0.5f - ScarletConfig.rite2317Drop();
            Helper.assertTrue(Math.abs(State.manifestation() - Expected) < 1.0E-3f, "manifestation is " + State.manifestation() + ", expected " + Expected);
            Helper.assertTrue(Scp2317Ritual.cooling(Server), "the rite did not start its rest");
            Helper.assertTrue(Scp2317Ritual.offer(User, End) == Scp2317Ritual.Outcome.Refused, "the vase accepted an offering during the rest");
            Helper.assertTrue(Math.abs(State.manifestation() - Expected) < 1.0E-3f, "the refused offering changed manifestation");
            Scp2317Ritual.resetCooldown(Server);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp2317_rite_wrong")
    public static void wrongOrderOrTimeoutDoesNothing(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            standIn(Helper);
            MinecraftServer Server = Helper.getLevel().getServer();
            ScarletState State = swap(Helper, true, ScarletState.Phase.Stirring, 0.5f);
            ServerLevel End = end(Helper);
            Scp2317Ritual.resetCooldown(Server);
            FakePlayer User = visitor(End, Scp2317World.centre().add(16.0, 0.0, 0.0), "scp2317_stray", Helper);
            Helper.assertTrue(Scp2317Ritual.offer(User, End) == Scp2317Ritual.Outcome.Armed, "the rite was not armed");
            Helper.assertTrue(walk(User, 0.0, -370.0, 16.0) == Scp2317Ritual.Outcome.Failed, "walking counter-clockwise first did not fail the rite");
            Helper.assertTrue(Scp2317Ritual.attempt(User.getUUID()) == null, "the failed rite stayed armed");
            Helper.assertTrue(Scp2317Ritual.offer(User, End) == Scp2317Ritual.Outcome.Armed, "the rite could not be armed again");
            Scp2317Ritual.Attempt Current = Scp2317Ritual.attempt(User.getUUID());
            Helper.assertTrue(Current != null, "no attempt");
            Current.ArmedAt -= Scp2317Config.window() + 5;
            Helper.assertTrue(walk(User, 0.0, 40.0, 16.0) == Scp2317Ritual.Outcome.Failed, "a late walk did not time out");
            Helper.assertTrue(Scp2317Ritual.offer(User, End) == Scp2317Ritual.Outcome.Armed, "the rite could not be armed a third time");
            Helper.assertTrue(walk(User, 0.0, 200.0, 16.0) == Scp2317Ritual.Outcome.Turned || Scp2317Ritual.attempt(User.getUUID()) != null, "the attempt vanished");
            Helper.assertTrue(walk(User, 200.0, 200.0, 50.0) == Scp2317Ritual.Outcome.Failed, "leaving the circle did not fail the rite");
            Helper.assertTrue(Math.abs(State.manifestation() - 0.5f) < 1.0E-4f, "a failed rite changed manifestation");
            Helper.assertTrue(!Scp2317Ritual.cooling(Server), "a failed rite started the rest");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp2317_chains")
    public static void chainsFollowBrokenAndNeverRepair(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            ScarletState State = swap(Helper, true, ScarletState.Phase.Asleep, 0.0f);
            BlockPos Pos = Helper.absolutePos(new BlockPos(4, 2, 4));
            Helper.getLevel().setBlock(Pos, Scp2317Registry.King.get().defaultBlockState(), Block.UPDATE_ALL);
            Helper.assertTrue(Helper.getLevel().getBlockEntity(Pos) instanceof Scp2317KingBlockEntity, "the sleeper has no block entity");
            Scp2317KingBlockEntity King = (Scp2317KingBlockEntity) Helper.getLevel().getBlockEntity(Pos);
            King.sync(State.broken());
            Helper.assertTrue(King.broken() == 0, "chains broke at zero manifestation");
            State.manifestation(3.0f / Scp2317Chains.count() + 0.001f);
            King.sync(State.broken());
            Helper.assertTrue(King.broken() == 3, "three sevenths did not break three chains: " + King.broken());
            State.manifestation(0.0f);
            King.sync(State.broken());
            Helper.assertTrue(King.broken() == 3, "lowering manifestation repaired a chain");
            King.sync(1);
            Helper.assertTrue(King.broken() == 3, "the sleeper accepted a lower chain count");
            CompoundTag Saved = King.saveWithoutMetadata(Helper.getLevel().registryAccess());
            Helper.assertTrue(Saved.getInt("Broken") == 3, "the broken count is not saved");
            State.manifestation(1.0f);
            King.sync(State.broken());
            Helper.assertTrue(King.broken() == Scp2317Chains.count(), "a full meter did not break every chain");
            Helper.assertTrue(Scp2317Chains.count() == 7 && Scp2317Chains.Pillar.length == 7, "there are not seven chains to seven pillars");
            boolean[] Used = new boolean[7];
            for (int Pillar : Scp2317Chains.Pillar) {
                Helper.assertTrue(!Used[Pillar], "two chains share pillar " + Pillar);
                Used[Pillar] = true;
            }
            Helper.getLevel().setBlock(Pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp2317_rite_guide")
    public static void guideLightsPillarsAndFlipsAtTheTurn(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            standIn(Helper);
            MinecraftServer Server = Helper.getLevel().getServer();
            swap(Helper, true, ScarletState.Phase.Stirring, 0.5f);
            ServerLevel End = end(Helper);
            Scp2317Ritual.resetCooldown(Server);
            FakePlayer User = visitor(End, Scp2317World.centre().add(16.0, 0.0, 0.0), "scp2317_guided", Helper);
            Helper.assertTrue(Scp2317Ritual.offer(User, End) == Scp2317Ritual.Outcome.Armed, "the rite was not armed");
            Scp2317Ritual.Attempt Current = Scp2317Ritual.attempt(User.getUUID());
            Helper.assertTrue(Current != null && Scp2317RitualGuide.litCount(Current) == 0, "pillars were lit before walking");
            Scp2317RitualGuide.show(User, Current);
            walk(User, 0.0, 100.0, 16.0);
            Helper.assertTrue(Scp2317RitualGuide.litCount(Current) == 2, "a hundred degrees clockwise lit " + Scp2317RitualGuide.litCount(Current) + " pillars, expected 2");
            Helper.assertTrue(walk(User, 100.0, 370.0, 16.0) == Scp2317Ritual.Outcome.Turned, "the first lap did not turn");
            Helper.assertTrue(Current.Stage == 1 && Scp2317RitualGuide.way(Current.Stage) == -1, "the guide did not reverse");
            Helper.assertTrue(Scp2317RitualGuide.litCount(Current) == 0, "pillars stayed lit after the turn");
            walk(User, 370.0, 300.0, 16.0);
            Helper.assertTrue(Scp2317RitualGuide.litCount(Current) == 1, "seventy degrees back lit " + Scp2317RitualGuide.litCount(Current) + " pillars, expected 1");
            Scp2317RitualGuide.show(User, Current);
            Scp2317Ritual.forget(User.getUUID());
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp2317_chain_floor")
    public static void chainsStayAboveTheFloorAndOutsideThePillars(GameTestHelper Helper) {
        for (int Index = 0; Index < Scp2317Chains.count(); Index++) {
            Vec3 Body = Scp2317Chains.attach(Index);
            Vec3 Anchor = Scp2317Chains.anchor(Index);
            Helper.assertTrue(Body.y > 0.5, "chain " + Scp2317Chains.Order[Index] + " is fixed below the floor at " + Body.y);
            Helper.assertTrue(Anchor.y > 0.5, "chain " + Scp2317Chains.Order[Index] + " meets its pillar below the floor at " + Anchor.y);
            double Low = Math.min(Body.y, Anchor.y);
            Helper.assertTrue((Body.y + Anchor.y) / 2.0 - 0.06 * Body.distanceTo(Anchor) > 0.5, "chain " + Scp2317Chains.Order[Index] + " sags into the floor from " + Low);
            int[] Centre = Scp2317Plan.pillarCentre(Scp2317Chains.Pillar[Index], Scp2317Plan.CavePillarRadius);
            double Out = Math.max(Math.abs(Anchor.x - Centre[0] - 0.5), Math.abs(Anchor.z - Centre[1] - 0.5));
            Helper.assertTrue(Out >= Scp2317Chains.PillarHalf, "chain " + Scp2317Chains.Order[Index] + " is anchored inside its pillar");
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp2317_plan")
    public static void ringAndVaseGenerateDeterministically(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            for (int X = -40; X <= 40; X += 7) {
                for (int Z = -40; Z <= 40; Z += 7) {
                    Helper.assertTrue(Scp2317Plan.surface(X, Z) == Scp2317Plan.surface(X, Z), "the surface is not deterministic");
                    for (int Y = 0; Y < Scp2317Plan.Height; Y += 5) {
                        Helper.assertTrue(Scp2317Plan.state(X, Y, Z) == Scp2317Plan.state(X, Y, Z), "the plan is not deterministic at " + X + " " + Y + " " + Z);
                    }
                }
            }
            for (int Index = 0; Index < Scp2317Plan.Pillars; Index++) {
                int[] Centre = Scp2317Plan.pillarCentre(Index, Scp2317Plan.RingRadius);
                double Radius = Math.sqrt(Centre[0] * Centre[0] + Centre[1] * Centre[1]);
                Helper.assertTrue(Radius > 10.0 && Radius < 12.0, "pillar " + Index + " is off the ring");
                Helper.assertTrue(Scp2317Plan.ring(Centre[0], Scp2317Plan.Ground + 3, Centre[1]) != null, "pillar " + Index + " is missing");
                int[] Below = Scp2317Plan.pillarCentre(Index, Scp2317Plan.CavePillarRadius);
                Helper.assertTrue(!Scp2317Plan.state(Below[0], Scp2317Plan.CaveFloor + 5, Below[1]).isAir(), "cave pillar " + Index + " is missing");
            }
            Helper.assertTrue(Scp2317Plan.state(0, Scp2317Plan.CaveFloor + 6, 0).isAir(), "the cave is not open above the sleeper");
            Helper.assertTrue(!Scp2317Plan.state(0, Scp2317Plan.CaveFloor, 0).isAir(), "the cave has no floor");
            Helper.assertTrue(!Scp2317Plan.state(0, Scp2317Plan.Ground, 0).isAir(), "the Ring has no ground");
            Helper.assertTrue(Scp2317Plan.surface(0, Scp2317Plan.DoorZ) == Scp2317Plan.Ground, "the ground at the return door is not flat");
            standIn(Helper);
            ServerLevel End = end(Helper);
            Scp2317Builder.build(End);
            Helper.assertTrue(End.getBlockState(Scp2317World.vasePos()).is(Scp2317Registry.Vase.get()), "the vase is not at the centre of the Ring");
            Helper.assertTrue(End.getBlockState(Scp2317World.kingPos()).is(Scp2317Registry.King.get()), "the sleeper is not in the cave");
            Helper.assertTrue(End.getBlockState(Scp2317World.farDoor()).is(Scp2317Registry.FarDoor.get()), "the return door is missing");
            int[] First = Scp2317Plan.pillarCentre(0, Scp2317Plan.RingRadius);
            Helper.assertTrue(!End.getBlockState(Scp2317World.at(First[0], Scp2317Plan.Ground + 3, First[1])).isAir(), "the first pillar was not built");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp2317_travel")
    public static void doorLeadsToTheDesertAndBack(GameTestHelper Helper) {
        GatewayGameTests.guard(Helper, () -> {
            standIn(Helper);
            swap(Helper, true, ScarletState.Phase.Stirring, 0.1f);
            Scp2317DoorBlockEntity Frame = door(Helper, new BlockPos(3, 2, 3));
            FakePlayer User = GatewayGameTests.person(Helper, 4.5, 6.5, "scp2317_traveller");
            Vec3 Home = User.position();
            Helper.assertTrue(Scp2317Travel.enter(User, Frame, Home), "the door refused to take the player");
            Helper.assertTrue(Scp2317World.inside(User), "the player did not arrive in the desert");
            Helper.assertTrue(User.position().distanceTo(Scp2317World.arrival(0)) < 8.0, "the player arrived far from the return door: " + User.position());
            CompoundTag Persisted = User.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
            Persisted.remove(Scp2317Travel.PassedKey);
            User.getPersistentData().put(Player.PERSISTED_NBT_TAG, Persisted);
            Helper.assertTrue(Scp2317Travel.leave(User), "the player could not leave");
            Helper.assertTrue(User.level().dimension() == Helper.getLevel().dimension(), "the player did not come home");
            Helper.assertTrue(User.position().distanceTo(Vec3.atCenterOf(Frame.getBlockPos())) < 6.0, "the player came back far from the door: " + User.position());
            Helper.succeed();
        });
    }
}
