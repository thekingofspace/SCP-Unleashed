package net.rai.scp_unleashed.intro;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;
import org.jetbrains.annotations.Nullable;

public final class IntroSettings {
    public static final String RuleName = "scpIntroCutscene";
    public static final int VeteranTicks = 6000;

    public static GameRules.Key<GameRules.BooleanValue> Enabled;

    private IntroSettings() {
    }

    public static void registerRules() {
        if (Enabled == null) {
            Enabled = GameRules.register(RuleName, GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
        }
    }

    public static boolean enabled(@Nullable MinecraftServer Server) {
        if (Server == null || Enabled == null) {
            return true;
        }
        GameRules.BooleanValue Value = Server.getGameRules().getRule(Enabled);
        return Value == null || Value.get();
    }

    public static void setEnabled(MinecraftServer Server, boolean On) {
        if (Enabled != null) {
            Server.getGameRules().getRule(Enabled).set(On, Server);
        }
    }
}
