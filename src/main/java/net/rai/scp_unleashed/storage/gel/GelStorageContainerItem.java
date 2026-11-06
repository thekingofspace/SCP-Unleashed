package net.rai.scp_unleashed.storage.gel;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.rai.scp_unleashed.scp513.Scp513Chimes;

import java.util.List;

public class GelStorageContainerItem extends BlockItem {
    public GelStorageContainerItem(Block Owner, Properties Settings) {
        super(Owner, Settings);
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack Stack, ItemEntity Dropped) {
        if (Dropped.level() instanceof ServerLevel Server && (Dropped.tickCount + Dropped.getId()) % Scp513Chimes.CheckEvery == 0 && Scp513Chimes.holdsBell(Stack)) {
            Scp513Chimes.tickLoose(Server, Dropped);
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        GelPacked Packed = Stack.get(GelStorageRegistry.Packed.get());
        if (Packed != null && !Packed.Contents().isEmpty()) {
            Lines.add(Component.translatable("block.scp_unleashed.storage_container_gel.holds", Packed.Contents().getHoverName()).withStyle(ChatFormatting.GRAY));
        }
        if (Packed != null && Packed.Gel() > 0) {
            Lines.add(Component.translatable("block.scp_unleashed.storage_container_gel.gel_left", GelRules.percent(Packed.Gel())).withStyle(ChatFormatting.DARK_AQUA));
        }
        super.appendHoverText(Stack, Context, Lines, Flag);
    }
}
