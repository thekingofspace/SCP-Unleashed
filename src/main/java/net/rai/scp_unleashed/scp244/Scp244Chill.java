package net.rai.scp_unleashed.scp244;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.rai.scp_unleashed.anomaly.Targeting;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

public final class Scp244Chill {
    public static final float MinDensity = 0.04f;
    public static final float FreezeRate = 2.4f;
    public static final int Overfreeze = 40;
    public static final float ExposureRamp = 600.0f;
    public static final float RampCap = 1.5f;
    public static final int MaxExposure = 2400;
    public static final int Recovery = 2;
    public static final int HurtGap = 20;
    public static final int HurtPhase = 10;
    public static final float HurtBase = 1.0f;
    public static final float HurtFloor = 0.5f;
    public static final float WardShare = 0.25f;
    public static final long CureGrace = 2L;
    private static final EquipmentSlot[] Worn = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.BODY};
    private static final Map<LivingEntity, Exposure> Records = new WeakHashMap<>();

    private Scp244Chill() {
    }

    private static final class Exposure {
        private long Seen = Long.MIN_VALUE;
        private float Density;
        private float Ticks;
        private float Carry;
    }

    public static boolean susceptible(LivingEntity Target) {
        if (!Target.isAlive() || Target.isSpectator() || Target instanceof ArmorStand || !Targeting.isTargetable(Target) || Target.getType().is(EntityTypeTags.FREEZE_IMMUNE_ENTITY_TYPES)) {
            return false;
        }
        return !(Target instanceof Player User) || !User.getAbilities().invulnerable;
    }

    public static void expose(LivingEntity Target, float Density, long Now) {
        Exposure Record = Records.computeIfAbsent(Target, Key -> new Exposure());
        if (Record.Seen != Now) {
            Record.Seen = Now;
            Record.Density = Density;
        } else {
            Record.Density = Math.max(Record.Density, Density);
        }
    }

    public static float ward(LivingEntity Target) {
        int Pieces = 0;
        for (EquipmentSlot Slot : Worn) {
            if (Target.getItemBySlot(Slot).is(ItemTags.FREEZE_IMMUNE_WEARABLES)) {
                Pieces++;
            }
        }
        return Math.min(1.0f, Pieces * WardShare);
    }

    public static float exposure(LivingEntity Target) {
        Exposure Record = Records.get(Target);
        return Record == null ? 0.0f : Record.Ticks;
    }

    public static float ramp(float Ticks) {
        return 1.0f + Math.min(Ticks / ExposureRamp, RampCap);
    }

    public static void apply(ServerLevel World) {
        if (Records.isEmpty()) {
            return;
        }
        long Now = World.getGameTime();
        Iterator<Map.Entry<LivingEntity, Exposure>> Entries = Records.entrySet().iterator();
        while (Entries.hasNext()) {
            Map.Entry<LivingEntity, Exposure> Entry = Entries.next();
            LivingEntity Target = Entry.getKey();
            Exposure Record = Entry.getValue();
            if (Target == null || Target.isRemoved() || !Target.isAlive()) {
                Entries.remove();
                continue;
            }
            if (Target.level() != World) {
                continue;
            }
            if (Record.Seen == Now && susceptible(Target)) {
                chill(World, Target, Record);
                continue;
            }
            Record.Ticks = Math.max(0.0f, Record.Ticks - Recovery);
            Record.Carry = 0.0f;
            if (Record.Ticks <= 0.0f) {
                Entries.remove();
            }
        }
    }

    private static void chill(ServerLevel World, LivingEntity Target, Exposure Record) {
        Record.Ticks = Math.min(MaxExposure, Record.Ticks + 1.0f);
        float Shield = ward(Target);
        if (Shield >= 1.0f) {
            Record.Carry = 0.0f;
            return;
        }
        float Ramp = ramp(Record.Ticks);
        Record.Carry += FreezeRate * Record.Density * Ramp * (1.0f - Shield);
        int Whole = (int) Record.Carry;
        Record.Carry -= Whole;
        int Frozen = Target.getTicksFrozen();
        int Cap = Target.getTicksRequiredToFreeze() + Overfreeze;
        Target.setTicksFrozen(Math.max(Frozen, Math.min(Cap, Frozen + 2 + Whole)));
        if (Target.isFullyFrozen() && Target.tickCount % HurtGap == HurtPhase) {
            float Amount = HurtBase * (HurtFloor + Record.Density) * Ramp * (1.0f - Shield);
            Target.hurt(World.damageSources().freeze(), Amount);
        }
    }

    public static void forget() {
        Records.clear();
    }

    public static boolean cure(LivingEntity Target) {
        Exposure Found = Records.remove(Target);
        return Found != null && Target.level().getGameTime() - Found.Seen > CureGrace;
    }
}
