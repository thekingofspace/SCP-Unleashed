package net.rai.scp_unleashed.scp035.client;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.rai.scp_unleashed.scp035.Scp035Host;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class Scp035Skins {
    public static final String FallbackName = "Host";
    private static final Map<String, Supplier<PlayerSkin>> Lookups = new ConcurrentHashMap<>();

    private Scp035Skins() {
    }

    public static PlayerSkin skinOf(Scp035Host Host) {
        Optional<UUID> Owner = Host.getSkinId();
        if (Owner.isEmpty()) {
            return DefaultPlayerSkin.get(Host.getUUID());
        }
        Minecraft Game = Minecraft.getInstance();
        if (Game.getConnection() != null) {
            PlayerInfo Online = Game.getConnection().getPlayerInfo(Owner.get());
            if (Online != null) {
                return Online.getSkin();
            }
        }
        String Textures = Host.getSkinTextures();
        String Signature = Host.getSkinSignature();
        String Name = Host.getSkinName().isEmpty() ? FallbackName : Host.getSkinName();
        Supplier<PlayerSkin> Lookup = Lookups.computeIfAbsent(Owner.get() + "|" + Textures.hashCode(), Unused -> {
            GameProfile Profile = new GameProfile(Owner.get(), Name);
            if (!Textures.isEmpty()) {
                Profile.getProperties().put(Scp035Host.TexturesKey, new Property(Scp035Host.TexturesKey, Textures, Signature.isEmpty() ? null : Signature));
            }
            return Game.getSkinManager().lookupInsecure(Profile);
        });
        return Lookup.get();
    }

    public static void reset() {
        Lookups.clear();
    }
}
