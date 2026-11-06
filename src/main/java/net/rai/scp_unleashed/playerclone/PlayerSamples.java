package net.rai.scp_unleashed.playerclone;

import com.mojang.authlib.GameProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.control.ControlSessions;
import net.rai.scp_unleashed.labware.LabVials;
import org.jetbrains.annotations.Nullable;

public final class PlayerSamples {
    public static final String NameKey = "Name";
    public static final String ProfileKey = "Profile";
    public static final int CooldownTicks = 40;
    public static final float DrawDamage = 1.0f;
    private static final String Prefix = "message.scp_unleashed.sampling_needle.";

    private PlayerSamples() {
    }

    public static CompoundTag write(@Nullable ResolvableProfile Profile) {
        CompoundTag Sample = new CompoundTag();
        if (Profile == null) {
            return Sample;
        }
        Sample.putString(NameKey, nameOf(Profile));
        ResolvableProfile.CODEC.encodeStart(NbtOps.INSTANCE, Profile).result().ifPresent(Encoded -> Sample.put(ProfileKey, Encoded));
        return Sample;
    }

    @Nullable
    public static ResolvableProfile read(CompoundTag Sample) {
        if (!Sample.contains(ProfileKey)) {
            return null;
        }
        return ResolvableProfile.CODEC.parse(NbtOps.INSTANCE, Sample.get(ProfileKey)).result().orElse(null);
    }

    public static String nameOf(@Nullable ResolvableProfile Profile) {
        return Profile == null ? "" : Profile.name().orElse("");
    }

    public static ItemStack sampleOf(GameProfile Donor) {
        GameProfile Copy = new GameProfile(Donor.getId(), Donor.getName());
        Copy.getProperties().putAll(Donor.getProperties());
        ItemStack Stack = new ItemStack(PlayerCloneRegistry.BloodSample.get());
        Stack.set(PlayerCloneRegistry.SampleComponent.get(), new ResolvableProfile(Copy));
        return Stack;
    }

    public static boolean sampleable(Player Donor) {
        return Donor.isAlive() && !Donor.isCreative() && !Donor.isSpectator() && !ControlSessions.isControlling(Donor)
                && StringUtil.isValidPlayerName(Donor.getGameProfile().getName());
    }

    public static boolean draw(ServerPlayer User, LivingEntity Target, ItemStack Needle) {
        if (User.getCooldowns().isOnCooldown(Needle.getItem())) {
            return false;
        }
        if (!(Target instanceof Player Donor) || !sampleable(Donor)) {
            User.displayClientMessage(Component.translatable(Prefix + "nothing").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        if (!User.getAbilities().instabuild && !takeVial(User)) {
            User.displayClientMessage(Component.translatable(Prefix + "no_vial").withStyle(ChatFormatting.RED), true);
            return false;
        }
        ItemStack Sample = sampleOf(Donor.getGameProfile());
        if (!User.getInventory().add(Sample)) {
            User.drop(Sample, false);
        }
        User.getCooldowns().addCooldown(Needle.getItem(), CooldownTicks);
        play(User.serverLevel(), Donor);
        User.displayClientMessage(Component.translatable(Prefix + "drawn", Donor.getGameProfile().getName()).withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    public static boolean drawSelf(ServerPlayer User, ItemStack Needle) {
        if (!draw(User, User, Needle)) {
            return false;
        }
        User.hurt(User.damageSources().source(PlayerCloneRegistry.NeedleDamage), DrawDamage);
        return true;
    }

    private static boolean takeVial(Player User) {
        Inventory Pockets = User.getInventory();
        ItemStack Offhand = User.getOffhandItem();
        if (LabVials.isEmptyVial(Offhand)) {
            Offhand.shrink(1);
            return true;
        }
        for (int Slot = 0; Slot < Pockets.getContainerSize(); Slot++) {
            ItemStack Stack = Pockets.getItem(Slot);
            if (LabVials.isEmptyVial(Stack)) {
                Stack.shrink(1);
                Pockets.setChanged();
                return true;
            }
        }
        return false;
    }

    private static void play(ServerLevel World, LivingEntity Donor) {
        float Pitch = 0.95f + World.getRandom().nextFloat() * 0.1f;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : PlayerCloneRegistry.NeedleLayers) {
            World.playSound(null, Donor.getX(), Donor.getY() + 1.0, Donor.getZ(), Layer.get(), SoundSource.PLAYERS, 0.8f, Pitch);
        }
    }
}
