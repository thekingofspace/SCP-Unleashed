package net.rai.scp_unleashed.scp372.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.advmaterials.AdvancedMaterialsRecipe;
import net.rai.scp_unleashed.advmaterials.AdvancedMaterialsRegistry;
import net.rai.scp_unleashed.scp372.Scp372AnchorBlock;
import net.rai.scp_unleashed.scp372.Scp372AnchorBlockEntity;
import net.rai.scp_unleashed.scp372.Scp372AnchorLight;
import net.rai.scp_unleashed.scp372.Scp372Anchoring;
import net.rai.scp_unleashed.scp372.Scp372Glimpses;
import net.rai.scp_unleashed.scp372.Scp372Insect;
import net.rai.scp_unleashed.scp372.Scp372Registry;
import net.rai.scp_unleashed.scp372.Scp372State;
import net.rai.scp_unleashed.scp372.Scp372Unlock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp372GameTests {
    public static final String Arena = "scp372_arena";
    public static final BlockPos AnchorPos = new BlockPos(8, 1, 9);
    public static final BlockPos SecondPos = new BlockPos(3, 1, 9);
    public static final Vec3 SqueezeStart = new Vec3(8.5, 2.0, 4.5);
    public static final Vec3 SqueezeEnd = new Vec3(8.5, 3.0, 7.5);
    public static final int SqueezeWall = 5;
    public static final BlockPos SqueezeHole = new BlockPos(8, 2, 5);
    public static final BlockPos SqueezeStep = new BlockPos(8, 2, 7);
    public static final int Settle = 5;
    public static final int HoldWait = Scp372AnchorBlockEntity.ChargeTicks + 8;

    private Scp372GameTests() {
    }

    private static void tidy(GameTestHelper Helper) {
        MinecraftServer Server = Helper.getLevel().getServer();
        if (Scp372State.get(Server).isHeld()) {
            Scp372Anchoring.release(Server);
        }
        Helper.killAllEntitiesOfClass(Scp372Insect.class);
    }

    private static ServerPlayer reader(GameTestHelper Helper, boolean Unlocked) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = new FakePlayer(World, new GameProfile(UUID.randomUUID(), "scp372_test"));
        Vec3 Spot = Helper.absoluteVec(new Vec3(8.5, 1.0, 13.5));
        User.moveTo(Spot.x, Spot.y, Spot.z, 180.0f, 0.0f);
        World.addNewPlayer(User);
        if (Unlocked) {
            Scp372Unlock.unlock(User);
        }
        return User;
    }

    private static void dismiss(GameTestHelper Helper, ServerPlayer User) {
        Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
    }

    private static Scp372AnchorBlockEntity anchor(GameTestHelper Helper, BlockPos Pos) {
        Helper.setBlock(Pos, Scp372Registry.Anchor.get().defaultBlockState().setValue(Scp372AnchorBlock.Facing, Direction.NORTH));
        return entityAt(Helper, Pos);
    }

    private static Scp372AnchorBlockEntity entityAt(GameTestHelper Helper, BlockPos Pos) {
        BlockEntity Found = Helper.getLevel().getBlockEntity(Helper.absolutePos(Pos));
        Helper.assertTrue(Found instanceof Scp372AnchorBlockEntity, "no anchor block entity at " + Pos);
        return (Scp372AnchorBlockEntity) Found;
    }

    private static void feed(GameTestHelper Helper, BlockPos Pos, AtomicBoolean Fed) {
        Helper.onEachTick(() -> {
            if (Fed.get() && Helper.getLevel().getBlockEntity(Helper.absolutePos(Pos)) instanceof Scp372AnchorBlockEntity Device) {
                Device.insertPower(Device.getPowerDemand());
            }
        });
    }

    private static Scp372AnchorLight light(GameTestHelper Helper, BlockPos Pos) {
        BlockState State = Helper.getLevel().getBlockState(Helper.absolutePos(Pos));
        return State.hasProperty(Scp372AnchorBlock.Light) ? State.getValue(Scp372AnchorBlock.Light) : Scp372AnchorLight.Off;
    }

    private static List<Scp372Insect> insects(GameTestHelper Helper) {
        return Helper.getLevel().getEntitiesOfClass(Scp372Insect.class, Helper.getBounds().inflate(4.0), Creature -> Creature.isAlive() && !Creature.isFlickering());
    }

    private static Optional<Scp372Insect> held(GameTestHelper Helper) {
        Optional<Scp372Insect> Found = Scp372Anchoring.creature(Helper.getLevel().getServer());
        return Found.filter(Creature -> Creature.isAlive() && !Creature.isFlickering());
    }

    private static String report(GameTestHelper Helper, BlockPos Pos) {
        Scp372AnchorBlockEntity Device = entityAt(Helper, Pos);
        Scp372State State = Scp372State.get(Helper.getLevel().getServer());
        return " [stage " + Device.stage() + ", light " + light(Helper, Pos) + ", held " + State.isHeld() + ", insects " + insects(Helper).size() + "]";
    }

    private static GameTestSequence anchorOne(GameTestHelper Helper, ServerPlayer User, Scp372Insect[] Body) {
        return Helper.startSequence()
                .thenExecuteAfter(Settle, () -> {
                    Helper.assertTrue(light(Helper, AnchorPos) == Scp372AnchorLight.Powered, "the fed anchor did not light amber" + report(Helper, AnchorPos));
                    Helper.useBlock(AnchorPos, User);
                })
                .thenExecuteAfter(10, () -> Helper.assertTrue(entityAt(Helper, AnchorPos).isCharging(), "right-clicking the powered anchor did not start the focusing sequence"
                        + report(Helper, AnchorPos)))
                .thenExecuteAfter(HoldWait - 10, () -> {
                    Helper.assertTrue(entityAt(Helper, AnchorPos).isHolding(), "the anchor is not holding SCP-372 after the sequence" + report(Helper, AnchorPos));
                    Helper.assertTrue(held(Helper).isPresent(), "no held SCP-372 body exists" + report(Helper, AnchorPos));
                    Body[0] = held(Helper).orElseThrow();
                });
    }

    @GameTest(template = Arena, timeoutTicks = 140, batch = "scp372_poweredAnchorSpawns")
    public static void poweredAnchorSpawns(GameTestHelper Helper) {
        tidy(Helper);
        anchor(Helper, AnchorPos);
        feed(Helper, AnchorPos, new AtomicBoolean(true));
        ServerPlayer User = reader(Helper, true);
        Scp372Insect[] Body = new Scp372Insect[1];
        anchorOne(Helper, User, Body)
                .thenExecute(() -> {
                    Helper.assertTrue(Scp372State.get(Helper.getLevel().getServer()).isHeldBy(Body[0].getUUID()), "the world state does not record the held SCP-372");
                    Helper.assertTrue(entityAt(Helper, AnchorPos).holds(Body[0].getUUID()), "the anchor does not hold the body that appeared");
                    Helper.assertTrue(insects(Helper).size() == 1, "expected exactly one SCP-372 body, found " + insects(Helper).size());
                })
                .thenExecuteAfter(4, () -> {
                    Helper.assertTrue(light(Helper, AnchorPos) == Scp372AnchorLight.Holding, "the anchor lamps did not turn green" + report(Helper, AnchorPos));
                    tidy(Helper);
                    dismiss(Helper, User);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp372_unpoweredAnchorRefuses")
    public static void unpoweredAnchorRefuses(GameTestHelper Helper) {
        tidy(Helper);
        anchor(Helper, AnchorPos);
        ServerPlayer User = reader(Helper, true);
        Helper.startSequence()
                .thenExecuteAfter(Settle, () -> {
                    Helper.assertTrue(light(Helper, AnchorPos) == Scp372AnchorLight.Off, "an unfed anchor lit up" + report(Helper, AnchorPos));
                    Helper.useBlock(AnchorPos, User);
                })
                .thenExecuteAfter(HoldWait, () -> {
                    Helper.assertTrue(entityAt(Helper, AnchorPos).stage() == Scp372AnchorBlockEntity.Stage.Idle, "an unpowered anchor started work" + report(Helper, AnchorPos));
                    Helper.assertTrue(insects(Helper).isEmpty() && !Scp372State.get(Helper.getLevel().getServer()).isHeld(), "an unpowered anchor produced SCP-372" + report(Helper, AnchorPos));
                    dismiss(Helper, User);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp372_lockedPlayerRefused")
    public static void lockedPlayerRefused(GameTestHelper Helper) {
        tidy(Helper);
        anchor(Helper, AnchorPos);
        feed(Helper, AnchorPos, new AtomicBoolean(true));
        ServerPlayer User = reader(Helper, false);
        Helper.startSequence()
                .thenExecuteAfter(Settle, () -> {
                    Helper.assertTrue(!Scp372Unlock.isUnlocked(User), "setup: the stranger is already unlocked");
                    Helper.useBlock(AnchorPos, User);
                })
                .thenExecuteAfter(HoldWait, () -> {
                    Helper.assertTrue(entityAt(Helper, AnchorPos).stage() == Scp372AnchorBlockEntity.Stage.Idle, "a player without the file started the anchor" + report(Helper, AnchorPos));
                    Helper.assertTrue(insects(Helper).isEmpty() && !Scp372State.get(Helper.getLevel().getServer()).isHeld(), "a player without the file produced SCP-372"
                            + report(Helper, AnchorPos));
                    Helper.assertTrue(light(Helper, AnchorPos) == Scp372AnchorLight.Powered, "the refused anchor should stay amber" + report(Helper, AnchorPos));
                    dismiss(Helper, User);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp372_powerLossReleases")
    public static void powerLossReleases(GameTestHelper Helper) {
        tidy(Helper);
        anchor(Helper, AnchorPos);
        AtomicBoolean Fed = new AtomicBoolean(true);
        feed(Helper, AnchorPos, Fed);
        ServerPlayer User = reader(Helper, true);
        Scp372Insect[] Body = new Scp372Insect[1];
        anchorOne(Helper, User, Body)
                .thenExecute(() -> Fed.set(false))
                .thenExecuteAfter(Scp372AnchorBlockEntity.PowerGrace + 6, () -> {
                    Helper.assertTrue(!Scp372State.get(Helper.getLevel().getServer()).isHeld(), "losing power did not release SCP-372" + report(Helper, AnchorPos));
                    Helper.assertTrue(entityAt(Helper, AnchorPos).stage() == Scp372AnchorBlockEntity.Stage.Idle, "the dark anchor still thinks it holds" + report(Helper, AnchorPos));
                    Helper.assertTrue(Body[0].isFlickering() || Body[0].isRemoved(), "the released body did not flicker out");
                })
                .thenExecuteAfter(Scp372Insect.FlickerTicks + 4, () -> {
                    Helper.assertTrue(Body[0].isRemoved(), "the released body is still in the world");
                    Helper.assertTrue(light(Helper, AnchorPos) == Scp372AnchorLight.Off, "the unpowered anchor did not go red" + report(Helper, AnchorPos));
                    tidy(Helper);
                    dismiss(Helper, User);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 160, batch = "scp372_breakingReleases")
    public static void breakingReleases(GameTestHelper Helper) {
        tidy(Helper);
        anchor(Helper, AnchorPos);
        feed(Helper, AnchorPos, new AtomicBoolean(true));
        ServerPlayer User = reader(Helper, true);
        Scp372Insect[] Body = new Scp372Insect[1];
        anchorOne(Helper, User, Body)
                .thenExecute(() -> {
                    Helper.destroyBlock(AnchorPos);
                    Helper.assertTrue(!Scp372State.get(Helper.getLevel().getServer()).isHeld(), "breaking the anchor did not release SCP-372");
                    Helper.assertTrue(Body[0].isFlickering(), "the body did not start to flicker out when its anchor broke");
                })
                .thenExecuteAfter(Scp372Insect.FlickerTicks + 4, () -> {
                    Helper.assertTrue(Body[0].isRemoved(), "the body outlived its broken anchor");
                    tidy(Helper);
                    dismiss(Helper, User);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 160, batch = "scp372_killingReleases")
    public static void killingReleases(GameTestHelper Helper) {
        tidy(Helper);
        anchor(Helper, AnchorPos);
        feed(Helper, AnchorPos, new AtomicBoolean(true));
        ServerPlayer User = reader(Helper, true);
        Scp372Insect[] Body = new Scp372Insect[1];
        anchorOne(Helper, User, Body)
                .thenExecute(() -> Body[0].hurt(Helper.getLevel().damageSources().playerAttack(User), 1000.0f))
                .thenExecuteAfter(2, () -> {
                    Helper.assertTrue(!Scp372State.get(Helper.getLevel().getServer()).isHeld(), "killing the body did not release SCP-372");
                    Helper.assertTrue(entityAt(Helper, AnchorPos).stage() == Scp372AnchorBlockEntity.Stage.Idle, "the anchor still holds a dead body" + report(Helper, AnchorPos));
                })
                .thenExecuteAfter(Scp372Insect.FlickerTicks + 4, () -> {
                    Helper.assertTrue(Body[0].isRemoved(), "the dead body did not flicker away");
                    Helper.assertTrue(light(Helper, AnchorPos) == Scp372AnchorLight.Powered, "the anchor did not drop back to amber" + report(Helper, AnchorPos));
                    tidy(Helper);
                    dismiss(Helper, User);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 220, batch = "scp372_onlyOneAnchored")
    public static void onlyOneAnchored(GameTestHelper Helper) {
        tidy(Helper);
        anchor(Helper, AnchorPos);
        anchor(Helper, SecondPos);
        feed(Helper, AnchorPos, new AtomicBoolean(true));
        feed(Helper, SecondPos, new AtomicBoolean(true));
        ServerPlayer User = reader(Helper, true);
        Scp372Insect[] Body = new Scp372Insect[1];
        anchorOne(Helper, User, Body)
                .thenExecute(() -> Helper.useBlock(SecondPos, User))
                .thenExecuteAfter(HoldWait, () -> {
                    Helper.assertTrue(entityAt(Helper, SecondPos).stage() == Scp372AnchorBlockEntity.Stage.Idle, "a second anchor took hold too" + report(Helper, SecondPos));
                    Helper.assertTrue(insects(Helper).size() == 1, "expected one SCP-372 body with two anchors, found " + insects(Helper).size());
                    Helper.assertTrue(Scp372State.get(Helper.getLevel().getServer()).isHeldAt(Helper.getLevel(), Helper.absolutePos(AnchorPos)), "the world state moved off the first anchor");
                    tidy(Helper);
                    dismiss(Helper, User);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 360, batch = "scp372_staysTethered")
    public static void staysTethered(GameTestHelper Helper) {
        tidy(Helper);
        anchor(Helper, AnchorPos);
        feed(Helper, AnchorPos, new AtomicBoolean(true));
        ServerPlayer User = reader(Helper, true);
        Vec3 Centre = Vec3.atBottomCenterOf(Helper.absolutePos(AnchorPos));
        double[] Worst = {0.0};
        int[] Watched = {0};
        Scp372Insect[] Body = new Scp372Insect[1];
        Helper.onEachTick(() -> {
            if (Body[0] == null || !Body[0].isAlive() || Body[0].isRemoved()) {
                return;
            }
            double Dx = Body[0].getX() - Centre.x;
            double Dz = Body[0].getZ() - Centre.z;
            Worst[0] = Math.max(Worst[0], Math.sqrt(Dx * Dx + Dz * Dz));
            Watched[0]++;
        });
        anchorOne(Helper, User, Body)
                .thenExecuteAfter(80, () -> {
                    Body[0].skitter();
                    Body[0].getNavigation().moveTo(Centre.x + 9.0, Centre.y, Centre.z, 2.0);
                })
                .thenExecuteAfter(70, () -> {
                    Body[0].setDeltaMovement(1.6, 0.0, 0.0);
                    Body[0].hurtMarked = true;
                })
                .thenExecuteAfter(80, () -> {
                    Helper.assertTrue(Watched[0] > 150, "the tether was only watched for " + Watched[0] + " ticks");
                    Helper.assertTrue(Worst[0] <= Scp372Insect.TetherRadius + 1.0E-3, "SCP-372 strayed " + String.format("%.3f", Worst[0]) + " blocks from its anchor");
                    Helper.assertTrue(Scp372State.get(Helper.getLevel().getServer()).isHeld(), "SCP-372 escaped while tethered");
                    tidy(Helper);
                    dismiss(Helper, User);
                })
                .thenSucceed();
    }

    private static Scp372AnchorBlockEntity reload(GameTestHelper Helper, Scp372AnchorBlockEntity Device, Scp372Insect Creature, UUID[] Copy) {
        ServerLevel World = Helper.getLevel();
        BlockPos Absolute = Device.getBlockPos();
        CompoundTag DeviceTag = Device.saveWithFullMetadata(World.registryAccess());
        CompoundTag BodyTag = new CompoundTag();
        Creature.saveWithoutId(BodyTag);
        CompoundTag StateTag = Scp372State.get(World.getServer()).save(new CompoundTag(), World.registryAccess());
        Scp372State Loaded = Scp372State.load(StateTag, World.registryAccess());
        Helper.assertTrue(Loaded.isHeldBy(Creature.getUUID()) && Loaded.isHeldAt(World, Absolute), "the saved world state lost the hold");
        Creature.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        BlockState State = World.getBlockState(Absolute);
        World.removeBlockEntity(Absolute);
        BlockEntity Fresh = BlockEntity.loadStatic(Absolute, State, DeviceTag, World.registryAccess());
        Helper.assertTrue(Fresh instanceof Scp372AnchorBlockEntity, "the saved anchor did not load back");
        Scp372AnchorBlockEntity Restored = (Scp372AnchorBlockEntity) Fresh;
        Helper.assertTrue(Restored.holds(Creature.getUUID()), "the reloaded anchor forgot which body it holds");
        World.setBlockEntity(Restored);
        Scp372Insect Body = Scp372Registry.Insect.get().create(World);
        Helper.assertTrue(Body != null, "could not create the reloaded body");
        Body.load(BodyTag);
        Helper.assertTrue(Absolute.equals(Body.anchor()), "the reloaded body forgot its anchor");
        World.addFreshEntity(Body);
        Copy[0] = Body.getUUID();
        return Restored;
    }

    @GameTest(template = Arena, timeoutTicks = 240, batch = "scp372_stateSurvivesReload")
    public static void stateSurvivesReload(GameTestHelper Helper) {
        tidy(Helper);
        anchor(Helper, AnchorPos);
        feed(Helper, AnchorPos, new AtomicBoolean(true));
        ServerPlayer User = reader(Helper, true);
        UUID[] Copy = new UUID[1];
        Scp372Insect[] Body = new Scp372Insect[1];
        Scp372AnchorBlockEntity[] Restored = new Scp372AnchorBlockEntity[1];
        anchorOne(Helper, User, Body)
                .thenExecute(() -> {
                    Restored[0] = reload(Helper, entityAt(Helper, AnchorPos), Body[0], Copy);
                    Helper.assertTrue(Body[0].getUUID().equals(Copy[0]), "the reloaded body changed identity");
                })
                .thenExecuteAfter(Scp372AnchorBlockEntity.LoadGrace + Scp372AnchorBlockEntity.PowerGrace + 30, () -> {
                    UUID Original = Copy[0];
                    Helper.assertTrue(Restored[0].holds(Original), "the reloaded, powered anchor let go" + report(Helper, AnchorPos));
                    Helper.assertTrue(Scp372State.get(Helper.getLevel().getServer()).isHeldBy(Original), "the world state lost the hold after reloading");
                    Helper.assertTrue(held(Helper).map(Found -> Found.getUUID().equals(Original)).orElse(false), "the reloaded body is not standing held");
                    tidy(Helper);
                    dismiss(Helper, User);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 240, batch = "scp372_unpoweredReloadEscapes")
    public static void unpoweredReloadEscapes(GameTestHelper Helper) {
        tidy(Helper);
        anchor(Helper, AnchorPos);
        AtomicBoolean Fed = new AtomicBoolean(true);
        feed(Helper, AnchorPos, Fed);
        ServerPlayer User = reader(Helper, true);
        UUID[] Copy = new UUID[1];
        Scp372Insect[] Body = new Scp372Insect[1];
        anchorOne(Helper, User, Body)
                .thenExecute(() -> {
                    Fed.set(false);
                    reload(Helper, entityAt(Helper, AnchorPos), Body[0], Copy);
                })
                .thenExecuteAfter(Scp372AnchorBlockEntity.LoadGrace + Scp372AnchorBlockEntity.PowerGrace + 8, () -> {
                    Helper.assertTrue(!Scp372State.get(Helper.getLevel().getServer()).isHeld(), "an anchor that came back without power kept its hold" + report(Helper, AnchorPos));
                    Helper.assertTrue(held(Helper).isEmpty(), "the body stayed held after an unpowered reload");
                    tidy(Helper);
                    dismiss(Helper, User);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 160, batch = "scp372_glimpsesPauseWhileHeld")
    public static void glimpsesPauseWhileHeld(GameTestHelper Helper) {
        tidy(Helper);
        anchor(Helper, AnchorPos);
        feed(Helper, AnchorPos, new AtomicBoolean(true));
        ServerPlayer User = reader(Helper, true);
        MinecraftServer Server = Helper.getLevel().getServer();
        Scp372Insect[] Body = new Scp372Insect[1];
        anchorOne(Helper, User, Body)
                .thenExecute(() -> {
                    long Now = Server.overworld().getGameTime();
                    int Before = Scp372Glimpses.sentTo(User.getUUID());
                    Scp372Glimpses.hurry(User);
                    Helper.assertTrue(Scp372Glimpses.blocked(Server, Now), "glimpses are not blocked while SCP-372 is held");
                    Helper.assertTrue(!Scp372Glimpses.offer(User, Now, Scp372Glimpses.CheckInterval), "a glimpse was offered while SCP-372 is held");
                    Helper.assertTrue(Scp372Glimpses.sweep(Server, Now, Scp372Glimpses.CheckInterval) == 0, "the scheduler sent glimpses while SCP-372 is held");
                    Helper.assertTrue(Scp372Glimpses.sentTo(User.getUUID()) == Before, "a glimpse payload went out while SCP-372 is held");
                    Scp372Anchoring.release(Server);
                    long After = Server.overworld().getGameTime();
                    Scp372Glimpses.hurry(User);
                    Helper.assertTrue(!Scp372Glimpses.offer(User, After, Scp372Glimpses.CheckInterval), "glimpses resumed with no delay at all");
                    long Resumed = Scp372State.get(Server).resumeAt();
                    Helper.assertTrue(Resumed > After && Resumed - After <= Scp372Glimpses.ResumeDelay, "the resume delay is " + (Resumed - After) + " ticks");
                    Scp372Glimpses.hurry(User);
                    Helper.assertTrue(Scp372Glimpses.offer(User, Resumed + 1, Scp372Glimpses.CheckInterval), "glimpses did not resume after the delay");
                    Helper.assertTrue(Scp372Glimpses.sentTo(User.getUUID()) == Before + 1, "the resumed glimpse was not sent");
                })
                .thenExecuteAfter(Scp372Insect.FlickerTicks + 4, () -> {
                    tidy(Helper);
                    dismiss(Helper, User);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp372_squeezesThroughGaps")
    public static void squeezesThroughGaps(GameTestHelper Helper) {
        tidy(Helper);
        for (int X = 0; X < 16; X++) {
            for (int Y = 2; Y <= 4; Y++) {
                BlockPos Pos = new BlockPos(X, Y, SqueezeWall);
                if (!Pos.equals(SqueezeHole)) {
                    Helper.setBlock(Pos, Blocks.STONE);
                }
            }
        }
        Helper.setBlock(SqueezeStep, Blocks.STONE);
        ServerLevel World = Helper.getLevel();
        Scp372Insect Body = Scp372Registry.Insect.get().create(World);
        Helper.assertTrue(Body != null, "could not create SCP-372");
        Vec3 Start = Helper.absoluteVec(SqueezeStart);
        Body.moveTo(Start.x, Start.y, Start.z, 0.0f, 0.0f);
        World.addFreshEntity(Body);
        Helper.assertTrue(Body.getBbWidth() < 1.0f && Body.getBbHeight() < 1.0f, "SCP-372 is " + Body.getBbWidth() + " by " + Body.getBbHeight() + ", too big for a one block hole");
        Vec3 Destination = Helper.absoluteVec(SqueezeEnd);
        BlockPos DestinationPos = BlockPos.containing(Destination);
        Helper.onEachTick(() -> {
            if (Body.isAlive() && (Body.getNavigation().isDone() || !DestinationPos.equals(Body.getNavigation().getTargetPos()))) {
                Body.getNavigation().moveTo(Destination.x, Destination.y, Destination.z, 1.0);
            }
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Body.isAlive() && !Body.isRemoved(), "SCP-372 vanished on the way");
            Helper.assertTrue(Math.abs(Body.getX() - Destination.x) < 0.6 && Math.abs(Body.getZ() - Destination.z) < 0.6 && Body.getY() > Destination.y - 0.01 && Body.onGround(),
                    "SCP-372 did not crawl through the hole and up the step, it is at " + Body.position().subtract(Helper.absoluteVec(Vec3.ZERO)));
            Body.discard();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp372_recipeAtBench")
    public static void recipeAtBench(GameTestHelper Helper) {
        ResourceLocation Id = Scp372Registry.location("advanced_materials/" + Scp372Registry.AnchorId);
        Optional<RecipeHolder<?>> Found = Helper.getLevel().getRecipeManager().byKey(Id);
        Helper.assertTrue(Found.isPresent(), "there is no recipe " + Id);
        Helper.assertTrue(Found.get().value().getType() == AdvancedMaterialsRegistry.RecipeKind.get(), "the anchor recipe is not an Advanced Materials Bench recipe");
        Helper.assertTrue(Found.get().value() instanceof AdvancedMaterialsRecipe Recipe && Recipe.getResult().is(Scp372Registry.AnchorItem.get()),
                "the anchor recipe does not make the anchor");
        AdvancedMaterialsRecipe Recipe = (AdvancedMaterialsRecipe) Found.get().value();
        Helper.assertTrue(Recipe.getInputs().stream().anyMatch(Input -> Input.ingredient().test(new ItemStack(AdvancedMaterialsRegistry.MagnetizedSteel.get()))),
                "the anchor recipe does not use magnetized blocks");
        Helper.succeed();
    }
}
