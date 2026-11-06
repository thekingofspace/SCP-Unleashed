package net.rai.scp_unleashed.z0001;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.z0001.client.Z0001ClientHooks;

import java.util.List;

public class Z0001BookItem extends Item {
    public static final float OpenVolume = 0.7f;

    public Z0001BookItem(Properties Settings) {
        super(Settings);
    }

    public static boolean is(ItemStack Stack) {
        return Stack.getItem() instanceof Z0001BookItem;
    }

    public static ItemStack create() {
        return new ItemStack(Z0001Registry.Book.get());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        if (World.isClientSide) {
            Z0001ClientHooks.openBook(Hand);
        } else {
            for (DeferredHolder<SoundEvent, SoundEvent> Layer : Z0001Registry.OpenLayers) {
                World.playSound(User, User.getX(), User.getY(), User.getZ(), Layer.get(), SoundSource.PLAYERS, OpenVolume, 1.0f);
            }
            User.awardStat(Stats.ITEM_USED.get(this));
        }
        return InteractionResultHolder.sidedSuccess(Stack, World.isClientSide);
    }

    @Override
    public boolean canBeHurtBy(ItemStack Stack, DamageSource Source) {
        return !Source.is(DamageTypeTags.IS_FIRE) && !Source.is(DamageTypeTags.IS_EXPLOSION);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        String Prefix = getDescriptionId();
        Lines.add(Component.translatable(Prefix + ".tooltip").withStyle(ChatFormatting.GRAY));
        Level World = Context.level();
        if (World != null && Z0001Ink.isSpent(Stack, Z0001Ink.today(World))) {
            Lines.add(Component.translatable(Prefix + ".tooltip.written").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
        if (!ItemTooltips.showDetails(Lines)) {
            return;
        }
        Lines.add(Component.translatable(Prefix + ".detail.open", Component.keybind("key.use")).withStyle(ChatFormatting.GRAY));
        Lines.add(Component.translatable(Prefix + ".detail.page").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        Lines.add(Component.translatable(Prefix + ".detail.morning").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        Lines.add(Component.translatable(Prefix + ".detail.fire").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
