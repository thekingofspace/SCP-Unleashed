package net.rai.scp_unleashed.z0001;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.List;

public final class Z0001Commands {
    public static final String Prefix = "commands.scp_unleashed.z0001.";

    private Z0001Commands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> Dispatcher) {
        Dispatcher.register(Commands.literal("scp_unleashed")
                .then(Commands.literal("z0001")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("status").executes(Z0001Commands::status))
                        .then(Commands.literal("reset").executes(Context -> reset(Context, List.of(Context.getSource().getPlayerOrException())))
                                .then(Commands.argument("targets", EntityArgument.players()).executes(Context -> reset(Context, EntityArgument.getPlayers(Context, "targets")))))
                        .then(Commands.literal("spare")
                                .then(Commands.argument("targets", EntityArgument.players()).executes(Context -> spare(Context, EntityArgument.getPlayers(Context, "targets")))))));
    }

    private static int status(CommandContext<CommandSourceStack> Context) {
        CommandSourceStack Source = Context.getSource();
        MinecraftServer Server = Source.getServer();
        long Today = Z0001Ink.today(Server);
        int Days = Z0001Registry.cooldownDays(Server.getGameRules());
        Source.sendSuccess(() -> Component.translatable(Prefix + "status.day", Today, Days), false);
        List<Z0001Ink.Pending> Waiting = Z0001Ink.allPending();
        if (Waiting.isEmpty()) {
            Source.sendSuccess(() -> Component.translatable(Prefix + "status.none"), false);
        }
        int Now = Server.getTickCount();
        for (Z0001Ink.Pending Entry : Waiting) {
            int Left = Math.max(0, Entry.Due() - Now);
            Source.sendSuccess(() -> Component.translatable(Prefix + "status.pending", Entry.TargetName(), Entry.WriterName(), Left), false);
        }
        if (Source.getEntity() instanceof ServerPlayer User) {
            ItemStack Held = Z0001BookItem.is(User.getMainHandItem()) ? User.getMainHandItem() : User.getOffhandItem();
            Z0001Page Page = Z0001BookItem.is(Held) ? Held.get(Z0001Registry.Page.get()) : null;
            if (Page != null) {
                Source.sendSuccess(() -> Component.translatable(Prefix + (Page.isSpent(Today) ? "status.spent" : "status.fresh"), Page.Name(), Page.Day(), Page.Until()), false);
            }
        }
        return Waiting.size();
    }

    private static int reset(CommandContext<CommandSourceStack> Context, Collection<ServerPlayer> Targets) {
        int Cleared = 0;
        for (ServerPlayer Target : Targets) {
            Inventory Pack = Target.getInventory();
            for (int Slot = 0; Slot < Pack.getContainerSize(); Slot++) {
                ItemStack Stack = Pack.getItem(Slot);
                if (Z0001BookItem.is(Stack) && Stack.has(Z0001Registry.Page.get())) {
                    Stack.remove(Z0001Registry.Page.get());
                    Cleared++;
                }
            }
        }
        int Total = Cleared;
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "reset", Total), true);
        return Total;
    }

    private static int spare(CommandContext<CommandSourceStack> Context, Collection<ServerPlayer> Targets) {
        int Spared = 0;
        for (ServerPlayer Target : Targets) {
            if (Z0001Ink.forget(Target.getUUID())) {
                Spared++;
                Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "spared", Target.getDisplayName()), true);
            }
        }
        if (Spared == 0) {
            Context.getSource().sendFailure(Component.translatable(Prefix + "spare.none"));
        }
        return Spared;
    }
}
