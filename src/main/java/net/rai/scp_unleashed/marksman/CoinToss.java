package net.rai.scp_unleashed.marksman;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.guns.GunSounds;
import net.rai.scp_unleashed.guns.GunTimer;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CoinToss {
    public static final int MaxCoins = 4;
    public static final int RechargeTicks = 50;
    public static final int TossCooldown = 5;
    public static final double TossUp = 0.55;
    public static final double TossForward = 0.20;
    public static final double CarryShare = 0.8;
    public static final double HandReach = 0.45;
    public static final double HandSide = 0.15;
    public static final double HandDrop = 0.25;
    private static final Map<UUID, Long> NextToss = new HashMap<>();

    private CoinToss() {
    }

    public static int getCoins(ItemStack Stack) {
        return Stack.getOrDefault(MarksmanRegistry.Coins.get(), 0);
    }

    public static void setCoins(ItemStack Stack, int Count) {
        Stack.set(MarksmanRegistry.Coins.get(), Mth.clamp(Count, 0, MaxCoins));
    }

    @Nullable
    public static GunTimer getCharge(ItemStack Stack) {
        return Stack.get(MarksmanRegistry.CoinCharge.get());
    }

    public static void recharge(ItemStack Stack, long Now) {
        int Coins = getCoins(Stack);
        GunTimer Charge = getCharge(Stack);
        if (Coins >= MaxCoins) {
            if (Charge != null) {
                Stack.remove(MarksmanRegistry.CoinCharge.get());
            }
            return;
        }
        if (Charge == null || Charge.Start() > Now || Charge.End() - Now > RechargeTicks) {
            Stack.set(MarksmanRegistry.CoinCharge.get(), new GunTimer(Now, Now + RechargeTicks));
            return;
        }
        if (!Charge.isDone(Now)) {
            return;
        }
        setCoins(Stack, Coins + 1);
        if (Coins + 1 < MaxCoins) {
            Stack.set(MarksmanRegistry.CoinCharge.get(), new GunTimer(Now, Now + RechargeTicks));
        } else {
            Stack.remove(MarksmanRegistry.CoinCharge.get());
        }
    }

    public static boolean toss(ServerPlayer User, ItemStack Stack) {
        ServerLevel World = User.serverLevel();
        long Now = World.getGameTime();
        Long Ready = NextToss.get(User.getUUID());
        if (Ready != null && Now < Ready) {
            return false;
        }
        NextToss.put(User.getUUID(), Now + TossCooldown);
        int Coins = getCoins(Stack);
        if (Coins <= 0) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.marksman.no_coins").withStyle(ChatFormatting.GOLD), true);
            GunSounds.play(World, null, User.getX(), User.getEyeY(), User.getZ(), CoinSounds.empty(), 1.0F);
            return false;
        }
        setCoins(Stack, Coins - 1);
        if (getCharge(Stack) == null) {
            Stack.set(MarksmanRegistry.CoinCharge.get(), new GunTimer(Now, Now + RechargeTicks));
        }
        Vec3 Eye = User.getEyePosition();
        Vec3 Look = User.getLookAngle();
        Vec3 Flat = Vec3.directionFromRotation(0.0F, User.getYRot());
        Vec3 Right = new Vec3(-Flat.z, 0.0, Flat.x);
        double Side = User.getMainArm() == HumanoidArm.LEFT ? -HandSide : HandSide;
        Vec3 Start = Eye.add(Look.scale(HandReach)).add(Right.scale(Side)).add(0.0, -HandDrop, 0.0);
        BlockHitResult Blocked = World.clip(new ClipContext(Eye, Start, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, User));
        if (Blocked.getType() != HitResult.Type.MISS) {
            Start = Blocked.getLocation().add(Eye.subtract(Blocked.getLocation()).normalize().scale(0.2));
        }
        Vec3 Carry = User.getKnownMovement();
        Vec3 Velocity = Look.scale(TossForward).add(Carry.x * CarryShare, TossUp, Carry.z * CarryShare);
        CoinEntity Coin = new CoinEntity(World, Start, Velocity, User.getYRot());
        World.addFreshEntity(Coin);
        GunSounds.play(World, null, Start.x, Start.y, Start.z, CoinSounds.flip(), 1.0F);
        return true;
    }

    public static void forget(UUID Id) {
        NextToss.remove(Id);
    }

    public static void forgetAll() {
        NextToss.clear();
    }
}
