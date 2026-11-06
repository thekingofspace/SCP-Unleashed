package net.rai.scp_unleashed.scp539;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp539Chat {
    public static final String LinePrefix = "message.scp_unleashed.scp539.rock.";
    public static final int LineCount = 28;
    public static final Set<String> SpokenCommands = Set.of("say", "me");
    public static final Set<String> WhisperCommands = Set.of("msg", "tell", "w");
    public static final Set<String> TeamCommands = Set.of("teammsg", "tm");
    private static final Map<UUID, Integer> LastLine = new HashMap<>();

    private Scp539Chat() {
    }

    public static Component line(ServerPlayer User) {
        int Previous = LastLine.getOrDefault(User.getUUID(), -1);
        int Picked = User.getRandom().nextInt(LineCount);
        if (Picked == Previous) {
            Picked = (Picked + 1 + User.getRandom().nextInt(LineCount - 1)) % LineCount;
        }
        LastLine.put(User.getUUID(), Picked);
        return Component.translatable(LinePrefix + Picked);
    }

    @Nullable
    public static Component replacement(ServerPlayer User) {
        if (!Scp539Compulsion.isAffected(User)) {
            return null;
        }
        return Component.translatable("chat.type.text", User.getDisplayName(), line(User));
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void chat(ServerChatEvent Event) {
        ServerPlayer User = Event.getPlayer();
        Component Replaced = replacement(User);
        if (Replaced != null) {
            Event.setCanceled(true);
            User.server.getPlayerList().broadcastSystemMessage(Replaced, false);
            return;
        }
        if (Scp539Plan.matches(Event.getRawText())) {
            Scp539Plan.arm(User, Scp539Compulsion.now(User.server));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void command(CommandEvent Event) {
        ParseResults<CommandSourceStack> Parse = Event.getParseResults();
        if (!(Parse.getContext().getSource().getEntity() instanceof ServerPlayer User) || !Scp539Compulsion.isAffected(User)) {
            return;
        }
        CommandContextBuilder<CommandSourceStack> Context = Parse.getContext();
        int Guard = 0;
        while (Context != null && Guard++ < 32) {
            List<ParsedCommandNode<CommandSourceStack>> Nodes = Context.getNodes();
            if (!Nodes.isEmpty() && Nodes.get(0).getNode() instanceof LiteralCommandNode<CommandSourceStack> Literal) {
                String Name = Literal.getLiteral();
                if (SpokenCommands.contains(Name) || WhisperCommands.contains(Name) || TeamCommands.contains(Name)) {
                    Event.setCanceled(true);
                    deliver(User, Name, Context, Parse.getReader().getString());
                    return;
                }
            }
            Context = Context.getChild();
        }
    }

    private static void deliver(ServerPlayer User, String Name, CommandContextBuilder<CommandSourceStack> Context, String Input) {
        MinecraftServer Server = User.server;
        Component Line = line(User);
        if (Name.equals("say")) {
            Server.getPlayerList().broadcastSystemMessage(Component.translatable("chat.type.announcement", User.getDisplayName(), Line), false);
        } else if (Name.equals("me")) {
            Server.getPlayerList().broadcastSystemMessage(Component.translatable("chat.type.emote", User.getDisplayName(), Line), false);
        } else if (WhisperCommands.contains(Name)) {
            for (ServerPlayer Target : whisperTargets(Context, Input)) {
                Target.sendSystemMessage(Component.translatable("commands.message.display.incoming", User.getDisplayName(), Line).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                User.sendSystemMessage(Component.translatable("commands.message.display.outgoing", Target.getDisplayName(), Line).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            }
        } else {
            PlayerTeam Squad = User.getTeam();
            if (Squad == null) {
                User.sendSystemMessage(Component.translatable("commands.teammsg.failed.noteam").withStyle(ChatFormatting.RED));
                return;
            }
            Component Shown = Squad.getFormattedDisplayName();
            for (ServerPlayer Member : Server.getPlayerList().getPlayers()) {
                if (Member == User) {
                    Member.sendSystemMessage(Component.translatable("chat.type.team.sent", Shown, User.getDisplayName(), Line));
                } else if (Member.getTeam() == Squad) {
                    Member.sendSystemMessage(Component.translatable("chat.type.team.text", Shown, User.getDisplayName(), Line));
                }
            }
        }
    }

    private static Collection<ServerPlayer> whisperTargets(CommandContextBuilder<CommandSourceStack> Context, String Input) {
        CommandContextBuilder<CommandSourceStack> Search = Context;
        int Guard = 0;
        while (Search != null && Guard++ < 32) {
            if (Search.getArguments().containsKey("targets")) {
                try {
                    return EntityArgument.getPlayers(Search.build(Input), "targets");
                } catch (CommandSyntaxException Problem) {
                    return List.of();
                }
            }
            Search = Search.getChild();
        }
        return List.of();
    }

    public static void reset() {
        LastLine.clear();
    }
}
