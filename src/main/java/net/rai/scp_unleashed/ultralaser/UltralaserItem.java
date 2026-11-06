package net.rai.scp_unleashed.ultralaser;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.guns.GunReloading;
import net.rai.scp_unleashed.guns.GunStats;
import net.rai.scp_unleashed.guns.GunTimer;
import net.rai.scp_unleashed.guns.GunsRegistry;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;
import java.util.Locale;

public class UltralaserItem extends GunItem {
    public static final String Prefix = "item.scp_unleashed.ultralaser";
    public static final String TooltipPrefix = "tooltip.scp_unleashed.ultralaser.";

    public UltralaserItem(Properties Properties) {
        super(GunKind.Ultralaser, Properties);
    }

    public static boolean isUltralaser(ItemStack Stack) {
        return Stack.getItem() instanceof UltralaserItem;
    }

    @Override
    public void inventoryTick(ItemStack Stack, Level World, Entity Holder, int Slot, boolean Selected) {
        if (World.isClientSide || !(Holder instanceof ServerPlayer User)) {
            return;
        }
        boolean InHand = User.getMainHandItem() == Stack;
        long Now = World.getGameTime();
        GunTimer Reloading = getReload(Stack);
        if (Reloading != null) {
            if (!InHand) {
                Stack.remove(GunsRegistry.Reload.get());
            } else if (Reloading.isDone(Now)) {
                int Before = getAmmo(Stack);
                GunReloading.finish(User, Stack, getKind());
                if (getAmmo(Stack) > Before) {
                    UltralaserSounds.play(User.serverLevel(), User.getEyePosition(), UltralaserSounds.Seat);
                }
            }
        }
        GunTimer Charging = getCharging(Stack);
        if (Charging != null && (!InHand || !UltralaserPriming.owns(User, Charging))) {
            Stack.remove(GunsRegistry.Charging.get());
        }
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        GunStats Stats = getKind().getStats();
        Lines.add(Component.translatable(Prefix + ".tooltip").withStyle(ChatFormatting.GRAY));
        Lines.add(Component.translatable("tooltip.scp_unleashed.guns.loaded", getAmmo(Stack), Stats.getCapacity(), getKind().getAmmo().getDescription())
                .withStyle(getAmmo(Stack) > 0 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.DARK_RED));
        if (!ItemTooltips.showDetails(Lines)) {
            return;
        }
        Lines.add(Component.translatable("tooltip.scp_unleashed.guns.damage", number(Stats.getDamage())).withStyle(ChatFormatting.DARK_GREEN));
        Lines.add(Component.translatable(TooltipPrefix + "beam", Math.round(Stats.getRange())).withStyle(ChatFormatting.DARK_GREEN));
        Lines.add(Component.translatable(TooltipPrefix + "prime", number(UltralaserScore.seconds())).withStyle(ChatFormatting.DARK_GREEN));
        Lines.add(Component.translatable(TooltipPrefix + "vent", number(UltralaserPriming.CooldownTicks / 20.0F)).withStyle(ChatFormatting.DARK_GREEN));
        Lines.add(Component.translatable("tooltip.scp_unleashed.guns.reload_time", number(Stats.getReloadTicks() / 20.0F)).withStyle(ChatFormatting.DARK_GREEN));
        Lines.add(Component.translatable(TooltipPrefix + "controls", Component.keybind("key.attack"), Component.keybind("key.use")).withStyle(ChatFormatting.GRAY));
        Lines.add(Component.translatable(TooltipPrefix + "cancel").withStyle(ChatFormatting.GRAY));
        Lines.add(Component.translatable("tooltip.scp_unleashed.guns.reload_hint", Component.keybind(ReloadKey)).withStyle(ChatFormatting.GRAY));
        Lines.add(Component.translatable(Prefix + ".warning").withStyle(ChatFormatting.RED));
    }

    private static String number(float Value) {
        return Value == Math.floor(Value) ? Integer.toString((int) Value) : String.format(Locale.ROOT, "%.1f", Value);
    }
}
