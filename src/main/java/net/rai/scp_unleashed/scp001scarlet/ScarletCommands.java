package net.rai.scp_unleashed.scp001scarlet;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.faction.FactionControl;
import net.rai.scp_unleashed.faction.FactionState;
import net.rai.scp_unleashed.raid.Raid;
import net.rai.scp_unleashed.raid.RaidDirector;
import net.rai.scp_unleashed.raid.RaidWorld;
import net.rai.scp_unleashed.scp001scarlet.core.ScarletState;
import net.rai.scp_unleashed.scp001scarlet.cult.ScarletCultFaction;
import net.rai.scp_unleashed.scp001scarlet.cult.ScarletRitualStyle;
import net.rai.scp_unleashed.scp001scarlet.throne.ScarletThrone;
import net.rai.scp_unleashed.scp001scarlet.throne.ScarletThroneEmitter;
import net.rai.scp_unleashed.scp001scarlet.throne.ScarletThroneSite;

import java.util.Arrays;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class ScarletCommands {
    private ScarletCommands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Event.getDispatcher().register(Commands.literal(Scp_unleashed.ModId)
                .then(Commands.literal("scarlet")
                        .requires(Source -> Source.hasPermission(2))
                        .then(Commands.literal("status").executes(Context -> status(Context.getSource())))
                        .then(Commands.literal("set").then(Commands.argument("percent", IntegerArgumentType.integer(0, 100))
                                .executes(Context -> set(Context.getSource(), IntegerArgumentType.getInteger(Context, "percent")))))
                        .then(Commands.literal("reset").executes(Context -> reset(Context.getSource())))
                        .then(Commands.literal("phase").then(Commands.argument("name", StringArgumentType.word())
                                .suggests((Context, Builder) -> SharedSuggestionProvider.suggest(Arrays.stream(ScarletState.Phase.values()).map(Enum::name), Builder))
                                .executes(Context -> phase(Context.getSource(), StringArgumentType.getString(Context, "name")))))
                        .then(Commands.literal("ritual").executes(Context -> ritual(Context.getSource())))
                        .then(Commands.literal("throne").then(Commands.literal("here").executes(Context -> throneHere(Context.getSource())))
                                .then(Commands.literal("build").executes(Context -> throneBuild(Context.getSource()))))));
    }

    private static int status(CommandSourceStack Source) {
        MinecraftServer Server = Source.getServer();
        ScarletState State = ScarletState.get(Server);
        ScarletWorld Data = ScarletWorld.get(Server);
        ScarletThroneSite Site = ScarletThroneSite.get(Server);
        String Line = String.format("SCP-001 (Scarlet King): phase %s, manifestation %.1f%%, chains broken %d/%d, read %s, XK-2 clock %d/%d, sky %.2f, throne %s (%d chunks built, %d pending%s)",
                State.phase().name(), State.manifestation() * 100.0f, State.broken(), ScarletState.Chains, State.read(), Data.clock(), ScarletConfig.xk2Ticks(), Data.sky(),
                State.thronePlaced() ? State.throne().toShortString() : "none", Site.finished(), ScarletThroneEmitter.pending(), Site.rising() ? ", rising stage " + Site.stage() : "");
        Source.sendSuccess(() -> Component.literal(Line), false);
        return 1;
    }

    private static int set(CommandSourceStack Source, int Percent) {
        MinecraftServer Server = Source.getServer();
        ScarletState State = ScarletState.get(Server);
        State.manifestation(Percent / 100.0f);
        if (State.phase() == ScarletState.Phase.Asleep && Percent > 0) {
            State.markRead();
            State.phase(ScarletState.Phase.Stirring, Server.overworld().getGameTime());
        }
        if (Percent >= 100) {
            ScarletXk.begin(Server);
        }
        ScarletMeter.syncAll(Server);
        Source.sendSuccess(() -> Component.literal("Manifestation set to " + Percent + "%"), true);
        return 1;
    }

    private static int reset(CommandSourceStack Source) {
        MinecraftServer Server = Source.getServer();
        ScarletState Fresh = new ScarletState();
        Fresh.setDirty();
        Server.overworld().getDataStorage().set("scp_unleashed_scarlet", Fresh);
        ScarletWorld.get(Server).wipe();
        ScarletThroneSite.get(Server).forget();
        ScarletThroneEmitter.clear();
        FactionControl.reset(Server, ScarletCultFaction.Instance);
        for (ServerPlayer User : Server.getPlayerList().getPlayers()) {
            User.setData(Scp001ScarletRegistry.Seen, false);
        }
        ScarletMeter.syncAll(Server);
        Source.sendSuccess(() -> Component.literal("SCP-001 (Scarlet King) reset"), true);
        return 1;
    }

    private static int phase(CommandSourceStack Source, String Name) {
        MinecraftServer Server = Source.getServer();
        ScarletState.Phase Found = null;
        for (ScarletState.Phase Each : ScarletState.Phase.values()) {
            if (Each.name().equalsIgnoreCase(Name)) {
                Found = Each;
            }
        }
        if (Found == null) {
            Source.sendFailure(Component.literal("Unknown phase " + Name));
            return 0;
        }
        if (Found == ScarletState.Phase.Xk2) {
            ScarletXk.begin(Server);
        } else {
            ScarletState State = ScarletState.get(Server);
            if (Found != ScarletState.Phase.Asleep) {
                State.markRead();
            }
            State.phase(Found, Server.overworld().getGameTime());
            ScarletWorld.get(Server).forgetDayTime();
        }
        ScarletMeter.syncAll(Server);
        ScarletState.Phase Shown = Found;
        Source.sendSuccess(() -> Component.literal("Scarlet King phase set to " + Shown.name()), true);
        return 1;
    }

    private static int ritual(CommandSourceStack Source) {
        ServerPlayer User = Source.getPlayer();
        if (User == null) {
            Source.sendFailure(Component.literal("Run this as a player"));
            return 0;
        }
        MinecraftServer Server = Source.getServer();
        RaidWorld Data = RaidWorld.get(Server);
        FactionState State = FactionControl.state(Server, ScarletCultFaction.Instance);
        if (Data.activeFor(ScarletCultFaction.Instance.id()) != null) {
            Source.sendFailure(Component.literal("A ritual is already under way"));
            return 0;
        }
        Raid Started = RaidDirector.launch(Server, Data, ScarletCultFaction.Instance, ScarletRitualStyle.Instance, State, RaidDirector.forcedCandidate(User, ScarletRitualStyle.Instance),
                true, Server.overworld().getGameTime());
        if (Started == null) {
            Source.sendFailure(Component.literal("The cult could not gather"));
            return 0;
        }
        Source.sendSuccess(() -> Component.literal("A cult ritual gathers near you"), true);
        return 1;
    }

    private static int throneHere(CommandSourceStack Source) {
        BlockPos At = BlockPos.containing(Source.getPosition());
        MinecraftServer Server = Source.getServer();
        ScarletThroneSite.placeAt(Server, At);
        ScarletState.get(Server).throne(At);
        ScarletMeter.syncAll(Server);
        Source.sendSuccess(() -> Component.literal("Scarlet Throne planned at " + At.toShortString()), true);
        return 1;
    }

    private static int throneBuild(CommandSourceStack Source) {
        MinecraftServer Server = Source.getServer();
        ScarletThroneSite Site = ScarletThroneSite.get(Server);
        if (!Site.planned()) {
            Source.sendFailure(Component.literal("No Scarlet Throne is planned"));
            return 0;
        }
        ScarletThroneEmitter.emitAll(Server.overworld(), Site);
        int Refit = ScarletThrone.refit(Server.overworld(), Site.centre());
        Source.sendSuccess(() -> Component.literal("Scarlet Throne built (" + Refit + " mount blocks refitted)"), true);
        return 1;
    }
}
