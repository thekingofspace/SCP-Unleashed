package net.rai.scp_unleashed.scp4666.christmas;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp4666.Scp4666Registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp4666Yuletide {
    public static final int CheckEvery = 100;
    public static final int[] JingleBeats = {0, 3, 5, 9, 12, 14, 18, 21, 23, 27};
    public static final float JingleVolume = 0.35f;
    public static final String Prefix = "commands.scp_unleashed.scp4666.christmas.";
    private static final Map<MinecraftServer, List<Cue>> Queues = new ConcurrentHashMap<>();

    private record Cue(ServerLevel World, long Due, Vec3 Spot, float Pitch, boolean Rattle) {
    }

    private Scp4666Yuletide() {
    }

    public static void jingle(ServerLevel World, Vec3 Spot) {
        long Now = World.getServer().getTickCount();
        List<Cue> Queue = Queues.computeIfAbsent(World.getServer(), Key -> new ArrayList<>());
        for (int Index = 0; Index < JingleBeats.length; Index++) {
            float Pitch = 0.92f + World.getRandom().nextFloat() * 0.16f;
            Queue.add(new Cue(World, Now + JingleBeats[Index], Spot, Pitch, Index % 2 == 0));
        }
    }

    public static int pending(MinecraftServer Server) {
        List<Cue> Queue = Queues.get(Server);
        return Queue == null ? 0 : Queue.size();
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post Event) {
        List<Cue> Queue = Queues.get(Event.getServer());
        if (Queue == null || Queue.isEmpty()) {
            return;
        }
        long Now = Event.getServer().getTickCount();
        Iterator<Cue> Walk = Queue.iterator();
        while (Walk.hasNext()) {
            Cue Next = Walk.next();
            if (Next.Due() > Now) {
                continue;
            }
            Walk.remove();
            Next.World().playSound(null, Next.Spot().x, Next.Spot().y, Next.Spot().z, Scp4666Registry.SleighBells.get(), SoundSource.AMBIENT, JingleVolume, Next.Pitch());
            if (Next.Rattle()) {
                Next.World().playSound(null, Next.Spot().x, Next.Spot().y, Next.Spot().z, Scp4666Registry.SleighRattle.get(), SoundSource.AMBIENT, JingleVolume * 0.8f, Next.Pitch());
            }
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent Event) {
        Queues.remove(Event.getServer());
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post Event) {
        if (!(Event.getEntity() instanceof ServerPlayer User) || User.tickCount % CheckEvery != 0) {
            return;
        }
        Scp4666Visit Visit = Scp4666Visit.of(User);
        Scp4666Sightings.noticeRead(User, Visit);
        Scp4666NoteEvent.tick(User, Visit);
        if (User.tickCount % Scp4666Sightings.CheckInterval == 0) {
            Scp4666Sightings.roll(User, Visit);
        }
    }

    @SubscribeEvent
    public static void placed(BlockEvent.EntityPlaceEvent Event) {
        if (!(Event.getEntity() instanceof ServerPlayer User) || !(Event.getLevel() instanceof Level World) || World.dimension() != Level.OVERWORLD) {
            return;
        }
        if (Scp4666NoteEvent.lowerDoor(Event.getPlacedBlock())) {
            Scp4666Visit.of(User).rememberDoor(Event.getPos());
        }
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId).then(Commands.literal("scp4666").then(Commands.literal("christmas")
                .requires(Source -> Source.hasPermission(2))
                .then(Commands.literal("note").then(Commands.argument("targets", EntityArgument.players())
                        .executes(Context -> note(Context, EntityArgument.getPlayers(Context, "targets")))))
                .then(Commands.literal("sighting").then(Commands.argument("targets", EntityArgument.players())
                        .executes(Context -> sighting(Context, EntityArgument.getPlayers(Context, "targets"))))))));
    }

    private static int note(CommandContext<CommandSourceStack> Context, Collection<ServerPlayer> Targets) throws CommandSyntaxException {
        int Count = 0;
        for (ServerPlayer User : Targets) {
            Scp4666Visit Visit = Scp4666Visit.of(User);
            Optional<Scp4666NoteEvent.Choice> Picked = User.level().dimension() == Level.OVERWORLD ? Scp4666NoteEvent.choose(User.serverLevel(), User, Visit) : Optional.empty();
            if (Picked.isEmpty() || Scp4666NoteEvent.place(User, Visit, Picked.get()).isEmpty()) {
                Context.getSource().sendFailure(Component.translatable(Prefix + "no_door", User.getDisplayName()));
                continue;
            }
            Count++;
            Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "note", User.getDisplayName()), true);
        }
        return Count;
    }

    private static int sighting(CommandContext<CommandSourceStack> Context, Collection<ServerPlayer> Targets) throws CommandSyntaxException {
        int Count = 0;
        for (ServerPlayer User : Targets) {
            Scp4666Visit Visit = Scp4666Visit.of(User);
            if (User.level().dimension() != Level.OVERWORLD || Scp4666Sightings.show(User, Visit).isEmpty()) {
                Context.getSource().sendFailure(Component.translatable(Prefix + "no_spot", User.getDisplayName()));
                continue;
            }
            Count++;
            Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "sighting", User.getDisplayName()), true);
        }
        return Count;
    }
}
