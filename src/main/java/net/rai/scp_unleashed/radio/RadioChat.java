package net.rai.scp_unleashed.radio;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.ServerChatEvent;
import org.slf4j.Logger;

import java.util.List;

public final class RadioChat {
    public static final int MaxLength = 256;
    public static final double WeakBelow = 0.25;
    public static final double WeakestLoss = 0.45;
    public static final double HauntedLoss = 0.2;
    public static final char Lost = '·';
    private static final Logger Log = LogUtils.getLogger();

    private RadioChat() {
    }

    public static Component tag(int Channel) {
        return Component.translatable("radio.scp_unleashed.tag", RadioCarry.label(Channel)).withStyle(ChatFormatting.DARK_GREEN);
    }

    public static void chat(ServerChatEvent Event) {
        ServerPlayer Sender = Event.getPlayer();
        String Raw = Event.getRawText();
        String Prefix = VoiceConfig.chatPrefix();
        if (Raw == null || !Raw.startsWith(Prefix)) {
            return;
        }
        ItemStack Radio = RadioCarry.held(Sender);
        if (Radio.isEmpty()) {
            return;
        }
        Event.setCanceled(true);
        String Body = Raw.substring(Prefix.length()).strip();
        if (!Body.isEmpty()) {
            send(Sender, RadioCarry.channel(Radio), Body);
        }
    }

    public static List<RadioRelay.Hearer> send(ServerPlayer Sender, int Channel, String Text) {
        String Body = Text.length() > MaxLength ? Text.substring(0, MaxLength) : Text;
        List<RadioRelay.Hearer> Hearers = RadioRelay.hearers(Sender, Channel);
        deliver(Sender, Channel, Body, Hearers);
        return Hearers;
    }

    public static void deliver(ServerPlayer Sender, int Channel, String Body, List<RadioRelay.Hearer> Hearers) {
        long Now = Sender.serverLevel().getGameTime();
        Component Name = Sender.getDisplayName();
        for (RadioRelay.Hearer Each : Hearers) {
            if (RadioBleed.isCorrupt(Each.Listener().getUUID(), Now)) {
                Each.Listener().sendSystemMessage(RadioBleed.machineLine(Channel, Each.Listener().getRandom()));
                continue;
            }
            boolean Haunted = RadioBleed.isHaunted(Each.Listener().getUUID(), Now);
            Component Line = Component.translatable("radio.scp_unleashed.chat", tag(Channel), Name, garble(Body, Each.Strength(), Haunted, Each.Listener().getRandom()))
                    .withStyle(ChatFormatting.GRAY);
            Each.Listener().sendSystemMessage(Line);
        }
        RadioSignal.playTo(Hearers, RadioRegistry.MessageLayers, 0.5f, 1.0f);
        String Echo = Hearers.isEmpty() ? "radio.scp_unleashed.chat.unheard" : "radio.scp_unleashed.chat.sent";
        Sender.sendSystemMessage(Component.translatable(Echo, tag(Channel), Name, Body).withStyle(ChatFormatting.GRAY));
        RadioSignal.playTo(Sender, List.of(RadioRegistry.Click), 0.4f, 1.6f);
        Log.info("[Radio CH {}] <{}> {} ({} listeners)", RadioCarry.label(Channel), Sender.getGameProfile().getName(), Body, Hearers.size());
    }

    public static Component noise(int Length, RandomSource Random) {
        StringBuilder Text = new StringBuilder();
        for (int Index = 0; Index < Length; Index++) {
            Text.append((char) ('A' + Random.nextInt(26)));
        }
        return Component.literal(Text.toString()).withStyle(Style.EMPTY.withObfuscated(true).withColor(ChatFormatting.DARK_RED));
    }

    public static double lossChance(double Strength, boolean Haunted) {
        double Chance = Strength < WeakBelow ? (WeakBelow - Math.max(0.0, Strength)) / WeakBelow * WeakestLoss : 0.0;
        return Haunted ? Chance + HauntedLoss : Chance;
    }

    public static Component garble(String Body, double Strength, boolean Haunted, RandomSource Random) {
        double Chance = lossChance(Strength, Haunted);
        if (Chance <= 0.0) {
            return Component.literal(Body);
        }
        MutableComponent Made = Component.empty();
        StringBuilder Clear = new StringBuilder();
        for (int Index = 0; Index < Body.length(); Index++) {
            char Letter = Body.charAt(Index);
            if (Letter == ' ' || Random.nextDouble() >= Chance) {
                Clear.append(Letter);
                continue;
            }
            if (!Clear.isEmpty()) {
                Made.append(Component.literal(Clear.toString()));
                Clear.setLength(0);
            }
            if (Haunted && Random.nextBoolean()) {
                Made.append(Component.literal(String.valueOf(Letter)).withStyle(Style.EMPTY.withObfuscated(true).withColor(ChatFormatting.DARK_RED)));
            } else {
                Made.append(Component.literal(String.valueOf(Lost)).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        if (!Clear.isEmpty()) {
            Made.append(Component.literal(Clear.toString()));
        }
        return Made;
    }
}
