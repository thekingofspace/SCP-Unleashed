package net.rai.scp_unleashed.scp343;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public final class Scp343Blessings {
    public static final long Cooldown = 24000L;
    public static final int Fewest = 2;
    public static final int Most = 3;

    public record Gift(Holder<MobEffect> Effect, int Ticks) {
        public MobEffectInstance make() {
            return new MobEffectInstance(Effect, Ticks, 0, false, true, true);
        }
    }

    public static final List<Gift> Pool = List.of(
            new Gift(MobEffects.REGENERATION, 1200),
            new Gift(MobEffects.DAMAGE_RESISTANCE, 2400),
            new Gift(MobEffects.DIG_SPEED, 3600),
            new Gift(MobEffects.MOVEMENT_SPEED, 2400),
            new Gift(MobEffects.DAMAGE_BOOST, 1800),
            new Gift(MobEffects.LUCK, 6000),
            new Gift(MobEffects.ABSORPTION, 2400),
            new Gift(MobEffects.SATURATION, 20),
            new Gift(MobEffects.NIGHT_VISION, 3600),
            new Gift(MobEffects.FIRE_RESISTANCE, 3600));

    private Scp343Blessings() {
    }

    public static long now(ServerPlayer User) {
        return User.server.overworld().getGameTime();
    }

    public static boolean ready(ServerPlayer User) {
        return now(User) >= Scp343Data.get(User.server).blessedUntil(User.getUUID());
    }

    public static List<Gift> roll(RandomSource Random) {
        List<Gift> Left = new ArrayList<>(Pool);
        int Count = Fewest + Random.nextInt(Most - Fewest + 1);
        List<Gift> Chosen = new ArrayList<>();
        for (int Index = 0; Index < Count && !Left.isEmpty(); Index++) {
            Chosen.add(Left.remove(Random.nextInt(Left.size())));
        }
        return Chosen;
    }

    public static List<Gift> grant(ServerPlayer User) {
        if (!ready(User)) {
            return List.of();
        }
        List<Gift> Chosen = roll(User.getRandom());
        for (Gift Each : Chosen) {
            User.addEffect(Each.make());
        }
        Scp343Data.get(User.server).bless(User.getUUID(), now(User) + Cooldown);
        return Chosen;
    }

    public static boolean isGift(Holder<MobEffect> Effect) {
        for (Gift Each : Pool) {
            if (Each.Effect().equals(Effect)) {
                return true;
            }
        }
        return false;
    }

    public static int giftsOn(Player User) {
        int Count = 0;
        for (Gift Each : Pool) {
            if (User.hasEffect(Each.Effect())) {
                Count++;
            }
        }
        return Count;
    }
}
