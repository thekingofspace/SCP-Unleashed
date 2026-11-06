package net.rai.scp_unleashed.scp610;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public enum Scp610Form {
    Apostle(Kind.Medium, Body.Biped, 34.0, 7.0, 0.27, 2.0, 0.75f, 2.5f, 12, 6, Scp610Gaits.ApostleWalk, Scp610Gaits.ApostleRun, Scp610Gaits.ApostleScale),
    Spewer(Kind.Hive, Body.Still, 150.0, 0.0, 0.0, 6.0, 2.6f, 3.2f, 0, 0, null, null, Scp610Gaits.SpewerScale),
    Villager(Kind.Medium, Body.Biped, 22.0, 5.0, 0.26, 0.0, 0.65f, 2.0f, 12, 6, Scp610Gaits.VillagerWalk, Scp610Gaits.VillagerRun, Scp610Gaits.VillagerScale),
    Pig(Kind.Small, Body.Quadruped, 12.0, 4.0, 0.26, 0.0, 0.9f, 0.9f, 12, 6, Scp610Gaits.PigWalk, Scp610Gaits.PigRun, Scp610Gaits.PigScale),
    Cow(Kind.Medium, Body.Quadruped, 18.0, 5.0, 0.25, 1.0, 0.9f, 1.4f, 12, 6, Scp610Gaits.CowWalk, Scp610Gaits.CowRun, Scp610Gaits.CowScale),
    Sheep(Kind.Medium, Body.Quadruped, 14.0, 4.0, 0.25, 1.0, 0.9f, 1.3f, 12, 6, Scp610Gaits.SheepWalk, Scp610Gaits.SheepRun, Scp610Gaits.SheepScale),
    Chicken(Kind.Small, Body.Biped, 6.0, 2.0, 0.27, 0.0, 0.5f, 0.8f, 12, 6, Scp610Gaits.ChickenWalk, Scp610Gaits.ChickenRun, Scp610Gaits.ChickenScale),
    Horse(Kind.Large, Body.Quadruped, 30.0, 6.0, 0.32, 2.0, 1.4f, 1.7f, 18, 10, Scp610Gaits.HorseWalk, Scp610Gaits.HorseRun, Scp610Gaits.HorseScale),
    Crawler(Kind.Small, Body.Quadruped, 10.0, 3.0, 0.31, 0.0, 0.8f, 0.6f, 12, 6, Scp610Gaits.CrawlerWalk, Scp610Gaits.CrawlerRun, Scp610Gaits.CrawlerScale),
    Shambler(Kind.Medium, Body.Biped, 24.0, 5.0, 0.24, 2.0, 0.7f, 1.95f, 12, 6, Scp610Gaits.ShamblerWalk, Scp610Gaits.ShamblerRun, Scp610Gaits.ShamblerScale),
    Hulk(Kind.Large, Body.Biped, 60.0, 11.0, 0.21, 6.0, 1.6f, 2.4f, 18, 10, Scp610Gaits.HulkWalk, Scp610Gaits.HulkRun, Scp610Gaits.HulkScale),
    Amalgam(Kind.Evolved, Body.Quadruped, 90.0, 12.0, 0.25, 8.0, 2.0f, 2.0f, 18, 10, Scp610Gaits.AmalgamWalk, Scp610Gaits.AmalgamRun, Scp610Gaits.AmalgamScale),
    Colossus(Kind.Evolved, Body.Biped, 120.0, 15.0, 0.22, 10.0, 1.5f, 3.4f, 18, 10, Scp610Gaits.ColossusWalk, Scp610Gaits.ColossusRun, Scp610Gaits.ColossusScale),
    Herald(Kind.Evolved, Body.Quadruped, 70.0, 6.0, 0.22, 6.0, 1.6f, 2.4f, 12, 6, Scp610Gaits.HeraldWalk, Scp610Gaits.HeraldRun, Scp610Gaits.HeraldScale),
    TendrilWorm(Kind.Hive, Body.Still, 30.0, 6.0, 0.0, 4.0, 0.8f, 2.2f, 12, 6, null, null, Scp610Gaits.TendrilWormScale),
    HiveLord(Kind.Hive, Body.Still, 500.0, 0.0, 0.0, 12.0, 4.5f, 4.5f, 0, 0, null, null, Scp610Gaits.HiveLordScale),
    Tender(Kind.Utility, Body.Quadruped, 16.0, 0.0, 0.27, 0.0, 0.9f, 0.8f, 0, 0, Scp610Gaits.TenderWalk, Scp610Gaits.TenderRun, Scp610Gaits.TenderScale),
    Weaver(Kind.Utility, Body.Quadruped, 18.0, 0.0, 0.24, 1.0, 1.1f, 0.7f, 0, 0, Scp610Gaits.WeaverWalk, Scp610Gaits.WeaverRun, Scp610Gaits.WeaverScale),
    Harvester(Kind.Utility, Body.Quadruped, 24.0, 3.0, 0.27, 2.0, 1.1f, 1.1f, 12, 6, Scp610Gaits.HarvesterWalk, Scp610Gaits.HarvesterRun, Scp610Gaits.HarvesterScale),
    Burrower(Kind.Utility, Body.Quadruped, 20.0, 0.0, 0.25, 2.0, 0.9f, 0.7f, 0, 0, Scp610Gaits.BurrowerWalk, Scp610Gaits.BurrowerRun, Scp610Gaits.BurrowerScale),
    SporeBearer(Kind.Utility, Body.Quadruped, 18.0, 0.0, 0.23, 0.0, 0.9f, 1.5f, 0, 0, Scp610Gaits.SporeBearerWalk, Scp610Gaits.SporeBearerRun, Scp610Gaits.SporeBearerScale),
    BroodSac(Kind.Hive, Body.Still, 80.0, 0.0, 0.0, 4.0, 1.6f, 1.8f, 0, 0, null, null, Scp610Gaits.BroodSacScale),
    SiegeBrute(Kind.Evolved, Body.Biped, 140.0, 16.0, 0.22, 10.0, 1.8f, 2.6f, 18, 10, Scp610Gaits.SiegeBruteWalk, Scp610Gaits.SiegeBruteRun, Scp610Gaits.SiegeBruteScale),
    Stalker(Kind.Evolved, Body.Quadruped, 50.0, 10.0, 0.34, 2.0, 0.9f, 1.0f, 12, 6, Scp610Gaits.StalkerWalk, Scp610Gaits.StalkerRun, Scp610Gaits.StalkerScale),
    Spitter(Kind.Evolved, Body.Biped, 60.0, 4.0, 0.24, 3.0, 0.9f, 2.0f, 12, 6, Scp610Gaits.SpitterWalk, Scp610Gaits.SpitterRun, Scp610Gaits.SpitterScale),
    Matriarch(Kind.Evolved, Body.Biped, 260.0, 14.0, 0.22, 12.0, 1.4f, 3.0f, 18, 10, Scp610Gaits.MatriarchWalk, Scp610Gaits.MatriarchRun, Scp610Gaits.MatriarchScale);

    public enum Kind {
        Small,
        Medium,
        Large,
        Evolved,
        Hive,
        Utility
    }

    public enum Body {
        Biped,
        Quadruped,
        Still
    }

    private final Kind Size;
    private final Body Frame;
    private final double Health;
    private final double Damage;
    private final double Speed;
    private final double Armour;
    private final float Width;
    private final float Height;
    private final int AttackTicks;
    private final int HitTick;
    @Nullable
    private final Scp610Stride Walk;
    @Nullable
    private final Scp610Stride Run;
    private final float Scale;

    Scp610Form(Kind Size, Body Frame, double Health, double Damage, double Speed, double Armour, float Width, float Height, int AttackTicks, int HitTick,
               @Nullable Scp610Stride Walk, @Nullable Scp610Stride Run, float Scale) {
        this.Size = Size;
        this.Frame = Frame;
        this.Health = Health;
        this.Damage = Damage;
        this.Speed = Speed;
        this.Armour = Armour;
        this.Width = Width;
        this.Height = Height;
        this.AttackTicks = AttackTicks;
        this.HitTick = HitTick;
        this.Walk = Walk;
        this.Run = Run;
        this.Scale = Scale;
    }

    public String id() {
        return switch (this) {
            case TendrilWorm -> "tendril_worm";
            case HiveLord -> "hive_lord";
            case SporeBearer -> "spore_bearer";
            case BroodSac -> "brood_sac";
            case SiegeBrute -> "siege_brute";
            default -> name().toLowerCase(Locale.ROOT);
        };
    }

    public String entityId() {
        return "scp610_" + id();
    }

    public Kind size() {
        return Size;
    }

    public Body frame() {
        return Frame;
    }

    public double health() {
        return Health;
    }

    public double damage() {
        return Damage;
    }

    public double speed() {
        return Speed;
    }

    public double armour() {
        return Armour;
    }

    public float width() {
        return Width;
    }

    public float height() {
        return Height;
    }

    public int attackTicks() {
        return AttackTicks;
    }

    public int hitTick() {
        return HitTick;
    }

    @Nullable
    public Scp610Stride walk() {
        return Walk;
    }

    @Nullable
    public Scp610Stride run() {
        return Run;
    }

    public float scale() {
        return Scale;
    }

    public boolean mobile() {
        return Walk != null && Run != null;
    }

    public boolean evolved() {
        return Size == Kind.Evolved;
    }

    public boolean utility() {
        return Size == Kind.Utility;
    }

    public boolean heavy() {
        return Size == Kind.Large || Size == Kind.Evolved;
    }

    public float strikeChance() {
        return utility() ? 0.0f : heavy() ? 0.6f : 0.35f;
    }

    public float touchChance() {
        return utility() ? 0.0f : 0.1f;
    }

    public Scp610Voice voice() {
        return switch (this) {
            case Apostle -> Scp610Voice.of(Scp610Sounds.ApostleAmbient, Scp610Sounds.ApostleHurt, Scp610Sounds.ApostleDeath, Scp610Sounds.Step, Scp610Sounds.Bite, false, 1.0f, 0.5f);
            case Villager -> Scp610Voice.of(Scp610Sounds.VillagerAmbient, Scp610Sounds.VillagerHurt, Scp610Sounds.VillagerDeath, Scp610Sounds.Step, Scp610Sounds.Bite, false, 1.0f, 0.45f);
            case Pig -> Scp610Voice.of(Scp610Sounds.PigAmbient, Scp610Sounds.PigHurt, Scp610Sounds.PigDeath, Scp610Sounds.StepLight, Scp610Sounds.Bite, false, 1.0f, 0.35f);
            case Cow -> Scp610Voice.of(Scp610Sounds.CowAmbient, Scp610Sounds.CowHurt, Scp610Sounds.CowDeath, Scp610Sounds.Step, Scp610Sounds.Bite, false, 1.0f, 0.45f);
            case Sheep -> Scp610Voice.of(Scp610Sounds.SheepAmbient, Scp610Sounds.SheepHurt, Scp610Sounds.SheepDeath, Scp610Sounds.Step, Scp610Sounds.Bite, false, 1.0f, 0.4f);
            case Chicken -> Scp610Voice.of(Scp610Sounds.ChickenAmbient, Scp610Sounds.ChickenHurt, Scp610Sounds.ChickenDeath, Scp610Sounds.StepLight, Scp610Sounds.Bite, false, 1.1f, 0.25f);
            case Horse -> Scp610Voice.of(Scp610Sounds.HorseAmbient, Scp610Sounds.HorseHurt, Scp610Sounds.HorseDeath, Scp610Sounds.StepHeavy, Scp610Sounds.Bite, true, 1.0f, 0.55f);
            case Crawler -> Scp610Voice.of(Scp610Sounds.CrawlerAmbient, Scp610Sounds.CrawlerHurt, Scp610Sounds.CrawlerDeath, Scp610Sounds.StepLight, Scp610Sounds.Bite, false, 1.0f, 0.25f);
            case Shambler -> Scp610Voice.of(Scp610Sounds.ShamblerAmbient, Scp610Sounds.ShamblerHurt, Scp610Sounds.ShamblerDeath, Scp610Sounds.Step, Scp610Sounds.Bite, false, 1.0f, 0.45f);
            case Hulk -> Scp610Voice.of(Scp610Sounds.HulkAmbient, Scp610Sounds.HulkHurt, Scp610Sounds.HulkDeath, Scp610Sounds.StepHeavy, Scp610Sounds.Slam, true, 1.0f, 0.75f);
            case Amalgam -> Scp610Voice.of(Scp610Sounds.AmalgamAmbient, Scp610Sounds.AmalgamHurt, Scp610Sounds.AmalgamDeath, Scp610Sounds.StepHeavy, Scp610Sounds.Bite, true, 1.0f, 0.8f);
            case Colossus -> Scp610Voice.of(Scp610Sounds.HulkAmbient, Scp610Sounds.HulkHurt, Scp610Sounds.HulkDeath, Scp610Sounds.StepHeavy, Scp610Sounds.Slam, true, 0.8f, 0.95f);
            case Herald -> Scp610Voice.of(Scp610Sounds.HeraldAmbient, Scp610Sounds.HeraldHurt, Scp610Sounds.HeraldDeath, Scp610Sounds.StepHeavy, Scp610Sounds.Lash, true, 1.0f, 0.7f);
            case TendrilWorm -> Scp610Voice.of(Scp610Sounds.CrawlerAmbient, Scp610Sounds.CrawlerHurt, Scp610Sounds.CrawlerDeath, Scp610Sounds.StepLight, Scp610Sounds.Lash, false, 0.7f, 0.2f);
            case Spewer -> Scp610Voice.of(Scp610Sounds.SpewerAmbient, Scp610Sounds.SpewerHurt, Scp610Sounds.SpewerDeath, Scp610Sounds.StepHeavy, Scp610Sounds.Lash, true, 1.0f, 0.5f);
            case HiveLord -> Scp610Voice.of(Scp610Sounds.SpewerAmbient, Scp610Sounds.SpewerHurt, Scp610Sounds.SpewerDeath, Scp610Sounds.StepHeavy, Scp610Sounds.Lash, true, 0.6f, 0.5f);
            case Tender -> Scp610Voice.of(Scp610Sounds.CrawlerAmbient, Scp610Sounds.CrawlerHurt, Scp610Sounds.CrawlerDeath, Scp610Sounds.StepLight, Scp610Sounds.Bite, false, 1.25f, 0.2f);
            case Weaver -> Scp610Voice.of(Scp610Sounds.CrawlerAmbient, Scp610Sounds.CrawlerHurt, Scp610Sounds.CrawlerDeath, Scp610Sounds.StepLight, Scp610Sounds.Lash, false, 0.9f, 0.25f);
            case Harvester -> Scp610Voice.of(Scp610Sounds.ShamblerAmbient, Scp610Sounds.ShamblerHurt, Scp610Sounds.ShamblerDeath, Scp610Sounds.Step, Scp610Sounds.Bite, false, 1.1f, 0.4f);
            case Burrower -> Scp610Voice.of(Scp610Sounds.CrawlerAmbient, Scp610Sounds.CrawlerHurt, Scp610Sounds.CrawlerDeath, Scp610Sounds.StepLight, Scp610Sounds.Bite, false, 0.8f, 0.25f);
            case SporeBearer -> Scp610Voice.of(Scp610Sounds.SpewerAmbient, Scp610Sounds.SpewerHurt, Scp610Sounds.SpewerDeath, Scp610Sounds.StepLight, Scp610Sounds.Lash, false, 1.3f, 0.3f);
            case BroodSac -> Scp610Voice.of(Scp610Sounds.SpewerAmbient, Scp610Sounds.SpewerHurt, Scp610Sounds.SpewerDeath, Scp610Sounds.StepHeavy, Scp610Sounds.Lash, true, 1.1f, 0.5f);
            case SiegeBrute -> Scp610Voice.of(Scp610Sounds.HulkAmbient, Scp610Sounds.HulkHurt, Scp610Sounds.HulkDeath, Scp610Sounds.StepHeavy, Scp610Sounds.Slam, true, 0.85f, 0.9f);
            case Stalker -> Scp610Voice.of(Scp610Sounds.CrawlerAmbient, Scp610Sounds.CrawlerHurt, Scp610Sounds.CrawlerDeath, Scp610Sounds.StepLight, Scp610Sounds.Bite, false, 0.75f, 0.12f);
            case Spitter -> Scp610Voice.of(Scp610Sounds.ShamblerAmbient, Scp610Sounds.ShamblerHurt, Scp610Sounds.ShamblerDeath, Scp610Sounds.Step, Scp610Sounds.Bite, false, 0.9f, 0.45f);
            case Matriarch -> Scp610Voice.of(Scp610Sounds.HulkAmbient, Scp610Sounds.HulkHurt, Scp610Sounds.HulkDeath, Scp610Sounds.StepHeavy, Scp610Sounds.Slam, true, 0.7f, 0.95f);
        };
    }

    @Nullable
    public static Scp610Form byId(String Id) {
        for (Scp610Form Form : values()) {
            if (Form.id().equals(Id)) {
                return Form;
            }
        }
        return null;
    }
}
