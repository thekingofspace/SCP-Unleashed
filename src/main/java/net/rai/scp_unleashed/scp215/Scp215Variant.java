package net.rai.scp_unleashed.scp215;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityDimensions;

import java.util.Collection;
import java.util.Locale;
import java.util.UUID;

public enum Scp215Variant {
    Lurker(0.7f, 2.8f, 2.45f, 5.5, 30, 7.0f, 1100, 4.0f, false),
    Crawler(1.1f, 0.85f, 0.45f, 4.5, 24, 8.0f, 900, 3.5f, false),
    Watcher(0.95f, 1.85f, 1.45f, 6.0, 46, 7.0f, 1000, 2.5f, true);

    public static final long Salt = 0x5C2150A1D6E5L;
    public final float Width;
    public final float Height;
    public final float Focus;
    public final double VanishDistance;
    public final int StareLimit;
    public final float WatchCone;
    public final int Lifetime;
    public final float Weight;
    public final boolean Glows;
    public final String Name;

    Scp215Variant(float Width, float Height, float Focus, double VanishDistance, int StareLimit, float WatchCone, int Lifetime, float Weight, boolean Glows) {
        this.Width = Width;
        this.Height = Height;
        this.Focus = Focus;
        this.VanishDistance = VanishDistance;
        this.StareLimit = StareLimit;
        this.WatchCone = WatchCone;
        this.Lifetime = Lifetime;
        this.Weight = Weight;
        this.Glows = Glows;
        this.Name = name().toLowerCase(Locale.ROOT);
    }

    public EntityDimensions dimensions() {
        return EntityDimensions.scalable(Width, Height).withEyeHeight(Focus);
    }

    public int clearance() {
        return (int) Math.ceil(Height);
    }

    public static Scp215Variant byIndex(int Index) {
        Scp215Variant[] All = values();
        return All[Math.floorMod(Index, All.length)];
    }

    public static Scp215Variant forUuid(UUID Id) {
        long Mixed = Id.getMostSignificantBits() * 31L + Id.getLeastSignificantBits() + Salt;
        Mixed = (Mixed ^ (Mixed >>> 33)) * 0xFF51AFD7ED558CCDL;
        Mixed = (Mixed ^ (Mixed >>> 33)) * 0xC4CEB9FE1A85EC53L;
        Mixed ^= Mixed >>> 33;
        return byIndex((int) Math.floorMod(Mixed, (long) values().length));
    }

    public static Scp215Variant pick(RandomSource Random, Collection<Scp215Variant> Present) {
        float Total = 0.0f;
        for (Scp215Variant Kind : values()) {
            Total += weightOf(Kind, Present);
        }
        float Roll = Random.nextFloat() * Total;
        for (Scp215Variant Kind : values()) {
            Roll -= weightOf(Kind, Present);
            if (Roll <= 0.0f) {
                return Kind;
            }
        }
        return Lurker;
    }

    private static float weightOf(Scp215Variant Kind, Collection<Scp215Variant> Present) {
        return Present.contains(Kind) ? Kind.Weight * 0.25f : Kind.Weight;
    }
}
