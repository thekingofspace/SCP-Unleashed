package net.rai.scp_unleashed.block;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public final class FluidHands {
    private FluidHands() {
    }

    public static boolean interact(Player User, InteractionHand Hand, IFluidHandler Handler) {
        ItemStack Held = User.getItemInHand(Hand);
        if (Held.isEmpty()) {
            return false;
        }
        ItemStack Single = Held.copyWithCount(1);
        FluidActionResult Filled = FluidUtil.tryFillContainer(Single, Handler, Integer.MAX_VALUE, User, true);
        if (Filled.isSuccess()) {
            User.setItemInHand(Hand, filledHand(User, Held, Filled.getResult()));
            return true;
        }
        FluidActionResult Emptied = FluidUtil.tryEmptyContainer(Single, Handler, Integer.MAX_VALUE, User, true);
        if (Emptied.isSuccess()) {
            if (!User.hasInfiniteMaterials()) {
                User.setItemInHand(Hand, ItemUtils.createFilledResult(Held, User, Emptied.getResult(), false));
            }
            return true;
        }
        return false;
    }

    private static ItemStack filledHand(Player User, ItemStack Held, ItemStack Result) {
        if (!User.hasInfiniteMaterials()) {
            return ItemUtils.createFilledResult(Held, User, Result, false);
        }
        if (Held.getCount() <= 1) {
            return Result;
        }
        Held.shrink(1);
        if (!User.getInventory().add(Result)) {
            User.drop(Result, false);
        }
        return Held;
    }
}
