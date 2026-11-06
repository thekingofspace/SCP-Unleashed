package net.rai.scp_unleashed.scpsign;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ScpSignItem extends BlockItem {
    public ScpSignItem(Block Placed, Properties Settings) {
        super(Placed, Settings);
    }

    @Nullable
    public static AnomalyDocument shown(ItemStack Stack) {
        ResourceLocation Id = Stack.get(ScpSignRegistry.DocumentComponent.get());
        return Id == null ? null : AnomalyDocuments.lookup(Id);
    }

    @Override
    public Component getName(ItemStack Stack) {
        Component Base = super.getName(Stack);
        AnomalyDocument Document = shown(Stack);
        return Document == null ? Base : Component.translatable(ScpSignBlock.Key + "named", Base, Document.designation());
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(ScpSignBlock.Key + "tooltip").withStyle(ChatFormatting.GRAY));
        ResourceLocation Id = Stack.get(ScpSignRegistry.DocumentComponent.get());
        AnomalyDocument Document = shown(Stack);
        if (Id == null) {
            Lines.add(Component.translatable(ScpSignBlock.Key + "unassigned").withStyle(ChatFormatting.DARK_GRAY));
        } else if (Document == null) {
            Lines.add(Component.translatable(ScpSignBlock.Key + "unreadable").withStyle(ChatFormatting.DARK_RED));
        } else {
            DocumentClass Classification = Document.Classification();
            Lines.add(Component.translatable(ScpSignBlock.Key + "shows", Document.designation(), Document.title())
                    .withStyle(Look -> Look.withColor(Classification.getColour() & 0xFFFFFF)));
        }
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(ScpSignBlock.Key + "assign").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(ScpSignBlock.Key + "clear").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(ScpSignBlock.Key + "glow").withStyle(ChatFormatting.GOLD));
            if (getBlock() instanceof ScpWideSignBlock) {
                Lines.add(Component.translatable(ScpSignBlock.Key + "room").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }
}
