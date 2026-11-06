package net.rai.scp_unleashed.scp465;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.dclass.DClassLike;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.Set;
import java.util.UUID;

public class Scp465Guest extends DClassLike {
    public static final float Width = 0.6f;
    public static final float Height = 1.8f;
    public static final float EyeHeight = 1.62f;
    public static final double MaxHealth = 20.0;
    public static final double MoveSpeed = 0.25;
    public static final byte KindClown = 0;
    public static final byte KindGuest = 1;
    public static final byte KindDj = 2;
    public static final byte ActionCheer = 10;
    public static final byte ActionDanceBounce = 11;
    public static final byte ActionDanceArms = 12;
    public static final byte ActionDanceTwist = 13;
    public static final byte ActionDanceSway = 14;
    public static final byte ActionDj = 15;
    public static final byte ActionChat = 16;
    public static final byte ActionDrink = 17;
    public static final byte ActionGuard = 18;
    public static final byte ActionHighFive = 19;
    public static final byte ActionLaugh = 20;
    public static final byte ActionWave = 21;
    public static final byte[] DanceActions = {ActionDanceBounce, ActionDanceArms, ActionDanceTwist, ActionDanceSway};
    public static final int ReactCooldown = 16;
    public static final int OrphanCheck = 20;
    public static final RawAnimation CheerLoop = RawAnimation.begin().thenLoop("cheer");
    public static final RawAnimation BounceLoop = RawAnimation.begin().thenLoop("dance_bounce");
    public static final RawAnimation ArmsLoop = RawAnimation.begin().thenLoop("dance_arms");
    public static final RawAnimation TwistLoop = RawAnimation.begin().thenLoop("dance_twist");
    public static final RawAnimation SwayLoop = RawAnimation.begin().thenLoop("dance_sway");
    public static final RawAnimation DjLoop = RawAnimation.begin().thenLoop("dj");
    public static final RawAnimation ChatLoop = RawAnimation.begin().thenLoop("chat");
    public static final RawAnimation DrinkLoop = RawAnimation.begin().thenLoop("drink");
    public static final RawAnimation GuardLoop = RawAnimation.begin().thenLoop("guard");
    public static final RawAnimation HighFiveLoop = RawAnimation.begin().thenLoop("high_five");
    public static final RawAnimation LaughLoop = RawAnimation.begin().thenLoop("laugh");
    public static final RawAnimation WaveLoop = RawAnimation.begin().thenLoop("wave");
    public static final Set<String> BeatClips = Set.of("cheer", "dance_bounce", "dance_arms", "dance_twist", "dance_sway", "dj");
    public static final int TransitionTicks = 4;
    public static final double AlignWindow = 0.75;
    private static final EntityDataAccessor<Byte> KindData = SynchedEntityData.defineId(Scp465Guest.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> LookData = SynchedEntityData.defineId(Scp465Guest.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> PartyData = SynchedEntityData.defineId(Scp465Guest.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BeatData = SynchedEntityData.defineId(Scp465Guest.class, EntityDataSerializers.INT);

    @Nullable
    private Vec3 Spot;
    @Nullable
    private UUID FaceId;
    @Nullable
    private Vec3 FaceAt;
    private byte TaskAction;
    private long TaskUntil;
    private String TaskName = "";
    private byte FlashAction;
    private long FlashUntil;
    @Nullable
    private UUID FlashFace;
    private long ReactReady;
    private float RenderPartial;
    @Nullable
    private RawAnimation Shown;
    private double WaitStart = Double.NaN;

    public Scp465Guest(EntityType<? extends Scp465Guest> Type, Level World) {
        super(Type, World);
        setPersistenceRequired();
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, MaxHealth)
                .add(Attributes.MOVEMENT_SPEED, MoveSpeed)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(KindData, KindGuest);
        Builder.define(LookData, (byte) 0);
        Builder.define(PartyData, 0);
        Builder.define(BeatData, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Scp465GuestBrain(this));
    }

    public byte kind() {
        return entityData.get(KindData);
    }

    public boolean isClown() {
        return kind() == KindClown;
    }

    public boolean isDj() {
        return kind() == KindDj;
    }

    public int look() {
        return entityData.get(LookData);
    }

    public int partyId() {
        return entityData.get(PartyData);
    }

    public int beatOrigin() {
        return entityData.get(BeatData);
    }

    public void setup(byte Kind, int Look, int Party, long MusicStart) {
        entityData.set(KindData, Kind);
        entityData.set(LookData, (byte) Look);
        entityData.set(PartyData, Party);
        entityData.set(BeatData, (int) MusicStart);
    }

    public void setRenderPartial(float Partial) {
        RenderPartial = Partial;
    }

    public double beatTicks() {
        return (int) level().getGameTime() - beatOrigin() + (double) RenderPartial;
    }

    public void assign(String Name, @Nullable Vec3 Target, @Nullable UUID Face, @Nullable Vec3 FacePoint, byte Action, long Until) {
        TaskName = Name;
        Spot = Target;
        FaceId = Face;
        FaceAt = FacePoint;
        TaskAction = Action;
        TaskUntil = Until;
    }

    public void flash(byte Action, @Nullable UUID Face, int Ticks) {
        FlashAction = Action;
        FlashFace = Face;
        FlashUntil = level().getGameTime() + Ticks;
    }

    public String taskName() {
        return TaskName;
    }

    @Nullable
    public Vec3 spot() {
        return Spot;
    }

    @Nullable
    public UUID faceId() {
        return FaceId;
    }

    @Nullable
    public Vec3 faceAt() {
        return FaceAt;
    }

    public byte taskAction() {
        return TaskAction;
    }

    public long taskUntil() {
        return TaskUntil;
    }

    public boolean flashing() {
        return FlashAction != ActionNone && level().getGameTime() < FlashUntil;
    }

    public byte flashAction() {
        return FlashAction;
    }

    @Nullable
    public UUID flashFace() {
        return FlashFace;
    }

    public void holdCup(boolean Holding) {
        ItemStack Wanted = Holding ? new ItemStack(Scp465Registry.Punch.get()) : ItemStack.EMPTY;
        if (ItemStack.isSameItem(getMainHandItem(), Wanted) && getMainHandItem().isEmpty() == Wanted.isEmpty()) {
            return;
        }
        setItemSlot(EquipmentSlot.MAINHAND, Wanted);
        setDropChance(EquipmentSlot.MAINHAND, 0.0f);
    }

    public static boolean isBeatClip(String Name) {
        return BeatClips.contains(Name);
    }

    public static RawAnimation clip(byte Action) {
        return switch (Action) {
            case ActionCheer -> CheerLoop;
            case ActionDanceBounce -> BounceLoop;
            case ActionDanceArms -> ArmsLoop;
            case ActionDanceTwist -> TwistLoop;
            case ActionDanceSway -> SwayLoop;
            case ActionDj -> DjLoop;
            case ActionChat -> ChatLoop;
            case ActionDrink -> DrinkLoop;
            case ActionGuard -> GuardLoop;
            case ActionHighFive -> HighFiveLoop;
            case ActionLaugh -> LaughLoop;
            case ActionWave -> WaveLoop;
            default -> IdleLoop;
        };
    }

    public static int beats(byte Action) {
        return switch (Action) {
            case ActionDanceArms, ActionDanceSway -> 4;
            case ActionCheer, ActionDanceBounce, ActionDanceTwist, ActionDj -> 2;
            default -> 0;
        };
    }

    @Override
    protected RawAnimation bodyAnimation() {
        byte Current = action();
        if (Current >= ActionCheer && !isStriding()) {
            RawAnimation Wanted = clip(Current);
            int Beats = beats(Current);
            if (Beats > 0 && Shown != Wanted && Shown != null) {
                double Length = Beats * Scp465Music.BeatTicks;
                double Now = beatTicks();
                if (Double.isNaN(WaitStart)) {
                    WaitStart = Now;
                }
                double Phase = ((Now + TransitionTicks) % Length + Length) % Length;
                boolean Aligned = Phase < AlignWindow || Phase > Length - AlignWindow;
                if (!Aligned && Now - WaitStart < Length + TransitionTicks) {
                    return Shown;
                }
            }
            WaitStart = Double.NaN;
            Shown = Wanted;
            return Wanted;
        }
        WaitStart = Double.NaN;
        Shown = super.bodyAnimation();
        return Shown;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Scp465BeatController(this, "body", TransitionTicks, Event -> Event.setAndContinue(bodyAnimation())));
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel World) {
            if (tickCount % OrphanCheck == 1 && Scp465Parties.find(World, partyId()) == null) {
                Scp465Celebration.vanish(World, this);
                return;
            }
            if (FlashAction != ActionNone && World.getGameTime() >= FlashUntil) {
                FlashAction = ActionNone;
                FlashFace = null;
            }
        }
    }

    @Override
    public boolean isInvulnerableTo(DamageSource Source) {
        return !Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || super.isInvulnerableTo(Source);
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(Source, Amount);
        }
        if (level() instanceof ServerLevel World && Source.getEntity() != null && World.getGameTime() >= ReactReady) {
            ReactReady = World.getGameTime() + ReactCooldown;
            Scp465Celebration.shrugOff(World, this, Source.getEntity());
        }
        return false;
    }

    @Override
    protected InteractionResult mobInteract(Player User, InteractionHand Hand) {
        if (Hand != InteractionHand.MAIN_HAND || User.isSpectator()) {
            return InteractionResult.PASS;
        }
        if (level() instanceof ServerLevel World) {
            return Scp465Celebration.interact(World, this, User);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean removeWhenFarAway(double Distance) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public void checkDespawn() {
    }

    @Override
    public boolean isPushable() {
        return !isDj() && super.isPushable();
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    protected boolean canRide(Entity Vehicle) {
        return false;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance Effect) {
        return false;
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    public boolean shouldDropExperience() {
        return false;
    }

    @Override
    public boolean isAlliedTo(Entity Other) {
        return Other instanceof Scp465Guest || super.isAlliedTo(Other);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Saved) {
        super.addAdditionalSaveData(Saved);
        Saved.putByte("Kind", kind());
        Saved.putByte("Look", (byte) look());
        Saved.putInt("PartyId", partyId());
        Saved.putInt("Beat", beatOrigin());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Saved) {
        super.readAdditionalSaveData(Saved);
        entityData.set(KindData, Saved.getByte("Kind"));
        entityData.set(LookData, Saved.getByte("Look"));
        entityData.set(PartyData, Saved.getInt("PartyId"));
        entityData.set(BeatData, Saved.getInt("Beat"));
    }
}
