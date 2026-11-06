package net.rai.scp_unleashed.z0002;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class Z0002Commands {
    public static final String Prefix = "commands.scp_unleashed.z0002.";

    private Z0002Commands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> Dispatcher) {
        Dispatcher.register(Commands.literal("scp_unleashed")
                .then(Commands.literal("z0002")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("activate").executes(Context -> activate(Context, List.of(Context.getSource().getPlayerOrException()), false))
                                .then(Commands.literal("instant").executes(Context -> activate(Context, List.of(Context.getSource().getPlayerOrException()), true))
                                        .then(Commands.argument("targets", EntityArgument.players()).executes(Context -> activate(Context, EntityArgument.getPlayers(Context, "targets"), true))))
                                .then(Commands.argument("targets", EntityArgument.players()).executes(Context -> activate(Context, EntityArgument.getPlayers(Context, "targets"), false))))
                        .then(Commands.literal("collapse").executes(Z0002Commands::collapseOwn)
                                .then(Commands.literal("all").executes(Z0002Commands::collapseAll)))
                        .then(Commands.literal("status").executes(Z0002Commands::status))
                        .then(Commands.literal("wake").executes(Context -> wake(Context, List.of(Context.getSource().getPlayerOrException())))
                                .then(Commands.argument("targets", EntityArgument.players()).executes(Context -> wake(Context, EntityArgument.getPlayers(Context, "targets")))))));
    }

    private static int activate(CommandContext<CommandSourceStack> Context, Collection<ServerPlayer> Targets, boolean Instant) {
        int Opened = 0;
        for (ServerPlayer Target : Targets) {
            InteractionHand Hand = Z0002FingerItem.is(Target.getMainHandItem()) || !Z0002FingerItem.is(Target.getOffhandItem()) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            Z0002Domains.Outcome Result;
            if (Instant) {
                Result = Z0002Domains.open(Target, Target.getItemInHand(Hand), true, Z0002Domains.DomainTicks);
                Z0002Domain Made = Z0002Domains.ownedBy(Target.server, Target.getUUID());
                if (Result == Z0002Domains.Outcome.Opened && Made != null) {
                    Z0002Domains.finishBuilding(Target.server, Made);
                }
            } else {
                Result = Z0002Domains.begin(Target, Hand, true, Z0002Domains.DomainTicks);
            }
            if (Result == Z0002Domains.Outcome.Opened || Result == Z0002Domains.Outcome.Channelling) {
                Opened++;
                String Key = Result == Z0002Domains.Outcome.Opened ? "opened" : "channelled";
                Context.getSource().sendSuccess(() -> Component.translatable(Prefix + Key, Target.getDisplayName()), true);
            } else {
                Context.getSource().sendFailure(Component.translatable(Prefix + "refused", Target.getDisplayName(), Component.translatable(Prefix + "outcome." + Result.key())));
            }
        }
        return Opened;
    }

    private static int collapseOwn(CommandContext<CommandSourceStack> Context) {
        CommandSourceStack Source = Context.getSource();
        MinecraftServer Server = Source.getServer();
        Z0002Domain Found = null;
        if (Source.getEntity() instanceof ServerPlayer User) {
            Found = Z0002Domains.ownedBy(Server, User.getUUID());
            if (Found == null) {
                Found = Z0002Domains.memberOf(Server, User.getUUID());
            }
        }
        if (Found == null) {
            double Best = Double.MAX_VALUE;
            for (Z0002Domain Each : Z0002World.get(Server).active()) {
                double Distance = Each.Region.floorCentre().distanceToSqr(Source.getPosition());
                if (Each.Dimension == Source.getLevel().dimension() && Distance < Best) {
                    Best = Distance;
                    Found = Each;
                }
            }
        }
        if (Found == null) {
            Source.sendFailure(Component.translatable(Prefix + "none"));
            return 0;
        }
        Z0002Domains.beginCollapse(Server, Found);
        String Owner = Found.OwnerName;
        Source.sendSuccess(() -> Component.translatable(Prefix + "collapsed", Owner), true);
        return 1;
    }

    private static int collapseAll(CommandContext<CommandSourceStack> Context) {
        MinecraftServer Server = Context.getSource().getServer();
        List<Z0002Domain> Open = new ArrayList<>(Z0002World.get(Server).active());
        for (Z0002Domain Each : Open) {
            Z0002Domains.beginCollapse(Server, Each);
        }
        int Count = Open.size();
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "collapsed_all", Count), true);
        return Count;
    }

    private static int status(CommandContext<CommandSourceStack> Context) {
        CommandSourceStack Source = Context.getSource();
        MinecraftServer Server = Source.getServer();
        long Now = Z0002Domains.now(Server);
        List<Z0002Domain> Open = Z0002World.get(Server).active();
        if (Open.isEmpty()) {
            Source.sendSuccess(() -> Component.translatable(Prefix + "status.none"), false);
        }
        for (Z0002Domain Each : Open) {
            String Where = Each.Region.Centre().toShortString();
            String Dimension = Each.Dimension.location().toString();
            int Members = Each.Members.size();
            long Left = Each.left(Now);
            String Owner = Each.OwnerName;
            Source.sendSuccess(() -> Component.translatable(Prefix + "status.domain", Owner, Where, Dimension, Members, Left), false);
            String Phase = Each.Phase.key();
            Source.sendSuccess(() -> Component.translatable(Prefix + "status.phase", Component.translatable(Prefix + "phase." + Phase)), false);
        }
        if (Source.getEntity() instanceof ServerPlayer User) {
            long Holder = Math.max(0L, Z0002World.get(Server).dormantUntil(User.getUUID()) - Now);
            ItemStack Held = Z0002FingerItem.is(User.getMainHandItem()) ? User.getMainHandItem() : User.getOffhandItem();
            long Carried = Z0002FingerItem.is(Held) ? Math.max(0L, Z0002Domains.itemUntil(Held) - Now) : 0L;
            Source.sendSuccess(() -> Component.translatable(Prefix + "status.dormancy", Holder, Carried), false);
        }
        return Open.size();
    }

    private static int wake(CommandContext<CommandSourceStack> Context, Collection<ServerPlayer> Targets) {
        MinecraftServer Server = Context.getSource().getServer();
        int Woken = 0;
        for (ServerPlayer Target : Targets) {
            Z0002World.get(Server).setDormant(Target.getUUID(), 0L);
            Inventory Pack = Target.getInventory();
            for (int Slot = 0; Slot < Pack.getContainerSize(); Slot++) {
                ItemStack Stack = Pack.getItem(Slot);
                if (Z0002FingerItem.is(Stack) && Stack.has(Z0002Registry.DormantUntil.get())) {
                    Stack.remove(Z0002Registry.DormantUntil.get());
                }
            }
            Woken++;
        }
        int Total = Woken;
        Context.getSource().sendSuccess(() -> Component.translatable(Prefix + "woken", Total), true);
        return Total;
    }
}
