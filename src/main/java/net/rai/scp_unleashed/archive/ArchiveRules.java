package net.rai.scp_unleashed.archive;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.jetbrains.annotations.Nullable;

public final class ArchiveRules {
    public static final String RepeatName = "scpArchiveRepeatFiles";
    public static final String PerPlayerName = "scpArchivePerPlayer";
    public static final String SpawningName = "scpDossierSpawning";

    public static GameRules.Key<GameRules.BooleanValue> RepeatFiles;
    public static GameRules.Key<GameRules.BooleanValue> PerPlayer;
    public static GameRules.Key<GameRules.BooleanValue> Spawning;

    public enum Mode {
        World,
        PerPlayer,
        Unlimited
    }

    private ArchiveRules() {
    }

    public static void register(IEventBus ModEventBus) {
        ModEventBus.addListener(FMLCommonSetupEvent.class, Event -> Event.enqueueWork(ArchiveRules::registerRules));
    }

    private static void registerRules() {
        if (RepeatFiles == null) {
            RepeatFiles = GameRules.register(RepeatName, GameRules.Category.SPAWNING, GameRules.BooleanValue.create(false));
        }
        if (PerPlayer == null) {
            PerPlayer = GameRules.register(PerPlayerName, GameRules.Category.SPAWNING, GameRules.BooleanValue.create(false));
        }
        if (Spawning == null) {
            Spawning = GameRules.register(SpawningName, GameRules.Category.SPAWNING, GameRules.BooleanValue.create(true));
        }
    }

    public static Mode mode(boolean Repeat, boolean Personal) {
        return Repeat ? Mode.Unlimited : Personal ? Mode.PerPlayer : Mode.World;
    }

    public static Mode mode(@Nullable MinecraftServer Server) {
        if (Server == null) {
            return Mode.World;
        }
        return mode(rule(Server.getGameRules(), RepeatFiles), rule(Server.getGameRules(), PerPlayer));
    }

    public static boolean repeatable(@Nullable MinecraftServer Server) {
        return mode(Server) != Mode.World;
    }

    public static boolean perPlayer(@Nullable MinecraftServer Server) {
        return mode(Server) == Mode.PerPlayer;
    }

    public static boolean spawning(@Nullable MinecraftServer Server) {
        if (Server == null || Spawning == null) {
            return true;
        }
        GameRules.BooleanValue Value = Server.getGameRules().getRule(Spawning);
        return Value == null || Value.get();
    }

    public static boolean unlimited(@Nullable MinecraftServer Server) {
        return mode(Server) == Mode.Unlimited;
    }

    public static boolean uniqueItemsReusable(@Nullable MinecraftServer Server) {
        return !unlimited(Server);
    }

    private static boolean rule(GameRules Rules, @Nullable GameRules.Key<GameRules.BooleanValue> Key) {
        if (Key == null) {
            return false;
        }
        GameRules.BooleanValue Value = Rules.getRule(Key);
        return Value != null && Value.get();
    }
}
