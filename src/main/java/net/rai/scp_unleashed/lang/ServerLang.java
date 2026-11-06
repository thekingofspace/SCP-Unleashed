package net.rai.scp_unleashed.lang;

import com.mojang.logging.LogUtils;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class ServerLang {
    private static final Logger Log = LogUtils.getLogger();

    private ServerLang() {
    }

    @SubscribeEvent
    public static void serverStarting(ServerStartingEvent Event) {
        if (FMLEnvironment.dist.isDedicatedServer()) {
            load(Event.getServer());
        }
    }

    public static void load(MinecraftServer Server) {
        try {
            ResourceManager Clients = new MultiPackResourceManager(PackType.CLIENT_RESOURCES, Server.getServerResources().resourceManager().listPacks().toList());
            Map<String, String> Table = new HashMap<>();
            Map<String, Component> Components = new HashMap<>();
            LangLoading.Session Running = LangLoading.begin(Clients, List.of(Language.DEFAULT));
            for (String Namespace : Clients.getNamespaces()) {
                ResourceLocation MainFile = ResourceLocation.fromNamespaceAndPath(Namespace, LangLoading.mainPath(Language.DEFAULT));
                LangLoading.appendNamespace(Running, Clients, Namespace, Language.DEFAULT, Clients.getResourceStack(MainFile), Table, Components, (Source, Output) -> {
                    try (InputStream In = Source.open()) {
                        Language.loadFromJson(In, Output, Components::put);
                    }
                });
            }
            LangLoading.finish(Running);
            install(Table, Components);
        } catch (RuntimeException Problem) {
            Log.error("Could not load the language folders on the server: {}", LangMerge.describe(Problem));
        }
    }

    private static void install(Map<String, String> Table, Map<String, Component> Components) {
        Language Current = Language.getInstance();
        if (Current instanceof Layered Previous) {
            Current = Previous.Base;
        }
        try {
            Current.getLanguageData().putAll(Table);
        } catch (UnsupportedOperationException Immutable) {
            Language.inject(new Layered(Current, Table, Components));
        }
    }

    public static final class Layered extends Language {
        private final Language Base;
        private final Map<String, String> Extra;
        private final Map<String, Component> ExtraComponents;

        public Layered(Language Base, Map<String, String> Extra, Map<String, Component> ExtraComponents) {
            this.Base = Base;
            this.Extra = Map.copyOf(Extra);
            this.ExtraComponents = Map.copyOf(ExtraComponents);
        }

        @Override
        public String getOrDefault(String Key, String Fallback) {
            String Found = Extra.get(Key);
            return Found != null ? Found : Base.getOrDefault(Key, Fallback);
        }

        @Override
        public boolean has(String Key) {
            return Extra.containsKey(Key) || Base.has(Key);
        }

        @Override
        public boolean isDefaultRightToLeft() {
            return Base.isDefaultRightToLeft();
        }

        @Override
        public FormattedCharSequence getVisualOrder(FormattedText Text) {
            return Base.getVisualOrder(Text);
        }

        @Override
        public Map<String, String> getLanguageData() {
            Map<String, String> Merged = new HashMap<>(Base.getLanguageData());
            Merged.putAll(Extra);
            return Merged;
        }

        @Override
        @Nullable
        public Component getComponent(String Key) {
            if (Extra.containsKey(Key)) {
                return ExtraComponents.get(Key);
            }
            return Base.getComponent(Key);
        }
    }
}
