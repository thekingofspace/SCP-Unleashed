package net.rai.scp_unleashed.raid.chaos;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.raid.Raid;
import net.rai.scp_unleashed.raid.RaidGround;
import net.rai.scp_unleashed.raid.hostile.HostileRaider;
import net.rai.scp_unleashed.raid.underground.RaidBreach;
import net.rai.scp_unleashed.raid.underground.UndergroundRules;

import java.util.EnumSet;

public class ChaosBreachGoal extends Goal {
    public static final int Repath = 15;
    public static final int SwingEvery = 10;
    public static final double Speed = 1.1;
    private final ChaosInsurgent Owner;
    private ItemStack Stowed = ItemStack.EMPTY;
    private boolean Holding;
    private int Wait;
    private int Time;
    private int Swing;

    public ChaosBreachGoal(ChaosInsurgent Owner) {
        this.Owner = Owner;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean wanted() {
        return Owner.active() && !Owner.hauling() && !(Owner.recentlyHurt() && Owner.getTarget() != null) && RaidBreach.held(Owner);
    }

    @Override
    public boolean canUse() {
        return wanted();
    }

    @Override
    public boolean canContinueToUse() {
        return wanted();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        Wait = 0;
        Time = 0;
        Swing = 0;
    }

    @Override
    public void stop() {
        stow();
        Owner.getNavigation().stop();
    }

    private void hold() {
        if (Holding || Owner.hauling() || Owner.issuedSlot() == HostileRaider.SlotHand) {
            return;
        }
        Stowed = Owner.getMainHandItem().copy();
        Owner.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.TNT));
        Owner.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
        Holding = true;
    }

    private void stow() {
        if (!Holding) {
            return;
        }
        Owner.setItemSlot(EquipmentSlot.MAINHAND, Stowed);
        Owner.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
        Stowed = ItemStack.EMPTY;
        Holding = false;
    }

    private void walk(BlockPos Goal) {
        if (--Wait <= 0) {
            Wait = Repath;
            Owner.getNavigation().moveTo(Goal.getX() + 0.5, Goal.getY(), Goal.getZ() + 0.5, Speed);
        }
    }

    private boolean near(BlockPos Spot, double Reach) {
        double Dx = Owner.getX() - (Spot.getX() + 0.5);
        double Dz = Owner.getZ() - (Spot.getZ() + 0.5);
        return Dx * Dx + Dz * Dz <= Reach * Reach && Math.abs(Owner.getY() - Spot.getY()) <= 1.5;
    }

    @Override
    public void tick() {
        Raid Current = Owner.raid();
        if (Current == null || !(Owner.level() instanceof ServerLevel World)) {
            return;
        }
        CompoundTag Tag = RaidBreach.data(Current);
        if (Tag.getByte("Mode") == RaidBreach.ModeEntrance) {
            stow();
            BlockPos Door = RaidBreach.entrance(Tag);
            if (Door != null) {
                walk(near(Door, UndergroundRules.EntranceReach) ? Current.target() : Door);
            }
            return;
        }
        byte Phase = Tag.getByte("Phase");
        BlockPos Lip = RaidBreach.lip(Tag);
        Vec3 Charge = RaidBreach.charge(Tag);
        if (Phase == RaidBreach.PhaseOpen) {
            stow();
            if (near(Lip, UndergroundRules.EnterReach)) {
                Owner.getNavigation().stop();
                Owner.beginRappel(RaidBreach.axis(Tag), Tag.getInt("Floor"), Tag.getInt("Top"));
                return;
            }
            walk(Lip);
            return;
        }
        if (Phase <= RaidBreach.PhasePlanting && RaidBreach.claim(World, Current, Owner)) {
            Time++;
            if (near(Lip, UndergroundRules.PlantReach)) {
                Owner.getNavigation().stop();
                Owner.getLookControl().setLookAt(Charge.x, Charge.y, Charge.z);
                hold();
                if (Swing++ % SwingEvery == 0) {
                    Owner.swing(InteractionHand.MAIN_HAND);
                }
                RaidBreach.plant(World, Current, Owner, World.getGameTime());
                return;
            }
            if (Phase == RaidBreach.PhasePlanned && Time > UndergroundRules.PlantGiveUp) {
                stow();
                RaidBreach.fail(World, Current, "the planter could not reach the charge point");
                return;
            }
            walk(Lip);
            return;
        }
        stow();
        gather(World, Lip, Charge);
    }

    private void gather(ServerLevel World, BlockPos Lip, Vec3 Charge) {
        Owner.getLookControl().setLookAt(Charge.x, Charge.y, Charge.z);
        double Dx = Lip.getX() + 0.5 - Charge.x;
        double Dz = Lip.getZ() + 0.5 - Charge.z;
        double Length = Math.sqrt(Dx * Dx + Dz * Dz);
        if (Length < 1.0E-3) {
            Dx = 1.0;
            Dz = 0.0;
            Length = 1.0;
        }
        double Flat = Math.hypot(Owner.getX() - Charge.x, Owner.getZ() - Charge.z);
        if (Flat >= UndergroundRules.GatherReach - 0.5 && Flat <= UndergroundRules.GatherReach + 4.0) {
            Owner.getNavigation().stop();
            return;
        }
        int X = Mth.floor(Charge.x + Dx / Length * UndergroundRules.GatherReach);
        int Z = Mth.floor(Charge.z + Dz / Length * UndergroundRules.GatherReach);
        BlockPos Ground = RaidGround.standNear(World, X, Z, Lip.getY(), 4);
        walk(Ground != null ? Ground : Lip);
    }
}
