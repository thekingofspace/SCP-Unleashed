package net.rai.scp_unleashed.food;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class SnackItem extends Item {
    private static final float OpenVolume = 0.8f;

    private final Snack Product;

    public SnackItem(Snack Product, Properties Properties) {
        super(Properties.food(Product.food()).stacksTo(Product.getKind().getStackSize()));
        this.Product = Product;
    }

    public Snack getProduct() {
        return Product;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        InteractionResultHolder<ItemStack> Result = super.use(World, User, Hand);
        if (Result.getResult().consumesAction()) {
            float Pitch = 0.92f + World.getRandom().nextFloat() * 0.16f;
            for (SoundEvent Layer : Product.getKind().getOpenSounds()) {
                World.playSound(User, User.getX(), User.getEyeY(), User.getZ(), Layer, SoundSource.PLAYERS, OpenVolume, Pitch);
            }
        }
        return Result;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack Stack) {
        return Product.getKind().isDrink() ? UseAnim.DRINK : UseAnim.EAT;
    }

    @Override
    public SoundEvent getDrinkingSound() {
        return FoodSounds.Gulp.get();
    }

    @Override
    public SoundEvent getEatingSound() {
        return Product.getKind().isDrink() ? FoodSounds.Gulp.get() : super.getEatingSound();
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
        MobEffectInstance Effect = Product.effect();
        if (Effect != null) {
            MutableComponent Name = Component.translatable(Effect.getDescriptionId());
            if (Effect.getAmplifier() > 0) {
                Name = Component.translatable("potion.withAmplifier", Name, Component.translatable("potion.potency." + Effect.getAmplifier()));
            }
            Lines.add(Component.translatable("potion.withDuration", Name, MobEffectUtil.formatDuration(Effect, 1.0f, Context.tickRate()))
                    .withStyle(Effect.getEffect().value().getCategory().getTooltipFormatting()));
        }
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("item.scp_unleashed.food.price", Product.getPrice(), Product.getCode()).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("item.scp_unleashed.food.vended").withStyle(ChatFormatting.GOLD));
        }
    }
}
