package net.rai.scp_unleashed.scp469.gametest;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.archive.investigation.StartCondition;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.scp469.Scp469Angel;
import net.rai.scp_unleashed.scp469.Scp469Arrival;
import net.rai.scp_unleashed.scp469.Scp469Embrace;
import net.rai.scp_unleashed.scp469.Scp469Prey;
import net.rai.scp_unleashed.scp469.Scp469Reform;
import net.rai.scp_unleashed.scp469.Scp469Registry;
import net.rai.scp_unleashed.scp469.Scp469SightingCondition;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp469GameTests {
    public static final String Arena = "scp469_arena";
    public static final double Floor = 2.0;
    private static final Logger Log = LogUtils.getLogger();

    private Scp469GameTests() {
    }

    private static final class Mortal extends FakePlayer {
        private Mortal(ServerLevel World, GameProfile Profile) {
            super(World, Profile);
        }

        @Override
        public boolean isInvulnerableTo(DamageSource Source) {
            return false;
        }

        @Override
        public void die(DamageSource Source) {
            if (CommonHooks.onLivingDeath(this, Source)) {
                return;
            }
            dead = true;
        }
    }

    private static String state(Scp469Angel Creature) {
        LivingEntity Target = Creature.getTarget();
        return " [target " + (Target == null ? "none" : Target.getName().getString()) + ", action " + Creature.action() + ", folded " + Creature.isFolded() + ", at "
                + Creature.position() + ", seen " + Creature.lastSeen() + ", " + Creature.foldState() + "]";
    }

    private static Scp469Angel angel(GameTestHelper Helper, double X, double Y, double Z) {
        Scp469Angel Creature = Helper.spawn(Scp469Registry.Angel.get(), new Vec3(X, Y, Z));
        Creature.setYRot(0.0f);
        Creature.setYBodyRot(0.0f);
        return Creature;
    }

    private static Mortal mortal(GameTestHelper Helper, double X, double Y, double Z, String Name) {
        ServerLevel World = Helper.getLevel();
        Mortal User = new Mortal(World, new GameProfile(UUID.randomUUID(), Name));
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, Y, Z));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        return User;
    }

    private static DClass prisoner(GameTestHelper Helper, double X, double Z, boolean Free) {
        if (Free) {
            return Helper.spawn(DClassRegistry.DClassType.get(), new Vec3(X, Floor, Z));
        }
        return Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new Vec3(X, Floor, Z));
    }

    private static void dismiss(GameTestHelper Helper, Player User) {
        Helper.getLevel().removePlayerImmediately((Mortal) User, Entity.RemovalReason.DISCARDED);
    }

    private static void tidy(GameTestHelper Helper) {
        Helper.killAllEntitiesOfClass(Scp469Angel.class);
        Helper.killAllEntitiesOfClass(DClass.class);
    }

    private static double flat(Vec3 From, Vec3 To) {
        double Dx = To.x - From.x;
        double Dz = To.z - From.z;
        return Math.sqrt(Dx * Dx + Dz * Dz);
    }

    private static void fill(GameTestHelper Helper, int X0, int Y0, int Z0, int X1, int Y1, int Z1) {
        for (int X = X0; X <= X1; X++) {
            for (int Y = Y0; Y <= Y1; Y++) {
                for (int Z = Z0; Z <= Z1; Z++) {
                    Helper.setBlock(new BlockPos(X, Y, Z), Blocks.STONE);
                }
            }
        }
    }

    @GameTest(template = Arena, timeoutTicks = 220, batch = "scp469_cannotFly")
    public static void cannotFly(GameTestHelper Helper) {
        fill(Helper, 30, 2, 30, 32, 4, 32);
        Mortal Bait = mortal(Helper, 31.5, 5.0, 31.5, "scp469_bait");
        Scp469Angel Creature = angel(Helper, 14.5, 10.0, 14.5);
        double Start = Creature.getY();
        double Ground = Helper.absoluteVec(new Vec3(0.0, Floor, 0.0)).y;
        double[] Highest = {Creature.getY()};
        double[] Grounded = {Double.NEGATIVE_INFINITY};
        Helper.onEachTick(() -> {
            Highest[0] = Math.max(Highest[0], Creature.getY());
            if (Creature.onGround()) {
                Grounded[0] = Math.max(Grounded[0], Creature.getY());
            }
        });
        Helper.runAfterDelay(200, () -> {
            Helper.assertTrue(Grounded[0] > Double.NEGATIVE_INFINITY, "SCP-469 never landed" + state(Creature));
            Helper.assertTrue(Highest[0] <= Start + 1.0e-3, "SCP-469 rose above the height it was dropped from" + state(Creature));
            Helper.assertTrue(Grounded[0] <= Ground + 1.05, "SCP-469 stood " + (Grounded[0] - Ground) + " blocks above the floor" + state(Creature));
            Helper.assertTrue(Creature.getY() < Bait.getY() - 2.0, "SCP-469 climbed onto the three block platform" + state(Creature));
            Helper.assertTrue(Creature.getTarget() == Bait, "SCP-469 did not hunt the player above it" + state(Creature));
            dismiss(Helper, Bait);
            tidy(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 260, batch = "scp469_targetsPeople")
    public static void targetsPeople(GameTestHelper Helper) {
        Scp469Angel Creature = angel(Helper, 10.5, Floor, 10.5);
        Mortal User = mortal(Helper, 22.5, Floor, 10.5, "scp469_survivor");
        DClass[] Prisoner = new DClass[1];
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Creature.getTarget() == User, "SCP-469 did not target the survival player" + state(Creature)))
                .thenExecute(() -> {
                    Helper.assertTrue(Creature.lastSeen() != null && Creature.remembered() != null, "SCP-469 did not remember where it saw the player" + state(Creature));
                    dismiss(Helper, User);
                    Prisoner[0] = prisoner(Helper, 10.5, 30.5, false);
                })
                .thenWaitUntil(() -> Helper.assertTrue(Creature.getTarget() == Prisoner[0], "SCP-469 did not target the D-Class" + state(Creature)))
                .thenExecute(() -> tidy(Helper))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp469_ignoresCreative")
    public static void ignoresCreativeAndSpectators(GameTestHelper Helper) {
        Scp469Angel Creature = angel(Helper, 16.5, Floor, 16.5);
        Mortal Builder = mortal(Helper, 22.5, Floor, 16.5, "scp469_builder");
        Mortal Ghost = mortal(Helper, 10.5, Floor, 16.5, "scp469_ghost");
        Builder.setGameMode(GameType.CREATIVE);
        Ghost.setGameMode(GameType.SPECTATOR);
        Helper.assertFalse(Scp469Prey.prey(Creature, Builder), "a creative player counts as prey");
        Helper.assertFalse(Scp469Prey.prey(Creature, Ghost), "a spectator counts as prey");
        Helper.runAfterDelay(80, () -> {
            Helper.assertTrue(Creature.getTarget() == null, "SCP-469 targeted a creative or spectator player" + state(Creature));
            Helper.assertTrue(Creature.lastSeen() == null, "SCP-469 remembered a creative or spectator player" + state(Creature));
            dismiss(Helper, Builder);
            dismiss(Helper, Ghost);
            tidy(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp469_sweep")
    public static void sweepDamagesAndShoves(GameTestHelper Helper) {
        Scp469Angel Creature = angel(Helper, 16.5, Floor, 16.5);
        Creature.restrain(200);
        DClass Prisoner = prisoner(Helper, 16.5, 18.6, false);
        float Health = Prisoner.getHealth();
        Helper.runAfterDelay(5, () -> {
            Creature.faceToward(Prisoner);
            Creature.beginSweep();
            Helper.assertTrue(Creature.action() == Scp469Angel.ActionSweep, "the sweep did not start" + state(Creature));
        });
        Helper.runAfterDelay(5 + Scp469Angel.SweepHitTick + 1, () -> {
            Vec3 Push = Prisoner.getDeltaMovement();
            double Shove = Math.sqrt(Push.x * Push.x + Push.z * Push.z);
            Vec3 Away = Prisoner.position().subtract(Creature.position());
            Log.info("SCP-469 sweep shoved the prisoner at {} blocks per tick", Shove);
            Helper.assertTrue(Prisoner.getHealth() < Health, "the sweep did no damage, health " + Prisoner.getHealth() + state(Creature));
            Helper.assertTrue(Shove > 0.6, "the sweep did not shove the prisoner hard, speed " + Shove);
            Helper.assertTrue(Push.x * Away.x + Push.z * Away.z > 0.0, "the sweep pulled the prisoner in instead of shoving it away");
            Helper.assertTrue(Push.y > 0.1, "the sweep did not lift the prisoner off its feet");
            tidy(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 160, batch = "scp469_embraceTimesOut")
    public static void embraceHoldsAndTimesOut(GameTestHelper Helper) {
        Scp469Angel Creature = angel(Helper, 16.5, Floor, 16.5);
        Creature.restrain(400);
        DClass Prisoner = prisoner(Helper, 16.5, 18.0, true);
        Helper.runAfterDelay(2, () -> {
            Creature.faceToward(Prisoner);
            Helper.assertTrue(Creature.canEmbrace(Prisoner), "SCP-469 could not embrace a prisoner in reach" + state(Creature));
            Creature.beginEmbrace(Prisoner);
            Helper.assertTrue(Scp469Embrace.isHeld(Prisoner), "the prisoner is not held");
            Helper.assertTrue(Prisoner.isNoAi(), "the held prisoner still thinks for itself");
            Helper.assertTrue(Creature.embracedId() == Prisoner.getId(), "the embrace is not synced");
        });
        float[] Health = new float[1];
        Helper.runAfterDelay(4, () -> Health[0] = Prisoner.getHealth());
        Helper.runAfterDelay(50, () -> {
            Helper.assertTrue(Scp469Embrace.isHeld(Prisoner) && Creature.isEmbracing(), "the embrace ended early" + state(Creature));
            Helper.assertTrue(Prisoner.getHealth() < Health[0], "the embrace did no damage, health " + Prisoner.getHealth());
            Helper.assertTrue(flat(Creature.holdPoint(), Prisoner.position()) < 0.9, "the prisoner drifted out of the wings");
        });
        Helper.runAfterDelay(2 + Scp469Angel.EmbraceTicks + 6, () -> {
            Helper.assertFalse(Scp469Embrace.isHeld(Prisoner), "the prisoner was never let go");
            Helper.assertFalse(Creature.isEmbracing(), "SCP-469 is still embracing" + state(Creature));
            Helper.assertFalse(Prisoner.isNoAi(), "the released prisoner did not get its own will back");
            Helper.assertTrue(Prisoner.isAlive(), "the embrace killed a healthy prisoner");
            Helper.assertTrue(Creature.embracedId() == -1, "the release is not synced");
            tidy(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp469_embraceBreaks")
    public static void embraceBreaksWhenHit(GameTestHelper Helper) {
        Scp469Angel Creature = angel(Helper, 16.5, Floor, 16.5);
        Creature.restrain(400);
        Mortal User = mortal(Helper, 16.5, Floor, 18.0, "scp469_struggler");
        Helper.runAfterDelay(3, () -> {
            Creature.faceToward(User);
            Creature.beginEmbrace(User);
            Helper.assertTrue(Scp469Embrace.isHeld(User) && Scp469Embrace.locked(User), "the player is not held and slowed");
        });
        Helper.runAfterDelay(20, () -> {
            Helper.assertTrue(Creature.isEmbracing(), "the embrace ended before the player struck" + state(Creature));
            boolean Hit = Creature.hurt(Helper.getLevel().damageSources().playerAttack(User), 4.0f);
            Helper.assertTrue(Hit, "the player's blow did not land");
            Helper.assertFalse(Creature.isEmbracing(), "a blow from the held player did not break the embrace" + state(Creature));
            Helper.assertFalse(Scp469Embrace.isHeld(User) || Scp469Embrace.locked(User), "the player is still held or slowed");
            Helper.assertFalse(Creature.canEmbrace(User), "SCP-469 can embrace again at once");
            dismiss(Helper, User);
            tidy(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 600, batch = "scp469_foldsThroughDoorway")
    public static void foldsThroughDoorway(GameTestHelper Helper) {
        for (int Z = 0; Z < 48; Z++) {
            for (int Y = 2; Y <= 5; Y++) {
                boolean Gap = Z == 24 && Y <= 3;
                if (!Gap) {
                    Helper.setBlock(new BlockPos(24, Y, Z), Blocks.STONE);
                }
            }
        }
        Scp469Angel Creature = angel(Helper, 12.5, Floor, 24.5);
        DClass Prisoner = prisoner(Helper, 36.5, 24.5, false);
        Creature.setTarget(Prisoner);
        Creature.remember(Prisoner.position(), Prisoner.getUUID());
        double Wall = Helper.absoluteVec(new Vec3(24.0, Floor, 0.0)).x;
        boolean[] Folded = {false};
        Helper.onEachTick(() -> Folded[0] |= Creature.isFolded());
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Creature.getX() > Wall + 3.0, "SCP-469 has not squeezed through the doorway" + state(Creature)))
                .thenExecute(() -> Helper.assertTrue(Folded[0], "SCP-469 got through without folding" + state(Creature)))
                .thenWaitUntil(() -> Helper.assertFalse(Creature.isFolded(), "SCP-469 stayed folded on the open side" + state(Creature)))
                .thenExecute(() -> {
                    Helper.assertTrue(Creature.getBbWidth() > 2.0f, "the spread collision box did not come back, width " + Creature.getBbWidth());
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp469_burstsAndReforms")
    public static void burstsAndReforms(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp469Reform Returns = Scp469Reform.get(World);
        Returns.clear();
        Scp469Angel Creature = angel(Helper, 24.5, Floor, 24.5);
        UUID Seen = UUID.randomUUID();
        Creature.remember(Helper.absoluteVec(new Vec3(30.5, Floor, 30.5)), Seen);
        Helper.runAfterDelay(5, () -> {
            Creature.hurt(World.damageSources().magic(), 10000.0f);
            Helper.assertTrue(Creature.isDeadOrDying(), "SCP-469 did not burst at zero health");
        });
        Scp469Angel[] Reformed = new Scp469Angel[1];
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Creature.isRemoved(), "the burst SCP-469 is still in the world"))
                .thenExecute(() -> {
                    List<Scp469Reform.Pending> Waiting = Returns.pending();
                    Helper.assertTrue(Waiting.size() == 1, "bursting scheduled " + Waiting.size() + " returns");
                    Helper.assertTrue(Waiting.get(0).Due() - World.getGameTime() >= Scp469Reform.DelayMin - 20, "SCP-469 is due back too soon");
                    AnomalyDocument Document = AnomalyDocuments.server().get(Scp469Registry.DocumentId);
                    Helper.assertTrue(Document != null && Document.Classification() == DocumentClass.Keter, "the SCP-469 file is no longer Keter");
                    Returns.hurry(World);
                    Returns.process(World, Scp469Reform.Near, 18);
                    Helper.assertTrue(Returns.pending().isEmpty(), "SCP-469 did not reform once due");
                    BlockPos Origin = Helper.absolutePos(new BlockPos(24, 2, 24));
                    List<Scp469Angel> Found = World.getEntitiesOfClass(Scp469Angel.class, new AABB(Origin).inflate(48.0), Scp469Angel::isAlive);
                    Helper.assertTrue(Found.size() == 1, "found " + Found.size() + " reformed SCP-469 nearby");
                    Reformed[0] = Found.get(0);
                    double Away = flat(Vec3.atBottomCenterOf(Origin), Reformed[0].position());
                    Helper.assertTrue(Away >= Scp469Reform.Near - 1.0 && Away <= 19.5, "SCP-469 reformed " + Away + " blocks from where it burst");
                    Helper.assertTrue(Reformed[0].isReforming(), "the reformed SCP-469 did not gather itself first");
                    Helper.assertTrue(Seen.equals(Reformed[0].remembered()) && Reformed[0].lastSeen() != null, "the reformed SCP-469 forgot who it was hunting");
                })
                .thenWaitUntil(() -> Helper.assertFalse(Reformed[0].isReforming(), "SCP-469 never finished reforming"))
                .thenExecute(() -> {
                    Reformed[0].kill();
                    Helper.assertTrue(Returns.pending().isEmpty(), "the kill command scheduled another return");
                    Reformed[0].discard();
                    Returns.clear();
                    tidy(Helper);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp469_persistence")
    public static void persistence(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp469Angel Creature = angel(Helper, 16.5, Floor, 16.5);
        UUID Who = UUID.randomUUID();
        Vec3 Seen = Helper.absoluteVec(new Vec3(30.5, Floor, 12.5));
        Creature.remember(Seen, Who);
        Creature.setFolded(true);
        Helper.assertTrue(Creature.getBbWidth() < 1.0f, "folding did not shrink the collision box");
        CompoundTag Saved = Creature.saveWithoutId(new CompoundTag());
        Scp469Angel Copy = Scp469Registry.Angel.get().create(World);
        Helper.assertTrue(Copy != null, "could not create a second SCP-469");
        Copy.load(Saved);
        Helper.assertTrue(Who.equals(Copy.remembered()), "the hunted person was not saved");
        Helper.assertTrue(Copy.lastSeen() != null && Copy.lastSeen().distanceTo(Seen) < 1.0e-3, "the last sighting was not saved");
        Helper.assertTrue(Copy.isFolded() && Copy.getBbWidth() < 1.0f, "the folded state was not saved");
        Helper.assertTrue(Copy.isPersistenceRequired() && !Copy.removeWhenFarAway(10000.0), "SCP-469 can despawn after loading");
        Copy.discard();
        Scp469Reform Returns = new Scp469Reform();
        Returns.schedule(World, Creature);
        CompoundTag Data = Returns.save(new CompoundTag(), World.registryAccess());
        Scp469Reform Loaded = Scp469Reform.load(Data, World.registryAccess());
        Helper.assertTrue(Loaded.pending().size() == 1, "the pending return was not saved");
        Scp469Reform.Pending Entry = Loaded.pending().get(0);
        Scp469Reform.Pending Original = Returns.pending().get(0);
        Helper.assertTrue(Entry.Origin().equals(Original.Origin()) && Entry.Due() == Original.Due(), "the return lost its place or time");
        Helper.assertTrue(Entry.Who().isPresent() && Entry.Who().get().equals(Who) && Entry.Seen().isPresent(), "the return lost its memory");
        tidy(Helper);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp469_arrival")
    public static void arrivalFindsHiddenGround(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Mortal Reader = mortal(Helper, 24.5, Floor, 10.5, "scp469_reader");
        Reader.setYRot(0.0f);
        Reader.setYHeadRot(0.0f);
        for (int X = 18; X <= 30; X++) {
            for (int Y = 2; Y <= 5; Y++) {
                Helper.setBlock(new BlockPos(X, Y, 14), Blocks.STONE);
            }
        }
        Scp469SightingCondition Condition = new Scp469SightingCondition(0, 0, 9, 16);
        Investigation Case = new Investigation(Scp469Registry.DocumentId, World.getGameTime());
        Condition.begin(Reader, Case);
        Optional<StartCondition.Finding> Found = Condition.tick(Reader, Case);
        Helper.assertTrue(Found.isPresent(), "the sighting found no spot");
        BlockPos Pos = Found.get().Pos();
        Vec3 Bottom = Vec3.atBottomCenterOf(Pos);
        double Away = flat(Reader.position(), Bottom);
        Helper.assertTrue(Away >= 8.0 && Away <= 17.5, "the spot is " + Away + " blocks from the reader");
        Helper.assertTrue(Scp469Reform.fits(World, Pos), "the spot has no room for SCP-469");
        Helper.assertFalse(Scp469Arrival.inView(Reader, Bottom.add(0.0, 1.1, 0.0)) && Scp469Arrival.inSight(World, Reader, Bottom), "the reader can see the spot");
        AnomalyDocument Document = AnomalyDocuments.server().get(Scp469Registry.DocumentId);
        Helper.assertTrue(Document != null, "the SCP-469 file did not load");
        boolean Spawned = Scp469Arrival.manifest(new AnomalySpawners.Discovery(World, Pos, Reader, Scp469Registry.DocumentId, Document, true));
        Helper.assertTrue(Spawned, "SCP-469 did not manifest at the spot");
        List<Scp469Angel> Arrived = World.getEntitiesOfClass(Scp469Angel.class, new AABB(Pos).inflate(2.0));
        Helper.assertTrue(Arrived.size() == 1, "found " + Arrived.size() + " SCP-469 at the spot");
        Helper.assertTrue(Reader.getUUID().equals(Arrived.get(0).remembered()), "SCP-469 did not set out after its reader");
        Helper.assertTrue(Arrived.get(0).isPersistenceRequired(), "the arriving SCP-469 can despawn");
        Arrived.get(0).discard();
        Component Label = Component.literal(Pos.toShortString());
        Log.info("SCP-469 arrival spot {}", Label.getString());
        dismiss(Helper, Reader);
        tidy(Helper);
        Helper.succeed();
    }
}
