package net.rai.scp_unleashed.scp439;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.Scp_unleashed;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public final class Scp439Commands {
    public static final String Prefix = "commands.scp_unleashed.scp439.";
    public static final double Reach = 32.0;

    private Scp439Commands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> Dispatcher) {
        Dispatcher.register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp439")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("queen").executes(Context -> queen(Context.getSource())))
                        .then(Commands.literal("infect").then(Commands.argument("targets", EntityArgument.entities())
                                .executes(Context -> infect(Context.getSource(), EntityArgument.getEntities(Context, "targets")))))
                        .then(Commands.literal("hive").executes(Context -> hive(Context.getSource(), Scp439Hive.Tiers + 1))
                                .then(Commands.argument("stage", IntegerArgumentType.integer(1, Scp439Hive.Tiers + 1))
                                        .executes(Context -> hive(Context.getSource(), IntegerArgumentType.getInteger(Context, "stage")))))
                        .then(Commands.literal("food").then(Commands.argument("amount", IntegerArgumentType.integer(1, 10000))
                                .executes(Context -> food(Context.getSource(), IntegerArgumentType.getInteger(Context, "amount")))))
                        .then(Commands.literal("status").executes(Context -> status(Context.getSource())))));
    }

    @Nullable
    public static Scp439Hive nearest(ServerLevel World, Vec3 At) {
        Scp439Hive Best = null;
        double BestDistance = Double.MAX_VALUE;
        for (Scp439Hive Found : World.getEntitiesOfClass(Scp439Hive.class, new AABB(At, At).inflate(Reach), Scp439Hive::isAlive)) {
            double Distance = Found.distanceToSqr(At);
            if (Distance < BestDistance) {
                Best = Found;
                BestDistance = Distance;
            }
        }
        return Best;
    }

    private static float yawOf(CommandSourceStack Source) {
        return Source.getEntity() != null ? Source.getEntity().getYRot() : 0.0f;
    }

    private static int queen(CommandSourceStack Source) {
        Scp439Queen Made = Scp439Hive.release(Source.getLevel(), Source.getPosition(), yawOf(Source), new CompoundTag());
        if (Made == null) {
            Source.sendFailure(Component.translatable(Prefix + "queen.failed"));
            return 0;
        }
        Source.sendSuccess(() -> Component.translatable(Prefix + "queen"), true);
        return 1;
    }

    private static int infect(CommandSourceStack Source, Collection<? extends Entity> Targets) {
        int Count = 0;
        for (Entity Target : Targets) {
            if (Target instanceof LivingEntity Living && Scp439Infection.infect(Living, null)) {
                Count++;
            }
        }
        if (Count == 0) {
            Source.sendFailure(Component.translatable(Prefix + "infect.none"));
            return 0;
        }
        int Total = Count;
        Source.sendSuccess(() -> Component.translatable(Prefix + "infect", Total), true);
        return Count;
    }

    private static int hive(CommandSourceStack Source, int Stage) {
        Scp439Look Shown = Source.getEntity() instanceof LivingEntity Living ? Scp439Look.of(Living) : Scp439Look.Blank;
        int Ticks = Stage > Scp439Hive.Tiers ? Scp439Hive.FullTicks : Math.max(Scp439Hive.StartTicks, (Stage - 1) * Scp439Hive.StageTicks + Scp439Hive.StartTicks);
        Vec3 At = Source.getPosition();
        Scp439Hive Made = Scp439Hive.grow(Source.getLevel(), At, yawOf(Source) + 180.0f, Shown, new CompoundTag(), Ticks);
        if (Made == null) {
            Source.sendFailure(Component.translatable(Prefix + "hive.failed"));
            return 0;
        }
        int Reached = Made.stage();
        Source.sendSuccess(() -> Component.translatable(Prefix + "hive", Reached, Scp439Hive.Tiers), true);
        return 1;
    }

    private static int food(CommandSourceStack Source, int Amount) {
        Scp439Hive Found = nearest(Source.getLevel(), Source.getPosition());
        if (Found == null) {
            Source.sendFailure(Component.translatable(Prefix + "no_hive", (int) Reach));
            return 0;
        }
        Found.addFood(Amount);
        int Stored = Found.food();
        Source.sendSuccess(() -> Component.translatable(Prefix + "food", Amount, Stored), true);
        return 1;
    }

    private static int status(CommandSourceStack Source) throws CommandSyntaxException {
        ServerLevel World = Source.getLevel();
        Scp439Colonies Colonies = Scp439Colonies.get(World);
        long Now = World.getGameTime();
        Component Spread = Component.translatable(Prefix + (Scp439Registry.spreads(World.getGameRules()) ? "on" : "off"));
        Source.sendSuccess(() -> Component.translatable(Prefix + "status.colonies", Colonies.hives(), Colonies.hosts(Now), Colonies.queens(Now), Scp439Colonies.Cap, Spread), false);
        Scp439Hive Found = nearest(World, Source.getPosition());
        if (Found != null) {
            Component Named = Found.look().name(World.registryAccess());
            Component Host = Named != null ? Named : Component.literal("?");
            int Workers = Found.count(World, Scp439Colonies.Workers);
            int Brutes = Found.count(World, Scp439Colonies.Brutes);
            Source.sendSuccess(() -> Component.translatable(Prefix + "status.hive", Host, Found.stage(), Scp439Hive.Tiers, Found.food(), Found.fed(), Workers, Brutes), false);
        }
        if (Source.getEntity() instanceof LivingEntity Living) {
            Scp439Infection.State State = Scp439Infection.stateOf(Living);
            if (State != null) {
                int Percent = Math.round(State.progress() * 100.0f);
                Source.sendSuccess(() -> Component.translatable(Prefix + "status.host", State.Stage, Scp439Infection.Stages, Percent), false);
            }
        }
        return 1;
    }
}
