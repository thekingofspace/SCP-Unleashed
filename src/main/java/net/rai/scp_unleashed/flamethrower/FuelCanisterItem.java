package net.rai.scp_unleashed.flamethrower;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.rai.scp_unleashed.guns.AmmoItem;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.scp939.Scp939Registry;

import java.util.List;

public class FuelCanisterItem extends AmmoItem {
    public static final int Capacity = 100;
    public static final int BarColour = 0xF0862E;
    public static final String TooltipPrefix = "tooltip.scp_unleashed.fuel_canister.";

    public FuelCanisterItem(Properties Properties) {
        super(Properties);
    }

    public static int fuel(ItemStack Stack) {
        return Mth.clamp(Stack.getOrDefault(FlamerRegistry.CanisterFuel.get(), Capacity), 0, Capacity);
    }

    public static void setFuel(ItemStack Stack, int Fuel) {
        if (Fuel >= Capacity) {
            Stack.remove(FlamerRegistry.CanisterFuel.get());
        } else {
            Stack.set(FlamerRegistry.CanisterFuel.get(), Math.max(0, Fuel));
        }
    }

    public static ItemStack partial(int Fuel) {
        ItemStack Stack = new ItemStack(FlamerRegistry.FuelCanister.get());
        setFuel(Stack, Fuel);
        return Stack;
    }

    @Override
    public int rounds(ItemStack Stack) {
        return Stack.getCount() * fuel(Stack);
    }

    @Override
    public int take(Player User, ItemStack Stack, int Wanted) {
        int Taken = 0;
        while (Taken < Wanted && !Stack.isEmpty()) {
            int Need = Wanted - Taken;
            int Here = fuel(Stack);
            if (Here <= Need) {
                Taken += Here;
                Stack.shrink(1);
                giveBack(User, new ItemStack(Scp939Registry.EmptyCanister.get()));
                continue;
            }
            if (Stack.getCount() == 1) {
                setFuel(Stack, Here - Need);
            } else {
                ItemStack Part = Stack.copyWithCount(1);
                setFuel(Part, Here - Need);
                Stack.shrink(1);
                giveBack(User, Part);
            }
            Taken += Need;
        }
        return Taken;
    }

    private static void giveBack(Player User, ItemStack Given) {
        if (!User.getInventory().add(Given)) {
            User.drop(Given, false);
        }
    }

    @Override
    public boolean isBarVisible(ItemStack Stack) {
        return fuel(Stack) < Capacity;
    }

    @Override
    public int getBarWidth(ItemStack Stack) {
        return Math.round(13.0F * fuel(Stack) / Capacity);
    }

    @Override
    public int getBarColor(ItemStack Stack) {
        return BarColour;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
        Lines.add(Component.translatable(TooltipPrefix + "fuel", fuel(Stack), Capacity).withStyle(ChatFormatting.GOLD));
        if (!ItemTooltips.showDetails(Lines)) {
            return;
        }
        Lines.add(Component.translatable("tooltip.scp_unleashed.guns.ammo_for", Component.translatable("item.scp_unleashed.flamethrower")).withStyle(ChatFormatting.GOLD));
        Lines.add(Component.translatable(TooltipPrefix + "empty").withStyle(ChatFormatting.GRAY));
        Lines.add(Component.translatable("tooltip.scp_unleashed.guns.ammo_reload", Component.keybind(GunItem.ReloadKey)).withStyle(ChatFormatting.GRAY));
    }
}
