package net.rai.scp_unleashed.raid.chaos;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.faction.FactionState;
import net.rai.scp_unleashed.faction.FactionWorld;
import net.rai.scp_unleashed.raid.Raid;
import net.rai.scp_unleashed.raid.RaidContext;
import net.rai.scp_unleashed.raid.hostile.HostileHauls;
import net.rai.scp_unleashed.raid.hostile.HostileLedger;
import net.rai.scp_unleashed.raid.hostile.HostileLevels;
import net.rai.scp_unleashed.raid.hostile.HostileRaidStyle;
import net.rai.scp_unleashed.raid.hostile.HostileRaider;
import net.rai.scp_unleashed.scp018.BallTuning;
import net.rai.scp_unleashed.scp018.Scp018Ball;
import net.rai.scp_unleashed.scp018.Scp018BallItem;
import net.rai.scp_unleashed.scp018.Scp018Registry;
import net.rai.scp_unleashed.scp127.Scp127Item;
import net.rai.scp_unleashed.scp148.Scp148HelmetItem;
import net.rai.scp_unleashed.z0003.Z0003Arming;
import net.rai.scp_unleashed.z0003.Z0003Device;
import net.rai.scp_unleashed.z0003.Z0003DeviceItem;
import net.rai.scp_unleashed.z0003.Z0003Registry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class ChaosArsenal {
    public enum Use {
        Wield,
        Wear,
        Throw,
        Carry
    }

    public static final int[] Users = {0, 0, 1, 1, 2, 2};
    public static final double CarryChance = 0.25;
    public static final int CarryMax = 2;
    public static final double ThrowMin = 6.0;
    public static final double ThrowMax = 18.0;
    public static final int ThrowRest = 100;
    public static final int FirstThrow = 60;
    public static final float FieldRadius = 3.0f;
    public static final int FieldDelay = 20;
    public static final String UsersKey = "Users";
    public static final String CarriersKey = "Carriers";

    private ChaosArsenal() {
    }

    public static Use useOf(ItemStack Stack) {
        if (Scp127Item.is(Stack)) {
            return Use.Wield;
        }
        if (Scp148HelmetItem.is(Stack)) {
            return Use.Wear;
        }
        if (Z0003DeviceItem.is(Stack) || Stack.getItem() instanceof Scp018BallItem) {
            return Use.Throw;
        }
        return Use.Carry;
    }

    public static boolean usable(ItemStack Stack) {
        return useOf(Stack) != Use.Carry;
    }

    public static void equip(RaidContext Context, HostileRaider Raider) {
        if (!(Raider instanceof ChaosInsurgent Insurgent)) {
            return;
        }
        CompoundTag Data = Context.data();
        int Level = HostileLevels.index(HostileRaidStyle.level(Context));
        int Wanted = Users[Level];
        if (Data.getInt(UsersKey) < Wanted && arm(Context, Insurgent)) {
            Data.putInt(UsersKey, Data.getInt(UsersKey) + 1);
            Context.changed();
            return;
        }
        if (Data.getInt(CarriersKey) < CarryMax && Context.random().nextDouble() < CarryChance && stow(Context, Insurgent)) {
            Data.putInt(CarriersKey, Data.getInt(CarriersKey) + 1);
            Context.changed();
        }
    }

    private static List<HostileLedger.Entry> free(RaidContext Context, boolean Usable) {
        FactionState State = Context.State();
        Raid Current = Context.Current();
        List<HostileLedger.Entry> Found = new ArrayList<>();
        for (HostileLedger.Entry One : HostileLedger.entries(State, Entry -> !Entry.Trophy().creature() && !HostileHauls.isIssued(Current, Entry.Id()))) {
            ItemStack Stack = One.Trophy().stack(Context.World().registryAccess());
            if (!Stack.isEmpty() && (!Usable || usable(Stack))) {
                Found.add(One);
            }
        }
        return Found;
    }

    public static boolean arm(RaidContext Context, ChaosInsurgent Insurgent) {
        List<HostileLedger.Entry> Choices = free(Context, true);
        if (Choices.isEmpty()) {
            return false;
        }
        HostileLedger.Entry Pick = Choices.get(Context.random().nextInt(Choices.size()));
        give(Context, Insurgent, Pick);
        return true;
    }

    public static boolean stow(RaidContext Context, ChaosInsurgent Insurgent) {
        List<HostileLedger.Entry> Choices = free(Context, false);
        if (Choices.isEmpty()) {
            return false;
        }
        HostileLedger.Entry Pick = Choices.get(Context.random().nextInt(Choices.size()));
        ItemStack Stack = Pick.Trophy().stack(Context.World().registryAccess());
        Insurgent.issue(Pick.Id(), Stack, HostileRaider.SlotHidden);
        HostileHauls.issue(Context.Current(), Insurgent.getUUID(), Pick.Id());
        return true;
    }

    public static void give(RaidContext Context, ChaosInsurgent Insurgent, HostileLedger.Entry Pick) {
        ItemStack Stack = Pick.Trophy().stack(Context.World().registryAccess());
        switch (useOf(Stack)) {
            case Wield -> Insurgent.issue(Pick.Id(), Stack, HostileRaider.SlotHand);
            case Wear -> Insurgent.issue(Pick.Id(), Stack, HostileRaider.SlotHead);
            case Throw -> {
                Insurgent.issue(Pick.Id(), Stack, HostileRaider.SlotHidden);
                Insurgent.setThrower(true);
                Insurgent.throwRest(FirstThrow);
            }
            default -> Insurgent.issue(Pick.Id(), Stack, HostileRaider.SlotHidden);
        }
        HostileHauls.issue(Context.Current(), Insurgent.getUUID(), Pick.Id());
    }

    public static boolean use(ChaosInsurgent Insurgent, LivingEntity Target, double Distance, boolean Visible) {
        if (!Insurgent.throwReady() || !Visible || Distance < ThrowMin || Distance > ThrowMax || !(Insurgent.level() instanceof ServerLevel World)) {
            return false;
        }
        ItemStack Held = Insurgent.hidden();
        if (Held.isEmpty() || useOf(Held) != Use.Throw) {
            Insurgent.setThrower(false);
            return false;
        }
        if (alliesNear(Insurgent, Target.position())) {
            Insurgent.throwRest(20);
            return false;
        }
        throwAt(World, Insurgent, Target);
        return true;
    }

    private static boolean alliesNear(ChaosInsurgent Insurgent, Vec3 Point) {
        AABB Area = new AABB(Point, Point).inflate(FieldRadius + 1.5);
        return !Insurgent.level().getEntitiesOfClass(HostileRaider.class, Area, Insurgent::ally).isEmpty();
    }

    public static void throwAt(ServerLevel World, ChaosInsurgent Insurgent, LivingEntity Target) {
        Vec3 Eye = Insurgent.getEyePosition();
        Vec3 Aim = Target.getBoundingBox().getCenter().subtract(Eye);
        float Yaw = (float) (Mth.atan2(Aim.z, Aim.x) * Mth.RAD_TO_DEG) - 90.0f;
        float Pitch = (float) -(Mth.atan2(Aim.y, Math.sqrt(Aim.x * Aim.x + Aim.z * Aim.z)) * Mth.RAD_TO_DEG);
        Insurgent.setYRot(Yaw);
        Insurgent.setYHeadRot(Yaw);
        Insurgent.setYBodyRot(Yaw);
        Insurgent.setXRot(Mth.clamp(Pitch, -60.0f, 60.0f));
        int EntryId = Insurgent.issued();
        ItemStack Thrown = Insurgent.takeIssued();
        Insurgent.setThrower(false);
        if (Thrown.isEmpty() || useOf(Thrown) != Use.Throw) {
            return;
        }
        Insurgent.swing(InteractionHand.MAIN_HAND);
        if (Z0003DeviceItem.is(Thrown)) {
            Z0003DeviceItem.arm(Thrown, new Z0003Arming(FieldRadius, World.getGameTime() + FieldDelay));
            Z0003Device.thrown(World, Insurgent, Thrown, 1.0f);
            Z0003Registry.play(World, Insurgent.position(), Z0003Registry.ThrowLayers, SoundSource.HOSTILE, 0.9f, 1.0f);
        } else {
            Scp018Ball.thrown(World, Insurgent, Thrown, BallTuning.ThrowSpeed);
            World.playSound(null, Insurgent.getX(), Insurgent.getEyeY(), Insurgent.getZ(), Scp018Registry.Throw.get(), SoundSource.HOSTILE, 0.6f, 0.95f);
        }
        forget(World, Insurgent, EntryId);
    }

    private static void forget(ServerLevel World, ChaosInsurgent Insurgent, int EntryId) {
        if (EntryId < 0) {
            return;
        }
        FactionState State = FactionWorld.state(World.getServer(), ChaosFaction.Instance);
        HostileLedger.remove(State, EntryId);
        Raid Current = Insurgent.raid();
        if (Current != null) {
            HostileHauls.unissue(Current, Insurgent.getUUID());
        }
    }

    @Nullable
    public static Use usedBy(HostileRaider Raider) {
        ItemStack Stack = Raider.issuedStack();
        return Stack.isEmpty() ? null : useOf(Stack);
    }
}
