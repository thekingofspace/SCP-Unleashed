package net.rai.scp_unleashed.scp079;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp079Commands {
    public static final String Prefix = "commands.scp_unleashed.scp079.";
    public static final int SearchRadius = 32;
    public static final int MoodTicks = 200;

    private Scp079Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp079")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("prank")
                                .executes(Context -> prank(Context.getSource(), null))
                                .then(Commands.argument("kind", StringArgumentType.word())
                                        .suggests((Context, Builder) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(Scp079Pranks.Kind.values()).map(Scp079Pranks.Kind::getSerializedName), Builder))
                                        .executes(Context -> prank(Context.getSource(), StringArgumentType.getString(Context, "kind")))))
                        .then(Commands.literal("mood")
                                .then(Commands.argument("face", StringArgumentType.word())
                                        .suggests((Context, Builder) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(Scp079Mood.values()).filter(Scp079Mood::isFace).map(Scp079Mood::getSerializedName), Builder))
                                        .executes(Context -> mood(Context.getSource(), StringArgumentType.getString(Context, "face")))))
                        .then(Commands.literal("broadcast")
                                .executes(Context -> broadcast(Context.getSource())))
                        .then(Commands.literal("exposure")
                                .then(Commands.argument("targets", EntityArgument.entities())
                                        .executes(Context -> showExposure(Context.getSource(), EntityArgument.getEntities(Context, "targets")))
                                        .then(Commands.literal("clear")
                                                .executes(Context -> setExposure(Context.getSource(), EntityArgument.getEntities(Context, "targets"), 0.0f)))
                                        .then(Commands.argument("value", FloatArgumentType.floatArg(0.0f, Scp079Exposure.MaxExposure))
                                                .executes(Context -> setExposure(Context.getSource(), EntityArgument.getEntities(Context, "targets"),
                                                        FloatArgumentType.getFloat(Context, "value"))))))));
    }

    @Nullable
    private static Scp079BlockEntity nearest(CommandSourceStack Source) {
        ServerLevel World = Source.getLevel();
        Vec3 From = Source.getPosition();
        BlockPos Centre = BlockPos.containing(From);
        Scp079BlockEntity Best = null;
        double BestDistance = Double.MAX_VALUE;
        int Chunks = SearchRadius / 16 + 1;
        for (int Dx = -Chunks; Dx <= Chunks; Dx++) {
            for (int Dz = -Chunks; Dz <= Chunks; Dz++) {
                int ChunkX = (Centre.getX() >> 4) + Dx;
                int ChunkZ = (Centre.getZ() >> 4) + Dz;
                if (!World.hasChunk(ChunkX, ChunkZ)) {
                    continue;
                }
                for (BlockEntity Found : World.getChunk(ChunkX, ChunkZ).getBlockEntities().values()) {
                    if (Found instanceof Scp079BlockEntity Computer) {
                        double Distance = Vec3.atCenterOf(Computer.getBlockPos()).distanceToSqr(From);
                        if (Distance < BestDistance && Distance <= SearchRadius * SearchRadius) {
                            Best = Computer;
                            BestDistance = Distance;
                        }
                    }
                }
            }
        }
        if (Best == null) {
            Source.sendFailure(Component.translatable(Prefix + "none", SearchRadius));
        }
        return Best;
    }

    private static int prank(CommandSourceStack Source, @Nullable String KindName) {
        Scp079Pranks.Kind Forced = KindName == null ? null : Scp079Pranks.Kind.byName(KindName);
        if (KindName != null && Forced == null) {
            Source.sendFailure(Component.translatable(Prefix + "prank.unknown", KindName));
            return 0;
        }
        Scp079BlockEntity Computer = nearest(Source);
        if (Computer == null) {
            return 0;
        }
        if (!Computer.isPowered()) {
            Source.sendFailure(Component.translatable(Prefix + "unpowered"));
            return 0;
        }
        ServerLevel World = Source.getLevel();
        Scp079Pranks.Result Done = Scp079Pranks.attempt(World, Computer, World.getRandom(), Forced);
        if (Done == null) {
            Source.sendFailure(Component.translatable(Prefix + "prank.nothing"));
            return 0;
        }
        Computer.recordPrank(World, World.getGameTime(), Done);
        Source.sendSuccess(() -> Component.translatable(Prefix + "prank.done", Done.Chosen().getSerializedName(), Done.Touched()), true);
        return 1;
    }

    private static int mood(CommandSourceStack Source, String Name) {
        Scp079Mood Wanted = Scp079Mood.byName(Name);
        if (Wanted == null || !Wanted.isFace()) {
            Source.sendFailure(Component.translatable(Prefix + "mood.unknown", Name));
            return 0;
        }
        Scp079BlockEntity Computer = nearest(Source);
        if (Computer == null) {
            return 0;
        }
        if (!Computer.isPowered()) {
            Source.sendFailure(Component.translatable(Prefix + "unpowered"));
            return 0;
        }
        Computer.forceMood(Wanted, MoodTicks);
        Source.sendSuccess(() -> Component.translatable(Prefix + "mood.done", Wanted.getSerializedName(), MoodTicks / 20), true);
        return 1;
    }

    private static int broadcast(CommandSourceStack Source) {
        Scp079BlockEntity Computer = nearest(Source);
        if (Computer == null) {
            return 0;
        }
        if (!Computer.isPowered()) {
            Source.sendFailure(Component.translatable(Prefix + "unpowered"));
            return 0;
        }
        ServerLevel World = Source.getLevel();
        Computer.startBroadcast(World, World.getGameTime());
        Source.sendSuccess(() -> Component.translatable(Prefix + "broadcast.done"), true);
        return 1;
    }

    private static int showExposure(CommandSourceStack Source, Collection<? extends Entity> Targets) {
        int Count = 0;
        for (Entity Target : Targets) {
            if (Target instanceof LivingEntity Living) {
                float Value = Scp079Exposure.exposure(Living);
                Source.sendSuccess(() -> Component.translatable(Prefix + "exposure.show", Living.getDisplayName(),
                        String.format("%.1f", Value), Scp079Exposure.stage(Value)), false);
                Count++;
            }
        }
        return Count;
    }

    private static int setExposure(CommandSourceStack Source, Collection<? extends Entity> Targets, float Value) {
        int Count = 0;
        for (Entity Target : Targets) {
            if (Target instanceof LivingEntity Living) {
                if (Value <= 0.0f) {
                    Scp079Exposure.clear(Living);
                } else {
                    Scp079Exposure.set(Living, Value);
                }
                Count++;
            }
        }
        int Changed = Count;
        Source.sendSuccess(() -> Component.translatable(Prefix + "exposure.set", Changed, String.format("%.1f", Value)), true);
        return Count;
    }
}
