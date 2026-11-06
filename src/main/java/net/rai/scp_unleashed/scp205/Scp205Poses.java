package net.rai.scp_unleashed.scp205;

import net.minecraft.util.RandomSource;

import java.util.List;

public final class Scp205Poses {
    public static final List<String> Day = List.of("stand", "wave", "sit", "reach", "walk", "crouch", "arms_up", "point");
    public static final List<String> Devil = List.of("devil_hunch", "devil_loom");
    public static final int DayCount = Day.size();
    public static final int DevilCount = Devil.size();

    private Scp205Poses() {
    }

    public static int another(RandomSource Random, int Current, int Count) {
        if (Count <= 1) {
            return 0;
        }
        int Next = Random.nextInt(Count - 1);
        return Next >= Current ? Next + 1 : Next;
    }

    public static String name(boolean DevilKind, int Index) {
        List<String> Names = DevilKind ? Devil : Day;
        return Names.get(Math.floorMod(Index, Names.size()));
    }
}
