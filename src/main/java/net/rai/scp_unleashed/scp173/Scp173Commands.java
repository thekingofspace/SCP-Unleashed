package net.rai.scp_unleashed.scp173;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp131.Scp131Pod;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp173Commands {
    public static final String Prefix = "commands.scp_unleashed.scp173.";
    public static final double SearchRadius = 64.0;
    public static final double SpawnAhead = 3.0;
    public static final int DefaultResidue = 8;
    public static final int MaxResidue = 64;
    public static final int ResidueSpread = 5;

    private Scp173Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        LiteralArgumentBuilder<CommandSourceStack> Root = Commands.literal("scp173").requires(Source -> Source.hasPermission(2));
        Root.then(Commands.literal("spawn").executes(Context -> spawn(Context.getSource())));
        Root.then(Commands.literal("clone").executes(Context -> cloneNearest(Context.getSource())));
        Root.then(Commands.literal("residue")
                .executes(Context -> residue(Context.getSource(), DefaultResidue))
                .then(Commands.argument("count", IntegerArgumentType.integer(1, MaxResidue))
                        .executes(Context -> residue(Context.getSource(), IntegerArgumentType.getInteger(Context, "count")))));
        Root.then(Commands.literal("status").executes(Context -> status(Context.getSource())));
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId).then(Root));
    }

    @Nullable
    private static Scp173Statue nearest(CommandSourceStack Source) {
        Vec3 Centre = Source.getPosition();
        List<Scp173Statue> Found = Source.getLevel().getEntitiesOfClass(Scp173Statue.class, new AABB(Centre, Centre).inflate(SearchRadius), Entity::isAlive);
        return Found.stream().min(Comparator.comparingDouble(Statue -> Statue.distanceToSqr(Centre))).orElse(null);
    }

    private static int spawn(CommandSourceStack Source) {
        ServerLevel World = Source.getLevel();
        Vec3 Spot = Source.getPosition();
        Entity Caller = Source.getEntity();
        if (Caller != null) {
            Vec3 Ahead = Vec3.directionFromRotation(0.0f, Caller.getYRot()).scale(SpawnAhead);
            Spot = Spot.add(Ahead);
        }
        BlockPos Feet = BlockPos.containing(Spot);
        Scp173Statue Statue = Scp173Registry.Statue.get().create(World);
        if (Statue == null) {
            Source.sendFailure(Component.translatable(Prefix + "spawn.failed"));
            return 0;
        }
        Statue.moveTo(Feet.getX() + 0.5, Feet.getY(), Feet.getZ() + 0.5, 0.0f, 0.0f);
        if (!World.noCollision(Statue)) {
            Source.sendFailure(Component.translatable(Prefix + "spawn.failed"));
            return 0;
        }
        Statue.finalizeSpawn(World, World.getCurrentDifficultyAt(Feet), MobSpawnType.COMMAND, null);
        if (Caller != null) {
            Statue.faceTowards(Caller.position());
        }
        World.addFreshEntity(Statue);
        Source.sendSuccess(() -> Component.translatable(Prefix + "spawn.success", Feet.getX(), Feet.getY(), Feet.getZ()), true);
        return 1;
    }

    private static int cloneNearest(CommandSourceStack Source) {
        Scp173Statue Statue = nearest(Source);
        if (Statue == null) {
            Source.sendFailure(Component.translatable(Prefix + "none", (int) SearchRadius));
            return 0;
        }
        Scp173Statue Made = Scp173Clones.spawnFrom(Source.getLevel(), Statue, true);
        if (Made == null) {
            Source.sendFailure(Component.translatable(Prefix + "clone.failed"));
            return 0;
        }
        BlockPos At = Made.blockPosition();
        Source.sendSuccess(() -> Component.translatable(Prefix + "clone.success", At.getX(), At.getY(), At.getZ(), Scp173Clones.get(Source.getServer()).count()), true);
        return 1;
    }

    private static int residue(CommandSourceStack Source, int Count) {
        ServerLevel World = Source.getLevel();
        Scp173Statue Statue = nearest(Source);
        BlockPos Centre = Statue != null ? Statue.blockPosition() : BlockPos.containing(Source.getPosition());
        int Placed = 0;
        for (int Attempt = 0; Attempt < Count * 8 && Placed < Count; Attempt++) {
            BlockPos Spot = Centre.offset(World.random.nextInt(ResidueSpread * 2 + 1) - ResidueSpread, 0, World.random.nextInt(ResidueSpread * 2 + 1) - ResidueSpread);
            for (int Dy = 2; Dy >= -2; Dy--) {
                if (Scp173Residue.place(World, Spot.above(Dy))) {
                    Placed++;
                    break;
                }
            }
        }
        int Made = Placed;
        if (Made == 0) {
            Source.sendFailure(Component.translatable(Prefix + "residue.failed"));
            return 0;
        }
        Source.sendSuccess(() -> Component.translatable(Prefix + "residue.success", Made), true);
        return Made;
    }

    private static int status(CommandSourceStack Source) {
        ServerLevel World = Source.getLevel();
        Scp173Statue Statue = nearest(Source);
        Scp173Clones Data = Scp173Clones.get(Source.getServer());
        if (Statue == null) {
            Source.sendSuccess(() -> Component.translatable(Prefix + "none", (int) SearchRadius), false);
            Source.sendSuccess(() -> Component.translatable(Prefix + "status.clones", Data.count()), false);
            return 0;
        }
        BlockPos At = Statue.blockPosition();
        Component Kind = Component.translatable(Prefix + (Statue.isClone() ? "status.clone" : "status.original"));
        Source.sendSuccess(() -> Component.translatable(Prefix + "status.statue", Kind, At.getX(), At.getY(), At.getZ(), Statue.getPoseIndex()), false);
        List<ServerPlayer> Players = Scp173Sight.playerObservers(World, Statue);
        List<Scp131Pod> Pods = Scp173Sight.podObservers(World, Statue);
        long Now = Scp173Sight.clock(World);
        MutableComponent Names = Component.empty();
        boolean First = true;
        for (ServerPlayer User : Players) {
            if (!First) {
                Names.append(Component.literal(", "));
            }
            First = false;
            Names.append(User.getDisplayName().copy().withStyle(ChatFormatting.WHITE));
            if (!Scp173Sight.reporting(User, Now)) {
                Names.append(Component.translatable(Prefix + "status.estimated").withStyle(ChatFormatting.GRAY));
            }
        }
        if (Players.isEmpty() && Pods.isEmpty()) {
            Source.sendSuccess(() -> Component.translatable(Prefix + "status.unwatched").withStyle(ChatFormatting.RED), false);
        } else {
            Source.sendSuccess(() -> Component.translatable(Prefix + "status.watched", Players.size(), Names, Pods.size()).withStyle(ChatFormatting.GREEN), false);
        }
        int Residue = Scp173Residue.count(World, At, Scp173Hunt.ResidueRadius, Scp173Hunt.ResidueHeight);
        int Percent = Scp173Registry.cloning(Source.getServer().getGameRules()) ? (int) Math.round(Scp173Hunt.cloneChance(Residue) * 100.0) : 0;
        Source.sendSuccess(() -> Component.translatable(Prefix + "status.residue", Residue, Scp173Hunt.ResidueRadius, Percent, Scp173Hunt.CloneCheck / 20), false);
        LivingEntity Target = Statue.hunt().target();
        if (Target != null) {
            Source.sendSuccess(() -> Component.translatable(Prefix + "status.target", Target.getDisplayName()), false);
        }
        if (Statue.isClinging()) {
            Source.sendSuccess(() -> Component.translatable(Prefix + "status.clinging"), false);
        }
        Scp173Course Plan = Statue.hunt().course();
        if (Plan != null && !Plan.isDone()) {
            Source.sendSuccess(() -> Component.translatable(Prefix + (Plan.climbing() ? "status.route.climb" : "status.route.walk"), Plan.remaining()), false);
        }
        Source.sendSuccess(() -> Component.translatable(Prefix + "status.clones", Data.count()), false);
        return Players.size() + Pods.size();
    }
}
