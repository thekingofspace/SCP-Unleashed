package net.rai.scp_unleashed.dclass.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.control.ControlRoster;
import net.rai.scp_unleashed.dclass.DClassAppearance;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.dclass.DClassScores;
import net.rai.scp_unleashed.dclass.PersonnelRecord;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PersonnelFileScreen extends Screen {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/dclass/personnel_file.png");
    public static final int FolderWidth = 216;
    public static final int FolderHeight = 236;
    public static final int HeaderX = 18;
    public static final int HeaderY = 20;
    public static final int FieldX = 18;
    public static final int FieldY = 40;
    public static final int FieldStep = 22;
    public static final int FieldRight = 138;
    public static final int ScoresY = 130;
    public static final int ValueOffset = 10;
    public static final int ValueIndent = 4;
    public static final int WriteRight = 198;
    public static final int PhotoX = 150;
    public static final int PhotoY = 42;
    public static final int PhotoWidth = 48;
    public static final int PhotoHeight = 58;
    public static final int FaceScale = 5;
    public static final int NotesX = 18;
    public static final int NotesY = 146;
    public static final int NotesPadding = 2;
    public static final int NotesFloor = 213;
    public static final int RuleDrop = 8;
    public static final int MinRules = 4;
    public static final int RuleLeft = 16;
    public static final int RuleRight = 200;
    public static final int RuleColour = 0xC8544C42;
    public static final String Ellipsis = "...";
    public static final int StampU = 0;
    public static final int StampV = 240;
    public static final int StampWidth = 66;
    public static final int StampHeight = 16;
    public static final int StampX = 140;
    public static final int StampY = 97;
    public static final int Ink = 0xFF261F1A;
    public static final int InkSoft = 0xFF5A4C3E;
    public static final int StatusGood = 0xFF2E6A3A;
    public static final int StatusBad = 0xFF8A1E1E;
    public static final int StatusLinked = 0xFF1E4E8A;
    private static final String Prefix = "gui.scp_unleashed.personnel_file.";

    private final InteractionHand Hand;
    private final ControlRoster.BodyStatus Status;
    private final String Controller;
    private int Left;
    private int Top;

    public PersonnelFileScreen(InteractionHand Hand, byte Status, String Controller) {
        super(Component.translatable(Prefix + "title"));
        this.Hand = Hand;
        ControlRoster.BodyStatus[] All = ControlRoster.BodyStatus.values();
        this.Status = Status >= 0 && Status < All.length ? All[Status] : ControlRoster.BodyStatus.Unknown;
        this.Controller = Controller;
    }

    @Override
    protected void init() {
        super.init();
        Left = (width - FolderWidth) / 2;
        Top = (height - FolderHeight) / 2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Nullable
    private PersonnelRecord record() {
        if (minecraft == null || minecraft.player == null) {
            return null;
        }
        ItemStack Held = minecraft.player.getItemInHand(Hand);
        return Held.get(DClassRegistry.RecordComponent.get());
    }

    @Override
    public void tick() {
        super.tick();
        if (record() == null) {
            onClose();
        }
    }

    @Override
    public void renderBackground(GuiGraphics Graphics, int MouseX, int MouseY, float Partial) {
        super.renderBackground(Graphics, MouseX, MouseY, Partial);
        Graphics.blit(Texture, Left, Top, 0, 0, FolderWidth, FolderHeight);
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float Partial) {
        super.render(Graphics, MouseX, MouseY, Partial);
        PersonnelRecord Record = record();
        if (Record == null) {
            return;
        }
        Graphics.drawString(font, fit(Component.translatable(Prefix + "header").getString(), WriteRight - HeaderX), Left + HeaderX, Top + HeaderY, InkSoft, false);
        field(Graphics, 0, Component.translatable(Prefix + "designation"), Component.literal(Record.designation()), Ink, FieldRight);
        field(Graphics, 1, Component.translatable(Prefix + "disposition"), Record.trait().getName(), Ink, FieldRight);
        field(Graphics, 2, Component.translatable(Prefix + "conviction"), Record.crime().getName(), Ink, FieldRight);
        field(Graphics, 3, Component.translatable(Prefix + "status"), statusText(), statusColour(), WriteRight);
        Component Scores = Component.translatable(Prefix + "scores", Record.Iq(), Record.Athletic(), DClassScores.band(Record.Athletic()));
        Graphics.drawString(font, fit(Scores.getString(), WriteRight - FieldX), Left + FieldX, Top + ScoresY, Ink, false);
        portrait(Graphics, Record.Appearance());
        Graphics.blit(Texture, Left + StampX, Top + StampY, StampU, StampV, StampWidth, StampHeight);
        notes(Graphics, Component.translatable(Prefix + "notes", Record.trait().getDescription(), Record.crime().getDescription()));
    }

    private void notes(GuiGraphics Graphics, Component Text) {
        int Width = WriteRight - NotesX;
        int Step = font.lineHeight + NotesPadding;
        int Capacity = (NotesFloor - NotesY - RuleDrop) / Step + 1;
        List<FormattedText> Lines = font.getSplitter().splitLines(Text, Width, Style.EMPTY);
        int Rows = Math.min(Lines.size(), Capacity);
        int Rules = Math.min(Capacity, Math.max(MinRules, Rows));
        for (int Index = 0; Index < Rules; Index++) {
            rule(Graphics, NotesY + Index * Step + RuleDrop);
        }
        for (int Index = 0; Index < Rows; Index++) {
            String Line = Lines.get(Index).getString();
            if (Index == Rows - 1 && Lines.size() > Rows) {
                Line = font.plainSubstrByWidth(Line, Width - font.width(Ellipsis)).stripTrailing() + Ellipsis;
            }
            Graphics.drawString(font, Line, Left + NotesX, Top + NotesY + Index * Step, Ink, false);
        }
    }

    private void rule(GuiGraphics Graphics, int Y) {
        int Start = RuleLeft;
        for (int X = RuleLeft; X <= RuleRight; X++) {
            if (X == RuleRight || (X * 13 + Y) % 17 == 0) {
                if (X > Start) {
                    Graphics.fill(Left + Start, Top + Y, Left + X, Top + Y + 1, RuleColour);
                }
                Start = X + 1;
            }
        }
    }

    private String fit(String Text, int Width) {
        if (font.width(Text) <= Width) {
            return Text;
        }
        return font.plainSubstrByWidth(Text, Width - font.width(Ellipsis)).stripTrailing() + Ellipsis;
    }

    private void field(GuiGraphics Graphics, int Row, Component Label, Component Value, int Colour, int Right) {
        int Y = Top + FieldY + Row * FieldStep;
        Graphics.drawString(font, fit(Label.getString(), FieldRight - FieldX), Left + FieldX, Y, InkSoft, false);
        Graphics.drawString(font, fit(Value.getString(), Right - FieldX - ValueIndent), Left + FieldX + ValueIndent, Y + ValueOffset, Colour, false);
    }

    private Component statusText() {
        return switch (Status) {
            case Free -> Component.translatable(Prefix + "status.active");
            case Controlled -> Component.translatable(Prefix + "status.controlled", Controller);
            case Deceased -> Component.translatable(Prefix + "status.deceased");
            default -> Component.translatable(Prefix + "status.unknown");
        };
    }

    private int statusColour() {
        return switch (Status) {
            case Free -> StatusGood;
            case Controlled -> StatusLinked;
            case Deceased -> StatusBad;
            default -> InkSoft;
        };
    }

    private void portrait(GuiGraphics Graphics, int Appearance) {
        ResourceLocation Skin = DClassSkins.textureFor(Appearance);
        int Face = 8 * FaceScale;
        int X = Left + PhotoX + (PhotoWidth - Face) / 2;
        int Y = Top + PhotoY + 6;
        int Width = DClassSkins.Width;
        int Height = DClassSkins.Height;
        Graphics.enableScissor(Left + PhotoX, Top + PhotoY, Left + PhotoX + PhotoWidth, Top + PhotoY + PhotoHeight);
        Graphics.blit(Skin, X, Y + Face, Face, Face, 20.0f, 20.0f, 8, 8, Width, Height);
        Graphics.blit(Skin, X - Face / 2, Y + Face, Face / 2, Face, 44.0f, 20.0f, 4, 8, Width, Height);
        Graphics.blit(Skin, X + Face, Y + Face, Face / 2, Face, 36.0f, 52.0f, 4, 8, Width, Height);
        Graphics.blit(Skin, X, Y, Face, Face, 8.0f, 8.0f, 8, 8, Width, Height);
        Graphics.blit(Skin, X, Y, Face, Face, 40.0f, 8.0f, 8, 8, Width, Height);
        int Iris = 0xFF000000 | DClassSkins.IrisColours[DClassAppearance.eyeColour(Appearance)];
        int Pupil = FaceScale - 1;
        int EyeTop = Y + 4 * FaceScale - Pupil / 2;
        Graphics.fill(X + 2 * FaceScale - Pupil / 2, EyeTop, X + 2 * FaceScale - Pupil / 2 + Pupil, EyeTop + Pupil, Iris);
        Graphics.fill(X + 6 * FaceScale - Pupil / 2, EyeTop, X + 6 * FaceScale - Pupil / 2 + Pupil, EyeTop + Pupil, Iris);
        Graphics.disableScissor();
    }
}
