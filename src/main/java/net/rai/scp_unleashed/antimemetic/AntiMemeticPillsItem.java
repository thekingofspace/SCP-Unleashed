package net.rai.scp_unleashed.antimemetic;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AntiMemeticPillsItem extends Item {
    public static final int Doses = 8;
    public static final int UseTicks = 24;
    public static final int EffectTicks = 6000;
    public static final int OverdoseTicks = 4800;
    public static final int NauseaTicks = 160;
    public static final int ShakeLead = 5;
    public static final int ShakeEvery = 4;
    public static final int BarSteps = 13;
    public static final float OpenVolume = 0.7f;
    public static final float ShakeVolume = 0.6f;
    public static final float SwallowVolume = 0.7f;
    public static final String Prefix = "item.scp_unleashed.anti_memetic_pills.";

    public AntiMemeticPillsItem(Properties Settings) {
        super(Settings);
    }

    public static int pills(ItemStack Stack) {
        return Mth.clamp(Stack.getOrDefault(AntiMemeticRegistry.PillsLeft.get(), Doses), 0, Doses);
    }

    public static void setPills(ItemStack Stack, int Count) {
        Stack.set(AntiMemeticRegistry.PillsLeft.get(), Mth.clamp(Count, 0, Doses));
    }

    public static void dose(LivingEntity Target) {
        MobEffectInstance Current = Target.getEffect(AntiMemeticRegistry.Shielded);
        if (Current != null && Current.getDuration() > OverdoseTicks) {
            Target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, NauseaTicks, 0, false, true, true));
        }
        Target.addEffect(new MobEffectInstance(AntiMemeticRegistry.Shielded, EffectTicks, 0, false, true, true));
    }

    private static void play(Level World, @Nullable Player Listener, LivingEntity Source, SoundEvent Sound, float Volume, float Pitch) {
        World.playSound(Listener, Source.getX(), Source.getEyeY(), Source.getZ(), Sound, Source.getSoundSource(), Volume, Pitch);
    }

    private static boolean shakes(int Remaining) {
        return UseTicks - Remaining > ShakeLead && Remaining % ShakeEvery == 0;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        if (pills(Stack) <= 0) {
            return InteractionResultHolder.fail(Stack);
        }
        User.startUsingItem(Hand);
        float Pitch = 0.94f + World.getRandom().nextFloat() * 0.12f;
        play(World, User, User, AntiMemeticRegistry.Cap.get(), OpenVolume, Pitch);
        play(World, User, User, AntiMemeticRegistry.Rattle.get(), OpenVolume, Pitch);
        return InteractionResultHolder.consume(Stack);
    }

    @Override
    public void onUseTick(Level World, LivingEntity User, ItemStack Stack, int Remaining) {
        if (shakes(Remaining)) {
            play(World, User instanceof Player Holder ? Holder : null, User, AntiMemeticRegistry.RattleTick.get(), ShakeVolume, 0.92f + User.getRandom().nextFloat() * 0.16f);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack Stack, Level World, LivingEntity User) {
        Player Taker = User instanceof Player Holder ? Holder : null;
        if (Taker instanceof ServerPlayer Server) {
            CriteriaTriggers.CONSUME_ITEM.trigger(Server, Stack);
        }
        if (!World.isClientSide) {
            dose(User);
        }
        float Pitch = 0.95f + User.getRandom().nextFloat() * 0.1f;
        play(World, Taker, User, AntiMemeticRegistry.Swallow.get(), SwallowVolume, Pitch);
        play(World, Taker, User, AntiMemeticRegistry.Cap.get(), OpenVolume * 0.7f, Pitch + 0.1f);
        User.gameEvent(GameEvent.DRINK);
        if (Taker != null) {
            Taker.awardStat(Stats.ITEM_USED.get(this));
            if (Taker.hasInfiniteMaterials()) {
                return Stack;
            }
        }
        int Left = pills(Stack) - 1;
        if (Left <= 0) {
            Stack.shrink(1);
            return Stack;
        }
        setPills(Stack, Left);
        return Stack;
    }

    @Override
    public int getUseDuration(ItemStack Stack, LivingEntity User) {
        return UseTicks;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack Stack) {
        return UseAnim.DRINK;
    }

    @Override
    public SoundEvent getDrinkingSound() {
        return AntiMemeticRegistry.Rattle.get();
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack OldStack, ItemStack NewStack, boolean SlotChanged) {
        return SlotChanged || !ItemStack.isSameItem(OldStack, NewStack);
    }

    @Override
    public boolean isBarVisible(ItemStack Stack) {
        return pills(Stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack Stack) {
        return Math.round(BarSteps * pills(Stack) / (float) Doses);
    }

    @Override
    public int getBarColor(ItemStack Stack) {
        return AntiMemeticEffect.Colour;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Prefix + "tooltip").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        Lines.add(Component.translatable(Prefix + "left", Component.literal(pills(Stack) + "/" + Doses).withStyle(ChatFormatting.LIGHT_PURPLE)).withStyle(ChatFormatting.GRAY));
        MobEffectInstance Effect = new MobEffectInstance(AntiMemeticRegistry.Shielded, EffectTicks);
        Lines.add(Component.translatable("potion.withDuration", Component.translatable(Effect.getDescriptionId()), MobEffectUtil.formatDuration(Effect, 1.0f, Context.tickRate()))
                .withStyle(Effect.getEffect().value().getCategory().getTooltipFormatting()));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Prefix + "use").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Prefix + "refresh", EffectTicks / 1200).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Prefix + "queasy", (EffectTicks - OverdoseTicks) / 1200).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Prefix + "limits").withStyle(ChatFormatting.RED));
        }
    }
}
