package net.rai.scp_unleashed.scp129;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Scp129SuppressantItem extends Item {
    public static final int UseTicks = 16;
    public static final int Cooldown = 20;
    public static final float ClickVolume = 0.7f;
    public static final float HissVolume = 0.8f;
    public static final int MistCount = 6;
    public static final String Prefix = "item.scp_unleashed.scp129_suppressant.";

    public Scp129SuppressantItem(Properties Settings) {
        super(Settings);
    }

    public static boolean isSuppressant(ItemStack Stack) {
        return Stack.getItem() instanceof Scp129SuppressantItem;
    }

    public static boolean canTreat(LivingEntity Target) {
        return Target.isAlive() && (Scp129Infection.canCatch(Target) || Scp129Infection.isInfected(Target));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Held = User.getItemInHand(Hand);
        User.startUsingItem(Hand);
        if (!World.isClientSide) {
            World.playSound(null, User.getX(), User.getY(0.6), User.getZ(), Scp129Registry.InjectClick.get(), User.getSoundSource(), ClickVolume, 1.0f);
        }
        return InteractionResultHolder.consume(Held);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack Stack, Level World, LivingEntity User) {
        if (World instanceof ServerLevel Server) {
            administer(Server, User instanceof Player Giver ? Giver : null, User);
            if (User instanceof ServerPlayer Taker) {
                CriteriaTriggers.CONSUME_ITEM.trigger(Taker, Stack);
            }
        }
        if (User instanceof Player Taker) {
            Taker.awardStat(Stats.ITEM_USED.get(this));
            Taker.getCooldowns().addCooldown(this, Cooldown);
            if (Taker.hasInfiniteMaterials()) {
                return Stack;
            }
        }
        Stack.shrink(1);
        return Stack;
    }

    public static InteractionResult inject(Player User, InteractionHand Hand, LivingEntity Target) {
        ItemStack Held = User.getItemInHand(Hand);
        if (!isSuppressant(Held) || !canTreat(Target) || User.getCooldowns().isOnCooldown(Held.getItem())) {
            return InteractionResult.PASS;
        }
        if (User.level() instanceof ServerLevel Server) {
            administer(Server, User, Target);
            User.awardStat(Stats.ITEM_USED.get(Held.getItem()));
            User.getCooldowns().addCooldown(Held.getItem(), Cooldown);
            Held.consume(1, User);
        }
        return InteractionResult.sidedSuccess(User.level().isClientSide);
    }

    public static void administer(ServerLevel World, @Nullable Player Giver, LivingEntity Target) {
        Scp129Infection.treat(Target);
        float Pitch = 0.95f + World.getRandom().nextFloat() * 0.1f;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp129Registry.InjectLayers) {
            World.playSound(null, Target.getX(), Target.getY(0.6), Target.getZ(), Layer.get(), Target.getSoundSource(), Layer == Scp129Registry.Inject ? HissVolume : ClickVolume, Pitch);
        }
        World.sendParticles(ParticleTypes.CLOUD, Target.getX(), Target.getY(0.55), Target.getZ(), MistCount, Target.getBbWidth() * 0.3, Target.getBbHeight() * 0.15,
                Target.getBbWidth() * 0.3, 0.01);
        Target.gameEvent(GameEvent.ITEM_INTERACT_FINISH);
        if (Target instanceof Player Patient) {
            Patient.displayClientMessage(Component.translatable("message.scp_unleashed.scp129.treated").withStyle(ChatFormatting.AQUA), true);
        }
        if (Giver != null && Giver != Target) {
            Giver.displayClientMessage(Component.translatable("message.scp_unleashed.scp129.treated_other", Target.getDisplayName()).withStyle(ChatFormatting.AQUA), true);
        }
    }

    @Override
    public int getUseDuration(ItemStack Stack, LivingEntity User) {
        return UseTicks;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack Stack) {
        return UseAnim.NONE;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Prefix + "tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Prefix + "self").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Prefix + "other").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Prefix + "clears").withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable(Prefix + "single").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
