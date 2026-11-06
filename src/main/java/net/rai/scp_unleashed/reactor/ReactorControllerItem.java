package net.rai.scp_unleashed.reactor;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class ReactorControllerItem extends BlockItem {
    public ReactorControllerItem(Block Placed, Properties Properties) {
        super(Placed, Properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext Context) {
        Player User = Context.getPlayer();
        Level World = Context.getLevel();
        BlockPos Clicked = Context.getClickedPos();
        if (User != null && User.isShiftKeyDown() && World.getBlockState(Clicked).getBlock() instanceof TowerSegmentBlock) {
            if (!World.isClientSide) {
                ItemStack Stack = Context.getItemInHand();
                Stack.set(ReactorRegistry.ReactorLink.get(), GlobalPos.of(World.dimension(), Clicked.immutable()));
                User.displayClientMessage(Component.translatable("message.scp_unleashed.reactor_controller.bound", Clicked.getX(), Clicked.getY(), Clicked.getZ())
                        .withStyle(ChatFormatting.GREEN), true);
                World.playSound(null, Clicked, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.4f, 1.4f);
            }
            return InteractionResult.sidedSuccess(World.isClientSide);
        }
        return super.useOn(Context);
    }

    @Override
    public boolean isFoil(ItemStack Stack) {
        return Stack.has(ReactorRegistry.ReactorLink.get()) || super.isFoil(Stack);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        super.appendHoverText(Stack, Context, Lines, Flag);
        GlobalPos Link = Stack.get(ReactorRegistry.ReactorLink.get());
        if (Link != null) {
            BlockPos Pos = Link.pos();
            Lines.add(Component.translatable("item.scp_unleashed.reactor_controller.bound", Pos.getX(), Pos.getY(), Pos.getZ()).withStyle(ChatFormatting.AQUA));
        }
    }
}
