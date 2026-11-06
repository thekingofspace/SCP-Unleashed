package net.rai.scp_unleashed.scp075;

import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.archive.investigation.NightfallCondition;

public final class Scp075Night {
    private Scp075Night() {
    }

    public static boolean isDark(Level World) {
        long TimeOfDay = World.getDayTime() % NightfallCondition.DayLength;
        return TimeOfDay >= NightfallCondition.DuskTime && TimeOfDay <= NightfallCondition.DawnTime;
    }

    public static boolean countsAsNight(Level World) {
        return !World.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT) || isDark(World);
    }
}
