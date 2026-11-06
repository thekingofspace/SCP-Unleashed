package net.rai.scp_unleashed.rift;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.FragmentRarity;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class RiftCommands {
    public static final int DefaultClearRadius = 64;
    public static final double LookReach = 48.0;
    private static final DynamicCommandExceptionType UnknownRarity = new DynamicCommandExceptionType(
            Name -> Component.translatable("commands.scp_unleashed.rift.spawn.unknown", Name));

    private RiftCommands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("rift")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("spawn")
                                .executes(Context -> spawn(Context.getSource(), null))
                                .then(Commands.argument("rarity", StringArgumentType.word())
                                        .suggests(RiftCommands::suggestRarities)
                                        .executes(Context -> spawn(Context.getSource(), StringArgumentType.getString(Context, "rarity")))))
                        .then(Commands.literal("clear")
                                .executes(Context -> clear(Context.getSource(), DefaultClearRadius))
                                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 4096))
                                        .executes(Context -> clear(Context.getSource(), IntegerArgumentType.getInteger(Context, "radius")))))));
    }

    private static CompletableFuture<Suggestions> suggestRarities(CommandContext<CommandSourceStack> Context, SuggestionsBuilder Builder) {
        List<String> Names = Arrays.stream(FragmentRarity.values()).map(FragmentRarity::getSerializedName).toList();
        return SharedSuggestionProvider.suggest(Names, Builder);
    }

    private static FragmentRarity parseRarity(String Name) throws CommandSyntaxException {
        String Wanted = Name.toLowerCase(Locale.ROOT);
        for (FragmentRarity Rarity : FragmentRarity.values()) {
            if (Rarity.getSerializedName().equals(Wanted)) {
                return Rarity;
            }
        }
        throw UnknownRarity.create(Name);
    }

    private static int spawn(CommandSourceStack Source, String Name) throws CommandSyntaxException {
        ServerLevel World = Source.getLevel();
        if (!RiftFormation.allowedIn(World)) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.rift.spawn.overworld"));
            return 0;
        }
        FragmentRarity Rarity = Name == null ? RiftFormation.rollRarity(World.random) : parseRarity(Name);
        Vec3 Bottom = placement(World, Source, Rarity);
        RiftEntity Rift = RiftRegistry.Rift.get().create(World);
        if (Rift == null) {
            return 0;
        }
        Rift.setRarity(Rarity);
        Rift.moveTo(Bottom.x, Bottom.y, Bottom.z, World.random.nextFloat() * 360.0f, 0.0f);
        if (!World.addFreshEntity(Rift)) {
            return 0;
        }
        Component RarityName = Component.translatable("commands.scp_unleashed.rift.rarity." + Rarity.getSerializedName());
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.rift.spawn.success", RarityName,
                String.format(Locale.ROOT, "%.1f", Bottom.x), String.format(Locale.ROOT, "%.1f", Bottom.y), String.format(Locale.ROOT, "%.1f", Bottom.z)), true);
        return Rarity.getTier();
    }

    private static Vec3 placement(ServerLevel World, CommandSourceStack Source, FragmentRarity Rarity) {
        float Height = RiftEntity.heightOf(Rarity);
        Entity Viewer = Source.getEntity();
        if (Viewer == null) {
            return Source.getPosition();
        }
        Vec3 Eye = Viewer.getEyePosition();
        HitResult Hit = Viewer.pick(LookReach, 1.0f, false);
        Vec3 Bottom;
        if (Hit instanceof BlockHitResult Struck && Hit.getType() == HitResult.Type.BLOCK) {
            Direction Side = Struck.getDirection();
            Vec3 Point = Struck.getLocation();
            double Clearance = Height * RiftEntity.WidthRatio * 0.5 + 0.25;
            if (Side == Direction.UP) {
                Bottom = new Vec3(Point.x, Point.y + 0.35, Point.z);
            } else if (Side == Direction.DOWN) {
                Bottom = new Vec3(Point.x, Point.y - Height - 0.3, Point.z);
            } else {
                Bottom = new Vec3(Point.x + Side.getStepX() * Clearance, Point.y - Height * 0.5, Point.z + Side.getStepZ() * Clearance);
            }
        } else {
            Bottom = Eye.add(Viewer.getViewVector(1.0f).scale(6.0)).subtract(0.0, Height * 0.5, 0.0);
        }
        for (int Step = 0; Step <= 12; Step++) {
            double Lift = (Step % 2 == 0 ? Step / 2 : -(Step + 1) / 2) * 0.5;
            Vec3 Candidate = Bottom.add(0.0, Lift, 0.0);
            if (World.noCollision(RiftEntity.boxAt(Candidate, Rarity).inflate(0.05))) {
                return Candidate;
            }
        }
        return Bottom;
    }

    private static int clear(CommandSourceStack Source, int Radius) {
        ServerLevel World = Source.getLevel();
        Vec3 Center = Source.getPosition();
        double Reach = (double) Radius * Radius;
        List<? extends RiftEntity> Rifts = World.getEntities(RiftRegistry.Rift.get(), new AABB(Center, Center).inflate(Radius), Rift -> Rift.distanceToSqr(Center) <= Reach);
        int Count = Rifts.size();
        Rifts.forEach(Entity::discard);
        if (Count == 0) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.rift.clear.none", Radius));
        } else if (Count == 1) {
            Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.rift.clear.single"), true);
        } else {
            Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.rift.clear.multiple", Count), true);
        }
        return Count;
    }
}
