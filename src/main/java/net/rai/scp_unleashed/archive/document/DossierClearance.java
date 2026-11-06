package net.rai.scp_unleashed.archive.document;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.door.KeycardItem;
import org.jetbrains.annotations.Nullable;

public final class DossierClearance {
    public static final int None = 0;
    public static final int Highest = 5;
    public static final int Omni = KeycardItem.OmniClearance;

    private DossierClearance() {
    }

    public static int required(DocumentClass Classification) {
        return switch (Classification) {
            case Safe, Neutralized -> 2;
            case Euclid -> 3;
            case Keter -> 4;
            case Thaumiel, Apollyon, Unknown, ZClass -> Highest;
        };
    }

    public static int required(@Nullable AnomalyDocument Document) {
        return Document == null ? Highest : required(Document.Classification());
    }

    public static int clearanceOf(ItemStack Stack) {
        return !Stack.isEmpty() && Stack.getItem() instanceof KeycardItem Card ? Card.getClearance() : None;
    }

    public static int highest(@Nullable Player Reader) {
        if (Reader == null) {
            return None;
        }
        Inventory Carried = Reader.getInventory();
        int Best = None;
        for (ItemStack Stack : Carried.items) {
            Best = Math.max(Best, clearanceOf(Stack));
        }
        for (ItemStack Stack : Carried.offhand) {
            Best = Math.max(Best, clearanceOf(Stack));
        }
        return Best;
    }

    public static boolean clears(int Held, int Needed) {
        return Held >= Omni || Held >= Needed;
    }

    public static boolean canRead(@Nullable Player Reader, DocumentClass Classification) {
        return clears(highest(Reader), required(Classification));
    }
}
