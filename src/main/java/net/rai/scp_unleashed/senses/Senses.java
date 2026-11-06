package net.rai.scp_unleashed.senses;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.neoforged.fml.ModList;
import net.rai.scp_unleashed.integration.curios.CuriosSenses;
import org.jetbrains.annotations.Nullable;

public final class Senses {
    public static final String CuriosId = "curios";
    public static final String MuffledKey = "message.scp_unleashed.senses.muffled";
    public static final String BlindKey = "message.scp_unleashed.senses.cannot_see";
    public static final float MuffledShare = 0.5f;
    private static Boolean CuriosLoaded;

    private Senses() {
    }

    public static boolean isDeafened(@Nullable LivingEntity Wearer) {
        return isWearing(Wearer, SensesRegistry.Earplugs.get());
    }

    public static boolean isBlindfolded(@Nullable LivingEntity Wearer) {
        return isWearing(Wearer, SensesRegistry.Blindfold.get());
    }

    public static Component heard(@Nullable LivingEntity Listener, Component Line) {
        return isDeafened(Listener) ? Component.translatable(MuffledKey).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC) : Line;
    }

    public static boolean refuseSight(@Nullable Player User) {
        if (!isBlindfolded(User)) {
            return false;
        }
        if (!User.level().isClientSide) {
            User.displayClientMessage(Component.translatable(BlindKey).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        }
        return true;
    }

    public static boolean isWearing(@Nullable LivingEntity Wearer, Item Worn) {
        if (Wearer == null) {
            return false;
        }
        if (Wearer.getItemBySlot(EquipmentSlot.HEAD).is(Worn)) {
            return true;
        }
        return curiosLoaded() && CuriosSenses.isWearing(Wearer, Worn);
    }

    public static boolean showsWorn(@Nullable LivingEntity Wearer, Item Worn) {
        if (Wearer == null) {
            return false;
        }
        if (Wearer.getItemBySlot(EquipmentSlot.HEAD).is(Worn)) {
            return true;
        }
        return curiosLoaded() && CuriosSenses.isShown(Wearer, Worn);
    }

    public static boolean curiosLoaded() {
        if (CuriosLoaded == null) {
            ModList Mods = ModList.get();
            if (Mods == null) {
                return false;
            }
            CuriosLoaded = Mods.isLoaded(CuriosId);
        }
        return CuriosLoaded;
    }
}
