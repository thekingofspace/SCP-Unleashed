package net.rai.scp_unleashed.scp149;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp149Commands {
    public static final String Prefix = "commands.scp_unleashed.scp149.";
    public static final int Reach = 32;
    public static final double MaxDays = 30.0;

    private Scp149Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp149")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("spawn")
                                .executes(Context -> spawn(Context.getSource(), Context.getSource().getPosition()))
                                .then(Commands.argument("pos", Vec3Argument.vec3())
                                        .executes(Context -> spawn(Context.getSource(), Vec3Argument.getVec3(Context, "pos")))))
                        .then(Commands.literal("infect")
                                .then(Commands.argument("targets", EntityArgument.entities())
                                        .executes(Context -> infect(Context.getSource(), EntityArgument.getEntities(Context, "targets"), false))
                                        .then(Commands.literal("shed")
                                                .executes(Context -> infect(Context.getSource(), EntityArgument.getEntities(Context, "targets"), true)))))
                        .then(Commands.literal("hatch")
                                .executes(Context -> hatch(Context.getSource())))
                        .then(Commands.literal("hunger")
                                .then(Commands.argument("days", DoubleArgumentType.doubleArg(0.0, MaxDays))
                                        .executes(Context -> hunger(Context.getSource(), DoubleArgumentType.getDouble(Context, "days")))))
                        .then(Commands.literal("status")
                                .executes(Context -> status(Context.getSource())))));
    }

    private static int spawn(CommandSourceStack Source, Vec3 Spot) {
        ServerLevel World = Source.getLevel();
        Scp149World Data = Scp149World.get(World.getServer());
        if (!Data.room()) {
            Source.sendFailure(Component.translatable(Prefix + "spawn.full", Scp149World.Cap));
            return 0;
        }
        Scp149Fly Fly = Scp149Registry.Fly.get().create(World);
        if (Fly == null) {
            Source.sendFailure(Component.translatable(Prefix + "spawn.failed"));
            return 0;
        }
        float Yaw = Source.getEntity() != null ? Source.getEntity().getYRot() + 180.0f : 0.0f;
        Fly.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        if (!World.noCollision(Fly)) {
            Source.sendFailure(Component.translatable(Prefix + "spawn.failed"));
            return 0;
        }
        Fly.finalizeSpawn(World, World.getCurrentDifficultyAt(BlockPos.containing(Spot)), MobSpawnType.COMMAND, null);
        Fly.newborn();
        if (!World.addFreshEntity(Fly)) {
            Source.sendFailure(Component.translatable(Prefix + "spawn.failed"));
            return 0;
        }
        Source.sendSuccess(() -> Component.translatable(Prefix + "spawn.success", (int) Math.floor(Spot.x), (int) Math.floor(Spot.y), (int) Math.floor(Spot.z),
                Data.count(), Scp149World.Cap), true);
        return 1;
    }

    private static int infect(CommandSourceStack Source, Collection<? extends Entity> Targets, boolean Shed) {
        int Infected = 0;
        for (Entity Target : Targets) {
            if (!(Target instanceof LivingEntity Subject)) {
                continue;
            }
            boolean Already = Scp149Host.isHost(Subject);
            if (!Already && !Scp149Host.infect(Subject)) {
                Source.sendFailure(Component.translatable(Prefix + "infect.failed", Subject.getDisplayName()));
                continue;
            }
            if (Shed) {
                Scp149Host.beginBursting(Subject);
            }
            Infected++;
            Source.sendSuccess(() -> Component.translatable(Prefix + (Shed ? "infect.bursting" : "infect.success"), Subject.getDisplayName()), true);
        }
        return Infected;
    }

    private static int hatch(CommandSourceStack Source) {
        ServerLevel World = Source.getLevel();
        BlockPos Centre = BlockPos.containing(Source.getPosition());
        List<Scp149CocoonBlockEntity> Found = Scp149Brood.cocoonsNear(World, Centre, Reach);
        if (Found.isEmpty()) {
            Source.sendFailure(Component.translatable(Prefix + "hatch.none", Reach));
            return 0;
        }
        int Freed = 0;
        int Opened = 0;
        int Waiting = 0;
        for (Scp149CocoonBlockEntity Cocoon : Found) {
            BlockPos Pos = Cocoon.getBlockPos();
            Cocoon.ripen();
            Scp149Brood.Split Result = Scp149Brood.hatch(World, Pos);
            Freed += Result.Freed();
            Waiting += Result.Opened() ? 0 : Cocoon.waiting();
            Opened += Result.Opened() ? 1 : 0;
        }
        int Flies = Freed;
        int Cocoons = Opened;
        int Left = Waiting;
        Source.sendSuccess(() -> Component.translatable(Prefix + "hatch.success", Flies, Cocoons, Left), true);
        return Flies;
    }

    private static Set<UUID> nearbyGroups(CommandSourceStack Source, Scp149World Data) {
        Set<UUID> Groups = new LinkedHashSet<>();
        for (Scp149Fly Fly : Source.getLevel().getEntitiesOfClass(Scp149Fly.class, AABB.ofSize(Source.getPosition(), Reach * 2.0, Reach * 2.0, Reach * 2.0), Scp149Fly::isAlive)) {
            UUID Group = Data.groupOf(Fly.getUUID());
            if (Group != null) {
                Groups.add(Group);
            }
        }
        return Groups;
    }

    private static int hunger(CommandSourceStack Source, double Days) {
        Scp149World Data = Scp149World.get(Source.getServer());
        Set<UUID> Groups = nearbyGroups(Source, Data);
        if (Groups.isEmpty()) {
            Source.sendFailure(Component.translatable(Prefix + "hunger.none", Reach));
            return 0;
        }
        long Now = Scp149World.now(Source.getServer());
        long Fed = Now - Math.round(Days * Scp149World.DayTicks);
        for (UUID Group : Groups) {
            Data.setFed(Group, Fed);
        }
        String Shown = String.format(Locale.ROOT, "%.1f", Days);
        int Count = Groups.size();
        Source.sendSuccess(() -> Component.translatable(Prefix + "hunger.success", Count, Shown), true);
        return Count;
    }

    private static int status(CommandSourceStack Source) {
        Scp149World Data = Scp149World.get(Source.getServer());
        long Now = Scp149World.now(Source.getServer());
        Source.sendSuccess(() -> Component.translatable(Prefix + "status.world", Data.count(), Scp149World.Cap, Data.groups().size()), false);
        for (UUID Group : nearbyGroups(Source, Data)) {
            String Days = String.format(Locale.ROOT, "%.2f", Data.daysHungry(Group, Now));
            boolean Restless = Data.restless(Group, Now);
            Source.sendSuccess(() -> Component.translatable(Prefix + "status.group", Group.toString().substring(0, 8), Data.members(Group), Days,
                    Component.translatable(Prefix + (Restless ? "status.restless" : "status.settled"))), false);
        }
        ServerLevel World = Source.getLevel();
        Vec3 Centre = Source.getPosition();
        int Larvae = World.getEntitiesOfClass(Scp149Larva.class, AABB.ofSize(Centre, Reach * 2.0, Reach * 2.0, Reach * 2.0), Scp149Larva::isAlive).size();
        List<Scp149CocoonBlockEntity> Nearby = Scp149Brood.cocoonsNear(World, BlockPos.containing(Centre), Reach);
        int Cocoons = Nearby.size();
        Source.sendSuccess(() -> Component.translatable(Prefix + "status.nearby", Larvae, Cocoons, Reach), false);
        for (Scp149CocoonBlockEntity Cocoon : Nearby) {
            BlockPos Pos = Cocoon.getBlockPos();
            Source.sendSuccess(() -> Component.translatable(Prefix + "status.cocoon", Pos.getX(), Pos.getY(), Pos.getZ(), Cocoon.strands(), Cocoon.released(), Cocoon.waiting(),
                    Cocoon.stage()), false);
        }
        if (Source.getEntity() instanceof LivingEntity Self) {
            Scp149Host.State Current = Scp149Host.stateOf(Self);
            if (Current.active()) {
                Source.sendSuccess(() -> Component.translatable(Prefix + (Current.Stage() == Scp149Host.Incubating ? "status.incubating" : "status.bursting"),
                        Scp149Host.secondsLeft(Current), Current.Brood()), false);
            } else {
                Source.sendSuccess(() -> Component.translatable(Prefix + "status.clear"), false);
            }
        }
        return Mth.clamp(Data.count(), 0, Scp149World.Cap);
    }
}
