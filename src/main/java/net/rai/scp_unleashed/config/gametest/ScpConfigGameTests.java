package net.rai.scp_unleashed.config.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.ModConfigs;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.config.ScpConfigMigration;
import net.rai.scp_unleashed.config.ScpConfigs;
import net.rai.scp_unleashed.discord.DiscordConfig;
import net.rai.scp_unleashed.flamethrower.FlamerConfig;
import net.rai.scp_unleashed.flamethrower.FlamerConfig.FireRule;
import net.rai.scp_unleashed.gateway.gametest.GatewayGameTests;
import net.rai.scp_unleashed.orbital.OrbitalConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class ScpConfigGameTests {
    public static final String Arena = GatewayGameTests.Arena;

    private ScpConfigGameTests() {
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "config_files")
    public static void everyConfigLivesInThreeFiles(GameTestHelper Helper) {
        ModConfig Server = ModConfigs.getFileMap().get(ScpConfigs.ServerFile);
        ModConfig Common = ModConfigs.getFileMap().get(ScpConfigs.CommonFile);
        Helper.assertTrue(Server != null && Server.getType() == ModConfig.Type.SERVER && Server.getSpec() == ScpConfigs.Server, "the merged server file is not registered");
        Helper.assertTrue(Common != null && Common.getType() == ModConfig.Type.COMMON && Common.getSpec() == ScpConfigs.Common, "the merged common file is not registered");
        Helper.assertTrue(ScpConfigs.Server.isLoaded() && ScpConfigs.Common.isLoaded(), "the merged configs are not loaded");
        for (String Name : ModConfigs.getFileMap().keySet()) {
            if (Name.startsWith(Scp_unleashed.ModId + "-")) {
                Helper.assertTrue(Name.equals(ScpConfigs.ServerFile) || Name.equals(ScpConfigs.CommonFile) || Name.equals(ScpConfigs.ClientFile), "an old config file is still registered: " + Name);
            }
        }
        for (ScpConfigMigration.Legacy Each : ScpConfigMigration.ServerFiles) {
            Helper.assertTrue(ScpConfigs.Server.getValues().get(Each.Section()) != null, "the server file has no " + Each.Section() + " section");
        }
        Helper.assertTrue(ScpConfigs.Common.getValues().get("discord.webhooks.defaultWebhook") != null, "the webhook URLs are not in the common file");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "config_migrate")
    public static void oldFilesMoveIntoTheMergedServerFile(GameTestHelper Helper) {
        int Cooldown = OrbitalConfig.CooldownSeconds.get();
        boolean Crater = OrbitalConfig.Crater.get();
        int Queue = DiscordConfig.QueueLimit.get();
        FireRule Fire = FlamerConfig.WorldFire.get();
        Path Folder = null;
        try {
            Folder = Files.createTempDirectory("scp_unleashed_config");
            FireRule Other = Fire == FireRule.values()[0] ? FireRule.values()[FireRule.values().length - 1] : FireRule.values()[0];
            Files.writeString(Folder.resolve("scp_unleashed-orbital-server.toml"), "cooldownSeconds = 42\ncrater = " + !Crater + "\nunknownSetting = 9\nrange = 99999\n", StandardCharsets.UTF_8);
            Files.writeString(Folder.resolve("scp_unleashed-discord-server.toml"), "[general]\nqueueLimit = 77\n", StandardCharsets.UTF_8);
            Files.writeString(Folder.resolve("scp_unleashed-flamethrower-server.toml"), "worldFire = \"" + Other.name() + "\"\n", StandardCharsets.UTF_8);
            int Moved = ScpConfigMigration.migrate(Folder, ScpConfigs.Server, ScpConfigMigration.ServerFiles);
            Helper.assertTrue(Moved == 4, "expected 4 settings to move, moved " + Moved);
            Helper.assertTrue(OrbitalConfig.CooldownSeconds.get() == 42 && OrbitalConfig.Crater.get() == !Crater, "the orbital settings did not move");
            Helper.assertTrue(OrbitalConfig.Range.get() != 99999, "an out-of-range old value was copied");
            Helper.assertTrue(DiscordConfig.QueueLimit.get() == 77, "a nested Discord setting did not move");
            Helper.assertTrue(FlamerConfig.WorldFire.get() == Other, "an enum setting did not move");
            Helper.assertTrue(!Files.exists(Folder.resolve("scp_unleashed-orbital-server.toml")) && Files.exists(Folder.resolve("scp_unleashed-orbital-server.toml" + ScpConfigMigration.Suffix)), "the old file was not set aside");
            Helper.assertTrue(ScpConfigMigration.migrate(Folder, ScpConfigs.Server, ScpConfigMigration.ServerFiles) == 0, "a second start moved settings again");
        } catch (IOException Problem) {
            throw new GameTestAssertException("could not write the old config files: " + Problem);
        } finally {
            OrbitalConfig.CooldownSeconds.set(Cooldown);
            OrbitalConfig.Crater.set(Crater);
            DiscordConfig.QueueLimit.set(Queue);
            FlamerConfig.WorldFire.set(Fire);
            ScpConfigs.Server.save();
            if (Folder != null) {
                for (String Name : List.of("scp_unleashed-orbital-server.toml", "scp_unleashed-discord-server.toml", "scp_unleashed-flamethrower-server.toml")) {
                    try {
                        Files.deleteIfExists(Folder.resolve(Name + ScpConfigMigration.Suffix));
                        Files.deleteIfExists(Folder.resolve(Name));
                    } catch (IOException Ignored) {
                    }
                }
                try {
                    Files.deleteIfExists(Folder);
                } catch (IOException Ignored) {
                }
            }
        }
        Helper.succeed();
    }
}
