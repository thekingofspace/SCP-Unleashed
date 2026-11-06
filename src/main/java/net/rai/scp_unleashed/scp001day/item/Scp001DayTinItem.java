package net.rai.scp_unleashed.scp001day.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.rai.scp_unleashed.scp001day.Scp001DayRegistry;

public class Scp001DayTinItem extends Item {
    public static final int UseTicks = 40;

    public enum Outcome {
        Plain(55, 6, 0.6f),
        Hearty(12, 9, 0.9f),
        Sour(8, 6, 0.3f),
        Warm(7, 6, 0.6f),
        Empty(6, 0, 0.0f),
        Strange(5, 6, 0.6f),
        Gritty(4, 6, 0.4f),
        Lucky(3, 6, 0.6f);

        public final int Weight;
        public final int Nutrition;
        public final float Saturation;

        Outcome(int Weight, int Nutrition, float Saturation) {
            this.Weight = Weight;
            this.Nutrition = Nutrition;
            this.Saturation = Saturation;
        }

        public String key() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public Scp001DayTinItem(Properties Properties) {
        super(Properties);
    }

    public static Outcome roll(RandomSource Random) {
        int Total = 0;
        for (Outcome Each : Outcome.values()) {
            Total += Each.Weight;
        }
        int Pick = Random.nextInt(Total);
        for (Outcome Each : Outcome.values()) {
            Pick -= Each.Weight;
            if (Pick < 0) {
                return Each;
            }
        }
        return Outcome.Plain;
    }

    @Override
    public int getUseDuration(ItemStack Stack, LivingEntity Eater) {
        return UseTicks;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack Stack, Level World, LivingEntity Eater) {
        if (World instanceof ServerLevel Server && Eater instanceof ServerPlayer User) {
            Outcome Result = roll(Server.random);
            apply(Server, User, Result);
            User.awardStat(Stats.ITEM_USED.get(this));
        }
        Eater.gameEvent(GameEvent.EAT);
        if (!(Eater instanceof Player User) || !User.getAbilities().instabuild) {
            Stack.shrink(1);
        }
        return Stack;
    }

    public static void apply(ServerLevel World, ServerPlayer User, Outcome Result) {
        World.playSound(null, User.getX(), User.getY() + 1.0, User.getZ(), Result == Outcome.Empty ? Scp001DayRegistry.TinEmpty.get() : Scp001DayRegistry.TinOpen.get(), SoundSource.PLAYERS, 0.8f,
                0.9f + 0.2f * World.random.nextFloat());
        if (Result.Nutrition > 0) {
            User.getFoodData().eat(Result.Nutrition, Result.Saturation);
        }
        switch (Result) {
            case Hearty -> User.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
            case Sour -> {
                User.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0));
                User.addEffect(new MobEffectInstance(MobEffects.HUNGER, 300, 0));
            }
            case Warm -> User.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 0));
            case Strange -> User.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0));
            case Gritty -> User.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 0));
            case Lucky -> {
                ItemStack Spare = new ItemStack(Scp001DayRegistry.Tin.get());
                if (!User.getInventory().add(Spare)) {
                    User.drop(Spare, false);
                }
            }
            default -> {
            }
        }
        User.displayClientMessage(Component.translatable("item.scp_unleashed.scp001_day_tin.outcome." + Result.key()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
    }

    @Override
    public void appendHoverText(ItemStack Stack, TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("item.scp_unleashed.scp001_day_tin.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
