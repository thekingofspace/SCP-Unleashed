package net.rai.scp_unleashed.dclass;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.Scp_unleashed;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public final class DClassCommands {
    public static final String Prefix = "commands.scp_unleashed.dclass.";
    private static final SimpleCommandExceptionType Failed = new SimpleCommandExceptionType(Component.translatable(Prefix + "spawn.failed"));

    private DClassCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> Dispatcher) {
        Dispatcher.register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("dclass")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("spawn")
                                .executes(Context -> spawn(Context.getSource(), null, null, 0, 0))
                                .then(Commands.argument("trait", StringArgumentType.word())
                                        .suggests((Context, Builder) -> SharedSuggestionProvider.suggest(Arrays.stream(DClassTrait.values()).map(DClassTrait::getId), Builder))
                                        .executes(Context -> spawn(Context.getSource(), StringArgumentType.getString(Context, "trait"), null, 0, 0))
                                        .then(Commands.argument("crime", StringArgumentType.word())
                                                .suggests((Context, Builder) -> SharedSuggestionProvider.suggest(Arrays.stream(DClassCrime.values()).map(DClassCrime::getId), Builder))
                                                .executes(Context -> spawn(Context.getSource(), StringArgumentType.getString(Context, "trait"),
                                                        StringArgumentType.getString(Context, "crime"), 0, 0))
                                                .then(Commands.argument("iq", IntegerArgumentType.integer(DClassScores.IqLow, DClassScores.IqHigh))
                                                        .executes(Context -> spawn(Context.getSource(), StringArgumentType.getString(Context, "trait"),
                                                                StringArgumentType.getString(Context, "crime"), IntegerArgumentType.getInteger(Context, "iq"), 0))
                                                        .then(Commands.argument("athletic", IntegerArgumentType.integer(DClassScores.AthleticLow, DClassScores.AthleticHigh))
                                                                .executes(Context -> spawn(Context.getSource(), StringArgumentType.getString(Context, "trait"),
                                                                        StringArgumentType.getString(Context, "crime"), IntegerArgumentType.getInteger(Context, "iq"),
                                                                        IntegerArgumentType.getInteger(Context, "athletic"))))))))));
    }

    private static int spawn(CommandSourceStack Source, @Nullable String TraitName, @Nullable String CrimeName, int Iq, int Athletic) throws CommandSyntaxException {
        ServerLevel World = Source.getLevel();
        DClass Made = DClassRegistry.DClassType.get().create(World);
        if (Made == null) {
            throw Failed.create();
        }
        DClassTrait Trait = TraitName == null ? DClassTrait.random(World.getRandom()) : DClassTrait.byId(TraitName);
        DClassCrime Crime = CrimeName == null ? DClassCrime.random(World.getRandom()) : DClassCrime.byId(CrimeName);
        Made.setScores(Iq, Athletic);
        Made.identify(Trait, Crime, DClassAppearance.random(World.getRandom()));
        Vec3 At = Source.getPosition();
        float Yaw = Source.getRotation().y;
        Made.moveTo(At.x, At.y, At.z, Yaw + 180.0f, 0.0f);
        Made.setYHeadRot(Yaw + 180.0f);
        Made.finalizeSpawn(World, World.getCurrentDifficultyAt(Made.blockPosition()), MobSpawnType.COMMAND, null);
        if (!World.addFreshEntity(Made)) {
            throw Failed.create();
        }
        Source.sendSuccess(() -> Component.translatable(Prefix + "spawn.success", Trait.getName(), Crime.getName(), Made.iq(), Made.athletic()), true);
        return 1;
    }
}
