package net.rai.scp_unleashed.speaker.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.block.NetworkObjectBlock;
import net.rai.scp_unleashed.network.SetPortPayload;
import net.rai.scp_unleashed.registry.ModSounds;
import net.rai.scp_unleashed.speaker.SoundCatalog;
import net.rai.scp_unleashed.speaker.SoundConsoleBlock;
import net.rai.scp_unleashed.speaker.SoundConsoleBlockEntity;
import net.rai.scp_unleashed.speaker.SoundConsolePayload;
import net.rai.scp_unleashed.speaker.SoundMessages;
import net.rai.scp_unleashed.speaker.SpeakerRegistry;
import net.rai.scp_unleashed.speaker.cassette.AnomalousLibrary;
import net.rai.scp_unleashed.speaker.cassette.CassetteRecording;
import net.rai.scp_unleashed.wire.WireChannels;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SoundConsoleScreen extends Screen {
    private static final ResourceLocation PanelTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/speaker_console.png");
    private static final ResourceLocation PartsTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/speaker_console_parts.png");
    private static final int PanelWidth = 236;
    private static final int PanelHeight = 232;
    private static final int TitleY = 6;
    private static final int[] TabX = {10, 83, 156};
    private static final int TabY = 24;
    private static final int TabWidth = 70;
    private static final int TabHeight = 14;
    private static final int TabOffU = 0;
    private static final int TabHoverU = 70;
    private static final int TabOnU = 140;
    private static final int TabV = 0;
    private static final int TabAlertU = 0;
    private static final int TabAlertV = 14;
    private static final int TabTextY = 27;
    private static final int ListX = 10;
    private static final int ListY = 44;
    private static final int ListWidth = 202;
    private static final int RowHeight = 10;
    private static final int VisibleRows = 7;
    private static final int RowSelectedV = 30;
    private static final int RowHoverV = 40;
    private static final int RowTextX = 21;
    private static final int RowTextDrop = 1;
    private static final int WarnX = 12;
    private static final int WarnDrop = 1;
    private static final int WarnU = 94;
    private static final int WarnV = 14;
    private static final int WarnSize = 7;
    private static final int TrackX = 218;
    private static final int TrackY = 44;
    private static final int TrackWidth = 8;
    private static final int TrackHeight = 70;
    private static final int ThumbHeight = 15;
    private static final int ThumbU = 104;
    private static final int ThumbHoverU = 112;
    private static final int ThumbOffU = 120;
    private static final int ThumbV = 14;
    private static final int LabelY = 124;
    private static final int[] Places = {1000, 100, 10, 1};
    private static final int[] DigitX = {14, 29, 44, 59};
    private static final int DigitY = 139;
    private static final int DigitWidth = 12;
    private static final int DigitHeight = 18;
    private static final int DigitV = 50;
    private static final int GhostU = 120;
    private static final int ChevronOffset = -1;
    private static final int ChevronY = 166;
    private static final int ChevronWidth = 13;
    private static final int ChevronHeight = 10;
    private static final int ChevronV = 68;
    private static final int ChevronHoverU = 13;
    private static final int ChevronPressedU = 26;
    private static final int PortCentreX = 42;
    private static final int StepperY = 134;
    private static final int StepperWidth = 12;
    private static final int StepperHeight = 14;
    private static final int StepperV = 68;
    private static final int MinusU = 39;
    private static final int PlusU = 75;
    private static final int StateStride = 12;
    private static final int VolumeMinusX = 81;
    private static final int VolumePlusX = 141;
    private static final int PitchMinusX = 156;
    private static final int PitchPlusX = 216;
    private static final int VolumeCentreX = 117;
    private static final int PitchCentreX = 192;
    private static final int ValueTextY = 137;
    private static final int ButtonY = 158;
    private static final int ButtonWidth = 72;
    private static final int ButtonHeight = 18;
    private static final int TestX = 81;
    private static final int SendX = 156;
    private static final int TestV = 82;
    private static final int SendV = 100;
    private static final int SendOffV = 118;
    private static final int ButtonHoverU = 72;
    private static final int ButtonPressedU = 144;
    private static final int ButtonTextDrop = 5;
    private static final int LampSize = 6;
    private static final int LampV = 14;
    private static final int PowerOffU = 70;
    private static final int PowerOnU = 76;
    private static final int TxOffU = 82;
    private static final int TxOnU = 88;
    private static final int LampY = 213;
    private static final int StatusTextY = 212;
    private static final int LoopRowY = 182;
    private static final int LatchX = 81;
    private static final int StopX = 156;
    private static final int IconShift = 4;
    private static final int LatchOffU = 0;
    private static final int LatchHoverU = 72;
    private static final int LatchOnU = 144;
    private static final int LatchV = 136;
    private static final int LatchOnHoverU = 0;
    private static final int LatchOnHoverV = 154;
    private static final int StopU = 72;
    private static final int StopHoverU = 144;
    private static final int StopV = 154;
    private static final int StopPressedU = 0;
    private static final int StopDisabledU = 72;
    private static final int StopLowerV = 172;
    private static final int LoopingOffU = 144;
    private static final int LoopingOnU = 150;
    private static final int LoopingV = 172;
    private static final int LoopingPlateX = 8;
    private static final int LoopingPlateWidth = 69;
    private static final int LoopingLampX = 14;
    private static final int LoopingLampY = 188;
    private static final int LoopingTextX = 23;
    private static final int LoopingTextY = 187;
    private static final int LatchColour = 0xFFE6E6E6;
    private static final int LatchOnColour = 0xFFFFE2A8;
    private static final int StopColour = 0xFFE6E6E6;
    private static final int LoopingColour = 0xFF7D8590;
    private static final int LoopingOnColour = 0xFFFFC24A;
    private static final int PowerLampX = 14;
    private static final int PowerTextX = 23;
    private static final int TxLampX = 80;
    private static final int TxTextX = 89;
    private static final int StatusRight = 224;
    private static final int TitleColour = 0xFFE0E0E0;
    private static final int LabelColour = 0xFFA7B0BA;
    private static final int StatusColour = 0xFFC8C8C8;
    private static final int TabColour = 0xFFC8C8C8;
    private static final int TabOnColour = 0xFFFFFFFF;
    private static final int TabAlertColour = 0xFFFF8E80;
    private static final int RowColour = 0xFFAFC4D6;
    private static final int RowOnColour = 0xFFFFFFFF;
    private static final int RowAlertColour = 0xFFE38176;
    private static final int RowAlertOnColour = 0xFFFFD2CB;
    private static final int ValueColour = 0xFF9AD6FF;
    private static final int TestColour = 0xFFE6E6E6;
    private static final int SendColour = 0xFFFFF4EE;
    private static final int SendOffColour = 0xFF8A8A8A;
    private static final float[] VolumeSteps = {0.1f, 0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f, 4.0f};
    private static final float PitchStep = 0.05f;
    private static final float PitchBigStep = 0.25f;
    private static final float TestVolume = 1.0f;
    private static final double MaxDistanceSqr = 64.0;
    private static final int HintWidth = 170;
    private static final int EmptyIconY = 5;
    private static final int EmptyTitleY = 25;
    private static final int EmptyHintY = 38;
    private static final int EmptyLineHeight = 10;
    private static final int EmptyWrapWidth = 180;
    private static final int EmptyTitleColour = 0xFFAFC4D6;
    private static final int EmptyHintColour = 0xFF7D91A4;

    private final BlockPos Pos;
    private final ItemStack BlankTape = new ItemStack(SpeakerRegistry.CassetteTape.get());
    private int Port;
    private ResourceLocation Sound;
    private float Volume;
    private float Pitch;
    private boolean Loop;
    private SoundCatalog.Category Shown;
    private List<Choice> Rows;
    private int LibraryVersion;
    private int Scroll;
    private boolean Dragging;
    @Nullable
    private Part Pressed;
    private int PressedIndex = -1;
    @Nullable
    private SoundInstance Preview;
    private int Left;
    private int Top;

    public SoundConsoleScreen(BlockPos Pos, Component Title, SoundConsoleBlockEntity Console) {
        super(Title);
        this.Pos = Pos;
        this.Port = Console.getSlot(0);
        this.Sound = Console.getSound();
        this.Volume = Console.getVolume();
        this.Pitch = Console.getPitch();
        this.Loop = Console.isLoop();
        SoundCatalog.Entry Current = SoundCatalog.find(Sound);
        this.Shown = Current != null && !Current.isAnomalous() ? Current.Group() : SoundCatalog.Category.Anomalous;
        this.LibraryVersion = AnomalousLibrary.clientVersion();
        this.Rows = rowsFor(Shown);
        revealSelection();
    }

    private static List<Choice> rowsFor(SoundCatalog.Category Group) {
        List<Choice> Found = new ArrayList<>();
        if (Group == SoundCatalog.Category.Anomalous) {
            for (CassetteRecording Recording : AnomalousLibrary.client()) {
                Found.add(new Choice(Recording.Sound(), Recording.Title(), true));
            }
            return Found;
        }
        for (SoundCatalog.Entry Entry : SoundCatalog.inCategory(Group)) {
            Found.add(new Choice(Entry.Id(), Component.translatable(Entry.getTranslationKey()), Entry.isAnomalous()));
        }
        return Found;
    }

    @Override
    protected void init() {
        super.init();
        Left = (width - PanelWidth) / 2;
        Top = (height - PanelHeight) / 2;
    }

    @Override
    public void tick() {
        super.tick();
        if (minecraft == null || minecraft.player == null || minecraft.level == null
                || !(minecraft.level.getBlockEntity(Pos) instanceof SoundConsoleBlockEntity)
                || minecraft.player.distanceToSqr(Vec3.atCenterOf(Pos)) > MaxDistanceSqr) {
            onClose();
            return;
        }
        if (LibraryVersion != AnomalousLibrary.clientVersion()) {
            LibraryVersion = AnomalousLibrary.clientVersion();
            if (Shown == SoundCatalog.Category.Anomalous) {
                Rows = rowsFor(Shown);
                Scroll = Mth.clamp(Scroll, 0, maxScroll());
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        super.render(Graphics, MouseX, MouseY, PartialTick);
        if (minecraft == null || minecraft.level == null) {
            return;
        }
        BlockState State = minecraft.level.getBlockState(Pos);
        boolean Powered = State.hasProperty(NetworkObjectBlock.Active) && State.getValue(NetworkObjectBlock.Active);
        boolean Sending = State.hasProperty(SoundConsoleBlock.Sending) && State.getValue(SoundConsoleBlock.Sending);
        boolean Driving = minecraft.level.getBlockEntity(Pos) instanceof SoundConsoleBlockEntity Console && Console.isDriving();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Graphics.blit(PanelTexture, Left, Top, 0, 0, PanelWidth, PanelHeight);
        renderTabs(Graphics, MouseX, MouseY);
        renderList(Graphics, MouseX, MouseY);
        renderPort(Graphics, MouseX, MouseY);
        renderSteppers(Graphics, MouseX, MouseY);
        renderButtons(Graphics, MouseX, MouseY, Powered);
        renderLoopRow(Graphics, MouseX, MouseY, Powered, Driving);
        Graphics.blit(PartsTexture, Left + PowerLampX, Top + LampY, Powered ? PowerOnU : PowerOffU, LampV, LampSize, LampSize);
        Graphics.blit(PartsTexture, Left + TxLampX, Top + LampY, Sending ? TxOnU : TxOffU, LampV, LampSize, LampSize);
        RenderSystem.disableBlend();
        Graphics.drawString(font, title, Left + (PanelWidth - font.width(title)) / 2, Top + TitleY, TitleColour, false);
        Graphics.drawString(font, Component.translatable("gui.scp_unleashed.port_panel.power"), Left + PowerTextX, Top + StatusTextY, StatusColour, false);
        SoundCatalog.Entry Current = SoundCatalog.find(Sound);
        boolean Anomalous = Current == null || Current.isAnomalous();
        String Clipped = font.plainSubstrByWidth(selectedName(Current).getString(), StatusRight - TxTextX);
        Graphics.drawString(font, Clipped, Left + TxTextX, Top + StatusTextY, Anomalous ? RowAlertColour : StatusColour, false);
        renderTooltips(Graphics, MouseX, MouseY, Powered);
    }

    private Component selectedName(@Nullable SoundCatalog.Entry Current) {
        if (Current != null && !Current.isAnomalous()) {
            return Component.translatable(Current.getTranslationKey());
        }
        for (CassetteRecording Recording : AnomalousLibrary.client()) {
            if (Recording.Sound().equals(Sound)) {
                return Recording.Title();
            }
        }
        if (minecraft != null && minecraft.level != null && minecraft.level.getBlockEntity(Pos) instanceof SoundConsoleBlockEntity Console
                && Console.getSound().equals(Sound)) {
            return Console.getSoundName();
        }
        return Component.literal(Sound.toString());
    }

    private void renderTabs(GuiGraphics Graphics, int MouseX, int MouseY) {
        SoundCatalog.Category[] Groups = SoundCatalog.Category.values();
        for (int Index = 0; Index < Groups.length && Index < TabX.length; Index++) {
            SoundCatalog.Category Group = Groups[Index];
            boolean On = Group == Shown;
            boolean Alert = Group == SoundCatalog.Category.Anomalous;
            int X = Left + TabX[Index];
            int Y = Top + TabY;
            if (On && Alert) {
                Graphics.blit(PartsTexture, X, Y, TabAlertU, TabAlertV, TabWidth, TabHeight);
            } else {
                Graphics.blit(PartsTexture, X, Y, On ? TabOnU : inside(MouseX, MouseY, X, Y, TabWidth, TabHeight) ? TabHoverU : TabOffU, TabV, TabWidth, TabHeight);
            }
            Component Label = Component.translatable(Group.getTranslationKey());
            int LabelWidth = font.width(Label) + (Alert ? WarnSize + 2 : 0);
            int LabelX = X + (TabWidth - LabelWidth) / 2;
            int LabelY = Top + TabTextY + (On ? 1 : 0);
            if (Alert) {
                Graphics.blit(PartsTexture, LabelX, LabelY, WarnU, WarnV, WarnSize, WarnSize);
                LabelX += WarnSize + 2;
            }
            Graphics.drawString(font, Label, LabelX, LabelY, On ? (Alert ? TabAlertColour : TabOnColour) : TabColour, false);
        }
    }

    private void renderList(GuiGraphics Graphics, int MouseX, int MouseY) {
        if (Rows.isEmpty()) {
            renderEmpty(Graphics);
        }
        int Hovered = rowAt(MouseX, MouseY);
        for (int Line = 0; Line < VisibleRows; Line++) {
            int Index = Scroll + Line;
            if (Index >= Rows.size()) {
                break;
            }
            Choice Row = Rows.get(Index);
            boolean On = Row.Id().equals(Sound);
            int Y = Top + ListY + Line * RowHeight;
            if (On) {
                Graphics.blit(PartsTexture, Left + ListX, Y, 0, RowSelectedV, ListWidth, RowHeight);
            } else if (Index == Hovered) {
                Graphics.blit(PartsTexture, Left + ListX, Y, 0, RowHoverV, ListWidth, RowHeight);
            }
            if (Row.Anomalous()) {
                Graphics.blit(PartsTexture, Left + WarnX, Y + WarnDrop, WarnU, WarnV, WarnSize, WarnSize);
            }
            int Colour = Row.Anomalous() ? (On ? RowAlertOnColour : RowAlertColour) : (On ? RowOnColour : RowColour);
            String Name = font.plainSubstrByWidth(Row.Name().getString(), ListX + ListWidth - RowTextX - 2);
            Graphics.drawString(font, Name, Left + RowTextX, Y + RowTextDrop, Colour, false);
        }
        int Thumb = thumbTop();
        boolean Scrollable = maxScroll() > 0;
        boolean ThumbHovered = Dragging || inside(MouseX, MouseY, Left + TrackX, Top + Thumb, TrackWidth, ThumbHeight);
        Graphics.blit(PartsTexture, Left + TrackX, Top + Thumb, !Scrollable ? ThumbOffU : ThumbHovered ? ThumbHoverU : ThumbU, ThumbV, TrackWidth, ThumbHeight);
    }

    private void renderEmpty(GuiGraphics Graphics) {
        int CentreX = ListX + ListWidth / 2;
        Graphics.renderItem(BlankTape, Left + CentreX - 8, Top + ListY + EmptyIconY);
        centred(Graphics, Component.translatable("gui.scp_unleashed.sound_console.library_empty"), CentreX, ListY + EmptyTitleY, EmptyTitleColour);
        List<FormattedCharSequence> Lines = font.split(Component.translatable("gui.scp_unleashed.sound_console.library_hint"), EmptyWrapWidth);
        for (int Index = 0; Index < Lines.size(); Index++) {
            FormattedCharSequence Line = Lines.get(Index);
            Graphics.drawString(font, Line, Left + CentreX - font.width(Line) / 2, Top + ListY + EmptyHintY + Index * EmptyLineHeight, EmptyHintColour, false);
        }
    }

    private void renderPort(GuiGraphics Graphics, int MouseX, int MouseY) {
        int Hovered = chevronAt(MouseX, MouseY);
        for (int Index = 0; Index < Places.length; Index++) {
            int X = Left + DigitX[Index];
            Graphics.blit(PartsTexture, X, Top + DigitY, GhostU, DigitV, DigitWidth, DigitHeight);
            Graphics.blit(PartsTexture, X, Top + DigitY, digit(Index) * DigitWidth, DigitV, DigitWidth, DigitHeight);
            int U = Pressed == Part.Chevron && PressedIndex == Index ? ChevronPressedU : Hovered == Index ? ChevronHoverU : 0;
            Graphics.blit(PartsTexture, X + ChevronOffset, Top + ChevronY, U, ChevronV, ChevronWidth, ChevronHeight);
        }
        Component Label = Component.translatable("gui.scp_unleashed.port_panel.port");
        Graphics.drawString(font, Label, Left + PortCentreX - font.width(Label) / 2, Top + LabelY, LabelColour, false);
    }

    private void renderSteppers(GuiGraphics Graphics, int MouseX, int MouseY) {
        stepper(Graphics, MouseX, MouseY, Part.VolumeDown, VolumeMinusX, MinusU);
        stepper(Graphics, MouseX, MouseY, Part.VolumeUp, VolumePlusX, PlusU);
        stepper(Graphics, MouseX, MouseY, Part.PitchDown, PitchMinusX, MinusU);
        stepper(Graphics, MouseX, MouseY, Part.PitchUp, PitchPlusX, PlusU);
        centred(Graphics, Component.translatable("gui.scp_unleashed.sound_console.volume"), VolumeCentreX, LabelY, LabelColour);
        centred(Graphics, Component.translatable("gui.scp_unleashed.sound_console.pitch"), PitchCentreX, LabelY, LabelColour);
        centred(Graphics, Component.literal(Math.round(Volume * 100.0f) + "%"), VolumeCentreX, ValueTextY, ValueColour);
        centred(Graphics, Component.literal(String.format(Locale.ROOT, "%.2f", Pitch)), PitchCentreX, ValueTextY, ValueColour);
    }

    private void stepper(GuiGraphics Graphics, int MouseX, int MouseY, Part Which, int X, int BaseU) {
        boolean Down = Pressed == Which;
        boolean Over = inside(MouseX, MouseY, Left + X, Top + StepperY, StepperWidth, StepperHeight);
        Graphics.blit(PartsTexture, Left + X, Top + StepperY, BaseU + (Down ? 2 : Over ? 1 : 0) * StateStride, StepperV, StepperWidth, StepperHeight);
    }

    private void renderButtons(GuiGraphics Graphics, int MouseX, int MouseY, boolean Powered) {
        boolean TestOver = inside(MouseX, MouseY, Left + TestX, Top + ButtonY, ButtonWidth, ButtonHeight);
        boolean TestDown = Pressed == Part.Test;
        Graphics.blit(PartsTexture, Left + TestX, Top + ButtonY, TestDown ? ButtonPressedU : TestOver ? ButtonHoverU : 0, TestV, ButtonWidth, ButtonHeight);
        centred(Graphics, Component.translatable("gui.scp_unleashed.sound_console.test"), TestX + ButtonWidth / 2, ButtonY + ButtonTextDrop + (TestDown ? 1 : 0), TestColour);
        boolean SendOver = inside(MouseX, MouseY, Left + SendX, Top + ButtonY, ButtonWidth, ButtonHeight);
        boolean SendDown = Pressed == Part.Send;
        if (Powered) {
            Graphics.blit(PartsTexture, Left + SendX, Top + ButtonY, SendDown ? ButtonPressedU : SendOver ? ButtonHoverU : 0, SendV, ButtonWidth, ButtonHeight);
        } else {
            Graphics.blit(PartsTexture, Left + SendX, Top + ButtonY, 0, SendOffV, ButtonWidth, ButtonHeight);
        }
        centred(Graphics, Component.translatable("gui.scp_unleashed.sound_console.send"), SendX + ButtonWidth / 2, ButtonY + ButtonTextDrop + (SendDown && Powered ? 1 : 0),
                Powered ? SendColour : SendOffColour);
    }

    private void renderLoopRow(GuiGraphics Graphics, int MouseX, int MouseY, boolean Powered, boolean Driving) {
        boolean LatchOver = inside(MouseX, MouseY, Left + LatchX, Top + LoopRowY, ButtonWidth, ButtonHeight);
        if (Loop) {
            Graphics.blit(PartsTexture, Left + LatchX, Top + LoopRowY, LatchOver ? LatchOnHoverU : LatchOnU, LatchOver ? LatchOnHoverV : LatchV, ButtonWidth, ButtonHeight);
        } else {
            Graphics.blit(PartsTexture, Left + LatchX, Top + LoopRowY, LatchOver ? LatchHoverU : LatchOffU, LatchV, ButtonWidth, ButtonHeight);
        }
        centred(Graphics, Component.translatable("gui.scp_unleashed.sound_console.loop"), LatchX + ButtonWidth / 2 + IconShift, LoopRowY + ButtonTextDrop + (Loop ? 1 : 0),
                Loop ? LatchOnColour : LatchColour);
        boolean StopOver = inside(MouseX, MouseY, Left + StopX, Top + LoopRowY, ButtonWidth, ButtonHeight);
        boolean StopDown = Pressed == Part.Stop;
        if (!Powered) {
            Graphics.blit(PartsTexture, Left + StopX, Top + LoopRowY, StopDisabledU, StopLowerV, ButtonWidth, ButtonHeight);
        } else if (StopDown) {
            Graphics.blit(PartsTexture, Left + StopX, Top + LoopRowY, StopPressedU, StopLowerV, ButtonWidth, ButtonHeight);
        } else {
            Graphics.blit(PartsTexture, Left + StopX, Top + LoopRowY, StopOver ? StopHoverU : StopU, StopV, ButtonWidth, ButtonHeight);
        }
        centred(Graphics, Component.translatable("gui.scp_unleashed.sound_console.stop"), StopX + ButtonWidth / 2 + IconShift,
                LoopRowY + ButtonTextDrop + (StopDown && Powered ? 1 : 0), Powered ? StopColour : SendOffColour);
        Graphics.blit(PartsTexture, Left + LoopingLampX, Top + LoopingLampY, Driving ? LoopingOnU : LoopingOffU, LoopingV, LampSize, LampSize);
        Graphics.drawString(font, Component.translatable("gui.scp_unleashed.sound_console.looping"), Left + LoopingTextX, Top + LoopingTextY,
                Driving ? LoopingOnColour : LoopingColour, false);
    }

    private void renderTooltips(GuiGraphics Graphics, int MouseX, int MouseY, boolean Powered) {
        if (Pressed != null || Dragging) {
            return;
        }
        int Row = rowAt(MouseX, MouseY);
        if (Row >= 0) {
            Choice Hovered = Rows.get(Row);
            Graphics.renderComponentTooltip(font, List.of(Hovered.Name(), Component.literal(Hovered.Id().toString()).withStyle(ChatFormatting.DARK_GRAY)),
                    MouseX, MouseY);
        } else if (chevronAt(MouseX, MouseY) >= 0 || digitColumnAt(MouseX, MouseY) >= 0) {
            hint(Graphics, Component.translatable("gui.scp_unleashed.port_panel.hint"), MouseX, MouseY);
        } else if (overVolume(MouseX, MouseY)) {
            hint(Graphics, Component.translatable("gui.scp_unleashed.sound_console.volume_hint"), MouseX, MouseY);
        } else if (overPitch(MouseX, MouseY)) {
            hint(Graphics, Component.translatable("gui.scp_unleashed.sound_console.pitch_hint"), MouseX, MouseY);
        } else if (inside(MouseX, MouseY, Left + TestX, Top + ButtonY, ButtonWidth, ButtonHeight)) {
            hint(Graphics, Component.translatable("gui.scp_unleashed.sound_console.test_hint"), MouseX, MouseY);
        } else if (inside(MouseX, MouseY, Left + SendX, Top + ButtonY, ButtonWidth, ButtonHeight)) {
            hint(Graphics, Powered ? Component.translatable(Loop ? "gui.scp_unleashed.sound_console.send_loop_hint" : "gui.scp_unleashed.sound_console.send_hint",
                    String.format("%04d", Port)) : Component.translatable("gui.scp_unleashed.sound_console.no_power").withStyle(ChatFormatting.RED), MouseX, MouseY);
        } else if (inside(MouseX, MouseY, Left + LatchX, Top + LoopRowY, ButtonWidth, ButtonHeight)) {
            hint(Graphics, Component.translatable("gui.scp_unleashed.sound_console.loop_hint"), MouseX, MouseY);
        } else if (inside(MouseX, MouseY, Left + StopX, Top + LoopRowY, ButtonWidth, ButtonHeight)) {
            hint(Graphics, Powered ? Component.translatable("gui.scp_unleashed.sound_console.stop_hint", String.format("%04d", Port))
                    : Component.translatable("gui.scp_unleashed.sound_console.no_power").withStyle(ChatFormatting.RED), MouseX, MouseY);
        } else if (inside(MouseX, MouseY, Left + LoopingPlateX, Top + LoopRowY, LoopingPlateWidth, ButtonHeight)) {
            hint(Graphics, Component.translatable("gui.scp_unleashed.sound_console.looping_hint"), MouseX, MouseY);
        }
    }

    private void hint(GuiGraphics Graphics, Component Text, int MouseX, int MouseY) {
        Graphics.renderTooltip(font, font.split(Text, HintWidth), MouseX, MouseY);
    }

    private void centred(GuiGraphics Graphics, Component Text, int CentreX, int Y, int Colour) {
        Graphics.drawString(font, Text, Left + CentreX - font.width(Text) / 2, Top + Y, Colour, false);
    }

    @Override
    public boolean mouseClicked(double MouseX, double MouseY, int Button) {
        boolean LeftButton = Button == GLFW.GLFW_MOUSE_BUTTON_LEFT;
        boolean RightButton = Button == GLFW.GLFW_MOUSE_BUTTON_RIGHT;
        int Chevron = chevronAt(MouseX, MouseY);
        if (Chevron >= 0 && (LeftButton || RightButton)) {
            press(Part.Chevron, Chevron, LeftButton ? 1.0f : 0.92f);
            changeDigit(Chevron, LeftButton ? 1 : -1);
            return true;
        }
        if (!LeftButton && !RightButton) {
            return super.mouseClicked(MouseX, MouseY, Button);
        }
        int Step = LeftButton ? 1 : -1;
        if (inside(MouseX, MouseY, Left + VolumeMinusX, Top + StepperY, StepperWidth, StepperHeight)) {
            press(Part.VolumeDown, 0, 0.96f);
            stepVolume(-Step);
            return true;
        }
        if (inside(MouseX, MouseY, Left + VolumePlusX, Top + StepperY, StepperWidth, StepperHeight)) {
            press(Part.VolumeUp, 0, 1.04f);
            stepVolume(Step);
            return true;
        }
        if (inside(MouseX, MouseY, Left + PitchMinusX, Top + StepperY, StepperWidth, StepperHeight)) {
            press(Part.PitchDown, 0, 0.96f);
            stepPitch(-Step);
            return true;
        }
        if (inside(MouseX, MouseY, Left + PitchPlusX, Top + StepperY, StepperWidth, StepperHeight)) {
            press(Part.PitchUp, 0, 1.04f);
            stepPitch(Step);
            return true;
        }
        if (!LeftButton) {
            return super.mouseClicked(MouseX, MouseY, Button);
        }
        int Tab = tabAt(MouseX, MouseY);
        if (Tab >= 0) {
            showCategory(SoundCatalog.Category.values()[Tab]);
            return true;
        }
        int Row = rowAt(MouseX, MouseY);
        if (Row >= 0) {
            select(Rows.get(Row));
            return true;
        }
        if (inside(MouseX, MouseY, Left + TrackX, Top + TrackY, TrackWidth, TrackHeight) && maxScroll() > 0) {
            Dragging = true;
            scrollTo(MouseY);
            return true;
        }
        if (inside(MouseX, MouseY, Left + TestX, Top + ButtonY, ButtonWidth, ButtonHeight)) {
            press(Part.Test, 0, 1.0f);
            preview();
            return true;
        }
        if (inside(MouseX, MouseY, Left + SendX, Top + ButtonY, ButtonWidth, ButtonHeight)) {
            press(Part.Send, 0, 1.0f);
            PacketDistributor.sendToServer(new SoundConsolePayload(Pos, Sound, Volume, Pitch, Loop, SoundConsolePayload.Command.Send));
            return true;
        }
        if (inside(MouseX, MouseY, Left + LatchX, Top + LoopRowY, ButtonWidth, ButtonHeight)) {
            Loop = !Loop;
            play((Loop ? SpeakerRegistry.LatchOn : SpeakerRegistry.LatchOff).get(), 1.0f);
            sync();
            return true;
        }
        if (inside(MouseX, MouseY, Left + StopX, Top + LoopRowY, ButtonWidth, ButtonHeight)) {
            press(Part.Stop, 0, 0.9f);
            PacketDistributor.sendToServer(new SoundConsolePayload(Pos, Sound, Volume, Pitch, Loop, SoundConsolePayload.Command.Stop));
            return true;
        }
        return super.mouseClicked(MouseX, MouseY, Button);
    }

    @Override
    public boolean mouseReleased(double MouseX, double MouseY, int Button) {
        if (Pressed != null) {
            play(ModSounds.PanelRelease.get(), 1.0f);
        }
        Pressed = null;
        PressedIndex = -1;
        Dragging = false;
        return super.mouseReleased(MouseX, MouseY, Button);
    }

    @Override
    public boolean mouseDragged(double MouseX, double MouseY, int Button, double DragX, double DragY) {
        if (Dragging) {
            scrollTo(MouseY);
            return true;
        }
        return super.mouseDragged(MouseX, MouseY, Button, DragX, DragY);
    }

    @Override
    public boolean mouseScrolled(double MouseX, double MouseY, double ScrollX, double ScrollY) {
        if (ScrollY == 0.0) {
            return super.mouseScrolled(MouseX, MouseY, ScrollX, ScrollY);
        }
        int Step = ScrollY > 0.0 ? 1 : -1;
        int Column = digitColumnAt(MouseX, MouseY);
        if (Column < 0) {
            Column = chevronAt(MouseX, MouseY);
        }
        if (Column >= 0) {
            play(ModSounds.PanelTick.get(), Step > 0 ? 1.05f : 0.95f);
            changeDigit(Column, Step);
            return true;
        }
        if (overVolume(MouseX, MouseY)) {
            play(ModSounds.PanelTick.get(), Step > 0 ? 1.05f : 0.95f);
            stepVolume(Step);
            return true;
        }
        if (overPitch(MouseX, MouseY)) {
            play(ModSounds.PanelTick.get(), Step > 0 ? 1.05f : 0.95f);
            stepPitch(Step);
            return true;
        }
        if (inside(MouseX, MouseY, Left + ListX, Top + ListY, TrackX + TrackWidth - ListX, TrackHeight)) {
            int Previous = Scroll;
            Scroll = Mth.clamp(Scroll - Step, 0, maxScroll());
            if (Scroll != Previous) {
                play(ModSounds.PanelTick.get(), Step > 0 ? 1.05f : 0.95f);
            }
            return true;
        }
        return super.mouseScrolled(MouseX, MouseY, ScrollX, ScrollY);
    }

    @Override
    public boolean keyPressed(int Key, int ScanCode, int Modifiers) {
        int Typed = Key >= GLFW.GLFW_KEY_0 && Key <= GLFW.GLFW_KEY_9 ? Key - GLFW.GLFW_KEY_0
                : Key >= GLFW.GLFW_KEY_KP_0 && Key <= GLFW.GLFW_KEY_KP_9 ? Key - GLFW.GLFW_KEY_KP_0 : -1;
        if (Typed >= 0) {
            play(ModSounds.PanelPress.get(), 1.0f);
            setPort(Port % 1000 * 10 + Typed);
            return true;
        }
        if (Key == GLFW.GLFW_KEY_BACKSPACE) {
            play(ModSounds.PanelRelease.get(), 0.9f);
            setPort(Port / 10);
            return true;
        }
        if (Key == GLFW.GLFW_KEY_UP || Key == GLFW.GLFW_KEY_DOWN) {
            moveSelection(Key == GLFW.GLFW_KEY_UP ? -1 : 1);
            return true;
        }
        if (Key == GLFW.GLFW_KEY_TAB) {
            SoundCatalog.Category[] Groups = SoundCatalog.Category.values();
            showCategory(Groups[(Shown.ordinal() + (hasShiftDown() ? Groups.length - 1 : 1)) % Groups.length]);
            return true;
        }
        if (Key == GLFW.GLFW_KEY_ENTER || Key == GLFW.GLFW_KEY_KP_ENTER || (minecraft != null && minecraft.options.keyInventory.matches(Key, ScanCode))) {
            onClose();
            return true;
        }
        return super.keyPressed(Key, ScanCode, Modifiers);
    }

    private void press(Part Which, int Index, float Tone) {
        Pressed = Which;
        PressedIndex = Index;
        play(ModSounds.PanelPress.get(), Tone);
    }

    private void play(SoundEvent Event, float Tone) {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(Event, Tone, 0.3f));
        }
    }

    private void preview() {
        if (minecraft == null) {
            return;
        }
        if (Preview != null) {
            minecraft.getSoundManager().stop(Preview);
        }
        Preview = new SimpleSoundInstance(Sound, SoundMessages.Source, Math.min(Volume, TestVolume), Pitch, SoundInstance.createUnseededRandom(), false, 0,
                SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true);
        minecraft.getSoundManager().play(Preview);
    }

    private void showCategory(SoundCatalog.Category Group) {
        if (Group == Shown) {
            return;
        }
        Shown = Group;
        LibraryVersion = AnomalousLibrary.clientVersion();
        Rows = rowsFor(Group);
        Scroll = 0;
        revealSelection();
        play(ModSounds.PanelSelect.get(), 1.0f);
    }

    private void select(Choice Row) {
        if (Row.Id().equals(Sound)) {
            return;
        }
        Sound = Row.Id();
        play(ModSounds.PanelSelect.get(), Row.Anomalous() ? 0.85f : 1.0f);
        sync();
    }

    private void moveSelection(int Step) {
        if (Rows.isEmpty()) {
            return;
        }
        int Current = -1;
        for (int Index = 0; Index < Rows.size(); Index++) {
            if (Rows.get(Index).Id().equals(Sound)) {
                Current = Index;
            }
        }
        int Next = Current < 0 ? (Step > 0 ? 0 : Rows.size() - 1) : Mth.clamp(Current + Step, 0, Rows.size() - 1);
        if (Next != Current) {
            select(Rows.get(Next));
            revealSelection();
        }
    }

    private void revealSelection() {
        for (int Index = 0; Index < Rows.size(); Index++) {
            if (Rows.get(Index).Id().equals(Sound)) {
                if (Index < Scroll) {
                    Scroll = Index;
                } else if (Index >= Scroll + VisibleRows) {
                    Scroll = Index - VisibleRows + 1;
                }
            }
        }
        Scroll = Mth.clamp(Scroll, 0, maxScroll());
    }

    private void stepVolume(int Step) {
        float Next = Volume;
        if (Step > 0) {
            for (float Candidate : VolumeSteps) {
                if (Candidate > Volume + 1.0E-4f) {
                    Next = Candidate;
                    break;
                }
            }
        } else {
            for (int Index = VolumeSteps.length - 1; Index >= 0; Index--) {
                if (VolumeSteps[Index] < Volume - 1.0E-4f) {
                    Next = VolumeSteps[Index];
                    break;
                }
            }
        }
        if (Next != Volume) {
            Volume = Next;
            sync();
        }
    }

    private void stepPitch(int Step) {
        float Amount = hasShiftDown() ? PitchBigStep : PitchStep;
        float Next = Mth.clamp(Math.round((Pitch + Step * Amount) / PitchStep) * PitchStep, SoundMessages.MinPitch, SoundMessages.MaxPitch);
        if (Math.abs(Next - Pitch) > 1.0E-4f) {
            Pitch = Next;
            sync();
        }
    }

    private void sync() {
        PacketDistributor.sendToServer(new SoundConsolePayload(Pos, Sound, Volume, Pitch, Loop, SoundConsolePayload.Command.Apply));
    }

    private int digit(int Index) {
        return Port / Places[Index] % 10;
    }

    private void changeDigit(int Index, int Delta) {
        int Current = digit(Index);
        setPort(Port + (Math.floorMod(Current + Delta, 10) - Current) * Places[Index]);
    }

    private void setPort(int Requested) {
        int Clamped = WireChannels.clampPort(Requested);
        if (Clamped != Port) {
            Port = Clamped;
            PacketDistributor.sendToServer(new SetPortPayload(Pos, 0, Clamped));
        }
    }

    private int maxScroll() {
        return Math.max(0, Rows.size() - VisibleRows);
    }

    private int thumbTop() {
        int Range = TrackHeight - ThumbHeight;
        int Max = maxScroll();
        return TrackY + (Max == 0 ? 0 : Math.round((float) Scroll * Range / Max));
    }

    private void scrollTo(double MouseY) {
        int Max = maxScroll();
        if (Max == 0) {
            return;
        }
        double Fraction = (MouseY - (Top + TrackY) - ThumbHeight / 2.0) / (TrackHeight - ThumbHeight);
        int Next = Mth.clamp((int) Math.round(Fraction * Max), 0, Max);
        if (Next != Scroll) {
            Scroll = Next;
            play(ModSounds.PanelTick.get(), 1.0f);
        }
    }

    private int tabAt(double MouseX, double MouseY) {
        for (int Index = 0; Index < TabX.length; Index++) {
            if (inside(MouseX, MouseY, Left + TabX[Index], Top + TabY, TabWidth, TabHeight)) {
                return Index;
            }
        }
        return -1;
    }

    private int rowAt(double MouseX, double MouseY) {
        if (!inside(MouseX, MouseY, Left + ListX, Top + ListY, ListWidth, VisibleRows * RowHeight)) {
            return -1;
        }
        int Index = Scroll + (int) ((MouseY - Top - ListY) / RowHeight);
        return Index < Rows.size() ? Index : -1;
    }

    private int chevronAt(double MouseX, double MouseY) {
        for (int Index = 0; Index < DigitX.length; Index++) {
            if (inside(MouseX, MouseY, Left + DigitX[Index] + ChevronOffset, Top + ChevronY, ChevronWidth, ChevronHeight)) {
                return Index;
            }
        }
        return -1;
    }

    private int digitColumnAt(double MouseX, double MouseY) {
        for (int Index = 0; Index < DigitX.length; Index++) {
            if (inside(MouseX, MouseY, Left + DigitX[Index], Top + DigitY, DigitWidth, DigitHeight)) {
                return Index;
            }
        }
        return -1;
    }

    private boolean overVolume(double MouseX, double MouseY) {
        return inside(MouseX, MouseY, Left + VolumeMinusX, Top + StepperY, VolumePlusX + StepperWidth - VolumeMinusX, StepperHeight);
    }

    private boolean overPitch(double MouseX, double MouseY) {
        return inside(MouseX, MouseY, Left + PitchMinusX, Top + StepperY, PitchPlusX + StepperWidth - PitchMinusX, StepperHeight);
    }

    private static boolean inside(double MouseX, double MouseY, int X, int Y, int Width, int Height) {
        return MouseX >= X && MouseX < X + Width && MouseY >= Y && MouseY < Y + Height;
    }

    private enum Part {
        Chevron,
        VolumeDown,
        VolumeUp,
        PitchDown,
        PitchUp,
        Test,
        Send,
        Stop
    }

    private record Choice(ResourceLocation Id, Component Name, boolean Anomalous) {
    }
}
