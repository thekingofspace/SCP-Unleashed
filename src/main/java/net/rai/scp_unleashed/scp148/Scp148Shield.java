package net.rai.scp_unleashed.scp148;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.control.ControlBodies;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class Scp148Shield {
    public static final double Radius = 18.0;
    public static final double PoweredReach = 3.0;
    public static final double PoweredRadius = Radius * PoweredReach;
    public static final int QuietTicks = 1200;
    private static final Map<LivingEntity, Long> Noticed = Collections.synchronizedMap(new WeakHashMap<>());

    private Scp148Shield() {
    }

    public static boolean isShielded(@Nullable LivingEntity Target) {
        return Target != null && (wearsHelmet(Target) || isWarded(Target));
    }

    public static boolean blocks(@Nullable LivingEntity Target) {
        if (!isShielded(Target)) {
            return false;
        }
        notice(Target);
        return true;
    }

    public static boolean wearsHelmet(LivingEntity Target) {
        return Scp148HelmetItem.is(Target.getItemBySlot(EquipmentSlot.HEAD));
    }

    public static boolean isWarded(LivingEntity Target) {
        if (!canBeWarded(Target)) {
            return false;
        }
        Vec3 Eyes = Target.getEyePosition();
        return Scp148Index.within(Target.level(), Eyes, Radius) || Scp148Index.withinPowered(Target.level(), Eyes, PoweredRadius);
    }

    public static boolean canBeWarded(LivingEntity Target) {
        return Target instanceof Player || ControlBodies.isPersonLike(Target);
    }

    @Nullable
    public static BlockPos ward(LivingEntity Target) {
        return canBeWarded(Target) ? Scp148Index.nearest(Target.level(), Target.getEyePosition(), Radius) : null;
    }

    @Nullable
    public static BlockPos poweredWard(LivingEntity Target) {
        return canBeWarded(Target) ? Scp148Index.nearestPowered(Target.level(), Target.getEyePosition(), PoweredRadius) : null;
    }

    private static void notice(LivingEntity Target) {
        long Now = Target.level().getGameTime();
        Long Last = Noticed.put(Target, Now);
        if (Last == null || Now - Last >= QuietTicks || Now < Last) {
            Scp148Cue.play(Target);
        }
    }
}
