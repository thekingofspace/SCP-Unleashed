package net.rai.scp_unleashed.scp3008.furniture.electronics;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.rai.scp_unleashed.scp3008.plan.Scp3008Painter;
import net.rai.scp_unleashed.scp3008.world.Scp3008Showrooms;

public final class Scp3008ElectronicsShowroom {
    private Scp3008ElectronicsShowroom() {
    }

    private static boolean fit(Scp3008Showrooms.Room Made, Block Piece, int D, int W, int H, Direction Facing, int SpanD, int SpanW) {
        if (Piece == null) {
            return false;
        }
        for (int[] Corner : new int[][]{{0, 0}, {0, SpanW - 1}, {SpanD - 1, 0}, {SpanD - 1, SpanW - 1}}) {
            if (Made.put(Piece, D + Corner[0], W + Corner[1], H, Facing)) {
                return true;
            }
        }
        return false;
    }

    private static boolean wallFit(Scp3008Showrooms.Room Made, Block Piece, int D, int W, int H, Direction Facing, int SpanD, int SpanW) {
        if (Piece == null) {
            return false;
        }
        for (int[] Corner : new int[][]{{0, 0}, {0, SpanW - 1}, {SpanD - 1, 0}, {SpanD - 1, SpanW - 1}}) {
            if (Made.wallBehind(D + Corner[0], W + Corner[1], Facing) && Made.put(Piece, D + Corner[0], W + Corner[1], H, Facing)) {
                return true;
            }
        }
        return false;
    }

    public static void furnish(Scp3008Showrooms.Room Made) {
        int Width = Made.Width;
        int Middle = Made.Middle;
        int Right = Width - 1;
        Direction Front = Made.Front;
        wallFit(Made, Made.pick(1, "tv_wall"), 13, Middle - 1, 1, Front, 1, 3);
        Made.put(Made.pick(2, "soundbar_set"), 13, Middle, 0, Front);
        Block Speaker = Made.pick(3, "floor_speaker");
        Made.put(Speaker, 12, Middle - 2, 0, Front);
        Made.put(Speaker, 12, Middle + 2, 0, Front);
        fit(Made, Made.pick(4, "tv_bench"), 13, 1, 0, Front, 1, 2);
        if (fit(Made, Made.pick(5, "computer_desk"), 13, Right - 2, 0, Front, 1, 2)) {
            int[] Span = Made.last();
            Made.put(Made.pick(6, "gaming_chair"), 11, Span[2] + (Span[3] - Span[2]) / 2, 0, Made.Back);
        }
        wallFit(Made, Made.pick(7, "headphone_wall"), 9, 0, 1, Made.Right, 2, 1);
        wallFit(Made, Made.pick(8, "cable_wall"), 6, 0, 1, Made.Right, 2, 1);
        wallFit(Made, Made.pick(9, "router_shelf"), 12, 0, 1, Made.Right, 1, 1);
        Block Boxes = Made.pick(10, "boxed_stack");
        Made.put(Boxes, 9, 0, 0, Made.Right);
        Made.put(Boxes, 10, 0, 0, Made.Right);
        Made.onTop(Made.pick(11, "radio"), 9, 0, Made.Right);
        wallFit(Made, Made.pick(12, "wall_tv"), 10, Right, 1, Made.Left, 2, 1);
        Made.put(Made.pick(13, "record_player"), 10, Right, 0, Made.Left);
        Made.put(Made.pick(14, "smart_speaker"), 11, Right, 0, Made.Left);
        Made.put(Made.pick(15, "gondola_end"), 8, Right, 0, Made.Left);
        Made.put(Made.pick(16, "charging_station"), 6, Right, 0, Made.Left);
        fit(Made, Made.pick(17, "phone_table"), 7, Middle - 1, 0, Front, 1, 2);
        Block Laptop = Made.pick(18, "laptop_stand");
        Made.put(Laptop, 7, Middle - 3, 0, Front);
        Made.put(Laptop, 7, Middle + 2, 0, Front);
        Made.put(Made.pick(19, "console_kiosk"), 4, Right - 2, 0, Front);
        Made.put(Made.pick(20, "smart_speaker"), 4, 2, 0, Front);
        Made.put(Made.pick(21, "gondola_end"), 2, Right, 0, Front);
        Made.put(Boxes, 2, 1, 0, Front);
        if (Made.put(Boxes, 3, 1, 0, Front)) {
            Made.onTop(Made.pick(22, "charger_lamp"), 3, 1, Front);
        }
        Made.put(Made.pick(23, "pendant_lamp"), Scp3008Painter.CrossDepth, Middle, 3, Front);
    }
}
