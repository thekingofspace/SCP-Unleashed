package net.rai.scp_unleashed.playerclone;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.PersonnelFileItem;
import org.jetbrains.annotations.Nullable;

public class PlayerClone extends DClass {
    public static final String NamedKey = "entity.scp_unleashed.player_clone.named";
    public static final String NamedFallback = "Clone of %s";
    public static final String SampleKey = "Sample";
    private static final EntityDataAccessor<CompoundTag> SampleData = SynchedEntityData.defineId(PlayerClone.class, EntityDataSerializers.COMPOUND_TAG);

    @Nullable
    private CompoundTag Decoded;
    @Nullable
    private ResolvableProfile Profile;

    public PlayerClone(EntityType<? extends PlayerClone> Type, Level World) {
        super(Type, World);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(SampleData, new CompoundTag());
    }

    @Override
    public void applySample(CompoundTag Sample) {
        setProfile(PlayerSamples.read(Sample));
    }

    public void setProfile(@Nullable ResolvableProfile NewProfile) {
        entityData.set(SampleData, PlayerSamples.write(NewProfile));
    }

    @Nullable
    public ResolvableProfile profile() {
        CompoundTag Current = entityData.get(SampleData);
        if (Current != Decoded) {
            Decoded = Current;
            Profile = PlayerSamples.read(Current);
        }
        return Profile;
    }

    public String sourceName() {
        return PlayerSamples.nameOf(profile());
    }

    @Override
    public boolean keepsRecords() {
        return false;
    }

    @Override
    public int assignNumber() {
        return 0;
    }

    @Override
    public String designation() {
        return getName().getString();
    }

    @Override
    public Component getName() {
        if (hasCustomName()) {
            return super.getName();
        }
        String Source = sourceName();
        return Source.isEmpty() ? getTypeName() : Component.translatableWithFallback(NamedKey, NamedFallback, Source);
    }

    @Override
    public Component controlName() {
        return getName();
    }

    @Override
    public boolean shouldShowName() {
        if (isPuppet()) {
            return super.shouldShowName();
        }
        return !isSpecimen() && !isInvisible();
    }

    @Override
    protected InteractionResult mobInteract(Player User, InteractionHand Hand) {
        if (User.getItemInHand(Hand).getItem() instanceof PersonnelFileItem File && File.isBlank()) {
            if (!level().isClientSide) {
                User.displayClientMessage(Component.translatable("message.scp_unleashed.player_clone.no_file").withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(User, Hand);
    }

    @Override
    public CompoundTag saveIdentity() {
        CompoundTag Identity = super.saveIdentity();
        Identity.put(SampleKey, entityData.get(SampleData).copy());
        return Identity;
    }

    @Override
    public void loadIdentity(CompoundTag Identity) {
        super.loadIdentity(Identity);
        entityData.set(SampleData, Identity.getCompound(SampleKey).copy());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Saved) {
        super.addAdditionalSaveData(Saved);
        Saved.put(SampleKey, entityData.get(SampleData).copy());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Saved) {
        super.readAdditionalSaveData(Saved);
        entityData.set(SampleData, Saved.getCompound(SampleKey).copy());
    }
}
