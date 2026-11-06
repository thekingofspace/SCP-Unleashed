package net.rai.scp_unleashed.computer.network;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.computer.ComputerBlockEntity;
import net.rai.scp_unleashed.computer.ComputerRegistry;
import net.rai.scp_unleashed.computer.MessageDatabaseBlockEntity;
import net.rai.scp_unleashed.computer.client.ComputerClientHooks;
import net.rai.scp_unleashed.computer.net.ComputerSection;
import net.rai.scp_unleashed.computer.net.ComputerSessions;
import net.rai.scp_unleashed.computer.net.ComputerSync;
import net.rai.scp_unleashed.speaker.cassette.AnomalousLibrary;

public final class ComputerNetworking {
    private ComputerNetworking() {
    }

    public static void register(PayloadRegistrar Registrar) {
        Registrar.playToServer(ComputerActionPayload.PayloadType, ComputerActionPayload.PayloadCodec, (Payload, Context) -> {
            if (Context.player() instanceof ServerPlayer User) {
                ComputerActions.handle(User, Payload);
            }
        });
        Registrar.playToClient(ComputerOpenPayload.PayloadType, ComputerOpenPayload.PayloadCodec, (Payload, Context) -> ComputerClientHooks.open(Payload));
        Registrar.playToClient(ComputerSyncPayload.PayloadType, ComputerSyncPayload.PayloadCodec, (Payload, Context) -> ComputerClientHooks.sync(Payload));
        Registrar.playToClient(ComputerClosePayload.PayloadType, ComputerClosePayload.PayloadCodec, (Payload, Context) -> ComputerClientHooks.close(Payload));
        Registrar.playToClient(ComputerNoticePayload.PayloadType, ComputerNoticePayload.PayloadCodec, (Payload, Context) -> ComputerClientHooks.notice(Payload));
    }

    public static void openComputer(ServerPlayer User, ComputerBlockEntity Computer) {
        ComputerSessions.open(User, Computer);
        AnomalousLibrary.sync(User);
        PacketDistributor.sendToPlayer(User, new ComputerOpenPayload(Computer.getBlockPos(), ComputerOpenPayload.ComputerScreen, new CompoundTag()));
        ComputerSessions.Session Open = ComputerSessions.of(User, Computer.getBlockPos());
        if (Open != null) {
            push(User, Computer, Open, ComputerSection.System.bit());
        }
        User.serverLevel().playSound(null, Computer.getBlockPos(), ComputerRegistry.Key.get(), SoundSource.BLOCKS, 0.4f, 1.1f);
    }

    public static void openDatabase(ServerPlayer User, MessageDatabaseBlockEntity Database) {
        PacketDistributor.sendToPlayer(User, new ComputerOpenPayload(Database.getBlockPos(), ComputerOpenPayload.DatabaseScreen, databaseState(Database)));
    }

    public static CompoundTag databaseState(MessageDatabaseBlockEntity Database) {
        CompoundTag Data = new CompoundTag();
        Data.putString("Name", Database.getChannelName());
        Data.putInt("Count", Database.count());
        Data.putInt("Capacity", MessageDatabaseBlockEntity.Capacity);
        Data.putBoolean("Running", Database.isRunning());
        return Data;
    }

    public static void push(ServerPlayer User, ComputerBlockEntity Computer, ComputerSessions.Session Open, int Bits) {
        ServerLevel World = User.serverLevel();
        ComputerSection Current = ComputerSection.ofApp(Open.getApp());
        for (ComputerSection Section : ComputerSection.values()) {
            if ((Bits & Section.bit()) == 0 || Section != ComputerSection.System && Section != Current) {
                continue;
            }
            PacketDistributor.sendToPlayer(User, new ComputerSyncPayload(Computer.getBlockPos(), Section.ordinal(), ComputerSync.build(World, Computer, Open, Section)));
        }
    }

    public static void close(ServerPlayer User, BlockPos Pos, ComputerSessions.CloseReason Reason) {
        PacketDistributor.sendToPlayer(User, new ComputerClosePayload(Pos, Reason.ordinal()));
    }

    public static void notice(ServerPlayer User, BlockPos Pos, Component Text, int Tone) {
        PacketDistributor.sendToPlayer(User, new ComputerNoticePayload(Pos, Text, Tone));
    }

    public static void chime(ServerPlayer User, BlockPos Pos) {
        notice(User, Pos, Component.empty(), ComputerNoticePayload.Chime);
    }
}
