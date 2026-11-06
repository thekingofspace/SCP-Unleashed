package net.rai.scp_unleashed.scp5001.event;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp5001.core.Scp5001State;

import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

public final class Scp5001EventCommands {
    private static final String Prefix = "commands.scp_unleashed.scp5001_event.";

    private Scp5001EventCommands() {
    }

    public static void register(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp5001")
                        .then(Commands.literal("event")
                                .requires(Source -> Source.hasPermission(2))
                                .then(Commands.literal("status").executes(Scp5001EventCommands::status))
                                .then(Commands.literal("arena").executes(Context -> arena(Context, 12))
                                        .then(Commands.argument("radius", IntegerArgumentType.integer(6, 24)).executes(Context -> arena(Context, IntegerArgumentType.getInteger(Context, "radius")))))
                                .then(Commands.literal("unlock").executes(Scp5001EventCommands::unlock))
                                .then(Commands.literal("phase").then(Commands.argument("phase", StringArgumentType.word())
                                        .suggests((Context, Builder) -> SharedSuggestionProvider.suggest(Arrays.stream(Scp5001State.Phase.values()).map(Each -> Each.name().toLowerCase(Locale.ROOT)), Builder))
                                        .executes(Scp5001EventCommands::phase)))
                                .then(Commands.literal("lift").then(Commands.argument("value", FloatArgumentType.floatArg(0.0f, 1.0f)).executes(Scp5001EventCommands::lift)))
                                .then(Commands.literal("overdrive").executes(Scp5001EventCommands::overdrive))
                                .then(Commands.literal("purge").executes(Scp5001EventCommands::purge))
                                .then(Commands.literal("reset").executes(Scp5001EventCommands::reset))
                                .then(Commands.literal("burst").executes(Scp5001EventCommands::burst))
                                .then(Commands.literal("clear").executes(Scp5001EventCommands::clear))
                                .then(Commands.literal("exposure").then(Commands.argument("targets", EntityArgument.entities())
                                        .then(Commands.argument("value", IntegerArgumentType.integer(0)).executes(Scp5001EventCommands::exposure)))))));
    }

    private static ServerLevel overworld(CommandContext<CommandSourceStack> Context) {
        return Context.getSource().getServer().overworld();
    }

    private static int status(CommandContext<CommandSourceStack> Context) {
        ServerLevel World = overworld(Context);
        Scp5001State State = Scp5001State.get(World.getServer());
        Scp5001EventData Data = Scp5001EventData.get(World.getServer());
        long Now = World.getGameTime();
        String Line = String.format(Locale.ROOT, "phase %s, lift %.3f, overdriven %d/%d, rods %d%s, lasers %d, swarm wave %d alive %d, cooldown %ds, core %s",
                State.phase().name(), State.lift(), State.overdriven(), Scp5001Event.generators(World, State, Data).size(), Scp5001Event.rods(World, State, Data).size(),
                Data.rodsSpent() ? " spent" : "", Scp5001Event.lasers(World, State, Data).size(), Data.swarmWave(), Data.swarm().size(),
                Data.cooling(Now) ? (Data.cooldownEnd() - Now) / 20 : 0, State.located() ? State.core().toShortString() : "none");
        Context.getSource().sendSuccess(() -> Component.literal(Line), false);
        return 1;
    }

    private static int arena(CommandContext<CommandSourceStack> Context, int Radius) {
        ServerLevel World = overworld(Context);
        if (Context.getSource().getLevel() != World) {
            Context.getSource().sendFailure(Component.translatable(Prefix + "overworld"));
            return 0;
        }
        BlockPos Origin = BlockPos.containing(Context.getSource().getPosition());
        Scp5001Arena.build(World, Origin, Radius);
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "arena", Origin.toShortString()), true);
        return 1;
    }

    private static int unlock(CommandContext<CommandSourceStack> Context) {
        ServerLevel World = overworld(Context);
        Scp5001EventData.get(World.getServer()).cooldownEnd(0);
        boolean Done = Scp5001Event.unlock(World, World.getGameTime());
        return report(Context, Done);
    }

    private static int phase(CommandContext<CommandSourceStack> Context) {
        ServerLevel World = overworld(Context);
        String Name = StringArgumentType.getString(Context, "phase");
        for (Scp5001State.Phase Each : Scp5001State.Phase.values()) {
            if (Each.name().equalsIgnoreCase(Name)) {
                Scp5001State.get(World.getServer()).phase(Each, World.getGameTime());
                return report(Context, true);
            }
        }
        Context.getSource().sendFailure(Component.translatable(Prefix + "unknown_phase", Name));
        return 0;
    }

    private static int lift(CommandContext<CommandSourceStack> Context) {
        ServerLevel World = overworld(Context);
        Scp5001State.get(World.getServer()).lift(FloatArgumentType.getFloat(Context, "value"));
        return report(Context, true);
    }

    private static int overdrive(CommandContext<CommandSourceStack> Context) {
        ServerLevel World = overworld(Context);
        Scp5001State State = Scp5001State.get(World.getServer());
        Scp5001EventData Data = Scp5001EventData.get(World.getServer());
        boolean Any = false;
        for (BlockPos Each : Scp5001Event.generators(World, State, Data)) {
            Any |= Scp5001Event.overdriveGenerator(World, Each, World.getGameTime());
        }
        return report(Context, Any);
    }

    private static int purge(CommandContext<CommandSourceStack> Context) {
        ServerLevel World = overworld(Context);
        Scp5001EventData Data = Scp5001EventData.get(World.getServer());
        for (UUID Id : Data.swarm().toArray(UUID[]::new)) {
            Entity Found = World.getEntity(Id);
            if (Found != null) {
                Found.kill();
            }
            Data.untrack(Id);
        }
        Data.swarmWave(Scp5001EventConfig.swarmWaves());
        return report(Context, true);
    }

    private static int reset(CommandContext<CommandSourceStack> Context) {
        ServerLevel World = overworld(Context);
        return report(Context, Scp5001Event.forceReset(World, World.getGameTime()));
    }

    private static int burst(CommandContext<CommandSourceStack> Context) {
        ServerLevel World = overworld(Context);
        Scp5001Event.burst(World, Scp5001State.get(World.getServer()), Scp5001EventData.get(World.getServer()), World.getGameTime());
        return report(Context, true);
    }

    private static int clear(CommandContext<CommandSourceStack> Context) {
        ServerLevel World = overworld(Context);
        clearAll(World);
        return report(Context, true);
    }

    public static void clearAll(ServerLevel World) {
        Scp5001State State = Scp5001State.get(World.getServer());
        Scp5001EventData Data = Scp5001EventData.get(World.getServer());
        State.phase(Scp5001State.Phase.Dormant, World.getGameTime());
        State.lift(0.0f);
        State.overdriven(0);
        Data.lastSeen(Scp5001State.Phase.Dormant);
        Data.cooldownEnd(0);
        Data.rodsSpent(false);
        Data.clearOverdrive();
        Data.clearSwarm();
        Data.abandoned(0);
        Scp5001Event.forgetCache();
        Scp5001Event.broadcast(World);
    }

    private static int exposure(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        int Value = IntegerArgumentType.getInteger(Context, "value");
        int Count = 0;
        for (Entity Each : EntityArgument.getEntities(Context, "targets")) {
            if (Each instanceof LivingEntity) {
                Scp5001Exposure.set(Each, Value);
                Count++;
            }
        }
        int Done = Count;
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "exposure", Done, Value), true);
        return Count;
    }

    private static int report(CommandContext<CommandSourceStack> Context, boolean Done) {
        if (!Done) {
            Context.getSource().sendFailure(Component.translatable(Prefix + "refused"));
            return 0;
        }
        return status(Context);
    }
}
