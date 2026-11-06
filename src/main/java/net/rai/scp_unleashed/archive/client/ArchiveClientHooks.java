package net.rai.scp_unleashed.archive.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.rai.scp_unleashed.archive.database.DatabaseRecord;

import java.util.List;

public final class ArchiveClientHooks {
    private ArchiveClientHooks() {
    }

    public static void openDossier(InteractionHand Hand, boolean Reveal) {
        Minecraft Game = Minecraft.getInstance();
        if (Game.player != null) {
            Game.setScreen(new DossierScreen(Hand, Reveal));
        }
    }

    public static void acceptDatabase(List<DatabaseRecord> Records) {
        DatabaseRecords.accept(Records);
    }

    public static void openDatabase(BlockPos Pos, boolean Main) {
        Minecraft Game = Minecraft.getInstance();
        if (Game.player != null) {
            Game.setScreen(new DatabaseScreen(Pos, Main));
        }
    }
}
