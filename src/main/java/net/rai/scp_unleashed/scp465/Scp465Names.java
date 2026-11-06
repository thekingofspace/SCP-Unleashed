package net.rai.scp_unleashed.scp465;

import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class Scp465Names {
    public static final String Prefix = "entity.scp_unleashed.scp465_instance.guest.";
    public static final String DjKey = "entity.scp_unleashed.scp465_instance.dj";
    public static final List<String> Guests = List.of("father", "mother", "friend", "best_friend", "grandma", "grandpa", "cousin", "neighbour", "classmate",
            "coworker", "uncle", "aunt", "little_brother", "big_sister", "teacher", "mail_carrier", "babysitter", "old_friend", "met_once", "pen_pal",
            "coach", "family_friend", "roommate", "childhood_friend", "that_guy", "barber", "neighbours_kid", "librarian", "baker", "bus_driver",
            "little_sister", "big_brother", "godmother", "team_captain", "lab_partner", "camp_counsellor", "piano_teacher", "nephew", "niece",
            "scout_leader", "ice_cream_man", "bandmate", "online_friend", "plus_one");
    public static final Set<String> Elders = Set.of("grandma", "grandpa");
    public static final int ClownLooks = 4;
    public static final int GuestLooks = 8;
    public static final int GrandmaLook = 6;
    public static final int GrandpaLook = 7;
    public static final int YoungLooks = 6;

    private Scp465Names() {
    }

    public static String key(String Name) {
        return Prefix + Name;
    }

    public static Component name(String Name) {
        return Component.translatable(key(Name));
    }

    public static List<String> pick(int Count, Collection<String> Taken, RandomSource Random) {
        List<String> Pool = new ArrayList<>();
        for (String Name : Guests) {
            if (!Taken.contains(Name)) {
                Pool.add(Name);
            }
        }
        List<String> Picked = new ArrayList<>();
        while (Picked.size() < Count && !Pool.isEmpty()) {
            Picked.add(Pool.remove(Random.nextInt(Pool.size())));
        }
        return Picked;
    }

    public static int lookFor(String Name, int Order) {
        if ("grandma".equals(Name)) {
            return GrandmaLook;
        }
        if ("grandpa".equals(Name)) {
            return GrandpaLook;
        }
        return Math.floorMod(Order, YoungLooks);
    }
}
