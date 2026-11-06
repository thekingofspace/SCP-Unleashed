package net.rai.scp_unleashed.scp539;

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
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class Scp539StoneItem extends Item {
    public static final String Prefix = "item.scp_unleashed.scp539_stone";
    public static final int ThrowCooldown = 30;
    public static final float ThrowVolume = 0.6f;

    public Scp539StoneItem(Properties Settings) {
        super(Settings);
    }

    public static boolean is(ItemStack Stack) {
        return Stack.getItem() instanceof Scp539StoneItem;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        if (World instanceof ServerLevel Server) {
            Scp539Registry.playLayers(Server, new Vec3(User.getX(), User.getEyeY(), User.getZ()), Scp539Registry.ThrowLayers, SoundSource.PLAYERS, ThrowVolume,
                    0.95f + Server.getRandom().nextFloat() * 0.1f);
            Scp539Stone.thrown(Server, User, Stack);
        }
        User.getCooldowns().addCooldown(this, ThrowCooldown);
        User.awardStat(Stats.ITEM_USED.get(this));
        Stack.shrink(1);
        return InteractionResultHolder.sidedSuccess(Stack, World.isClientSide());
    }

    @Override
    public boolean canBeHurtBy(ItemStack Stack, DamageSource Source) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Prefix + ".tooltip").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Prefix + ".detail.0").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Prefix + ".detail.1").withStyle(ChatFormatting.GOLD));
        }
    }
}
