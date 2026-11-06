package net.rai.scp_unleashed.scp354;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp354Commands {
    public static final String Prefix = "commands.scp_unleashed.scp354.";
    public static final double EmergeRange = 160.0;

    private Scp354Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        CommandBuildContext Context = Event.getBuildContext();
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp354")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("pool").executes(Scp354Commands::pool))
                        .then(Commands.literal("emerge")
                                .executes(Run -> emerge(Run, null))
                                .then(Commands.argument("type", ResourceArgument.resource(Context, Registries.ENTITY_TYPE))
                                        .executes(Run -> emerge(Run, ResourceArgument.getSummonableEntityType(Run, "type")))))
                        .then(Commands.literal("status").executes(Scp354Commands::status))));
    }

    private static int pool(CommandContext<CommandSourceStack> Run) {
        CommandSourceStack Source = Run.getSource();
        ServerLevel World = Source.getLevel();
        BlockPos At = BlockPos.containing(Source.getPosition());
        int Ground = Scp354Sites.ground(World, At.getX(), At.getZ());
        BlockPos Site = new BlockPos(At.getX(), Ground, At.getZ());
        Scp354Pools.Pool Made = Scp354Pools.create(World, Site);
        if (Made == null) {
            Source.sendFailure(Component.translatable(Prefix + "pool.failed"));
            return 0;
        }
        BlockPos Centre = Made.Centre;
        Source.sendSuccess(() -> Component.translatable(Prefix + "pool.success", Centre.getX(), Centre.getY(), Centre.getZ()), true);
        return 1;
    }

    private static int emerge(CommandContext<CommandSourceStack> Run, @Nullable Holder.Reference<EntityType<?>> Wanted) {
        CommandSourceStack Source = Run.getSource();
        ServerLevel World = Source.getLevel();
        Optional<Scp354Pools.Pool> Near = Scp354Pools.get(World.getServer()).nearest(World.dimension(), BlockPos.containing(Source.getPosition()), EmergeRange);
        if (Near.isEmpty()) {
            Source.sendFailure(Component.translatable(Prefix + "none"));
            return 0;
        }
        Optional<Entity> Made = Scp354Emergence.emerge(World, Near.get(), Wanted == null ? null : Wanted.value());
        if (Made.isEmpty()) {
            Source.sendFailure(Component.translatable(Prefix + "emerge.failed"));
            return 0;
        }
        Component Name = Made.get().getDisplayName();
        Source.sendSuccess(() -> Component.translatable(Prefix + "emerge.success", Name), true);
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> Run) {
        CommandSourceStack Source = Run.getSource();
        Scp354Pools Data = Scp354Pools.get(Source.getServer());
        if (Data.pools().isEmpty()) {
            Source.sendFailure(Component.translatable(Prefix + "none"));
            return 0;
        }
        for (Scp354Pools.Pool Entry : Data.pools()) {
            ServerLevel World = Source.getServer().getLevel(Entry.Dimension);
            boolean Loaded = World != null && World.isLoaded(Entry.Centre);
            int Alive = Loaded ? Scp354Emergence.alive(World, Entry) : -1;
            Component Line = Component.translatable(Prefix + "status.line", Entry.Centre.getX(), Entry.Centre.getY(), Entry.Centre.getZ(), Entry.Dimension.location().toString(),
                    Math.max(0, Entry.Timer) / 20, Loaded ? Component.translatable(Prefix + "status.loaded") : Component.translatable(Prefix + "status.unloaded"),
                    Alive < 0 ? "?" : String.valueOf(Alive), Scp354Emergence.Cap, Entry.Born);
            Source.sendSuccess(() -> Line, false);
        }
        return Data.pools().size();
    }
}
