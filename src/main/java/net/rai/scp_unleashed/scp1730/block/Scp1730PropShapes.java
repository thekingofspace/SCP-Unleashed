package net.rai.scp_unleashed.scp1730.block;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

public final class Scp1730PropShapes {
    public static final String Digits = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final double Step = 0.5;
    public static final Set<String> Fitted = Set.of("scp1730_overturned_desk", "scp1730_sandbags", "scp1730_barricade");
    private static final String[] OverturnedChair = {
            "0000000000000000",
            "0000000000000000",
            "00000000000000ii",
            "00000mmml000hhii",
            "00000mmml000gg66",
            "00o00mmml000ff00",
            "00o00mmml000ff00",
            "00o00mmmleeennpp",
            "00o00mmmleeennpp",
            "00o00mmml000ff00",
            "00o00mmml000ff00",
            "00000mmml000gg66",
            "lllllmmml000hhii",
            "lllllll0000000ii",
            "lllllll000000000",
            "0kkkkkk000000000"
    };
    private static final String[] OverturnedDesk = {
            "0000000000kk0000",
            "000000000f0k0000",
            "0000000fhhhkk000",
            "0000000hhhhhk000",
            "k000000hhhhhkk00",
            "kk000000hhhlkk00",
            "kk000000hllllk00",
            "0k00000llllllkk0",
            "0kk0llllllllllk0",
            "00klllllllllllkk",
            "00klllllllllllmm",
            "00kllllllllmmmm0",
            "000klllllmmmm000",
            "000kllmmmm000000",
            "0000mmmm00000000",
            "0000mm0000000000"
    };
    private static final String[] CrackedTerminal = {
            "0000000222220000",
            "0000022222220000",
            "0022222222222000",
            "0002622222000000",
            "000oom6600000000",
            "000oooom66000000",
            "000ooooooo660000",
            "00oooooooooom660",
            "00ooooooooooooo0",
            "0ooooooooooooo00",
            "000moooooooooo00",
            "000mmmooooooo000",
            "00mmmmmmmoooo000",
            "00mmmmmmmm2oo000",
            "0000mmmmmm000000",
            "0000000mmm000000"
    };
    private static final String[] Gurney = {
            "0000000000000000",
            "00eiiiiiiiiiie00",
            "00eiiiiiiiiiie00",
            "00ejjjjjjjjjjid0",
            "00ejjjjjjjjjjid0",
            "00ejjjjjjjjjjid0",
            "00ejjjjjjjjjjid0",
            "00ejjjjjjjjjjid0",
            "00ejjjjjjjjjjid0",
            "00ejjjjjjjjjje00",
            "00ejjjjjjjjjje00",
            "00ekkkkkkkkkke00",
            "00ekkkkkkkkkke00",
            "00ekkkkkkkkkke00",
            "00eiiiiiiiiiie00",
            "0000000000000000"
    };
    private static final String[] FallenPanel = {
            "0000000000022000",
            "0000000000222220",
            "0444444444444440",
            "0444444444444440",
            "0555555555555550",
            "0666666666666660",
            "0777777777777770",
            "0888888888888880",
            "0999999999999990",
            "0999999999999990",
            "0aaaaaaaaaaaaaa0",
            "0bbbbbbbbbbbbbb0",
            "0cccccccccccccc0",
            "0000999999555000",
            "0000999900555000",
            "0000090000555000"
    };
    private static final String[] CableTangle = {
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000077000",
            "0000000000077770",
            "0000000000777770",
            "0000000000077700",
            "0000000000000700"
    };
    private static final String[] BodyBag = {
            "0888000000000000",
            "8888007700000000",
            "8880477885000000",
            "8804778998000000",
            "0047789998000000",
            "0077899987700000",
            "0778999877880500",
            "0789998778888050",
            "0089987788886000",
            "0058877888866600",
            "0000078888666650",
            "0000008886666555",
            "0000000866665557",
            "0000005006655579",
            "0000000500555779",
            "0000000000057990"
    };
    private static final String[] CellDoorWreck = {
            "0033330000000400",
            "0233332222244440",
            "0333333333334440",
            "0444444444444444",
            "0555555555550400",
            "0555555555550000",
            "0444444444444440",
            "0444444444444440",
            "4444444444444440",
            "4444444444444440",
            "4444444444444440",
            "0444444444444440",
            "4444444444444440",
            "4444444444444440",
            "4444444444444440",
            "0000000000000000"
    };
    private static final String[] Rubble = {
            "0000000000000000",
            "0000080000400000",
            "0088888004440220",
            "8888888004444220",
            "0888ddddd0440000",
            "0888ccccc0000000",
            "0888bbbbb6660000",
            "0088bbbbb6666600",
            "0088000066a99660",
            "0005555066a99660",
            "0005555666a99600",
            "0005555666666600",
            "0005555006666600",
            "0005555000006000",
            "0000000000000000",
            "0000000000000000"
    };
    private static final String[] WeaponRack = {
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "00nn000000000000",
            "0oooooooooooooo0",
            "0oooooooooooooo0",
            "0oooooooooooooo0",
            "0uuuuuuuuuuuuuu0"
    };
    private static final String[] AmmoCrate = {
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0dddddddddddddd0",
            "0dddddddddddddd0",
            "0dddddddddddddd0",
            "0dddddddddddddd0",
            "0dddddddddddddd0",
            "0dddddddddddddd0",
            "0tttttttttttttt0",
            "0ssssssssssssss0",
            "0nnnnnnnnnnnnnn0",
            "0iiiiiiiiiiiiii0",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000"
    };
    private static final String[] Conduit = {
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "uutttuuuuuuttttt",
            "uutttuuuuuutwwtt",
            "uutttuuuuuutwwtt",
            "uu000uuuuuu0ww00"
    };
    private static final String[] BrokenVent = {
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000022200000",
            "0000002222220000",
            "0000022222220000",
            "0000022222200000",
            "0000002220000000",
            "0pppppppppppppp0",
            "0pppppppppppppp0"
    };
    private static final String[] WallSign = {
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "uuuuuuuuuuuuuuuu",
            "uuuuuuuuuuuuuuuu"
    };
    private static final String[] BrokenGlass = {
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "wwwwwwwwwwwwwwww",
            "wwwwwwwwwwwwwwww",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000"
    };
    private static final String[] Sandbags = {
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "6666666666666666",
            "kkkkkkkkkkkkkkkk",
            "kmmmsssssssmmmmk",
            "kmmqtttttttqmmmk",
            "kmmqtttttttqmmmk",
            "kmmqtttttttqmmmk",
            "kkkkssssssskkkkk",
            "6666eeeeeeee0000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000"
    };
    private static final String[] Barricade = {
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "mponmlkkjihggfed",
            "mponmlkkjihhijkj",
            "0mm0000000000mm0",
            "qqqqqqqqqqqqqqqq",
            "qqqqqqqqqqqqqqqq",
            "qqqqqqqqqqqqqqqq",
            "qqqqqqqqqqqqqqqq",
            "0399999999999930",
            "0044444444444400",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000"
    };
    private static final String[] OverturnedBench = {
            "0002000000000000",
            "0002222000000000",
            "0002222220000000",
            "0022222022220000",
            "0022222222220200",
            "0222220222222222",
            "0222220222202222",
            "0gg2222222202222",
            "eefgg02222222222",
            "eeee0ggg22022222",
            "0eeeeeffgg222220",
            "0e000eeee0ggg220",
            "ie00000eeeeffgg0",
            "0i00000000eeee00",
            "00000000000eee00",
            "0000000000ee0000"
    };
    private static final String[] MicStand = {
            "0000000000000000",
            "0000000PP0000000",
            "0000000RR0000000",
            "0000000QQ0000000",
            "0000000PP0000000",
            "0000022PP0000000",
            "000002v20v000000",
            "000000xGGx000000",
            "000000xGGx000000",
            "000000v02v200000",
            "0000000002200000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000"
    };
    private static final String[] SpeakerCabinet = {
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "00yy00000000yy00",
            "00yyxxxxxxxxyy00",
            "000xxxxxxxxxx000",
            "000xxxxxxxxxx000",
            "000xxxxxxxxxx000",
            "000xxxxxxxxxx000",
            "000xxxxxxxxxx000",
            "000xxxxxxxxxx000",
            "000xxxxxxxxxx000",
            "000wwwwwwwwww000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000"
    };
    private static final String[] StudioMonitor = {
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "000yy000000yy000",
            "000yyxxxxxxyy000",
            "0000xxxxxxxx0000",
            "0000xxxxxxxx0000",
            "0000xxxxxxxx0000",
            "0000xxxxxxxx0000",
            "0000xxxxxxxx0000",
            "0000000ll0000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000"
    };
    private static final Map<String, String[]> Heights = Map.ofEntries(
            Map.entry("scp1730_overturned_chair", OverturnedChair),
            Map.entry("scp1730_overturned_desk", OverturnedDesk),
            Map.entry("scp1730_cracked_terminal", CrackedTerminal),
            Map.entry("scp1730_gurney", Gurney),
            Map.entry("scp1730_fallen_panel", FallenPanel),
            Map.entry("scp1730_cable_tangle", CableTangle),
            Map.entry("scp1730_body_bag", BodyBag),
            Map.entry("scp1730_cell_door_wreck", CellDoorWreck),
            Map.entry("scp1730_rubble", Rubble),
            Map.entry("scp1730_weapon_rack", WeaponRack),
            Map.entry("scp1730_ammo_crate", AmmoCrate),
            Map.entry("scp1730_conduit", Conduit),
            Map.entry("scp1730_broken_vent", BrokenVent),
            Map.entry("scp1730_sign_memetic", WallSign),
            Map.entry("scp1730_sign_high_voltage", WallSign),
            Map.entry("scp1730_sign_biohazard", WallSign),
            Map.entry("scp1730_sign_keter", WallSign),
            Map.entry("scp1730_sign_sector_a", WallSign),
            Map.entry("scp1730_sign_sector_b", WallSign),
            Map.entry("scp1730_sign_sector_c", WallSign),
            Map.entry("scp1730_sign_sector_d", WallSign),
            Map.entry("scp1730_broken_glass", BrokenGlass),
            Map.entry("scp1730_sandbags", Sandbags),
            Map.entry("scp1730_barricade", Barricade),
            Map.entry("scp1730_overturned_bench", OverturnedBench),
            Map.entry("scp1730_mic_stand", MicStand),
            Map.entry("scp1730_speaker_cabinet", SpeakerCabinet),
            Map.entry("scp1730_studio_monitor", StudioMonitor));

    private Scp1730PropShapes() {
    }

    public static boolean item(CollisionContext Context) {
        return Context instanceof EntityCollisionContext Holder && Holder.getEntity() instanceof ItemEntity;
    }

    @Nullable
    public static VoxelShape resting(String Id) {
        String[] Rows = Heights.get(Id);
        return Rows == null ? null : heightmap(Rows);
    }

    @Nullable
    public static VoxelShape fitted(String Id) {
        VoxelShape Rest = Fitted.contains(Id) ? resting(Id) : null;
        if (Rest == null || Rest.isEmpty()) {
            return null;
        }
        AABB Bounds = Rest.bounds();
        return Shapes.box(Bounds.minX, 0.0, Bounds.minZ, Bounds.maxX, Bounds.maxY, Bounds.maxZ);
    }

    public static VoxelShape heightmap(String[] Rows) {
        VoxelShape Made = Shapes.empty();
        for (int Z = 0; Z < Rows.length; Z++) {
            String Row = Rows[Z];
            int X = 0;
            while (X < Row.length()) {
                char Mark = Row.charAt(X);
                int End = X + 1;
                while (End < Row.length() && Row.charAt(End) == Mark) {
                    End++;
                }
                int Height = Digits.indexOf(Mark);
                if (Height > 0) {
                    Made = Shapes.or(Made, Block.box(X, 0.0, Z, End, Height * Step, Z + 1));
                }
                X = End;
            }
        }
        return Made.optimize();
    }
}
