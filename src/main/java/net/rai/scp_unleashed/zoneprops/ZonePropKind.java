package net.rai.scp_unleashed.zoneprops;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.props.PropShapes;

import static net.rai.scp_unleashed.props.PropShapes.box;

public enum ZonePropKind implements StringRepresentable {
    LczIntercom("lcz_intercom", ZoneMount.Wall, "mount", box(3, 1, 12, 13, 16, 16)),
    LczNoticeBoard("lcz_notice_board", ZoneMount.Wall, "mount", box(1, 2, 14, 15, 15, 16)),
    LczWallPipes("lcz_wall_pipes", ZoneMount.Wall, "mount", box(0, 0, 9, 16, 16, 16)),
    LczSupplyLocker("lcz_supply_locker", ZoneMount.Stack, "stack",
            PropShapes.of(box(1, 0, 3, 15, 2, 15), box(0, 2, 2, 16, 13, 16), box(0, 13, 1, 16, 16, 16)),
            PropShapes.of(box(1, 0, 3, 15, 2, 15), box(0, 2, 2, 16, 16, 16)),
            box(0, 0, 2, 16, 16, 16),
            PropShapes.of(box(0, 0, 2, 16, 13, 16), box(0, 13, 1, 16, 16, 16))),
    LczCheckpointBarrier("lcz_checkpoint_barrier", ZoneMount.SidePair, "pair",
            PropShapes.of(box(9, 0, 4, 15, 16, 12), box(0, 8, 6, 9, 12, 10)),
            PropShapes.of(box(0, 8, 6, 16, 12, 10), box(2, 0, 5, 7, 12, 11))),
    LczCellBunk("lcz_cell_bunk", ZoneMount.Pair, "pair",
            PropShapes.of(box(1, 0, 0, 15, 9, 14), box(0, 0, 14, 16, 10, 16)),
            PropShapes.of(box(0, 0, 0, 16, 16, 2), box(1, 0, 2, 15, 10, 16), box(15, 0, 2, 16, 14, 11))),
    HczValvePanel("hcz_valve_panel", ZoneMount.Wall, "mount", box(1, 0, 8, 15, 16, 16)),
    HczPipeManifold("hcz_pipe_manifold", ZoneMount.Stack, "stack",
            PropShapes.of(box(0, 0, 7, 16, 4, 15), box(0, 4, 8, 16, 16, 16)),
            PropShapes.of(box(0, 0, 7, 16, 4, 15), box(0, 4, 7, 16, 16, 16)),
            box(0, 0, 8, 16, 16, 16),
            box(0, 0, 8, 16, 16, 16)),
    HczSteamVent("hcz_steam_vent", ZoneMount.Stack, "stack",
            PropShapes.of(box(2, 0, 2, 14, 2, 14), box(3, 2, 3, 13, 11, 13), box(2, 11, 2, 14, 16, 14)),
            PropShapes.of(box(2, 0, 2, 14, 2, 14), box(3, 2, 3, 13, 16, 13)),
            box(3, 0, 3, 13, 16, 13),
            PropShapes.of(box(3, 0, 3, 13, 11, 13), box(2, 11, 2, 14, 16, 14))),
    HczCagedLamp("hcz_caged_lamp", ZoneMount.WallStack, "stack", box(4, 0, 7, 12, 16, 16)),
    HczHazardCrate("hcz_hazard_crate", ZoneMount.Stack, "stack", box(1, 0, 1, 15, 16, 15)),
    HczMonitorConsole("hcz_monitor_console", ZoneMount.Stack, "stack",
            PropShapes.of(box(0, 0, 4, 16, 10, 16), box(2, 10, 9, 14, 16, 15)),
            PropShapes.of(box(0, 0, 4, 16, 11, 16), box(0, 11, 10, 16, 16, 16)),
            PropShapes.of(box(0, 0, 11, 16, 5, 16), box(0, 5, 9, 16, 16, 16)),
            PropShapes.of(box(0, 0, 10, 16, 8, 16), box(0, 8, 8, 16, 12, 16))),
    EzDirectorySign("ez_directory_sign", ZoneMount.Wall, "mount", box(1, 1, 14, 15, 16, 16)),
    EzPlanter("ez_planter", ZoneMount.Floor, null, PropShapes.of(box(2, 0, 2, 14, 9, 14), box(3, 9, 3, 13, 16, 13))),
    EzWaitingChairs("ez_waiting_chairs", ZoneMount.Floor, "row",
            PropShapes.of(box(0, 0, 3, 16, 7, 13), box(0, 7, 4, 16, 10, 13), box(1, 10, 11, 15, 15, 13))),
    EzTurnstile("ez_turnstile", ZoneMount.Floor, null, PropShapes.of(box(9, 0, 1, 16, 15, 15), box(1, 6, 6, 9, 13, 10))),
    EzDisplayCase("ez_display_case", ZoneMount.Stack, "stack",
            PropShapes.of(box(0, 0, 0, 16, 1, 16), box(1, 1, 1, 15, 15, 15), box(0, 15, 0, 16, 16, 16)),
            PropShapes.of(box(0, 0, 0, 16, 1, 16), box(1, 1, 1, 15, 16, 15)),
            box(1, 0, 1, 15, 16, 15),
            PropShapes.of(box(1, 0, 1, 15, 15, 15), box(0, 15, 0, 16, 16, 16))),
    EzDummyCameraDome("ez_dummy_camera_dome", ZoneMount.Ceiling, "mount", box(4, 6, 3, 12, 16, 12));

    public static final Codec<ZonePropKind> KindCodec = StringRepresentable.fromEnum(ZonePropKind::values);

    private final String Name;
    private final ZoneMount Mount;
    private final String Detail;
    private final VoxelShape[] Shapes;

    ZonePropKind(String Name, ZoneMount Mount, String Detail, VoxelShape... Shapes) {
        this.Name = Name;
        this.Mount = Mount;
        this.Detail = Detail;
        this.Shapes = Shapes;
    }

    public ZoneMount getMount() {
        return Mount;
    }

    public VoxelShape getShape(int Index) {
        return Shapes[Math.min(Index, Shapes.length - 1)];
    }

    public String[] details() {
        return Detail == null ? new String[0] : new String[]{Detail};
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
