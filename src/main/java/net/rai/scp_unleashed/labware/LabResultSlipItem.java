package net.rai.scp_unleashed.labware;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class LabResultSlipItem extends Item {
    public static final String Marker = "■ ";

    public LabResultSlipItem(Properties Settings) {
        super(Settings);
    }

    public static ItemStack of(AssayRecord Result) {
        ItemStack Stack = new ItemStack(LabwareRegistry.ResultSlip.get());
        Stack.set(LabwareRegistry.Assay.get(), Result);
        return Stack;
    }

    public static MutableComponent findingLine(ResourceLocation Finding) {
        return Component.literal(Marker).withColor(LabOrganisms.colourOf(Finding)).append(LabOrganisms.nameOf(Finding).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public Component getName(ItemStack Stack) {
        AssayRecord Result = Stack.get(LabwareRegistry.Assay.get());
        if (Result == null) {
            return Component.translatable("item.scp_unleashed.lab_result_slip");
        }
        return Component.translatable(Result.isPositive() ? "item.scp_unleashed.lab_result_slip.positive" : "item.scp_unleashed.lab_result_slip.clean");
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        AssayRecord Result = Stack.get(LabwareRegistry.Assay.get());
        if (Result == null) {
            Lines.add(Component.translatable("item.scp_unleashed.lab_result_slip.blank").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        Lines.add(Component.translatable("item.scp_unleashed.lab_result_slip.subject", Result.subjectName()).withStyle(ChatFormatting.GRAY));
        Lines.add(Component.translatable("item.scp_unleashed.lab_result_slip.sampled", LabClock.stamp(Result.Sampled())).withStyle(ChatFormatting.GRAY));
        if (!Result.isPositive()) {
            Lines.add(Component.translatable("item.scp_unleashed.lab_result_slip.negative").withStyle(ChatFormatting.GREEN));
        } else {
            Lines.add(Component.translatable("item.scp_unleashed.lab_result_slip.found", Result.Findings().size()).withStyle(ChatFormatting.RED));
            for (ResourceLocation Finding : Result.Findings()) {
                Lines.add(Component.literal(" ").append(findingLine(Finding)));
            }
        }
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.lab_result_slip.printed", LabClock.stamp(Result.Printed())).withStyle(ChatFormatting.DARK_GRAY));
            Lines.add(Component.translatable("item.scp_unleashed.lab_result_slip.detail").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
