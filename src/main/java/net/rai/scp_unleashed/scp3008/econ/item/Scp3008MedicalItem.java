package net.rai.scp_unleashed.scp3008.econ.item;

import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.scp3008.econ.Scp3008EconRegistry;

public class Scp3008MedicalItem extends Scp3008StoreItem {
    public enum Kind {
        Bandage(30, 3.0f, 100, 0, 20, UseAnim.BRUSH),
        Kit(50, 6.0f, 300, 0, 100, UseAnim.BOW);

        public final int UseTicks;
        public final float Heal;
        public final int RegenTicks;
        public final int RegenLevel;
        public final int Cooldown;
        public final UseAnim Animation;

        Kind(int UseTicks, float Heal, int RegenTicks, int RegenLevel, int Cooldown, UseAnim Animation) {
            this.UseTicks = UseTicks;
            this.Heal = Heal;
            this.RegenTicks = RegenTicks;
            this.RegenLevel = RegenLevel;
            this.Cooldown = Cooldown;
            this.Animation = Animation;
        }
    }

    private final Kind Type;

    public Scp3008MedicalItem(Properties Settings, Kind Type) {
        super(Settings);
        this.Type = Type;
    }

    public Kind kind() {
        return Type;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level World, Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        if (User.getHealth() >= User.getMaxHealth()) {
            return InteractionResultHolder.fail(Stack);
        }
        User.startUsingItem(Hand);
        if (!World.isClientSide) {
            World.playSound(null, User.getX(), User.getY(), User.getZ(), Type == Kind.Kit ? Scp3008EconRegistry.KitOpen.get() : Scp3008EconRegistry.BandageWrap.get(),
                    SoundSource.PLAYERS, 0.8f, 0.95f + World.getRandom().nextFloat() * 0.1f);
        }
        return InteractionResultHolder.consume(Stack);
    }

    @Override
    public int getUseDuration(ItemStack Stack, LivingEntity User) {
        return Type.UseTicks;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack Stack) {
        return Type.Animation;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack Stack, Level World, LivingEntity User) {
        if (!World.isClientSide) {
            treat(User);
            World.playSound(null, User.getX(), User.getY(), User.getZ(), Scp3008EconRegistry.BandageTie.get(), SoundSource.PLAYERS, 0.8f,
                    0.95f + World.getRandom().nextFloat() * 0.1f);
            if (User instanceof Player Patient) {
                Patient.getCooldowns().addCooldown(this, Type.Cooldown);
                Patient.awardStat(Stats.ITEM_USED.get(this));
            }
        }
        Stack.consume(1, User);
        return Stack;
    }

    public void treat(LivingEntity User) {
        User.heal(Type.Heal);
        User.addEffect(new MobEffectInstance(MobEffects.REGENERATION, Type.RegenTicks, Type.RegenLevel));
    }
}
