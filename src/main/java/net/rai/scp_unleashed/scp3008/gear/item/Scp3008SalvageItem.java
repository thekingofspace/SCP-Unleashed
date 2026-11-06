package net.rai.scp_unleashed.scp3008.gear.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Scp3008SalvageItem extends Item {
    public static final int LegBurnTicks = 150;
    public static final int DoorBurnTicks = 300;
    private final int BurnTicks;

    public Scp3008SalvageItem(Properties Settings, int BurnTicks) {
        super(Settings);
        this.BurnTicks = BurnTicks;
    }

    @Override
    public int getBurnTime(ItemStack Stack, @Nullable RecipeType<?> Recipe) {
        return BurnTicks;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
    }
}
