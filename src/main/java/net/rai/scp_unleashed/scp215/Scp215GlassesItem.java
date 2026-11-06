package net.rai.scp_unleashed.scp215;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Scp215GlassesItem extends Item implements Equipable {
    public static final String Prefix = "item.scp_unleashed.scp215_glasses.";

    public Scp215GlassesItem(Properties Settings) {
        super(Settings);
    }

    public static boolean is(ItemStack Stack) {
        return !Stack.isEmpty() && Stack.getItem() instanceof Scp215GlassesItem;
    }

    public static boolean isWorn(@Nullable LivingEntity Wearer) {
        return Wearer != null && is(Wearer.getItemBySlot(EquipmentSlot.HEAD));
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public Holder<SoundEvent> getEquipSound() {
        return Scp215Registry.GlassesOn;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        return swapWithEquipmentSlot(this, World, User, Hand);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Prefix + "tooltip").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Prefix + "wear").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Prefix + "sight").withStyle(ChatFormatting.DARK_PURPLE));
            Lines.add(Component.translatable(Prefix + "others").withStyle(ChatFormatting.DARK_RED));
            Lines.add(Component.translatable(Prefix + "contain").withStyle(ChatFormatting.DARK_GREEN));
        }
    }
}
