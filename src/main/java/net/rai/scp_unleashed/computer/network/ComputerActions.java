package net.rai.scp_unleashed.computer.network;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.archive.database.DatabaseArchive;
import net.rai.scp_unleashed.archive.database.DatabaseRecord;
import net.rai.scp_unleashed.computer.ComputerBlockEntity;
import net.rai.scp_unleashed.computer.ComputerImages;
import net.rai.scp_unleashed.computer.ComputerRegistry;
import net.rai.scp_unleashed.computer.MessageDatabaseBlockEntity;
import net.rai.scp_unleashed.computer.UploadPortBlockEntity;
import net.rai.scp_unleashed.computer.data.ActionStep;
import net.rai.scp_unleashed.computer.data.ChatChannels;
import net.rai.scp_unleashed.computer.data.ChatMessage;
import net.rai.scp_unleashed.computer.data.ComputerFile;
import net.rai.scp_unleashed.computer.data.ComputerText;
import net.rai.scp_unleashed.computer.data.RedstoneButton;
import net.rai.scp_unleashed.computer.data.ScheduledAction;
import net.rai.scp_unleashed.computer.data.SoundEntry;
import net.rai.scp_unleashed.computer.net.ChatService;
import net.rai.scp_unleashed.computer.net.ComputerApp;
import net.rai.scp_unleashed.computer.net.ComputerCodecs;
import net.rai.scp_unleashed.computer.net.ComputerGrid;
import net.rai.scp_unleashed.computer.net.ComputerSection;
import net.rai.scp_unleashed.computer.net.ComputerSessions;
import net.rai.scp_unleashed.mining.shop.ShopService;
import net.rai.scp_unleashed.speaker.SoundCatalog;
import net.rai.scp_unleashed.speaker.cassette.AnomalousLibrary;
import net.rai.scp_unleashed.speaker.cassette.CassetteRecording;
import net.rai.scp_unleashed.discord.DiscordNotices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ComputerActions {
    public static final int ChatCooldown = 8;
    public static final double DatabaseReach = 8.0;
    private static final Map<UUID, Long> LastChat = new HashMap<>();

    private ComputerActions() {
    }

    public static void handle(ServerPlayer User, ComputerActionPayload Payload) {
        ServerLevel World = User.serverLevel();
        BlockPos Pos = Payload.Pos();
        if (!World.isLoaded(Pos)) {
            return;
        }
        ComputerAction Action = ComputerAction.byId(Payload.Action());
        CompoundTag Data = Payload.Data();
        if (Action == ComputerAction.DatabaseRename || Action == ComputerAction.DatabaseClear) {
            handleDatabase(User, World, Pos, Action, Data);
            return;
        }
        if (Action == ComputerAction.Close) {
            if (ComputerSessions.of(User, Pos) != null) {
                ComputerSessions.close(User);
            }
            return;
        }
        ComputerSessions.Session Open = ComputerSessions.of(User, Pos);
        if (Open == null || !(World.getBlockEntity(Pos) instanceof ComputerBlockEntity Computer) || !Computer.isRunning()) {
            return;
        }
        switch (Action) {
            case App -> Open.setApp(ComputerApp.byId(Data.getInt("App")));
            case FileDelete -> result(User, Computer, Computer.deleteFile(Data.getInt("Index")), "deleted", "failed");
            case FileRename -> result(User, Computer, Computer.renameFile(Data.getInt("Index"), Data.getString("Name")), "renamed", "failed");
            case NoteSave -> result(User, Computer, Computer.saveNote(Data.getInt("Index"), Data.getString("Name"), Data.getString("Text")), "saved", "full");
            case FileImport -> importFile(User, World, Computer, Data);
            case FileExport -> exportFile(User, World, Computer, Data);
            case DriveDelete -> driveDelete(User, World, Computer, Data);
            case DriveRename -> driveRename(User, World, Computer, Data);
            case ChatChannel -> {
                String Key = Data.getString("Key");
                if (ChatChannels.isValidKey(Key)) {
                    Open.setChannel(Key);
                }
            }
            case ChatSend -> chatSend(User, World, Computer, Open, Data);
            case ChatView -> {
                if (Data.getString("Key").equals(Open.getChannel())) {
                    Open.setView(Data.getInt("Scroll"), Data.getInt("Visible"));
                }
            }
            case ChatSave -> chatSave(User, World, Computer, Data);
            case DocumentSave -> documentSave(User, World, Computer, Data);
            case ButtonSave -> ComputerCodecs.decode(RedstoneButton.ButtonCodec, Data.getCompound("Button"), World.registryAccess())
                    .ifPresent(Button -> result(User, Computer, Computer.saveButton(Button), "saved", "full"));
            case ButtonDelete -> Computer.deleteButton(Data.getInt("Id"));
            case ButtonPress -> Computer.pressButton(World, Data.getInt("Id"));
            case SoundSave -> ComputerCodecs.decode(SoundEntry.EntryCodec, Data.getCompound("Sound"), World.registryAccess())
                    .ifPresent(Entry -> result(User, Computer, Computer.saveSound(validated(User, Computer, Entry)), "saved", "full"));
            case SoundDelete -> Computer.deleteSound(Data.getInt("Id"));
            case SoundPress -> Computer.pressSound(World, Data.getInt("Id"));
            case ActionSave -> ComputerCodecs.decode(ScheduledAction.ActionCodec, Data.getCompound("Action"), World.registryAccess())
                    .ifPresent(Saved -> result(User, Computer, Computer.saveAction(named(World, Computer, Saved)), "saved", "full"));
            case ActionDelete -> Computer.deleteAction(Data.getInt("Id"));
            case ActionRun -> Computer.runAction(World, Data.getInt("Id"), Data.getBoolean("On"));
            case ShopBuy -> ShopService.buy(User, World, Computer, Data);
            case ImageView -> imageView(User, World, Computer, Data);
            case ChatImage -> chatImage(User, World, Computer, Data);
            default -> {
            }
        }
    }

    private static void handleDatabase(ServerPlayer User, ServerLevel World, BlockPos Pos, ComputerAction Action, CompoundTag Data) {
        if (!(World.getBlockEntity(Pos) instanceof MessageDatabaseBlockEntity Database) || User.distanceToSqr(Vec3.atCenterOf(Pos)) > DatabaseReach * DatabaseReach) {
            return;
        }
        if (Action == ComputerAction.DatabaseRename) {
            Database.rename(Data.getString("Name"));
        } else {
            Database.clear();
            World.playSound(null, Pos, ComputerRegistry.DiskSeek.get(), SoundSource.BLOCKS, 0.7f, 0.8f);
        }
        ComputerSessions.touchReach(World, ComputerGrid.reach(World, Pos), ComputerSection.Chat.bit() | ComputerSection.System.bit() | ComputerSection.Scheduler.bit());
        ComputerNetworking.openDatabase(User, Database);
    }

    private static void result(ServerPlayer User, ComputerBlockEntity Computer, boolean Done, String Good, String Bad) {
        ComputerNetworking.notice(User, Computer.getBlockPos(), Component.translatable("gui.scp_unleashed.computer.notice." + (Done ? Good : Bad)),
                Done ? ComputerNoticePayload.Info : ComputerNoticePayload.Error);
    }

    private static void notice(ServerPlayer User, ComputerBlockEntity Computer, String Key, boolean Error, Object... Arguments) {
        ComputerNetworking.notice(User, Computer.getBlockPos(), Component.translatable("gui.scp_unleashed.computer.notice." + Key, Arguments),
                Error ? ComputerNoticePayload.Error : ComputerNoticePayload.Info);
    }

    private static UploadPortBlockEntity drive(ServerLevel World, ComputerBlockEntity Computer, CompoundTag Data) {
        return ComputerGrid.reach(World, Computer.getBlockPos()).port(BlockPos.of(Data.getLong("Drive")));
    }

    private static void importFile(ServerPlayer User, ServerLevel World, ComputerBlockEntity Computer, CompoundTag Data) {
        UploadPortBlockEntity Port = drive(World, Computer, Data);
        if (Port == null) {
            notice(User, Computer, "no_drive", true);
            return;
        }
        List<ComputerFile> Files = Port.files();
        int Index = Data.getInt("Index");
        if (Index < 0 || Index >= Files.size()) {
            return;
        }
        ComputerFile File = Files.get(Index);
        if (Computer.hasCopy(File)) {
            notice(User, Computer, "exists", true);
            return;
        }
        if (!Computer.addFile(File)) {
            notice(User, Computer, "full", true);
            return;
        }
        Port.readSound();
        notice(User, Computer, "copied", false, File.Name());
    }

    private static void imageView(ServerPlayer User, ServerLevel World, ComputerBlockEntity Computer, CompoundTag Data) {
        UploadPortBlockEntity Port = Data.contains("Drive") ? drive(World, Computer, Data) : null;
        List<ComputerFile> Files = Port != null ? Port.files() : Data.contains("Drive") ? List.of() : Computer.getFiles();
        int Index = Data.getInt("Index");
        if (Index < 0 || Index >= Files.size() || !Files.get(Index).isImage()) {
            return;
        }
        if (Port != null) {
            Port.readSound();
        }
        ComputerImages.viewed(User, Computer, Port, Files.get(Index));
    }

    private static void exportFile(ServerPlayer User, ServerLevel World, ComputerBlockEntity Computer, CompoundTag Data) {
        UploadPortBlockEntity Port = drive(World, Computer, Data);
        ComputerFile File = Computer.file(Data.getInt("Index"));
        if (Port == null || File == null) {
            notice(User, Computer, "no_drive", true);
            return;
        }
        if (!Port.isWritable()) {
            notice(User, Computer, "read_only", true);
            return;
        }
        if (!Port.write(File)) {
            notice(User, Computer, "disc_full", true);
            return;
        }
        ComputerSessions.touchReach(World, ComputerGrid.reach(World, Computer.getBlockPos()), ComputerSection.Files.bit());
        notice(User, Computer, "written", false, File.Name());
    }

    private static void driveDelete(ServerPlayer User, ServerLevel World, ComputerBlockEntity Computer, CompoundTag Data) {
        UploadPortBlockEntity Port = drive(World, Computer, Data);
        if (Port == null || !Port.delete(Data.getInt("Index"))) {
            notice(User, Computer, "read_only", true);
            return;
        }
        ComputerSessions.touchReach(World, ComputerGrid.reach(World, Computer.getBlockPos()), ComputerSection.Files.bit());
        notice(User, Computer, "deleted", false);
    }

    private static void driveRename(ServerPlayer User, ServerLevel World, ComputerBlockEntity Computer, CompoundTag Data) {
        UploadPortBlockEntity Port = drive(World, Computer, Data);
        if (Port == null || !Port.rename(Data.getInt("Index"), Data.getString("Name"))) {
            notice(User, Computer, "read_only", true);
            return;
        }
        ComputerSessions.touchReach(World, ComputerGrid.reach(World, Computer.getBlockPos()), ComputerSection.Files.bit());
        notice(User, Computer, "renamed", false);
    }

    private static void chatSend(ServerPlayer User, ServerLevel World, ComputerBlockEntity Computer, ComputerSessions.Session Open, CompoundTag Data) {
        String Key = Data.getString("Key");
        if (!ChatChannels.isValidKey(Key)) {
            return;
        }
        long Now = World.getGameTime();
        Long Last = LastChat.get(User.getUUID());
        if (Last != null && Now - Last < ChatCooldown && Now >= Last) {
            return;
        }
        String Text = ComputerText.clean(Data.getString("Text"), ChatMessage.MaxText);
        int Attach = Data.contains("Attach") ? Data.getInt("Attach") : -1;
        Optional<ComputerFile> File = Optional.ofNullable(Attach >= 0 ? Computer.file(Attach) : null);
        if (Text.isEmpty() && File.isEmpty()) {
            return;
        }
        LastChat.put(User.getUUID(), Now);
        ChatMessage Draft = new ChatMessage(0L, ChatMessage.Sender.Player, User.getGameProfile().getName(), Text, "", File, World.getDayTime(), false);
        ChatService.Result Done = ChatService.post(World, ComputerGrid.reach(World, Computer.getBlockPos()), Key, Draft);
        switch (Done) {
            case NoServer -> notice(User, Computer, "no_server", true);
            case NoChannel -> notice(User, Computer, "no_channel", true);
            case Posted -> {
                Open.setChannel(Key);
                Open.setView(0, 8);
                DiscordNotices.computerChat(User, World, Computer.getBlockPos(), Key, Draft);
            }
        }
    }

    private static void chatSave(ServerPlayer User, ServerLevel World, ComputerBlockEntity Computer, CompoundTag Data) {
        String Key = Data.getString("Key");
        long Id = Data.getLong("Id");
        for (ChatMessage Message : ChatService.history(World, ComputerGrid.reach(World, Computer.getBlockPos()), Key)) {
            if (Message.Id() != Id || Message.Attachment().isEmpty()) {
                continue;
            }
            ComputerFile File = Message.Attachment().get();
            if (Computer.hasCopy(File)) {
                notice(User, Computer, "exists", true);
            } else if (!Computer.addFile(File)) {
                notice(User, Computer, "full", true);
            } else {
                notice(User, Computer, "copied", false, File.Name());
            }
            return;
        }
        notice(User, Computer, "gone", true);
    }

    private static void chatImage(ServerPlayer User, ServerLevel World, ComputerBlockEntity Computer, CompoundTag Data) {
        String Key = Data.getString("Key");
        long Id = Data.getLong("Id");
        for (ChatMessage Message : ChatService.history(World, ComputerGrid.reach(World, Computer.getBlockPos()), Key)) {
            if (Message.Id() == Id && Message.Attachment().isPresent() && Message.Attachment().get().isImage()) {
                ComputerImages.viewed(User, Computer, null, Message.Attachment().get());
                return;
            }
        }
    }

    private static void documentSave(ServerPlayer User, ServerLevel World, ComputerBlockEntity Computer, CompoundTag Data) {
        ComputerGrid.Reach Reach = ComputerGrid.reach(World, Computer.getBlockPos());
        ResourceLocation Id = ResourceLocation.tryParse(Data.getString("Document"));
        if (!Reach.hasDocuments() || Id == null) {
            notice(User, Computer, "no_documents", true);
            return;
        }
        Optional<DatabaseRecord> Entry = DatabaseArchive.get(World.getServer()).find(Id);
        if (Entry.isEmpty()) {
            notice(User, Computer, "gone", true);
            return;
        }
        ComputerFile File = ComputerFile.of(Entry.get());
        if (Computer.hasCopy(File)) {
            notice(User, Computer, "exists", true);
        } else if (!Computer.addFile(File)) {
            notice(User, Computer, "full", true);
        } else {
            notice(User, Computer, "copied", false, File.Name());
        }
    }

    private static SoundEntry validated(ServerPlayer User, ComputerBlockEntity Computer, SoundEntry Requested) {
        SoundCatalog.Entry Known = SoundCatalog.find(Requested.Sound());
        if (Known != null && !Known.isAnomalous()) {
            return Requested.withSound(Known.Id(), "");
        }
        Optional<CassetteRecording> Owned = AnomalousLibrary.find(User, Requested.Sound());
        if (Owned.isPresent()) {
            return Requested.withSound(Requested.Sound(), Requested.Label().isBlank() ? Owned.get().Title().getString() : Requested.Label());
        }
        SoundEntry Existing = Computer.sound(Requested.Id());
        if (Existing != null && Existing.Sound().equals(Requested.Sound())) {
            return Requested.withSound(Existing.Sound(), Existing.Label());
        }
        return Requested.withSound(SoundCatalog.first().Id(), "");
    }

    private static ScheduledAction named(ServerLevel World, ComputerBlockEntity Computer, ScheduledAction Requested) {
        ComputerGrid.Reach Reach = ComputerGrid.reach(World, Computer.getBlockPos());
        return new ScheduledAction(Requested.Id(), Requested.Title(), renamed(Reach, Requested.OnSteps()), renamed(Reach, Requested.OffSteps()), Requested.Triggers());
    }

    private static List<ActionStep> renamed(ComputerGrid.Reach Reach, List<ActionStep> Steps) {
        List<ActionStep> Fixed = new ArrayList<>();
        for (ActionStep Step : Steps) {
            if (Step.Type() == ActionStep.Kind.Message && ChatChannels.databaseId(Step.Target()) != null) {
                MessageDatabaseBlockEntity Database = Reach.database(Step.Target());
                if (Database != null) {
                    Step = new ActionStep(Step.Type(), Step.Text(), Step.Target(), Database.getChannelName(), Step.Port(), Step.Timed(), Step.Seconds(), Step.SoundId(), Step.Start());
                }
            }
            Fixed.add(Step);
        }
        return Fixed;
    }

    public static void forget(UUID Id) {
        LastChat.remove(Id);
    }
}
