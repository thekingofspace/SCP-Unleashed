package net.rai.scp_unleashed.scp465;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.List;

public final class Scp465Commands {
    public static final String Prefix = "commands.scp_unleashed.scp465.";

    private Scp465Commands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> Dispatcher) {
        Dispatcher.register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp465")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("list")
                                .executes(Context -> list(Context.getSource())))
                        .then(Commands.literal("close")
                                .executes(Context -> closeHere(Context.getSource()))
                                .then(Commands.literal("all")
                                        .executes(Context -> closeAll(Context.getSource()))))));
    }

    private static int list(CommandSourceStack Source) {
        ServerLevel World = Source.getLevel();
        Scp465Parties Data = Scp465Parties.peek(World);
        if (Data == null || Data.isEmpty()) {
            Source.sendSuccess(() -> Component.translatable(Prefix + "none"), false);
            return 0;
        }
        for (Scp465Party Party : Data.all()) {
            BlockPos Box = Party.boxPos();
            Source.sendSuccess(() -> Component.translatable(Prefix + "entry", Party.id(), Box.getX() + " " + Box.getY() + " " + Box.getZ(),
                    Party.celebrants().size(), Party.guests().size()), false);
        }
        return Data.all().size();
    }

    private static int closeHere(CommandSourceStack Source) {
        ServerLevel World = Source.getLevel();
        Scp465Party Party = Scp465Parties.containing(World, BlockPos.containing(Source.getPosition()));
        if (Party == null) {
            Source.sendFailure(Component.translatable(Prefix + "none"));
            return 0;
        }
        Scp465Celebration.close(World, Party, Source.getEntity());
        Source.sendSuccess(() -> Component.translatable(Prefix + "closed", 1), true);
        return 1;
    }

    private static int closeAll(CommandSourceStack Source) {
        ServerLevel World = Source.getLevel();
        Scp465Parties Data = Scp465Parties.peek(World);
        if (Data == null || Data.isEmpty()) {
            Source.sendFailure(Component.translatable(Prefix + "none"));
            return 0;
        }
        List<Scp465Party> Parties = Data.snapshotList();
        for (Scp465Party Party : Parties) {
            Scp465Celebration.close(World, Party, Source.getEntity());
        }
        Source.sendSuccess(() -> Component.translatable(Prefix + "closed", Parties.size()), true);
        return Parties.size();
    }
}
