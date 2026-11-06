package net.rai.scp_unleashed.daevite;

import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.scp140.api.DaeviteTags;
import net.rai.scp_unleashed.scp268.Scp268;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DaeviteTargets {
    public static final long ProvokedTicks = 2400L;
    private static final EquipmentSlot[] Robed = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final Map<UUID, Long> Provoked = new ConcurrentHashMap<>();

    private DaeviteTargets() {
    }

    public static boolean isAlly(@Nullable Entity Other) {
        return Other != null && Other.getType().is(DaeviteTags.Allies);
    }

    public static boolean isDaevite(@Nullable Entity Other) {
        return Other != null && Other.getType().is(DaeviteTags.Daevites);
    }

    public static boolean isPrey(Entity Hunter, @Nullable Entity Found, boolean SeesThroughRobes) {
        if (!(Found instanceof LivingEntity Body) || Body == Hunter || isAlly(Body)) {
            return false;
        }
        if (!Body.isAlive() || Body.isRemoved() || Body.isDeadOrDying() || Body.isSpectator() || !Targeting.isTargetable(Body) || Hunter.level().getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        if (Body instanceof Player User) {
            if (User.isCreative() || Scp268.isNeglected(User)) {
                return false;
            }
            return SeesThroughRobes || !disguised(User) || provoked(User);
        }
        if (Body instanceof AbstractVillager || Body instanceof IronGolem) {
            return true;
        }
        return ControlBodies.isPersonLike(Body);
    }

    public static boolean disguised(Player User) {
        for (EquipmentSlot Slot : Robed) {
            if (!User.getItemBySlot(Slot).is(DaeviteTags.Robes)) {
                return false;
            }
        }
        return true;
    }

    public static void provoke(Player User) {
        Provoked.put(User.getUUID(), User.level().getGameTime() + ProvokedTicks);
    }

    public static boolean provoked(Player User) {
        Long Until = Provoked.get(User.getUUID());
        if (Until == null) {
            return false;
        }
        if (Until < User.level().getGameTime()) {
            Provoked.remove(User.getUUID());
            return false;
        }
        return true;
    }

    public static void forget() {
        Provoked.clear();
    }
}
