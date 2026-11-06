package net.rai.scp_unleashed.scp049.client;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.PlayerSkin;
import net.rai.scp_unleashed.scp049.Scp049Cured;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class Scp049Skins {
    public static final String FallbackName = "Subject";
    private static final Map<String, Supplier<PlayerSkin>> Lookups = new ConcurrentHashMap<>();

    private Scp049Skins() {
    }

    @Nullable
    public static PlayerSkin skinOf(Scp049Cured Risen) {
        Optional<UUID> Owner = Risen.getSkinId();
        if (Owner.isEmpty()) {
            return null;
        }
        Minecraft Game = Minecraft.getInstance();
        if (Game.getConnection() != null) {
            PlayerInfo Online = Game.getConnection().getPlayerInfo(Owner.get());
            if (Online != null) {
                return Online.getSkin();
            }
        }
        String Textures = Risen.getSkinTextures();
        String Signature = Risen.getSkinSignature();
        String Name = Risen.getSkinName().isEmpty() ? FallbackName : Risen.getSkinName();
        Supplier<PlayerSkin> Lookup = Lookups.computeIfAbsent(Owner.get() + "|" + Textures.hashCode(), Unused -> {
            GameProfile Profile = new GameProfile(Owner.get(), Name);
            if (!Textures.isEmpty()) {
                Profile.getProperties().put(Scp049Cured.TexturesKey, new Property(Scp049Cured.TexturesKey, Textures, Signature.isEmpty() ? null : Signature));
            }
            return Game.getSkinManager().lookupInsecure(Profile);
        });
        return Lookup.get();
    }

    public static void reset() {
        Lookups.clear();
    }
}
