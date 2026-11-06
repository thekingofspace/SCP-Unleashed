package net.rai.scp_unleashed.scp444;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.Targeting;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp444Silence {
    public static final Set<String> ChatCommands = Set.of("msg", "tell", "w", "me", "say", "teammsg", "tm");
    public static final String SilentKey = "message.scp_unleashed.scp444.silent";

    private Scp444Silence() {
    }

    public static boolean isSilenced(ServerPlayer User) {
        return Scp444Affliction.isAfflicted(User);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void chat(ServerChatEvent Event) {
        ServerPlayer User = Event.getPlayer();
        if (isSilenced(User)) {
            Event.setCanceled(true);
            refuse(User);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void command(CommandEvent Event) {
        ParseResults<CommandSourceStack> Parse = Event.getParseResults();
        if (!(Parse.getContext().getSource().getEntity() instanceof ServerPlayer User) || !isSilenced(User)) {
            return;
        }
        if (isChatCommand(Parse)) {
            Event.setCanceled(true);
            refuse(User);
        }
    }

    public static boolean isChatCommand(ParseResults<CommandSourceStack> Parse) {
        CommandContextBuilder<CommandSourceStack> Context = Parse.getContext();
        int Guard = 0;
        while (Context != null && Guard++ < 32) {
            List<ParsedCommandNode<CommandSourceStack>> Nodes = Context.getNodes();
            if (!Nodes.isEmpty() && Nodes.get(0).getNode() instanceof LiteralCommandNode<CommandSourceStack> Literal && ChatCommands.contains(Literal.getLiteral())) {
                return true;
            }
            Context = Context.getChild();
        }
        return false;
    }

    public static void refuse(ServerPlayer User) {
        if (!Scp444Spread.ready(User) || Targeting.isStatue(User)) {
            return;
        }
        User.displayClientMessage(Component.translatable(SilentKey).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        Vec3 Mouth = Scp444Affliction.mouth(User);
        Scp444Affliction.play(User.serverLevel(), Mouth, Scp444Registry.HushLayers, 0.6f, 0.9f + User.getRandom().nextFloat() * 0.15f);
        Scp444Spread.speak(User);
    }

    public static void forget(UUID Id) {
        Scp444Spread.forget(Id);
    }

    public static void clear() {
        Scp444Spread.clear();
    }
}
