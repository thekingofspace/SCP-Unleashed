package net.rai.scp_unleashed.scp131;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp131Commands {
    public static final String Prefix = "commands.scp_unleashed.scp131.";
    public static final int DefaultSeconds = 300;
    public static final int MaxSeconds = 3600;
    public static final double CountRadius = 128.0;
    public static final double FamilySpread = 1.1;
    private static final SuggestionProvider<CommandSourceStack> Colours = (Context, Builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(Scp131Colour.values()).map(Scp131Colour::getSerializedName), Builder);

    private Scp131Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        LiteralArgumentBuilder<CommandSourceStack> Root = Commands.literal("scp131").requires(Source -> Source.hasPermission(2));
        Root.then(Commands.literal("spawn")
                .executes(Context -> spawn(Context.getSource(), null))
                .then(Commands.argument("colour", StringArgumentType.word()).suggests(Colours)
                        .executes(Context -> spawn(Context.getSource(), StringArgumentType.getString(Context, "colour")))));
        Root.then(Commands.literal("family").executes(Context -> family(Context.getSource())));
        Root.then(Commands.literal("grieve")
                .executes(Context -> grieve(Context.getSource(), DefaultSeconds))
                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, MaxSeconds))
                        .executes(Context -> grieve(Context.getSource(), IntegerArgumentType.getInteger(Context, "seconds")))));
        Root.then(Commands.literal("cheer").executes(Context -> cheer(Context.getSource())));
        Root.then(Commands.literal("status").executes(Context -> status(Context.getSource())));
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId).then(Root));
    }

    private static UUID caller(CommandSourceStack Source) {
        return Source.getEntity() != null ? Source.getEntity().getUUID() : new UUID(0L, 0L);
    }

    private static int spawn(CommandSourceStack Source, String Name) {
        ServerLevel World = Source.getLevel();
        Scp131Colour Colour;
        if (Name == null) {
            Colour = Scp131Colour.random(World.random);
        } else {
            Colour = Scp131Colour.ColourCodec.byName(Name);
            if (Colour == null) {
                Source.sendFailure(Component.translatable(Prefix + "unknown_colour", Name));
                return 0;
            }
        }
        Vec3 Spot = Source.getPosition();
        Vec3 Toward = Source.getEntity() != null ? Spot.add(Source.getEntity().getLookAngle()) : Spot.add(0.0, 0.0, 1.0);
        if (Scp131ArrivalCondition.place(World, Spot, Colour, caller(Source), Toward).isEmpty()) {
            Source.sendFailure(Component.translatable(Prefix + "spawn.failed"));
            return 0;
        }
        Scp131Colour Made = Colour;
        Source.sendSuccess(() -> Component.translatable(Prefix + "spawn.success", Made.displayName()), true);
        return 1;
    }

    private static int family(CommandSourceStack Source) {
        ServerLevel World = Source.getLevel();
        Vec3 Centre = Source.getPosition();
        Vec3 Toward = Source.getEntity() != null ? Source.getEntity().position() : Centre.add(0.0, 0.0, 3.0);
        int Spawned = 0;
        Scp131Colour[] All = Scp131Colour.values();
        for (int Index = 0; Index < All.length; Index++) {
            double Turn = Index * Math.PI * 2.0 / All.length;
            Vec3 Spot = Centre.add(Math.cos(Turn) * FamilySpread, 0.0, Math.sin(Turn) * FamilySpread);
            if (Scp131ArrivalCondition.place(World, Spot, All[Index], caller(Source), Toward).isPresent()) {
                Spawned++;
            }
        }
        if (Spawned == 0) {
            Source.sendFailure(Component.translatable(Prefix + "spawn.failed"));
            return 0;
        }
        int Count = Spawned;
        Source.sendSuccess(() -> Component.translatable(Prefix + "family.success", Count), true);
        return Spawned;
    }

    private static int grieve(CommandSourceStack Source, int Seconds) {
        ServerLevel World = Source.getLevel();
        Scp131Grief.get(World.getServer()).grieve(World.getServer(), World.getGameTime(), Seconds * 20, World, Source.getPosition());
        Source.sendSuccess(() -> Component.translatable(Prefix + "grieve.success", Seconds), true);
        return Seconds;
    }

    private static int cheer(CommandSourceStack Source) {
        ServerLevel World = Source.getLevel();
        Scp131Grief.get(World.getServer()).cheer(World.getServer());
        Source.sendSuccess(() -> Component.translatable(Prefix + "cheer.success"), true);
        return 1;
    }

    private static int status(CommandSourceStack Source) {
        ServerLevel World = Source.getLevel();
        Scp131Grief Grief = Scp131Grief.get(World.getServer());
        long Left = Grief.remaining(World.getGameTime());
        Vec3 Centre = Source.getPosition();
        List<Scp131Pod> Pods = World.getEntitiesOfClass(Scp131Pod.class, new AABB(Centre, Centre).inflate(CountRadius), Scp131Pod::isAlive);
        Map<Scp131Colour, Integer> Counts = new EnumMap<>(Scp131Colour.class);
        for (Scp131Pod Pod : Pods) {
            Counts.merge(Pod.getColour(), 1, Integer::sum);
        }
        MutableComponent Tally = Component.empty();
        boolean First = true;
        for (Map.Entry<Scp131Colour, Integer> Entry : Counts.entrySet()) {
            if (!First) {
                Tally.append(Component.literal(", "));
            }
            First = false;
            Tally.append(Entry.getKey().displayName()).append(Component.literal(" " + Entry.getValue()));
        }
        int Total = Pods.size();
        Source.sendSuccess(() -> Component.translatable(Prefix + "status.pods", Total, (int) CountRadius, Tally), false);
        if (Left > 0L) {
            Source.sendSuccess(() -> Component.translatable(Prefix + "status.grieving", (Left + 19L) / 20L), false);
        } else {
            Source.sendSuccess(() -> Component.translatable(Prefix + "status.cheerful"), false);
        }
        return Total;
    }
}
