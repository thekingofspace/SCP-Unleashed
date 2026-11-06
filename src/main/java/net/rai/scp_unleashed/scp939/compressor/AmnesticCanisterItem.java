package net.rai.scp_unleashed.scp939.compressor;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.scp939.Scp939Breath;
import net.rai.scp_unleashed.scp939.Scp939Registry;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AmnesticCanisterItem extends Item {
    public static final int UseTicks = 24;
    public static final int CooldownTicks = 60;
    public static final float InhaleVolume = 0.8f;
    public static final String Prefix = "item.scp_unleashed.amnestic_canister.";
    public static final String MaskedKey = "message.scp_unleashed.amnestic_canister.masked";

    public AmnesticCanisterItem(Properties Settings) {
        super(Settings);
    }

    private static void play(Level World, @Nullable Player Listener, LivingEntity Source, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(Listener, Source.getX(), Source.getEyeY(), Source.getZ(), Layer.get(), SoundSource.PLAYERS, Volume, Pitch);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        if (Scp939Breath.masked(User)) {
            if (!World.isClientSide) {
                User.displayClientMessage(Component.translatable(MaskedKey).withStyle(ChatFormatting.GRAY), true);
                World.playSound(null, User.getX(), User.getEyeY(), User.getZ(), Scp939Registry.Muffled.get(), SoundSource.PLAYERS, 0.6f, 1.0f);
            }
            User.getCooldowns().addCooldown(this, CooldownTicks / 3);
            return InteractionResultHolder.fail(Stack);
        }
        User.startUsingItem(Hand);
        return InteractionResultHolder.consume(Stack);
    }

    @Override
    public void onUseTick(Level World, LivingEntity User, ItemStack Stack, int Remaining) {
        if (Remaining == UseTicks - 2) {
            play(World, User instanceof Player Holder ? Holder : null, User, Scp939Registry.InhaleLayers, InhaleVolume, 0.95f + User.getRandom().nextFloat() * 0.1f);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack Stack, Level World, LivingEntity User) {
        Player Taker = User instanceof Player Holder ? Holder : null;
        if (Taker instanceof ServerPlayer Server) {
            CriteriaTriggers.CONSUME_ITEM.trigger(Server, Stack);
        }
        if (!World.isClientSide) {
            Amnestics.inhale(User);
        }
        User.gameEvent(GameEvent.DRINK);
        if (Taker == null) {
            Stack.shrink(1);
            return Stack;
        }
        Taker.awardStat(Stats.ITEM_USED.get(this));
        Taker.getCooldowns().addCooldown(this, CooldownTicks);
        if (Taker.hasInfiniteMaterials()) {
            return Stack;
        }
        return ItemUtils.createFilledResult(Stack, Taker, new ItemStack(Scp939Registry.EmptyCanister.get()), false);
    }

    @Override
    public int getUseDuration(ItemStack Stack, LivingEntity User) {
        return UseTicks;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack Stack) {
        return UseAnim.TOOT_HORN;
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(Prefix + "tooltip").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable(Prefix + "use").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Prefix + "settle").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable(Prefix + "limits").withStyle(ChatFormatting.RED));
        }
    }
}
