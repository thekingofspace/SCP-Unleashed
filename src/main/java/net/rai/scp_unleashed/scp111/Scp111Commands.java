package net.rai.scp_unleashed.scp111;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.storage.StorageContainerBlockEntity;
import net.rai.scp_unleashed.storage.StorageContainers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp111Commands {
    public static final String Prefix = "commands.scp_unleashed.scp111.";
    public static final int DefaultRadius = 16;
    public static final int MaxRadius = 64;
    public static final int MaxEggs = 64;
    private static final SuggestionProvider<CommandSourceStack> Variants = (Context, Builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(Scp111Variant.values()).map(Scp111Variant::getSerializedName), Builder);

    private Scp111Commands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        LiteralArgumentBuilder<CommandSourceStack> Root = Commands.literal("scp111").requires(Source -> Source.hasPermission(2));
        Root.then(Commands.literal("spawn")
                .executes(Context -> spawn(Context, null, false))
                .then(Commands.argument("variant", StringArgumentType.word()).suggests(Variants)
                        .executes(Context -> spawn(Context, StringArgumentType.getString(Context, "variant"), false))
                        .then(Commands.argument("baby", BoolArgumentType.bool())
                                .executes(Context -> spawn(Context, StringArgumentType.getString(Context, "variant"), BoolArgumentType.getBool(Context, "baby"))))));
        Root.then(Commands.literal("lay").executes(Scp111Commands::lay));
        Root.then(Commands.literal("hatch")
                .executes(Context -> hatch(Context.getSource(), DefaultRadius))
                .then(Commands.argument("radius", IntegerArgumentType.integer(1, MaxRadius))
                        .executes(Context -> hatch(Context.getSource(), IntegerArgumentType.getInteger(Context, "radius")))));
        Root.then(Commands.literal("box").executes(Context -> box(Context.getSource())));
        Root.then(Commands.literal("eggs")
                .then(Commands.argument("variant", StringArgumentType.word()).suggests(Variants)
                        .executes(Context -> eggs(Context.getSource(), StringArgumentType.getString(Context, "variant"), Scp111EggItem.StackSize))
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, MaxEggs))
                                .executes(Context -> eggs(Context.getSource(), StringArgumentType.getString(Context, "variant"), IntegerArgumentType.getInteger(Context, "count"))))));
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId).then(Root));
    }

    private static Scp111Variant parse(CommandSourceStack Source, String Name) {
        Scp111Variant Found = Scp111Variant.VariantCodec.byName(Name);
        if (Found == null) {
            Source.sendFailure(Component.translatable(Prefix + "unknown_variant", Name));
        }
        return Found;
    }

    private static int spawn(CommandContext<CommandSourceStack> Context, String Name, boolean Baby) {
        CommandSourceStack Source = Context.getSource();
        ServerLevel World = Source.getLevel();
        Scp111Variant Variant = Name == null ? Scp111Variant.random(World.random) : parse(Source, Name);
        if (Variant == null) {
            return 0;
        }
        Vec3 Spot = Source.getPosition();
        Scp111Snail Snail = Scp111Registry.Snail.get().create(World);
        if (Snail == null) {
            return 0;
        }
        float Yaw = Source.getEntity() != null ? Source.getEntity().getYRot() + 180.0f : 0.0f;
        Snail.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        Snail.setYBodyRot(Yaw);
        Snail.setYHeadRot(Yaw);
        Snail.finalizeSpawn(World, World.getCurrentDifficultyAt(BlockPos.containing(Spot)), MobSpawnType.COMMAND, null);
        Snail.setVariant(Variant);
        if (Baby) {
            Snail.setAge(-Scp111Snail.GrowTicks);
        }
        if (!World.addFreshEntity(Snail)) {
            Source.sendFailure(Component.translatable(Prefix + "spawn.failed"));
            return 0;
        }
        Source.sendSuccess(() -> Component.translatable(Prefix + "spawn.success", Variant.displayName()), true);
        return 1;
    }

    private static int lay(CommandContext<CommandSourceStack> Context) {
        CommandSourceStack Source = Context.getSource();
        ServerLevel World = Source.getLevel();
        Vec3 Centre = Source.getPosition();
        List<Scp111Snail> Near = World.getEntitiesOfClass(Scp111Snail.class, new AABB(Centre, Centre).inflate(DefaultRadius),
                Found -> Found.isAlive() && !Found.isBaby());
        Scp111Snail Chosen = Near.stream().min(Comparator.comparingDouble(Found -> Found.distanceToSqr(Centre))).orElse(null);
        if (Chosen == null) {
            Source.sendFailure(Component.translatable(Prefix + "lay.none"));
            return 0;
        }
        if (!Chosen.layNow(World, true)) {
            Source.sendFailure(Component.translatable(Prefix + "lay.blocked"));
            return 0;
        }
        Source.sendSuccess(() -> Component.translatable(Prefix + "lay.success", Chosen.getVariant().displayName()), true);
        return 1;
    }

    private static int hatch(CommandSourceStack Source, int Radius) {
        ServerLevel World = Source.getLevel();
        BlockPos Centre = BlockPos.containing(Source.getPosition());
        int Clutches = 0;
        List<BlockPos> Found = new ArrayList<>();
        for (BlockPos Cell : BlockPos.betweenClosed(Centre.offset(-Radius, -Radius, -Radius), Centre.offset(Radius, Radius, Radius))) {
            if (World.isLoaded(Cell) && World.getBlockState(Cell).is(Scp111Registry.EggBlock.get())) {
                Found.add(Cell.immutable());
            }
        }
        for (BlockPos Cell : Found) {
            BlockState State = World.getBlockState(Cell);
            if (State.getBlock() instanceof Scp111EggBlock Clutch) {
                Clutch.hatch(State, World, Cell);
                Clutches++;
            }
        }
        int Ripened = 0;
        for (StorageContainerBlockEntity Locker : StorageContainers.near(World, Centre, Radius)) {
            Ripened += Scp111Shelf.ripen(Locker);
        }
        int Hatched = Clutches;
        int Readied = Ripened;
        Source.sendSuccess(() -> Component.translatable(Prefix + "hatch.success", Hatched, Readied), true);
        return Hatched + Readied;
    }

    private static int box(CommandSourceStack Source) {
        ServerPlayer User = Source.getPlayer();
        if (User == null) {
            Source.sendFailure(Component.translatable(Prefix + "player_only"));
            return 0;
        }
        ItemHandlerHelper.giveItemToPlayer(User, Scp111Registry.BoxItem.get().getDefaultInstance());
        Source.sendSuccess(() -> Component.translatable(Prefix + "box.success"), true);
        return 1;
    }

    private static int eggs(CommandSourceStack Source, String Name, int Count) {
        ServerPlayer User = Source.getPlayer();
        if (User == null) {
            Source.sendFailure(Component.translatable(Prefix + "player_only"));
            return 0;
        }
        Scp111Variant Variant = parse(Source, Name);
        if (Variant == null) {
            return 0;
        }
        int Left = Count;
        while (Left > 0) {
            int Taken = Math.min(Left, Scp111EggItem.StackSize);
            ItemHandlerHelper.giveItemToPlayer(User, new Scp111Egg(Variant, 0).stack(Taken));
            Left -= Taken;
        }
        Source.sendSuccess(() -> Component.translatable(Prefix + "eggs.success", Count, Variant.displayName()), true);
        return Count;
    }
}
