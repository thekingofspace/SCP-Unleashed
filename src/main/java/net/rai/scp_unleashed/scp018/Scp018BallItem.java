package net.rai.scp_unleashed.scp018;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class Scp018BallItem extends Item {
    public Scp018BallItem(Properties Properties) {
        super(Properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        boolean Gentle = User.isSecondaryUseActive();
        World.playSound(null, User.getX(), User.getEyeY(), User.getZ(), Scp018Registry.Throw.get(), SoundSource.PLAYERS, Gentle ? 0.4f : 0.6f,
                (Gentle ? 1.15f : 0.95f) + World.getRandom().nextFloat() * 0.1f);
        if (World instanceof ServerLevel Server) {
            Scp018Ball.thrown(Server, User, Stack, Gentle ? BallTuning.TossSpeed : BallTuning.ThrowSpeed);
        }
        User.getCooldowns().addCooldown(this, BallTuning.ThrowCooldown);
        User.awardStat(Stats.ITEM_USED.get(this));
        Stack.consume(1, User);
        return InteractionResultHolder.sidedSuccess(Stack, World.isClientSide());
    }

    @Override
    public boolean canBeHurtBy(ItemStack Stack, DamageSource Source) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed.scp018_ball.tooltip").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.scp018_ball.throw").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.scp018_ball.toss").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.scp018_ball.gain").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.scp018_ball.catch").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.scp018_ball.danger").withStyle(ChatFormatting.RED));
        }
    }
}
