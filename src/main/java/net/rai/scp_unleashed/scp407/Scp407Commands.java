package net.rai.scp_unleashed.scp407;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.speaker.SoundMessages;
import net.rai.scp_unleashed.speaker.SpeakerBlockEntity;
import net.rai.scp_unleashed.wire.WireMessage;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class Scp407Commands {
    public static final double Reach = 48.0;
    public static final int SpeakerSearch = 12;
    public static final String Prefix = "commands.scp_unleashed.scp407.";

    private Scp407Commands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> Dispatcher) {
        Dispatcher.register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp407")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("act1").executes(Context -> act(Context.getSource(), Scp407Session.ActGenesis)))
                        .then(Commands.literal("act2").executes(Context -> act(Context.getSource(), Scp407Session.ActWanderer)))
                        .then(Commands.literal("stop").executes(Context -> stop(Context.getSource())))
                        .then(Commands.literal("wither").executes(Context -> wither(Context.getSource())))
                        .then(Commands.literal("status").executes(Context -> status(Context.getSource())))));
    }

    @Nullable
    public static SpeakerBlockEntity nearestSpeaker(ServerLevel World, Vec3 At) {
        BlockPos Centre = BlockPos.containing(At);
        SpeakerBlockEntity Best = null;
        double BestDistance = Double.MAX_VALUE;
        for (BlockPos Pos : BlockPos.betweenClosed(Centre.offset(-SpeakerSearch, -SpeakerSearch, -SpeakerSearch), Centre.offset(SpeakerSearch, SpeakerSearch, SpeakerSearch))) {
            if (World.isLoaded(Pos) && World.getBlockEntity(Pos) instanceof SpeakerBlockEntity Speaker) {
                double Distance = Pos.distToCenterSqr(At);
                if (Distance < BestDistance) {
                    Best = Speaker;
                    BestDistance = Distance;
                }
            }
        }
        return Best;
    }

    public static void play(ServerLevel World, SpeakerBlockEntity Speaker, boolean Loop) {
        Speaker.receiveMessage(new WireMessage(Speaker.getChannel(), SoundMessages.payload(Scp407Registry.SongId, SoundMessages.DefaultVolume, SoundMessages.DefaultPitch, Loop),
                Speaker.getBlockPos()));
    }

    public static void silence(ServerLevel World, Scp407Session Session) {
        for (BlockPos Pos : List.copyOf(Session.Tracks.keySet())) {
            if (World.isLoaded(Pos) && World.getBlockEntity(Pos) instanceof SpeakerBlockEntity Speaker) {
                Speaker.receiveMessage(new WireMessage(Speaker.getChannel(), SoundMessages.stopPayload(), Pos));
            }
        }
    }

    private static int act(CommandSourceStack Source, byte Act) {
        ServerLevel World = Source.getLevel();
        Scp407Sessions Data = Scp407Sessions.get(World);
        Scp407Session Session = Data.nearest(Source.getPosition(), Reach);
        if (Session == null || !Session.isPlaying()) {
            SpeakerBlockEntity Speaker = nearestSpeaker(World, Source.getPosition());
            if (Speaker == null) {
                Source.sendFailure(Component.translatable(Prefix + "no_speaker"));
                return 0;
            }
            play(World, Speaker, false);
            Session = Data.withSpeaker(Speaker.getBlockPos());
            if (Session == null) {
                Source.sendFailure(Component.translatable(Prefix + "no_speaker"));
                return 0;
            }
        }
        if (Session.Listeners.isEmpty()) {
            for (BlockPos Pos : Session.Tracks.keySet()) {
                if (World.getBlockEntity(Pos) instanceof SpeakerBlockEntity Speaker) {
                    Scp407Hearing.scan(World, Session, Speaker.mouth(), Pos, Session.position(World.getGameTime()));
                }
            }
        }
        Data.forceAct(World, Session, Act);
        int Id = Session.Id;
        Source.sendSuccess(() -> Component.translatable(Prefix + (Act == Scp407Session.ActGenesis ? "act1" : "act2"), Id), true);
        return 1;
    }

    private static int stop(CommandSourceStack Source) {
        ServerLevel World = Source.getLevel();
        Scp407Sessions Data = Scp407Sessions.get(World);
        Scp407Session Session = Data.nearest(Source.getPosition(), Reach);
        if (Session == null) {
            Source.sendFailure(Component.translatable(Prefix + "none"));
            return 0;
        }
        Data.remove(Session);
        silence(World, Session);
        Scp407Ending.clear(World, Session);
        int Id = Session.Id;
        Source.sendSuccess(() -> Component.translatable(Prefix + "stopped", Id), true);
        return 1;
    }

    private static int wither(CommandSourceStack Source) {
        ServerLevel World = Source.getLevel();
        Scp407Sessions Data = Scp407Sessions.get(World);
        Scp407Session Session = Data.nearest(Source.getPosition(), Reach);
        if (Session == null || !Session.isPlaying()) {
            Source.sendFailure(Component.translatable(Prefix + "none"));
            return 0;
        }
        Scp407Ending.begin(World, Session);
        silence(World, Session);
        Data.setDirty();
        int Id = Session.Id;
        Source.sendSuccess(() -> Component.translatable(Prefix + "withered", Id), true);
        return 1;
    }

    private static String clock(long Ticks) {
        long Seconds = Math.max(0L, Ticks) / 20L;
        return String.format("%d:%02d", Seconds / 60L, Seconds % 60L);
    }

    private static int status(CommandSourceStack Source) {
        ServerLevel World = Source.getLevel();
        Scp407Sessions Data = Scp407Sessions.get(World);
        if (Data.all().isEmpty()) {
            Source.sendSuccess(() -> Component.translatable(Prefix + "idle"), false);
            return 0;
        }
        long Now = World.getGameTime();
        for (Scp407Session Session : Data.all()) {
            String Act = switch (Session.Act) {
                case Scp407Session.ActGenesis -> "act1";
                case Scp407Session.ActWanderer -> "act2";
                default -> "pending";
            };
            String State = Session.isPlaying() ? "playing" : "withering";
            Component Line = Component.translatable(Prefix + "status", Session.Id, Session.Origin.toShortString(), Component.translatable(Prefix + "state." + State),
                    Component.translatable(Prefix + "phase." + Act), clock(Session.position(Now)), clock(Scp407Song.Length), Session.Listeners.size(), Session.fusedCount(),
                    Session.Grown.size(), Session.Region.size(), Session.Creatures.size(), Session.Tracks.size()).withStyle(ChatFormatting.GREEN);
            Source.sendSuccess(() -> Line, false);
        }
        int Pending = Scp407Doom.get(World.getServer()).size();
        if (Pending > 0) {
            Source.sendSuccess(() -> Component.translatable(Prefix + "pending", Pending).withStyle(ChatFormatting.GRAY), false);
        }
        return Data.all().size();
    }
}
