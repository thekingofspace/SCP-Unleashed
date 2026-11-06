package net.rai.scp_unleashed.scp049;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp049Commands {
    public static final double SearchRange = 96.0;
    public static final int SpawnAhead = 3;

    private Scp049Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp049")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("spawn").executes(Context -> spawn(Context.getSource())))
                        .then(Commands.literal("release").executes(Context -> release(Context.getSource())))
                        .then(Commands.literal("routine").executes(Context -> routine(Context.getSource())))
                        .then(Commands.literal("status").executes(Context -> routine(Context.getSource())))));
    }

    @Nullable
    private static Scp049Doctor nearest(ServerLevel World, Vec3 From) {
        Scp049Doctor Best = null;
        double BestDistance = SearchRange * SearchRange;
        for (Scp049Doctor Doctor : World.getEntitiesOfClass(Scp049Doctor.class, new AABB(From, From).inflate(SearchRange))) {
            double Distance = Doctor.distanceToSqr(From);
            if (Distance < BestDistance) {
                BestDistance = Distance;
                Best = Doctor;
            }
        }
        return Best;
    }

    private static int spawn(CommandSourceStack Source) {
        ServerLevel World = Source.getLevel();
        Vec3 From = Source.getPosition();
        Entity Caller = Source.getEntity();
        BlockPos Centre = BlockPos.containing(From);
        if (Caller != null) {
            Vec3 Ahead = Caller.getLookAngle().multiply(1.0, 0.0, 1.0);
            if (Ahead.lengthSqr() > 1.0e-4) {
                Ahead = Ahead.normalize().scale(SpawnAhead);
                Centre = BlockPos.containing(From.add(Ahead));
            }
        }
        Optional<BlockPos> Site = Scp049Discovery.openSite(World, Centre, World.random);
        if (Site.isEmpty()) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp049.no_room"));
            return 0;
        }
        Scp049Doctor Doctor = Scp049Discovery.stage(World, Site.get(), From);
        if (Doctor == null) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp049.no_room"));
            return 0;
        }
        BlockPos Tray = Site.get();
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp049.spawned", Tray.getX(), Tray.getY(), Tray.getZ()), true);
        return 1;
    }

    private static int release(CommandSourceStack Source) {
        ServerLevel World = Source.getLevel();
        Vec3 From = Source.getPosition();
        int Freed = 0;
        for (Scp049Doctor Doctor : World.getEntitiesOfClass(Scp049Doctor.class, new AABB(From, From).inflate(SearchRange))) {
            Freed += Doctor.releaseAll(World);
        }
        if (Source.getEntity() instanceof LivingEntity Caller && Scp049Hold.isHeld(Caller)) {
            Scp049Hold.release(Caller);
            Freed++;
        }
        int Total = Freed;
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp049.released", Total), true);
        return Total;
    }

    private static int routine(CommandSourceStack Source) {
        ServerLevel World = Source.getLevel();
        Scp049Doctor Doctor = nearest(World, Source.getPosition());
        if (Doctor == null) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.scp049.none", (int) SearchRange));
            return 0;
        }
        Component Phase = Component.translatable("jade.scp_unleashed.scp049." + Doctor.stateKey());
        Component Routine = Component.translatable(Doctor.quarryId() != null ? "commands.scp_unleashed.scp049.routine.person"
                : !Doctor.trayUsable(World) ? "commands.scp_unleashed.scp049.routine.hunt" : "commands.scp_unleashed.scp049.routine.villager");
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp049.status", Routine, Phase, (int) Doctor.getHealth(), (int) Doctor.getMaxHealth()), false);
        Component TrayLine = Doctor.trayDebug();
        Source.sendSuccess(() -> TrayLine, false);
        Component Reach = Doctor.reachDebug();
        Source.sendSuccess(() -> Reach, false);
        int Patients = Doctor.patientsLeft(World);
        int Followers = Doctor.followers(World).size();
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp049.status.area", Patients, Followers), false);
        LivingEntity Held = Doctor.held(World);
        if (Held != null) {
            Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.scp049.status.held", Held.getDisplayName()), false);
        }
        Component Movement = Doctor.navDebug(World);
        Source.sendSuccess(() -> Movement, false);
        Component Debug = Doctor.dragDebug(World);
        Source.sendSuccess(() -> Debug, false);
        Component Loss = Doctor.lossDebug(World);
        Source.sendSuccess(() -> Loss, false);
        Component Defence = Doctor.defenceDebug(World);
        Source.sendSuccess(() -> Defence, false);
        Component Fright = Doctor.fearDebug();
        Source.sendSuccess(() -> Fright, false);
        return 1;
    }
}
