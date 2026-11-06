package net.rai.scp_unleashed.scp458;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.rai.scp_unleashed.control.ControlSessions;
import org.jetbrains.annotations.Nullable;

public final class Scp458Favourites {
    private Scp458Favourites() {
    }

    public static AttachmentType<Scp458Pizza> slot(LivingEntity Person) {
        if (Person instanceof Player User && !ControlSessions.isControlling(User)) {
            return Scp458Registry.Favourite.get();
        }
        return Scp458Registry.BodyFavourite.get();
    }

    @Nullable
    public static Scp458Pizza of(LivingEntity Person) {
        return Person.getExistingDataOrNull(slot(Person));
    }

    public static Scp458Pizza claim(LivingEntity Person, RandomSource Random) {
        Scp458Pizza Known = of(Person);
        if (Known != null) {
            return Known;
        }
        Scp458Pizza Chosen = Scp458Pizza.random(Random);
        Person.setData(slot(Person), Chosen);
        return Chosen;
    }

    public static void set(LivingEntity Person, Scp458Pizza Chosen) {
        Person.setData(slot(Person), Chosen);
    }

    public static boolean clear(LivingEntity Person) {
        AttachmentType<Scp458Pizza> Slot = slot(Person);
        if (!Person.hasData(Slot)) {
            return false;
        }
        Person.removeData(Slot);
        return true;
    }
}
