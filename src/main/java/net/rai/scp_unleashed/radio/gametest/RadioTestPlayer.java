package net.rai.scp_unleashed.radio.gametest;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.rai.scp_unleashed.scp538.gametest.Scp538TestPlayer;

import java.util.ArrayList;
import java.util.List;

public class RadioTestPlayer extends Scp538TestPlayer {
    private final List<Component> Received = new ArrayList<>();

    public RadioTestPlayer(ServerLevel World, String Name) {
        super(World, Name);
    }

    @Override
    public void sendSystemMessage(Component Message, boolean Overlay) {
        if (!Overlay) {
            Received.add(Message);
        }
    }

    public List<Component> received() {
        return Received;
    }

    public boolean heard(String Text) {
        for (Component Each : Received) {
            if (mentions(Each, Text)) {
                return true;
            }
        }
        return false;
    }

    public static boolean mentions(Component Line, String Text) {
        if (Line.getContents() instanceof PlainTextContents Plain && Plain.text().contains(Text)) {
            return true;
        }
        if (Line.getContents() instanceof TranslatableContents Translated) {
            if (Translated.getKey().contains(Text)) {
                return true;
            }
            for (Object Argument : Translated.getArgs()) {
                if (Argument instanceof Component Inner && mentions(Inner, Text) || Argument instanceof String Plain && Plain.contains(Text)) {
                    return true;
                }
            }
        }
        for (Component Sibling : Line.getSiblings()) {
            if (mentions(Sibling, Text)) {
                return true;
            }
        }
        return false;
    }
}
