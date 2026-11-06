package net.rai.scp_unleashed.scp5001.props.lab;

public enum Scp5001LabProp {
    Bench("scp5001_lab_bench", Kind.Floor, 4, Sound.Stone, new int[][]{{0, 0, 0, 16, 16, 16}}, new int[0][]),
    SpecimenTank("scp5001_lab_specimen_tank", Kind.FloorTall, 10, Sound.Glass, new int[][]{{0, 0, 0, 16, 16, 16}}, new int[][]{{2, 0, 2, 14, 16, 14}, {5, 10, 1, 11, 15, 2}, {1, 10, 5, 2, 15, 11}, {14, 10, 5, 15, 15, 11}, {5, 10, 14, 11, 15, 15}}),
    AnalysisColumn("scp5001_lab_analysis_column", Kind.FloorTall, 9, Sound.Metal, new int[][]{{1, 0, 0, 15, 4, 15}, {4, 4, 1, 12, 16, 15}, {1, 4, 4, 4, 15, 12}, {12, 4, 4, 15, 15, 12}}, new int[][]{{4, 0, 4, 12, 16, 12}, {4, 4, 2, 12, 13, 4}, {2, 4, 4, 4, 13, 12}, {12, 4, 4, 14, 13, 12}, {4, 4, 12, 12, 13, 14}}),
    SampleRack("scp5001_lab_sample_rack", Kind.Floor, 5, Sound.Metal, new int[][]{{1, 0, 4, 15, 4, 14}, {1, 4, 4, 15, 16, 10}, {1, 4, 10, 4, 16, 14}, {12, 4, 10, 15, 16, 14}, {4, 4, 12, 12, 16, 14}, {4, 8, 10, 12, 9, 12}, {4, 14, 10, 12, 16, 12}}, new int[0][]),
    HoloScope("scp5001_lab_holo_scope", Kind.Floor, 6, Sound.Metal, new int[][]{{4, 0, 2, 12, 12, 14}, {2, 0, 4, 4, 8, 12}, {12, 0, 4, 14, 8, 12}, {2, 7, 2, 4, 8, 4}, {12, 7, 2, 14, 8, 4}, {2, 7, 12, 4, 8, 14}, {12, 7, 12, 14, 8, 14}, {5, 12, 5, 11, 16, 13}}, new int[0][]),
    CeilingLight("scp5001_lab_ceiling_light", Kind.Ceiling, 15, Sound.Metal, new int[][]{{6, 12, 6, 10, 16, 10}, {4, 14, 0, 12, 16, 6}, {2, 14, 2, 4, 16, 14}, {12, 14, 2, 14, 16, 14}, {0, 14, 4, 2, 16, 12}, {14, 14, 4, 16, 16, 12}, {4, 14, 6, 6, 16, 16}, {10, 14, 6, 12, 16, 16}, {6, 14, 10, 10, 16, 16}}, new int[0][]),
    WallCabinet("scp5001_lab_wall_cabinet", Kind.Wall, 7, Sound.Metal, new int[][]{{1, 2, 9, 15, 14, 15}, {2, 3, 15, 14, 14, 16}, {2, 4, 8, 3, 13, 9}, {9, 4, 8, 13, 6, 9}, {5, 6, 8, 7, 11, 9}}, new int[0][]),
    WallSterilizer("scp5001_lab_wall_sterilizer", Kind.Wall, 8, Sound.Metal, new int[][]{{3, 2, 10, 13, 13, 16}, {2, 2, 11, 3, 13, 16}, {13, 2, 11, 14, 13, 16}, {1, 2, 15, 2, 14, 16}, {14, 2, 15, 15, 14, 16}, {3, 13, 11, 13, 14, 16}, {2, 13, 15, 3, 14, 16}, {13, 13, 15, 14, 14, 16}, {6, 14, 11, 10, 15, 15}}, new int[0][]),
    BenchConsole("scp5001_lab_bench_console", Kind.Floor, 6, Sound.Stone, new int[][]{{0, 0, 0, 16, 16, 16}}, new int[0][]),
    BenchDish("scp5001_lab_bench_dish", Kind.Floor, 6, Sound.Stone, new int[][]{{0, 0, 0, 16, 16, 16}}, new int[0][]),
    BenchBasin("scp5001_lab_bench_basin", Kind.Floor, 5, Sound.Stone, new int[][]{{0, 0, 0, 16, 14, 16}, {0, 14, 0, 16, 15, 4}, {0, 14, 4, 4, 16, 16}, {12, 14, 4, 16, 16, 16}, {4, 14, 11, 12, 16, 16}}, new int[0][]),
    WallDisplay("scp5001_lab_wall_display", Kind.Wall, 8, Sound.Metal, new int[][]{{2, 2, 13, 14, 14, 15}, {1, 3, 12, 15, 13, 13}, {1, 3, 13, 2, 14, 15}, {14, 3, 13, 15, 14, 15}, {3, 3, 15, 13, 13, 16}, {0, 5, 13, 1, 12, 15}, {15, 5, 13, 16, 12, 15}, {5, 14, 13, 11, 15, 15}}, new int[0][]),
    WallVialShelf("scp5001_lab_wall_vial_shelf", Kind.Wall, 5, Sound.Metal, new int[][]{{1, 1, 9, 15, 15, 16}}, new int[0][]),
    SpecimenTankViolet("scp5001_lab_specimen_tank_violet", Kind.FloorTall, 10, Sound.Glass, new int[][]{{0, 0, 0, 16, 12, 16}, {4, 12, 2, 12, 16, 14}, {2, 12, 4, 4, 16, 12}, {12, 12, 4, 14, 16, 12}}, new int[][]{{1, 0, 0, 15, 16, 15}}),
    SpecimenTankDrained("scp5001_lab_specimen_tank_drained", Kind.FloorTall, 5, Sound.Glass, new int[][]{{0, 0, 0, 16, 16, 16}}, new int[][]{{2, 0, 2, 14, 16, 14}, {5, 12, 1, 11, 16, 2}, {1, 12, 5, 2, 16, 11}, {14, 12, 5, 15, 16, 11}, {5, 12, 14, 11, 16, 15}}),
    SampleCarousel("scp5001_lab_sample_carousel", Kind.Floor, 6, Sound.Metal, new int[][]{{0, 0, 0, 16, 4, 16}, {4, 4, 1, 12, 15, 15}, {1, 4, 4, 4, 13, 12}, {12, 4, 4, 15, 13, 12}, {3, 12, 3, 4, 13, 4}, {12, 12, 3, 13, 13, 4}, {3, 12, 12, 4, 13, 13}, {12, 12, 12, 13, 13, 13}}, new int[0][]),
    CryoVault("scp5001_lab_cryo_vault", Kind.Floor, 5, Sound.Metal, new int[][]{{2, 0, 3, 14, 11, 13}, {2, 1, 2, 14, 12, 3}, {1, 1, 3, 2, 12, 14}, {14, 1, 3, 15, 12, 14}, {2, 1, 13, 14, 12, 14}, {11, 3, 1, 14, 6, 2}, {1, 9, 2, 2, 12, 3}, {14, 9, 2, 15, 12, 3}, {6, 10, 1, 10, 11, 2}}, new int[0][]),
    FumeHood("scp5001_lab_fume_hood", Kind.FloorTall, 9, Sound.Metal, new int[][]{{0, 0, 1, 16, 14, 16}, {2, 14, 1, 14, 16, 3}, {0, 14, 2, 2, 16, 16}, {14, 14, 2, 16, 16, 16}, {9, 14, 6, 14, 16, 10}, {5, 14, 10, 6, 16, 11}, {8, 14, 10, 11, 16, 12}, {2, 14, 15, 14, 16, 16}}, new int[][]{{0, 0, 2, 16, 15, 4}, {0, 0, 4, 2, 15, 16}, {14, 0, 4, 16, 15, 16}, {4, 0, 9, 7, 2, 12}, {8, 0, 10, 11, 1, 12}, {2, 0, 14, 14, 15, 16}, {2, 9, 0, 14, 14, 2}, {1, 11, 1, 2, 14, 2}, {14, 11, 1, 15, 14, 2}, {2, 10, 4, 14, 16, 14}}),
    IsolationHood("scp5001_lab_isolation_hood", Kind.FloorTall, 8, Sound.Metal, new int[][]{{1, 0, 2, 15, 2, 14}, {2, 0, 14, 14, 1, 15}, {2, 2, 4, 14, 16, 14}, {1, 9, 1, 15, 14, 4}, {1, 9, 4, 2, 14, 15}, {14, 9, 4, 15, 14, 15}, {2, 9, 14, 14, 13, 15}, {1, 14, 2, 15, 16, 4}, {1, 14, 13, 2, 16, 14}, {14, 14, 13, 15, 16, 14}}, new int[][]{{1, 0, 1, 16, 12, 14}, {3, 12, 4, 13, 15, 12}}),
    HoverCart("scp5001_lab_hover_cart", Kind.Floor, 5, Sound.Metal, new int[][]{{2, 0, 2, 14, 8, 14}, {8, 8, 5, 12, 9, 8}, {3, 8, 12, 13, 14, 14}}, new int[0][]),
    InstrumentCart("scp5001_lab_instrument_cart", Kind.Floor, 4, Sound.Metal, new int[][]{{1, 0, 2, 15, 4, 14}, {2, 4, 3, 4, 12, 13}, {12, 4, 3, 15, 12, 13}, {4, 4, 4, 12, 12, 12}, {4, 10, 3, 12, 12, 4}, {4, 10, 12, 12, 12, 13}}, new int[0][]),
    DataSlates("scp5001_lab_data_slates", Kind.Floor, 3, Sound.Metal, new int[][]{{2, 0, 3, 9, 1, 11}, {9, 0, 6, 15, 1, 15}, {7, 0, 11, 9, 1, 15}, {2, 0, 12, 7, 1, 13}}, new int[0][]),
    ToolTray("scp5001_lab_tool_tray", Kind.Floor, 2, Sound.Metal, new int[][]{{2, 0, 3, 14, 1, 13}, {2, 1, 3, 14, 2, 4}, {2, 1, 4, 3, 2, 13}, {13, 1, 4, 14, 2, 13}, {9, 1, 6, 11, 2, 8}, {4, 1, 8, 7, 2, 11}, {7, 1, 9, 8, 2, 11}, {10, 1, 9, 11, 2, 11}, {3, 1, 12, 13, 2, 13}}, new int[0][]),
    VialCase("scp5001_lab_vial_case", Kind.Floor, 3, Sound.Metal, new int[][]{{3, 0, 4, 13, 3, 12}, {4, 3, 5, 12, 4, 12}, {3, 3, 6, 4, 9, 12}, {12, 3, 6, 13, 9, 12}, {4, 4, 6, 12, 9, 12}}, new int[0][]),
    Canisters("scp5001_lab_canisters", Kind.Floor, 3, Sound.Metal, new int[][]{{3, 0, 3, 7, 1, 9}, {9, 0, 3, 12, 1, 8}, {2, 0, 4, 3, 1, 8}, {7, 0, 4, 9, 1, 7}, {12, 0, 4, 13, 1, 7}, {7, 0, 7, 8, 1, 8}, {6, 0, 9, 11, 1, 13}, {3, 1, 4, 7, 7, 8}, {9, 1, 4, 12, 5, 7}, {7, 1, 9, 10, 4, 13}}, new int[0][]),
    SpecimenJar("scp5001_lab_specimen_jar", Kind.Floor, 6, Sound.Glass, new int[][]{{4, 0, 4, 12, 8, 12}, {6, 8, 6, 10, 9, 10}}, new int[0][]),
    DishStack("scp5001_lab_dish_stack", Kind.Floor, 4, Sound.Metal, new int[][]{{3, 0, 4, 10, 1, 11}, {10, 0, 10, 15, 1, 16}, {4, 1, 4, 10, 2, 11}, {3, 1, 5, 4, 3, 10}, {10, 1, 5, 11, 2, 10}, {3, 2, 4, 9, 3, 5}, {2, 2, 5, 3, 3, 10}, {4, 2, 5, 10, 3, 10}, {3, 2, 10, 9, 3, 11}}, new int[0][]),
    CeilingArm("scp5001_lab_ceiling_arm", Kind.Ceiling, 6, Sound.Metal, new int[][]{{5, 4, 1, 11, 8, 6}, {6, 7, 6, 10, 10, 12}, {7, 8, 3, 9, 9, 6}, {6, 10, 8, 10, 16, 12}, {6, 14, 6, 10, 16, 8}, {4, 14, 8, 6, 16, 12}, {10, 14, 8, 12, 16, 12}, {6, 14, 12, 10, 16, 14}}, new int[0][]),
    CeilingScanner("scp5001_lab_ceiling_scanner", Kind.Ceiling, 9, Sound.Metal, new int[][]{{0, 9, 0, 16, 11, 16}, {4, 12, 4, 12, 16, 12}}, new int[0][]),
    Centrifuge("scp5001_lab_centrifuge", Kind.FloorTall, 8, Sound.Metal, new int[][]{{1, 0, 1, 15, 12, 16}, {4, 12, 2, 12, 16, 16}, {2, 12, 4, 4, 15, 12}, {12, 12, 4, 14, 15, 12}}, new int[][]{{1, 0, 1, 15, 4, 16}, {3, 4, 12, 13, 14, 16}}),
    SpectralArray("scp5001_lab_spectral_array", Kind.FloorTall, 10, Sound.Metal, new int[][]{{4, 0, 0, 12, 4, 16}, {2, 0, 2, 4, 4, 14}, {12, 0, 2, 14, 4, 14}, {0, 0, 4, 2, 4, 12}, {14, 0, 4, 16, 4, 12}, {4, 4, 4, 12, 5, 12}, {1, 4, 6, 4, 16, 10}, {12, 4, 6, 15, 16, 10}}, new int[][]{{4, 0, 5, 12, 4, 11}, {1, 0, 6, 4, 14, 10}, {12, 0, 6, 15, 14, 10}, {4, 11, 6, 12, 14, 10}, {6, 14, 7, 10, 15, 9}}),
    ScannerRing("scp5001_lab_scanner_ring", Kind.Floor, 8, Sound.Metal, new int[][]{{4, 0, 1, 12, 15, 15}, {1, 0, 4, 4, 15, 12}, {12, 0, 4, 15, 15, 12}, {3, 10, 3, 4, 12, 4}, {12, 10, 3, 13, 12, 4}, {3, 10, 12, 4, 12, 13}, {12, 10, 12, 13, 12, 13}}, new int[0][]);

    public enum Kind {
        Floor, FloorTall, Wall, Ceiling
    }

    public enum Sound {
        Metal, Glass, Stone
    }

    private final String Id;
    private final Kind Type;
    private final int Light;
    private final Sound Material;
    private final int[][] Lower;
    private final int[][] Upper;

    Scp5001LabProp(String Id, Kind Type, int Light, Sound Material, int[][] Lower, int[][] Upper) {
        this.Id = Id;
        this.Type = Type;
        this.Light = Light;
        this.Material = Material;
        this.Lower = Lower;
        this.Upper = Upper;
    }

    public String id() {
        return Id;
    }

    public Kind kind() {
        return Type;
    }

    public int light() {
        return Light;
    }

    public Sound sound() {
        return Material;
    }

    public int[][] lower() {
        return Lower;
    }

    public int[][] upper() {
        return Upper;
    }
}
