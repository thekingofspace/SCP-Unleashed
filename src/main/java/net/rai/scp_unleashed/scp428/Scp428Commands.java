package net.rai.scp_unleashed.scp428;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
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

import java.util.Comparator;
import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp428Commands {
    public static final String Prefix = "commands.scp_unleashed.scp428.";
    public static final double SearchRange = 64.0;
    public static final int ListedNames = 8;

    private Scp428Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp428")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("spawn")
                                .executes(Context -> spawn(Context.getSource(), Context.getSource().getPosition()))
                                .then(Commands.argument("pos", Vec3Argument.vec3())
                                        .executes(Context -> spawn(Context.getSource(), Vec3Argument.getVec3(Context, "pos")))))
                        .then(Commands.literal("absorb")
                                .then(Commands.argument("target", EntityArgument.entity())
                                        .executes(Context -> absorb(Context.getSource(), EntityArgument.getEntity(Context, "target")))))
                        .then(Commands.literal("count")
                                .executes(Context -> count(Context.getSource())))
                        .then(Commands.literal("release")
                                .executes(Context -> release(Context.getSource())))));
    }

    @Nullable
    public static Scp428Core nearest(ServerLevel World, Vec3 Here) {
        List<Scp428Core> Found = World.getEntitiesOfClass(Scp428Core.class, new AABB(Here, Here).inflate(SearchRange), Scp428Core::isAlive);
        Found.sort(Comparator.comparingDouble(Core -> Core.distanceToSqr(Here)));
        return Found.isEmpty() ? null : Found.get(0);
    }

    private static int spawn(CommandSourceStack Source, Vec3 Spot) {
        ServerLevel World = Source.getLevel();
        float Yaw = Source.getEntity() != null ? Source.getEntity().getYRot() + 180.0f : 0.0f;
        Scp428Core Core = Scp428Arrival.place(World, Spot, Yaw);
        if (Core == null) {
            Source.sendFailure(Component.translatable(Prefix + "spawn.failed"));
            return 0;
        }
        Source.sendSuccess(() -> Component.translatable(Prefix + "spawn.success", (int) Math.floor(Spot.x), (int) Math.floor(Spot.y), (int) Math.floor(Spot.z)), true);
        return 1;
    }

    private static int absorb(CommandSourceStack Source, Entity Target) throws CommandSyntaxException {
        if (!(Target instanceof LivingEntity Living) || !Scp428Quarry.isQuarry(Living) || !(Target.level() instanceof ServerLevel World)) {
            Source.sendFailure(Component.translatable(Prefix + "absorb.invalid", Target.getDisplayName()));
            return 0;
        }
        Scp428Core Core = nearest(World, Target.position());
        if (Core == null) {
            Source.sendFailure(Component.translatable(Prefix + "none"));
            return 0;
        }
        if (Core.count() >= Scp428Size.Capacity) {
            Source.sendFailure(Component.translatable(Prefix + "absorb.full"));
            return 0;
        }
        Core.releaseGrip(false);
        Core.strike(World, Living);
        Source.sendSuccess(() -> Component.translatable(Prefix + "absorb.success", Target.getDisplayName()), true);
        return 1;
    }

    private static int count(CommandSourceStack Source) {
        Scp428Core Core = nearest(Source.getLevel(), Source.getPosition());
        if (Core == null) {
            Source.sendFailure(Component.translatable(Prefix + "none"));
            return 0;
        }
        List<Scp428Record> Records = Core.records();
        StringBuilder Names = new StringBuilder();
        for (int Index = 0; Index < Math.min(ListedNames, Records.size()); Index++) {
            if (Index > 0) {
                Names.append(", ");
            }
            Names.append(Records.get(Index).name());
        }
        if (Records.size() > ListedNames) {
            Names.append(", ...");
        }
        String Listed = Names.toString();
        Source.sendSuccess(() -> Component.translatable(Prefix + "count", Core.count(), Scp428Size.Capacity, (int) Math.ceil(Core.getHealth()), (int) Core.getMaxHealth(),
                Listed.isEmpty() ? "-" : Listed), false);
        return Core.count();
    }

    private static int release(CommandSourceStack Source) {
        Scp428Core Core = nearest(Source.getLevel(), Source.getPosition());
        if (Core == null) {
            Source.sendFailure(Component.translatable(Prefix + "none"));
            return 0;
        }
        if (!Core.isGripping() && Core.holdTotal() == 0) {
            Source.sendFailure(Component.translatable(Prefix + "release.idle"));
            return 0;
        }
        Core.releaseAll();
        Source.sendSuccess(() -> Component.translatable(Prefix + "release.success"), true);
        return 1;
    }
}
