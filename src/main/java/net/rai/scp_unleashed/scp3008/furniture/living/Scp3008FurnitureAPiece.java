package net.rai.scp_unleashed.scp3008.furniture.living;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

public enum Scp3008FurnitureAPiece {
    Sofa("scp3008_living_sofa", "living", Form.Pair, Span.Side, Stuff.Fabric, false, true, 0, Voice.None, Salvage.None,
            new double[][]{{0, 0, 1, 16, 8, 15.5}, {0, 8, 12.5, 16, 15, 15.5}, {0, 8, 1, 4, 11.5, 15.5}},
            new double[][]{{0, 0, 1, 16, 8, 15.5}, {0, 8, 12.5, 16, 15, 15.5}, {12, 8, 1, 16, 11.5, 15.5}}),
    Armchair("scp3008_living_armchair", "living", Form.Plain, Span.None, Stuff.Fabric, false, true, 0, Voice.None, Salvage.None,
            new double[][]{{2, 0, 1.5, 14, 8, 14}, {2, 8, 11, 14, 17, 14.75}, {1.5, 8, 1.5, 4, 11, 13}, {12, 8, 1.5, 14.5, 11, 13}}, null),
    CoffeeTable("scp3008_living_coffee_table", "living", Form.Plain, Span.None, Stuff.Wood, false, false, 0, Voice.None, Salvage.Legs,
            new double[][]{{0.5, 9, 1.5, 15.5, 10.5, 14.5}, {1, 0, 2, 2.5, 9, 3.5}, {13.5, 0, 2, 15, 9, 3.5}, {1, 0, 12.5, 2.5, 9, 14}, {13.5, 0, 12.5, 15, 9, 14},
                    {1.5, 3, 2.5, 14.5, 4, 13.5}}, null),
    SideTable("scp3008_living_side_table", "living", Form.Plain, Span.None, Stuff.Wood, false, false, 0, Voice.None, Salvage.Legs,
            new double[][]{{2.5, 11, 2.5, 13.5, 12.5, 13.5}, {7, 0, 7, 9, 11, 9}, {3, 0, 7, 13, 1, 9}, {7, 0, 3, 9, 1, 13}, {5, 12.5, 7, 9, 15, 11}}, null),
    TvBench("scp3008_living_tv_bench", "living", Form.Cabinet, Span.None, Stuff.Wood, false, false, 9, Voice.Drawer, Salvage.None,
            new double[][]{{0, 0, 3, 16, 8, 15}, {1, 8, 8, 15, 17, 10.5}}, null),
    Bookcase("scp3008_living_bookcase", "living", Form.Tall, Span.Up, Stuff.Wood, false, false, 18, Voice.Books, Salvage.None,
            new double[][]{{0, 0, 1.5, 16, 16, 16}}, new double[][]{{0, 0, 1.5, 16, 16, 16}}),
    CubeShelf("scp3008_living_cube_shelf", "living", Form.Cabinet, Span.None, Stuff.Wood, false, false, 9, Voice.Drawer, Salvage.None,
            new double[][]{{0, 0, 2, 16, 16, 16}}, null),
    Rug("scp3008_living_rug", "living", Form.Plain, Span.None, Stuff.Fabric, false, false, 0, Voice.None, Salvage.None,
            new double[][]{{0.5, 0, 0.25, 15.5, 0.5, 15.75}}, null),
    DiningTable("scp3008_dining_table", "dining", Form.Pair, Span.Side, Stuff.Wood, false, false, 0, Voice.None, Salvage.Legs,
            new double[][]{{0, 14, 0.5, 16, 15.5, 15.5}, {1, 0, 1.5, 3, 14, 3.5}, {1, 0, 12.5, 3, 14, 14.5}},
            new double[][]{{0, 14, 0.5, 16, 15.5, 15.5}, {13, 0, 1.5, 15, 14, 3.5}, {13, 0, 12.5, 15, 14, 14.5}}),
    DiningChair("scp3008_dining_chair", "dining", Form.Plain, Span.None, Stuff.Wood, false, true, 0, Voice.None, Salvage.None,
            new double[][]{{2.5, 0, 2.5, 13.5, 10, 14}, {3, 10, 12, 13, 19, 13.5}}, null),
    Bench("scp3008_dining_bench", "dining", Form.Plain, Span.None, Stuff.Wood, false, true, 0, Voice.None, Salvage.None,
            new double[][]{{0, 8, 4, 16, 10, 12}, {1, 0, 4.5, 3.5, 8, 11.5}, {12.5, 0, 4.5, 15, 8, 11.5}}, null),
    Stool("scp3008_dining_stool", "dining", Form.Plain, Span.None, Stuff.Wood, false, true, 0, Voice.None, Salvage.None,
            new double[][]{{3, 0, 3, 13, 10, 13}}, null),
    Sideboard("scp3008_dining_sideboard", "dining", Form.Cabinet, Span.None, Stuff.Wood, false, false, 18, Voice.Wood, Salvage.None,
            new double[][]{{0.5, 0, 2, 15.5, 15, 15.5}, {0, 15, 1.5, 16, 16, 16}}, null),
    DisplayCabinet("scp3008_dining_display_cabinet", "dining", Form.Tall, Span.Up, Stuff.Wood, false, false, 18, Voice.Glass, Salvage.None,
            new double[][]{{0, 0, 1, 16, 16, 16}}, new double[][]{{0, 0, 1, 16, 16, 16}}),
    BaseCabinet("scp3008_kitchen_base_cabinet", "kitchen", Form.Cabinet, Span.None, Stuff.Wood, false, false, 9, Voice.Wood, Salvage.Doors,
            new double[][]{{0, 0, 1.5, 16, 14.5, 16}, {0, 14.5, 0.5, 16, 16, 16}}, null),
    WallCabinet("scp3008_kitchen_wall_cabinet", "kitchen", Form.Cabinet, Span.None, Stuff.Wood, true, false, 9, Voice.Wood, Salvage.Doors,
            new double[][]{{0, 1, 7.5, 16, 15, 16}}, null),
    Sink("scp3008_kitchen_sink", "kitchen", Form.Cabinet, Span.None, Stuff.Wood, false, false, 9, Voice.Wood, Salvage.Doors,
            new double[][]{{0, 0, 1.5, 16, 14.5, 16}, {0, 14.5, 0.5, 16, 16, 16}}, null),
    Stove("scp3008_kitchen_stove", "kitchen", Form.Stove, Span.None, Stuff.Metal, false, false, 0, Voice.None, Salvage.None,
            new double[][]{{0, 0, 0.5, 16, 15.5, 16}, {0, 15.5, 14.5, 16, 17.5, 16}}, null),
    Fridge("scp3008_kitchen_fridge", "kitchen", Form.Tall, Span.Up, Stuff.Metal, false, false, 27, Voice.Fridge, Salvage.None,
            new double[][]{{0.5, 0, 1, 15.5, 16, 16}}, new double[][]{{0.5, 0, 1, 15.5, 16, 16}}),
    Island("scp3008_kitchen_island", "kitchen", Form.Plain, Span.None, Stuff.Wood, false, false, 0, Voice.None, Salvage.Legs,
            new double[][]{{0, 14, 0, 16, 16, 16}, {0.5, 0, 0.5, 2, 14, 2}, {14, 0, 0.5, 15.5, 14, 2}, {0.5, 0, 14, 2, 14, 15.5}, {14, 0, 14, 15.5, 14, 15.5},
                    {0.5, 3, 2, 15.5, 4, 14}}, null),
    Cart("scp3008_kitchen_cart", "kitchen", Form.Plain, Span.None, Stuff.Metal, false, false, 0, Voice.None, Salvage.None,
            new double[][]{{1.5, 0, 2.5, 14.5, 13, 13.5}}, null),
    WallShelf("scp3008_kitchen_wall_shelf", "kitchen", Form.Plain, Span.None, Stuff.Wood, true, false, 0, Voice.None, Salvage.None,
            new double[][]{{0, 12, 10, 16, 13, 16}, {0.5, 6.5, 14, 15.5, 8, 16}, {1.5, 13, 11.5, 8.5, 16.5, 13.5}}, null);

    public enum Form {
        Plain, Pair, Cabinet, Tall, Stove
    }

    public enum Span {
        None, Up, Side
    }

    public enum Stuff {
        Wood, Fabric, Metal
    }

    public enum Voice {
        None, Wood, Drawer, Glass, Fridge, Books
    }

    public enum Salvage {
        None, Legs, Doors
    }

    public final String Id;
    public final String Department;
    public final Form Kind;
    public final Span Reach;
    public final Stuff Finish;
    public final boolean Wall;
    public final boolean Seat;
    public final int Slots;
    public final Voice Sound;
    public final Salvage Scrap;
    private final Map<Direction, VoxelShape> MainShapes;
    private final Map<Direction, VoxelShape> ExtraShapes;

    Scp3008FurnitureAPiece(String Id, String Department, Form Kind, Span Reach, Stuff Finish, boolean Wall, boolean Seat, int Slots, Voice Sound, Salvage Scrap,
                           double[][] Main, double[][] Extra) {
        this.Id = Id;
        this.Department = Department;
        this.Kind = Kind;
        this.Reach = Reach;
        this.Finish = Finish;
        this.Wall = Wall;
        this.Seat = Seat;
        this.Slots = Slots;
        this.Sound = Sound;
        this.Scrap = Scrap;
        this.MainShapes = Scp3008FurnitureAShapes.table(Main);
        this.ExtraShapes = Extra == null ? MainShapes : Scp3008FurnitureAShapes.table(Extra);
    }

    public boolean paired() {
        return Reach != Span.None;
    }

    public boolean storage() {
        return Slots > 0;
    }

    public VoxelShape shape(BlockState State) {
        Direction Facing = State.getValue(HorizontalDirectionalBlock.FACING);
        boolean Extra = State.hasProperty(Scp3008FurnitureAPairs.Part) && State.getValue(Scp3008FurnitureAPairs.Part) == Scp3008FurnitureAPart.Extra;
        return (Extra ? ExtraShapes : MainShapes).get(Facing);
    }
}
