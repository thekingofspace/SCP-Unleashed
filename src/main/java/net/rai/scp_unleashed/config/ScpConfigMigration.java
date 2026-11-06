package net.rai.scp_unleashed.config;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.FileNotFoundAction;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.mojang.logging.LogUtils;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public final class ScpConfigMigration {
    public static final String Suffix = ".migrated";
    public static final List<Legacy> CommonFiles = List.of(new Legacy("scp_unleashed-discord-webhooks.toml", "discord"));
    public static final List<Legacy> ClientFiles = List.of(new Legacy("scp_unleashed-scarlet-client.toml", "scarlet"));
    public static final List<Legacy> ServerFiles = List.of(
            new Legacy("scp_unleashed-anomaly_drops-server.toml", "anomaly_drops"),
            new Legacy("scp_unleashed-chunk_loader-server.toml", "chunk_loader"),
            new Legacy("scp_unleashed-discord-server.toml", "discord"),
            new Legacy("scp_unleashed-flamethrower-server.toml", "flamethrower"),
            new Legacy("scp_unleashed-orbital-server.toml", "orbital"),
            new Legacy("scp_unleashed-seasons-server.toml", "seasons"),
            new Legacy("scp_unleashed-voice-server.toml", "voice"),
            new Legacy("scp_unleashed-worldend-server.toml", "world_endings"),
            new Legacy("scp_unleashed-scp610-server.toml", "scp610"),
            new Legacy("scp_unleashed-scarlet-2317-server.toml", "scp2317"),
            new Legacy("scp_unleashed-scp5001-server.toml", "scp5001"),
            new Legacy("scp_unleashed-scp5001-mekhane-server.toml", "scp5001_mekhane"),
            new Legacy("scp_unleashed-scp5001-xk-server.toml", "scp5001_xk"),
            new Legacy("scp_unleashed-scarlet-server.toml", "scarlet"),
            new Legacy("scp_unleashed-scarlet-king-server.toml", "scarlet_king"),
            new Legacy("scp_unleashed-scarlet-court-server.toml", "scarlet_court"));
    private static final Logger Log = LogUtils.getLogger();

    public record Legacy(String File, String Section) {
    }

    private ScpConfigMigration() {
    }

    public static int migrate(Path Folder, ModConfigSpec Spec, List<Legacy> Files0) {
        if (!Spec.isLoaded()) {
            return 0;
        }
        int Moved = 0;
        for (Legacy Each : Files0) {
            Path Old = Folder.resolve(Each.File());
            if (!Files.isRegularFile(Old)) {
                continue;
            }
            try {
                UnmodifiableConfig Data = TomlFormat.instance().createParser().parse(Old, FileNotFoundAction.THROW_ERROR);
                int Count = copy(Data, Each.Section(), Spec);
                Files.move(Old, Old.resolveSibling(Each.File() + Suffix), StandardCopyOption.REPLACE_EXISTING);
                Log.info("Moved {} settings from {} into the merged SCP Unleashed config", Count, Each.File());
                Moved += Count;
            } catch (Exception Problem) {
                Log.warn("Could not move the old SCP Unleashed config {}: {}", Each.File(), Problem.toString());
            }
        }
        if (Moved > 0) {
            Spec.save();
        }
        return Moved;
    }

    private static int copy(UnmodifiableConfig Source, String Prefix, ModConfigSpec Spec) {
        int Count = 0;
        for (UnmodifiableConfig.Entry Entry : Source.entrySet()) {
            String Path0 = Prefix + "." + Entry.getKey();
            Object Value = Entry.getValue();
            if (Value instanceof UnmodifiableConfig Inner) {
                Count += copy(Inner, Path0, Spec);
                continue;
            }
            if (apply(Spec, Path0, Value)) {
                Count++;
            }
        }
        return Count;
    }

    @SuppressWarnings("unchecked")
    private static boolean apply(ModConfigSpec Spec, String Path0, Object Value) {
        Object Target = Spec.getValues().get(Path0);
        Object Rule = Spec.getSpec().get(Path0);
        if (!(Target instanceof ModConfigSpec.ConfigValue<?> Setting) || !(Rule instanceof ModConfigSpec.ValueSpec Check)) {
            return false;
        }
        Object Default = Check.getDefault();
        if (Value instanceof Number Amount) {
            if (Default instanceof Double) {
                Value = Amount.doubleValue();
            } else if (Default instanceof Integer) {
                Value = Amount.intValue();
            } else if (Default instanceof Long) {
                Value = Amount.longValue();
            }
        }
        if (!Check.test(Value)) {
            return false;
        }
        if (Setting instanceof ModConfigSpec.EnumValue<?> Choice && Value instanceof String Name) {
            Class<? extends Enum> Type = ((Enum<?>) Choice.getDefault()).getDeclaringClass();
            try {
                Value = Enum.valueOf(Type, Name);
            } catch (IllegalArgumentException Unknown) {
                return false;
            }
        }
        ((ModConfigSpec.ConfigValue<Object>) Setting).set(Value);
        return true;
    }
}
