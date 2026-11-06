package net.rai.scp_unleashed.scp224;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.Collection;
import java.util.List;

public final class Scp224Commands {
    public static final String Prefix = "commands.scp_unleashed.scp224.";
    public static final double ScanRadius = 48.0;

    private Scp224Commands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> Dispatcher) {
        Dispatcher.register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp224")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("give")
                                .executes(Context -> give(Context.getSource(), List.of(Context.getSource().getPlayerOrException())))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(Context -> give(Context.getSource(), EntityArgument.getPlayers(Context, "targets")))))
                        .then(Commands.literal("toll")
                                .executes(Context -> toll(Context.getSource())))
                        .then(Commands.literal("age")
                                .then(Commands.argument("targets", EntityArgument.entities())
                                        .then(Commands.argument("seconds", IntegerArgumentType.integer(0, Scp224Aging.Span / 20))
                                                .executes(Context -> age(Context.getSource(), EntityArgument.getEntities(Context, "targets"), IntegerArgumentType.getInteger(Context, "seconds"))))))
                        .then(Commands.literal("clear")
                                .then(Commands.argument("targets", EntityArgument.entities())
                                        .executes(Context -> clear(Context.getSource(), EntityArgument.getEntities(Context, "targets")))))
                        .then(Commands.literal("status")
                                .executes(Context -> status(Context.getSource(), Context.getSource().getPlayerOrException()))
                                .then(Commands.argument("target", EntityArgument.entity())
                                        .executes(Context -> status(Context.getSource(), EntityArgument.getEntity(Context, "target")))))));
    }

    private static int give(CommandSourceStack Source, Collection<ServerPlayer> Targets) {
        for (ServerPlayer Target : Targets) {
            ItemStack Stack = new ItemStack(Scp224Registry.ClockItem.get());
            if (!Target.getInventory().add(Stack)) {
                Target.drop(Stack, false);
            }
            Source.sendSuccess(() -> Component.translatable(Prefix + "give", Target.getDisplayName()), true);
        }
        return Targets.size();
    }

    private static int toll(CommandSourceStack Source) {
        ServerLevel World = Source.getLevel();
        Scp224ClockBlockEntity Clock = Scp224Clocks.nearest(World, Source.getPosition(), ScanRadius);
        if (Clock == null) {
            Source.sendFailure(Component.translatable(Prefix + "none", (int) ScanRadius));
            return 0;
        }
        if (Clock.isStilled()) {
            Clock.restart(World, true);
        }
        Clock.markRoomDirty();
        int Caught = Clock.toll(World).size();
        BlockPos Pos = Clock.getBlockPos();
        Source.sendSuccess(() -> Component.translatable(Prefix + "tolled", Pos.getX(), Pos.getY(), Pos.getZ(), Caught), true);
        return Caught;
    }

    private static int age(CommandSourceStack Source, Collection<? extends Entity> Targets, int Seconds) {
        ServerLevel World = Source.getLevel();
        Scp224ClockBlockEntity Clock = Scp224Clocks.nearest(World, Source.getPosition(), ScanRadius);
        if (Clock == null) {
            Source.sendFailure(Component.translatable(Prefix + "none", (int) ScanRadius));
            return 0;
        }
        int Count = 0;
        for (Entity Each : Targets) {
            if (Each instanceof LivingEntity Living) {
                Scp224Aging.seize(Living, World, Clock.getBlockPos(), Clock.getSerial());
                Living.setData(Scp224Registry.Age, Scp224Aging.age(Living).withElapsed(Math.max(1, Seconds * 20)));
                Count++;
            }
        }
        int Total = Count;
        Source.sendSuccess(() -> Component.translatable(Prefix + "aged", Total, Seconds), true);
        return Count;
    }

    private static int clear(CommandSourceStack Source, Collection<? extends Entity> Targets) {
        int Count = 0;
        for (Entity Each : Targets) {
            if (Each instanceof LivingEntity Living && Living.hasData(Scp224Registry.Age)) {
                Scp224Aging.clear(Living);
                Count++;
            }
        }
        int Total = Count;
        Source.sendSuccess(() -> Component.translatable(Prefix + "cleared", Total), true);
        return Count;
    }

    private static int status(CommandSourceStack Source, Entity Target) {
        if (Target instanceof LivingEntity Living) {
            Scp224Age Current = Scp224Aging.age(Living);
            if (Current == null) {
                Source.sendSuccess(() -> Component.translatable(Prefix + "unaged", Target.getDisplayName()), false);
            } else {
                Component Condition = Component.translatable(Prefix + (Current.Released() ? "recovering" : "aging"));
                BlockPos Bound = Current.Clock().pos();
                Source.sendSuccess(() -> Component.translatable(Prefix + "status", Target.getDisplayName(), Current.Elapsed() / 20, Scp224Aging.Span / 20,
                        Math.round(Current.progress() * 100.0f), Condition, Bound.getX(), Bound.getY(), Bound.getZ()), false);
            }
        }
        ServerLevel World = Source.getLevel();
        Scp224ClockBlockEntity Clock = Scp224Clocks.nearest(World, Target.position(), ScanRadius);
        if (Clock == null) {
            Source.sendSuccess(() -> Component.translatable(Prefix + "none", (int) ScanRadius), false);
            return 0;
        }
        Scp224Room Room = Clock.room(World);
        BlockPos Pos = Clock.getBlockPos();
        long Now = World.getGameTime();
        Component State = Clock.isStilled() ? Component.translatable(Prefix + "still", Math.max(0L, Clock.getRestartAt() - Now) / 20)
                : Clock.isTolling() ? Component.translatable(Prefix + "tolling", Clock.getStrokesLeft())
                : Component.translatable(Prefix + "running", Math.max(0L, Clock.getNextToll() - Now) / 20);
        Component Shape = Component.translatable(Prefix + (Room.isOpen() ? "open" : "enclosed"), Room.size(), Room.doors());
        boolean Inside = Target instanceof LivingEntity Living && Room.contains(Living);
        boolean Sees = Target instanceof LivingEntity Living && Scp224Sight.sees(World, Living.getEyePosition(), Pos, Clock.facing());
        Source.sendSuccess(() -> Component.translatable(Prefix + "clock", Pos.getX(), Pos.getY(), Pos.getZ(), State, Shape,
                Component.translatable(Prefix + (Inside ? "inside" : "outside")), Component.translatable(Prefix + (Sees ? "seen" : "unseen"))), false);
        return 1;
    }
}
