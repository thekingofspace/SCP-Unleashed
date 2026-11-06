package net.rai.scp_unleashed.health.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.health.HealthGuard;
import net.rai.scp_unleashed.health.LethalDamage;
import net.rai.scp_unleashed.scp096.Scp096Registry;
import net.rai.scp_unleashed.scp173.Scp173Registry;
import net.rai.scp_unleashed.scp330.Scp330Registry;
import net.rai.scp_unleashed.scp682.Scp682Registry;

import java.util.List;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class HealthGameTests {
    public static final String Arena = "gear_arena";
    private static final List<ResourceKey<DamageType>> Kills = List.of(Scp096Registry.Caught, Scp173Registry.Snap, Scp682Registry.CommandDamage);

    private HealthGameTests() {
    }

    private static FakePlayer victim(GameTestHelper Helper, String Name) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = new FakePlayer(World, new GameProfile(UUID.randomUUID(), Name)) {
            @Override
            public boolean isInvulnerableTo(DamageSource Source) {
                return false;
            }
        };
        Vec3 Spot = Helper.absoluteVec(new Vec3(2.5, 2.0, 2.5));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        User.setGameMode(GameType.SURVIVAL);
        ObfuscationReflectionHelper.setPrivateValue(ServerPlayer.class, User, 0, "spawnInvulnerableTime");
        return User;
    }

    private static void dismiss(GameTestHelper Helper, ServerPlayer User) {
        if (!User.isRemoved()) {
            Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
        }
    }

    private static void slain(GameTestHelper Helper, ServerPlayer User, String Case) {
        Helper.assertFalse(Float.isNaN(User.getHealth()) || Float.isInfinite(User.getHealth()), Case + ": health became " + User.getHealth());
        Helper.assertFalse(Float.isNaN(User.getAbsorptionAmount()), Case + ": absorption became NaN");
        Helper.assertTrue(User.isDeadOrDying(), Case + ": the player survived with health " + User.getHealth());
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "nanhealth_anomalyKills")
    public static void anomalyKills(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Zombie Killer = Helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(5.5, 2.0, 5.5));
        Killer.addEffect(new MobEffectInstance(Scp330Registry.Wrath, 400));
        try {
            for (ResourceKey<DamageType> Kind : Kills) {
                for (boolean Armoured : List.of(false, true)) {
                    ServerPlayer User = victim(Helper, "nanhealth_" + Kind.location().getPath());
                    try {
                        if (Armoured) {
                            User.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
                            User.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                            User.setAbsorptionAmount(8.0f);
                        }
                        float Amount = LethalDamage.against(User);
                        Helper.assertTrue(Float.isFinite(Amount * 5.0f * 1.5f), "the lethal amount has no headroom: " + Amount);
                        User.hurt(World.damageSources().source(Kind, Killer), Amount);
                        slain(Helper, User, Kind.location() + (Armoured ? " in armour" : ""));
                    } finally {
                        dismiss(Helper, User);
                    }
                }
            }
        } finally {
            Killer.discard();
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "nanhealth_overflowGuard")
    public static void overflowGuard(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Difficulty Before = World.getDifficulty();
        Zombie Killer = Helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(5.5, 2.0, 5.5));
        try {
            Killer.addEffect(new MobEffectInstance(Scp330Registry.Wrath, 400));
            ServerPlayer Wrathful = victim(Helper, "nanhealth_wrath");
            try {
                Wrathful.hurt(World.damageSources().source(Scp173Registry.Snap, Killer), Float.MAX_VALUE);
                slain(Helper, Wrathful, "a Float.MAX_VALUE snap from an attacker under SCP-330 wrath");
            } finally {
                dismiss(Helper, Wrathful);
            }
            Killer.removeAllEffects();
            World.getServer().setDifficulty(Difficulty.HARD, true);
            ServerPlayer Scaled = victim(Helper, "nanhealth_hard");
            try {
                Scaled.hurt(World.damageSources().mobAttack(Killer), Float.MAX_VALUE);
                slain(Helper, Scaled, "a Float.MAX_VALUE mob attack scaled up on Hard");
            } finally {
                dismiss(Helper, Scaled);
            }
        } finally {
            World.getServer().setDifficulty(Before, true);
            Killer.discard();
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "nanhealth_repair")
    public static void repair(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Helper.assertTrue(HealthGuard.safeDamage(5.0f, Float.POSITIVE_INFINITY) == Float.MAX_VALUE, "infinite damage was not capped");
        Helper.assertTrue(HealthGuard.safeDamage(5.0f, Float.NEGATIVE_INFINITY) == 0.0f, "negative infinite damage was not dropped");
        Helper.assertTrue(HealthGuard.safeDamage(5.0f, Float.NaN) == 5.0f, "NaN damage did not fall back to the original amount");
        Helper.assertTrue(HealthGuard.safeDamage(Float.POSITIVE_INFINITY, Float.NaN) == Float.MAX_VALUE, "NaN from an infinite blow was not kept lethal");
        Helper.assertTrue(HealthGuard.safeDamage(Float.NaN, Float.NaN) == 0.0f, "NaN from a NaN blow was not dropped");
        Helper.assertTrue(HealthGuard.safeDamage(3.0f, 2.5f) == 2.5f, "finite damage was changed");
        ServerPlayer Loaded = victim(Helper, "nanhealth_loaded");
        try {
            CompoundTag Saved = new CompoundTag();
            Loaded.saveWithoutId(Saved);
            Saved.putFloat("Health", Float.NaN);
            Saved.putFloat("AbsorptionAmount", Float.NaN);
            Loaded.load(Saved);
            Helper.assertTrue(Float.isNaN(Loaded.getHealth()), "the softlocked save did not load as NaN");
            Helper.assertTrue(HealthGuard.repair(Loaded, false) == HealthGuard.Repair.Restored, "a NaN save was not restored");
            Helper.assertTrue(Loaded.getHealth() == Loaded.getMaxHealth(), "a NaN save did not come back at full health: " + Loaded.getHealth());
            Helper.assertTrue(Loaded.getAbsorptionAmount() == 0.0f, "NaN absorption was not cleared");
            Helper.assertTrue(Loaded.isAlive(), "the repaired player is not alive");
            CompoundTag Clean = new CompoundTag();
            Loaded.saveWithoutId(Clean);
            Helper.assertTrue(Float.isFinite(Clean.getFloat("Health")) && Float.isFinite(Clean.getFloat("AbsorptionAmount")), "the repaired player still saves broken health");
            Helper.assertTrue(HealthGuard.repair(Loaded, false) == HealthGuard.Repair.None, "a healthy player was repaired");
        } finally {
            dismiss(Helper, Loaded);
        }
        ServerPlayer Stuck = victim(Helper, "nanhealth_stuck");
        try {
            Stuck.hurt(World.damageSources().generic(), 1.0f);
            Stuck.setHealth(Float.NaN);
            Helper.assertTrue(Float.isNaN(Stuck.getHealth()) && !Stuck.isDeadOrDying() && !Stuck.isAlive(), "the softlock could not be staged");
            Helper.assertTrue(HealthGuard.repair(Stuck, true) == HealthGuard.Repair.Finished, "a player stuck after a blow was not let die");
            Helper.assertTrue(Stuck.getHealth() == 0.0f && Stuck.isDeadOrDying(), "the stuck player did not finish dying");
        } finally {
            dismiss(Helper, Stuck);
        }
        Helper.succeed();
    }
}
