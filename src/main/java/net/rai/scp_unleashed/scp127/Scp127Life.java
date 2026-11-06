package net.rai.scp_unleashed.scp127;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class Scp127Life {
    public static final long HungerTicks = 12000L;
    public static final int WhineChance = 1600;
    public static final int LevelColour = 0xE85840;

    private Scp127Life() {
    }

    public static boolean isHungry(ItemStack Stack, long Now) {
        Long Used = Stack.get(Scp127Registry.Used.get());
        return Used != null && Now - Used >= HungerTicks;
    }

    public static void tick(ServerPlayer User, ItemStack Stack, long Now) {
        Long Used = Stack.get(Scp127Registry.Used.get());
        if (Used == null || Used > Now) {
            Stack.set(Scp127Registry.Used.get(), Now);
            return;
        }
        RandomSource Random = User.getRandom();
        boolean Hungry = Now - Used >= HungerTicks;
        boolean Wielded = User.getMainHandItem() == Stack;
        if (Hungry && Random.nextInt(WhineChance) == 0) {
            Scp127Sounds.play(User.serverLevel(), near(User), Scp127Sounds.Whine);
            Scp127Voice.say(User, Scp127Voice.Topic.Hungry);
            return;
        }
        if (Wielded && !Hungry && Random.nextInt(Scp127Voice.IdleChance) == 0) {
            Scp127Voice.say(User, Scp127Voice.Topic.Idle);
        }
    }

    public static void used(ItemStack Stack, long Now) {
        Stack.set(Scp127Registry.Used.get(), Now);
    }

    public static void fed(ServerPlayer User, ItemStack Stack) {
        ServerLevel World = User.serverLevel();
        long Now = World.getGameTime();
        int Rank = Scp127Levels.rankOf(Stack);
        int Kills = Scp127Levels.killsOf(Stack) + 1;
        Stack.set(Scp127Registry.Kills.get(), Kills);
        Stack.set(Scp127Registry.Fed.get(), Now);
        int Earned = Math.max(Rank, Scp127Levels.rankForKills(Kills));
        if (Earned > Rank) {
            Stack.set(Scp127Registry.Rank.get(), Earned);
            levelUp(User, Earned);
            return;
        }
        Scp127Voice.say(User, Scp127Voice.Topic.Kill);
    }

    public static void levelUp(ServerPlayer User, int Rank) {
        ServerLevel World = User.serverLevel();
        Vec3 Hand = near(User);
        Scp127Sounds.play(World, Hand, Scp127Sounds.LevelUp);
        World.sendParticles(ParticleTypes.WHITE_SMOKE, Hand.x, Hand.y, Hand.z, 10, 0.18, 0.12, 0.18, 0.01);
        User.displayClientMessage(Component.translatable("message.scp_unleashed.scp127_level_up", Scp127Levels.numeral(Rank))
                .withStyle(Style.EMPTY.withColor(LevelColour)), false);
        Scp127Voice.say(User, Scp127Voice.levelTopic(Rank), true);
    }

    public static Vec3 near(ServerPlayer User) {
        Vec3 Look = User.getLookAngle();
        return User.getEyePosition().add(Look.x * 0.4, -0.35, Look.z * 0.4);
    }
}
