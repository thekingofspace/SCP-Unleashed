package net.rai.scp_unleashed.scp346;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp346Commands {
    public static final String Prefix = "commands.scp_unleashed.scp346.";
    public static final String RandomName = "random";
    public static final int MaxCount = 12;

    private Scp346Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scp346")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("flock")
                                .executes(Context -> flock(Context.getSource(), Scp346FlockCondition.FlockSize, RandomName))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, MaxCount))
                                        .executes(Context -> flock(Context.getSource(), IntegerArgumentType.getInteger(Context, "count"), RandomName))
                                        .then(Commands.argument("variant", StringArgumentType.word())
                                                .suggests((Context, Builder) -> SharedSuggestionProvider.suggest(names(), Builder))
                                                .executes(Context -> flock(Context.getSource(), IntegerArgumentType.getInteger(Context, "count"),
                                                        StringArgumentType.getString(Context, "variant"))))))));
    }

    private static Stream<String> names() {
        return Stream.concat(Stream.of(RandomName), Arrays.stream(Scp346Variant.values()).map(Scp346Variant::getSerializedName));
    }

    private static int flock(CommandSourceStack Source, int Count, String Name) {
        @Nullable Scp346Variant Variant = null;
        if (!RandomName.equals(Name)) {
            Variant = Scp346Variant.lookup(Name);
            if (Variant == null) {
                Source.sendFailure(Component.translatable(Prefix + "unknown_variant", Name));
                return 0;
            }
        }
        Vec3 Spot = Source.getPosition();
        List<Scp346Pterosaur> Made = Scp346Flock.spawnFlock(Source.getLevel(), Spot, Count, Variant, Source.getEntity() != null ? Source.getEntity().position() : null, false);
        if (Made.isEmpty()) {
            Source.sendFailure(Component.translatable(Prefix + "flock.failed"));
            return 0;
        }
        Source.sendSuccess(() -> Component.translatable(Prefix + "flock.success", Made.size()), true);
        return Made.size();
    }
}
