package net.rai.scp_unleashed.daevitegod;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import org.jetbrains.annotations.Nullable;

import java.util.OptionalDouble;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class DaeviteGodCommands {
    public static final double PlaceAhead = 10.0;
    public static final String Prefix = "commands.scp_unleashed.daevite_god.";

    private DaeviteGodCommands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("daevite_god")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("summon")
                                .executes(Context -> summon(Context.getSource(), null))
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(Context -> summon(Context.getSource(), BlockPosArgument.getLoadedBlockPos(Context, "pos")))))
                        .then(Commands.literal("stage2").executes(Context -> stageTwo(Context.getSource())))
                        .then(Commands.literal("status").executes(Context -> status(Context.getSource())))
                        .then(Commands.literal("banish").executes(Context -> banish(Context.getSource())))));
    }

    private static int summon(CommandSourceStack Source, @Nullable BlockPos Given) throws CommandSyntaxException {
        ServerLevel World = Source.getLevel();
        Vec3 At;
        if (Given != null) {
            At = Vec3.atBottomCenterOf(Given);
        } else {
            Entity Caller = Source.getEntity();
            Vec3 Ahead = Caller != null ? Caller.getLookAngle().multiply(1.0, 0.0, 1.0) : new Vec3(0.0, 0.0, 1.0);
            if (Ahead.lengthSqr() < 1.0e-4) {
                Ahead = new Vec3(0.0, 0.0, 1.0);
            }
            Vec3 Spot = Source.getPosition().add(Ahead.normalize().scale(PlaceAhead));
            OptionalDouble Ground = DaeviteGodTargets.groundAt(World, Spot.x, Spot.z, Source.getPosition().y + 4.0, 24);
            At = new Vec3(Spot.x, Ground.isPresent() ? Ground.getAsDouble() : Spot.y, Spot.z);
        }
        DaeviteGod God = DaeviteGodSummoning.call(World, At);
        if (God == null) {
            Source.sendFailure(Component.translatable(Prefix + "failed"));
            return 0;
        }
        BlockPos Placed = God.blockPosition();
        Source.sendSuccess(() -> Component.translatable(Prefix + "summoned", Placed.getX(), Placed.getY(), Placed.getZ()), true);
        return 1;
    }

    @Nullable
    private static DaeviteGod live(CommandSourceStack Source) {
        MinecraftServer Server = Source.getServer();
        return DaeviteGodLedger.get(Server).live(Server);
    }

    private static int stageTwo(CommandSourceStack Source) {
        DaeviteGod God = live(Source);
        if (God == null) {
            Source.sendFailure(Component.translatable(Prefix + "none"));
            return 0;
        }
        if (God.king()) {
            Source.sendFailure(Component.translatable(Prefix + "already_king"));
            return 0;
        }
        God.forceTransform();
        Source.sendSuccess(() -> Component.translatable(Prefix + "transforming"), true);
        return 1;
    }

    private static int status(CommandSourceStack Source) {
        MinecraftServer Server = Source.getServer();
        DaeviteGodLedger Ledger = DaeviteGodLedger.get(Server);
        DaeviteGod God = Ledger.live(Server);
        if (God == null) {
            if (Ledger.alive() && Ledger.dimension() != null) {
                BlockPos Pos = Ledger.pos();
                Source.sendSuccess(() -> Component.translatable(Prefix + "status_unloaded", Ledger.stage(), Ledger.dimension().location().toString(), Pos.getX(), Pos.getY(), Pos.getZ()), false);
                return 1;
            }
            Source.sendSuccess(() -> Ledger.slain() > 0 ? Component.translatable(Prefix + "status_slain", Ledger.slain()) : Component.translatable(Prefix + "status_none"), false);
            return 0;
        }
        BlockPos Pos = God.blockPosition();
        LivingEntity Target = God.getTarget();
        Component Hunting = Target == null ? Component.translatable(Prefix + "no_target") : Target.getDisplayName();
        String State = God.isDeadOrDying() ? "dying" : God.entering() ? "arriving" : God.transforming() ? "transforming" : God.recovering() ? "recovering" : "active";
        Component Mood = Component.translatable(Prefix + "state." + State);
        Source.sendSuccess(() -> Component.translatable(Prefix + "status", God.stage(), Mood, God.level().dimension().location().toString(), Pos.getX(), Pos.getY(), Pos.getZ(), Hunting), false);
        return 1;
    }

    private static int banish(CommandSourceStack Source) {
        MinecraftServer Server = Source.getServer();
        DaeviteGodLedger Ledger = DaeviteGodLedger.get(Server);
        DaeviteGod God = Ledger.live(Server);
        if (God == null) {
            if (Ledger.alive()) {
                Ledger.forget();
                Source.sendSuccess(() -> Component.translatable(Prefix + "banished_unloaded"), true);
                return 1;
            }
            Source.sendFailure(Component.translatable(Prefix + "none"));
            return 0;
        }
        God.banish((ServerLevel) God.level());
        Source.sendSuccess(() -> Component.translatable(Prefix + "banished"), true);
        return 1;
    }
}
