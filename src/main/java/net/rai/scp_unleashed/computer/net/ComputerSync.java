package net.rai.scp_unleashed.computer.net;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.rai.scp_unleashed.archive.database.DatabaseArchive;
import net.rai.scp_unleashed.archive.database.DatabaseRecord;
import net.rai.scp_unleashed.computer.ComputerBlockEntity;
import net.rai.scp_unleashed.computer.MessageDatabaseBlockEntity;
import net.rai.scp_unleashed.computer.UploadPortBlockEntity;
import net.rai.scp_unleashed.computer.data.ChatChannels;
import net.rai.scp_unleashed.computer.data.ChatMessage;
import net.rai.scp_unleashed.computer.data.ComputerFile;
import net.rai.scp_unleashed.computer.data.ComputerText;
import net.rai.scp_unleashed.computer.data.RedstoneButton;
import net.rai.scp_unleashed.computer.data.ScheduledAction;
import net.rai.scp_unleashed.computer.data.SoundEntry;
import net.rai.scp_unleashed.mining.shop.ShopService;

import java.util.List;

public final class ComputerSync {
    public static final int MaxShownMessages = 60;

    private ComputerSync() {
    }

    public static CompoundTag build(ServerLevel World, ComputerBlockEntity Computer, ComputerSessions.Session Open, ComputerSection Section) {
        ComputerGrid.Reach Reach = ComputerGrid.reach(World, Computer.getBlockPos());
        HolderLookup.Provider Registries = World.registryAccess();
        return switch (Section) {
            case System -> system(World, Computer, Reach);
            case Files -> files(Computer, Reach, Registries);
            case Chat -> chat(World, Computer, Reach, Open, Registries);
            case Documents -> documents(World, Reach, Registries);
            case Redstone -> redstone(Computer, Registries);
            case Sounds -> sounds(Computer, Registries);
            case Scheduler -> scheduler(World, Computer, Reach, Registries);
            case Shop -> ShopService.sync(World, Computer.getBlockPos());
        };
    }

    private static CompoundTag system(ServerLevel World, ComputerBlockEntity Computer, ComputerGrid.Reach Reach) {
        CompoundTag Data = new CompoundTag();
        Data.putBoolean("Running", Computer.isRunning());
        Data.putInt("Clock", ComputerText.clockMinute(World.getDayTime()));
        Data.putLong("Day", World.getDayTime() / 24000L + 1L);
        Data.putBoolean("Chat", Reach.chatServer() != null);
        Data.putBoolean("Docs", Reach.hasDocuments());
        Data.putInt("Drives", Reach.runningPorts().size());
        Data.putInt("Computers", Reach.runningComputers());
        ListTag Channels = new ListTag();
        for (ChatChannels.Info Info : ChatService.channels(World, Reach)) {
            CompoundTag Entry = new CompoundTag();
            Entry.putString("Key", Info.Key());
            Entry.putLong("Last", Info.LastId());
            Channels.add(Entry);
        }
        Data.put("Channels", Channels);
        return Data;
    }

    private static CompoundTag files(ComputerBlockEntity Computer, ComputerGrid.Reach Reach, HolderLookup.Provider Registries) {
        CompoundTag Data = new CompoundTag();
        Data.put("Local", ComputerCodecs.encodeList(ComputerFile.FileCodec, Computer.getFiles(), Registries));
        Data.putInt("Capacity", ComputerBlockEntity.MaxFiles);
        ListTag Drives = new ListTag();
        for (UploadPortBlockEntity Port : Reach.runningPorts()) {
            CompoundTag Drive = new CompoundTag();
            Drive.putLong("Pos", Port.getBlockPos().asLong());
            Drive.putInt("Media", Port.getMedia().ordinal());
            Drive.putInt("Capacity", Port.capacity());
            Drive.putBoolean("Writable", Port.isWritable());
            if (Port.getContent().has(DataComponents.CUSTOM_NAME)) {
                Drive.putString("Label", ComputerText.clean(Port.getContent().getHoverName().getString(), 20));
            }
            Drive.put("Files", ComputerCodecs.encodeList(ComputerFile.FileCodec, Port.files(), Registries));
            Drives.add(Drive);
        }
        Data.put("Drives", Drives);
        return Data;
    }

    private static CompoundTag chat(ServerLevel World, ComputerBlockEntity Computer, ComputerGrid.Reach Reach, ComputerSessions.Session Open, HolderLookup.Provider Registries) {
        CompoundTag Data = new CompoundTag();
        Data.putBoolean("Online", Reach.chatServer() != null);
        Data.put("Channels", ComputerCodecs.encodeList(ChatChannels.Info.InfoCodec, ChatService.channels(World, Reach), Registries));
        Data.putString("Channel", Open.getChannel());
        List<ChatMessage> Messages = ChatService.history(World, Reach, Open.getChannel());
        if (Messages.size() > MaxShownMessages) {
            Messages = Messages.subList(Messages.size() - MaxShownMessages, Messages.size());
        }
        Data.put("Messages", ComputerCodecs.encodeList(ChatMessage.MessageCodec, Messages, Registries));
        Data.put("Local", ComputerCodecs.encodeList(ComputerFile.FileCodec, Computer.getFiles(), Registries));
        return Data;
    }

    private static CompoundTag documents(ServerLevel World, ComputerGrid.Reach Reach, HolderLookup.Provider Registries) {
        CompoundTag Data = new CompoundTag();
        Data.putBoolean("Present", Reach.Documents() != null);
        Data.putBoolean("Online", Reach.hasDocuments());
        if (Reach.hasDocuments()) {
            Data.put("Records", ComputerCodecs.encodeList(DatabaseRecord.RecordCodec, DatabaseArchive.get(World.getServer()).records(), Registries));
        }
        return Data;
    }

    private static CompoundTag redstone(ComputerBlockEntity Computer, HolderLookup.Provider Registries) {
        CompoundTag Data = new CompoundTag();
        ListTag Buttons = new ListTag();
        for (RedstoneButton Button : Computer.getButtons()) {
            Tag Encoded = ComputerCodecs.encode(RedstoneButton.ButtonCodec, Button, Registries);
            if (Encoded instanceof CompoundTag Entry) {
                Entry.putBoolean("On", Computer.isButtonOn(Button.Id()));
                Entry.putLong("Left", Computer.buttonRemaining(Button.Id()));
                Buttons.add(Entry);
            }
        }
        Data.put("Buttons", Buttons);
        Data.putInt("Max", ComputerBlockEntity.MaxButtons);
        return Data;
    }

    private static CompoundTag sounds(ComputerBlockEntity Computer, HolderLookup.Provider Registries) {
        CompoundTag Data = new CompoundTag();
        ListTag Entries = new ListTag();
        for (SoundEntry Entry : Computer.getSounds()) {
            Tag Encoded = ComputerCodecs.encode(SoundEntry.EntryCodec, Entry, Registries);
            if (Encoded instanceof CompoundTag Saved) {
                Saved.putBoolean("Playing", Computer.isPlaying(Entry.Id()));
                Entries.add(Saved);
            }
        }
        Data.put("Entries", Entries);
        Data.putInt("Max", ComputerBlockEntity.MaxSounds);
        return Data;
    }

    private static CompoundTag scheduler(ServerLevel World, ComputerBlockEntity Computer, ComputerGrid.Reach Reach, HolderLookup.Provider Registries) {
        CompoundTag Data = new CompoundTag();
        ListTag Actions = new ListTag();
        for (ScheduledAction Action : Computer.getActions()) {
            Tag Encoded = ComputerCodecs.encode(ScheduledAction.ActionCodec, Action, Registries);
            if (Encoded instanceof CompoundTag Saved) {
                Saved.putBoolean("Active", Computer.isActive(Action.Id()));
                Saved.putLong("For", Computer.activeFor(Action.Id()));
                Saved.putInt("Holds", Computer.holdCount(Action.Id()));
                Actions.add(Saved);
            }
        }
        Data.put("Actions", Actions);
        Data.putInt("Max", ComputerBlockEntity.MaxActions);
        Data.putInt("Clock", ComputerText.clockMinute(World.getDayTime()));
        ListTag Channels = new ListTag();
        for (MessageDatabaseBlockEntity Database : Reach.runningDatabases()) {
            CompoundTag Entry = new CompoundTag();
            Entry.putString("Key", Database.getKey());
            Entry.putString("Name", Database.getChannelName());
            Channels.add(Entry);
        }
        Data.put("Channels", Channels);
        Data.put("Sounds", ComputerCodecs.encodeList(SoundEntry.EntryCodec, Computer.getSounds(), Registries));
        return Data;
    }

    public static int listSize(CompoundTag Holder, String Key) {
        return Holder.getList(Key, Tag.TAG_COMPOUND).size();
    }

    public static boolean hasList(CompoundTag Holder, String Key) {
        return Holder.contains(Key, Tag.TAG_LIST);
    }
}
