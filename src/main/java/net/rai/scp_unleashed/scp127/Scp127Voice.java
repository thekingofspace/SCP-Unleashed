package net.rai.scp_unleashed.scp127;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.scp148.Scp148Shield;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Scp127Voice {
    public static final String Prefix = "voice.scp_unleashed.scp127.";
    public static final int GlobalGap = 200;
    public static final int IdleChance = 2400;
    public static final int NameColour = 0xE85840;
    public static final int LineColour = 0xF2E6C8;
    private static final Map<UUID, Memory> Memories = new HashMap<>();

    private Scp127Voice() {
    }

    public enum Topic {
        Equip("equip", 12, 900, 1.0F),
        PutAway("put_away", 6, 1200, 0.35F),
        Kill("kill", 10, 400, 0.7F),
        LevelTwo("level_two", 3, 0, 1.0F),
        LevelThree("level_three", 3, 0, 1.0F),
        Empty("empty", 8, 300, 1.0F),
        Idle("idle", 8, 2400, 1.0F),
        Hungry("hungry", 8, 1200, 1.0F);

        private final String Key;
        private final int Lines;
        private final int Cooldown;
        private final float Chance;

        Topic(String Key, int Lines, int Cooldown, float Chance) {
            this.Key = Key;
            this.Lines = Lines;
            this.Cooldown = Cooldown;
            this.Chance = Chance;
        }

        public String key(int Line) {
            return Prefix + Key + "." + Line;
        }

        public int lines() {
            return Lines;
        }

        public int cooldown() {
            return Cooldown;
        }
    }

    private static final class Memory {
        private long LastSpoke = Long.MIN_VALUE / 2;
        private final long[] LastTopic = new long[Topic.values().length];
        private final int[] LastLine = new int[Topic.values().length];

        private Memory() {
            Arrays.fill(LastTopic, Long.MIN_VALUE / 2);
            Arrays.fill(LastLine, -1);
        }
    }

    public static Topic levelTopic(int Rank) {
        return Rank >= Scp127Levels.MaxLevel ? Topic.LevelThree : Topic.LevelTwo;
    }

    public static boolean say(ServerPlayer User, Topic Subject) {
        return say(User, Subject, false);
    }

    public static boolean say(ServerPlayer User, Topic Subject, boolean Forced) {
        if (!User.isAlive() || User.isSpectator()) {
            return false;
        }
        long Now = User.serverLevel().getGameTime();
        Memory Mind = Memories.computeIfAbsent(User.getUUID(), Id -> new Memory());
        int Slot = Subject.ordinal();
        if (!Forced) {
            if (Now - Mind.LastSpoke < GlobalGap || Now - Mind.LastTopic[Slot] < Subject.Cooldown) {
                return false;
            }
            if (Subject.Chance < 1.0F && User.getRandom().nextFloat() >= Subject.Chance) {
                return false;
            }
        }
        int Line = pick(User.getRandom(), Subject.Lines, Mind.LastLine[Slot]);
        Mind.LastSpoke = Now;
        Mind.LastTopic[Slot] = Now;
        Mind.LastLine[Slot] = Line;
        if (!Scp148Shield.blocks(User)) {
            User.displayClientMessage(line(Subject, Line), true);
        }
        return true;
    }

    public static void dry(ServerPlayer User) {
        ItemStack Held = User.getMainHandItem();
        if (Scp127Item.is(Held) && GunItem.getAmmo(Held) <= 0) {
            say(User, Topic.Empty);
        }
    }

    public static MutableComponent line(Topic Subject, int Line) {
        return Component.empty()
                .append(Component.translatable(Prefix + "speaker").withStyle(Style.EMPTY.withColor(NameColour).withBold(true)))
                .append(Component.literal(" "))
                .append(Component.translatable(Subject.key(Line)).withStyle(Style.EMPTY.withColor(LineColour).withItalic(true)));
    }

    private static int pick(RandomSource Random, int Count, int Last) {
        if (Count <= 1) {
            return 0;
        }
        if (Last < 0 || Last >= Count) {
            return Random.nextInt(Count);
        }
        int Line = Random.nextInt(Count - 1);
        return Line >= Last ? Line + 1 : Line;
    }

    public static void forget(UUID Id) {
        Memories.remove(Id);
    }

    public static void forgetAll() {
        Memories.clear();
    }
}
