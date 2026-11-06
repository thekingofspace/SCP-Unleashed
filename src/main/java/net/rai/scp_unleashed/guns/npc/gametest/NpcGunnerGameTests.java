package net.rai.scp_unleashed.guns.npc.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.guns.npc.NpcAmmoSource;
import net.rai.scp_unleashed.guns.npc.NpcGunner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class NpcGunnerGameTests {
    public static final String Arena = "npc_gunner_arena";
    public static final double Lane = 4.5;
    public static final double ShooterX = 2.5;
    public static final double TargetX = 14.5;
    public static final float Sturdy = 400.0F;

    private NpcGunnerGameTests() {
    }

    public static int stand(GameTestHelper Helper) {
        for (int Y = 0; Y < 4; Y++) {
            if (Helper.getBlockState(new BlockPos(1, Y, 1)).is(Blocks.SMOOTH_STONE) && Helper.getBlockState(new BlockPos(1, Y + 1, 1)).isAir()) {
                return Y + 1;
            }
        }
        return 1;
    }

    private static Husk gunner(GameTestHelper Helper, ItemStack Gun) {
        Husk Shooter = Helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(ShooterX, stand(Helper), Lane));
        Shooter.setItemSlot(EquipmentSlot.MAINHAND, Gun);
        return Shooter;
    }

    private static Husk target(GameTestHelper Helper, double X, double Z) {
        Husk Target = Helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(X, stand(Helper), Z));
        Target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(Sturdy);
        Target.setHealth(Sturdy);
        return Target;
    }

    private static NpcAmmoSource pool(int[] Pool, GunKind Kind) {
        return (AmmoItem, Wanted) -> {
            if (AmmoItem != Kind.getAmmo()) {
                return 0;
            }
            int Given = Math.min(Wanted, Pool[0]);
            Pool[0] -= Given;
            return Given;
        };
    }

    @GameTestGenerator
    public static Collection<TestFunction> eachKind() {
        List<TestFunction> Tests = new ArrayList<>();
        for (GunKind Kind : GunKind.values()) {
            if (Kind == GunKind.Ultralaser || Kind == GunKind.MicroHid || Kind == GunKind.Flamethrower || Kind == GunKind.ScarletRifle) {
                continue;
            }
            String Name = "npcgunner_hits_" + Kind.getName();
            Tests.add(new TestFunction(Name, Name, Scp_unleashed.ModId + ":" + Arena, 240, 0L, true, Helper -> hits(Helper, Kind)));
        }
        return Tests;
    }

    private static void hits(GameTestHelper Helper, GunKind Kind) {
        ItemStack Gun = GunItem.loaded(Kind);
        Husk Shooter = gunner(Helper, Gun);
        Husk Target = target(Helper, TargetX, Lane);
        Helper.assertTrue(NpcGunner.isGun(Gun), "not recognised as a gun");
        Helper.assertTrue(NpcGunner.range(Gun) >= 12.0, "range too short");
        int[] Fired = {0};
        int[] Pool = {64};
        NpcAmmoSource Ammo = pool(Pool, Kind);
        Helper.onEachTick(() -> {
            if (Target.isAlive() && NpcGunner.fireAt(Shooter, Shooter.getMainHandItem(), Target, Ammo, 1.0F)) {
                Fired[0]++;
            }
        });
        Helper.succeedWhen(() -> {
            Helper.assertTrue(Fired[0] > 0, "never fired");
            Helper.assertTrue(!Target.isAlive() || Target.getHealth() < Sturdy, "target unhurt");
            Helper.assertTrue(Shooter.isAlive(), "shooter died");
            Helper.assertTrue(Target.getLastHurtByMob() == Shooter, "damage not credited to the shooter");
        });
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "npcgunner_reloads")
    public static void reloadsFromSource(GameTestHelper Helper) {
        ItemStack Gun = GunItem.loaded(GunKind.Pistol);
        GunItem.setAmmo(Gun, 2);
        Husk Shooter = gunner(Helper, Gun);
        Husk Target = target(Helper, TargetX, Lane);
        int[] Pool = {5};
        NpcAmmoSource Ammo = pool(Pool, GunKind.Pistol);
        int Interval = GunKind.Pistol.getStats().getInterval();
        int Reload = GunKind.Pistol.getStats().getReloadTicks();
        Helper.startSequence()
                .thenExecute(() -> {
                    Helper.assertTrue(NpcGunner.ready(Shooter, Gun), "not ready at start");
                    Helper.assertTrue(NpcGunner.fireAt(Shooter, Gun, Target, Ammo, 1.0F), "first shot refused");
                    Helper.assertFalse(NpcGunner.fireAt(Shooter, Gun, Target, Ammo, 1.0F), "cooldown ignored");
                    Helper.assertFalse(NpcGunner.ready(Shooter, Gun), "ready during cooldown");
                    Helper.assertTrue(GunItem.getAmmo(Gun) == 1, "magazine not mirrored");
                })
                .thenExecuteAfter(Interval, () -> {
                    Helper.assertTrue(NpcGunner.fireAt(Shooter, Gun, Target, Ammo, 1.0F), "second shot refused");
                    Helper.assertTrue(Pool[0] == 0, "reload did not draw from the source");
                    Helper.assertTrue(NpcGunner.reloading(Shooter, Gun), "no reload after emptying");
                    Helper.assertFalse(NpcGunner.ready(Shooter, Gun), "ready while reloading");
                })
                .thenExecuteAfter(Reload / 2, () -> Helper.assertFalse(NpcGunner.fireAt(Shooter, Gun, Target, Ammo, 1.0F), "fired mid reload"))
                .thenWaitUntil(() -> Helper.assertTrue(NpcGunner.fireAt(Shooter, Gun, Target, Ammo, 1.0F), "no shot after reload"))
                .thenExecute(() -> Helper.assertTrue(NpcGunner.rounds(Shooter, Gun) == 4, "wrong magazine after reload: " + NpcGunner.rounds(Shooter, Gun)))
                .thenWaitUntil(() -> {
                    NpcGunner.fireAt(Shooter, Gun, Target, Ammo, 1.0F);
                    Helper.assertTrue(NpcGunner.dry(Shooter, Gun), "not dry");
                })
                .thenExecute(() -> {
                    Helper.assertFalse(NpcGunner.fireAt(Shooter, Gun, Target, Ammo, 1.0F), "fired while dry");
                    Pool[0] = 20;
                })
                .thenWaitUntil(() -> Helper.assertTrue(NpcGunner.fireAt(Shooter, Gun, Target, Ammo, 1.0F), "never recovered from dry"))
                .thenExecute(() -> {
                    Helper.assertTrue(Pool[0] == 8, "second reload took the wrong amount: " + Pool[0]);
                    Helper.assertTrue(Target.getHealth() < Sturdy, "target unhurt");
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "npcgunner_unlimited")
    public static void unlimitedWithoutSource(GameTestHelper Helper) {
        ItemStack Gun = GunItem.loaded(GunKind.Pistol);
        GunItem.setAmmo(Gun, 1);
        Husk Shooter = gunner(Helper, Gun);
        Husk Target = target(Helper, TargetX, Lane);
        Helper.startSequence()
                .thenExecute(() -> {
                    Helper.assertTrue(NpcGunner.fireAt(Shooter, Gun, Target, null, 1.0F), "shot refused");
                    Helper.assertTrue(NpcGunner.reloading(Shooter, Gun), "no reload");
                })
                .thenWaitUntil(() -> Helper.assertTrue(NpcGunner.fireAt(Shooter, Gun, Target, null, 1.0F), "no shot after reload"))
                .thenExecute(() -> Helper.assertTrue(NpcGunner.rounds(Shooter, Gun) == GunKind.Pistol.getStats().getCapacity() - 1, "unlimited reload did not fill"))
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "npcgunner_burnout")
    public static void disruptorBurnsOut(GameTestHelper Helper) {
        ItemStack Gun = GunItem.loaded(GunKind.ParticleDisruptor);
        GunItem.setCharges(Gun, 1);
        Husk Shooter = gunner(Helper, Gun);
        Husk Target = target(Helper, TargetX, Lane);
        Helper.startSequence()
                .thenExecute(() -> Helper.assertFalse(NpcGunner.fireAt(Shooter, Gun, Target, null, 1.0F), "fired without charging"))
                .thenWaitUntil(() -> Helper.assertTrue(NpcGunner.fireAt(Shooter, Gun, Target, null, 1.0F), "never discharged"))
                .thenExecute(() -> {
                    Helper.assertTrue(Shooter.getMainHandItem().isEmpty(), "spent disruptor not burnt out");
                    Helper.assertTrue(!Target.isAlive() || Target.getHealth() < Sturdy, "target unhurt");
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "npcgunner_friendly")
    public static void holdsForFriendly(GameTestHelper Helper) {
        ItemStack Gun = GunItem.loaded(GunKind.AssaultRifle);
        Husk Shooter = gunner(Helper, Gun);
        Husk Target = target(Helper, TargetX, Lane);
        int Floor = stand(Helper);
        Villager Friend = Helper.spawnWithNoFreeWill(EntityType.VILLAGER, new Vec3(8.5, Floor, Lane));
        float FriendHealth = Friend.getHealth();
        int[] Fired = {0};
        Runnable Volley = () -> {
            if (NpcGunner.clearShot(Shooter, Target, Other -> Other instanceof Villager) && NpcGunner.fireAt(Shooter, Gun, Target, null, 1.0F)) {
                Fired[0]++;
            }
        };
        Helper.startSequence()
                .thenExecute(() -> {
                    Helper.assertFalse(NpcGunner.clearShot(Shooter, Target, Other -> Other instanceof Villager), "clear shot through a friendly");
                    Helper.assertTrue(NpcGunner.clearShot(Shooter, Target, Other -> false), "bystander rule ignored");
                })
                .thenExecuteFor(30, Volley)
                .thenExecute(() -> {
                    Helper.assertTrue(Fired[0] == 0, "fired through a friendly");
                    Helper.assertTrue(Friend.getHealth() >= FriendHealth, "friendly hurt");
                    Vec3 Aside = Helper.absoluteVec(new Vec3(8.5, Floor, 7.5));
                    Friend.teleportTo(Aside.x, Aside.y, Aside.z);
                    Helper.setBlock(new BlockPos(10, Floor, 4), Blocks.STONE);
                    Helper.setBlock(new BlockPos(10, Floor + 1, 4), Blocks.STONE);
                    Helper.setBlock(new BlockPos(10, Floor + 2, 4), Blocks.STONE);
                })
                .thenExecute(() -> Helper.assertFalse(NpcGunner.clearShot(Shooter, Target, Other -> Other instanceof Villager), "clear shot through a wall"))
                .thenExecute(() -> {
                    Helper.setBlock(new BlockPos(10, Floor, 4), Blocks.AIR);
                    Helper.setBlock(new BlockPos(10, Floor + 1, 4), Blocks.AIR);
                    Helper.setBlock(new BlockPos(10, Floor + 2, 4), Blocks.AIR);
                })
                .thenExecute(() -> Helper.assertTrue(NpcGunner.clearShot(Shooter, Target, Other -> Other instanceof Villager), "no clear shot once the lane is open "))
                .thenWaitUntil(() -> {
                    Volley.run();
                    Helper.assertTrue(Fired[0] > 0 && Target.getHealth() < Sturdy, "did not engage once clear");
                })
                .thenExecute(() -> Helper.assertTrue(Friend.getHealth() >= FriendHealth, "friendly hurt aside the lane"))
                .thenSucceed();
    }
}
