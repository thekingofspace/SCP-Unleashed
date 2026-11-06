package net.rai.scp_unleashed.intro;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.dclass.DClassAppearance;
import net.rai.scp_unleashed.dclass.DClassLike;
import net.rai.scp_unleashed.gear.GearRegistry;
import net.rai.scp_unleashed.guidebook.GuidebookRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class IntroStaff extends DClassLike {
    public static final int RoleScientist = 0;
    public static final int RoleGuard = 1;
    public static final int RoleTalker = 2;
    public static final double EndLow = 1.4;
    public static final double EndHigh = 12.6;
    public static final double[] Windows = {4.9, 9.9};
    public static final double Spacing = 0.3;
    @Nullable
    private UUID Owner;
    private BlockPos Origin = BlockPos.ZERO;
    private double Along;
    private double Lane;
    private int Direction = 1;
    private double Speed = 0.07;
    private double High = EndHigh;
    private int Wait;
    private boolean Glancing;
    private int LastWindow = -1;
    private int Role;
    @Nullable
    private IntroStaff Leader;

    public IntroStaff(EntityType<? extends IntroStaff> Type, Level World) {
        super(Type, World);
        setNoAi(true);
        setInvulnerable(true);
        setSilent(true);
        setPersistenceRequired();
        noPhysics = true;
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    public void setup(UUID Player, BlockPos Office, int NewRole, double Start, double NewLane, int NewDirection, double NewSpeed, int Delay, double NewHigh) {
        High = NewHigh;
        Owner = Player;
        Origin = Office;
        Role = NewRole;
        Along = Start;
        Lane = NewLane;
        Direction = NewDirection;
        Speed = NewSpeed;
        Wait = Delay;
        setAppearance(DClassAppearance.pack(random.nextInt(6), 1 + random.nextInt(5), random.nextInt(6), random.nextInt(6), random.nextInt(3), 0, 0,
                NewRole == RoleGuard ? DClassAppearance.Fatigues : DClassAppearance.Jumpsuit));
        dress();
        place();
    }

    public void follow(IntroStaff Ahead) {
        Leader = Ahead;
    }

    public int role() {
        return Role;
    }

    private void dress() {
        if (Role == RoleGuard) {
            setItemSlot(EquipmentSlot.HEAD, new ItemStack(GearRegistry.SiteGuardCap.get()));
            setItemSlot(EquipmentSlot.CHEST, new ItemStack(GearRegistry.SiteGuardShirt.get()));
            setItemSlot(EquipmentSlot.LEGS, new ItemStack(GearRegistry.SiteGuardTrousers.get()));
            setItemSlot(EquipmentSlot.FEET, new ItemStack(GearRegistry.SiteGuardBoots.get()));
        } else {
            setItemSlot(EquipmentSlot.CHEST, new ItemStack(GearRegistry.ScientistLabCoat.get()));
            setItemSlot(EquipmentSlot.LEGS, new ItemStack(GearRegistry.ScientistSlacks.get()));
            setItemSlot(EquipmentSlot.FEET, new ItemStack(GearRegistry.ScientistShoes.get()));
            if (Role == RoleScientist) {
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(GuidebookRegistry.FacilityTablet.get()));
            }
        }
        for (EquipmentSlot Slot : EquipmentSlot.values()) {
            setDropChance(Slot, 0.0f);
        }
    }

    private float walkYaw() {
        return Direction > 0 ? -90.0f : 90.0f;
    }

    private void place() {
        float Yaw = Glancing ? 0.0f : walkYaw();
        moveTo(Origin.getX() + Along, Origin.getY() + 1.0, Origin.getZ() + Lane, Yaw, 0.0f);
        setYBodyRot(Yaw);
        float Head = Yaw;
        if (Role == RoleTalker && !Glancing) {
            Head = Yaw + (Leader != null ? -35.0f : 35.0f) * Direction * (Mth.sin(tickCount * 0.05f) > -0.3f ? 1.0f : 0.0f);
        }
        setYHeadRot(Head);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !(level() instanceof ServerLevel World)) {
            return;
        }
        Player Watcher = Owner == null ? null : World.getPlayerByUUID(Owner);
        if (Watcher == null || Leader != null && Leader.isRemoved()) {
            if (tickCount > 40) {
                discard();
            }
            return;
        }
        if (Leader != null) {
            Direction = Leader.Direction;
            Glancing = Leader.Glancing;
            Along = Leader.Along - Direction * Spacing;
            Speed = Leader.Speed;
            stepTo();
            return;
        }
        if (Wait > 0) {
            Wait--;
            if (Wait == 0) {
                Glancing = false;
            }
            stepTo();
            return;
        }
        if (Speed <= 0.0) {
            if (random.nextInt(160) == 0) {
                Glancing = true;
                Wait = 40 + random.nextInt(30);
            }
            stepTo();
            return;
        }
        double Before = Along;
        Along += Direction * Speed;
        for (int Index = 0; Index < Windows.length; Index++) {
            double Spot = Windows[Index];
            if (Index != LastWindow && (Before - Spot) * (Along - Spot) <= 0.0) {
                LastWindow = Index;
                if (random.nextInt(3) == 0) {
                    Wait = 26 + random.nextInt(30);
                    Glancing = true;
                }
            }
        }
        if (Along >= High || Along <= EndLow) {
            Along = Mth.clamp(Along, EndLow, High);
            Direction = -Direction;
            Wait = 40 + random.nextInt(120);
            LastWindow = -1;
        }
        stepTo();
    }

    private void stepTo() {
        double OldX = getX();
        double OldZ = getZ();
        place();
        xo = OldX;
        zo = OldZ;
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource Source) {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity Other) {
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    protected InteractionResult mobInteract(Player User, InteractionHand Hand) {
        return InteractionResult.FAIL;
    }

    @Override
    public boolean removeWhenFarAway(double Distance) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    public boolean canChangeDimensions(Level From, Level To) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Saved) {
        super.addAdditionalSaveData(Saved);
        if (Owner != null) {
            Saved.putUUID("Owner", Owner);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Saved) {
        super.readAdditionalSaveData(Saved);
        Owner = Saved.hasUUID("Owner") ? Saved.getUUID("Owner") : null;
    }
}
