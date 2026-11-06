package net.rai.scp_unleashed.scp3199;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.jetbrains.annotations.Nullable;

public final class Scp3199Rules {
    public static final String MaxPopulationName = "scp3199MaxPopulation";
    public static final int DefaultMaxPopulation = 32;
    @Nullable
    public static GameRules.Key<GameRules.IntegerValue> MaxPopulation;

    private Scp3199Rules() {
    }

    public static void register(IEventBus ModEventBus) {
        ModEventBus.addListener(FMLCommonSetupEvent.class, Event -> Event.enqueueWork(Scp3199Rules::registerRules));
    }

    private static void registerRules() {
        if (MaxPopulation == null) {
            MaxPopulation = GameRules.register(MaxPopulationName, GameRules.Category.SPAWNING, GameRules.IntegerValue.create(DefaultMaxPopulation));
        }
    }

    public static int maxPopulation(MinecraftServer Server) {
        if (MaxPopulation == null) {
            return DefaultMaxPopulation;
        }
        return Server.getGameRules().getInt(MaxPopulation);
    }

    public static void setMaxPopulation(MinecraftServer Server, int Value) {
        if (MaxPopulation != null) {
            Server.getGameRules().getRule(MaxPopulation).set(Value, Server);
        }
    }

    public static boolean breedingAllowed(ServerLevel World) {
        int Cap = maxPopulation(World.getServer());
        return Cap > 0 && Scp3199Population.level(World) < Cap;
    }

    public static boolean hatchAllowed(ServerLevel World) {
        int Cap = maxPopulation(World.getServer());
        return Cap > 0 && Scp3199Population.level(World) <= Cap;
    }
}
