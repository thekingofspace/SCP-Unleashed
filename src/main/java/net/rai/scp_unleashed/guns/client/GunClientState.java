package net.rai.scp_unleashed.guns.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.guns.GunSounds;
import net.rai.scp_unleashed.guns.GunStats;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public final class GunClientState {
    public static final int RecentShot = 30;
    public static final int HitMarkerTicks = 6;
    public static final float KickDecay = 0.45F;
    public static final float ScopeStart = 0.7F;
    public static final float RecoverShare = 0.4F;
    public static final float RecoverSpeed = 20.0F;
    public static final double RecoverDelay = 0.12;
    public static final double RecoilResponse = 0.03;
    public static final double WalkSpeed = 0.2;
    public static final float CrouchSteadiness = 0.75F;
    public static final float EquipTicks = 4.0F;
    private static final GunAmmoPredictor Rounds = new GunAmmoPredictor();
    private static final Map<Integer, Long> ShotTicks = new HashMap<>();
    private static long Ticks;
    private static float Aim;
    private static float AimO;
    private static float Kick;
    private static float KickO;
    private static float Lowered;
    private static float LoweredO;
    private static float Sprint;
    private static float SprintO;
    private static float Bloom;
    private static float PendingPitch;
    private static float PendingYaw;
    private static float Recover;
    private static long LastFrame;
    private static double LastShotSeconds;
    private static long LocalShotTick = Long.MIN_VALUE / 2;
    private static int LocalStreak;
    private static int EquipSlot = -1;
    @Nullable
    private static Item EquipItem;
    private static double EquipStart;
    @Nullable
    private static GunKind AimKind;
    private static long HitTick = -100L;
    private static boolean HitKill;
    private static long FlashTick = -100L;

    private GunClientState() {
    }

    public static long ticks() {
        return Ticks;
    }

    public static void tick(Minecraft Game) {
        Ticks++;
        AimO = Aim;
        KickO = Kick;
        LoweredO = Lowered;
        SprintO = Sprint;
        LocalPlayer User = Game.player;
        ItemStack Stack = User == null ? ItemStack.EMPTY : User.getMainHandItem();
        GunKind Kind = GunItem.kindOf(Stack);
        boolean Reloading = Kind != null && GunItem.getReload(Stack) != null;
        boolean Aiming = Kind != null && User.isUsingItem() && User.getUsedItemHand() == InteractionHand.MAIN_HAND && !Reloading;
        if (Kind != null) {
            AimKind = Kind;
        }
        float Step = 1.0F / (Kind == null ? 4 : Kind.getStats().getAimTicks());
        Aim = approach(Aim, Aiming ? 1.0F : 0.0F, Kind == null ? 0.34F : Step);
        if (Kind == null && Aim <= 0.0F) {
            AimKind = null;
        }
        Kick *= KickDecay;
        if (Kick < 0.002F) {
            Kick = 0.0F;
        }
        Lowered = approach(Lowered, Reloading ? 1.0F : 0.0F, 0.2F);
        Sprint = approach(Sprint, User != null && User.isSprinting() && !Aiming && Kind != null ? 1.0F : 0.0F, 0.2F);
        if (Kind != null) {
            Bloom = Math.max(0.0F, Bloom - Kind.getStats().getBloomRecovery());
        } else {
            Bloom = 0.0F;
        }
        Iterator<Map.Entry<Integer, Long>> Entries = ShotTicks.entrySet().iterator();
        while (Entries.hasNext()) {
            if (Ticks - Entries.next().getValue() > RecentShot * 4L) {
                Entries.remove();
            }
        }
    }

    public static void reset() {
        Rounds.reset();
        ShotTicks.clear();
        Aim = AimO = Kick = KickO = Lowered = LoweredO = Sprint = SprintO = Bloom = 0.0F;
        PendingPitch = PendingYaw = Recover = 0.0F;
        EquipSlot = -1;
        EquipItem = null;
        AimKind = null;
    }

    public static float aim(float Partial) {
        return Mth.lerp(Partial, AimO, Aim);
    }

    public static float aimEased(float Partial) {
        return smooth(aim(Partial));
    }

    public static float kick(float Partial) {
        return Mth.lerp(Partial, KickO, Kick);
    }

    public static float lowered(float Partial) {
        return smooth(Mth.lerp(Partial, LoweredO, Lowered));
    }

    public static float sprint(float Partial) {
        return smooth(Mth.lerp(Partial, SprintO, Sprint));
    }

    public static float scope(float Partial) {
        if (AimKind == null || !AimKind.getStats().isScoped()) {
            return 0.0F;
        }
        return smooth((aim(Partial) - ScopeStart) / (1.0F - ScopeStart));
    }

    public static boolean hidesGun(float Partial) {
        return scope(Partial) >= 0.98F;
    }

    public static float zoom(float Partial) {
        if (AimKind == null) {
            return 1.0F;
        }
        GunStats Stats = AimKind.getStats();
        float Weight = Stats.isScoped() ? scope(Partial) : aimEased(Partial);
        return 1.0F + (Stats.getAimZoom() - 1.0F) * Weight;
    }

    public static int predictedAmmo(LocalPlayer User, ItemStack Stack) {
        GunKind Kind = GunItem.kindOf(Stack);
        int Owner = Kind == null ? -1 : User.getInventory().selected * GunKind.values().length + Kind.ordinal();
        return Rounds.display(Owner, GunItem.getAmmo(Stack), Ticks);
    }

    public static int fireSequence(boolean Predict) {
        return Rounds.fire(Ticks, Predict);
    }

    public static void acknowledge(int Sequence, int Ammo) {
        Rounds.acknowledge(Sequence, Ammo);
    }

    public static float equipProgress(LocalPlayer User, ItemStack Rendered, float Vanilla, float Partial) {
        ItemStack Current = User.getMainHandItem();
        if (!ItemStack.isSameItem(Rendered, Current)) {
            EquipSlot = -1;
            return Vanilla;
        }
        double Now = Ticks + Partial;
        int Slot = User.getInventory().selected;
        if (Slot != EquipSlot || Current.getItem() != EquipItem) {
            EquipSlot = Slot;
            EquipItem = Current.getItem();
            EquipStart = Now;
        }
        return 1.0F - smooth((float) ((Now - EquipStart) / EquipTicks));
    }

    public static void onLocalShot(LocalPlayer User, GunKind Kind) {
        GunStats Stats = Kind.getStats();
        kickCamera(User, Stats);
        Bloom = Math.min(Stats.getBloomLimit(), Bloom + Stats.getBloomPerShot());
        ShotTicks.put(User.getId(), Ticks);
        FlashTick = Ticks;
        LocalStreak = Stats.isAutomatic() && Ticks - LocalShotTick <= Stats.getInterval() * 2L ? LocalStreak + 1 : 0;
        LocalShotTick = Ticks;
        GunClientSounds.play(GunSounds.burst(Kind, LocalStreak), 1.0F, GunSounds.jitter(LocalStreak));
    }

    public static void onShotSeen(int ShooterId, GunKind Kind, @Nullable LocalPlayer User) {
        ShotTicks.put(ShooterId, Ticks);
        if (User != null && User.getId() == ShooterId && Kind.getStats().usesCharges()) {
            kickCamera(User, Kind.getStats());
            FlashTick = Ticks;
        }
    }

    private static void kickCamera(LocalPlayer User, GunStats Stats) {
        float Steady = (Aim > 0.5F ? 0.7F : 1.0F) * (User.isCrouching() ? Stats.getCrouchRecoil() : 1.0F);
        PendingPitch += Stats.getRecoilPitch() * Steady * (0.9F + User.getRandom().nextFloat() * 0.2F);
        PendingYaw += Stats.getRecoilYaw() * Steady * (User.getRandom().nextFloat() * 2.0F - 1.0F);
        Kick = Math.min(1.0F, Kick + 1.0F);
        LastShotSeconds = System.nanoTime() / 1.0E9;
    }

    public static void onHit(boolean Kill) {
        HitKill = Kill || Ticks - HitTick <= 1 && HitKill;
        HitTick = Ticks;
    }

    public static float hitMarker(float Partial) {
        float Age = Ticks + Partial - HitTick;
        return Age < 0.0F || Age > HitMarkerTicks ? 0.0F : 1.0F - Age / HitMarkerTicks;
    }

    public static boolean hitKill() {
        return HitKill;
    }

    public static float shotAge(int EntityId, float Partial) {
        Long At = ShotTicks.get(EntityId);
        return At == null ? Float.MAX_VALUE : Ticks + Partial - At;
    }

    public static Set<Integer> shooters() {
        return Collections.unmodifiableSet(ShotTicks.keySet());
    }

    public static boolean firedRecently(int EntityId) {
        Long At = ShotTicks.get(EntityId);
        return At != null && Ticks - At <= RecentShot;
    }

    public static float flash(int EntityId, float Partial) {
        Long At = ShotTicks.get(EntityId);
        if (At == null) {
            return 0.0F;
        }
        float Age = Ticks + Partial - At;
        return Age < 0.0F || Age >= 2.0F ? 0.0F : 1.0F - Age / 2.0F;
    }

    public static float localFlash(float Partial) {
        float Age = Ticks + Partial - FlashTick;
        return Age < 0.0F || Age >= 2.0F ? 0.0F : 1.0F - Age / 2.0F;
    }

    public static float spread(LocalPlayer User, GunKind Kind, float Partial) {
        GunStats Stats = Kind.getStats();
        float Aiming = aimEased(Partial);
        float Base = Mth.lerp(Aiming, Stats.getHipSpread(), Stats.getAimSpread());
        float Spread = Bloom;
        if (User.isCrouching()) {
            Base *= Stats.getCrouchSpread();
            Spread *= Stats.getCrouchBloom();
        }
        Vec3 Motion = User.getDeltaMovement();
        float Moving = (float) Mth.clamp(Math.sqrt(Motion.x * Motion.x + Motion.z * Motion.z) / WalkSpeed, 0.0, 1.0) * Stats.getMovePenalty() * (1.0F - 0.5F * Aiming);
        float Airborne = User.onGround() || User.isPassenger() ? 0.0F : Stats.getAirPenalty();
        return Base + Moving + Airborne + Spread;
    }

    public static void applyRecoil(@Nullable LocalPlayer User) {
        long Now = System.nanoTime();
        double Elapsed = LastFrame == 0L ? 0.0 : Math.min(0.1, (Now - LastFrame) / 1.0E9);
        LastFrame = Now;
        if (User == null) {
            PendingPitch = PendingYaw = Recover = 0.0F;
            return;
        }
        if (PendingPitch != 0.0F || PendingYaw != 0.0F) {
            float Portion = (float) (1.0 - Math.exp(-Elapsed / RecoilResponse));
            float Pitch = PendingPitch * Portion;
            float Yaw = PendingYaw * Portion;
            PendingPitch -= Pitch;
            PendingYaw -= Yaw;
            if (Math.abs(PendingPitch) < 0.001F && Math.abs(PendingYaw) < 0.001F) {
                PendingPitch = PendingYaw = 0.0F;
            }
            turn(User, Yaw, -Pitch);
            Recover += Pitch * RecoverShare;
        } else if (Recover > 0.0F && Now / 1.0E9 - LastShotSeconds > RecoverDelay) {
            float Back = Math.min(Recover, (float) (Elapsed * RecoverSpeed));
            Recover -= Back;
            turn(User, 0.0F, Back);
        }
    }

    private static void turn(LocalPlayer User, float Yaw, float Pitch) {
        User.setYRot(User.getYRot() + Yaw);
        User.setXRot(Mth.clamp(User.getXRot() + Pitch, -90.0F, 90.0F));
        User.yRotO += Yaw;
        User.xRotO = Mth.clamp(User.xRotO + Pitch, -90.0F, 90.0F);
    }

    public static float approach(float Value, float Target, float Step) {
        return Value < Target ? Math.min(Target, Value + Step) : Math.max(Target, Value - Step);
    }

    public static float smooth(float Value) {
        float Clamped = Mth.clamp(Value, 0.0F, 1.0F);
        return Clamped * Clamped * (3.0F - 2.0F * Clamped);
    }
}
