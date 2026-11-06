package net.rai.scp_unleashed.scp352;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
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
import java.util.Optional;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp352Commands {
    public static final String Prefix = "commands.scp_unleashed.scp352.";
    public static final double SearchRadius = 64.0;

    private Scp352Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        LiteralArgumentBuilder<CommandSourceStack> Root = Commands.literal("scp352").requires(Source -> Source.hasPermission(2));
        Root.then(Commands.literal("spawn")
                .executes(Context -> spawn(Context.getSource(), Context.getSource().getPosition()))
                .then(Commands.argument("pos", Vec3Argument.vec3()).executes(Context -> spawn(Context.getSource(), Vec3Argument.getVec3(Context, "pos")))));
        Root.then(Commands.literal("hunt").executes(Context -> hunt(Context.getSource())));
        Root.then(Commands.literal("rest")
                .executes(Context -> rest(Context.getSource(), -1.0))
                .then(Commands.argument("days", DoubleArgumentType.doubleArg(0.0, 8.0)).executes(Context -> rest(Context.getSource(), DoubleArgumentType.getDouble(Context, "days")))));
        Root.then(Commands.literal("wake").executes(Context -> wake(Context.getSource())));
        Root.then(Commands.literal("status").executes(Context -> status(Context.getSource())));
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId).then(Root));
    }

    @Nullable
    public static Scp352Crone nearest(CommandSourceStack Source) {
        Vec3 Centre = Source.getPosition();
        return Source.getLevel().getEntitiesOfClass(Scp352Crone.class, new AABB(Centre, Centre).inflate(SearchRadius), Entity::isAlive).stream()
                .min(Comparator.comparingDouble(Creature -> Creature.distanceToSqr(Centre))).orElse(null);
    }

    private static int spawn(CommandSourceStack Source, Vec3 Spot) {
        float Yaw = Source.getEntity() != null ? Source.getEntity().getYRot() + 180.0f : 0.0f;
        Optional<Scp352Crone> Placed = Scp352Arrival.place(Source.getLevel(), Spot, Yaw);
        if (Placed.isEmpty()) {
            Source.sendFailure(Component.translatable(Prefix + "no_room"));
            return 0;
        }
        Source.sendSuccess(() -> Component.translatable(Prefix + "spawned", (int) Math.floor(Spot.x), (int) Math.floor(Spot.y), (int) Math.floor(Spot.z)), true);
        return 1;
    }

    private static int hunt(CommandSourceStack Source) {
        Scp352Crone Creature = nearest(Source);
        if (Creature == null) {
            Source.sendFailure(Component.translatable(Prefix + "none"));
            return 0;
        }
        LivingEntity Target = Source.getEntity() instanceof LivingEntity Caller && Scp352Prey.prey(Creature, Caller) ? Caller
                : Scp352Prey.nearest(Source.getLevel(), Creature, Scp352Crone.HuntRange, false);
        Creature.beginHunt(Target);
        Source.sendSuccess(() -> Component.translatable(Prefix + "hunt"), true);
        return 1;
    }

    private static int rest(CommandSourceStack Source, double Days) {
        Scp352Crone Creature = nearest(Source);
        if (Creature == null) {
            Source.sendFailure(Component.translatable(Prefix + "none"));
            return 0;
        }
        long Ticks = Days < 0.0 ? -1L : Math.round(Days * Scp352Rest.Day);
        Creature.restNow(Ticks);
        Source.sendSuccess(() -> Component.translatable(Prefix + "rest"), true);
        return 1;
    }

    private static int wake(CommandSourceStack Source) {
        Scp352Crone Creature = nearest(Source);
        if (Creature == null || Creature.phase() != Scp352Phase.Rest) {
            Source.sendFailure(Component.translatable(Prefix + "not_resting"));
            return 0;
        }
        Creature.beginWake();
        Source.sendSuccess(() -> Component.translatable(Prefix + "wake"), true);
        return 1;
    }

    private static int status(CommandSourceStack Source) {
        Scp352Crone Creature = nearest(Source);
        if (Creature == null) {
            Source.sendFailure(Component.translatable(Prefix + "none"));
            return 0;
        }
        Component Phase = Component.translatable(Prefix + "phase." + Creature.phase().word());
        Component Sleep = Creature.phase() == Scp352Phase.Rest ? Component.translatable(Prefix + "status.left", String.format("%.2f", Creature.restLeft() / (double) Scp352Rest.Day))
                : Component.translatable(Prefix + "status.awake");
        Source.sendSuccess(() -> Component.translatable(Prefix + "status", Phase, Math.round(Creature.getHealth()), Math.round(Creature.getMaxHealth()), Creature.kills(), Sleep), false);
        return 1;
    }
}
