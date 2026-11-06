package net.rai.scp_unleashed.scp001;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp1730.entity.RealityAnchorField;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp001Commands {
    public static final String Prefix = "commands.scp_unleashed.scp001.";
    public static final double SearchRadius = 64.0;

    private Scp001Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        LiteralArgumentBuilder<CommandSourceStack> Root = Commands.literal("scp001").requires(Source -> Source.hasPermission(2));
        Root.then(Commands.literal("arrive").executes(Context -> arrive(Context.getSource())));
        Root.then(Commands.literal("anchor").executes(Context -> anchor(Context.getSource())));
        Root.then(Commands.literal("status").executes(Context -> status(Context.getSource())));
        Root.then(Commands.literal("escape").executes(Context -> escape(Context.getSource())));
        Root.then(Commands.literal("returns").executes(Context -> returns(Context.getSource())));
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId).then(Root));
    }

    @Nullable
    private static Scp001Prototype nearest(CommandSourceStack Source) {
        Vec3 Centre = Source.getPosition();
        List<Scp001Prototype> Found = Source.getLevel().getEntitiesOfClass(Scp001Prototype.class, new AABB(Centre, Centre).inflate(SearchRadius), Entity::isAlive);
        return Found.stream().min(Comparator.comparingDouble(Creature -> Creature.distanceToSqr(Centre))).orElse(null);
    }

    private static int arrive(CommandSourceStack Source) {
        ServerLevel World = Source.getLevel();
        if (!(Source.getEntity() instanceof ServerPlayer Reader)) {
            Source.sendFailure(Component.translatable(Prefix + "player"));
            return 0;
        }
        Optional<BlockPos> Spot = Scp001ArrivalCondition.arrivalSpot(World, Reader, 6, 12, World.getRandom());
        Scp001Prototype Creature = Scp001Registry.Prototype.get().create(World);
        if (Spot.isEmpty() || Creature == null) {
            Source.sendFailure(Component.translatable(Prefix + "no_room"));
            return 0;
        }
        Vec3 Toward = Reader.position().subtract(Vec3.atBottomCenterOf(Spot.get()));
        float Yaw = (float) Math.toDegrees(Math.atan2(Toward.z, Toward.x)) - 90.0f;
        if (!Scp001Returns.emerge(World, Creature, Spot.get(), Yaw)) {
            Source.sendFailure(Component.translatable(Prefix + "no_room"));
            return 0;
        }
        Source.sendSuccess(() -> Component.translatable(Prefix + "arrived"), true);
        return 1;
    }

    private static int anchor(CommandSourceStack Source) {
        RealityAnchorField.spawn(Source.getLevel(), Source.getPosition().add(0.0, 1.0, 0.0));
        Source.sendSuccess(() -> Component.translatable(Prefix + "anchor"), true);
        return 1;
    }

    private static int escape(CommandSourceStack Source) {
        Scp001Prototype Creature = nearest(Source);
        if (Creature == null) {
            Source.sendFailure(Component.translatable(Prefix + "none"));
            return 0;
        }
        Creature.forceEscape();
        return 1;
    }

    private static int returns(CommandSourceStack Source) {
        int Count = Scp001Returns.get(Source.getLevel()).pending().size();
        Source.sendSuccess(() -> Component.translatable(Prefix + "returns", Count), false);
        return Count;
    }

    private static int status(CommandSourceStack Source) {
        Scp001Prototype Creature = nearest(Source);
        if (Creature == null) {
            Source.sendFailure(Component.translatable(Prefix + "none"));
            return 0;
        }
        LivingEntity Target = Creature.getTarget();
        Component Hunting = Target == null ? Component.translatable(Prefix + "status.nothing") : Target.getDisplayName();
        Source.sendSuccess(() -> Component.translatable(Prefix + "status", Math.round(Creature.getHealth()), Math.round(Creature.getMaxHealth()), Hunting,
                Component.translatable(Prefix + (Creature.isLocked() ? "status.locked" : Creature.isRestrained() ? "status.restrained" : "status.free"))), false);
        return 1;
    }
}
