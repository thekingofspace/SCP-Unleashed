package net.rai.scp_unleashed.computer.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.computer.ChatServerBlockEntity;
import net.rai.scp_unleashed.computer.ComputerBlockEntity;
import net.rai.scp_unleashed.computer.ComputerDeviceEntity;
import net.rai.scp_unleashed.computer.MessageDatabaseBlockEntity;
import net.rai.scp_unleashed.computer.PortMedia;
import net.rai.scp_unleashed.computer.UploadPortBlockEntity;
import net.rai.scp_unleashed.computer.data.ComputerFile;
import net.rai.scp_unleashed.computer.data.ScheduledAction;
import net.rai.scp_unleashed.computer.net.ComputerGrid;
import net.rai.scp_unleashed.computer.net.ComputerSessions;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.List;

public enum ComputerJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "computer_device");
    private static final String DataKey = "ComputerDevice";
    private static final String Prefix = "jade.scp_unleashed.computer.";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        if (!(Accessor.getLevel() instanceof ServerLevel World) || !(Accessor.getBlockEntity() instanceof ComputerDeviceEntity Device)) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        Info.putBoolean("Running", Device.isRunning());
        BlockEntity Entity = Accessor.getBlockEntity();
        if (Entity instanceof ComputerBlockEntity Computer) {
            Info.putInt("Files", Computer.getFiles().size());
            int Active = 0;
            for (ScheduledAction Action : Computer.getActions()) {
                if (Computer.isActive(Action.Id())) {
                    Active++;
                }
            }
            Info.putInt("Active", Active);
            Info.putInt("Viewers", ComputerSessions.openCount(World, Computer.getBlockPos()));
        } else if (Entity instanceof ChatServerBlockEntity Server && Server.isRunning()) {
            ComputerGrid.Reach Reach = ComputerGrid.reach(World, Server.getBlockPos());
            Info.putInt("Terminals", Reach.runningComputers());
            Info.putInt("Channels", Reach.runningDatabases().size() + 1);
        } else if (Entity instanceof MessageDatabaseBlockEntity Database) {
            Info.putString("Channel", Database.getChannelName());
            Info.putInt("Stored", Database.count());
        } else if (Entity instanceof UploadPortBlockEntity Port) {
            Info.putInt("Media", Port.getMedia().ordinal());
            List<ComputerFile> Files = Port.files();
            Info.putInt("Count", Files.size());
            Info.putInt("Capacity", Port.capacity());
            if (Port.getMedia() == PortMedia.Dossier && !Files.isEmpty()) {
                Info.putString("Dossier", Files.getFirst().Name());
            }
        }
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Info = Data.getCompound(DataKey);
        boolean Running = Info.getBoolean("Running");
        Tooltip.add(Component.translatable(Running ? Prefix + "online" : Prefix + "offline").withStyle(Running ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
        if (Info.contains("Files")) {
            Tooltip.add(Component.translatable(Prefix + "files", Info.getInt("Files"), ComputerBlockEntity.MaxFiles).withStyle(ChatFormatting.GRAY));
            if (Info.getInt("Active") > 0) {
                Tooltip.add(Component.translatable(Prefix + "actions", Info.getInt("Active")).withStyle(ChatFormatting.GOLD));
            }
            if (Info.getInt("Viewers") > 0) {
                Tooltip.add(Component.translatable(Prefix + "viewers", Info.getInt("Viewers")).withStyle(ChatFormatting.GRAY));
            }
        }
        if (Info.contains("Terminals")) {
            Tooltip.add(Component.translatable(Prefix + "terminals", Info.getInt("Terminals")).withStyle(ChatFormatting.GRAY));
            Tooltip.add(Component.translatable(Prefix + "channels", Info.getInt("Channels")).withStyle(ChatFormatting.GRAY));
        }
        if (Info.contains("Channel")) {
            Tooltip.add(Component.translatable(Prefix + "channel", Info.getString("Channel")).withStyle(ChatFormatting.AQUA));
            Tooltip.add(Component.translatable(Prefix + "stored", Info.getInt("Stored"), MessageDatabaseBlockEntity.Capacity).withStyle(ChatFormatting.GRAY));
        }
        if (Info.contains("Media")) {
            PortMedia Media = PortMedia.byId(Info.getInt("Media"));
            switch (Media) {
                case Empty -> Tooltip.add(Component.translatable(Prefix + "port.empty").withStyle(ChatFormatting.DARK_GRAY));
                case Floppy -> Tooltip.add(Component.translatable(Prefix + "port.floppy", Info.getInt("Count"), Info.getInt("Capacity")).withStyle(ChatFormatting.AQUA));
                case Dossier -> Tooltip.add(Component.translatable(Prefix + "port.dossier", Info.getString("Dossier")).withStyle(ChatFormatting.AQUA));
            }
        }
    }
}
