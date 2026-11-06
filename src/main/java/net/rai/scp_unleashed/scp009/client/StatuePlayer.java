package net.rai.scp_unleashed.scp009.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.resources.PlayerSkin;

public class StatuePlayer extends RemotePlayer {
    public StatuePlayer(ClientLevel World, GameProfile Profile) {
        super(World, Profile);
    }

    @Override
    public PlayerSkin getSkin() {
        Minecraft Client = Minecraft.getInstance();
        ClientPacketListener Connection = Client.getConnection();
        PlayerInfo Info = Connection == null ? null : Connection.getPlayerInfo(getUUID());
        if (Info != null) {
            return Info.getSkin();
        }
        return Client.getSkinManager().getInsecureSkin(getGameProfile());
    }

    @Override
    public boolean isSpectator() {
        return false;
    }

    @Override
    public boolean isCreative() {
        return false;
    }
}
