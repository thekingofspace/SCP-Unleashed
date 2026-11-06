package net.rai.scp_unleashed.scp031;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.Optional;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp031Commands {
    public static final String Prefix = "commands.scp_unleashed.scp031.";
    public static final double Ahead = 3.0;

    private Scp031Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp031")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("spawn").executes(Context -> spawn(Context.getSource())))
                        .then(Commands.literal("polyp")
                                .executes(Context -> polyp(Context.getSource(), 0))
                                .then(Commands.argument("stage", IntegerArgumentType.integer(0, Scp031Polyp.Stages - 1))
                                        .executes(Context -> polyp(Context.getSource(), IntegerArgumentType.getInteger(Context, "stage")))))
                        .then(Commands.literal("release").executes(Context -> release(Context.getSource())))));
    }

    private static Vec3 spot(CommandSourceStack Source) {
        ServerLevel World = Source.getLevel();
        Vec3 Origin = Source.getPosition();
        Entity Caller = Source.getEntity();
        if (Caller == null) {
            return Origin;
        }
        Vec3 Look = Caller.getLookAngle().multiply(1.0, 0.0, 1.0);
        if (Look.lengthSqr() < 1.0e-4) {
            return Origin;
        }
        Vec3 Forward = Origin.add(Look.normalize().scale(Ahead));
        BlockPos Cell = BlockPos.containing(Forward);
        for (int Dy = 2; Dy >= -3; Dy--) {
            BlockPos Candidate = Cell.above(Dy);
            if (Scp031Mimic.roomFor(World, Candidate) && !World.getBlockState(Candidate.below()).getCollisionShape(World, Candidate.below()).isEmpty()) {
                return new Vec3(Forward.x, Candidate.getY(), Forward.z);
            }
        }
        return Origin;
    }

    private static float facing(CommandSourceStack Source, Vec3 Where) {
        Vec3 Toward = Source.getPosition().subtract(Where);
        return (float) Math.toDegrees(Math.atan2(Toward.z, Toward.x)) - 90.0f;
    }

    private static int spawn(CommandSourceStack Source) {
        Vec3 Where = spot(Source);
        Optional<Scp031Mimic> Made = Scp031Mimic.spawnAt(Source.getLevel(), Where, facing(Source, Where));
        if (Made.isEmpty()) {
            Source.sendFailure(Component.translatable(Prefix + "failed"));
            return 0;
        }
        int Count = Scp031Census.get(Source.getLevel()).count(Source.getLevel().getGameTime());
        Source.sendSuccess(() -> Component.translatable(Prefix + "spawned", (int) Where.x, (int) Where.y, (int) Where.z, Count, Scp031Census.Cap), true);
        return 1;
    }

    private static int polyp(CommandSourceStack Source, int Stage) {
        Vec3 Where = spot(Source);
        Optional<Scp031Polyp> Made = Scp031Polyp.sprout(Source.getLevel(), Where);
        if (Made.isEmpty()) {
            Source.sendFailure(Component.translatable(Prefix + "failed"));
            return 0;
        }
        if (Stage > 0) {
            Made.get().setStage(Stage);
        }
        Source.sendSuccess(() -> Component.translatable(Prefix + "polyp", (int) Where.x, (int) Where.y, (int) Where.z, Stage + 1, Scp031Polyp.Stages), true);
        return 1;
    }

    private static int release(CommandSourceStack Source) {
        int Freed = Scp031Holds.releaseAll(Source.getServer());
        if (Freed == 0) {
            Source.sendSuccess(() -> Component.translatable(Prefix + "release.none"), false);
            return 0;
        }
        Source.sendSuccess(() -> Component.translatable(Prefix + "released", Freed), true);
        return Freed;
    }
}
