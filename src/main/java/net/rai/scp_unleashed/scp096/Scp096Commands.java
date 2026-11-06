package net.rai.scp_unleashed.scp096;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp096Commands {
    public static final String Prefix = "commands.scp_unleashed.scp096.";
    public static final double Reach = 96.0;

    private Scp096Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp096")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("spawn")
                                .executes(Context -> spawn(Context.getSource(), Context.getSource().getPosition()))
                                .then(Commands.argument("pos", Vec3Argument.vec3())
                                        .executes(Context -> spawn(Context.getSource(), Vec3Argument.getVec3(Context, "pos")))))
                        .then(Commands.literal("enrage")
                                .executes(Context -> enrage(Context.getSource(), null))
                                .then(Commands.argument("witnesses", EntityArgument.entities())
                                        .executes(Context -> enrage(Context.getSource(), EntityArgument.getEntities(Context, "witnesses")))))
                        .then(Commands.literal("calm")
                                .executes(Context -> calm(Context.getSource())))
                        .then(Commands.literal("bag")
                                .executes(Context -> bag(Context.getSource(), null))
                                .then(Commands.literal("on")
                                        .executes(Context -> bag(Context.getSource(), true)))
                                .then(Commands.literal("off")
                                        .executes(Context -> bag(Context.getSource(), false))))
                        .then(Commands.literal("targets")
                                .executes(Context -> targets(Context.getSource())))));
    }

    private static List<Scp096ShyGuy> nearby(CommandSourceStack Source) {
        Vec3 At = Source.getPosition();
        ServerLevel World = Source.getLevel();
        List<Scp096ShyGuy> Found = World.getEntitiesOfClass(Scp096ShyGuy.class, new AABB(At, At).inflate(Reach), Scp096ShyGuy::isAlive);
        Found.sort(Comparator.comparingDouble(Each -> Each.distanceToSqr(At)));
        return Found;
    }

    private static Optional<Scp096ShyGuy> nearest(CommandSourceStack Source) {
        List<Scp096ShyGuy> Found = nearby(Source);
        if (Found.isEmpty()) {
            Source.sendFailure(Component.translatable(Prefix + "none", (int) Reach));
            return Optional.empty();
        }
        return Optional.of(Found.get(0));
    }

    private static int spawn(CommandSourceStack Source, Vec3 Spot) {
        float Yaw = Source.getEntity() != null ? Source.getEntity().getYRot() : 0.0f;
        Optional<Scp096ShyGuy> Placed = Scp096Spawning.place(Source.getLevel(), Spot, Yaw, MobSpawnType.COMMAND);
        if (Placed.isEmpty()) {
            Source.sendFailure(Component.translatable(Prefix + "spawn.failed"));
            return 0;
        }
        Source.sendSuccess(() -> Component.translatable(Prefix + "spawn.success", (int) Math.floor(Spot.x), (int) Math.floor(Spot.y), (int) Math.floor(Spot.z)), true);
        return 1;
    }

    private static int enrage(CommandSourceStack Source, Collection<? extends Entity> Witnesses) {
        Optional<Scp096ShyGuy> Found = nearest(Source);
        if (Found.isEmpty()) {
            return 0;
        }
        Scp096ShyGuy Guy = Found.get();
        ServerLevel World = Source.getLevel();
        if (Witnesses == null) {
            Guy.forceRage(World, Source.getEntity() instanceof LivingEntity Body ? Body : null);
        } else {
            for (Entity Each : Witnesses) {
                if (Each instanceof LivingEntity Body) {
                    Guy.forceRage(World, Body);
                }
            }
        }
        Source.sendSuccess(() -> Component.translatable(Prefix + "enrage", Guy.targetCount()), true);
        return 1;
    }

    private static int calm(CommandSourceStack Source) {
        Optional<Scp096ShyGuy> Found = nearest(Source);
        if (Found.isEmpty()) {
            return 0;
        }
        Found.get().forceCalm(Source.getLevel());
        Source.sendSuccess(() -> Component.translatable(Prefix + "calm"), true);
        return 1;
    }

    private static int bag(CommandSourceStack Source, Boolean Wanted) {
        Optional<Scp096ShyGuy> Found = nearest(Source);
        if (Found.isEmpty()) {
            return 0;
        }
        Scp096ShyGuy Guy = Found.get();
        boolean On = Wanted == null ? !Guy.isBagged() : Wanted;
        if (On) {
            if (!Guy.isCalm()) {
                Guy.forceCalm(Source.getLevel());
            }
            Guy.putBagOn(Source.getLevel());
        } else {
            Guy.takeBagOff(Source.getLevel(), false);
        }
        Source.sendSuccess(() -> Component.translatable(Prefix + (On ? "bag.on" : "bag.off")), true);
        return 1;
    }

    private static int targets(CommandSourceStack Source) {
        Optional<Scp096ShyGuy> Found = nearest(Source);
        if (Found.isEmpty()) {
            return 0;
        }
        Scp096ShyGuy Guy = Found.get();
        ServerLevel World = Source.getLevel();
        Component State = Component.translatable("jade.scp_unleashed.scp096.state." + Guy.stateKey());
        Source.sendSuccess(() -> Component.translatable(Prefix + "targets", State, Guy.targetKeys().size(), (int) Guy.getHealth(), (int) Guy.getMaxHealth()), false);
        for (UUID Key : Guy.targetKeys()) {
            LivingEntity Body = Scp096Persons.resolve(World, Key);
            Component Name = Body != null ? Body.getDisplayName() : Component.translatable(Prefix + "targets.absent", Key.toString().substring(0, 8)).withStyle(ChatFormatting.GRAY);
            Source.sendSuccess(() -> Component.translatable(Prefix + "targets.entry", Name), false);
        }
        return Guy.targetKeys().size();
    }
}
