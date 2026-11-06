package net.rai.scp_unleashed.door;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ScrewdriverItem extends Item {
    public static final int Durability = 128;

    public ScrewdriverItem(Properties Properties) {
        super(Properties);
    }

    @Nullable
    public static Screwdrivable targetAt(Level World, BlockPos Pos, BlockState State) {
        if (State.getBlock() instanceof Screwdrivable Device) {
            return Device;
        }
        return World.getBlockEntity(Pos) instanceof Screwdrivable Owner ? Owner : null;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack Stack, UseOnContext Context) {
        Level World = Context.getLevel();
        BlockPos Pos = Context.getClickedPos();
        BlockState State = World.getBlockState(Pos);
        Screwdrivable Target = targetAt(World, Pos, State);
        Player User = Context.getPlayer();
        boolean Sneaking = User != null && User.isSecondaryUseActive();
        if (Target == null || !Target.accepts(State, Sneaking)) {
            return InteractionResult.PASS;
        }
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        InteractionResult Result = Target.screwdrive(World, Pos, State, User, Sneaking);
        if (Result.consumesAction()) {
            World.playSound(null, Pos, DoorSounds.Screwdriver.get(), SoundSource.BLOCKS, 0.7f, 0.92f + World.getRandom().nextFloat() * 0.16f);
            World.gameEvent(User, GameEvent.BLOCK_CHANGE, Pos);
            if (User != null) {
                Stack.hurtAndBreak(1, User, LivingEntity.getSlotForHand(Context.getHand()));
            }
        }
        return Result;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed.screwdriver.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.screwdriver.raise").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.screwdriver.pulse").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.screwdriver.creative_source").withStyle(ChatFormatting.GOLD));
        }
    }
}
