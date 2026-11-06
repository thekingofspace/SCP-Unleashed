package net.rai.scp_unleashed.scp2521;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp2521Chat {
    public static final Set<String> Speech = Set.of("say", "me", "msg", "tell", "w", "teammsg", "tm");
    public static final List<String> MessageArguments = List.of("message", "action");

    private Scp2521Chat() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void chat(ServerChatEvent Event) {
        ServerPlayer Speaker = Event.getPlayer();
        String Raw = Event.getRawText();
        if (Scp2521Name.matches(Raw)) {
            Speaker.server.execute(() -> Scp2521Rules.heard(Speaker, Raw));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void command(CommandEvent Event) {
        ParseResults<CommandSourceStack> Parse = Event.getParseResults();
        if (!(Parse.getContext().getSource().getEntity() instanceof ServerPlayer Speaker)) {
            return;
        }
        String Said = spokenText(Parse);
        if (Said != null && Scp2521Name.matches(Said)) {
            Speaker.server.execute(() -> Scp2521Rules.heard(Speaker, Said));
        }
    }

    @Nullable
    public static String spokenText(ParseResults<CommandSourceStack> Parse) {
        String Input = Parse.getReader().getString();
        CommandContextBuilder<CommandSourceStack> Context = Parse.getContext();
        boolean Speaking = false;
        int Guard = 0;
        while (Context != null && Guard++ < 32) {
            List<ParsedCommandNode<CommandSourceStack>> Nodes = Context.getNodes();
            if (!Nodes.isEmpty() && Nodes.get(0).getNode() instanceof LiteralCommandNode<CommandSourceStack> Literal && Speech.contains(Literal.getLiteral())) {
                Speaking = true;
            }
            if (Speaking) {
                for (String Name : MessageArguments) {
                    ParsedArgument<CommandSourceStack, ?> Found = Context.getArguments().get(Name);
                    if (Found != null) {
                        return Found.getRange().get(Input);
                    }
                }
            }
            Context = Context.getChild();
        }
        return null;
    }
}
