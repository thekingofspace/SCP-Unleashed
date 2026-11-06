package net.rai.scp_unleashed.config;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.rai.scp_unleashed.anomaly.AnomalyDropConfig;
import net.rai.scp_unleashed.chunkloader.ChunkLoaderConfig;
import net.rai.scp_unleashed.discord.DiscordConfig;
import net.rai.scp_unleashed.discord.DiscordWebhooks;
import net.rai.scp_unleashed.flamethrower.FlamerConfig;
import net.rai.scp_unleashed.orbital.OrbitalConfig;
import net.rai.scp_unleashed.radio.VoiceConfig;
import net.rai.scp_unleashed.scp001scarlet.ScarletClientConfig;
import net.rai.scp_unleashed.scp001scarlet.ScarletConfig;
import net.rai.scp_unleashed.scp001scarlet.court.ScarletCourtConfig;
import net.rai.scp_unleashed.scp001scarlet.king.ScarletKingConfig;
import net.rai.scp_unleashed.scp2317.Scp2317Config;
import net.rai.scp_unleashed.scp5001.event.Scp5001EventConfig;
import net.rai.scp_unleashed.scp5001.mekhane.MekhaneConfig;
import net.rai.scp_unleashed.scp5001.xk.Scp5001XkConfig;
import net.rai.scp_unleashed.scp610.Scp610Config;
import net.rai.scp_unleashed.seasonal.SeasonConfig;
import net.rai.scp_unleashed.worldend.WorldEndConfig;

import java.util.function.Consumer;

public final class ScpConfigs {
    public static final String CommonFile = "scp_unleashed-common.toml";
    public static final String ClientFile = "scp_unleashed-client.toml";
    public static final String ServerFile = "scp_unleashed-server.toml";
    public static final String SectionPrefix = "config.scp_unleashed.section.";
    public static final ModConfigSpec Common;
    public static final ModConfigSpec Client;
    public static final ModConfigSpec Server;

    static {
        ModConfigSpec.Builder CommonBuilder = new ModConfigSpec.Builder();
        section(CommonBuilder, "discord", "Discord webhook URLs. This file is never sent to players.", DiscordWebhooks::define);
        Common = CommonBuilder.build();
        ModConfigSpec.Builder ClientBuilder = new ModConfigSpec.Builder();
        ScpClientConfig.define(ClientBuilder);
        section(ClientBuilder, "scarlet", "SCP-001 The Scarlet King, on this client only.", ScarletClientConfig::define);
        section(ClientBuilder, "scp001_day", "SCP-001 When Day Breaks, on this client only.", net.rai.scp_unleashed.scp001day.Scp001DayClientConfig::define);
        Client = ClientBuilder.build();
        ModConfigSpec.Builder ServerBuilder = new ModConfigSpec.Builder();
        section(ServerBuilder, "anomaly_drops", "What anomalies may be carried inside containers.", AnomalyDropConfig::define);
        section(ServerBuilder, "chunk_loader", "The chunk loader machine.", ChunkLoaderConfig::define);
        section(ServerBuilder, "discord", "Discord alerts. The webhook URLs themselves are in " + CommonFile + ".", DiscordConfig::define);
        section(ServerBuilder, "flamethrower", "The flamethrower.", FlamerConfig::define);
        section(ServerBuilder, "orbital", "The ZENITH Orbital Lance.", OrbitalConfig::define);
        section(ServerBuilder, "seasons", "Seasonal events.", SeasonConfig::define);
        section(ServerBuilder, "voice", "Radios, voice chat and anomaly voice recordings.", VoiceConfig::define);
        section(ServerBuilder, "world_endings", "Anomalies that can end the world.", WorldEndConfig::define);
        section(ServerBuilder, "scp610", "SCP-610 hives and growth.", Scp610Config::define);
        section(ServerBuilder, "scp2317", "SCP-2317 and its desert.", Scp2317Config::define);
        section(ServerBuilder, "scp5001", "The SCP-5001 site event.", Scp5001EventConfig::define);
        section(ServerBuilder, "scp5001_mekhane", "The emergency protocol and Mekhane.", MekhaneConfig::define);
        section(ServerBuilder, "scp5001_xk", "The SCP-5001 XK-1 scenario.", Scp5001XkConfig::define);
        section(ServerBuilder, "scarlet", "SCP-001 The Scarlet King: the meter, the cult and XK-2.", ScarletConfig::define);
        section(ServerBuilder, "scarlet_king", "The Scarlet King's rite and fight.", ScarletKingConfig::define);
        section(ServerBuilder, "scarlet_court", "The Scarlet King's daughters and minions.", ScarletCourtConfig::define);
        section(ServerBuilder, "scp1730_guardian", "SCP-1730-2 Malidramagiuan and its recovered form.", net.rai.scp_unleashed.scp1730.guardian.Scp1730GuardianConfig::define);
        Server = ServerBuilder.build();
    }

    private ScpConfigs() {
    }

    private static void section(ModConfigSpec.Builder Builder, String Name, String Comment, Consumer<ModConfigSpec.Builder> Body) {
        Builder.comment(Comment).translation(SectionPrefix + Name).push(Name);
        Body.accept(Builder);
        Builder.pop();
    }

    public static void register(IEventBus ModEventBus, ModContainer Container) {
        Container.registerConfig(ModConfig.Type.COMMON, Common, CommonFile);
        Container.registerConfig(ModConfig.Type.CLIENT, Client, ClientFile);
        Container.registerConfig(ModConfig.Type.SERVER, Server, ServerFile);
        ModEventBus.addListener(FMLCommonSetupEvent.class, Event -> ScpConfigMigration.migrate(FMLPaths.CONFIGDIR.get(), Common, ScpConfigMigration.CommonFiles));
        ModEventBus.addListener(FMLClientSetupEvent.class, Event -> ScpConfigMigration.migrate(FMLPaths.CONFIGDIR.get(), Client, ScpConfigMigration.ClientFiles));
        NeoForge.EVENT_BUS.addListener(ServerAboutToStartEvent.class, Event -> ScpConfigMigration.migrate(FMLPaths.CONFIGDIR.get(), Server, ScpConfigMigration.ServerFiles));
    }
}
