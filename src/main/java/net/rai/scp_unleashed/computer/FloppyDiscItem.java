package net.rai.scp_unleashed.computer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.rai.scp_unleashed.computer.data.ComputerFile;
import net.rai.scp_unleashed.computer.data.FloppyContents;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class FloppyDiscItem extends Item {
    public FloppyDiscItem(Properties Properties) {
        super(Properties);
    }

    public static FloppyContents contentsOf(ItemStack Stack) {
        return Stack.getOrDefault(ComputerRegistry.FloppyComponent.get(), FloppyContents.Empty);
    }

    public static void setContents(ItemStack Stack, FloppyContents Contents) {
        if (Contents.isEmpty()) {
            Stack.remove(ComputerRegistry.FloppyComponent.get());
        } else {
            Stack.set(ComputerRegistry.FloppyComponent.get(), Contents);
        }
    }

    public boolean isWritable(ItemStack Stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        FloppyContents Contents = contentsOf(Stack);
        if (Contents.isEmpty()) {
            Lines.add(Component.translatable("item.scp_unleashed.floppy_disc.blank").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            Lines.add(Component.translatable("item.scp_unleashed.floppy_disc.files", Contents.Files().size(), FloppyContents.Capacity).withStyle(ChatFormatting.GRAY));
            for (ComputerFile File : Contents.Files()) {
                Lines.add(Component.translatable(File.isDossier() ? "item.scp_unleashed.floppy_disc.dossier" : "item.scp_unleashed.floppy_disc.note", File.Name())
                        .withStyle(File.isDossier() ? ChatFormatting.AQUA : ChatFormatting.WHITE));
            }
        }
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.floppy_disc.capacity", FloppyContents.Capacity).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.floppy_disc.use").withStyle(ChatFormatting.GOLD));
        }
    }
}
