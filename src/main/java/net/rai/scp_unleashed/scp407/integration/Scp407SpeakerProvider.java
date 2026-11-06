package net.rai.scp_unleashed.scp407.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.rai.scp_unleashed.scp407.Scp407Registry;
import net.rai.scp_unleashed.scp407.Scp407Session;
import net.rai.scp_unleashed.scp407.Scp407Sessions;
import net.rai.scp_unleashed.scp407.Scp407Song;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum Scp407SpeakerProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    public static final String Prefix = "jade.scp_unleashed.scp407.";
    private static final ResourceLocation Uid = Scp407Registry.location("scp407_speaker");
    private static final String PositionKey = "Scp407Position";
    private static final String PhaseKey = "Scp407Phase";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    public static String clock(long Ticks) {
        long Seconds = Math.max(0L, Ticks) / 20L;
        return String.format("%d:%02d", Seconds / 60L, Seconds % 60L);
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        if (!(Accessor.getLevel() instanceof ServerLevel World)) {
            return;
        }
        Scp407Session Session = Scp407Sessions.get(World).withSpeaker(Accessor.getPosition());
        if (Session == null) {
            return;
        }
        long Position = Session.position(World.getGameTime());
        Data.putLong(PositionKey, Session.anyLoop() ? Position % Scp407Song.Length : Math.min(Position, Scp407Song.Length));
        String Phase = !Session.isPlaying() ? "coda" : switch (Session.Act) {
            case Scp407Session.ActGenesis -> "genesis";
            case Scp407Session.ActWanderer -> "wanderer";
            default -> "overture";
        };
        Data.putString(PhaseKey, Phase);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(PhaseKey)) {
            return;
        }
        long Position = Data.getLong(PositionKey);
        int Percent = (int) Math.round(100.0 * Position / Scp407Song.Length);
        Tooltip.add(Component.translatable(Prefix + "playing", Component.translatable(Scp407Registry.TrackKey), clock(Position), clock(Scp407Song.Length), Percent)
                .withStyle(ChatFormatting.GREEN));
        Tooltip.add(Component.translatable(Prefix + "movement." + Data.getString(PhaseKey)).withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC));
    }
}
