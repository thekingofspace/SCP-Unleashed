package net.rai.scp_unleashed.z0003;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.Collection;

public final class Z0003Commands {
    public static final String Prefix = "commands.scp_unleashed.z0003.";

    private Z0003Commands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> Dispatcher) {
        Dispatcher.register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("z0003")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("arm")
                                .then(Commands.argument("radius", FloatArgumentType.floatArg(Z0003Phrase.MinRadius, Z0003Phrase.MaxRadius))
                                        .then(Commands.argument("seconds", FloatArgumentType.floatArg(0.0f, Z0003Phrase.MaxDelaySeconds))
                                                .executes(Z0003Commands::arm))))
                        .then(Commands.literal("petrify")
                                .then(Commands.argument("targets", EntityArgument.entities()).executes(Context -> petrify(Context, EntityArgument.getEntities(Context, "targets")))))
                        .then(Commands.literal("free")
                                .then(Commands.argument("targets", EntityArgument.entities()).executes(Context -> free(Context, EntityArgument.getEntities(Context, "targets")))))));
    }

    private static int arm(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        ServerPlayer User = Context.getSource().getPlayerOrException();
        Z0003Phrase.Setting Setting = Z0003Phrase.clamp(FloatArgumentType.getFloat(Context, "radius"), FloatArgumentType.getFloat(Context, "seconds"));
        int Count = Z0003Voice.apply(User, Setting);
        if (Count == 0) {
            Context.getSource().sendFailure(Component.translatable(Prefix + "arm.none"));
            return 0;
        }
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "arm", Count, Z0003Voice.number(Setting.Radius()), Z0003Voice.number(Setting.seconds())), true);
        return Count;
    }

    private static int petrify(CommandContext<CommandSourceStack> Context, Collection<? extends Entity> Targets) {
        int Count = 0;
        for (Entity Target : Targets) {
            if (Target instanceof LivingEntity Living && Z0003Petrify.petrify(Living, true)) {
                Count++;
            }
        }
        int Total = Count;
        if (Total == 0) {
            Context.getSource().sendFailure(Component.translatable(Prefix + "petrify.none"));
            return 0;
        }
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "petrify", Total), true);
        return Total;
    }

    private static int free(CommandContext<CommandSourceStack> Context, Collection<? extends Entity> Targets) {
        int Count = 0;
        for (Entity Target : Targets) {
            if (Target instanceof LivingEntity Living && Z0003Petrify.free(Living)) {
                Count++;
            }
        }
        int Total = Count;
        if (Total == 0) {
            Context.getSource().sendFailure(Component.translatable(Prefix + "free.none"));
            return 0;
        }
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "free", Total), true);
        return Total;
    }
}
