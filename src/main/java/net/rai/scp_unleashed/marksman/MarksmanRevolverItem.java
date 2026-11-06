package net.rai.scp_unleashed.marksman;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.guns.GunStats;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.marksman.ability.ToolAbilityItem;

import java.util.List;
import java.util.Locale;

public class MarksmanRevolverItem extends GunItem implements ToolAbilityItem {
    public static final String Prefix = "item.scp_unleashed.marksman_revolver";
    public static final int NameColour = 0xE8574A;

    public MarksmanRevolverItem(Properties Settings) {
        super(GunKind.Revolver, Settings);
    }

    public static ItemStack loaded() {
        ItemStack Stack = new ItemStack(MarksmanRegistry.MarksmanRevolver.get());
        setAmmo(Stack, GunKind.Revolver.getStats().getCapacity());
        CoinToss.setCoins(Stack, CoinToss.MaxCoins);
        return Stack;
    }

    @Override
    public Component getName(ItemStack Stack) {
        return super.getName(Stack).copy().withStyle(Style.EMPTY.withColor(NameColour));
    }

    @Override
    public void useAbility(ServerPlayer User, ItemStack Stack, InteractionHand Hand) {
        CoinToss.toss(User, Stack);
    }

    @Override
    public boolean acceptsAbility(ItemStack Stack, InteractionHand Hand) {
        return Hand == InteractionHand.MAIN_HAND;
    }

    @Override
    public void inventoryTick(ItemStack Stack, Level World, Entity Holder, int Slot, boolean Selected) {
        super.inventoryTick(Stack, World, Holder, Slot, Selected);
        if (!World.isClientSide) {
            CoinToss.recharge(Stack, World.getGameTime());
        }
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        GunStats Stats = getKind().getStats();
        Lines.add(Component.translatable(Prefix + ".tooltip").withStyle(ChatFormatting.GRAY));
        Lines.add(Component.translatable("tooltip.scp_unleashed.guns.loaded", getAmmo(Stack), Stats.getCapacity(), getKind().getAmmo().getDescription())
                .withStyle(ChatFormatting.GOLD));
        Lines.add(Component.translatable("tooltip.scp_unleashed.marksman.coins", CoinToss.getCoins(Stack), CoinToss.MaxCoins).withStyle(ChatFormatting.YELLOW));
        Lines.add(Component.translatable("tooltip.scp_unleashed.marksman.coin_hint", Component.keybind(AbilityKey)).withStyle(ChatFormatting.GRAY));
        if (!ItemTooltips.showDetails(Lines)) {
            return;
        }
        Lines.add(Component.translatable("tooltip.scp_unleashed.guns.damage", number(Stats.getDamage())).withStyle(ChatFormatting.DARK_GREEN));
        Lines.add(Component.translatable("tooltip.scp_unleashed.guns.action", Component.translatable("tooltip.scp_unleashed.guns.action." + getKind().getAction()),
                Math.round(1200.0F / Stats.getInterval())).withStyle(ChatFormatting.DARK_GREEN));
        Lines.add(Component.translatable("tooltip.scp_unleashed.guns.range", Math.round(Stats.getRange())).withStyle(ChatFormatting.DARK_GREEN));
        Lines.add(Component.translatable("tooltip.scp_unleashed.guns.reload_time", number(Stats.getReloadTicks() / 20.0F)).withStyle(ChatFormatting.DARK_GREEN));
        Lines.add(Component.translatable("tooltip.scp_unleashed.marksman.ricochet_damage", number(CoinRicochet.BaseBonus)).withStyle(ChatFormatting.DARK_GREEN));
        Lines.add(Component.translatable("tooltip.scp_unleashed.marksman.recharge", number(CoinToss.RechargeTicks / 20.0F)).withStyle(ChatFormatting.DARK_GREEN));
        Lines.add(Component.translatable("tooltip.scp_unleashed.marksman.ricochet_hint").withStyle(ChatFormatting.GRAY));
        Lines.add(Component.translatable("tooltip.scp_unleashed.guns.controls", Component.keybind("key.use"), Component.keybind("key.attack")).withStyle(ChatFormatting.GRAY));
        Lines.add(Component.translatable("tooltip.scp_unleashed.guns.reload_hint", Component.keybind(ReloadKey)).withStyle(ChatFormatting.GRAY));
    }

    private static String number(float Value) {
        return Value == Math.floor(Value) ? Integer.toString((int) Value) : String.format(Locale.ROOT, "%.1f", Value);
    }
}
