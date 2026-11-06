package net.rai.scp_unleashed.terminal.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.archive.client.DossierScreen;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.terminal.TerminalArchive;

import java.util.Map;

public final class TitleArchive {
    private static boolean Installed;

    private TitleArchive() {
    }

    public static boolean installed() {
        return Installed;
    }

    public static boolean offline() {
        Minecraft Game = Minecraft.getInstance();
        return Game.level == null && Game.getConnection() == null;
    }

    public static Map<ResourceLocation, AnomalyDocument> documents() {
        return TerminalArchive.documents();
    }

    public static void install() {
        if (Installed || !offline()) {
            return;
        }
        Map<ResourceLocation, AnomalyDocument> Loaded = documents();
        if (Loaded.isEmpty() || !AnomalyDocuments.client().isEmpty()) {
            return;
        }
        AnomalyDocuments.acceptClient(Loaded);
        Installed = true;
    }

    public static void release() {
        if (Installed && offline()) {
            AnomalyDocuments.clearClient();
        }
        Installed = false;
    }

    public static void opening(Screen Next) {
        if (Installed && !(Next instanceof TerminalArchiveScreen) && !(Next instanceof DossierScreen)) {
            release();
        }
    }
}
