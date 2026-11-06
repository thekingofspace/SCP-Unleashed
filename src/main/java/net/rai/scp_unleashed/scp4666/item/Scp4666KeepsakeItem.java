package net.rai.scp_unleashed.scp4666.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;
import java.util.function.Supplier;

public class Scp4666KeepsakeItem extends Item {
    public static final int Cooldown = 60;
    private final String Prefix;
    private final Supplier<SoundEvent> Sound;
    private final float Pitch;

    public Scp4666KeepsakeItem(Properties Properties, String Name, Supplier<SoundEvent> Sound, float Pitch) {
        super(Properties);
        this.Prefix = "item.scp_unleashed." + Name + ".";
        this.Sound = Sound;
        this.Pitch = Pitch;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Held = User.getItemInHand(Hand);
        if (!World.isClientSide) {
            World.playSound(null, User.getX(), User.getY(), User.getZ(), Sound.get(), SoundSource.PLAYERS, 0.6f, Pitch + (World.getRandom().nextFloat() - 0.5f) * 0.1f);
            User.displayClientMessage(Component.translatable(Prefix + "use").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            User.getCooldowns().addCooldown(this, Cooldown);
        }
        return InteractionResultHolder.sidedSuccess(Held, World.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Prefix + "tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Prefix + "detail").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
