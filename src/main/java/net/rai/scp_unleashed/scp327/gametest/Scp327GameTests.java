package net.rai.scp_unleashed.scp327.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.antimemetic.AntiMemeticRegistry;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.archive.investigation.StartCondition;
import net.rai.scp_unleashed.control.ControlManager;
import net.rai.scp_unleashed.control.ControlSessions;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.scp148.Scp148Registry;
import net.rai.scp_unleashed.scp327.Scp327Hold;
import net.rai.scp_unleashed.scp327.Scp327Lure;
import net.rai.scp_unleashed.scp327.Scp327Registry;
import net.rai.scp_unleashed.scp327.Scp327Shoal;
import net.rai.scp_unleashed.scp327.Scp327ShoalCondition;
import net.rai.scp_unleashed.scp327.Scp327Siren;
import net.rai.scp_unleashed.scp327.Scp327Waters;
import net.rai.scp_unleashed.senses.SensesRegistry;
import net.rai.scp_unleashed.stasis.StasisPodItem;
import net.rai.scp_unleashed.stasis.StasisPods;
import net.rai.scp_unleashed.stasis.StasisRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp327GameTests {
    public static final String Arena = "scp327_arena";

    private Scp327GameTests() {
    }

    private static void fill(GameTestHelper Helper, int X0, int Y0, int Z0, int X1, int Y1, int Z1, BlockState State) {
        for (int X = Math.min(X0, X1); X <= Math.max(X0, X1); X++) {
            for (int Y = Math.min(Y0, Y1); Y <= Math.max(Y0, Y1); Y++) {
                for (int Z = Math.min(Z0, Z1); Z <= Math.max(Z0, Z1); Z++) {
                    Helper.setBlock(X, Y, Z, State);
                }
            }
        }
    }

    private static void pool(GameTestHelper Helper, int X0, int Z0, int X1, int Z1, int Depth, int Rim) {
        fill(Helper, X0 - 1, 1, Z0 - 1, X1 + 1, 1, Z1 + 1, Blocks.STONE.defaultBlockState());
        fill(Helper, X0 - 1, 2, Z0 - 1, X1 + 1, 1 + Depth + Rim, Z1 + 1, Blocks.STONE.defaultBlockState());
        fill(Helper, X0, 2, Z0, X1, 1 + Depth, Z1, Blocks.WATER.defaultBlockState());
    }

    private static int surface(int Depth) {
        return 1 + Depth;
    }

    private static Scp327Siren siren(GameTestHelper Helper, double X, double Y, double Z) {
        Scp327Siren Creature = Helper.spawn(Scp327Registry.Siren.get(), new Vec3(X, Y, Z));
        Creature.setYRot(0.0f);
        return Creature;
    }

    private static ServerPlayer survivor(GameTestHelper Helper, double X, double Y, double Z) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = new Survivor(World, new GameProfile(UUID.randomUUID(), "scp327_test"));
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, Y, Z));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        User.setGameMode(GameType.SURVIVAL);
        World.addNewPlayer(User);
        return User;
    }

    private static ServerPlayer mortal(GameTestHelper Helper, double X, double Y, double Z) {
        ServerLevel World = Helper.getLevel();
        Mortal User = new Mortal(World, new GameProfile(UUID.randomUUID(), "scp327_operator"));
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, Y, Z));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        User.setGameMode(GameType.SURVIVAL);
        World.addNewPlayer(User);
        return User;
    }

    private static class Survivor extends FakePlayer {
        private Survivor(ServerLevel World, GameProfile Profile) {
            super(World, Profile);
        }

        @Override
        public boolean isFakePlayer() {
            return false;
        }
    }

    private static final class Mortal extends Survivor {
        private Mortal(ServerLevel World, GameProfile Profile) {
            super(World, Profile);
            ObfuscationReflectionHelper.setPrivateValue(ServerPlayer.class, this, 0, "spawnInvulnerableTime");
        }

        @Override
        public boolean isInvulnerableTo(DamageSource Source) {
            return false;
        }

        @Override
        public void tick() {
            if (invulnerableTime > 0) {
                invulnerableTime--;
            }
        }

        @Override
        public void die(DamageSource Source) {
            if (CommonHooks.onLivingDeath(this, Source)) {
                return;
            }
            dead = true;
        }
    }

    private static DClass prisoner(GameTestHelper Helper, double X, double Y, double Z) {
        return Helper.spawnWithNoFreeWill(DClassRegistry.DClassType.get(), new Vec3(X, Y, Z));
    }

    private static void dismiss(GameTestHelper Helper, ServerPlayer User) {
        Scp327Hold.forget(User.getUUID());
        Scp327Lure.forget(User);
        Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
    }

    private static void tidy(GameTestHelper Helper) {
        Helper.killAllEntitiesOfClass(Scp327Siren.class);
        Helper.killAllEntitiesOfClass(DClass.class);
    }

    private static String state(Scp327Siren Creature) {
        return " [mode " + Creature.mode() + ", stage " + Creature.stage() + ", target " + (Creature.chosen() == null ? "none" : Creature.chosen().getName().getString())
                + ", at " + Creature.position() + ", in water " + Creature.isInWater() + ", moisture " + Creature.moisture() + ", health " + Creature.getHealth()
                + ", kills " + Creature.kills() + ", beach " + Creature.beachSpot() + "]";
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp327_shoalFindsWater")
    public static void shoalFindsWater(GameTestHelper Helper) {
        pool(Helper, 20, 20, 31, 31, 3, 0);
        fill(Helper, 2, 1, 2, 8, 1, 8, Blocks.STONE.defaultBlockState());
        ServerPlayer Reader = survivor(Helper, 4.5, 2.0, 4.5);
        ServerLevel World = Helper.getLevel();
        Optional<Scp327Waters.Body> Water = Scp327Shoal.findWater(World, Reader.position(), 8, 34, World.getRandom());
        Helper.assertTrue(Water.isPresent(), "the shoal search found no water near the reader");
        AnomalyDocument Document = AnomalyDocuments.server().get(Scp327Registry.DocumentId);
        boolean Spawned;
        if (Document != null) {
            Spawned = Scp327Shoal.manifest(new AnomalySpawners.Discovery(World, Water.get().Centre(), Reader, Scp327Registry.DocumentId, Document, true));
        } else {
            Spawned = !Scp327Shoal.spawn(World, Water.get(), Scp327Shoal.ShoalSize, Reader.position(), World.getRandom()).isEmpty();
        }
        Helper.assertTrue(Spawned, "the shoal did not manifest");
        Helper.runAfterDelay(5, () -> {
            List<Scp327Siren> Found = World.getEntitiesOfClass(Scp327Siren.class, new AABB(Helper.absolutePos(BlockPos.ZERO)).inflate(48.0), Scp327Siren::isAlive);
            Helper.assertTrue(Found.size() == Scp327Shoal.ShoalSize, "expected 3 instances, found " + Found.size());
            for (Scp327Siren Creature : Found) {
                Helper.assertTrue(Scp327Waters.isWater(World, Creature.blockPosition()), "an instance was placed out of the water" + state(Creature));
                Helper.assertTrue(Creature.chosen() == Reader, "an instance is not hunting the reader" + state(Creature));
            }
            dismiss(Helper, Reader);
            tidy(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 160, batch = "scp327_shoalWaitsForWater")
    public static void shoalWaitsForWater(GameTestHelper Helper) {
        fill(Helper, 2, 1, 2, 36, 1, 36, Blocks.STONE.defaultBlockState());
        ServerPlayer Reader = survivor(Helper, 4.5, 2.0, 4.5);
        Scp327ShoalCondition Condition = new Scp327ShoalCondition(0, 0, 8, 34, 1);
        Investigation Case = new Investigation(Scp327Registry.DocumentId, Helper.getLevel().getGameTime());
        Condition.begin(Reader, Case);
        Optional<StartCondition.Finding> First = Condition.tick(Reader, Case);
        Helper.assertTrue(First.isEmpty(), "the condition found water on a dry stone floor");
        Helper.runAfterDelay(25, () -> {
            Helper.assertTrue(Condition.tick(Reader, Case).isEmpty(), "the condition found water before any was placed");
            Helper.assertTrue(Case.getState().getInt(Scp327ShoalCondition.TriesKey) >= 2, "the condition did not keep retrying");
            pool(Helper, 20, 20, 31, 31, 3, 0);
        });
        Helper.runAfterDelay(55, () -> {
            Optional<StartCondition.Finding> Found = Condition.tick(Reader, Case);
            Helper.assertTrue(Found.isPresent(), "the condition did not find the new pool on retry");
            Helper.assertTrue(Scp327Waters.isWater(Helper.getLevel(), Found.get().Pos()), "the finding is not in water");
            dismiss(Helper, Reader);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp327_targetsClosestPerson")
    public static void targetsClosestPerson(GameTestHelper Helper) {
        fill(Helper, 2, 1, 2, 38, 1, 38, Blocks.STONE.defaultBlockState());
        pool(Helper, 10, 10, 22, 22, 3, 0);
        Scp327Siren Creature = siren(Helper, 16.5, 2.5, 16.5);
        ServerPlayer Far = survivor(Helper, 35.5, 5.0, 16.5);
        ServerPlayer Builder = survivor(Helper, 16.5, 5.0, 25.5);
        Builder.setGameMode(GameType.CREATIVE);
        DClass Near = prisoner(Helper, 16.5, 5.0, 27.5);
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Creature.chosen() == Near, "SCP-327 did not pick the nearest person, skipping the creative player" + state(Creature));
            dismiss(Helper, Far);
            dismiss(Helper, Builder);
            tidy(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 500, batch = "scp327_beachesOnShore")
    public static void beachesOnShore(GameTestHelper Helper) {
        fill(Helper, 1, 1, 1, 38, 1, 38, Blocks.STONE.defaultBlockState());
        pool(Helper, 4, 4, 18, 30, 3, 0);
        fill(Helper, 19, 2, 3, 30, 4, 31, Blocks.SAND.defaultBlockState());
        Scp327Siren Creature = siren(Helper, 6.5, 2.5, 6.5);
        ServerPlayer Target = survivor(Helper, 24.5, 5.0, 24.5);
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Creature.isBeachedMode(), "SCP-327 has not hauled itself onto the shore" + state(Creature));
            double Edge = Creature.position().distanceTo(Helper.absoluteVec(new Vec3(19.5, 5.0, 24.5)));
            Helper.assertTrue(Edge <= 3.5, "SCP-327 beached " + Edge + " blocks from the shore spot nearest the target" + state(Creature));
            Helper.assertTrue(Scp327Lure.strength(Target) > 0.0f, "SCP-327 is beached but not singing" + state(Creature));
            dismiss(Helper, Target);
            tidy(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp327_staysInWater")
    public static void staysInWater(GameTestHelper Helper) {
        fill(Helper, 1, 1, 1, 38, 1, 38, Blocks.STONE.defaultBlockState());
        pool(Helper, 4, 4, 18, 30, 3, 4);
        Scp327Siren Creature = siren(Helper, 6.5, 2.5, 6.5);
        ServerPlayer Target = survivor(Helper, 24.5, 2.0, 24.5);
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Creature.mode() == Scp327Siren.ModeSing, "SCP-327 is not singing from the water" + state(Creature));
            Helper.assertTrue(Creature.isInWater() && !Creature.isBeachedMode(), "SCP-327 left the walled pool" + state(Creature));
            double Corner = Creature.position().distanceTo(Helper.absoluteVec(new Vec3(18.0, 3.0, 24.5)));
            Helper.assertTrue(Corner <= 4.0, "SCP-327 is singing " + Corner + " blocks from the water nearest the target" + state(Creature));
            dismiss(Helper, Target);
            tidy(Helper);
        });
    }

    private static void singingSetup(GameTestHelper Helper) {
        fill(Helper, 1, 1, 1, 38, 1, 38, Blocks.STONE.defaultBlockState());
        pool(Helper, 4, 4, 14, 14, 3, 0);
    }

    @GameTest(template = Arena, timeoutTicks = 380, batch = "scp327_singingPulls")
    public static void singingPulls(GameTestHelper Helper) {
        singingSetup(Helper);
        fill(Helper, 3, 2, 16, 15, 4, 26, Blocks.STONE.defaultBlockState());
        Scp327Siren Creature = siren(Helper, 12.5, 2.5, 12.5);
        ServerPlayer Listener = survivor(Helper, 22.5, 5.0, 12.5);
        DClass Prisoner = prisoner(Helper, 12.5, 5.0, 22.5);
        Prisoner.setNoAi(false);
        Creature.setInvulnerable(true);
        Creature.forceSong(Listener, 600);
        boolean[] Drawn = {false};
        boolean[] Seized = {false};
        Helper.onEachTick(() -> {
            if (Creature.chosen() != Prisoner) {
                return;
            }
            BlockPos Goal = Prisoner.getNavigation().getTargetPos();
            if (Goal != null && Goal.closerThan(Creature.blockPosition(), 3.0) && Scp327Lure.strength(Prisoner) > 0.0f) {
                Drawn[0] = true;
            }
            if (Scp327Hold.isHeld(Prisoner)) {
                Seized[0] = true;
            }
        });
        Helper.runAfterDelay(140, () -> {
            float Strength = Scp327Lure.strength(Listener);
            Helper.assertTrue(Creature.isSinging(), "SCP-327 stopped singing" + state(Creature));
            Helper.assertTrue(Strength > 0.3f, "the song pull on an unprotected player is only " + Strength + state(Creature));
            Helper.assertTrue(!Scp327Lure.shielded(Listener), "an unprotected player counts as protected");
            Creature.forceSong(Prisoner, 600);
        });
        Helper.runAfterDelay(340, () -> {
            float Peak = Scp327Lure.peak(Prisoner);
            Helper.assertTrue(Peak > 0.3f || Seized[0], "the song pull on a D-Class peaked at only " + Peak + state(Creature));
            Helper.assertTrue(Drawn[0] || Seized[0], "the D-Class never walked toward the singer" + state(Creature));
            dismiss(Helper, Listener);
            tidy(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp327_wallBlocksSong")
    public static void wallBlocksSong(GameTestHelper Helper) {
        singingSetup(Helper);
        fill(Helper, 19, 2, 6, 19, 9, 19, Blocks.STONE.defaultBlockState());
        Scp327Siren Creature = siren(Helper, 12.5, 2.5, 12.5);
        ServerPlayer Hidden = survivor(Helper, 23.5, 2.0, 12.5);
        Creature.setInvulnerable(true);
        Creature.forceSong(Hidden, 600);
        Helper.runAfterDelay(140, () -> {
            Helper.assertFalse(Creature.heardBy(Hidden), "SCP-327 can see a player behind a wall" + state(Creature));
            Helper.assertTrue(Scp327Lure.strength(Hidden) <= 0.0f, "the song pulled a player who could not hear it: " + Scp327Lure.strength(Hidden) + state(Creature));
            dismiss(Helper, Hidden);
            tidy(Helper);
            Helper.succeed();
        });
    }

    private static void protectedListener(GameTestHelper Helper, String What, Consumer<ServerPlayer> Protect) {
        singingSetup(Helper);
        Scp327Siren Creature = siren(Helper, 12.5, 2.5, 12.5);
        ServerPlayer Listener = survivor(Helper, 22.5, 5.0, 12.5);
        Protect.accept(Listener);
        Creature.setInvulnerable(true);
        Creature.forceSong(Listener, 600);
        Helper.runAfterDelay(140, () -> {
            Helper.assertTrue(Creature.isSinging(), "SCP-327 stopped singing" + state(Creature));
            Helper.assertTrue(Scp327Lure.strength(Listener) <= 0.0f, What + " did not block the pull: " + Scp327Lure.strength(Listener));
            Helper.assertTrue(Scp327Lure.shielded(Listener), What + " is not recognised as protection");
            dismiss(Helper, Listener);
            tidy(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp327_earplugsBlock")
    public static void earplugsBlock(GameTestHelper Helper) {
        protectedListener(Helper, "earplugs", Listener -> Listener.setItemSlot(EquipmentSlot.HEAD, new ItemStack(SensesRegistry.Earplugs.get())));
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp327_pillsBlock")
    public static void pillsBlock(GameTestHelper Helper) {
        protectedListener(Helper, "anti-memetic protection", Listener -> Listener.addEffect(new MobEffectInstance(AntiMemeticRegistry.Shielded, 2400)));
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp327_shieldBlocks")
    public static void shieldBlocks(GameTestHelper Helper) {
        protectedListener(Helper, "the telekill shield", Listener -> Listener.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Scp148Registry.Helmet.get())));
    }

    @GameTest(template = Arena, timeoutTicks = 500, batch = "scp327_dragDrowns")
    public static void dragDrowns(GameTestHelper Helper) {
        singingSetup(Helper);
        Scp327Siren Creature = siren(Helper, 8.5, 2.5, 8.5);
        DClass Victim = prisoner(Helper, 9.5, 2.5, 9.0);
        int[] Lowest = {Integer.MAX_VALUE};
        boolean[] Seen = {false};
        Helper.onEachTick(() -> {
            if (Scp327Hold.isHeld(Victim)) {
                Seen[0] = true;
                Lowest[0] = Math.min(Lowest[0], Victim.getAirSupply());
            }
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Seen[0], "SCP-327 never seized the D-Class" + state(Creature));
            Helper.assertTrue(Lowest[0] <= 0, "the D-Class never ran out of air");
            Helper.assertTrue(!Victim.isAlive(), "the D-Class has not drowned yet" + state(Creature));
            Helper.assertTrue(Creature.kills() == 1, "SCP-327 counted " + Creature.kills() + " kills for one drowning");
            Helper.assertTrue(!Scp327Hold.isHeld(Victim), "the hold mark outlived the victim");
            tidy(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "scp327_damageReleases")
    public static void damageReleases(GameTestHelper Helper) {
        singingSetup(Helper);
        Scp327Siren Creature = siren(Helper, 8.5, 2.5, 8.5);
        DClass Victim = prisoner(Helper, 9.5, 2.5, 9.0);
        ServerPlayer Friend = survivor(Helper, 20.5, 2.0, 8.5);
        boolean[] Struck = {false};
        Helper.onEachTick(() -> {
            if (!Struck[0] && Scp327Hold.isHeld(Victim) && Creature.mode() == Scp327Siren.ModeDrag) {
                Struck[0] = true;
                Creature.invulnerableTime = 0;
                Creature.hurt(Helper.getLevel().damageSources().playerAttack(Friend), 5.0f);
                Creature.invulnerableTime = 0;
                Creature.hurt(Helper.getLevel().damageSources().playerAttack(Friend), 5.0f);
            }
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Struck[0], "SCP-327 never seized the D-Class" + state(Creature));
            Helper.assertTrue(!Scp327Hold.isHeld(Victim), "SCP-327 kept its grip after 10 damage" + state(Creature));
            Helper.assertTrue(Creature.mode() != Scp327Siren.ModeDrag, "SCP-327 is still dragging" + state(Creature));
            Helper.assertTrue(Victim.isAlive(), "the D-Class died anyway");
            dismiss(Helper, Friend);
            tidy(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "scp327_struggleReleases")
    public static void struggleReleases(GameTestHelper Helper) {
        singingSetup(Helper);
        Scp327Siren Creature = siren(Helper, 8.5, 2.5, 8.5);
        ServerPlayer Victim = survivor(Helper, 9.5, 2.5, 9.0);
        Victim.setHealth(Victim.getMaxHealth());
        int[] Presses = {0};
        Helper.onEachTick(() -> {
            if (Scp327Hold.isHeld(Victim) && Helper.getLevel().getGameTime() % 3 == 0) {
                Scp327Hold.struggle(Victim);
                Presses[0]++;
            }
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Presses[0] >= Scp327Hold.StruggleNeeded, "the player was not held long enough to struggle (" + Presses[0] + " presses)" + state(Creature));
            Helper.assertTrue(!Scp327Hold.isHeld(Victim), "struggling did not break the hold" + state(Creature));
            Helper.assertTrue(Victim.isAlive(), "the player died while struggling free");
            dismiss(Helper, Victim);
            tidy(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp327_killsSpawnPup")
    public static void killsSpawnPup(GameTestHelper Helper) {
        singingSetup(Helper);
        Scp327Siren Creature = siren(Helper, 8.5, 2.5, 8.5);
        ServerLevel World = Helper.getLevel();
        Creature.recordKill(World);
        Creature.recordKill(World);
        Helper.runAfterDelay(10, () -> {
            Helper.assertTrue(pups(Helper).isEmpty(), "a pup appeared after only two kills");
            Creature.recordKill(World);
        });
        Helper.runAfterDelay(25, () -> {
            List<Scp327Siren> Found = pups(Helper);
            Helper.assertTrue(Found.size() == 1, "three kills made " + Found.size() + " pups" + state(Creature));
            Helper.assertTrue(Found.get(0).isBaby() && Found.get(0).distanceTo(Creature) < 4.0, "the pup is not a baby beside its parent");
            Helper.assertTrue(Creature.kills() == 0, "the parent kept " + Creature.kills() + " kills after the birth");
            tidy(Helper);
            Helper.succeed();
        });
    }

    private static List<Scp327Siren> pups(GameTestHelper Helper) {
        return Helper.getLevel().getEntitiesOfClass(Scp327Siren.class, new AABB(Helper.absolutePos(BlockPos.ZERO)).inflate(48.0), Candidate -> Candidate.isAlive() && Candidate.isPup());
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp327_pupGrows")
    public static void pupGrows(GameTestHelper Helper) {
        singingSetup(Helper);
        Scp327Siren Pup = Helper.spawn(Scp327Registry.Pup.get(), new Vec3(8.5, 2.5, 8.5));
        ServerLevel World = Helper.getLevel();
        float BabyWidth = Pup.getBbWidth();
        Helper.assertTrue(Pup.isBaby(), "a fresh pup is not a baby");
        Pup.rewindStage(Scp327Siren.StageTicks - 10L);
        Helper.runAfterDelay(5, () -> Helper.assertTrue(Pup.isBaby(), "the pup grew before a full day had passed"));
        Helper.runAfterDelay(40, () -> {
            Helper.assertTrue(Pup.stage() == Scp327Siren.StageTeen, "the pup did not reach its teen stage on schedule" + state(Pup));
            Helper.assertTrue(Pup.getBbWidth() > BabyWidth, "the teen did not grow");
            CompoundTag Saved = Pup.saveWithoutId(new CompoundTag());
            Scp327Siren Copy = Scp327Registry.Pup.get().create(World);
            Helper.assertTrue(Copy != null, "could not make a copy");
            Copy.load(Saved);
            Helper.assertTrue(Copy.stage() == Scp327Siren.StageTeen, "the stage was lost on reload");
            Helper.assertTrue(Copy.stageStarted() == Pup.stageStarted(), "the growth clock was lost on reload");
            Pup.rewindStage(Scp327Siren.StageTicks);
        });
        Helper.runAfterDelay(80, () -> {
            List<Scp327Siren> Adults = World.getEntitiesOfClass(Scp327Siren.class, new AABB(Helper.absolutePos(BlockPos.ZERO)).inflate(48.0),
                    Candidate -> Candidate.isAlive() && Candidate.getType() == Scp327Registry.Siren.get());
            Helper.assertTrue(Pup.isRemoved(), "the teen did not mature");
            Helper.assertTrue(Adults.size() == 1 && Adults.get(0).stage() == Scp327Siren.StageAdult, "the teen did not become a full adult");
            Helper.assertTrue(Adults.get(0).getMaxHealth() == (float) Scp327Siren.AdultHealth, "the new adult has " + Adults.get(0).getMaxHealth() + " max health");
            tidy(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp327_populationCap")
    public static void populationCap(GameTestHelper Helper) {
        fill(Helper, 1, 1, 1, 38, 1, 38, Blocks.STONE.defaultBlockState());
        pool(Helper, 4, 4, 30, 30, 3, 0);
        ServerLevel World = Helper.getLevel();
        Scp327Siren First = null;
        for (int Index = 0; Index < Scp327Shoal.PopulationCap; Index++) {
            Scp327Siren Creature = siren(Helper, 6.5 + (Index % 4) * 6.0, 2.5, 6.5 + (Index / 4) * 8.0);
            if (First == null) {
                First = Creature;
            }
        }
        Scp327Siren Parent = First;
        Parent.setKills(3);
        Helper.runAfterDelay(40, () -> {
            int Count = Scp327Shoal.population(World, Parent.position());
            Helper.assertTrue(Count == Scp327Shoal.PopulationCap, "the population grew past the cap to " + Count);
            Helper.assertTrue(pups(Helper).isEmpty(), "a pup was born above the cap");
            Helper.assertTrue(Parent.kills() == 3, "the kills owed to a future pup were lost");
            tidy(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp327_strandedDies")
    public static void strandedDies(GameTestHelper Helper) {
        fill(Helper, 1, 1, 1, 38, 1, 38, Blocks.STONE.defaultBlockState());
        Scp327Siren Creature = siren(Helper, 20.5, 2.0, 20.5);
        Creature.setMoisture(Creature.maxMoisture() / 2);
        Creature.setHealth(3.0f);
        int[] Wettest = {Creature.moisture()};
        Helper.runAfterDelay(30, () -> {
            Helper.assertTrue(Creature.mode() == Scp327Siren.ModeStranded, "SCP-327 does not know it is stranded" + state(Creature));
            Helper.assertTrue(Creature.moisture() < Wettest[0], "SCP-327 is not drying out" + state(Creature));
            Creature.setMoisture(0);
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(!Creature.isAlive(), "SCP-327 survived stranded and dry" + state(Creature));
            Helper.assertTrue(Creature.getLastDamageSource() != null && Creature.getLastDamageSource().is(DamageTypes.DRY_OUT),
                    "SCP-327 died of something other than drying out");
            tidy(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 260, batch = "scp327_tankKeepsAlive")
    public static void tankKeepsAlive(GameTestHelper Helper) {
        fill(Helper, 1, 1, 1, 38, 1, 38, Blocks.STONE.defaultBlockState());
        pool(Helper, 16, 16, 21, 21, 4, 1);
        fill(Helper, 15, 6, 15, 22, 6, 22, Blocks.GLASS.defaultBlockState());
        Scp327Siren Creature = siren(Helper, 18.5, 2.5, 18.5);
        Creature.setMoisture(100);
        Helper.runAfterDelay(240, () -> {
            Helper.assertTrue(Creature.isAlive(), "SCP-327 died in a water tank");
            Helper.assertTrue(Creature.isInWater() && Creature.mode() != Scp327Siren.ModeStranded, "SCP-327 left the tank" + state(Creature));
            Helper.assertTrue(Creature.moisture() == Creature.maxMoisture(), "SCP-327 is not kept wet by the tank" + state(Creature));
            Helper.assertTrue(Creature.getHealth() == Creature.getMaxHealth(), "SCP-327 took damage in the tank" + state(Creature));
            tidy(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 500, batch = "scp327_controlledDClass")
    public static void controlledDClass(GameTestHelper Helper) {
        singingSetup(Helper);
        Scp327Siren Creature = siren(Helper, 8.5, 2.5, 8.5);
        Creature.setYRot(0.0f);
        DClass Body = prisoner(Helper, 12.5, 5.0, 16.5);
        ServerPlayer Operator = mortal(Helper, 13.5, 5.0, 16.5);
        Helper.assertTrue(ControlManager.possess(Operator, Body), "the operator could not take control of the D-Class");
        Helper.assertTrue(ControlSessions.isControlling(Operator), "the control session did not start");
        Vec3 Spot = Helper.absoluteVec(new Vec3(9.5, 2.5, 9.0));
        Operator.moveTo(Spot.x, Spot.y, Spot.z);
        int[] Deferred = {0};
        Helper.onEachTick(() -> {
            if (ControlManager.hasPendingDeath(Operator)) {
                Deferred[0]++;
                Helper.assertTrue(Creature.kills() == 0, "SCP-327 counted the controlled body before its deferred death");
                ControlManager.processDeath(Operator);
            }
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Deferred[0] == 1, "the controlled D-Class did not go through the deferred death exactly once (" + Deferred[0] + ")" + state(Creature));
            Helper.assertTrue(Creature.kills() == 1, "SCP-327 counted " + Creature.kills() + " kills for one controlled D-Class");
            Helper.assertTrue(!ControlSessions.isControlling(Operator), "the control session survived the death");
            Helper.assertTrue(!Scp327Hold.isHeld(Operator), "the operator is still held after the body died");
            dismiss(Helper, Operator);
            tidy(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp327_stasisRoundTrip")
    public static void stasisRoundTrip(GameTestHelper Helper) {
        fill(Helper, 1, 1, 1, 38, 1, 38, Blocks.STONE.defaultBlockState());
        pool(Helper, 4, 4, 30, 30, 3, 0);
        ServerLevel World = Helper.getLevel();
        ServerPlayer Keeper = survivor(Helper, 34.5, 5.0, 34.5);
        Keeper.setGameMode(GameType.CREATIVE);
        Scp327Siren Adult = siren(Helper, 8.5, 2.5, 8.5);
        Scp327Siren Teen = Helper.spawn(Scp327Registry.Pup.get(), new Vec3(20.5, 2.5, 20.5));
        Adult.setKills(2);
        Adult.setMoisture(1500);
        Teen.setStage(Scp327Siren.StageTeen);
        Teen.rewindStage(6000L);
        DClass Victim = prisoner(Helper, 9.5, 2.5, 9.0);
        ItemStack AdultPod = new ItemStack(StasisRegistry.podFor(FragmentRarity.Faint));
        ItemStack TeenPod = new ItemStack(StasisRegistry.podFor(FragmentRarity.Faint));
        long[] TeenAge = {0L};
        Helper.runAfterDelay(30, () -> {
            Helper.assertTrue(Scp327Hold.isHeld(Victim), "SCP-327 did not seize the D-Class before capture" + state(Adult));
            TeenAge[0] = Teen.stageAge();
            Helper.assertTrue(StasisPods.capture(World, Keeper, AdultPod, Adult), "the pod refused the adult while it dragged");
            Helper.assertTrue(StasisPods.capture(World, Keeper, TeenPod, Teen), "the pod refused the teen");
            Helper.assertTrue(StasisPodItem.isOccupied(AdultPod) && StasisPodItem.isOccupied(TeenPod), "a pod holds nothing");
            Helper.assertTrue(!Scp327Hold.isHeld(Victim), "the capture did not break the grip");
        });
        Helper.runAfterDelay(90, () -> {
            StasisPods.breakOut(World, AdultPod, Helper.absoluteVec(new Vec3(14.5, 2.2, 14.5)), null);
            StasisPods.breakOut(World, TeenPod, Helper.absoluteVec(new Vec3(24.5, 2.2, 24.5)), null);
        });
        Helper.runAfterDelay(100, () -> {
            List<Scp327Siren> Back = World.getEntitiesOfClass(Scp327Siren.class, new AABB(Helper.absolutePos(BlockPos.ZERO)).inflate(48.0), Scp327Siren::isAlive);
            Scp327Siren NewAdult = Back.stream().filter(Candidate -> !Candidate.isPup()).findFirst().orElse(null);
            Scp327Siren NewTeen = Back.stream().filter(Scp327Siren::isPup).findFirst().orElse(null);
            Helper.assertTrue(NewAdult != null && NewTeen != null, "the released instances are missing");
            Helper.assertTrue(NewAdult.kills() == 2, "the adult lost its kill count in the pod: " + NewAdult.kills());
            Helper.assertTrue(Math.abs(NewAdult.moisture() - 1500) < 30 || NewAdult.isInWater(), "the adult's moisture changed in the pod" + state(NewAdult));
            Helper.assertTrue(NewTeen.stage() == Scp327Siren.StageTeen, "the teen lost its stage in the pod");
            long Drift = Math.abs(NewTeen.stageAge() - TeenAge[0]);
            Helper.assertTrue(Drift < 40L, "time in the pod counted toward growth: drift " + Drift + " ticks");
            Helper.assertTrue(NewAdult.isInWater() && NewTeen.isInWater(), "a released instance is not in the water");
            Helper.assertTrue(NewAdult.mode() != Scp327Siren.ModeDrag, "the released adult is still dragging");
            dismiss(Helper, Keeper);
            tidy(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 720, batch = "scp327_flourishCooldown")
    public static void flourishCooldown(GameTestHelper Helper) {
        fill(Helper, 1, 1, 1, 38, 1, 38, Blocks.STONE.defaultBlockState());
        pool(Helper, 4, 4, 34, 34, 6, 0);
        ServerLevel World = Helper.getLevel();
        Scp327Siren Creature = siren(Helper, 19.5, 4.0, 19.5);
        Creature.scheduleFlourish(5);
        List<Long> Starts = new ArrayList<>();
        byte[] Last = {Scp327Siren.FlourishNone};
        int[] Run = {0};
        int[] Longest = {0};
        Helper.onEachTick(() -> {
            byte Now = Creature.flourish();
            if (Now != Scp327Siren.FlourishNone && Last[0] == Scp327Siren.FlourishNone) {
                Starts.add(World.getGameTime());
            }
            Run[0] = Now != Scp327Siren.FlourishNone ? Run[0] + 1 : 0;
            Longest[0] = Math.max(Longest[0], Run[0]);
            Last[0] = Now;
        });
        Helper.runAfterDelay(680, () -> {
            Helper.assertTrue(!Starts.isEmpty(), "no flourish played after it was scheduled" + state(Creature));
            Helper.assertTrue(Starts.size() <= 2, Starts.size() + " flourishes played in 34 seconds");
            for (int Index = 1; Index < Starts.size(); Index++) {
                long Gap = Starts.get(Index) - Starts.get(Index - 1);
                Helper.assertTrue(Gap >= Scp327Siren.FlourishMin, "two flourishes started only " + Gap + " ticks apart");
            }
            int Limit = 0;
            for (int Ticks : Scp327Siren.FlourishTicks) {
                Limit = Math.max(Limit, Ticks);
            }
            Helper.assertTrue(Longest[0] <= Limit + 2, "a flourish kept playing for " + Longest[0] + " ticks");
            tidy(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 320, batch = "scp327_rotationWraps")
    public static void rotationWraps(GameTestHelper Helper) {
        fill(Helper, 1, 1, 1, 38, 1, 38, Blocks.STONE.defaultBlockState());
        pool(Helper, 4, 4, 34, 34, 6, 0);
        ServerLevel World = Helper.getLevel();
        Scp327Siren Creature = siren(Helper, 19.5, 5.0, 19.5);
        Creature.setYRot(175.0f);
        Creature.setYBodyRot(175.0f);
        Creature.setYHeadRot(175.0f);
        int[] Clock = {0};
        int[] Wraps = {0};
        float[] Previous = {175.0f};
        float[] Deepest = {0.0f};
        List<String> Faults = new ArrayList<>();
        Vec3 Centre = Helper.absoluteVec(new Vec3(19.5, 5.0, 19.5));
        Helper.onEachTick(() -> {
            int Now = Clock[0]++;
            if (Now < 200) {
                double Angle = Now * 0.12;
                Creature.guide(Centre.add(Math.cos(Angle) * 5.0, 0.0, Math.sin(Angle) * 5.0), 2);
            } else if (Now < 260) {
                Creature.guide(Creature.position().add(Vec3.directionFromRotation(0.0f, Creature.getYRot()).scale(3.0)).add(0.0, -4.0, 0.0), 2);
                Deepest[0] = Math.max(Deepest[0], Creature.getXRot());
            }
            float Yaw = Creature.getYRot();
            if (Math.abs(Yaw - Previous[0]) > 180.0f) {
                Wraps[0]++;
            }
            Previous[0] = Yaw;
            if (Math.abs(Yaw) > 180.001f || Math.abs(Creature.yBodyRot) > 180.001f || Math.abs(Creature.getYHeadRot()) > 180.001f) {
                Faults.add("tick " + Now + ": yaw " + Yaw + ", body " + Creature.yBodyRot + ", head " + Creature.getYHeadRot());
            }
            if (Math.abs(Mth.wrapDegrees(Creature.getYHeadRot() - Creature.yBodyRot)) > 60.0f) {
                Faults.add("tick " + Now + ": head turned " + Mth.wrapDegrees(Creature.getYHeadRot() - Creature.yBodyRot) + " from the body");
            }
            if (Math.abs(Creature.getXRot()) > Scp327Siren.PitchLimit + 0.001f) {
                Faults.add("tick " + Now + ": pitch " + Creature.getXRot());
            }
        });
        Helper.runAfterDelay(270, () -> {
            Helper.assertTrue(Faults.isEmpty(), "rotation left its range: " + (Faults.isEmpty() ? "" : Faults.get(0)) + " (" + Faults.size() + " faults)");
            Helper.assertTrue(Wraps[0] >= 2, "the circling siren crossed yaw +-180 only " + Wraps[0] + " times");
            Helper.assertTrue(Deepest[0] >= 20.0f, "the dive pitch never built up past " + Deepest[0] + " degrees");
            tidy(Helper);
            Helper.succeed();
        });
    }

    private static void shoreSetup(GameTestHelper Helper) {
        fill(Helper, 1, 1, 1, 38, 1, 38, Blocks.STONE.defaultBlockState());
        pool(Helper, 4, 4, 18, 30, 3, 0);
        fill(Helper, 19, 2, 3, 30, 4, 31, Blocks.SAND.defaultBlockState());
    }

    private static void watchDrag(GameTestHelper Helper, Scp327Siren Creature, LivingEntity Victim, List<String> Faults, boolean[] Wet, boolean[] Held) {
        ServerLevel World = Helper.getLevel();
        Helper.onEachTick(() -> {
            if (!Victim.isAlive()) {
                return;
            }
            if (Scp327Hold.stuck(World, Victim)) {
                Faults.add("victim inside a block at " + Victim.position());
            }
            if (Victim.getAirSupply() < Victim.getMaxAirSupply() && !Wet[0] && !Scp327Hold.eyesUnder(Victim)) {
                Faults.add("air drained on land at " + Victim.position());
            }
            if (Scp327Hold.eyesUnder(Victim)) {
                Wet[0] = true;
            }
            if (Scp327Hold.isHeld(Victim)) {
                Held[0] = true;
            }
            if (Victim.getLastDamageSource() != null && Victim.getLastDamageSource().is(DamageTypes.IN_WALL)) {
                Faults.add("victim took suffocation damage at " + Victim.position());
            }
        });
    }

    @GameTest(template = Arena, timeoutTicks = 700, batch = "scp327_beachDragPlayer")
    public static void beachDragPlayer(GameTestHelper Helper) {
        shoreSetup(Helper);
        Scp327Siren Creature = Helper.spawn(Scp327Registry.Siren.get(), new Vec3(10.5, 2.5, 17.5));
        Creature.restOnShore(Helper.absolutePos(new BlockPos(19, 5, 17)), Helper.absolutePos(new BlockPos(18, 4, 17)));
        ServerPlayer Victim = mortal(Helper, 21.5, 5.0, 17.5);
        Creature.lookFor(Victim);
        List<String> Faults = new ArrayList<>();
        boolean[] Wet = {false};
        boolean[] Held = {false};
        watchDrag(Helper, Creature, Victim, Faults, Wet, Held);
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Faults.isEmpty(), "the drag went wrong: " + (Faults.isEmpty() ? "" : Faults.get(0)) + " (" + Faults.size() + " faults)" + state(Creature));
            Helper.assertTrue(Held[0], "the beached siren never seized the player" + state(Creature));
            Helper.assertTrue(Wet[0], "the player was never pulled under the water" + state(Creature));
            Helper.assertTrue(Creature.kills() == 1, "the player has not drowned yet" + state(Creature));
            dismiss(Helper, Victim);
            tidy(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 700, batch = "scp327_beachDragMob")
    public static void beachDragMob(GameTestHelper Helper) {
        shoreSetup(Helper);
        Scp327Siren Creature = Helper.spawn(Scp327Registry.Siren.get(), new Vec3(10.5, 2.5, 17.5));
        Creature.restOnShore(Helper.absolutePos(new BlockPos(19, 5, 17)), Helper.absolutePos(new BlockPos(18, 4, 17)));
        DClass Victim = prisoner(Helper, 21.5, 5.0, 17.5);
        Creature.lookFor(Victim);
        List<String> Faults = new ArrayList<>();
        boolean[] Wet = {false};
        boolean[] Held = {false};
        watchDrag(Helper, Creature, Victim, Faults, Wet, Held);
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Faults.isEmpty(), "the drag went wrong: " + (Faults.isEmpty() ? "" : Faults.get(0)) + " (" + Faults.size() + " faults)" + state(Creature));
            Helper.assertTrue(Held[0] && Wet[0], "the D-Class was not dragged under" + state(Creature));
            Helper.assertTrue(!Victim.isAlive() && Creature.kills() == 1, "the D-Class has not drowned yet" + state(Creature));
            tidy(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp327_wallReleases")
    public static void wallReleases(GameTestHelper Helper) {
        fill(Helper, 1, 1, 1, 38, 1, 38, Blocks.STONE.defaultBlockState());
        pool(Helper, 4, 4, 14, 14, 3, 0);
        fill(Helper, 15, 2, 3, 25, 4, 15, Blocks.STONE.defaultBlockState());
        fill(Helper, 16, 5, 3, 16, 6, 15, Blocks.STONE.defaultBlockState());
        ServerLevel World = Helper.getLevel();
        Scp327Siren Creature = siren(Helper, 13.5, 3.0, 9.5);
        DClass Victim = prisoner(Helper, 17.5, 5.0, 9.5);
        double StartX = Victim.getX();
        List<String> Faults = new ArrayList<>();
        Helper.runAfterDelay(2, () -> Creature.grab(World, Victim));
        Helper.onEachTick(() -> {
            if (Scp327Hold.stuck(World, Victim)) {
                Faults.add("victim inside a block at " + Victim.position());
            }
            if (Math.abs(Victim.getX() - StartX) > 0.6) {
                Faults.add("victim was pulled toward the wall to " + Victim.position());
            }
        });
        Helper.runAfterDelay(40, () -> {
            Helper.assertTrue(Faults.isEmpty(), "the wall grab went wrong: " + (Faults.isEmpty() ? "" : Faults.get(0)));
            Helper.assertTrue(!Scp327Hold.isHeld(Victim), "SCP-327 kept hold of a victim behind a wall" + state(Creature));
            Helper.assertTrue(Creature.mode() != Scp327Siren.ModeDrag, "SCP-327 is still dragging through a wall" + state(Creature));
            tidy(Helper);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp327_floorClamp")
    public static void floorClamp(GameTestHelper Helper) {
        fill(Helper, 1, 1, 1, 38, 1, 38, Blocks.STONE.defaultBlockState());
        pool(Helper, 4, 4, 16, 16, 2, 0);
        ServerLevel World = Helper.getLevel();
        double Floor = Helper.absoluteVec(new Vec3(0.0, 2.0, 0.0)).y;
        Scp327Siren Creature = siren(Helper, 9.5, 2.2, 9.5);
        DClass Victim = prisoner(Helper, 10.5, 2.0, 9.5);
        List<String> Faults = new ArrayList<>();
        boolean[] Held = {false};
        Helper.onEachTick(() -> {
            if (Scp327Hold.isHeld(Victim)) {
                Held[0] = true;
            }
            if (Victim.isAlive() && Victim.getY() < Floor - 0.001) {
                Faults.add("victim pushed to y " + Victim.getY() + " below the floor at " + Floor);
            }
            if (Victim.isAlive() && Scp327Hold.stuck(World, Victim)) {
                Faults.add("victim inside a block at " + Victim.position());
            }
            if (Creature.getY() < Floor - 0.001) {
                Faults.add("siren sank to " + Creature.getY());
            }
        });
        Helper.runAfterDelay(160, () -> {
            Helper.assertTrue(Held[0], "SCP-327 never seized the D-Class" + state(Creature));
            Helper.assertTrue(Faults.isEmpty(), "the shallow drag went wrong: " + (Faults.isEmpty() ? "" : Faults.get(0)) + " (" + Faults.size() + " faults)");
            tidy(Helper);
            Helper.succeed();
        });
    }
}
