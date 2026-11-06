package net.rai.scp_unleashed.scp261;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Rarity;
import org.jetbrains.annotations.Nullable;

public enum Scp261Snack {
    GravityGum("gravity_gum", Kind.Chew, Tier.Common, 0, 0.0f),
    EchoCrisps("echo_crisps", Kind.Eat, Tier.Common, 3, 0.4f),
    StaticSoda("static_soda", Kind.Drink, Tier.Uncommon, 1, 0.2f),
    MirrorMints("mirror_mints", Kind.Chew, Tier.Common, 0, 0.0f),
    HollowBar("hollow_bar", Kind.Eat, Tier.Common, 0, 0.0f),
    FrostFizz("frost_fizz", Kind.Drink, Tier.Uncommon, 1, 0.2f),
    LuckyWafer("lucky_wafer", Kind.Eat, Tier.Rare, 3, 0.5f),
    EmberJerky("ember_jerky", Kind.Eat, Tier.Uncommon, 5, 0.6f),
    InkToffee("ink_toffee", Kind.Eat, Tier.Common, 2, 0.2f),
    WeightGummies("weight_gummies", Kind.Chew, Tier.Common, 0, 0.0f),
    TasteOfTomorrow("taste_of_tomorrow", Kind.Eat, Tier.Uncommon, 0, 0.0f),
    PocketPretzels("pocket_pretzels", Kind.Eat, Tier.Uncommon, 3, 0.4f),
    RewindRaisins("rewind_raisins", Kind.Eat, Tier.Uncommon, 2, 0.3f),
    HiccupPops("hiccup_pops", Kind.Chew, Tier.Common, 0, 0.0f),
    MobMorsels("mob_morsels", Kind.Eat, Tier.Rare, 2, 0.3f),
    FrightBites("fright_bites", Kind.Chew, Tier.Rare, 0, 0.0f);

    public static final int Columns = 4;

    private final String Name;
    private final Kind Way;
    private final Tier Rank;
    private final int Nutrition;
    private final float Saturation;

    Scp261Snack(String Name, Kind Way, Tier Rank, int Nutrition, float Saturation) {
        this.Name = Name;
        this.Way = Way;
        this.Rank = Rank;
        this.Nutrition = Nutrition;
        this.Saturation = Saturation;
    }

    public String getName() {
        return Name;
    }

    public String getId() {
        return "scp261_" + Name;
    }

    public Kind getKind() {
        return Way;
    }

    public Tier getTier() {
        return Rank;
    }

    public int getRow() {
        return ordinal() / Columns;
    }

    public int getColumn() {
        return ordinal() % Columns;
    }

    @Nullable
    public FoodProperties food() {
        if (Way == Kind.Chew) {
            return null;
        }
        FoodProperties.Builder Builder = new FoodProperties.Builder().nutrition(Nutrition).saturationModifier(Saturation).alwaysEdible();
        if (Way == Kind.Drink || Nutrition <= 2) {
            Builder.fast();
        }
        return Builder.build();
    }

    @Nullable
    public static Scp261Snack byIndex(int Index) {
        Scp261Snack[] All = values();
        return Index >= 0 && Index < All.length ? All[Index] : null;
    }

    public enum Kind {
        Eat,
        Drink,
        Chew
    }

    public enum Tier {
        Common(10, 5, Rarity.COMMON),
        Uncommon(6, 8, Rarity.UNCOMMON),
        Rare(3, 9, Rarity.RARE);

        private final int IronWeight;
        private final int GoldWeight;
        private final Rarity Shown;

        Tier(int IronWeight, int GoldWeight, Rarity Shown) {
            this.IronWeight = IronWeight;
            this.GoldWeight = GoldWeight;
            this.Shown = Shown;
        }

        public int weight(boolean Gold) {
            return Gold ? GoldWeight : IronWeight;
        }

        public Rarity getRarity() {
            return Shown;
        }
    }
}
