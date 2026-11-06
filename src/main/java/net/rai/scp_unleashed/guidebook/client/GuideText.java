package net.rai.scp_unleashed.guidebook.client;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.rai.scp_unleashed.guidebook.GuidebookValues;

import java.util.List;

public record GuideText(String Key, List<String> Args) {
    public static final String Marker = "**";

    public String plain() {
        Object[] Resolved = new Object[Args.size()];
        for (int Index = 0; Index < Resolved.length; Index++) {
            Resolved[Index] = GuidebookValues.resolve(Args.get(Index));
        }
        return Component.translatableWithFallback(Key, Key, Resolved).getString();
    }

    public Component styled(Style Accent) {
        MutableComponent Out = Component.empty();
        String[] Parts = plain().split("\\*\\*", -1);
        for (int Index = 0; Index < Parts.length; Index++) {
            if (!Parts[Index].isEmpty()) {
                Out.append(Component.literal(Parts[Index]).withStyle(Index % 2 == 1 ? Accent : Style.EMPTY));
            }
        }
        return Out;
    }

    public String flat() {
        return plain().replace(Marker, "");
    }
}
