package net.rai.scp_unleashed.faction;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class FactionPlans {
    public static final String Folder = "faction_plan";
    private static final Logger Log = LogUtils.getLogger();
    private static final Map<ResourceLocation, FactionPlan> Cache = new HashMap<>();

    private FactionPlans() {
    }

    @Nullable
    public static FactionPlan get(MinecraftServer Server, ResourceLocation Id) {
        FactionPlan Found = Cache.get(Id);
        if (Found != null) {
            return Found;
        }
        ResourceLocation File = ResourceLocation.fromNamespaceAndPath(Id.getNamespace(), Folder + "/" + Id.getPath() + ".json");
        Optional<Resource> Source = Server.getResourceManager().getResource(File);
        if (Source.isEmpty()) {
            Log.warn("faction plan {} is missing", File);
            return null;
        }
        try (Reader Text = Source.get().openAsReader()) {
            JsonObject Root = JsonParser.parseReader(Text).getAsJsonObject();
            Found = FactionPlan.parse(Id.toString(), Root);
            Cache.put(Id, Found);
            return Found;
        } catch (Exception Problem) {
            Log.error("faction plan {} could not be read", File, Problem);
            return null;
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent Event) {
        Cache.clear();
    }
}
