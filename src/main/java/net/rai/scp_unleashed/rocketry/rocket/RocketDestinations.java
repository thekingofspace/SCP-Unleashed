package net.rai.scp_unleashed.rocketry.rocket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.Scp_unleashed;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class RocketDestinations {
    public static final String Directory = "rocketry/destination";
    public static final ResourceLocation EarthId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "earth");
    public static final RocketDestination BuiltInEarth = new RocketDestination(EarthId, Level.OVERWORLD, Component.translatable("destination.scp_unleashed.earth"),
            ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/rocketry/destination/earth.png"), 8000, 200000, 180, 1.0f, 0);
    private static final Logger Log = LogUtils.getLogger();
    private static final Gson Parser = new GsonBuilder().create();
    private static final Comparator<RocketDestination> Ordering = Comparator.comparingInt(RocketDestination::Order).thenComparing(Each -> Each.Id().toString());

    public record Entry(RocketDestination Destination, boolean Available) {
    }

    private static volatile List<RocketDestination> Loaded = List.of(BuiltInEarth);
    private static volatile List<Entry> Client = List.of();

    private RocketDestinations() {
    }

    public static List<RocketDestination> all() {
        return Loaded;
    }

    public static void set(List<RocketDestination> Destinations) {
        List<RocketDestination> Sorted = new ArrayList<>(Destinations);
        if (Sorted.stream().noneMatch(RocketDestination::isEarth)) {
            Sorted.add(BuiltInEarth);
        }
        Sorted.sort(Ordering);
        Loaded = List.copyOf(Sorted);
    }

    @Nullable
    public static RocketDestination byId(@Nullable ResourceLocation Id) {
        if (Id == null) {
            return null;
        }
        for (RocketDestination Each : Loaded) {
            if (Each.Id().equals(Id)) {
                return Each;
            }
        }
        return null;
    }

    public static int indexOf(@Nullable RocketDestination Wanted) {
        return Wanted == null ? -1 : Loaded.indexOf(Wanted);
    }

    public static RocketDestination earth() {
        for (RocketDestination Each : Loaded) {
            if (Each.isEarth()) {
                return Each;
            }
        }
        return BuiltInEarth;
    }

    @Nullable
    public static RocketDestination forDimension(ResourceKey<Level> Dimension) {
        for (RocketDestination Each : Loaded) {
            if (Each.Dimension().equals(Dimension)) {
                return Each;
            }
        }
        return null;
    }

    public static float gravity(Level World) {
        RocketDestination Found = forDimension(World.dimension());
        return Found == null ? 1.0f : Found.Gravity();
    }

    public static boolean isLaunchSite(ResourceKey<Level> Dimension) {
        return Level.OVERWORLD.equals(Dimension) || forDimension(Dimension) != null;
    }

    public static boolean available(MinecraftServer Server, RocketDestination Destination) {
        return Server.getLevel(Destination.Dimension()) != null;
    }

    public static List<RocketDestination> choicesFrom(ResourceKey<Level> From) {
        List<RocketDestination> Choices = new ArrayList<>();
        for (RocketDestination Each : Loaded) {
            if (!Each.Dimension().equals(From)) {
                Choices.add(Each);
            }
        }
        return Choices;
    }

    @Nullable
    public static RocketDestination defaultFrom(MinecraftServer Server, ResourceKey<Level> From) {
        List<RocketDestination> Choices = choicesFrom(From);
        for (RocketDestination Each : Choices) {
            if (available(Server, Each)) {
                return Each;
            }
        }
        return Choices.isEmpty() ? null : Choices.getFirst();
    }

    @Nullable
    public static RocketDestination portableRoute(MinecraftServer Server, ResourceKey<Level> From) {
        if (!Level.OVERWORLD.equals(From)) {
            return earth();
        }
        return defaultFrom(Server, From);
    }

    @Nullable
    public static RocketDestination step(MinecraftServer Server, ResourceKey<Level> From, @Nullable RocketDestination Current, int Step) {
        List<RocketDestination> Choices = choicesFrom(From);
        if (Choices.isEmpty()) {
            return null;
        }
        int Index = Current == null ? -1 : Choices.indexOf(Current);
        if (Index < 0) {
            return defaultFrom(Server, From);
        }
        return Choices.get(Math.floorMod(Index + Step, Choices.size()));
    }

    public static List<Entry> snapshot(MinecraftServer Server) {
        List<Entry> Entries = new ArrayList<>();
        for (RocketDestination Each : Loaded) {
            Entries.add(new Entry(Each, available(Server, Each)));
        }
        return Entries;
    }

    public static List<Entry> client() {
        return Client;
    }

    public static void setClient(List<Entry> Entries) {
        Client = List.copyOf(Entries);
    }

    @Nullable
    public static Entry clientAt(int Index) {
        List<Entry> Entries = Client;
        return Index >= 0 && Index < Entries.size() ? Entries.get(Index) : null;
    }

    public static class Loader extends SimpleJsonResourceReloadListener {
        private final HolderLookup.Provider Registries;

        public Loader(HolderLookup.Provider Registries) {
            super(Parser, Directory);
            this.Registries = Registries;
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> Files, ResourceManager Resources, ProfilerFiller Profiler) {
            RegistryOps<JsonElement> Ops = RegistryOps.create(JsonOps.INSTANCE, Registries);
            List<RocketDestination> Found = new ArrayList<>();
            for (Map.Entry<ResourceLocation, JsonElement> File : Files.entrySet()) {
                RocketDestination.SpecCodec.parse(Ops, File.getValue())
                        .resultOrPartial(Problem -> Log.warn("Rocket destination {} is invalid: {}", File.getKey(), Problem))
                        .ifPresent(Spec -> Found.add(Spec.build(File.getKey())));
            }
            set(Found);
            Log.info("Loaded {} rocket destinations", Loaded.size());
        }
    }
}
