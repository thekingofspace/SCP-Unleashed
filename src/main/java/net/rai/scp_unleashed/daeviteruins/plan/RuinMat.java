package net.rai.scp_unleashed.daeviteruins.plan;

public enum RuinMat {
    Air("minecraft:air", Shape.Air),
    RedSandstone("minecraft:red_sandstone", Shape.Cube),
    CutRedSandstone("minecraft:cut_red_sandstone", Shape.Cube),
    SmoothRedSandstone("minecraft:smooth_red_sandstone", Shape.Cube),
    ChiseledRedSandstone("minecraft:chiseled_red_sandstone", Shape.Cube),
    RedSandstoneStairs("minecraft:red_sandstone_stairs", Shape.Stairs),
    SmoothRedSandstoneStairs("minecraft:smooth_red_sandstone_stairs", Shape.Stairs),
    RedSandstoneSlab("minecraft:red_sandstone_slab", Shape.Slab),
    CutRedSandstoneSlab("minecraft:cut_red_sandstone_slab", Shape.Slab),
    SmoothRedSandstoneSlab("minecraft:smooth_red_sandstone_slab", Shape.Slab),
    RedSandstoneWall("minecraft:red_sandstone_wall", Shape.Wall),
    RedSand("minecraft:red_sand", Shape.Cube),
    RedTerracotta("minecraft:red_terracotta", Shape.Cube),
    Terracotta("minecraft:terracotta", Shape.Cube),
    BrownTerracotta("minecraft:brown_terracotta", Shape.Cube),
    OrangeTerracotta("minecraft:orange_terracotta", Shape.Cube),
    BlackTerracotta("minecraft:black_terracotta", Shape.Cube),
    Granite("minecraft:granite", Shape.Cube),
    PolishedGranite("minecraft:polished_granite", Shape.Cube),
    PolishedGraniteStairs("minecraft:polished_granite_stairs", Shape.Stairs),
    PolishedGraniteSlab("minecraft:polished_granite_slab", Shape.Slab),
    GraniteWall("minecraft:granite_wall", Shape.Wall),
    Basalt("minecraft:basalt", Shape.Pillar),
    PolishedBasalt("minecraft:polished_basalt", Shape.Pillar),
    SmoothBasalt("minecraft:smooth_basalt", Shape.Cube),
    Blackstone("minecraft:blackstone", Shape.Cube),
    PolishedBlackstone("minecraft:polished_blackstone", Shape.Cube),
    BlackstoneBricks("minecraft:polished_blackstone_bricks", Shape.Cube),
    CrackedBlackstoneBricks("minecraft:cracked_polished_blackstone_bricks", Shape.Cube),
    ChiseledBlackstone("minecraft:chiseled_polished_blackstone", Shape.Cube),
    GildedBlackstone("minecraft:gilded_blackstone", Shape.Cube),
    BlackstoneStairs("minecraft:blackstone_stairs", Shape.Stairs),
    PolishedBlackstoneStairs("minecraft:polished_blackstone_stairs", Shape.Stairs),
    BlackstoneBrickStairs("minecraft:polished_blackstone_brick_stairs", Shape.Stairs),
    BlackstoneSlab("minecraft:blackstone_slab", Shape.Slab),
    PolishedBlackstoneSlab("minecraft:polished_blackstone_slab", Shape.Slab),
    BlackstoneBrickSlab("minecraft:polished_blackstone_brick_slab", Shape.Slab),
    BlackstoneWall("minecraft:blackstone_wall", Shape.Wall),
    BlackstoneBrickWall("minecraft:polished_blackstone_brick_wall", Shape.Wall),
    BlackstonePlate("minecraft:polished_blackstone_pressure_plate", Shape.Plate),
    StonePlate("minecraft:stone_pressure_plate", Shape.Plate),
    RedNetherBricks("minecraft:red_nether_bricks", Shape.Cube),
    RedNetherBrickStairs("minecraft:red_nether_brick_stairs", Shape.Stairs),
    RedNetherBrickSlab("minecraft:red_nether_brick_slab", Shape.Slab),
    RedNetherBrickWall("minecraft:red_nether_brick_wall", Shape.Wall),
    NetherBricks("minecraft:nether_bricks", Shape.Cube),
    CrackedNetherBricks("minecraft:cracked_nether_bricks", Shape.Cube),
    NetherBrickFence("minecraft:nether_brick_fence", Shape.Fence),
    NetherBrickStairs("minecraft:nether_brick_stairs", Shape.Stairs),
    NetherBrickSlab("minecraft:nether_brick_slab", Shape.Slab),
    Netherrack("minecraft:netherrack", Shape.Cube),
    Magma("minecraft:magma_block", Shape.Cube),
    NetherWartBlock("minecraft:nether_wart_block", Shape.Cube),
    CrimsonPlanks("minecraft:crimson_planks", Shape.Cube),
    CrimsonStem("minecraft:crimson_stem", Shape.Pillar),
    StrippedCrimsonStem("minecraft:stripped_crimson_stem", Shape.Pillar),
    CrimsonFence("minecraft:crimson_fence", Shape.Fence),
    CrimsonSlab("minecraft:crimson_slab", Shape.Slab),
    CrimsonStairs("minecraft:crimson_stairs", Shape.Stairs),
    CrimsonTrapdoor("minecraft:crimson_trapdoor", Shape.Trapdoor),
    DarkOakLog("minecraft:dark_oak_log", Shape.Pillar),
    StrippedDarkOakLog("minecraft:stripped_dark_oak_log", Shape.Pillar),
    DarkOakPlanks("minecraft:dark_oak_planks", Shape.Cube),
    DarkOakFence("minecraft:dark_oak_fence", Shape.Fence),
    DarkOakSlab("minecraft:dark_oak_slab", Shape.Slab),
    DarkOakStairs("minecraft:dark_oak_stairs", Shape.Stairs),
    DarkOakTrapdoor("minecraft:dark_oak_trapdoor", Shape.Trapdoor),
    CoalBlock("minecraft:coal_block", Shape.Cube),
    RedWool("minecraft:red_wool", Shape.Cube),
    RedCarpet("minecraft:red_carpet", Shape.Carpet),
    BlackWool("minecraft:black_wool", Shape.Cube),
    BlackCarpet("minecraft:black_carpet", Shape.Carpet),
    BrownCarpet("minecraft:brown_carpet", Shape.Carpet),
    Copper("minecraft:waxed_cut_copper", Shape.Cube),
    AgedCopper("minecraft:waxed_exposed_cut_copper", Shape.Cube),
    ChiseledCopper("minecraft:waxed_chiseled_copper", Shape.Cube),
    CopperGrate("minecraft:waxed_copper_grate", Shape.See),
    CopperStairs("minecraft:waxed_cut_copper_stairs", Shape.Stairs),
    CopperSlab("minecraft:waxed_cut_copper_slab", Shape.Slab),
    AgedCopperStairs("minecraft:waxed_exposed_cut_copper_stairs", Shape.Stairs),
    AgedCopperSlab("minecraft:waxed_exposed_cut_copper_slab", Shape.Slab),
    CopperBlock("minecraft:waxed_copper_block", Shape.Cube),
    LightningRod("minecraft:lightning_rod", Shape.Rod),
    GoldBlock("minecraft:gold_block", Shape.Cube),
    RawGoldBlock("minecraft:raw_gold_block", Shape.Cube),
    CoarseDirt("minecraft:coarse_dirt", Shape.Cube),
    Gravel("minecraft:gravel", Shape.Cube),
    Dirt("minecraft:dirt", Shape.Cube),
    PackedMud("minecraft:packed_mud", Shape.Cube),
    Tuff("minecraft:tuff", Shape.Cube),
    AshPowder("minecraft:gray_concrete_powder", Shape.Cube),
    SootPowder("minecraft:black_concrete_powder", Shape.Cube),
    SuspiciousSand("minecraft:suspicious_sand", Shape.Brush),
    SuspiciousGravel("minecraft:suspicious_gravel", Shape.Brush),
    Fire("minecraft:fire", Shape.Fire),
    Campfire("minecraft:campfire", Shape.Campfire),
    Lantern("minecraft:lantern", Shape.Lantern),
    RedCandle("minecraft:red_candle", Shape.Candle),
    BlackCandle("minecraft:black_candle", Shape.Candle),
    MossBlock("minecraft:moss_block", Shape.Cube),
    MossCarpet("minecraft:moss_carpet", Shape.Carpet),
    Vine("minecraft:vine", Shape.Vine),
    DeadBush("minecraft:dead_bush", Shape.Plant),
    ShortGrass("minecraft:short_grass", Shape.Plant),
    Fern("minecraft:fern", Shape.Plant),
    Cobweb("minecraft:cobweb", Shape.Web),
    HangingRoots("minecraft:hanging_roots", Shape.Hang),
    IronBars("minecraft:iron_bars", Shape.Bars),
    Ladder("minecraft:ladder", Shape.Banner),
    Chain("minecraft:chain", Shape.Chain),
    Anvil("minecraft:anvil", Shape.Horizontal),
    ChippedAnvil("minecraft:chipped_anvil", Shape.Horizontal),
    DamagedAnvil("minecraft:damaged_anvil", Shape.Horizontal),
    Cauldron("minecraft:cauldron", Shape.Cube),
    Lectern("minecraft:lectern", Shape.Horizontal),
    Bookshelf("minecraft:bookshelf", Shape.Cube),
    ChiseledBookshelf("minecraft:chiseled_bookshelf", Shape.Horizontal),
    Barrel("minecraft:barrel", Shape.Directional),
    Chest("minecraft:chest", Shape.Container),
    TrappedChest("minecraft:trapped_chest", Shape.Container),
    Dispenser("minecraft:dispenser", Shape.Directional),
    Spawner("minecraft:spawner", Shape.Spawner),
    DecoratedPot("minecraft:decorated_pot", Shape.Pot),
    Lava("minecraft:lava", Shape.Fluid),
    BoneBlock("minecraft:bone_block", Shape.Pillar),
    Calcite("minecraft:calcite", Shape.Cube),
    PolishedDiorite("minecraft:polished_diorite", Shape.Cube),
    DioriteWall("minecraft:diorite_wall", Shape.Wall),
    SmoothQuartz("minecraft:smooth_quartz", Shape.Cube),
    SmoothQuartzSlab("minecraft:smooth_quartz_slab", Shape.Slab),
    Growth("minecraft:short_grass", Shape.Growth),
    Ground("minecraft:grass_block", Shape.Ground),
    RuinBrick("scp_unleashed:daevite_ruin_bricks", Shape.Cube),
    RuinReliefHost("scp_unleashed:daevite_ruin_relief_host", Shape.Horizontal),
    RuinReliefGod("scp_unleashed:daevite_ruin_relief_god", Shape.Horizontal),
    RuinColumn("scp_unleashed:daevite_ruin_column", Shape.Pillar),
    RuinBrazier("scp_unleashed:daevite_ruin_brazier", Shape.Brazier),
    RuinBanner("scp_unleashed:daevite_ruin_banner", Shape.Banner),
    RuinIdolHead("scp_unleashed:daevite_ruin_idol_head", Shape.Horizontal),
    RuinIdolHand("scp_unleashed:daevite_ruin_idol_hand", Shape.Horizontal),
    RuinWeaponRack("scp_unleashed:daevite_ruin_weapon_rack", Shape.Horizontal),
    RuinUrn("scp_unleashed:daevite_ruin_urn", Shape.Horizontal);

    public enum Shape {
        Air, Cube, See, Pillar, Stairs, Slab, Wall, Fence, Bars, Horizontal, Directional, Carpet, Plant, Vine, Lantern, Chain, Trapdoor, Candle, Campfire,
        Plate, Fluid, Container, Web, Fire, Brush, Spawner, Rod, Pot, Growth, Ground, Hang, Brazier, Banner
    }

    public static final int Down = 0;
    public static final int Up = 1;
    public static final int North = 2;
    public static final int South = 3;
    public static final int West = 4;
    public static final int East = 5;
    private static final String[] FacingNames = {"down", "up", "north", "south", "west", "east"};
    private static final RuinMat[] All = values();

    private final String Id;
    private final Shape Form;

    RuinMat(String Id, Shape Form) {
        this.Id = Id;
        this.Form = Form;
    }

    public static RuinMat byOrdinal(int Ordinal) {
        return All[Ordinal];
    }

    public String id() {
        return Id;
    }

    public Shape shape() {
        return Form;
    }

    public boolean isAir() {
        return Form == Shape.Air;
    }

    public boolean isOpaqueCube() {
        return Form == Shape.Cube || Form == Shape.Pillar || Form == Shape.Brush || Form == Shape.Ground || this == Barrel || this == Dispenser;
    }

    public boolean isStructural() {
        return switch (Form) {
            case Cube, See, Pillar, Stairs, Slab, Wall, Fence, Bars, Horizontal, Directional, Container, Brush, Spawner, Pot, Ground, Brazier, Trapdoor, Chain, Rod -> true;
            default -> false;
        };
    }

    public boolean carriesLoad() {
        return switch (Form) {
            case Cube, See, Pillar, Stairs, Slab, Wall, Fence, Bars, Directional, Brush, Spawner, Ground -> true;
            case Horizontal -> this == RuinReliefHost || this == RuinReliefGod || this == ChiseledBookshelf || this == Lectern;
            default -> false;
        };
    }

    public boolean isAttachment() {
        return switch (Form) {
            case Carpet, Plant, Vine, Lantern, Candle, Plate, Fire, Web, Growth, Hang, Banner, Campfire -> true;
            default -> false;
        };
    }

    public static int opposite(int Facing) {
        return switch (Facing) {
            case Down -> Up;
            case Up -> Down;
            case North -> South;
            case South -> North;
            case West -> East;
            default -> West;
        };
    }

    public static int[] step(int Facing) {
        return switch (Facing) {
            case Down -> new int[]{0, -1, 0};
            case Up -> new int[]{0, 1, 0};
            case North -> new int[]{0, 0, -1};
            case South -> new int[]{0, 0, 1};
            case West -> new int[]{-1, 0, 0};
            default -> new int[]{1, 0, 0};
        };
    }

    public static String facingName(int Facing) {
        return FacingNames[Math.max(0, Math.min(5, Facing))];
    }

    private static String horizontal(int Facing) {
        return Facing >= North ? FacingNames[Facing] : "south";
    }

    private static String axis(int Facing) {
        return Facing <= Up ? "y" : Facing <= South ? "z" : "x";
    }

    public String state(int Facing, int Variant) {
        String Properties = switch (Form) {
            case Pillar, Chain -> "axis=" + axis(Facing);
            case Stairs -> "facing=" + horizontal(Facing) + ",half=" + ((Variant & 1) != 0 ? "top" : "bottom");
            case Slab -> "type=" + (Variant == 1 ? "top" : Variant == 2 ? "double" : "bottom");
            case Horizontal, Pot, Banner -> "facing=" + horizontal(Facing);
            case Container -> "facing=" + horizontal(Facing) + ",type=single";
            case Directional -> "facing=" + FacingNames[Math.max(0, Math.min(5, Facing))];
            case Rod -> "facing=" + (Facing == Down ? "down" : "up");
            case Trapdoor -> "facing=" + horizontal(Facing) + ",half=" + ((Variant & 1) != 0 ? "top" : "bottom") + ",open=" + ((Variant & 2) != 0);
            case Lantern -> "hanging=" + ((Variant & 1) != 0);
            case Candle -> "candles=" + ((Variant & 3) + 1) + ",lit=false";
            case Campfire -> "facing=" + horizontal(Facing) + ",lit=" + ((Variant & 1) != 0) + ",signal_fire=false";
            case Brazier -> "lit=" + ((Variant & 1) != 0);
            case Vine -> horizontal(Facing) + "=true";
            case Hang -> "";
            default -> "";
        };
        return Properties.isEmpty() ? Id : Id + "[" + Properties + "]";
    }
}
