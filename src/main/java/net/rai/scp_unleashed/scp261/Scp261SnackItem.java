package net.rai.scp_unleashed.scp261;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.food.FoodSounds;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class Scp261SnackItem extends Item {
    public static final int ChewCooldown = 16;
    public static final int ChewNutrition = 1;
    public static final float ChewSaturation = 0.1f;
    public static final String SharedKey = "item.scp_unleashed.scp261_snack.";

    private final Scp261Snack Treat;

    public Scp261SnackItem(Scp261Snack Treat, Properties Properties) {
        super(Treat.food() == null ? Properties : Properties.food(Treat.food()));
        this.Treat = Treat;
    }

    public Scp261Snack getSnack() {
        return Treat;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        if (Treat.getKind() != Scp261Snack.Kind.Chew) {
            InteractionResultHolder<ItemStack> Result = super.use(World, User, Hand);
            if (Result.getResult().consumesAction()) {
                openSound(World, User);
            }
            return Result;
        }
        if (!World.isClientSide && User instanceof ServerPlayer Server) {
            Scp261Registry.playCue(World, User.getEyePosition(), Scp261Registry.ChewCue, SoundSource.PLAYERS, 0.7f, 0.95f + World.getRandom().nextFloat() * 0.15f);
            CriteriaTriggers.CONSUME_ITEM.trigger(Server, Stack);
            Server.awardStat(Stats.ITEM_USED.get(this));
            Server.getFoodData().eat(ChewNutrition, ChewSaturation);
            World.gameEvent(User, GameEvent.EAT, User.position());
            Scp261Effects.consume(Server.serverLevel(), Server, Treat);
        }
        User.getCooldowns().addCooldown(this, ChewCooldown);
        Stack.consume(1, User);
        return InteractionResultHolder.sidedSuccess(Stack, World.isClientSide);
    }

    private void openSound(Level World, Player User) {
        float Pitch = 0.92f + World.getRandom().nextFloat() * 0.16f;
        List<DeferredHolder<SoundEvent, SoundEvent>> Layers = switch (Treat.getKind()) {
            case Drink -> Treat == Scp261Snack.StaticSoda ? List.of(FoodSounds.CanOpen, FoodSounds.CanOpenSnap, FoodSounds.CanOpenFizz)
                    : List.of(FoodSounds.BottleOpen, FoodSounds.BottleOpenPop, FoodSounds.BottleOpenFizz);
            default -> List.of(FoodSounds.BagOpen, FoodSounds.BagOpenPop);
        };
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(User, User.getX(), User.getEyeY(), User.getZ(), Layer.get(), SoundSource.PLAYERS, 0.7f, Pitch);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack Stack, Level World, LivingEntity Eater) {
        Scp261Snack Eaten = Treat;
        ItemStack Left = super.finishUsingItem(Stack, World, Eater);
        if (World instanceof ServerLevel Server && Eater instanceof ServerPlayer User) {
            Scp261Effects.consume(Server, User, Eaten);
        }
        return Left;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack Stack) {
        return Treat.getKind() == Scp261Snack.Kind.Drink ? UseAnim.DRINK : super.getUseAnimation(Stack);
    }

    @Override
    public SoundEvent getDrinkingSound() {
        return FoodSounds.Gulp.get();
    }

    @Override
    public SoundEvent getEatingSound() {
        return Treat.getKind() == Scp261Snack.Kind.Drink ? FoodSounds.Gulp.get() : super.getEatingSound();
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (ItemTooltips.showDetails(Lines)) {
            String Way = switch (Treat.getKind()) {
                case Eat -> "eat";
                case Drink -> "drink";
                case Chew -> "chew";
            };
            Lines.add(Component.translatable(SharedKey + Way).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(SharedKey + "origin").withStyle(ChatFormatting.DARK_AQUA));
        }
    }
}
