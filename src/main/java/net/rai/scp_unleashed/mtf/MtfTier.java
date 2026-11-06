package net.rai.scp_unleashed.mtf;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.anomaly.RiftFragment;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public enum MtfTier {
    Faint(FragmentRarity.Faint, "fireteam", 2400, 20.0, 0.0, 0.0, 0.55f, 20, 1, 0.30,
            List.of(MtfRole.Sidearm, MtfRole.Sidearm)),
    Wavering(FragmentRarity.Wavering, "patrol", 3000, 24.0, 1.0, 0.0, 0.62f, 15, 2, 0.31,
            List.of(MtfRole.Sidearm, MtfRole.Sidearm, MtfRole.Pointman)),
    Volatile(FragmentRarity.Volatile, "squad", 3600, 28.0, 2.0, 1.0, 0.70f, 10, 3, 0.32,
            List.of(MtfRole.Rifleman, MtfRole.Rifleman, MtfRole.Pointman, MtfRole.Rifleman, MtfRole.Pointman)),
    Severe(FragmentRarity.Severe, "strike_team", 4800, 34.0, 4.0, 2.0, 0.80f, 7, 4, 0.33,
            List.of(MtfRole.Rifleman, MtfRole.Rifleman, MtfRole.Marksman, MtfRole.Rifleman, MtfRole.Pointman, MtfRole.Rifleman, MtfRole.Rifleman)),
    Abyssal(FragmentRarity.Abyssal, "detachment", 6000, 40.0, 6.0, 4.0, 0.90f, 4, 5, 0.34,
            List.of(MtfRole.Leader, MtfRole.Rifleman, MtfRole.Bearer, MtfRole.Rifleman, MtfRole.Marksman, MtfRole.Rifleman, MtfRole.Rifleman,
                    MtfRole.Marksman, MtfRole.Rifleman, MtfRole.Rifleman));

    private final FragmentRarity Rarity;
    private final String Key;
    private final int DurationTicks;
    private final double Health;
    private final double Armour;
    private final double Toughness;
    private final float Accuracy;
    private final int ReactionTicks;
    private final int Level;
    private final double Speed;
    private final List<MtfRole> Roles;

    MtfTier(FragmentRarity Rarity, String Key, int DurationTicks, double Health, double Armour, double Toughness, float Accuracy, int ReactionTicks, int Level,
            double Speed, List<MtfRole> Roles) {
        this.Rarity = Rarity;
        this.Key = Key;
        this.DurationTicks = DurationTicks;
        this.Health = Health;
        this.Armour = Armour;
        this.Toughness = Toughness;
        this.Accuracy = Accuracy;
        this.ReactionTicks = ReactionTicks;
        this.Level = Level;
        this.Speed = Speed;
        this.Roles = Roles;
    }

    public FragmentRarity rarity() {
        return Rarity;
    }

    public String key() {
        return Key;
    }

    public String commandName() {
        return Rarity.getSerializedName();
    }

    public int durationTicks() {
        return DurationTicks;
    }

    public double health() {
        return Health;
    }

    public double armour() {
        return Armour;
    }

    public double toughness() {
        return Toughness;
    }

    public float accuracy() {
        return Accuracy;
    }

    public int reactionTicks() {
        return ReactionTicks;
    }

    public int level() {
        return Level;
    }

    public double speed() {
        return Speed;
    }

    public List<MtfRole> roles() {
        return Roles;
    }

    public int size() {
        return Roles.size();
    }

    public boolean takesCover() {
        return Level >= 2;
    }

    public boolean strafes() {
        return Level >= 3;
    }

    public boolean focusesFire() {
        return Level >= 3;
    }

    public boolean kites() {
        return Level >= 4;
    }

    public int colour() {
        return Rarity.getColour() & 0xFFFFFF;
    }

    public Component displayName() {
        return Component.translatable("mtf.scp_unleashed.tier." + Key).withStyle(Style.EMPTY.withColor(colour()));
    }

    public static MtfTier byIndex(int Index) {
        MtfTier[] All = values();
        return All[Math.floorMod(Index, All.length)];
    }

    public static MtfTier of(FragmentRarity Grade) {
        for (MtfTier Tier : values()) {
            if (Tier.Rarity == Grade) {
                return Tier;
            }
        }
        return Faint;
    }

    @Nullable
    public static MtfTier byName(String Name) {
        for (MtfTier Tier : values()) {
            if (Tier.commandName().equals(Name)) {
                return Tier;
            }
        }
        return null;
    }

    @Nullable
    public static MtfTier fromFragment(ItemStack Stack) {
        if (Stack.getItem() instanceof RiftFragment Fragment) {
            return of(Fragment.getRarity());
        }
        return null;
    }
}
