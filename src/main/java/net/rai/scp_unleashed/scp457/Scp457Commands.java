package net.rai.scp_unleashed.scp457;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp457Commands {
    public static final String Prefix = "commands.scp_unleashed.scp457.";
    public static final double DouseRange = 64.0;

    private Scp457Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp457")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("spawn")
                                .executes(Context -> spawn(Context.getSource(), Context.getSource().getPosition()))
                                .then(Commands.argument("pos", Vec3Argument.vec3())
                                        .executes(Context -> spawn(Context.getSource(), Vec3Argument.getVec3(Context, "pos")))))
                        .then(Commands.literal("count")
                                .executes(Context -> count(Context.getSource())))
                        .then(Commands.literal("douse")
                                .executes(Context -> douse(Context.getSource(), false))
                                .then(Commands.literal("all")
                                        .executes(Context -> douse(Context.getSource(), true))))));
    }

    private static int spawn(CommandSourceStack Source, Vec3 Spot) {
        ServerLevel World = Source.getLevel();
        Scp457 Burning = Scp457Registry.Burning.get().create(World);
        if (Burning == null) {
            Source.sendFailure(Component.translatable(Prefix + "spawn.failed"));
            return 0;
        }
        float Yaw = Source.getEntity() != null ? Source.getEntity().getYRot() + 180.0f : 0.0f;
        Burning.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        Burning.setYBodyRot(Yaw);
        Burning.setYHeadRot(Yaw);
        if (!World.noCollision(Burning)) {
            Source.sendFailure(Component.translatable(Prefix + "spawn.failed"));
            return 0;
        }
        Burning.finalizeSpawn(World, World.getCurrentDifficultyAt(Burning.blockPosition()), MobSpawnType.COMMAND, null);
        if (!World.addFreshEntity(Burning)) {
            Source.sendFailure(Component.translatable(Prefix + "spawn.failed"));
            return 0;
        }
        Source.sendSuccess(() -> Component.translatable(Prefix + "spawn.success", (int) Math.floor(Spot.x), (int) Math.floor(Spot.y), (int) Math.floor(Spot.z)), true);
        return 1;
    }

    private static int count(CommandSourceStack Source) {
        ServerLevel World = Source.getLevel();
        int Found = Scp457Spread.count(World);
        Component Rule = Component.translatable(Prefix + (Scp457Registry.spreads(World.getGameRules()) ? "count.on" : "count.off"));
        Source.sendSuccess(() -> Component.translatable(Prefix + "count", Found, Scp457Spread.MaxPerDimension, Rule), false);
        return Found;
    }

    private static int douse(CommandSourceStack Source, boolean Everything) {
        ServerLevel World = Source.getLevel();
        Vec3 Here = Source.getPosition();
        List<Scp457> Found = new ArrayList<>(World.getEntities(Scp457Registry.Burning.get(), Entity::isAlive));
        if (!Everything) {
            Found.removeIf(Burning -> Burning.distanceToSqr(Here) > DouseRange * DouseRange);
            Found.sort(Comparator.comparingDouble(Burning -> Burning.distanceToSqr(Here)));
            if (Found.size() > 1) {
                Found = List.of(Found.get(0));
            }
        }
        if (Found.isEmpty()) {
            Source.sendFailure(Component.translatable(Prefix + "douse.none"));
            return 0;
        }
        for (Scp457 Burning : Found) {
            Burning.douse(World);
        }
        int Count = Found.size();
        Source.sendSuccess(() -> Component.translatable(Prefix + "douse.success", Count), true);
        return Count;
    }
}
