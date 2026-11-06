package net.rai.scp_unleashed.storage;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class StorageUpgradeItem extends Item {
    private final StorageEnvironment Environment;

    public StorageUpgradeItem(StorageEnvironment Environment, Properties Properties) {
        super(Properties);
        this.Environment = Environment;
    }

    public StorageEnvironment getEnvironment() {
        return Environment;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack Stack, UseOnContext Context) {
        Level World = Context.getLevel();
        BlockPos Pos = Context.getClickedPos();
        BlockState State = World.getBlockState(Pos);
        if (!(State.getBlock() instanceof StorageContainerBlock Owner)) {
            return InteractionResult.PASS;
        }
        StorageContainerBlockEntity Locker = StorageContainerBlock.containerAt(World, Pos);
        if (Locker == null) {
            return InteractionResult.PASS;
        }
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Player User = Context.getPlayer();
        StorageEnvironment Current = State.getValue(StorageContainerBlock.Environment);
        Component Name = Stack.getHoverName();
        if (Current == Environment) {
            if (User != null) {
                User.displayClientMessage(Component.translatable("message.scp_unleashed.storage.upgrade_already", Name), true);
            }
            return InteractionResult.FAIL;
        }
        ItemStack Returned = Current.upgradeStack();
        Owner.setEnvironment(World, Pos, State, Environment);
        if (User != null) {
            Stack.consume(1, User);
            StorageContainerBlock.give(World, Pos, User, Returned.copy());
            User.displayClientMessage(Returned.isEmpty() ? Component.translatable("message.scp_unleashed.storage.upgrade_installed", Name)
                    : Component.translatable("message.scp_unleashed.storage.upgrade_swapped", Name, Returned.getHoverName()), true);
        } else {
            Stack.shrink(1);
            StorageContainerBlock.give(World, Pos, null, Returned);
        }
        Locker.upgraded();
        World.gameEvent(User, GameEvent.BLOCK_CHANGE, Pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.storage_upgrade.fits").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.storage_upgrade.install").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.storage_upgrade.remove").withStyle(ChatFormatting.GOLD));
        }
    }
}
