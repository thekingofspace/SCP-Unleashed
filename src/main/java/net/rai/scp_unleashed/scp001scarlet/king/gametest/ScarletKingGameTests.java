package net.rai.scp_unleashed.scp001scarlet.king.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guns.GunDamage;
import net.rai.scp_unleashed.guns.GunFiring;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.guns.gametest.Guns2GameTests;
import net.rai.scp_unleashed.scp001scarlet.core.ScarletCourtHook;
import net.rai.scp_unleashed.scp001scarlet.core.ScarletState;
import net.rai.scp_unleashed.scp001scarlet.king.ScarletArena;
import net.rai.scp_unleashed.scp001scarlet.king.ScarletKing;
import net.rai.scp_unleashed.scp001scarlet.king.ScarletKingConfig;
import net.rai.scp_unleashed.scp001scarlet.king.ScarletKingBar;
import net.rai.scp_unleashed.scp001scarlet.king.ScarletKingData;
import net.rai.scp_unleashed.scp001scarlet.king.ScarletKingEvents;
import net.rai.scp_unleashed.scp001scarlet.king.ScarletKingPart;
import net.rai.scp_unleashed.scp001scarlet.king.ScarletKingRegistry;
import net.rai.scp_unleashed.scp001scarlet.king.ScarletPhrase;
import net.rai.scp_unleashed.scp001scarlet.king.ScarletRite;
import net.rai.scp_unleashed.scp001scarlet.king.ScarletThorn;
import net.rai.scp_unleashed.scp001scarlet.king.loot.ScarletPodBlockEntity;
import net.rai.scp_unleashed.scp001scarlet.king.loot.ScarletRifleItem;
import net.rai.scp_unleashed.scp999.Scp999Blob;
import net.rai.scp_unleashed.scp999.Scp999Registry;

import java.util.List;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class ScarletKingGameTests {
    public static final String Arena = "scarletking_arena";
    public static final BlockPos Middle = new BlockPos(12, 1, 12);
    public static final int MinuteTicks = 1200;

    private ScarletKingGameTests() {
    }

    private static ScarletCourtHook Installed;
    private static boolean Captured;

    public static final class StubCourt implements ScarletCourtHook {
        public int Daughters;
        public int Begun;
        public int Ticks;
        public int Resets;
        public int Ends;
        public int LastPhase = -1;

        public StubCourt(int Daughters) {
            this.Daughters = Daughters;
        }

        @Override
        public void begin(ServerLevel Level, BlockPos Dais, LivingEntity King, int Players) {
            Begun++;
        }

        @Override
        public void tick(ServerLevel Level, LivingEntity King) {
            Ticks++;
        }

        @Override
        public int daughtersRemaining() {
            return Daughters;
        }

        @Override
        public boolean daughterActive() {
            return Daughters > 0;
        }

        @Override
        public void kingPhase(int Phase) {
            LastPhase = Phase;
        }

        @Override
        public void reset(ServerLevel Level) {
            Resets++;
        }

        @Override
        public void end(ServerLevel Level) {
            Ends++;
        }
    }

    private static AABB area(GameTestHelper Helper) {
        return new AABB(Helper.absolutePos(BlockPos.ZERO)).expandTowards(24.0, 30.0, 24.0).inflate(48.0, 4.0, 48.0);
    }

    private static void sweep(GameTestHelper Helper) {
        for (Entity Each : Helper.getLevel().getEntitiesOfClass(Entity.class, area(Helper), Each -> !(Each instanceof net.minecraft.world.entity.player.Player))) {
            if (Each instanceof ScarletKing King) {
                King.clearBar();
            }
            Each.discard();
        }
    }

    private static void restore(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        ScarletState.get(World.getServer()).phase(ScarletState.Phase.Asleep, World.getGameTime());
        ScarletKingData Data = ScarletKingData.get(World.getServer());
        Data.consumed(false);
        Data.endRite();
        Data.king(null);
        if (Captured) {
            ScarletCourtHook.Holder.set(Installed);
        }
    }

    private static void useCourt(ScarletCourtHook Hook) {
        if (!Captured) {
            Installed = ScarletCourtHook.Holder.get();
            Captured = true;
        }
        ScarletCourtHook.Holder.set(Hook);
    }

    private static void finish(GameTestHelper Helper) {
        sweep(Helper);
        restore(Helper);
        Helper.succeed();
    }

    private static BlockPos throne(GameTestHelper Helper) {
        return Helper.absolutePos(Middle).below(2);
    }

    private static ScarletKing king(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        ScarletKing Made = ScarletKingRegistry.KingType.get().create(World);
        Vec3 At = Vec3.atBottomCenterOf(Helper.absolutePos(Middle));
        Made.moveTo(At.x, At.y, At.z, 180.0f, 0.0f);
        Made.generation(ScarletKingData.get(World.getServer()).generation());
        Made.place(ScarletArena.of(World, throne(Helper)));
        World.addFreshEntity(Made);
        return Made;
    }

    private static FakePlayer shooter(GameTestHelper Helper, String Name) {
        return new FakePlayer(Helper.getLevel(), new GameProfile(UUID.randomUUID(), Name));
    }

    private static DamageSource gunshot(GameTestHelper Helper, FakePlayer User) {
        return GunDamage.sourceFor(Helper.getLevel(), User, GunKind.AssaultRifle);
    }

    private static void fight(GameTestHelper Helper, ScarletKing King, int Players) {
        ScarletState.get(Helper.getLevel().getServer()).phase(ScarletState.Phase.Fight, Helper.getLevel().getGameTime());
        King.beginFight(Players);
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scarletking_phrase")
    public static void phraseMatchesVariants(GameTestHelper Helper) {
        Helper.assertTrue(ScarletPhrase.matches("Seven thrones, Seven brides, Seven hearts for the scarlet king"), "the exact phrase failed");
        Helper.assertTrue(ScarletPhrase.matches("seven throne seven brides seven hearts for the Scarlet King!"), "the singular throne failed");
        Helper.assertTrue(ScarletPhrase.matches("SEVEN THRONES... seven brides; seven hearts - for the scarlet king."), "punctuation and case failed");
        Helper.assertTrue(ScarletPhrase.matches("7 thrones 7 brides 7 hearts for the scarlet king"), "digits failed");
        Helper.assertFalse(ScarletPhrase.matches("Seven thrones, Seven brides, Seven hearts for the red king"), "a wrong word matched");
        Helper.assertFalse(ScarletPhrase.matches("Seven thrones, Seven brides, for the scarlet king"), "a missing clause matched");
        Helper.assertFalse(ScarletPhrase.matches("I say: Seven thrones, Seven brides, Seven hearts for the scarlet king"), "extra words matched");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scarletking_conditions")
    public static void riteNeedsXk2ThroneAndNinehundred(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        BlockPos Throne = throne(Helper);
        Vec3 Near = Vec3.atBottomCenterOf(Helper.absolutePos(Middle));
        Vec3 Far = Near.add(60.0, 0.0, 0.0);
        Helper.assertTrue(ScarletRite.outcome(World, Near, ScarletState.Phase.Asleep, true, Throne) == ScarletRite.Outcome.Silent, "asleep was not silent");
        Helper.assertTrue(ScarletRite.outcome(World, Near, ScarletState.Phase.Stirring, true, Throne) == ScarletRite.Outcome.Stirring, "stirring answered");
        Helper.assertTrue(ScarletRite.outcome(World, Near, ScarletState.Phase.Xk2, false, Throne) == ScarletRite.Outcome.Unbound, "no throne and no 999 still answered");
        Helper.assertTrue(ScarletRite.outcome(World, Far, ScarletState.Phase.Xk2, true, Throne) == ScarletRite.Outcome.Elsewhere, "far from the throne still answered");
        Helper.assertTrue(ScarletRite.outcome(World, Near, ScarletState.Phase.Xk2, true, Throne) == ScarletRite.Outcome.Unbound, "no 999 still answered");
        Scp999Blob Blob = Scp999Registry.Blob.get().spawn(World, Helper.absolutePos(Middle.east(2)), MobSpawnType.COMMAND);
        Helper.assertTrue(Blob != null, "999 did not spawn");
        Helper.assertTrue(ScarletRite.outcome(World, Near, ScarletState.Phase.Xk2, true, Throne) == ScarletRite.Outcome.Ready, "the rite was not ready with 999 near");
        ScarletRite.Outcome Unbuilt = net.rai.scp_unleashed.scp001scarlet.throne.ScarletThroneRise.survey(World, Blob.position()).fine() ? ScarletRite.Outcome.Ready : ScarletRite.Outcome.Unsuitable;
        Helper.assertTrue(ScarletRite.outcome(World, Near, ScarletState.Phase.Xk2, false, Throne) == Unbuilt, "an unbuilt throne did not survey the spot under 999");
        Helper.assertTrue(ScarletRite.outcome(World, Near, ScarletState.Phase.Freed, true, Throne) == ScarletRite.Outcome.Silent, "freed still answered");
        finish(Helper);
    }

    @GameTest(template = Arena, timeoutTicks = 340, batch = "scarletking_rite")
    public static void confirmBindsNinehundredAndSpawnsTheKing(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        ScarletState State = ScarletState.get(World.getServer());
        State.phase(ScarletState.Phase.Xk2, World.getGameTime());
        Scp999Blob Blob = Scp999Registry.Blob.get().spawn(World, Helper.absolutePos(Middle.east(2)), MobSpawnType.COMMAND);
        Helper.assertTrue(Blob != null, "999 did not spawn");
        UUID BlobId = Blob.getUUID();
        ScarletRite.begin(World, throne(Helper), Blob);
        Helper.assertTrue(State.phase() == ScarletState.Phase.Rite, "the rite phase did not start");
        Helper.startSequence()
                .thenExecuteAfter(ScarletRite.FightAt + 12, () -> {
                    ScarletKingData Data = ScarletKingData.get(World.getServer());
                    Helper.assertTrue(State.phase() == ScarletState.Phase.Fight, "the fight did not start, phase " + State.phase());
                    Helper.assertTrue(World.getEntity(BlobId) == null, "999 was not consumed");
                    Helper.assertTrue(Data.consumed(), "999 was not recorded as consumed");
                    ScarletKing King = ScarletRite.king(World, Data);
                    Helper.assertTrue(King != null && King.fighting(), "the King is missing or not fighting");
                    Helper.assertTrue(King.bands() == ScarletKing.AllBands, "the seven bands are not bound");
                })
                .thenExecute(() -> finish(Helper));
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scarletking_gate")
    public static void kingOnlyTakesDamageInTheFight(GameTestHelper Helper) {
        sweep(Helper);
        useCourt(null);
        ScarletKing King = king(Helper);
        FakePlayer User = shooter(Helper, "scarlet_gate");
        Helper.assertFalse(King.hurt(gunshot(Helper, User), 50.0f), "the manifesting King took damage");
        fight(Helper, King, 1);
        float Before = King.pool();
        Helper.assertTrue(King.hurt(gunshot(Helper, User), 50.0f), "the fighting King ignored a gunshot");
        Helper.assertTrue(King.pool() < Before, "the pool did not drop");
        ScarletState.get(Helper.getLevel().getServer()).phase(ScarletState.Phase.Rite, Helper.getLevel().getGameTime());
        float Held = King.pool();
        Helper.assertFalse(King.hurt(gunshot(Helper, User), 50.0f), "the King took damage outside the Fight phase");
        Helper.assertTrue(King.pool() == Held, "the pool changed outside the fight");
        finish(Helper);
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scarletking_scale")
    public static void healthScalesAndTheHitCapHolds(GameTestHelper Helper) {
        sweep(Helper);
        useCourt(null);
        double Base = ScarletKingConfig.baseHealth();
        Helper.assertTrue(Math.abs(ScarletKing.scaledPool(1) - Base) < 1.0, "solo health is " + ScarletKing.scaledPool(1));
        Helper.assertTrue(Math.abs(ScarletKing.scaledPool(2) - Base * 1.6) < 1.0, "two players gave " + ScarletKing.scaledPool(2));
        Helper.assertTrue(Math.abs(ScarletKing.scaledPool(4) - Base * 2.8) < 1.0, "four players gave " + ScarletKing.scaledPool(4));
        ScarletKing King = king(Helper);
        fight(Helper, King, 3);
        Helper.assertTrue(Math.abs(King.poolMax() - Base * 2.2) < 1.0, "three players gave a pool of " + King.poolMax());
        FakePlayer User = shooter(Helper, "scarlet_cap");
        float Before = King.pool();
        King.hurt(gunshot(Helper, User), 1.0e6f);
        float Lost = Before - King.pool();
        Helper.assertTrue(Lost > 0.0f && Lost <= King.hitCap() + 0.01f, "a huge hit removed " + Lost + " with a cap of " + King.hitCap());
        Before = King.pool();
        King.setHealth(1.0f);
        Lost = Before - King.pool();
        Helper.assertTrue(Lost <= King.hitCap() + 0.01f, "setHealth removed " + Lost);
        Helper.assertTrue(King.isAlive(), "a percent kill went through");
        finish(Helper);
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scarletking_dps")
    public static void realisticDpsKillsWithinTheTargetWindow(GameTestHelper Helper) {
        sweep(Helper);
        useCourt(null);
        ScarletKing King = king(Helper);
        fight(Helper, King, 1);
        King.simulating(true);
        FakePlayer User = shooter(Helper, "scarlet_dps");
        DamageSource Rifle = GunDamage.sourceFor(Helper.getLevel(), User, GunKind.MachineGun);
        DamageSource Laser = GunDamage.sourceFor(Helper.getLevel(), User, GunKind.SniperRifle);
        ScarletKingPart Chest = King.parts()[1];
        ScarletKingPart Head = King.parts()[2];
        int Ticks = 0;
        while (King.pool() > 0.0f && Ticks < 20 * MinuteTicks) {
            King.simulate(1);
            int Second = Ticks % 20;
            if (Second < 17 && Second % 2 == 0) {
                Chest.hurt(Rifle, 6.0f);
            }
            if (Ticks % 400 == 200) {
                Head.hurt(Laser, 800.0f);
            }
            Ticks++;
        }
        King.simulating(false);
        float Minutes = Ticks / (float) MinuteTicks;
        Helper.assertTrue(King.pool() <= 0.0f, "the King survived " + Minutes + " minutes");
        Helper.assertTrue(Minutes >= 6.0f && Minutes <= 8.0f, "the King fell after " + Minutes + " minutes");
        finish(Helper);
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scarletking_court")
    public static void courtWardBlocksDamageUntilTheDaughtersFall(GameTestHelper Helper) {
        sweep(Helper);
        StubCourt Court = new StubCourt(2);
        useCourt(Court);
        ScarletKing King = king(Helper);
        fight(Helper, King, 1);
        Helper.assertTrue(Court.Begun == 1, "the court was not begun");
        Helper.assertTrue(King.warded(), "the King is not warded while daughters live");
        FakePlayer User = shooter(Helper, "scarlet_court");
        float Before = King.pool();
        Helper.assertFalse(King.hurt(gunshot(Helper, User), 200.0f), "the ward let a shot through");
        Helper.assertTrue(King.pool() == Before, "the warded pool dropped");
        int Intact = Integer.bitCount(King.bands());
        Helper.assertTrue(Intact == 3, "two slain daughters left " + Intact + " bands");
        Court.Daughters = 1;
        King.watchCourt(Helper.getLevel());
        Helper.assertTrue(Integer.bitCount(King.bands()) == Intact - 2, "a daughter's death did not snap two bands");
        Helper.assertTrue(King.warded(), "the ward fell with a daughter left");
        Court.Daughters = 0;
        King.watchCourt(Helper.getLevel());
        Helper.assertFalse(King.warded(), "the ward did not shatter");
        Helper.assertTrue(King.bands() == 0, "bands remain after the ward fell");
        Helper.assertTrue(King.hurt(gunshot(Helper, User), 200.0f), "the unwarded King ignored a shot");
        Helper.assertTrue(King.pool() < Before, "the unwarded pool did not drop");
        Helper.assertTrue(Court.Ticks > 0, "the court was never ticked");
        ScarletRite.reset(Helper.getLevel(), throne(Helper));
        Helper.assertTrue(Court.Resets == 1, "the court was not reset on a wipe");
        ScarletKing Second = king(Helper);
        fight(Helper, Second, 1);
        Second.hurt(Helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE);
        Helper.assertTrue(Court.Ends == 1, "the court was not ended on the King's death");
        finish(Helper);
    }

    @GameTest(template = Arena, timeoutTicks = 260, batch = "scarletking_death")
    public static void deathFreesTheWorldAndDropsTheLoot(GameTestHelper Helper) {
        sweep(Helper);
        useCourt(null);
        ServerLevel World = Helper.getLevel();
        ScarletKing King = king(Helper);
        fight(Helper, King, 1);
        King.hurt(World.damageSources().genericKill(), Float.MAX_VALUE);
        Helper.assertTrue(ScarletState.get(World.getServer()).phase() == ScarletState.Phase.Freed, "death did not free the world");
        Helper.assertTrue(World.getEntitiesOfClass(ItemEntity.class, area(Helper)).isEmpty(), "the loot dropped before the death played out");
        Helper.assertFalse(King.isPickable(), "the dying King can still be targeted");
        FakePlayer User = shooter(Helper, "scarlet_death");
        Helper.assertFalse(King.hurt(gunshot(Helper, User), 200.0f), "the dying King took a hit");
        Helper.runAfterDelay(ScarletKing.DeathTicks / 2, () -> {
            Helper.assertFalse(King.isRemoved(), "the King was removed before the death played out");
            Helper.assertTrue(World.getEntitiesOfClass(ItemEntity.class, area(Helper)).isEmpty(), "the loot dropped mid death");
        });
        Helper.runAfterDelay(ScarletKing.DeathTicks + 10, () -> {
            Helper.assertTrue(King.isRemoved(), "the King lingered after the death");
            List<ItemEntity> Drops = World.getEntitiesOfClass(ItemEntity.class, area(Helper));
            for (Item Wanted : new Item[]{ScarletKingRegistry.Helmet.get(), ScarletKingRegistry.Chestplate.get(), ScarletKingRegistry.Leggings.get(),
                    ScarletKingRegistry.Boots.get(), ScarletKingRegistry.Rifle.get(), ScarletKingRegistry.PodItem.get()}) {
                Helper.assertTrue(Drops.stream().anyMatch(Each -> Each.getItem().is(Wanted)), "the remains lack " + Wanted);
            }
            boolean Remains = false;
            for (BlockPos Pos : BlockPos.betweenClosed(throne(Helper).offset(-2, -2, -2), throne(Helper).offset(2, 8, 2))) {
                Remains |= World.getBlockState(Pos).is(ScarletKingRegistry.Remains.get());
            }
            Helper.assertTrue(Remains, "no remains block was left");
            finish(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scarletking_reset")
    public static void wipeResetsAndRestoresNinehundred(GameTestHelper Helper) {
        sweep(Helper);
        useCourt(null);
        ServerLevel World = Helper.getLevel();
        ScarletKing King = king(Helper);
        fight(Helper, King, 1);
        ScarletKingData Data = ScarletKingData.get(World.getServer());
        Data.consumed(true);
        Data.king(King.getUUID());
        ScarletThorn.spawn(World, King, Vec3.atBottomCenterOf(Helper.absolutePos(Middle.east(4))), 5, 40, false);
        ScarletRite.reset(World, throne(Helper));
        Helper.assertTrue(ScarletState.get(World.getServer()).phase() == ScarletState.Phase.Xk2, "the wipe did not return to XK-2");
        Helper.assertFalse(King.isAlive() && !King.isRemoved(), "the King survived the wipe");
        Helper.assertFalse(Data.consumed(), "999 is still marked consumed");
        Helper.assertTrue(!World.getEntitiesOfClass(Scp999Blob.class, area(Helper)).isEmpty(), "999 was not restored");
        Helper.assertTrue(World.getEntitiesOfClass(ScarletThorn.class, area(Helper)).isEmpty(), "thorns were left behind");
        finish(Helper);
    }

    private static List<ScarletKing> kings(GameTestHelper Helper) {
        return Helper.getLevel().getEntitiesOfClass(ScarletKing.class, area(Helper), Each -> !Each.isRemoved());
    }

    public static final class Mortal extends FakePlayer {
        public boolean Dead;

        public Mortal(ServerLevel World, GameProfile Profile) {
            super(World, Profile);
        }

        @Override
        public boolean isAlive() {
            return !Dead && !isRemoved();
        }

        @Override
        public boolean isDeadOrDying() {
            return Dead;
        }
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scarletking_bar")
    public static void oneKingAndOneBarThroughDeathRespawnAndWipe(GameTestHelper Helper) {
        sweep(Helper);
        useCourt(null);
        ServerLevel World = Helper.getLevel();
        ScarletKingData Data = ScarletKingData.get(World.getServer());
        ScarletKing King = king(Helper);
        fight(Helper, King, 1);
        Data.king(King.getUUID());
        Vec3 Near = Vec3.atBottomCenterOf(Helper.absolutePos(Middle.east(5)));
        GameProfile Profile = new GameProfile(UUID.randomUUID(), "scarlet_bar");
        Mortal Fighter = new Mortal(World, Profile);
        Fighter.moveTo(Near.x, Near.y, Near.z, 0.0f, 0.0f);
        World.addNewPlayer(Fighter);
        King.refreshBar(World);
        net.minecraft.server.level.ServerBossEvent Bar = King.bar();
        Helper.assertTrue(Bar != null && Bar == ScarletKingBar.current(), "the King does not use the fight's bar");
        Helper.assertTrue(Bar.getPlayers().size() == 1 && Bar.getPlayers().contains(Fighter), "the bar does not hold exactly the fighter");
        Fighter.Dead = true;
        ScarletKingEvents.died(new net.neoforged.neoforge.event.entity.living.LivingDeathEvent(Fighter, World.damageSources().genericKill()));
        Helper.assertTrue(Bar.getPlayers().isEmpty(), "a dead player is still on the bar");
        King.refreshBar(World);
        Helper.assertTrue(Bar.getPlayers().isEmpty(), "a dead player came back onto the bar");
        World.removePlayerImmediately(Fighter, Entity.RemovalReason.KILLED);
        Mortal Reborn = new Mortal(World, Profile);
        Reborn.moveTo(Near.x, Near.y, Near.z, 0.0f, 0.0f);
        World.addNewPlayer(Reborn);
        King.refreshBar(World);
        King.refreshBar(World);
        Helper.assertTrue(Bar == ScarletKingBar.current() && King.bar() == Bar, "respawning made a second bar");
        Helper.assertTrue(Bar.getPlayers().size() == 1 && Bar.getPlayers().contains(Reborn), "the respawned fighter is not on the bar exactly once, " + Bar.getPlayers().size());
        Helper.assertTrue(kings(Helper).size() == 1, "there are " + kings(Helper).size() + " Kings after the respawn");
        ScarletKing Rogue = king(Helper);
        Helper.runAfterDelay(3, () -> {
            Helper.assertTrue(Rogue.isRemoved(), "a second King of the same fight survived");
            Helper.assertTrue(kings(Helper).size() == 1 && kings(Helper).get(0) == King, "the fight does not have exactly one King");
            ScarletRite.reset(World, throne(Helper));
            Helper.assertTrue(Bar.getPlayers().isEmpty(), "the wipe left players on the bar");
            Helper.assertTrue(kings(Helper).isEmpty(), "the wipe left a King behind");
            ScarletKing Next = king(Helper);
            fight(Helper, Next, 1);
            Data.king(Next.getUUID());
            Next.refreshBar(World);
            net.minecraft.server.level.ServerBossEvent Fresh = Next.bar();
            Helper.assertTrue(Fresh != null && Fresh == ScarletKingBar.current(), "the next fight has no bar");
            Helper.assertTrue(Fresh.getPlayers().size() == 1 && Fresh.getPlayers().contains(Reborn), "the next fight's bar does not hold exactly the fighter");
            Helper.assertTrue(Fresh != Bar && Bar.getPlayers().isEmpty(), "the old fight's bar is still shown");
            Helper.assertTrue(kings(Helper).size() == 1, "the next fight does not have exactly one King");
            Next.clearBar();
            World.removePlayerImmediately(Reborn, Entity.RemovalReason.DISCARDED);
            finish(Helper);
        });
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scarletking_armor")
    public static void scarletArmourBeatsNetherite(GameTestHelper Helper) {
        ArmorMaterial Scarlet = ScarletKingRegistry.Scarlet.value();
        ArmorMaterial Netherite = ArmorMaterials.NETHERITE.value();
        for (ArmorItem.Type Slot : new ArmorItem.Type[]{ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS}) {
            Helper.assertTrue(Scarlet.getDefense(Slot) > Netherite.getDefense(Slot), "the " + Slot + " is not above netherite");
        }
        Helper.assertTrue(Scarlet.toughness() > Netherite.toughness(), "toughness is not above netherite");
        Helper.assertTrue(Scarlet.knockbackResistance() > Netherite.knockbackResistance(), "knockback resistance is not above netherite");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scarletking_rifle")
    public static void rifleFiresSixtyThenRecharges(GameTestHelper Helper) {
        sweep(Helper);
        Guns2GameTests.Shooter User = new Guns2GameTests.Shooter(Helper.getLevel(), "scarlet_rifle", Helper.absoluteVec(new Vec3(2.5, 1.0, 2.5)));
        User.setItemInHand(InteractionHand.MAIN_HAND, GunItem.loaded(GunKind.ScarletRifle));
        User.setXRot(-60.0f);
        ItemStack Gun = User.getMainHandItem();
        Helper.assertTrue(GunItem.getAmmo(Gun) == ScarletRifleItem.Capacity, "a fresh rifle holds " + GunItem.getAmmo(Gun));
        int[] Sequence = {0};
        Helper.onEachTick(() -> Gun.inventoryTick(User.level(), User, User.getInventory().selected, true));
        Helper.startSequence()
                .thenExecuteFor(130, () -> GunFiring.requestFire(User, GunKind.ScarletRifle.ordinal(), Sequence[0]++))
                .thenExecute(() -> {
                    Helper.assertTrue(GunItem.getAmmo(Gun) == 0, "the cell still holds " + GunItem.getAmmo(Gun));
                    Helper.assertTrue(ScarletRifleItem.recharging(Gun), "the empty rifle is not recharging");
                    GunFiring.requestFire(User, GunKind.ScarletRifle.ordinal(), Sequence[0]++);
                    Helper.assertTrue(GunItem.getAmmo(Gun) == 0, "the rifle fired while recharging");
                })
                .thenExecuteAfter(ScarletRifleItem.EmptyCooldown + 4, () -> {
                    Helper.assertTrue(GunItem.getAmmo(Gun) == ScarletRifleItem.Capacity, "the recharged cell holds " + GunItem.getAmmo(Gun));
                    Helper.assertFalse(ScarletRifleItem.recharging(Gun), "the recharge never ended");
                    User.discard();
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scarletking_pod")
    public static void podHatchesNinehundred(GameTestHelper Helper) {
        sweep(Helper);
        ServerLevel World = Helper.getLevel();
        BlockPos Pos = Helper.absolutePos(Middle);
        World.setBlock(Pos, ScarletKingRegistry.Pod.get().defaultBlockState(), Block.UPDATE_ALL);
        Helper.assertTrue(World.getBlockEntity(Pos) instanceof ScarletPodBlockEntity, "the pod has no block entity");
        ScarletPodBlockEntity Pod = (ScarletPodBlockEntity) World.getBlockEntity(Pos);
        Pod.start(World.getGameTime() - ScarletKingConfig.podTicks() + 10);
        Helper.startSequence()
                .thenExecuteAfter(30, () -> {
                    Helper.assertFalse(World.getBlockState(Pos).is(ScarletKingRegistry.Pod.get()), "the pod never hatched, state " + World.getBlockState(Pos)
                            + " start " + (World.getBlockEntity(Pos) instanceof ScarletPodBlockEntity Left ? Left.start() + " progress " + Left.progress(World.getGameTime()) : "none")
                            + " now " + World.getGameTime() + " ticks " + ScarletKingConfig.podTicks());
                    Helper.assertTrue(!World.getEntitiesOfClass(Scp999Blob.class, new AABB(Pos).inflate(3.0)).isEmpty(), "no 999 hatched from the pod");
                })
                .thenExecute(() -> finish(Helper));
    }
}
