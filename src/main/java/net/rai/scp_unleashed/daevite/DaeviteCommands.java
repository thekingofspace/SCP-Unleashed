package net.rai.scp_unleashed.daevite;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.daevite.entity.DaeviteKnight;
import net.rai.scp_unleashed.daevite.entity.DaeviteMob;
import net.rai.scp_unleashed.faction.FactionControl;
import net.rai.scp_unleashed.faction.FactionPhase;
import net.rai.scp_unleashed.faction.FactionSite;
import net.rai.scp_unleashed.faction.FactionState;
import net.rai.scp_unleashed.scp140.api.Scp140PageEvent;
import net.rai.scp_unleashed.scp140.api.Scp140Pages;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class DaeviteCommands {
    public static final int TempleAhead = 40;
    public static final int CampAhead = 22;

    private DaeviteCommands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("daevite")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("awaken")
                                .executes(Context -> page(Context.getSource(), Scp140Pages.Awakening))
                                .then(Commands.literal("here").executes(Context -> awakenHere(Context.getSource()))))
                        .then(Commands.literal("page").then(Commands.argument("page", IntegerArgumentType.integer(Scp140Pages.Awakening, Scp140Pages.Total))
                                .executes(Context -> page(Context.getSource(), IntegerArgumentType.getInteger(Context, "page")))))
                        .then(Commands.literal("camp").then(Commands.literal("here").executes(Context -> campHere(Context.getSource()))))
                        .then(Commands.literal("ritual").then(Commands.literal("now").executes(Context -> ritualNow(Context.getSource()))))
                        .then(Commands.literal("energy")
                                .executes(Context -> energy(Context.getSource()))
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0, DaeviteFaction.MaxPower))
                                        .executes(DaeviteCommands::setEnergy)))
                        .then(Commands.literal("killall").executes(Context -> killAll(Context.getSource())))
                        .then(Commands.literal("knight")
                                .then(Commands.literal("spear").executes(Context -> knight(Context.getSource(), DaeviteKnight.Spear)))
                                .then(Commands.literal("sword").executes(Context -> knight(Context.getSource(), DaeviteKnight.Sword))))));
    }

    private static FactionState state(CommandSourceStack Source) {
        return FactionControl.state(Source.getServer(), DaeviteFaction.Instance);
    }

    private static BlockPos ahead(CommandSourceStack Source, int Distance) {
        Entity Caller = Source.getEntity();
        Vec3 Look = Caller != null ? Caller.getLookAngle().multiply(1.0, 0.0, 1.0) : new Vec3(0.0, 0.0, 1.0);
        if (Look.lengthSqr() < 1.0e-4) {
            Look = new Vec3(0.0, 0.0, 1.0);
        }
        return BlockPos.containing(Source.getPosition().add(Look.normalize().scale(Distance)));
    }

    private static int page(CommandSourceStack Source, int Number) {
        ServerLevel World = Source.getLevel();
        Scp140PageEvent Fired = Scp140PageEvent.fire(World, Number, BlockPos.containing(Source.getPosition()));
        BlockPos Site = Fired.getSite();
        if (Site == null) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.daevite.page_none", Number));
            return 0;
        }
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.daevite.page", Number, Site.getX(), Site.getZ(),
                Fired.getSiteDimension() != null ? Fired.getSiteDimension().location().toString() : "?"), true);
        return 1;
    }

    private static int awakenHere(CommandSourceStack Source) {
        ServerLevel World = Source.getLevel();
        if (World.dimension() != Level.OVERWORLD) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.daevite.overworld"));
            return 0;
        }
        FactionState State = state(Source);
        if (State.phase() == FactionPhase.Defeated) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.faction.defeated", DaeviteFaction.Instance.displayName()));
            return 0;
        }
        BlockPos Here = BlockPos.containing(Source.getPosition());
        FactionControl.awaken(World, DaeviteFaction.Instance, Here, Source.getEntity());
        FactionSite Temple = DaeviteSites.templeSite(State);
        if (Temple == null) {
            Temple = DaeviteSites.planTemple(World, State, Here);
        }
        if (Temple.built()) {
            BlockPos At = Temple.anchor();
            Source.sendFailure(Component.translatable("commands.scp_unleashed.daevite.temple_exists", At.getX(), At.getY(), At.getZ()));
            return 0;
        }
        BlockPos Target = ahead(Source, TempleAhead);
        if (!DaeviteSites.buildNow(World, Temple, Target, Source.getPosition())) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.daevite.no_plan"));
            return 0;
        }
        DaeviteSites.garrison(World, State, Temple);
        Temple.setLastReinforce(World.getGameTime());
        State.changed();
        BlockPos At = Temple.anchor();
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.daevite.temple_built", At.getX(), At.getY(), At.getZ()), true);
        return 1;
    }

    private static int campHere(CommandSourceStack Source) {
        ServerLevel World = Source.getLevel();
        if (World.dimension() != Level.OVERWORLD) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.daevite.overworld"));
            return 0;
        }
        FactionState State = state(Source);
        if (State.phase() == FactionPhase.Defeated) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.faction.defeated", DaeviteFaction.Instance.displayName()));
            return 0;
        }
        BlockPos Here = BlockPos.containing(Source.getPosition());
        FactionControl.awaken(World, DaeviteFaction.Instance, Here, Source.getEntity());
        BlockPos Target = ahead(Source, CampAhead);
        FactionSite Camp = State.addSite(DaeviteFaction.Camp, Level.OVERWORLD, Target, World.random.nextLong());
        if (!DaeviteSites.buildNow(World, Camp, Target, Source.getPosition())) {
            State.sites().remove(Camp);
            State.changed();
            Source.sendFailure(Component.translatable("commands.scp_unleashed.daevite.no_plan"));
            return 0;
        }
        DaeviteSites.garrison(World, State, Camp);
        Camp.setLastReinforce(World.getGameTime());
        State.changed();
        BlockPos At = Camp.anchor();
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.daevite.camp_built", At.getX(), At.getY(), At.getZ()), true);
        return 1;
    }

    private static int ritualNow(CommandSourceStack Source) {
        MinecraftServer Server = Source.getServer();
        FactionState State = state(Source);
        if (State.phase() != FactionPhase.Awakened) {
            Source.sendFailure(Component.translatable("commands.scp_unleashed.daevite.not_awake"));
            return 0;
        }
        State.setPower(DaeviteFaction.MaxPower, DaeviteFaction.MaxPower);
        DaeviteRitual.force(Server, State);
        BlockPos Centre = DaeviteRitual.centre(State);
        if (!DaeviteRitual.active(State) || Centre == null) {
            Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.daevite.ritual_pending"), true);
            return 1;
        }
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.daevite.ritual_started", Centre.getX(), Centre.getY(), Centre.getZ()), true);
        return 1;
    }

    private static int energy(CommandSourceStack Source) {
        FactionState State = state(Source);
        String Stage = switch (DaeviteRitual.stage(State)) {
            case DaeviteRitual.Temple -> "temple";
            case DaeviteRitual.Waiting -> "waiting";
            case DaeviteRitual.Remote -> "remote";
            default -> "none";
        };
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.daevite.energy", String.format(Locale.ROOT, "%.1f", State.power()),
                String.format(Locale.ROOT, "%.0f", DaeviteFaction.MaxPower), State.phase().getSerializedName(), Stage,
                String.format(Locale.ROOT, "%.0f", DaeviteRitual.share(State) * 100.0f)), false);
        return (int) State.power();
    }

    private static int setEnergy(CommandContext<CommandSourceStack> Context) {
        CommandSourceStack Source = Context.getSource();
        FactionState State = state(Source);
        double Amount = DoubleArgumentType.getDouble(Context, "amount");
        State.setPower(Amount, DaeviteFaction.MaxPower);
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.daevite.energy_set", String.format(Locale.ROOT, "%.1f", State.power())), true);
        return 1;
    }

    private static int killAll(CommandSourceStack Source) {
        int Count = 0;
        for (ServerLevel World : Source.getServer().getAllLevels()) {
            List<Entity> Found = new ArrayList<>();
            for (Entity Body : World.getAllEntities()) {
                if (Body.isAlive() && (DaeviteTargets.isDaevite(Body) || Body instanceof DaeviteMob)) {
                    Found.add(Body);
                }
            }
            for (Entity Body : Found) {
                DaeviteCollapse.doom(World, Body, 2 + World.random.nextInt(30));
                Count++;
            }
        }
        int Total = Count;
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.daevite.killall", Total), true);
        return Count;
    }

    private static int knight(CommandSourceStack Source, byte Loadout) {
        ServerLevel World = Source.getLevel();
        BlockPos At = ahead(Source, 4);
        DaeviteKnight Knight = DaeviteSites.knightAt(World, state(Source), null, At, Loadout);
        if (Knight == null) {
            return 0;
        }
        Source.sendSuccess(() -> Component.translatable("commands.scp_unleashed.daevite.knight", Component.translatable(
                "jade.scp_unleashed.daevite." + (Loadout == DaeviteKnight.Sword ? "sword" : "spear"))), true);
        return 1;
    }
}
