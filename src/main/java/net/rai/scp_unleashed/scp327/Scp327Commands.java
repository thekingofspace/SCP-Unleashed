package net.rai.scp_unleashed.scp327;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp327Commands {
    public static final String Prefix = "commands.scp_unleashed.scp327.";
    public static final double SearchRange = 64.0;
    public static final int ShoalMin = 6;
    public static final int ShoalMax = 48;
    public static final int SingTicks = 1200;

    private Scp327Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp327")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("shoal").executes(Context -> shoal(Context.getSource())))
                        .then(Commands.literal("sing").executes(Context -> sing(Context.getSource())))
                        .then(Commands.literal("pup").executes(Context -> pup(Context.getSource())))
                        .then(Commands.literal("grow").executes(Context -> grow(Context.getSource())))
                        .then(Commands.literal("status").executes(Context -> status(Context.getSource())))));
    }

    @Nullable
    private static Scp327Siren nearest(CommandSourceStack Source, boolean PupsOnly) {
        ServerLevel World = Source.getLevel();
        Vec3 From = Source.getPosition();
        List<Scp327Siren> Found = World.getEntitiesOfClass(Scp327Siren.class, new AABB(From, From).inflate(SearchRange),
                Candidate -> Candidate.isAlive() && (!PupsOnly || Candidate.isPup()));
        return Found.stream().min(Comparator.comparingDouble(Candidate -> Candidate.distanceToSqr(From))).orElse(null);
    }

    private static int shoal(CommandSourceStack Source) {
        ServerLevel World = Source.getLevel();
        Vec3 From = Source.getPosition();
        Optional<Scp327Waters.Body> Water = Scp327Shoal.findWater(World, From, ShoalMin, ShoalMax, World.getRandom());
        if (Water.isEmpty()) {
            Source.sendFailure(Component.translatable(Prefix + "shoal.no_water"));
            return 0;
        }
        List<Scp327Siren> Made = Scp327Shoal.spawn(World, Water.get(), Scp327Shoal.ShoalSize, From, World.getRandom());
        if (Made.isEmpty()) {
            Source.sendFailure(Component.translatable(Prefix + "shoal.failed"));
            return 0;
        }
        BlockPos Centre = Water.get().Centre();
        Source.sendSuccess(() -> Component.translatable(Prefix + "shoal.success", Made.size(), Centre.getX(), Centre.getY(), Centre.getZ()), true);
        return Made.size();
    }

    private static int sing(CommandSourceStack Source) {
        Scp327Siren Creature = nearest(Source, false);
        if (Creature == null || !(Source.getEntity() instanceof LivingEntity Listener)) {
            Source.sendFailure(Component.translatable(Prefix + "none"));
            return 0;
        }
        Creature.forceSong(Listener, SingTicks);
        Source.sendSuccess(() -> Component.translatable(Prefix + "sing.success"), true);
        return 1;
    }

    private static int pup(CommandSourceStack Source) {
        ServerLevel World = Source.getLevel();
        Vec3 Spot = Source.getPosition();
        Scp327Siren Pup = Scp327Registry.Pup.get().create(World);
        if (Pup == null) {
            Source.sendFailure(Component.translatable(Prefix + "pup.failed"));
            return 0;
        }
        Pup.moveTo(Spot.x, Spot.y, Spot.z, Source.getRotation().y, 0.0f);
        Pup.finalizeSpawn(World, World.getCurrentDifficultyAt(BlockPos.containing(Spot)), MobSpawnType.COMMAND, null);
        Pup.setStage(Scp327Siren.StageBaby);
        if (!World.addFreshEntity(Pup)) {
            Source.sendFailure(Component.translatable(Prefix + "pup.failed"));
            return 0;
        }
        Source.sendSuccess(() -> Component.translatable(Prefix + "pup.success"), true);
        return 1;
    }

    private static int grow(CommandSourceStack Source) {
        Scp327Siren Creature = nearest(Source, true);
        if (Creature == null) {
            Source.sendFailure(Component.translatable(Prefix + "grow.none"));
            return 0;
        }
        boolean Teen = Creature.stage() == Scp327Siren.StageBaby;
        Creature.advanceStage(Source.getLevel());
        Source.sendSuccess(() -> Component.translatable(Prefix + (Teen ? "grow.teen" : "grow.adult")), true);
        return 1;
    }

    private static int status(CommandSourceStack Source) {
        Scp327Siren Creature = nearest(Source, false);
        if (Creature == null) {
            Source.sendFailure(Component.translatable(Prefix + "none"));
            return 0;
        }
        LivingEntity Target = Creature.chosen();
        String Stage = Creature.stage() == Scp327Siren.StageBaby ? "baby" : Creature.stage() == Scp327Siren.StageTeen ? "teen" : "adult";
        String Mode = switch (Creature.mode()) {
            case Scp327Siren.ModeSing, Scp327Siren.ModeBeachSing -> "singing";
            case Scp327Siren.ModeBeached, Scp327Siren.ModeHaul -> "beached";
            case Scp327Siren.ModeDrag -> "dragging";
            case Scp327Siren.ModeStranded -> "stranded";
            default -> "swimming";
        };
        long Age = Creature.isPup() ? Creature.stageAge() / 20L : 0L;
        long Left = Creature.isPup() ? Math.max(0L, (Scp327Siren.StageTicks - Creature.stageAge()) / 20L) : 0L;
        Source.sendSuccess(() -> Component.translatable(Prefix + "status", Component.translatable(Prefix + "stage." + Stage), Component.translatable(Prefix + "mode." + Mode),
                Target == null ? Component.translatable(Prefix + "no_target") : Target.getDisplayName(), Creature.kills(),
                Math.round(Creature.moistureShare() * 100.0f), Age, Left), false);
        return 1;
    }
}
