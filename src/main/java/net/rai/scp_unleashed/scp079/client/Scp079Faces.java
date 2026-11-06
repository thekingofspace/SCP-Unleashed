package net.rai.scp_unleashed.scp079.client;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp079.Scp079Mood;

public final class Scp079Faces {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/block/scp079/screen.png");
    public static final int TextureWidth = 64;
    public static final int TextureHeight = 128;
    public static final int Cell = 16;
    public static final int FaceWidth = 12;
    public static final int FaceHeight = 9;
    public static final int LookLeft = 0;
    public static final int LookCentre = 1;
    public static final int LookRight = 2;

    private Scp079Faces() {
    }

    public static int frame(Scp079Mood Mood, int Ticks, int Seed, int Look) {
        int Time = Ticks + Seed;
        return switch (Mood) {
            case Watching -> Math.floorMod(Time, 90) < 3 ? 3 : Look;
            case Idle -> {
                if (Math.floorMod(Time, 73) < 3) {
                    yield 2;
                }
                if (Math.floorMod(Time * 7 + 11, 151) < 2) {
                    yield 3;
                }
                yield (Time / 14) % 2;
            }
            case Annoyed -> Math.floorMod(Time, 47) < 4 ? 2 + (Time / 2) % 2 : (Time / 10) % 2;
            case Smug -> Math.floorMod(Time, 61) < 6 ? 2 + (Time / 3) % 2 : (Time / 12) % 2;
            case Laughing -> {
                int Step = (Time / 4) % 4;
                yield Step == 3 ? 1 : Step;
            }
            case Glitching -> Math.floorMod((Time / 2) * 7 + Seed, 4);
            case Pattern -> (Time / 5) % 4;
            default -> 0;
        };
    }

    public static float u0(int Frame) {
        return Frame * Cell / (float) TextureWidth;
    }

    public static float u1(int Frame) {
        return (Frame * Cell + FaceWidth) / (float) TextureWidth;
    }

    public static float v0(int Row) {
        return Row * Cell / (float) TextureHeight;
    }

    public static float v1(int Row) {
        return (Row * Cell + FaceHeight) / (float) TextureHeight;
    }
}
