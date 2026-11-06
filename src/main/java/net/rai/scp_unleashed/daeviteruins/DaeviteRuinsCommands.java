package net.rai.scp_unleashed.daeviteruins;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.daeviteruins.plan.RuinPages;
import net.rai.scp_unleashed.daeviteruins.world.DaeviteRuin;
import net.rai.scp_unleashed.daeviteruins.world.DaeviteRuinsPlanner;
import net.rai.scp_unleashed.daeviteruins.world.DaeviteRuinsRuntime;
import net.rai.scp_unleashed.daeviteruins.world.DaeviteRuinsWorld;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class DaeviteRuinsCommands {
    private static final String[] Compass = {"south", "south_west", "west", "north_west", "north", "north_east", "east", "south_east"};

    private DaeviteRuinsCommands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("daevite_ruins")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("build").then(Commands.argument("page", IntegerArgumentType.integer(RuinPages.First, RuinPages.Last))
                                .executes(DaeviteRuinsCommands::build)))
                        .then(Commands.literal("plan").then(Commands.argument("page", IntegerArgumentType.integer(RuinPages.First, RuinPages.Last))
                                .executes(DaeviteRuinsCommands::plan)))
                        .then(Commands.literal("locate").then(Commands.argument("page", IntegerArgumentType.integer(RuinPages.First, RuinPages.Last))
                                .executes(DaeviteRuinsCommands::locate)))
                        .then(Commands.literal("list").executes(DaeviteRuinsCommands::list))));
    }

    public static Component title(int Page) {
        RuinPages.Entry Entry = RuinPages.entry(Page);
        return Component.translatable("daevite_ruins.scp_unleashed.page." + (Entry == null ? "unknown" : Entry.Key()));
    }

    private static Component compass(BlockPos From, BlockPos To) {
        double Angle = Math.atan2(-(To.getX() - From.getX()), To.getZ() - From.getZ());
        int Index = Math.floorMod((int) Math.round(Angle / (Math.PI / 4)), 8);
        return Component.translatable("commands.scp_unleashed.daevite_ruins.direction." + Compass[Index]);
    }

    private static int distance(BlockPos From, BlockPos To) {
        return (int) Math.round(Math.sqrt((double) (To.getX() - From.getX()) * (To.getX() - From.getX()) + (double) (To.getZ() - From.getZ()) * (To.getZ() - From.getZ())));
    }

    private static MutableComponent where(BlockPos Pos) {
        String Teleport = "/tp @s " + Pos.getX() + " " + (Pos.getY() + 1) + " " + Pos.getZ();
        return Component.translatable("commands.scp_unleashed.daevite_ruins.coords", Pos.getX(), Pos.getY(), Pos.getZ()).withStyle(Style -> Style
                .withColor(ChatFormatting.GREEN).withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, Teleport))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.coordinates.tooltip"))));
    }

    private static int build(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        CommandSourceStack Source = Context.getSource();
        ServerPlayer User = Source.getPlayerOrException();
        ServerLevel World = User.serverLevel();
        int Page = IntegerArgumentType.getInteger(Context, "page");
        if (World.dimension() != Level.OVERWORLD) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.daevite_ruins.overworld"));
            return 0;
        }
        BlockPos Feet = User.blockPosition();
        BlockPos Ground = Feet.below();
        for (int Depth = 0; Depth < 24 && World.getBlockState(Ground).getCollisionShape(World, Ground).isEmpty(); Depth++) {
            Ground = Ground.below();
        }
        Direction Front = User.getDirection();
        DaeviteRuin Ruin = new DaeviteRuin();
        Ruin.Page = Page;
        Ruin.Command = true;
        Ruin.Origin = Ground;
        Ruin.Target = Ground;
        Ruin.Created = World.getGameTime();
        DaeviteRuinsWorld.get(Source.getServer()).add(Ruin);
        DaeviteRuinsRuntime.buildHere(World, Ruin, Ground, Front);
        BlockPos Door = Ruin.Entrance;
        for (int Lift = 0; Lift < 16 && !(World.getBlockState(Door).getCollisionShape(World, Door).isEmpty()
                && World.getBlockState(Door.above()).getCollisionShape(World, Door.above()).isEmpty()); Lift++) {
            Door = Door.above();
        }
        Vec3 Look = Vec3.atCenterOf(Ruin.Centre).subtract(Vec3.atCenterOf(Door));
        float Yaw = (float) (Mth.atan2(Look.z, Look.x) * Mth.RAD_TO_DEG) - 90.0f;
        User.teleportTo(World, Door.getX() + 0.5, Door.getY(), Door.getZ() + 0.5, Yaw, 10.0f);
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.daevite_ruins.build.success", Page, title(Page), where(Ruin.Site)), true);
        return 1;
    }

    private static int plan(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        CommandSourceStack Source = Context.getSource();
        ServerPlayer User = Source.getPlayerOrException();
        int Page = IntegerArgumentType.getInteger(Context, "page");
        BlockPos Origin = DaeviteRuinsPlanner.overworldOrigin(User.serverLevel(), User.blockPosition());
        DaeviteRuin Ruin = DaeviteRuinsPlanner.plan(Source.getServer().overworld(), Page, Origin);
        BlockPos Target = Ruin.where();
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.daevite_ruins.plan.success", Page, title(Page), distance(Origin, Target),
                compass(Origin, Target), where(Target)), true);
        return 1;
    }

    private static int locate(CommandContext<CommandSourceStack> Context) throws CommandSyntaxException {
        CommandSourceStack Source = Context.getSource();
        int Page = IntegerArgumentType.getInteger(Context, "page");
        DaeviteRuin Ruin = DaeviteRuinsWorld.get(Source.getServer()).latest(Page);
        if (Ruin == null) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.daevite_ruins.locate.none", Page, title(Page)));
            return 0;
        }
        BlockPos From = BlockPos.containing(Source.getPosition());
        BlockPos Target = Ruin.where();
        Source.sendSuccess(() -> Component.translatable(Ruin.built() ? "commands.scp_unleashed.daevite_ruins.locate.built" : "commands.scp_unleashed.daevite_ruins.locate.planned",
                Page, title(Page), where(Target), distance(From, Target), compass(From, Target)), false);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> Context) {
        CommandSourceStack Source = Context.getSource();
        DaeviteRuinsWorld Data = DaeviteRuinsWorld.get(Source.getServer());
        if (Data.ruins().isEmpty()) {
            Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.daevite_ruins.list.empty"), false);
            return 0;
        }
        BlockPos From = BlockPos.containing(Source.getPosition());
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.daevite_ruins.list.header", Data.ruins().size()), false);
        for (DaeviteRuin Ruin : Data.ruins()) {
            String State = !Ruin.built() ? "planned" : Ruin.Woken ? "woken" : "built";
            Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.daevite_ruins.list.entry", Ruin.Id, Ruin.Page, title(Ruin.Page),
                    Component.translatable("commands.scp_unleashed.daevite_ruins.state." + State), where(Ruin.where()), distance(From, Ruin.where())), false);
        }
        return Data.ruins().size();
    }
}
