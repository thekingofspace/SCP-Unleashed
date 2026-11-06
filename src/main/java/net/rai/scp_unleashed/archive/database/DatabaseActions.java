package net.rai.scp_unleashed.archive.database;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.archive.ArchiveRegistry;
import net.rai.scp_unleashed.archive.ArchiveRules;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.DocumentClaims;
import net.rai.scp_unleashed.archive.document.DocumentStamp;
import net.rai.scp_unleashed.archive.document.SpawnRule;
import net.rai.scp_unleashed.archive.network.DatabaseSyncPayload;
import net.rai.scp_unleashed.archive.network.OpenDatabasePayload;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class DatabaseActions {
    public static final double Reach = 8.0;
    public static final int PrintInterval = 5;
    private static final Map<UUID, Long> LastPrint = new HashMap<>();

    private DatabaseActions() {
    }

    public static boolean isFolder(ItemStack Stack) {
        return Stack.is(ArchiveRegistry.SealedDocument.get()) || Stack.is(ArchiveRegistry.OpenedDocument.get()) || Stack.is(ArchiveRegistry.DatabaseCopy.get());
    }

    public static boolean isUnit(BlockState State) {
        return State.getBlock() instanceof DatabaseBlock || State.getBlock() instanceof DatabaseTerminalBlock;
    }

    public static void open(ServerPlayer User, BlockPos Pos, boolean Main) {
        sync(User);
        PacketDistributor.sendToPlayer(User, new OpenDatabasePayload(Pos, Main));
        play(User.serverLevel(), Pos, ArchiveRegistry.DatabaseOpen, 0.5f, 1.0f);
    }

    public static void sync(ServerPlayer User) {
        PacketDistributor.sendToPlayer(User, new DatabaseSyncPayload(DatabaseArchive.get(User.server).records()));
    }

    public static void broadcast(MinecraftServer Server) {
        PacketDistributor.sendToAllPlayers(new DatabaseSyncPayload(DatabaseArchive.get(Server).records()));
    }

    public static void forget(UUID Id) {
        LastPrint.remove(Id);
    }

    private static void play(ServerLevel World, BlockPos Pos, DeferredHolder<SoundEvent, SoundEvent> Sound, float Volume, float Pitch) {
        World.playSound(null, Pos, Sound.get(), SoundSource.BLOCKS, Volume, Pitch);
    }

    private static void tell(ServerPlayer User, String Key, ChatFormatting Colour, Object... Arguments) {
        User.displayClientMessage(Component.translatable(Key, Arguments).withStyle(Colour), true);
    }

    private static Component designation(ResourceLocation Id) {
        AnomalyDocument Document = AnomalyDocuments.server().get(Id);
        return Document == null ? Component.literal(Id.toString()) : Document.designation();
    }

    public static void useFolder(ServerPlayer User, InteractionHand Hand, BlockPos Pos, boolean Main) {
        ItemStack Stack = User.getItemInHand(Hand);
        ServerLevel World = User.serverLevel();
        boolean Shred = User.isSecondaryUseActive();
        if (Stack.is(ArchiveRegistry.DatabaseCopy.get())) {
            Stack.shrink(1);
            play(World, Pos, ArchiveRegistry.DatabasePaper, 0.7f, 1.15f);
            tell(User, "archive.scp_unleashed.database.returned", ChatFormatting.AQUA);
            return;
        }
        if (!Main) {
            deny(World, Pos);
            tell(User, "archive.scp_unleashed.database.terminal_only", ChatFormatting.GRAY);
            return;
        }
        DocumentStamp Stamp = Stack.get(ArchiveRegistry.StampComponent.get());
        if (Stack.is(ArchiveRegistry.SealedDocument.get())) {
            UUID Claimant = Stack.get(ArchiveRegistry.ClaimantComponent.get());
            boolean Released = Stamp != null && releaseClaim(World.getServer(), Stamp.Document(), Claimant != null ? Claimant : User.getUUID());
            Stack.shrink(1);
            shredSound(World, Pos);
            tell(User, Released ? "archive.scp_unleashed.database.shredded_sealed_pool" : "archive.scp_unleashed.database.shredded_sealed", ChatFormatting.GRAY);
            return;
        }
        if (Stamp == null) {
            Stack.shrink(1);
            shredSound(World, Pos);
            tell(User, "archive.scp_unleashed.database.shredded_blank", ChatFormatting.GRAY);
            return;
        }
        DatabaseArchive Archive = DatabaseArchive.get(World.getServer());
        boolean Archived = Archive.contains(Stamp.Document());
        Component Designation = designation(Stamp.Document());
        if (!Shred && !Archived) {
            if (upload(User, Pos, Stack, Stamp, Archive)) {
                tell(User, "archive.scp_unleashed.database.archived", ChatFormatting.GREEN, Designation);
            }
            return;
        }
        if (!Archived) {
            if (AnomalyDocuments.server().get(Stamp.Document()) == null) {
                Stack.shrink(1);
                shredSound(World, Pos);
                tell(User, "archive.scp_unleashed.database.shredded_blank", ChatFormatting.GRAY);
                return;
            }
            if (!upload(User, Pos, Stack, Stamp, Archive)) {
                return;
            }
            Stack.shrink(1);
            shredSound(World, Pos);
            tell(User, "archive.scp_unleashed.database.filed_shredded", ChatFormatting.GREEN, Designation);
            return;
        }
        Stack.shrink(1);
        shredSound(World, Pos);
        tell(User, "archive.scp_unleashed.database.shredded", ChatFormatting.GRAY, Designation);
    }

    private static boolean releaseClaim(MinecraftServer Server, ResourceLocation Id, UUID Owner) {
        AnomalyDocument Document = AnomalyDocuments.server().get(Id);
        return Document != null && Document.Spawn() == SpawnRule.Once && DocumentClaims.get(Server).giveBack(Id, ArchiveRules.mode(Server), Owner);
    }

    private static boolean upload(ServerPlayer User, BlockPos Pos, ItemStack Stack, DocumentStamp Stamp, DatabaseArchive Archive) {
        ServerLevel World = User.serverLevel();
        if (AnomalyDocuments.server().get(Stamp.Document()) == null) {
            deny(World, Pos);
            tell(User, "archive.scp_unleashed.database.unknown", ChatFormatting.RED);
            return false;
        }
        long Day = World.getServer().overworld().getDayTime() / 24000L + 1L;
        DatabaseRecord Entry = new DatabaseRecord(Stamp.Document(), Stamp.Classification(), DatabaseRecord.snapshot(Stack.get(ArchiveRegistry.NotesComponent.get())),
                User.getGameProfile().getName(), Day);
        if (!Archive.add(Entry)) {
            return false;
        }
        play(World, Pos, ArchiveRegistry.DatabaseUpload, 0.8f, 1.0f);
        play(World, Pos, ArchiveRegistry.DatabaseWhir, 0.55f, 1.0f);
        broadcast(World.getServer());
        return true;
    }

    private static void shredSound(ServerLevel World, BlockPos Pos) {
        play(World, Pos, ArchiveRegistry.DatabaseShred, 0.8f, 1.0f);
        play(World, Pos, ArchiveRegistry.DatabasePaper, 0.5f, 0.7f);
    }

    private static void deny(ServerLevel World, BlockPos Pos) {
        play(World, Pos, ArchiveRegistry.DatabaseDeny, 0.6f, 1.0f);
    }

    public static void printCopy(ServerPlayer User, BlockPos Pos, ResourceLocation Document) {
        ServerLevel World = User.serverLevel();
        if (!World.isLoaded(Pos) || !isUnit(World.getBlockState(Pos)) || User.distanceToSqr(Vec3.atCenterOf(Pos)) > Reach * Reach) {
            return;
        }
        long Now = World.getGameTime();
        Long Last = LastPrint.get(User.getUUID());
        if (Last != null && Now - Last < PrintInterval && Now >= Last) {
            return;
        }
        Optional<DatabaseRecord> Entry = DatabaseArchive.get(World.getServer()).find(Document);
        if (Entry.isEmpty()) {
            deny(World, Pos);
            sync(User);
            return;
        }
        LastPrint.put(User.getUUID(), Now);
        ItemStack Copy = DatabaseCopyItem.create(Entry.get());
        if (!User.getInventory().add(Copy)) {
            User.drop(Copy, false);
        }
        play(World, Pos, ArchiveRegistry.DatabasePrint, 0.7f, 1.0f);
        play(World, Pos, ArchiveRegistry.DatabasePaper, 0.6f, 1.25f);
    }
}
