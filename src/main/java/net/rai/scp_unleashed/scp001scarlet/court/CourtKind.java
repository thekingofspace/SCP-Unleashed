package net.rai.scp_unleashed.scp001scarlet.court;

public enum CourtKind {
    Thrall("scarlet_thrall", "thrall", 0.6f, 1.9f, 24.0, 4.0, 0.23, 2.0, 0x5C1A1F, 0xB8A890),
    ChainHound("scarlet_chain_hound", "hound", 0.9f, 1.1f, 28.0, 5.0, 0.33, 2.0, 0x2A1416, 0x8A8C94),
    ThornCrawler("scarlet_thorn_crawler", "crawler", 1.2f, 0.6f, 20.0, 3.0, 0.28, 2.0, 0x120E12, 0x9E2A2B),
    Eyeling("scarlet_eyeling", "eyeling", 0.6f, 0.6f, 6.0, 2.0, 0.5, 0.0, 0x161014, 0xF08A30),
    CrimsonKnight("scarlet_crimson_knight", "knight", 0.8f, 2.3f, 80.0, 9.0, 0.22, 12.0, 0x1E1C22, 0x9E2A2B),
    FlayedHerald("scarlet_flayed_herald", "herald", 0.6f, 2.1f, 30.0, 6.0, 0.25, 2.0, 0x7A1E22, 0xE8E2D2),
    BellRinger("scarlet_bell_ringer", "bell", 0.9f, 1.8f, 40.0, 3.0, 0.2, 4.0, 0x3A2A22, 0x9E2A2B),
    AshWraith("scarlet_ash_wraith", "wraith", 0.6f, 1.9f, 26.0, 5.0, 0.3, 0.0, 0x2C2A2C, 0xE8E2D2),
    BrideHusk("scarlet_bride_husk", "husk", 1.1f, 2.2f, 90.0, 8.0, 0.17, 6.0, 0x8E1E24, 0xE8E2D2),
    AntlerStalker("scarlet_antler_stalker", "stalker", 0.7f, 2.0f, 34.0, 6.0, 0.34, 2.0, 0x100C10, 0x6E1A1E),
    Gnawer("scarlet_gnawer", "gnawer", 0.5f, 0.4f, 4.0, 2.0, 0.36, 0.0, 0x4A1216, 0xF0D8A0),
    ThroneGolem("scarlet_throne_golem", "golem", 1.6f, 3.1f, 160.0, 12.0, 0.18, 10.0, 0x2E2A2E, 0xC23A2A),
    StarMoth("scarlet_star_moth", "moth", 0.6f, 0.5f, 8.0, 3.0, 0.6, 0.0, 0x2A2448, 0xC8D4F0);

    private final String Id;
    private final String SoundKey;
    private final float Width;
    private final float Height;
    private final double Health;
    private final double Damage;
    private final double Speed;
    private final double Armor;
    private final int EggBase;
    private final int EggSpots;

    CourtKind(String Id, String SoundKey, float Width, float Height, double Health, double Damage, double Speed, double Armor, int EggBase, int EggSpots) {
        this.Id = Id;
        this.SoundKey = SoundKey;
        this.Width = Width;
        this.Height = Height;
        this.Health = Health;
        this.Damage = Damage;
        this.Speed = Speed;
        this.Armor = Armor;
        this.EggBase = EggBase;
        this.EggSpots = EggSpots;
    }

    public String id() {
        return Id;
    }

    public String soundKey() {
        return SoundKey;
    }

    public float width() {
        return Width;
    }

    public float height() {
        return Height;
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

    public double armor() {
        return Armor;
    }

    public int eggBase() {
        return EggBase;
    }

    public int eggSpots() {
        return EggSpots;
    }

    public boolean flying() {
        return this == Eyeling || this == StarMoth;
    }
}
