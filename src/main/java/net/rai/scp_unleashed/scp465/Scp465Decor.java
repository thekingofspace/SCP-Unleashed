package net.rai.scp_unleashed.scp465;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.props.PropShapes;

public enum Scp465Decor {
    DanceFloor("dance_floor", PropShapes.box(0, 0, 0, 16, 1, 16), true, 6, false, MapColor.COLOR_MAGENTA, SoundType.GLASS),
    DjDeck("dj_deck", PropShapes.of(PropShapes.box(0, 0, 2, 16, 12, 15), PropShapes.box(0, 12, 1, 16, 14, 15)), true, 7, false, MapColor.COLOR_BLACK, SoundType.METAL),
    Speaker("speaker", PropShapes.box(2, 0, 3, 14, 16, 13), true, 0, false, MapColor.COLOR_BLACK, SoundType.WOOD),
    StageLight("stage_light", PropShapes.of(PropShapes.box(4, 0, 4, 12, 2, 12), PropShapes.box(4, 2, 5, 12, 10, 11)), true, 11, false, MapColor.COLOR_GRAY, SoundType.METAL),
    SnackCounter("snack_counter", PropShapes.box(0, 0, 0, 16, 16, 16), true, 0, false, MapColor.SNOW, SoundType.WOOL),
    PunchBowl("punch_bowl", PropShapes.of(PropShapes.box(5, 0, 5, 11, 2, 11), PropShapes.box(3, 2, 3, 13, 4, 13), PropShapes.box(2, 4, 2, 14, 7, 14),
            PropShapes.box(1, 7, 1, 15, 8, 15), PropShapes.box(0.75, 0, 0.75, 3.25, 4.5, 3.25)), true, 0, false, MapColor.COLOR_RED, SoundType.GLASS),
    SnackBowl("snack_bowl", PropShapes.box(3, 0, 3, 13, 5, 13), true, 0, false, MapColor.COLOR_YELLOW, SoundType.WOOD),
    CupcakeStand("cupcake_stand", PropShapes.box(3, 0, 3, 13, 11, 13), true, 0, false, MapColor.COLOR_PINK, SoundType.WOOL),
    SoloCups("solo_cups", PropShapes.box(3, 0, 3, 13, 8, 13), true, 0, false, MapColor.COLOR_RED, SoundType.WOOD),
    DiscoBall("disco_ball", PropShapes.box(4, 3, 4, 12, 16, 12), false, 9, true, MapColor.COLOR_LIGHT_GRAY, SoundType.GLASS),
    PartyLights("party_lights", PropShapes.box(0, 9, 12, 16, 16, 16), false, 11, true, MapColor.COLOR_YELLOW, SoundType.GLASS),
    Balloons("balloons", PropShapes.box(2, 2, 2, 14, 16, 14), false, 0, true, MapColor.COLOR_RED, SoundType.WOOL),
    Streamers("streamers", PropShapes.box(1, 2, 1, 15, 16, 15), false, 0, true, MapColor.COLOR_PURPLE, SoundType.WOOL);

    private final String Suffix;
    private final VoxelShape Outline;
    private final boolean Solid;
    private final int Glow;
    private final boolean Hanging;
    private final MapColor Colour;
    private final SoundType Sound;

    Scp465Decor(String Suffix, VoxelShape Outline, boolean Solid, int Glow, boolean Hanging, MapColor Colour, SoundType Sound) {
        this.Suffix = Suffix;
        this.Outline = Outline;
        this.Solid = Solid;
        this.Glow = Glow;
        this.Hanging = Hanging;
        this.Colour = Colour;
        this.Sound = Sound;
    }

    public String id() {
        return "scp465_" + Suffix;
    }

    public String suffix() {
        return Suffix;
    }

    public VoxelShape outline() {
        return Outline;
    }

    public boolean solid() {
        return Solid;
    }

    public int glow() {
        return Glow;
    }

    public boolean hanging() {
        return Hanging;
    }

    public boolean serves() {
        return this == PunchBowl || this == SnackBowl || this == CupcakeStand;
    }

    public boolean onBar() {
        return serves() || this == SoloCups;
    }

    public BlockBehaviour.Properties properties() {
        BlockBehaviour.Properties Made = BlockBehaviour.Properties.of().mapColor(Colour).sound(Sound).strength(-1.0f, 3600000.0f).noLootTable()
                .noOcclusion().forceSolidOn().pushReaction(PushReaction.BLOCK).isValidSpawn((State, Getter, Pos, Type) -> false)
                .isRedstoneConductor((State, Getter, Pos) -> false).isSuffocating((State, Getter, Pos) -> false).isViewBlocking((State, Getter, Pos) -> false);
        if (!Solid) {
            Made = Made.noCollission();
        }
        int Light = Glow;
        if (Light > 0) {
            Made = Made.lightLevel(State -> Light).emissiveRendering((State, Getter, Pos) -> false);
        }
        return Made;
    }
}
