package net.rai.scp_unleashed.playerclone.client;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.rai.scp_unleashed.playerclone.PlayerClone;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class PlayerCloneSkins {
    public static final String TexturesKey = "textures";
    private static final Map<ResolvableProfile, CompletableFuture<PlayerSkin>> Loading = new HashMap<>();

    private PlayerCloneSkins() {
    }

    public static PlayerSkin skinOf(PlayerClone Clone) {
        return skinOf(Clone.profile(), Clone.getUUID());
    }

    public static PlayerSkin skinOf(@Nullable ResolvableProfile Profile, UUID Fallback) {
        if (Profile == null || Profile.id().isEmpty()) {
            return DefaultPlayerSkin.get(Fallback);
        }
        GameProfile Known = Profile.gameProfile();
        PlayerSkin Default = DefaultPlayerSkin.get(Known);
        Property Packed = Known.getProperties().get(TexturesKey).stream().findFirst().orElse(null);
        if (Packed == null) {
            PlayerSkin Online = onlineSkin(Known.getId());
            if (Online != null) {
                return Online;
            }
        }
        boolean Textured = Packed != null;
        CompletableFuture<PlayerSkin> Future = Loading.computeIfAbsent(Profile, Missing -> load(Known, Textured));
        if (!Future.isDone() || Future.isCompletedExceptionally() || Future.isCancelled()) {
            return Default;
        }
        PlayerSkin Loaded = Future.getNow(Default);
        return Loaded == null ? Default : Loaded;
    }

    @Nullable
    private static PlayerSkin onlineSkin(UUID Id) {
        ClientPacketListener Connection = Minecraft.getInstance().getConnection();
        PlayerInfo Online = Connection == null ? null : Connection.getPlayerInfo(Id);
        return Online == null ? null : Online.getSkin();
    }

    private static CompletableFuture<PlayerSkin> load(GameProfile Known, boolean Textured) {
        SkinManager Skins = Minecraft.getInstance().getSkinManager();
        if (Textured) {
            return Skins.getOrLoad(Known);
        }
        return SkullBlockEntity.fetchGameProfile(Known.getId()).thenCompose(Found -> Skins.getOrLoad(Found.orElse(Known)));
    }

    public static void reset() {
        Loading.clear();
    }
}
