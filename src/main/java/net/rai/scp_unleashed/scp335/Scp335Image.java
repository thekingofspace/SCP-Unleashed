package net.rai.scp_unleashed.scp335;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.computer.FloppyDiscItem;
import net.rai.scp_unleashed.computer.data.ComputerFile;
import net.rai.scp_unleashed.computer.data.FloppyContents;

import java.util.List;

public final class Scp335Image {
    public static final ResourceLocation Picture = Scp335Registry.location("scp335_all");
    public static final String FileName = "ALL.IMG";

    private Scp335Image() {
    }

    public static ComputerFile file() {
        return ComputerFile.image(FileName, Picture);
    }

    public static FloppyContents contents() {
        return new FloppyContents(List.of(file()));
    }

    public static boolean isOriginal(ComputerFile File) {
        return File.isImage() && Picture.equals(File.Document());
    }

    public static boolean holdsOriginal(ItemStack Stack) {
        for (ComputerFile File : FloppyDiscItem.contentsOf(Stack).Files()) {
            if (isOriginal(File)) {
                return true;
            }
        }
        return false;
    }
}
