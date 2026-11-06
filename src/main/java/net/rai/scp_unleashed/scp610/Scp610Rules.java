package net.rai.scp_unleashed.scp610;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;

public final class Scp610Rules {
    public static final String SpreadName = "scp610Spread";
    public static final String UnlimitedName = "scp610UnlimitedGrowth";
    public static GameRules.Key<GameRules.BooleanValue> Spread;
    public static GameRules.Key<GameRules.BooleanValue> Unlimited;

    private Scp610Rules() {
    }

    public static void register() {
        if (Spread == null) {
            Spread = GameRules.register(SpreadName, GameRules.Category.UPDATES, GameRules.BooleanValue.create(true));
        }
        if (Unlimited == null) {
            Unlimited = GameRules.register(UnlimitedName, GameRules.Category.UPDATES, GameRules.BooleanValue.create(false));
        }
    }

    public static boolean spreads(MinecraftServer Server) {
        if (Spread == null) {
            return true;
        }
        GameRules.BooleanValue Rule = Server.getGameRules().getRule(Spread);
        return Rule == null || Rule.get();
    }

    public static void setSpread(MinecraftServer Server, boolean Value) {
        if (Spread != null) {
            Server.getGameRules().getRule(Spread).set(Value, Server);
        }
    }

    public static boolean unlimited(MinecraftServer Server) {
        if (Unlimited == null) {
            return false;
        }
        GameRules.BooleanValue Rule = Server.getGameRules().getRule(Unlimited);
        return Rule != null && Rule.get();
    }

    public static void setUnlimited(MinecraftServer Server, boolean Value) {
        if (Unlimited != null) {
            Server.getGameRules().getRule(Unlimited).set(Value, Server);
        }
        Scp610Limits.refresh(Server);
    }
}
