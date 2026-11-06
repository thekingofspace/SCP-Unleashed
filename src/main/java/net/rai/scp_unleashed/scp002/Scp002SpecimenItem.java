package net.rai.scp_unleashed.scp002;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class Scp002SpecimenItem extends Item {
    public Scp002SpecimenItem(Properties Properties) {
        super(Properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext Context) {
        Player User = Context.getPlayer();
        if (User == null) {
            return InteractionResult.PASS;
        }
        if (!User.isCreative()) {
            if (!Context.getLevel().isClientSide) {
                User.displayClientMessage(Component.translatable("item.scp_unleashed.scp002_specimen.creative_only").withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.FAIL;
        }
        if (User.isShiftKeyDown() && !Scp002Strikes.fallsIn(Context.getLevel())) {
            if (!Context.getLevel().isClientSide) {
                User.displayClientMessage(Component.translatable("item.scp_unleashed.scp002_specimen.overworld").withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.FAIL;
        }
        if (!(Context.getLevel() instanceof ServerLevel World)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos Ground = Context.getClickedPos();
        Direction Front = Scp002Impact.frontToward(Ground, User.position(), World.random);
        if (User.isShiftKeyDown()) {
            Scp002Strikes.get(World.getServer()).strikeAt(World, Ground, User.position());
            User.displayClientMessage(Component.translatable("item.scp_unleashed.scp002_specimen.called").withStyle(ChatFormatting.GOLD), true);
        } else {
            Scp002Structure.build(World, Ground, Front, true, Pos -> !Scp002Impact.isProtected(World, Pos, World.getBlockState(Pos)));
            Scp002Impact.clearPorch(World, Ground, Front, World.random);
            World.playSound(null, Ground.getX() + 0.5, Ground.getY() + 1.0, Ground.getZ() + 0.5, Scp002Registry.FleshSquelch.get(), SoundSource.BLOCKS, 1.0f, 0.6f);
            User.displayClientMessage(Component.translatable("item.scp_unleashed.scp002_specimen.placed").withStyle(ChatFormatting.GOLD), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed.scp002_specimen.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.scp002_specimen.place").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.scp002_specimen.strike").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.scp002_specimen.creative").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
