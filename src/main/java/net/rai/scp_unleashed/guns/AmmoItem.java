package net.rai.scp_unleashed.guns;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class AmmoItem extends Item {
    public AmmoItem(Properties Properties) {
        super(Properties);
    }

    public int rounds(ItemStack Stack) {
        return Stack.getCount();
    }

    public int take(Player User, ItemStack Stack, int Wanted) {
        int Used = Math.min(Wanted, Stack.getCount());
        Stack.shrink(Used);
        return Used;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (!ItemTooltips.showDetails(Lines)) {
            return;
        }
        MutableComponent Users = null;
        for (GunKind Kind : GunKind.values()) {
            if (Kind.usesAmmo() && Kind.getAmmo() == this) {
                Component Name = Component.translatable("item.scp_unleashed." + Kind.getName());
                Users = Users == null ? Name.copy() : Users.append(Component.literal(", ")).append(Name);
            }
        }
        if (Users != null) {
            Lines.add(Component.translatable("tooltip.scp_unleashed.guns.ammo_for", Users).withStyle(ChatFormatting.GOLD));
        }
        Lines.add(Component.translatable("tooltip.scp_unleashed.guns.ammo_reload", Component.keybind(GunItem.ReloadKey)).withStyle(ChatFormatting.GRAY));
    }
}
