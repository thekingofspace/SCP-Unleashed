package net.rai.scp_unleashed.scp439;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.UUID;

public class Scp439Hive extends LivingEntity implements GeoEntity {
    public static final float Width = 0.95f;
    public static final float Height = 1.05f;
    public static final float EyeHeight = 0.7f;
    public static final int Tiers = 5;
    public static final int StageTicks = 2400;
    public static final int StartTicks = 840;
    public static final int FullTicks = Tiers * StageTicks;
    public static final float Spurt = (float) StartTicks / StageTicks;
    public static final int WorkerCap = 8;
    public static final int WorkerInterval = 900;
    public static final int FirstWorker = 200;
    public static final int BruteCap = 3;
    public static final int BruteFirstFed = 20;
    public static final int BruteStepFed = 40;
    public static final int BruteInterval = 1200;
    public static final int QueenCost = 150;
    public static final int QueenInterval = 2400;
    public static final double AlertRange = 32.0;
    public static final double CountRange = 48.0;
    public static final int DeathTicks = 20;
    public static final int HurtTicks = 10;
    public static final int HeaveTicks = 30;
    public static final byte HeaveEvent = 70;
    public static final double CullMargin = 0.9;
    public static final int FoodCeiling = 100000;
    private static final EntityDataAccessor<CompoundTag> LookData = SynchedEntityData.defineId(Scp439Hive.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Integer> GrowthData = SynchedEntityData.defineId(Scp439Hive.class, EntityDataSerializers.INT);
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private final NonNullList<ItemStack> Armour = NonNullList.withSize(4, ItemStack.EMPTY);
    private int Growth = StartTicks;
    private int ClientGrowth = StartTicks;
    private int Food;
    private int Fed;
    private int WorkerTimer = FirstWorker;
    private int BruteTimer = BruteInterval;
    private int QueenTimer = QueenInterval;
    private int Rustle = 200;
    private int Daughters;
    private int Generation;
    private int HeaveTimer;
    private CompoundTag QueenTag = new CompoundTag();
    private boolean Collapsed;
    @Nullable
    private CompoundTag Decoded;
    private Scp439Look Look = Scp439Look.Blank;
    private ItemStack[] Worn = {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
    @Nullable
    private ResolvableProfile Profile;

    public Scp439Hive(EntityType<? extends Scp439Hive> Type, Level World) {
        super(Type, World);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes().add(Attributes.MAX_HEALTH, 80.0).add(Attributes.ARMOR, 6.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(LookData, new CompoundTag());
        Builder.define(GrowthData, StartTicks);
    }

    @Nullable
    public static Scp439Hive grow(ServerLevel World, Vec3 Where, float Yaw, Scp439Look Shown, CompoundTag Queen, int Ticks) {
        Scp439Hive Made = Scp439Registry.Hive.get().create(World);
        if (Made == null) {
            return null;
        }
        Made.setLook(Shown);
        Made.QueenTag = Queen.copy();
        Made.Generation = Queen.getInt("Generation");
        Made.setGrowth(Math.max(StartTicks, Ticks));
        Made.moveTo(Where.x, Where.y, Where.z, Yaw, 0.0f);
        Made.setYBodyRot(Yaw);
        Made.setYHeadRot(Yaw);
        if (!World.addFreshEntity(Made)) {
            return null;
        }
        Scp439Colonies.get(World).addHive(Made.getUUID(), Made.blockPosition());
        World.sendParticles(Scp439Registry.Flake.get(), Where.x, Where.y + 0.6, Where.z, 16, 0.35, 0.4, 0.35, 0.03);
        Made.playLayers(Scp439Registry.HiveGrow, 1.0f, 0.8f);
        return Made;
    }

    @Nullable
    public static Scp439Queen release(ServerLevel World, Vec3 Where, float Yaw, CompoundTag Carried) {
        Scp439Queen Queen = Scp439Registry.Queen.get().create(World);
        if (Queen == null) {
            return null;
        }
        Queen.moveTo(Where.x, Where.y, Where.z, Yaw, 0.0f);
        Queen.setYBodyRot(Yaw);
        Queen.setYHeadRot(Yaw);
        Queen.adopt(Carried);
        Queen.beginEmerge();
        if (!World.addFreshEntity(Queen)) {
            return null;
        }
        Scp439Colonies.get(World).seeQueen(Queen.getUUID(), World.getGameTime());
        return Queen;
    }

    public ItemStack gear(EquipmentSlot Slot) {
        look();
        for (int Index = 0; Index < Scp439Look.Worn.length; Index++) {
            if (Scp439Look.Worn[Index] == Slot) {
                return Worn[Index];
            }
        }
        return ItemStack.EMPTY;
    }

    public void setLook(Scp439Look Shown) {
        entityData.set(LookData, Shown.save());
    }

    public Scp439Look look() {
        CompoundTag Current = entityData.get(LookData);
        if (Current != Decoded) {
            Decoded = Current;
            Look = Scp439Look.load(Current);
            Profile = Look.profile();
            ItemStack[] Fresh = new ItemStack[Scp439Look.Worn.length];
            for (int Index = 0; Index < Fresh.length; Index++) {
                Fresh[Index] = Look.gear(Scp439Look.Worn[Index]);
            }
            Worn = Fresh;
        }
        return Look;
    }

    @Nullable
    public ResolvableProfile profile() {
        look();
        return Profile;
    }

    public int growth() {
        return Growth;
    }

    public void setGrowth(int Ticks) {
        Growth = Mth.clamp(Ticks, 0, FullTicks);
        ClientGrowth = Growth;
        entityData.set(GrowthData, Growth);
    }

    public float progress(float Partial) {
        int Shown = level().isClientSide ? ClientGrowth : Growth;
        return shown(Mth.clamp((Shown + (level().isClientSide && Shown < FullTicks ? Partial : 0.0f)) / StageTicks, 0.0f, Tiers));
    }

    public static float shown(float Raw) {
        float Reached = Math.min(Tiers - 1, Mth.floor(Raw));
        return Math.min(Tiers, Reached + Math.min(1.0f, (Raw - Reached) / Spurt));
    }

    public int stage() {
        return Math.min(Tiers, 1 + Growth / StageTicks);
    }

    public boolean seeking() {
        return Growth >= FullTicks;
    }

    public int food() {
        return Food;
    }

    public int fed() {
        return Fed;
    }

    public int daughters() {
        return Daughters;
    }

    public void addFood(int Amount) {
        Food = Mth.clamp(Food + Amount, 0, FoodCeiling);
        Fed = Mth.clamp(Fed + Math.max(0, Amount), 0, FoodCeiling);
    }

    public void feed(ItemStack Stack) {
        int Value = Scp439Food.value(Stack);
        if (Value <= 0) {
            return;
        }
        addFood(Value);
        if (level() instanceof ServerLevel World) {
            World.sendParticles(Scp439Registry.Flake.get(), getX(), getY() + 0.7, getZ(), 4, 0.25, 0.2, 0.25, 0.02);
        }
    }

    public CompoundTag queenTag() {
        return QueenTag;
    }

    public int count(ServerLevel World, byte Kind) {
        int Listed = Scp439Colonies.get(World).members(getUUID(), Kind, World.getGameTime());
        AABB Area = getBoundingBox().inflate(CountRange);
        Class<? extends Scp439Creature> Wanted = Kind == Scp439Colonies.Workers ? Scp439Worker.class : Scp439Brute.class;
        int Near = World.getEntitiesOfClass(Scp439Creature.class, Area, Member -> Wanted.isInstance(Member) && Member.isAlive() && getUUID().equals(Member.hiveId())).size();
        return Math.max(Listed, Near);
    }

    public int bruteAllowance() {
        if (Fed < BruteFirstFed) {
            return 0;
        }
        return Math.min(BruteCap, 1 + (Fed - BruteFirstFed) / BruteStepFed);
    }

    @Override
    public void tick() {
        super.tick();
        float Yaw = getYRot();
        yBodyRot = Yaw;
        yBodyRotO = Yaw;
        yHeadRot = Yaw;
        yHeadRotO = Yaw;
        if (level().isClientSide) {
            if (ClientGrowth < FullTicks) {
                ClientGrowth++;
            }
            if (HeaveTimer > 0) {
                HeaveTimer--;
            }
            return;
        }
        if (!isAlive() || !(level() instanceof ServerLevel World)) {
            return;
        }
        if (tickCount % 20 == 1) {
            Scp439Colonies.get(World).addHive(getUUID(), blockPosition());
        }
        int Before = stage();
        if (Growth < FullTicks) {
            Growth++;
            if (Growth % 10 == 0 || Growth == FullTicks) {
                entityData.set(GrowthData, Growth);
            }
            if (stage() != Before || Growth == FullTicks) {
                playLayers(Scp439Registry.HiveGrow, 1.0f, 0.85f + 0.05f * stage());
                World.sendParticles(Scp439Registry.Flake.get(), getX(), getY() + 0.6, getZ(), 10, 0.3, 0.35, 0.3, 0.02);
            }
        }
        if (--Rustle <= 0) {
            Rustle = 200 + random.nextInt(300);
            playLayers(Scp439Registry.HiveRustle, 0.6f, 0.9f + random.nextFloat() * 0.2f);
        }
        if (seeking()) {
            colonyTick(World);
        }
    }

    private void colonyTick(ServerLevel World) {
        if (--WorkerTimer <= 0) {
            WorkerTimer = WorkerInterval;
            if (count(World, Scp439Colonies.Workers) < WorkerCap) {
                Scp439Worker Worker = Scp439Registry.Worker.get().create(World);
                if (Worker != null) {
                    hatch(World, Worker);
                }
            }
        }
        if (--BruteTimer <= 0) {
            BruteTimer = BruteInterval;
            if (count(World, Scp439Colonies.Brutes) < bruteAllowance()) {
                Scp439Brute Brute = Scp439Registry.Brute.get().create(World);
                if (Brute != null) {
                    hatch(World, Brute);
                }
            }
        }
        if (QueenTimer > 0) {
            QueenTimer--;
        }
        if (Food >= QueenCost && QueenTimer <= 0) {
            raiseQueen(World);
        }
    }

    public boolean raiseQueen(ServerLevel World) {
        if (!Scp439Registry.spreads(World.getGameRules()) || !Scp439Colonies.get(World).roomForQueen(World.getGameTime())) {
            return false;
        }
        Food -= QueenCost;
        QueenTimer = QueenInterval;
        Daughters++;
        CompoundTag Carried = new CompoundTag();
        Carried.putInt("Generation", Generation + 1);
        Vec3 Front = front(0.9);
        Scp439Queen Queen = release(World, Front, getYRot() + random.nextFloat() * 60.0f - 30.0f, Carried);
        if (Queen == null) {
            Food += QueenCost;
            Daughters--;
            return false;
        }
        Queen.makeDaughter(blockPosition(), Generation + 1);
        playLayers(Scp439Registry.HiveBirth, 1.2f, 0.7f);
        World.broadcastEntityEvent(this, HeaveEvent);
        return true;
    }

    public void hatch(ServerLevel World, Scp439Creature Member) {
        Vec3 Front = front(0.75);
        Member.moveTo(Front.x, getY(), Front.z, getYRot() + random.nextFloat() * 90.0f - 45.0f, 0.0f);
        Member.bindHive(this);
        Member.beginEmerge();
        if (World.addFreshEntity(Member)) {
            playLayers(Scp439Registry.HiveBirth, 1.0f, 1.0f);
            World.broadcastEntityEvent(this, HeaveEvent);
            World.sendParticles(Scp439Registry.Flake.get(), getX(), getY() + 0.5, getZ(), 8, 0.3, 0.3, 0.3, 0.03);
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(CullMargin, 0.3, CullMargin);
    }

    public float heave(float Partial) {
        if (HeaveTimer <= 0) {
            return 0.0f;
        }
        float Ticks = HeaveTicks - HeaveTimer + Partial;
        if (Ticks <= 0.0f || Ticks >= HeaveTicks) {
            return 0.0f;
        }
        return (float) Math.sin(Math.PI * Ticks / HeaveTicks);
    }

    public float flinch(float Partial) {
        return hurtTime > 0 ? Mth.clamp((hurtTime - Partial) / HurtTicks, 0.0f, 1.0f) : 0.0f;
    }

    public float crumble(float Partial) {
        return deathTime > 0 ? Mth.clamp((deathTime + Partial) / DeathTicks, 0.0f, 1.0f) : 0.0f;
    }

    @Override
    public void handleEntityEvent(byte Id) {
        if (Id == HeaveEvent) {
            HeaveTimer = HeaveTicks;
            return;
        }
        super.handleEntityEvent(Id);
    }

    public Vec3 front(double Reach) {
        Vec3 Facing = Vec3.directionFromRotation(0.0f, getYRot());
        return position().add(Facing.scale(Reach));
    }

    public void playLayers(List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            level().playSound(null, getX(), getY() + 0.6, getZ(), Layer.get(), SoundSource.HOSTILE, Volume, Pitch);
        }
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        Entity Attacker = Source.getEntity();
        if (Attacker != null && Scp439Creature.isColony(Attacker)) {
            return false;
        }
        boolean Hurt = super.hurt(Source, Amount);
        if (Hurt && level() instanceof ServerLevel World && isAlive()) {
            World.sendParticles(Scp439Registry.Flake.get(), getX(), getY() + 0.6, getZ(), 6, 0.3, 0.3, 0.3, 0.04);
            if (Attacker instanceof LivingEntity Living && !(Attacker instanceof Player User && User.isCreative())) {
                alert(World, Living);
            }
        }
        return Hurt;
    }

    public void alert(ServerLevel World, LivingEntity Attacker) {
        for (Scp439Creature Member : World.getEntitiesOfClass(Scp439Creature.class, getBoundingBox().inflate(AlertRange),
                Found -> Found.isAlive() && getUUID().equals(Found.hiveId()) && !(Found instanceof Scp439Queen))) {
            if (Member.getTarget() == null && Member.canAttack(Attacker)) {
                Member.setTarget(Attacker);
            }
        }
    }

    @Override
    protected void playHurtSound(DamageSource Source) {
        playLayers(Scp439Registry.HiveHurt, 1.0f, 0.9f + random.nextFloat() * 0.2f);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp439Registry.HiveHurt.get(0).get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp439Registry.HiveDeath.get(0).get();
    }

    @Override
    public void die(DamageSource Source) {
        boolean Fresh = !isRemoved() && !dead;
        super.die(Source);
        if (Fresh && level() instanceof ServerLevel World) {
            for (int Index = 1; Index < Scp439Registry.HiveDeath.size(); Index++) {
                level().playSound(null, getX(), getY() + 0.6, getZ(), Scp439Registry.HiveDeath.get(Index).get(), SoundSource.HOSTILE, 1.0f, 1.0f);
            }
            collapseColony(World, true);
        }
    }

    public void collapseColony(ServerLevel World, boolean FreeQueen) {
        if (Collapsed) {
            return;
        }
        Collapsed = true;
        List<UUID> Bound = Scp439Colonies.get(World).removeHive(getUUID());
        for (UUID Id : Bound) {
            for (ServerLevel Each : World.getServer().getAllLevels()) {
                if (Each.getEntity(Id) instanceof Scp439Creature Member) {
                    Member.collapse();
                }
            }
        }
        for (Scp439Creature Member : World.getEntitiesOfClass(Scp439Creature.class, getBoundingBox().inflate(CountRange),
                Found -> Found.isAlive() && getUUID().equals(Found.hiveId()))) {
            Member.collapse();
        }
        World.sendParticles(Scp439Registry.Flake.get(), getX(), getY() + 0.5, getZ(), 30, 0.4, 0.4, 0.4, 0.06);
        if (FreeQueen) {
            release(World, position(), getYRot(), QueenTag);
        }
    }

    @Override
    public void remove(RemovalReason Reason) {
        if (Reason.shouldDestroy() && level() instanceof ServerLevel World) {
            collapseColony(World, false);
        }
        super.remove(Reason);
    }

    @Override
    protected void tickDeath() {
        deathTime++;
        if (deathTime >= DeathTicks && !level().isClientSide() && !isRemoved()) {
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity Other) {
    }

    @Override
    public void push(double X, double Y, double Z) {
    }

    @Override
    public void knockback(double Strength, double X, double Z) {
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isAffectedByPotions() {
        return false;
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return Armour;
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot Slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(EquipmentSlot Slot, ItemStack Stack) {
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Key) {
        super.onSyncedDataUpdated(Key);
        if (Key.equals(GrowthData) && level().isClientSide) {
            ClientGrowth = entityData.get(GrowthData);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.put("Look", entityData.get(LookData).copy());
        Tag.putInt("Growth", Growth);
        Tag.putInt("Food", Food);
        Tag.putInt("Fed", Fed);
        Tag.putInt("WorkerTimer", WorkerTimer);
        Tag.putInt("BruteTimer", BruteTimer);
        Tag.putInt("QueenTimer", QueenTimer);
        Tag.putInt("Daughters", Daughters);
        Tag.putInt("Generation", Generation);
        Tag.put("Queen", QueenTag.copy());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        entityData.set(LookData, Tag.getCompound("Look").copy());
        setGrowth(Tag.contains("Growth") ? Tag.getInt("Growth") : StartTicks);
        Food = Tag.getInt("Food");
        Fed = Tag.getInt("Fed");
        WorkerTimer = Tag.contains("WorkerTimer") ? Tag.getInt("WorkerTimer") : FirstWorker;
        BruteTimer = Tag.contains("BruteTimer") ? Tag.getInt("BruteTimer") : BruteInterval;
        QueenTimer = Tag.contains("QueenTimer") ? Tag.getInt("QueenTimer") : QueenInterval;
        Daughters = Tag.getInt("Daughters");
        Generation = Tag.getInt("Generation");
        QueenTag = Tag.getCompound("Queen").copy();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
