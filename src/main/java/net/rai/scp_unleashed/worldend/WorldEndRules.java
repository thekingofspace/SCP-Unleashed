package net.rai.scp_unleashed.worldend;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;

public final class WorldEndRules {
    public static final String EndingsName = "scpWorldEndings";
    public static GameRules.Key<GameRules.BooleanValue> Endings;

    private WorldEndRules() {
    }

    public static void register() {
        if (Endings == null) {
            Endings = GameRules.register(EndingsName, GameRules.Category.MISC, GameRules.BooleanValue.create(true));
        }
    }

    public static boolean endings(MinecraftServer Server) {
        if (Endings == null) {
            return true;
        }
        GameRules.BooleanValue Rule = Server.getGameRules().getRule(Endings);
        return Rule == null || Rule.get();
    }

    public static void setEndings(MinecraftServer Server, boolean Value) {
        if (Endings != null) {
            Server.getGameRules().getRule(Endings).set(Value, Server);
        }
    }
}
