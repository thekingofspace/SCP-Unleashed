package net.rai.scp_unleashed.scp127;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.guns.GunStats;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;
import java.util.Locale;

public class Scp127Item extends GunItem {
    public static final String Prefix = "item.scp_unleashed.scp127_living_gun";
    public static final String TooltipPrefix = "tooltip.scp_unleashed.scp127.";
    public static final int BarColour = 0xE85840;

    public Scp127Item(Properties Settings) {
        super(GunKind.Scp127, Settings);
    }

    public static boolean is(ItemStack Stack) {
        return Stack.getItem() instanceof Scp127Item;
    }

    public static ItemStack create() {
        return new ItemStack(Scp127Registry.LivingGun.get());
    }

    @Override
    public float shotDamage(ItemStack Stack) {
        return Scp127Levels.damageAt(Scp127Levels.rankOf(Stack));
    }

    @Override
    public void onFired(ServerPlayer User, ItemStack Stack) {
        Scp127Life.used(Stack, User.level().getGameTime());
    }

    @Override
    public void inventoryTick(ItemStack Stack, Level World, Entity Holder, int Slot, boolean Selected) {
        super.inventoryTick(Stack, World, Holder, Slot, Selected);
        if (World.isClientSide) {
            return;
        }
        long Now = World.getGameTime();
        Scp127Levels.regrow(Stack, Now);
        if (Holder instanceof ServerPlayer User) {
            Scp127Life.tick(User, Stack, Now);
        }
    }

    @Override
    public int getBarColor(ItemStack Stack) {
        return BarColour;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        GunStats Stats = getKind().getStats();
        int Rank = Scp127Levels.rankOf(Stack);
        int Kills = Scp127Levels.killsOf(Stack);
        int Capacity = Scp127Levels.capacity();
        int Rounds = getAmmo(Stack);
        Style Accent = Style.EMPTY.withColor(Scp127Life.LevelColour);
        Lines.add(Component.translatable(Prefix + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (Rank >= Scp127Levels.MaxLevel) {
            Lines.add(Component.translatable(TooltipPrefix + "level_max", Scp127Levels.numeral(Rank), Kills).withStyle(Accent));
        } else {
            Lines.add(Component.translatable(TooltipPrefix + "level", Scp127Levels.numeral(Rank), Kills, Scp127Levels.killsFor(Rank + 1)).withStyle(Accent));
        }
        Lines.add(Component.translatable(TooltipPrefix + (Rounds < Capacity ? "regrowing" : "rounds"), Rounds, Capacity).withStyle(ChatFormatting.GOLD));
        Level World = Context.level();
        if (World != null && Scp127Life.isHungry(Stack, World.getGameTime())) {
            Lines.add(Component.translatable(TooltipPrefix + "restless").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        }
        if (!ItemTooltips.showDetails(Lines)) {
            return;
        }
        Lines.add(Component.translatable("tooltip.scp_unleashed.guns.damage", number(Scp127Levels.damageAt(Rank))).withStyle(ChatFormatting.DARK_GREEN));
        Lines.add(Component.translatable("tooltip.scp_unleashed.guns.action", Component.translatable("tooltip.scp_unleashed.guns.action." + getKind().getAction()),
                Math.round(1200.0F / Stats.getInterval())).withStyle(ChatFormatting.DARK_GREEN));
        Lines.add(Component.translatable("tooltip.scp_unleashed.guns.range", Math.round(Stats.getRange())).withStyle(ChatFormatting.DARK_GREEN));
        Lines.add(Component.translatable(TooltipPrefix + "regrowth", Component.translatable(TooltipPrefix + "regrowth." + Rank)).withStyle(ChatFormatting.DARK_GREEN));
        Lines.add(Component.translatable(TooltipPrefix + "no_reload").withStyle(ChatFormatting.GRAY));
        Lines.add(Component.translatable(TooltipPrefix + "storage").withStyle(ChatFormatting.GRAY));
        Lines.add(Component.translatable("tooltip.scp_unleashed.guns.controls", Component.keybind("key.use"), Component.keybind("key.attack")).withStyle(ChatFormatting.GRAY));
    }

    private static String number(float Value) {
        return Value == Math.floor(Value) ? Integer.toString((int) Value) : String.format(Locale.ROOT, "%.1f", Value);
    }
}
