package net.rai.scp_unleashed.senses;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class SenseItem extends Item implements Equipable {
    public enum Kind {
        Hearing,
        Sight
    }

    private final Kind Sense;

    public SenseItem(Kind Sense, Properties Properties) {
        super(Properties);
        this.Sense = Sense;
    }

    public Kind getSense() {
        return Sense;
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public Holder<SoundEvent> getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_LEATHER;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        return swapWithEquipmentSlot(this, World, User, Hand);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(getDescriptionId() + ".detail").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Senses.curiosLoaded() ? "tooltip.scp_unleashed.senses.worn_curios." + slot() : "tooltip.scp_unleashed.senses.worn_head")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private String slot() {
        return Sense == Kind.Hearing ? SensesRegistry.EarsSlot : SensesRegistry.HeadSlot;
    }
}
