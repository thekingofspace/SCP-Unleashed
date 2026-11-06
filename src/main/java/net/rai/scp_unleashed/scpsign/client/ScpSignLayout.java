package net.rai.scp_unleashed.scpsign.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record ScpSignLayout(@Nullable AnomalyDocument Source, List<Quad> SheetQuads, @Nullable ResourceLocation Photo, @Nullable Quad PhotoQuad, List<Line> Lines) {
    public record Quad(float X0, float Y0, float X1, float Y1, float U0, float V0, float U1, float V1, int Colour, int Layer) {
    }

    public record Line(FormattedCharSequence Text, float X, float Y, float Scale) {
    }
}
