package net.rai.scp_unleashed.archive.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.anomaly.RiftFragment;
import net.rai.scp_unleashed.archive.ArchiveBlockEntity;
import net.rai.scp_unleashed.archive.ArchiveMenu;
import net.rai.scp_unleashed.archive.ArchiveRegistry;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.ArchivePool;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.archive.document.DocumentRoller;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ArchiveScreen extends AbstractContainerScreen<ArchiveMenu> {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/archive/terminal.png");
    private static final int TextureSize = 512;
    private static final int PanelWidth = 296;
    private static final int PanelHeight = 244;
    private static final int GlassX0 = 10;
    private static final int GlassY0 = 24;
    private static final int GlassX1 = 286;
    private static final int GlassY1 = 74;
    private static final int FeltX0 = 6;
    private static final int FeltY0 = 86;
    private static final int FeltX1 = 290;
    private static final int ReaderY0 = 84;
    private static final int LipY = 137;
    private static final int MouthY = 138;
    private static final int ThroatX0 = 9;
    private static final int ThroatX1 = 287;
    private static final int ThroatY0 = 139;
    private static final int ThroatY1 = 149;
    private static final int TrayHintY = 107;
    private static final int CardWidth = 54;
    private static final int CardHeight = 50;
    private static final int CardY = 87;
    private static final int CardLeft = 9;
    private static final int CardPitch = 56;
    private static final int HoverLift = 1;
    private static final int StampWidth = 52;
    private static final int LogX = 15;
    private static final int LogY = 27;
    private static final int LogPitch = 9;
    private static final int LogLines = 5;
    private static final int LogWidth = 186;
    private static final int OddsX = 212;
    private static final int OddsWidth = 70;
    private static final int OddsBarWidth = 10;
    private static final int OddsPitch = 15;
    private static final int OddsBottom = 59;
    private static final int OddsHeight = 21;
    private static final int RollX = 229;
    private static final int RollY = 162;
    private static final int RollWidth = 58;
    private static final int RollHeight = 24;
    private static final int HeadX0 = 206;
    private static final int HeadY0 = 194;
    private static final int HeadTravel = 33;
    private static final int SlitBottom = 209;
    private static final int SheetX = 222;
    private static final int SheetWidth = 24;
    private static final int SheetHeight = 30;
    private static final int OutputClipX0 = 197;
    private static final int OutputClipX1 = 271;
    private static final int OutputClipY1 = 238;
    private static final int LandWidth = 18;
    private static final int LandHeight = 14;
    private static final int[] LampPrint = {186, 195};
    private static final int[] LampReady = {186, 203};
    private static final int[][] StatusLamps = {{266, 7}, {274, 7}, {282, 7}};
    private static final int[] CardBase = {0, 256, 54, 50};
    private static final int[] CardVoid = {54, 256, 54, 50};
    private static final int[] CardShadow = {108, 256, 58, 54};
    private static final int[] CardGlow = {166, 256, 58, 54};
    private static final int IconU = 224;
    private static final int IconV = 256;
    private static final int UnknownIconU = 344;
    private static final int ZClassIconU = 360;
    private static final int[] TabMask = {320, 256, 20, 5};
    private static final int[] StampFrame = {0, 312, 52, 14};
    private static final int[] StampDistress = {52, 312, 52, 14};
    private static final int RollU = 0;
    private static final int RollV = 330;
    private static final int LampU = 232;
    private static final int LampV = 330;
    private static final int[] PaperSheet = {262, 330, 24, 30};
    private static final int[] PrintHead = {286, 330, 10, 8};
    private static final int[] FragmentGlow = {296, 330, 22, 22};
    private static final int TitleColour = 0xFFE8DDC0;
    private static final int LabelColour = 0xFFC8C4B6;
    private static final int PhosphorText = 0xFF9CF5B0;
    private static final int PhosphorDim = 0xFF3E9A5C;
    private static final int PhosphorHot = 0xFFE8FFE8;
    private static final int PhosphorFaint = 0xFF1C5A34;
    private static final int AlertAmber = 0xFFFFC857;
    private static final int AlertAmberDim = 0xFFB8862E;
    private static final int AlertRed = 0xFFFF6B5B;
    private static final int InkColour = 0xFF2E261E;
    private static final int FeltText = 0xFF284A38;
    private static final int PurgeInk = 0xB0201A;
    private static final int VoidInk = 0x3C3C38;
    private static final int ToothColour = 0xFFAAB0BA;
    private static final int RollerColour = 0xFF787E88;
    private static final int DustLight = 0xFFEED6A0;
    private static final int DustWhite = 0xFFF8F5E8;
    private static final int PuffColour = 0xFFD6D0BE;
    private static final int SettleTicks = 3;
    private static final int BootTicks = 8;
    private static final int DealStep = 3;
    private static final int DealSlide = 6;
    private static final int StampDelay = 16;
    private static final int StampStep = 4;
    private static final int StampSlam = 3;
    private static final int ReadyDelay = StampDelay + StampStep * 4 + StampSlam;
    private static final int PurgeStamp = 3;
    private static final int ReadDelay = 2;
    private static final int ReadTicks = 6;
    private static final int ShredDelay = 6;
    private static final int ShredStagger = 4;
    private static final float GrabTicks = 2.0f;
    private static final float FeedAccel = 2.0f;
    private static final float FeedSpeed = 5.5f;
    private static final int FeedTravel = ThroatY0 - CardY;
    private static final float StripExit = 4.0f;
    private static final int StripPitch = 6;
    private static final int StripWidth = 5;
    private static final int StripCount = 9;
    private static final int StripBands = 3;
    private static final float StripShade = 0.22f;
    private static final float StripFlare = 0.5f;
    private static final float StripFlutter = 1.1f;
    private static final float BackShade = 0.86f;
    private static final int DustCount = 7;
    private static final float DustLife = 3.0f;
    private static final int PassStart = 10;
    private static final int PassCount = 10;
    private static final int PassTicks = 3;
    private static final float PassMove = 0.7f;
    private static final int LineStep = 3;
    private static final int FeedEnd = PassStart + PassCount * PassTicks;
    private static final int TearTicks = 3;
    private static final int DropStart = FeedEnd + TearTicks;
    private static final int DropTicks = ArchiveBlockEntity.PrintTicks - DropStart;
    private static final float BounceTicks = 8.0f;
    private static final float BounceHeight = 3.5f;
    private static final float BouncePeriod = 3.2f;
    private static final float BounceDecay = 2.4f;
    private static final float SquashTicks = 1.0f;
    private static final float Squash = 0.18f;
    private static final int PuffCount = 4;
    private static final float PuffLife = 5.0f;
    private static final int PressTicks = 4;
    private static final int ErrorTicks = 30;
    private static final int PaperSettle = 8;
    private static final int PickLock = 20;
    private static final int Never = -100000;
    private static final String[] Numerals = {"I", "II", "III", "IV", "V"};

    private final TerminalLog Log = new TerminalLog(LogLines);
    private final RandomSource Noise = RandomSource.create();
    private final int[] Codes = new int[ArchiveBlockEntity.CardCount];
    private boolean Initialized;
    private int Ticks;
    private float FrameTime;
    private int DealStart = Never;
    private int LastSerial;
    private int LastCount;
    private int LastMask;
    private int PrintStart = Never;
    private int PrintSerial;
    private int PrintedCard = -1;
    private int PurgeCount;
    private boolean WasPrinting;
    private int LandedAt = Never;
    private ItemStack LastFragment = ItemStack.EMPTY;
    private int LastPaper;
    private int PaperChangedAt = Never;
    private int PressedAt = Never;
    private int ErrorAt = Never;
    private String LastError = "";
    private int PickLockUntil;
    private int HoveredCard = -1;
    private float Shade = 1.0f;

    public ArchiveScreen(ArchiveMenu Menu, Inventory PlayerInventory, Component Title) {
        super(Menu, PlayerInventory, Title);
        imageWidth = PanelWidth;
        imageHeight = PanelHeight;
        titleLabelX = 12;
        titleLabelY = 6;
        inventoryLabelX = 8;
        inventoryLabelY = 153;
    }

    @Override
    protected void init() {
        super.init();
        if (!Initialized) {
            Initialized = true;
            LastSerial = menu.serial();
            LastCount = menu.cardCount();
            LastMask = menu.tierMask();
            for (int Index = 0; Index < Codes.length; Index++) {
                Codes[Index] = menu.cardCode(Index);
            }
            WasPrinting = isPrinting();
            if (WasPrinting) {
                beginPrint();
            }
            int Node = Math.floorMod(menu.getPos().hashCode(), 89) + 10;
            Log.push(text("gui.scp_unleashed.archive.log.boot", Node), PhosphorDim);
            play(ArchiveRegistry.Boot, 1.0f, 0.55f);
        }
    }

    private static String text(String Key, Object... Arguments) {
        return Component.translatable(Key, Arguments).getString();
    }

    private void play(DeferredHolder<SoundEvent, SoundEvent> Sound, float Pitch, float Volume) {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(Sound.get(), Pitch, Volume));
        }
    }

    private int paperCount() {
        ItemStack Paper = menu.getPaper();
        return ArchiveBlockEntity.isPaper(Paper) ? Paper.getCount() : 0;
    }

    @Nullable
    private FragmentRarity fragmentRarity() {
        return menu.getFragment().getItem() instanceof RiftFragment Source ? Source.getRarity() : null;
    }

    private int fragmentTier() {
        return ArchivePool.tierFor(menu.getFragment());
    }

    private int fragmentColour() {
        FragmentRarity Rarity = fragmentRarity();
        return Rarity != null ? Rarity.getColour() : ArchivePool.poolOf(menu.getFragment()).getColour();
    }

    private String fragmentName() {
        FragmentRarity Rarity = fragmentRarity();
        return Rarity != null ? rarityName(Rarity) : text(ArchivePool.poolOf(menu.getFragment()).labelKey()).toUpperCase(Locale.ROOT);
    }

    private int stampTick(int Index) {
        return DealStart + StampDelay + Index * StampStep;
    }

    private boolean isPrinting() {
        return menu.printRemaining() > 0;
    }

    private void beginPrint() {
        int Elapsed = Mth.clamp(ArchiveBlockEntity.PrintTicks - menu.printRemaining(), 0, ArchiveBlockEntity.PrintTicks);
        PrintStart = Ticks - Elapsed;
        PrintSerial = menu.serial();
        PrintedCard = menu.printingCard();
        PurgeCount = Math.min(ArchiveBlockEntity.CardCount, Math.max(LastCount, menu.cardCount()));
        LandedAt = Never;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        Ticks++;
        int Typed = Ticks > BootTicks ? Log.tick() : 0;
        if (Typed > 0 && Ticks % 2 == 0) {
            play(ArchiveRegistry.Keystroke, 0.9f + Noise.nextFloat() * 0.25f, 0.12f);
        }
        if (Ticks == SettleTicks) {
            settle();
            return;
        }
        if (Ticks < SettleTicks) {
            return;
        }
        observeRoll();
        observePrint();
        observeSlots();
        observePool();
        scheduledSounds();
    }

    private void settle() {
        LastFragment = menu.getFragment().copy();
        LastPaper = paperCount();
        LastMask = menu.tierMask();
        Log.push(text("gui.scp_unleashed.archive.log.link"), PhosphorDim);
        if (isPrinting()) {
            Log.push(text("gui.scp_unleashed.archive.log.busy"), AlertAmber);
            return;
        }
        if (menu.cardCount() > 0) {
            Log.push(text("gui.scp_unleashed.archive.log.waiting", menu.cardCount()), PhosphorText);
            Log.push(text("gui.scp_unleashed.archive.log.select"), PhosphorHot);
            return;
        }
        if (!menu.getOutput().isEmpty()) {
            Log.push(text("gui.scp_unleashed.archive.log.collect"), PhosphorHot);
        }
        if (poolBlocker() != null) {
            logPool();
            return;
        }
        logFragment();
        Log.push(text("gui.scp_unleashed.archive.log.paper", LastPaper), PhosphorText);
        if (menu.getOutput().isEmpty() && rollBlocker() == null) {
            Log.push(text("gui.scp_unleashed.archive.log.ready"), PhosphorHot);
        }
    }

    private void logPool() {
        boolean Empty = AnomalyDocuments.client().isEmpty();
        Log.push(text(Empty ? "gui.scp_unleashed.archive.log.no_records" : "gui.scp_unleashed.archive.log.exhausted"), AlertAmber);
        Log.push(text("gui.scp_unleashed.archive.log.roll_disabled"), PhosphorDim);
    }

    private void logFragment() {
        int Tier = fragmentTier();
        if (Tier <= 0) {
            Log.push(text("gui.scp_unleashed.archive.log.no_fragment"), PhosphorDim);
        } else {
            Log.push(text("gui.scp_unleashed.archive.log.fragment", fragmentName(), Numerals[Tier - 1]), PhosphorText);
        }
    }

    private static String rarityName(FragmentRarity Rarity) {
        return text("archive.scp_unleashed.rarity." + Rarity.getSerializedName()).toUpperCase(Locale.ROOT);
    }

    private void observeRoll() {
        if (menu.serial() != LastSerial) {
            LastSerial = menu.serial();
            DealStart = Ticks;
            PrintStart = Never;
            PrintedCard = -1;
            LandedAt = Never;
            for (int Index = 0; Index < Codes.length; Index++) {
                Codes[Index] = menu.cardCode(Index);
            }
            LastCount = menu.cardCount();
            Log.push(text("gui.scp_unleashed.archive.log.query"), PhosphorText);
            Log.push(text("gui.scp_unleashed.archive.log.recovered", LastCount), PhosphorText);
            return;
        }
        for (int Index = 0; Index < Codes.length; Index++) {
            int Code = menu.cardCode(Index);
            if (Code == ArchiveBlockEntity.VoidCard && Codes[Index] != ArchiveBlockEntity.VoidCard && Codes[Index] != 0) {
                Log.push(text("gui.scp_unleashed.archive.log.expunged", String.format(Locale.ROOT, "%02d", Index + 1)), AlertRed);
                play(ArchiveRegistry.Deny, 1.0f, 0.5f);
                ErrorAt = Ticks;
            }
            if (Code != 0) {
                Codes[Index] = Code;
            }
        }
        if (menu.cardCount() > 0) {
            LastCount = menu.cardCount();
        }
    }

    private void observePrint() {
        boolean Printing = isPrinting();
        if (Printing && !WasPrinting) {
            beginPrint();
            Log.push(text("gui.scp_unleashed.archive.log.selected", String.format(Locale.ROOT, "%02d", PrintedCard + 1)), PhosphorText);
            int Purged = Math.max(0, PurgeCount - 1);
            if (Purged > 0) {
                Log.push(text("gui.scp_unleashed.archive.log.purging", Purged), AlertAmber);
            }
            Log.push(text("gui.scp_unleashed.archive.log.printing"), PhosphorHot);
        }
        if (!Printing && WasPrinting) {
            LandedAt = Ticks;
            Log.push(text("gui.scp_unleashed.archive.log.complete"), PhosphorText);
            Log.push(text("gui.scp_unleashed.archive.log.collect"), PhosphorHot);
            if (poolBlocker() != null) {
                logPool();
            }
            play(ArchiveRegistry.Deal, 0.75f, 0.65f);
        }
        WasPrinting = Printing;
    }

    private void observeSlots() {
        ItemStack Fragment = menu.getFragment();
        if (!ItemStack.isSameItemSameComponents(Fragment, LastFragment) || Fragment.getCount() != LastFragment.getCount()) {
            boolean Changed = !ItemStack.isSameItemSameComponents(Fragment, LastFragment);
            LastFragment = Fragment.copy();
            if (Changed) {
                if (Fragment.isEmpty()) {
                    Log.push(text("gui.scp_unleashed.archive.log.fragment_removed"), PhosphorDim);
                } else {
                    logFragment();
                    play(ArchiveRegistry.Keystroke, 0.6f, 0.4f);
                }
            }
        }
        int Paper = paperCount();
        if (Paper != LastPaper) {
            LastPaper = Paper;
            PaperChangedAt = Ticks;
        }
        if (PaperChangedAt != Never && Ticks - PaperChangedAt == PaperSettle && menu.cardCount() == 0 && !isPrinting()) {
            Log.push(text("gui.scp_unleashed.archive.log.paper", LastPaper), PhosphorText);
            PaperChangedAt = Never;
        }
    }

    private void observePool() {
        int Mask = menu.tierMask();
        if (Mask == LastMask) {
            return;
        }
        boolean WasEmpty = LastMask == 0;
        LastMask = Mask;
        if (menu.cardCount() > 0 || isPrinting()) {
            return;
        }
        if (Mask == 0) {
            logPool();
        } else if (WasEmpty && rollBlocker() == null) {
            Log.push(text("gui.scp_unleashed.archive.log.ready"), PhosphorHot);
        }
    }

    private void scheduledSounds() {
        if (DealStart != Never) {
            for (int Index = 0; Index < LastCount; Index++) {
                if (Ticks == DealStart + Index * DealStep) {
                    play(ArchiveRegistry.Deal, 0.92f + Index * 0.04f, 0.5f);
                }
                if (Ticks == stampTick(Index)) {
                    play(ArchiveRegistry.StampSound, 0.95f + Noise.nextFloat() * 0.1f, 0.7f);
                }
            }
            if (Ticks == DealStart + ReadyDelay && menu.cardCount() > 0 && !isPrinting()) {
                Log.push(text("gui.scp_unleashed.archive.log.select"), PhosphorHot);
            }
        }
        if (PrintStart == Never) {
            return;
        }
        int Since = Ticks - PrintStart;
        if (Since == PurgeStamp && PurgeCount > 1) {
            play(ArchiveRegistry.StampSound, 0.85f, 0.6f);
        }
        if (Since == ReadDelay) {
            play(ArchiveRegistry.Deal, 1.25f, 0.45f);
        }
        int Order = 0;
        for (int Index = 0; Index < PurgeCount; Index++) {
            if (Index == PrintedCard) {
                continue;
            }
            if (Since == ShredDelay + Order * ShredStagger) {
                play(ArchiveRegistry.Shred, 0.94f + Order * 0.04f, 0.5f);
            }
            Order++;
        }
    }

    @Nullable
    private String poolBlocker() {
        if (menu.tierMask() != 0) {
            return null;
        }
        return AnomalyDocuments.client().isEmpty() ? "gui.scp_unleashed.archive.error.no_records" : "gui.scp_unleashed.archive.error.exhausted";
    }

    @Nullable
    private String rollBlocker() {
        if (isPrinting()) {
            return "gui.scp_unleashed.archive.error.busy";
        }
        if (menu.cardCount() > 0) {
            return "gui.scp_unleashed.archive.error.select";
        }
        String Pool = poolBlocker();
        if (Pool != null) {
            return Pool;
        }
        if (fragmentTier() <= 0) {
            return "gui.scp_unleashed.archive.error.fragment";
        }
        if (paperCount() < ArchiveBlockEntity.PaperCost) {
            return "gui.scp_unleashed.archive.error.paper";
        }
        return null;
    }

    private boolean ready() {
        return DealStart == Never || Ticks >= DealStart + ReadyDelay;
    }

    private boolean selectable(int Index) {
        return Index >= 0 && Index < menu.cardCount() && menu.cardCode(Index) != ArchiveBlockEntity.VoidCard && !isPrinting() && ready() && Ticks >= PickLockUntil;
    }

    private void fail(String Key, Object... Arguments) {
        play(ArchiveRegistry.Deny, 1.0f, 0.5f);
        String Message = text(Key, Arguments);
        boolean Repeat = Message.equals(LastError) && Ticks - ErrorAt < ErrorTicks;
        if (!Repeat) {
            Log.push(Message, AlertRed);
        }
        ErrorAt = Ticks;
        LastError = Message;
    }

    private int cardAt(double MouseX, double MouseY) {
        if (isPrinting()) {
            return -1;
        }
        for (int Index = 0; Index < menu.cardCount(); Index++) {
            int X = leftPos + CardLeft + Index * CardPitch;
            int Y = topPos + CardY - (Index == HoveredCard ? HoverLift : 0);
            if (MouseX >= X && MouseX < X + CardWidth && MouseY >= Y && MouseY < Y + CardHeight + HoverLift) {
                return Index;
            }
        }
        return -1;
    }

    private boolean overRoll(double MouseX, double MouseY) {
        return MouseX >= leftPos + RollX && MouseX < leftPos + RollX + RollWidth && MouseY >= topPos + RollY && MouseY < topPos + RollY + RollHeight;
    }

    private boolean canPress() {
        return menu.getCarried().isEmpty() && minecraft != null && minecraft.gameMode != null;
    }

    private void pressRoll() {
        String Blocker = rollBlocker();
        if (Blocker == null) {
            PressedAt = Ticks;
            play(ArchiveRegistry.Pick, 0.7f, 0.6f);
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ArchiveMenu.RollButton);
        } else if (Blocker.endsWith("paper")) {
            fail(Blocker, paperCount(), ArchiveBlockEntity.PaperCost);
        } else {
            fail(Blocker);
        }
    }

    private void pressCard(int Card) {
        if (!menu.getOutput().isEmpty() && selectable(Card)) {
            fail("gui.scp_unleashed.archive.error.output");
        } else if (selectable(Card)) {
            PickLockUntil = Ticks + PickLock;
            play(ArchiveRegistry.Pick, 1.0f, 0.7f);
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, ArchiveMenu.PickButton + Card);
        }
    }

    @Override
    public boolean mouseClicked(double MouseX, double MouseY, int Button) {
        if (Button == GLFW.GLFW_MOUSE_BUTTON_LEFT && canPress()) {
            if (overRoll(MouseX, MouseY)) {
                pressRoll();
                return true;
            }
            int Card = cardAt(MouseX, MouseY);
            if (Card >= 0) {
                pressCard(Card);
                return true;
            }
        }
        return super.mouseClicked(MouseX, MouseY, Button);
    }

    @Override
    public boolean keyPressed(int Key, int ScanCode, int Modifiers) {
        if (hoveredSlot == null && canPress()) {
            if (Key == GLFW.GLFW_KEY_ENTER || Key == GLFW.GLFW_KEY_KP_ENTER) {
                pressRoll();
                return true;
            }
            int Card = Key - GLFW.GLFW_KEY_1;
            if (Card >= 0 && Card < menu.cardCount()) {
                pressCard(Card);
                return true;
            }
        }
        return super.keyPressed(Key, ScanCode, Modifiers);
    }

    @Override
    protected void renderBg(GuiGraphics Graphics, float PartialTick, int MouseX, int MouseY) {
        float Time = Ticks + PartialTick;
        FrameTime = Time;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Graphics.blit(Texture, leftPos, topPos, 0, 0, imageWidth, imageHeight, TextureSize, TextureSize);
        renderStatusLamps(Graphics, Time);
        renderCrt(Graphics, Time);
        HoveredCard = selectableHover(MouseX, MouseY);
        renderTray(Graphics, Time);
        renderIntake(Graphics, Time, MouseX, MouseY);
        renderPrinter(Graphics, Time);
        RenderSystem.disableBlend();
    }

    private int selectableHover(int MouseX, int MouseY) {
        int Index = cardAt(MouseX, MouseY);
        return selectable(Index) ? Index : -1;
    }

    private void sprite(GuiGraphics Graphics, int[] Region, int X, int Y) {
        Graphics.blit(Texture, X, Y, Region[0], Region[1], Region[2], Region[3], TextureSize, TextureSize);
    }

    private void tint(GuiGraphics Graphics, float Red, float Green, float Blue, float Alpha) {
        Graphics.setColor(Red * Shade, Green * Shade, Blue * Shade, Alpha);
    }

    private void tinted(GuiGraphics Graphics, int[] Region, int X, int Y, int Colour, float Alpha) {
        tint(Graphics, ((Colour >> 16) & 255) / 255.0f, ((Colour >> 8) & 255) / 255.0f, (Colour & 255) / 255.0f, Alpha);
        sprite(Graphics, Region, X, Y);
        tint(Graphics, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void lamp(GuiGraphics Graphics, int Colour, boolean On, int X, int Y) {
        Graphics.blit(Texture, X, Y, LampU + Colour * 10 + (On ? 5 : 0), LampV, 5, 5, TextureSize, TextureSize);
    }

    private void renderStatusLamps(GuiGraphics Graphics, float Time) {
        boolean Busy = menu.cardCount() > 0 || isPrinting();
        lamp(Graphics, 0, true, leftPos + StatusLamps[0][0], topPos + StatusLamps[0][1]);
        lamp(Graphics, 1, Busy && ((int) (Time / 6)) % 2 == 0, leftPos + StatusLamps[1][0], topPos + StatusLamps[1][1]);
        lamp(Graphics, 2, Ticks - ErrorAt < ErrorTicks && ((int) (Time / 3)) % 2 == 0, leftPos + StatusLamps[2][0], topPos + StatusLamps[2][1]);
    }

    private void glowText(GuiGraphics Graphics, String Line, int X, int Y, int Colour) {
        int Halo = (Colour & 0x00FFFFFF) | 0x2C000000;
        Graphics.drawString(font, Line, X - 1, Y, Halo, false);
        Graphics.drawString(font, Line, X + 1, Y, Halo, false);
        Graphics.drawString(font, Line, X, Y - 1, Halo, false);
        Graphics.drawString(font, Line, X, Y + 1, Halo, false);
        Graphics.drawString(font, Line, X, Y, Colour, false);
    }

    private void glowText(GuiGraphics Graphics, FormattedCharSequence Line, int X, int Y, int Colour) {
        int Halo = (Colour & 0x00FFFFFF) | 0x2C000000;
        Graphics.drawString(font, Line, X - 1, Y, Halo, false);
        Graphics.drawString(font, Line, X + 1, Y, Halo, false);
        Graphics.drawString(font, Line, X, Y - 1, Halo, false);
        Graphics.drawString(font, Line, X, Y + 1, Halo, false);
        Graphics.drawString(font, Line, X, Y, Colour, false);
    }

    private void renderCrt(GuiGraphics Graphics, float Time) {
        int X0 = leftPos + GlassX0;
        int Y0 = topPos + GlassY0;
        int X1 = leftPos + GlassX1;
        int Y1 = topPos + GlassY1;
        Graphics.enableScissor(X0, Y0, X1, Y1);
        float Boot = Mth.clamp(Time / BootTicks, 0.0f, 1.0f);
        if (Boot < 1.0f) {
            Graphics.fill(X0, Y0, X1, Y1, 0xFF020604);
            int Middle = (Y0 + Y1) / 2;
            if (Boot < 0.5f) {
                int Half = (int) ((X1 - X0) / 2.0f * Boot * 2.0f);
                Graphics.fill((X0 + X1) / 2 - Half, Middle, (X0 + X1) / 2 + Half, Middle + 1, 0xFFE8FFE8);
            } else {
                int Half = (int) ((Y1 - Y0) / 2.0f * (Boot - 0.5f) * 2.0f);
                int Fade = (int) (200 * (1.0f - (Boot - 0.5f) * 2.0f));
                Graphics.fill(X0, Middle - Half, X1, Middle + Half + 1, (Fade << 24) | 0x9CF5B0);
            }
        } else {
            renderLog(Graphics, Time);
            renderOdds(Graphics, Time);
            int Flicker = (int) (7 + 5 * Math.sin(Time * 2.3) * Math.sin(Time * 0.71));
            Graphics.fill(X0, Y0, X1, Y1, Math.max(0, Flicker) << 24);
            float Band = (Time * 0.7f) % (GlassY1 - GlassY0 + 24) - 12;
            int BandY = Y0 + (int) Band;
            Graphics.fillGradient(X0, BandY, X1, BandY + 6, 0x009CF5B0, 0x109CF5B0);
            Graphics.fillGradient(X0, BandY + 6, X1, BandY + 12, 0x109CF5B0, 0x009CF5B0);
        }
        renderScanlines(Graphics, X0, Y0, X1, Y1);
        Graphics.disableScissor();
    }

    private void renderScanlines(GuiGraphics Graphics, int X0, int Y0, int X1, int Y1) {
        double Scale = minecraft == null ? 1.0 : minecraft.getWindow().getGuiScale();
        if (Scale < 2.0) {
            for (int Y = Y0; Y < Y1; Y += 2) {
                Graphics.fill(X0, Y, X1, Y + 1, 0x16000000);
            }
            return;
        }
        int Step = Scale >= 3.0 ? 3 : 2;
        Graphics.pose().pushPose();
        Graphics.pose().scale((float) (1.0 / Scale), (float) (1.0 / Scale), 1.0f);
        int Left = (int) Math.round(X0 * Scale);
        int Right = (int) Math.round(X1 * Scale);
        int Top = (int) Math.round(Y0 * Scale);
        int Bottom = (int) Math.round(Y1 * Scale);
        for (int Row = Top + Step - 1; Row < Bottom; Row += Step) {
            Graphics.fill(Left, Row, Right, Row + 1, 0x38000000);
        }
        Graphics.pose().popPose();
    }

    private void renderLog(GuiGraphics Graphics, float Time) {
        List<TerminalLog.Entry> Lines = Log.visible();
        int Last = Lines.size() - 1;
        for (int Row = 0; Row < Lines.size(); Row++) {
            TerminalLog.Entry Line = Lines.get(Row);
            String Shown = font.plainSubstrByWidth(Line.Text(), LogWidth);
            int Y = topPos + LogY + Row * LogPitch;
            glowText(Graphics, Shown, leftPos + LogX, Y, Line.Colour());
            if (Row == Last && ((int) (Time / 8)) % 2 == 0) {
                int CursorX = leftPos + LogX + font.width(Shown) + 1;
                if (CursorX + 5 <= leftPos + LogX + LogWidth) {
                    Graphics.fill(CursorX, Y, CursorX + 5, Y + 8, PhosphorText);
                }
            }
        }
    }

    private int oddsTier() {
        int Tier = fragmentTier();
        if (Tier > 0) {
            return Tier;
        }
        return menu.cardCount() > 0 ? menu.rollTier() : 0;
    }

    private void renderOdds(GuiGraphics Graphics, float Time) {
        String Title = text("gui.scp_unleashed.archive.odds");
        glowText(Graphics, Title, leftPos + OddsX + OddsWidth / 2 - font.width(Title) / 2, topPos + LogY, PhosphorDim);
        if (poolBlocker() != null && menu.cardCount() == 0 && !isPrinting()) {
            String Key = AnomalyDocuments.client().isEmpty() ? "gui.scp_unleashed.archive.odds.no_records" : "gui.scp_unleashed.archive.odds.claimed";
            List<FormattedCharSequence> Lines = font.split(Component.translatable(Key), OddsWidth);
            int Colour = ((int) (Time / 10)) % 2 == 0 ? AlertAmber : AlertAmberDim;
            int Top = topPos + (LogY + 10 + OddsBottom + 10) / 2 - Lines.size() * 10 / 2;
            for (int Row = 0; Row < Lines.size(); Row++) {
                FormattedCharSequence Line = Lines.get(Row);
                glowText(Graphics, Line, leftPos + OddsX + OddsWidth / 2 - font.width(Line) / 2, Top + Row * 10, Colour);
            }
            return;
        }
        int Tier = oddsTier();
        double[] Shares = Tier > 0 ? DocumentRoller.odds(Tier, menu.tierMask()) : new double[DocumentRoller.Tiers];
        double Highest = 0.0;
        for (double Share : Shares) {
            Highest = Math.max(Highest, Share);
        }
        for (int Index = 0; Index < DocumentRoller.Tiers; Index++) {
            int X = leftPos + OddsX + Index * OddsPitch;
            int Filled = Highest > 0.0 ? (int) Math.round(Shares[Index] / Highest * OddsHeight) : 0;
            for (int Segment = 0; Segment < OddsHeight; Segment += 3) {
                int Y = topPos + OddsBottom - Segment - 2;
                int Colour = Segment < Filled ? (Segment + 3 >= Filled ? PhosphorHot : PhosphorText) : PhosphorFaint;
                Graphics.fill(X, Y, X + OddsBarWidth, Y + 2, Colour);
            }
            String Label = Numerals[Index];
            Graphics.drawString(font, Label, X + OddsBarWidth / 2 - font.width(Label) / 2 + 1, topPos + OddsBottom + 2, Shares[Index] > 0.0 ? PhosphorText : PhosphorDim, false);
        }
    }

    private boolean purgeActive(float Time) {
        return PrintStart != Never && Time - PrintStart < purgeEnd();
    }

    private float purgeEnd() {
        int Purged = PurgeCount - (PrintedCard >= 0 && PrintedCard < PurgeCount ? 1 : 0);
        float Shredding = Purged <= 0 ? 0.0f : ShredDelay + (Purged - 1) * ShredStagger + releaseTime() + StripExit;
        return Math.max(ReadDelay + ReadTicks, Shredding);
    }

    private void renderTray(GuiGraphics Graphics, float Time) {
        if (purgeActive(Time)) {
            renderPurge(Graphics, Time);
            return;
        }
        int Count = menu.cardCount();
        if (isPrinting()) {
            return;
        }
        if (Count == 0) {
            boolean Empty = menu.tierMask() == 0 && AnomalyDocuments.client().isEmpty();
            String Hint = text(Empty ? "gui.scp_unleashed.archive.tray_no_records" : "gui.scp_unleashed.archive.tray_empty");
            Graphics.drawString(font, Hint, leftPos + (FeltX0 + FeltX1) / 2 - font.width(Hint) / 2, topPos + TrayHintY, FeltText, false);
            return;
        }
        Graphics.enableScissor(leftPos + FeltX0, topPos + FeltY0, leftPos + FeltX1, topPos + LipY);
        for (int Index = 0; Index < Count; Index++) {
            drawTrayCard(Graphics, Index, Time);
        }
        Graphics.disableScissor();
        if (HoveredCard >= 0) {
            sprite(Graphics, CardGlow, leftPos + CardLeft + HoveredCard * CardPitch - 2, topPos + CardY - HoverLift - 2);
        }
        if (DealStart != Never) {
            for (int Index = 0; Index < Count; Index++) {
                float Appear = (Time - (DealStart + Index * DealStep)) / DealSlide;
                if (Appear > 0.0f && Appear < 1.0f) {
                    rollers(Graphics, CardLeft + Index * CardPitch, CardLeft + Index * CardPitch + CardWidth, Time);
                }
            }
        }
    }

    private void drawTrayCard(GuiGraphics Graphics, int Index, float Time) {
        int Code = menu.cardCode(Index);
        float X = leftPos + CardLeft + Index * CardPitch;
        float Y = topPos + CardY;
        float StampScale = 1.0f;
        boolean Stamped = true;
        if (DealStart != Never) {
            float Appear = Mth.clamp((Time - (DealStart + Index * DealStep)) / DealSlide, 0.0f, 1.0f);
            if (Appear <= 0.0f) {
                return;
            }
            float Eased = 1.0f - (1.0f - Appear) * (1.0f - Appear) * (1.0f - Appear);
            Y -= Math.round((CardHeight + 1) * (1.0f - Eased));
            float Since = Time - stampTick(Index);
            Stamped = Since >= 0.0f;
            StampScale = 1.0f + 1.4f * (1.0f - Mth.clamp(Since / StampSlam, 0.0f, 1.0f));
        }
        if (Index == HoveredCard) {
            Y -= HoverLift;
        }
        drawCard(Graphics, Index, Code, X, Y, Stamped, StampScale, false, 1.0f, true);
    }

    private void rollers(GuiGraphics Graphics, int X0, int X1, float Time) {
        int Phase = (int) (Time * 3.0f) % 4;
        for (int Px = Math.max(X0, FeltX0 + 3); Px < Math.min(X1, FeltX1 - 3); Px++) {
            if ((Px + Phase) % 4 == 0) {
                Graphics.fill(leftPos + Px, topPos + ReaderY0 + 1, leftPos + Px + 1, topPos + ReaderY0 + 2, RollerColour);
            }
        }
    }

    public static int classIconU(DocumentClass Classification) {
        return switch (Classification) {
            case Unknown -> UnknownIconU;
            case ZClass -> ZClassIconU;
            default -> IconU + Classification.ordinal() * 16;
        };
    }

    private void drawCard(GuiGraphics Graphics, int Index, int Code, float X, float Y, boolean Stamped, float StampScale, boolean Purged, float PurgeScale, boolean Shadow) {
        boolean Void = Code == ArchiveBlockEntity.VoidCard;
        DocumentClass Classification = Code > 0 && !Void ? DocumentClass.byId(Code - 1) : null;
        Graphics.pose().pushPose();
        Graphics.pose().translate(X, Y, 0.0f);
        tint(Graphics, 1.0f, 1.0f, 1.0f, 1.0f);
        if (Shadow) {
            sprite(Graphics, CardShadow, -2, -1);
        }
        sprite(Graphics, Void ? CardVoid : CardBase, 0, 0);
        if (Classification != null && Stamped) {
            tinted(Graphics, TabMask, 3, 0, Classification.getColour(), 1.0f);
        }
        String Label = text("gui.scp_unleashed.archive.file", String.format(Locale.ROOT, "%02d", Index + 1));
        Graphics.drawString(font, Label, CardWidth / 2 - font.width(Label) / 2 + 1, 7, InkColour, false);
        if (Classification != null && Stamped) {
            Graphics.blit(Texture, CardWidth / 2 - 8, 19, classIconU(Classification), IconV, 16, 16, TextureSize, TextureSize);
            stamp(Graphics, Classification.getName().getString().toUpperCase(Locale.ROOT), Classification.getInk(), 1, 35, StampScale);
        }
        if (Void) {
            stamp(Graphics, text("gui.scp_unleashed.archive.expunged"), VoidInk, 1, 35, 1.0f);
        }
        if (Purged) {
            stamp(Graphics, text("gui.scp_unleashed.archive.purged"), PurgeInk, 1, 21, PurgeScale);
        }
        Graphics.pose().popPose();
    }

    private void drawCardBack(GuiGraphics Graphics, int Code, float X, float Y) {
        Graphics.pose().pushPose();
        Graphics.pose().translate(X, Y, 0.0f);
        tint(Graphics, BackShade, BackShade * 0.97f, BackShade * 0.92f, 1.0f);
        sprite(Graphics, Code == ArchiveBlockEntity.VoidCard ? CardVoid : CardBase, 0, 0);
        tint(Graphics, 1.0f, 1.0f, 1.0f, 1.0f);
        Graphics.pose().popPose();
    }

    private void stamp(GuiGraphics Graphics, String Word, int Ink, int X, int Y, float Scale) {
        float Shown = Mth.clamp(2.4f - Scale, 0.0f, 1.0f);
        if (Shown <= 0.05f) {
            return;
        }
        Graphics.pose().pushPose();
        float CentreX = X + StampWidth / 2.0f;
        float CentreY = Y + 7.0f;
        Graphics.pose().translate(CentreX, CentreY, 0.0f);
        Graphics.pose().scale(Scale, Scale, 1.0f);
        Graphics.pose().translate(-CentreX, -CentreY, 0.0f);
        tinted(Graphics, StampFrame, X, Y, Ink, Shown);
        int Width = font.width(Word);
        int Room = StampWidth - 4;
        int Colour = (Ink & 0xFFFFFF) | ((int) (Shown * 255) << 24);
        if (Width > Room) {
            float Fit = (float) Room / Width;
            Graphics.pose().pushPose();
            Graphics.pose().translate(CentreX, CentreY, 0.0f);
            Graphics.pose().scale(Fit, Fit, 1.0f);
            Graphics.drawString(font, Word, -Width / 2, -4, Colour, false);
            Graphics.pose().popPose();
        } else {
            Graphics.drawString(font, Word, (int) CentreX - Width / 2 + 1, Y + 3, Colour, false);
        }
        tint(Graphics, 1.0f, 1.0f, 1.0f, Shown);
        sprite(Graphics, StampDistress, X, Y);
        tint(Graphics, 1.0f, 1.0f, 1.0f, 1.0f);
        Graphics.pose().popPose();
    }

    private static float unit(int Seed) {
        int Hash = Seed * 0x9E3779B1;
        Hash ^= Hash >>> 16;
        Hash *= 0x85EBCA6B;
        Hash ^= Hash >>> 13;
        Hash *= 0xC2B2AE35;
        Hash ^= Hash >>> 16;
        return (Hash >>> 8) / 16777216.0f;
    }

    private int stripSeed(int Card, int Strip, int Salt) {
        return PrintSerial * 977 + Card * 61 + Strip * 7 + Salt * 7919;
    }

    private static float feedOffset(float U) {
        if (U <= 0.0f) {
            return 0.0f;
        }
        if (U < GrabTicks) {
            return U >= GrabTicks * 0.5f ? 1.0f : 0.0f;
        }
        float V = U - GrabTicks;
        if (V < FeedAccel) {
            return 1.0f + 0.5f * FeedSpeed / FeedAccel * V * V;
        }
        return 1.0f + 0.5f * FeedSpeed * FeedAccel + FeedSpeed * (V - FeedAccel);
    }

    private static float releaseTime() {
        return GrabTicks + FeedAccel + (FeedTravel - 1.0f - 0.5f * FeedSpeed * FeedAccel) / FeedSpeed;
    }

    private static int stripDrift(int Card, int Strip, float Depth, float Time) {
        float Centre = Strip - (StripCount - 1) / 2.0f;
        float Flare = Centre * StripFlare * Depth;
        float Flutter = Mth.sin(Time * 1.7f + Strip * 2.1f + Card) * StripFlutter * Depth;
        return Math.round(Flare + Flutter);
    }

    private float stripFall(int Card, int Strip, float Since) {
        float Speed = FeedSpeed * (0.8f + unit(stripSeed(Card, Strip, 2)) * 0.35f);
        float Gravity = 0.35f + unit(stripSeed(Card, Strip, 3)) * 0.4f;
        float Delay = unit(stripSeed(Card, Strip, 4)) * 1.5f;
        float Moving = Math.max(0.0f, Since - Delay);
        return FeedSpeed * Math.min(Since, Delay) + Speed * Moving + 0.5f * Gravity * Moving * Moving;
    }

    private float stripTwist(int Card, int Strip, float Since) {
        float Rate = (0.3f + unit(stripSeed(Card, Strip, 5)) * 0.4f) * (unit(stripSeed(Card, Strip, 6)) < 0.5f ? 1.0f : -1.0f);
        return Mth.cos(Rate * Math.max(0.0f, Since));
    }

    private void renderPurge(GuiGraphics Graphics, float Time) {
        float Since = Time - PrintStart;
        float Release = releaseTime();
        int Order = 0;
        for (int Index = 0; Index < PurgeCount; Index++) {
            if (Index == PrintedCard) {
                continue;
            }
            renderShred(Graphics, Index, Codes[Index], Since - (ShredDelay + Order * ShredStagger), Since, Time, Release);
            Order++;
        }
        renderRead(Graphics, Since, Time);
        Shade = 1.0f;
        Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void renderRead(GuiGraphics Graphics, float Since, float Time) {
        if (PrintedCard < 0 || PrintedCard >= Codes.length) {
            return;
        }
        float Progress = Mth.clamp((Since - ReadDelay) / ReadTicks, 0.0f, 1.0f);
        if (Progress >= 1.0f) {
            return;
        }
        int Lift = Math.round((CardHeight + 1) * Progress * Progress);
        int X = CardLeft + PrintedCard * CardPitch;
        Graphics.enableScissor(leftPos + FeltX0, topPos + FeltY0, leftPos + FeltX1, topPos + LipY);
        drawCard(Graphics, PrintedCard, Codes[PrintedCard], leftPos + X, topPos + CardY - Lift, true, 1.0f, false, 1.0f, true);
        Graphics.disableScissor();
        if (Progress > 0.0f) {
            rollers(Graphics, X, X + CardWidth, Time);
        }
    }

    private void renderShred(GuiGraphics Graphics, int Index, int Code, float U, float Since, float Time, float Release) {
        float PurgeScale = 1.0f + 1.4f * (1.0f - Mth.clamp(Since / PurgeStamp, 0.0f, 1.0f));
        int X = CardLeft + Index * CardPitch;
        int Offset = Mth.floor(feedOffset(U));
        boolean Feeding = U >= GrabTicks * 0.5f && U < Release;
        if (U < Release) {
            int Jitter = Feeding ? ((int) Time) % 2 : 0;
            Graphics.enableScissor(leftPos + FeltX0, topPos + FeltY0, leftPos + FeltX1, topPos + LipY);
            drawCard(Graphics, Index, Code, leftPos + X + Jitter, topPos + CardY + Offset, true, 1.0f, true, PurgeScale, true);
            Graphics.disableScissor();
        }
        if (Feeding) {
            int Phase = (int) (Time * 2.0f) % 3;
            for (int Px = X; Px < X + CardWidth; Px++) {
                if ((Px + Phase) % 3 == 0) {
                    Graphics.fill(leftPos + Px, topPos + MouthY, leftPos + Px + 1, topPos + MouthY + 1, ToothColour);
                }
            }
        }
        if (U <= 0.0f) {
            return;
        }
        float Fallen = U - Release;
        int Top = CardY + Offset;
        for (int Strip = 0; Strip < StripCount; Strip++) {
            int Column = Strip * StripPitch;
            int StripTop = Mth.floor(Fallen < 0.0f ? Top : ThroatY0 + stripFall(Index, Strip, Fallen));
            if (StripTop + CardHeight <= ThroatY0 || StripTop >= ThroatY1) {
                continue;
            }
            float Twist = Fallen < 0.0f ? 1.0f : stripTwist(Index, Strip, Fallen);
            float Width = StripWidth * Math.abs(Twist);
            for (int Band = 0; Band < StripBands; Band++) {
                int Y0 = ThroatY0 + Band * (ThroatY1 - ThroatY0) / StripBands;
                int Y1 = ThroatY0 + (Band + 1) * (ThroatY1 - ThroatY0) / StripBands;
                float Depth = (Band + 0.5f) / StripBands;
                float Centre = X + Column + StripWidth / 2.0f + stripDrift(Index, Strip, Depth, Time);
                int ClipX0 = Math.max(ThroatX0, Mth.ceil(Centre - Width / 2.0f - 0.01f));
                int ClipX1 = Math.min(ThroatX1, Mth.floor(Centre + Width / 2.0f + 0.01f));
                if (ClipX1 <= ClipX0) {
                    continue;
                }
                Shade = 1.0f - StripShade * Depth;
                Graphics.enableScissor(leftPos + ClipX0, topPos + Y0, leftPos + ClipX1, topPos + Y1);
                Graphics.pose().pushPose();
                Graphics.pose().translate(leftPos + Centre, topPos + StripTop, 0.0f);
                Graphics.pose().scale(Math.max(0.01f, Math.abs(Twist)), 1.0f, 1.0f);
                if (Twist >= 0.0f) {
                    drawCard(Graphics, Index, Code, -(Column + StripWidth / 2.0f), 0.0f, true, 1.0f, true, 1.0f, false);
                } else {
                    drawCardBack(Graphics, Code, -(Column + StripWidth / 2.0f), 0.0f);
                }
                Graphics.pose().popPose();
                Graphics.disableScissor();
            }
        }
        Shade = 1.0f;
        Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        if (Feeding || (Fallen >= 0.0f && Fallen < DustLife)) {
            Graphics.enableScissor(leftPos + ThroatX0, topPos + ThroatY0, leftPos + ThroatX1, topPos + ThroatY1);
            for (int Speck = 0; Speck < DustCount; Speck++) {
                float Born = GrabTicks + Speck * (Release - GrabTicks) / DustCount;
                float Age = U - Born;
                if (Age < 0.0f || Age > DustLife) {
                    continue;
                }
                int Seed = stripSeed(Index, Speck, 20);
                int Px = Math.round(X + 2 + unit(Seed) * (CardWidth - 4) + (unit(Seed + 1) - 0.5f) * 2.0f * Age);
                int Py = Math.round(ThroatY0 + (1.5f + unit(Seed + 2) * 2.0f) * Age);
                Graphics.fill(leftPos + Px, topPos + Py, leftPos + Px + 1, topPos + Py + 1, unit(Seed + 3) < 0.6f ? DustLight : DustWhite);
            }
            Graphics.disableScissor();
        }
    }

    private void renderIntake(GuiGraphics Graphics, float Time, int MouseX, int MouseY) {
        if (fragmentTier() > 0) {
            float Pulse = 0.55f + 0.35f * (float) Math.sin(Time * 0.25f);
            tinted(Graphics, FragmentGlow, leftPos + ArchiveMenu.FragmentX - 3, topPos + ArchiveMenu.FragmentY - 3, fragmentColour(), Pulse);
        }
        String Blocker = rollBlocker();
        boolean Pressed = Ticks - PressedAt < PressTicks;
        int State = Blocker != null && !Pressed ? 3 : Pressed ? 2 : overRoll(MouseX, MouseY) ? 1 : 0;
        Graphics.blit(Texture, leftPos + RollX, topPos + RollY, RollU + State * RollWidth, RollV, RollWidth, RollHeight, TextureSize, TextureSize);
        String Label = text("gui.scp_unleashed.archive.roll");
        int Colour = State == 3 ? 0xFF828284 : 0xFFFFECDC;
        Graphics.drawString(font, Label, leftPos + RollX + RollWidth / 2 - font.width(Label) / 2, topPos + RollY + 8 + (State == 2 ? 1 : 0), Colour, State != 3);
    }

    private static float headX(float Since) {
        float Park = HeadX0 + 2;
        if (Since < PassStart || Since >= FeedEnd) {
            return Park;
        }
        int Pass = (int) ((Since - PassStart) / PassTicks);
        float Within = (Since - PassStart - Pass * PassTicks) / PassTicks;
        float Move = Mth.clamp(Within / PassMove, 0.0f, 1.0f);
        Move = Move * Move * (3.0f - 2.0f * Move);
        return Pass % 2 == 0 ? Park + HeadTravel * Move : Park + HeadTravel * (1.0f - Move);
    }

    private static float paperFed(float Since) {
        if (Since < PassStart) {
            return 0.0f;
        }
        if (Since >= FeedEnd) {
            return SheetHeight;
        }
        int Pass = (int) ((Since - PassStart) / PassTicks);
        float Within = (Since - PassStart - Pass * PassTicks) / PassTicks;
        float Step = 0.0f;
        if (Within > PassMove) {
            Step = Mth.clamp((Within - PassMove) / (1.0f - PassMove), 0.0f, 1.0f);
            Step = Step * Step * (3.0f - 2.0f * Step);
        }
        return LineStep * (Pass + Step);
    }

    private void renderPrinter(GuiGraphics Graphics, float Time) {
        boolean Printing = isPrinting();
        float Since = Printing && PrintStart != Never ? Math.min(Time - PrintStart, ArchiveBlockEntity.PrintTicks - 0.01f) : -1.0f;
        int HeadX = Math.round(Since >= 0.0f ? headX(Since) : HeadX0 + 2);
        sprite(Graphics, PrintHead, leftPos + HeadX, topPos + HeadY0 + 1);
        lamp(Graphics, 1, Printing && ((int) (Time / 3)) % 2 == 0, leftPos + LampPrint[0], topPos + LampPrint[1]);
        lamp(Graphics, 0, !Printing && !menu.getOutput().isEmpty(), leftPos + LampReady[0], topPos + LampReady[1]);
        if (Since >= 0.0f) {
            renderSheet(Graphics, Since);
        } else if (!Printing && LandedAt != Never) {
            renderPuffs(Graphics, Time - LandedAt);
        }
    }

    private void renderSheet(GuiGraphics Graphics, float Since) {
        Graphics.enableScissor(leftPos + OutputClipX0, topPos + SlitBottom, leftPos + OutputClipX1, topPos + OutputClipY1);
        if (Since < DropStart) {
            int Rows = Math.min(SheetHeight, Mth.floor(paperFed(Since)));
            int Sag = Since >= FeedEnd + 1 ? 1 : 0;
            if (Rows > 0) {
                Graphics.blit(Texture, leftPos + SheetX, topPos + SlitBottom + Sag, PaperSheet[0], PaperSheet[1] + SheetHeight - Rows, SheetWidth, Rows, TextureSize, TextureSize);
            }
        } else {
            float Fall = Mth.clamp((Since - DropStart) / DropTicks, 0.0f, 1.0f);
            Fall *= Fall;
            int OutputX = ArchiveMenu.OutputX;
            int OutputY = ArchiveMenu.OutputY;
            Graphics.fill(leftPos + OutputX - 1, topPos + OutputY + 3, leftPos + OutputX + 17, topPos + OutputY + 17, ((int) (110 * Fall)) << 24);
            float StartX = SheetX + SheetWidth / 2.0f;
            float StartY = SlitBottom + 1 + SheetHeight / 2.0f;
            float CentreX = StartX + (OutputX + 8.0f - StartX) * Fall;
            float CentreY = StartY + (OutputY + 9.0f - StartY) * Fall;
            float Width = SheetWidth + (LandWidth - SheetWidth) * Fall;
            float Height = SheetHeight + (LandHeight - SheetHeight) * Fall;
            Graphics.pose().pushPose();
            Graphics.pose().translate(leftPos + CentreX - Width / 2.0f, topPos + CentreY - Height / 2.0f, 0.0f);
            Graphics.pose().scale(Width / SheetWidth, Height / SheetHeight, 1.0f);
            sprite(Graphics, PaperSheet, 0, 0);
            Graphics.pose().popPose();
        }
        Graphics.disableScissor();
    }

    private void renderPuffs(GuiGraphics Graphics, float Since) {
        if (Since < 0.0f || Since > PuffLife) {
            return;
        }
        int OutputX = ArchiveMenu.OutputX;
        int OutputY = ArchiveMenu.OutputY;
        Graphics.enableScissor(leftPos + OutputClipX0, topPos + SlitBottom, leftPos + OutputClipX1, topPos + OutputClipY1);
        for (int Puff = 0; Puff < PuffCount; Puff++) {
            int Side = Puff % 2 == 0 ? -1 : 1;
            int Seed = 4000 + Puff;
            int Px = Math.round(OutputX + 8 + Side * (7 + 0.9f * Since * (0.7f + unit(Seed) * 0.6f)));
            int Py = Math.round(OutputY + 14 - (0.6f + unit(Seed + 1) * 0.8f) * Since + 0.1f * Since * Since);
            Graphics.fill(leftPos + Px, topPos + Py, leftPos + Px + 1, topPos + Py + 1, PuffColour);
        }
        Graphics.disableScissor();
    }

    @Override
    protected void renderSlot(GuiGraphics Graphics, Slot Target) {
        if (Target != menu.getSlot(ArchiveBlockEntity.OutputSlot)) {
            super.renderSlot(Graphics, Target);
            return;
        }
        if (isPrinting()) {
            return;
        }
        boolean Arriving = WasPrinting;
        float Since = Arriving ? 0.0f : FrameTime - LandedAt;
        if (!Arriving && (LandedAt == Never || Since < 0.0f || Since >= BounceTicks)) {
            super.renderSlot(Graphics, Target);
            return;
        }
        float Lift = -BounceHeight * Math.abs(Mth.sin((float) Math.PI * Since / BouncePeriod)) * (float) Math.exp(-Since / BounceDecay);
        float Press = Squash * Math.max(0.0f, 1.0f - Since / SquashTicks);
        Graphics.pose().pushPose();
        Graphics.pose().translate(Target.x + 8.0f, Target.y + 16.0f + Lift, 0.0f);
        Graphics.pose().scale(1.0f + Press, 1.0f - Press, 1.0f);
        Graphics.pose().translate(-(Target.x + 8.0f), -(Target.y + 16.0f), 0.0f);
        super.renderSlot(Graphics, Target);
        Graphics.pose().popPose();
    }

    @Override
    protected void renderLabels(GuiGraphics Graphics, int MouseX, int MouseY) {
        Graphics.drawString(font, title, titleLabelX, titleLabelY + 1, TitleColour, false);
        Graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LabelColour, false);
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        FrameTime = Ticks + PartialTick;
        super.render(Graphics, MouseX, MouseY, PartialTick);
        renderTooltip(Graphics, MouseX, MouseY);
        if (!menu.getCarried().isEmpty()) {
            return;
        }
        List<Component> Lines = hoverLines(MouseX, MouseY);
        if (!Lines.isEmpty()) {
            Graphics.renderComponentTooltip(font, Lines, MouseX, MouseY);
        }
    }

    private List<Component> hoverLines(int MouseX, int MouseY) {
        List<Component> Lines = new ArrayList<>();
        int Card = cardAt(MouseX, MouseY);
        if (Card >= 0) {
            int Code = menu.cardCode(Card);
            if (Code == ArchiveBlockEntity.VoidCard) {
                Lines.add(Component.translatable("gui.scp_unleashed.archive.card.void").withStyle(ChatFormatting.DARK_GRAY));
            } else if (Code > 0 && ready()) {
                DocumentClass Classification = DocumentClass.byId(Code - 1);
                Lines.add(Component.translatable("gui.scp_unleashed.archive.card.title", Classification.getName()).withStyle(Look -> Look.withColor(Classification.getColour() & 0xFFFFFF)));
                Lines.add(Classification.getSummary().copy().withStyle(ChatFormatting.GRAY));
                Lines.add(Component.translatable(menu.getOutput().isEmpty() ? "gui.scp_unleashed.archive.card.print" : "gui.scp_unleashed.archive.error.output")
                        .withStyle(menu.getOutput().isEmpty() ? ChatFormatting.YELLOW : ChatFormatting.RED));
                Lines.add(Component.translatable("gui.scp_unleashed.archive.card.key", Card + 1).withStyle(ChatFormatting.DARK_GRAY));
            }
            return Lines;
        }
        if (overRoll(MouseX, MouseY)) {
            Lines.add(Component.translatable("gui.scp_unleashed.archive.roll.title").withStyle(ChatFormatting.WHITE));
            Lines.add(Component.translatable("gui.scp_unleashed.archive.roll.cost", ArchiveBlockEntity.PaperCost).withStyle(ChatFormatting.GRAY));
            String Blocker = rollBlocker();
            if (Blocker != null) {
                Lines.add(Component.translatable(Blocker, paperCount(), ArchiveBlockEntity.PaperCost).withStyle(ChatFormatting.RED));
            }
            Lines.add(Component.translatable("gui.scp_unleashed.archive.roll.key").withStyle(ChatFormatting.DARK_GRAY));
            return Lines;
        }
        int Tier = oddsTier();
        for (int Index = 0; Index < DocumentRoller.Tiers; Index++) {
            int X = leftPos + OddsX + Index * OddsPitch;
            if (MouseX >= X - 2 && MouseX < X + OddsBarWidth + 2 && MouseY >= topPos + OddsBottom - OddsHeight - 2 && MouseY < topPos + OddsBottom + 10) {
                Lines.add(Component.translatable("gui.scp_unleashed.archive.odds.tier", Numerals[Index]).withStyle(ChatFormatting.GREEN));
                if (Tier > 0) {
                    double Share = DocumentRoller.odds(Tier, menu.tierMask())[Index];
                    Lines.add(Component.translatable("gui.scp_unleashed.archive.odds.chance", String.format(Locale.ROOT, "%.0f", Share * 100.0)).withStyle(ChatFormatting.GRAY));
                } else {
                    Lines.add(Component.translatable("gui.scp_unleashed.archive.odds.none").withStyle(ChatFormatting.DARK_GRAY));
                }
                Lines.add(Component.translatable("gui.scp_unleashed.archive.odds.hint." + (Index + 1)).withStyle(ChatFormatting.DARK_GRAY));
                if ((menu.tierMask() & (1 << Index)) == 0) {
                    Lines.add(Component.translatable("gui.scp_unleashed.archive.odds.exhausted").withStyle(ChatFormatting.RED));
                }
                return Lines;
            }
        }
        if (isHovering(ArchiveMenu.FragmentX, ArchiveMenu.FragmentY, 16, 16, MouseX, MouseY) && menu.getFragment().isEmpty()) {
            Lines.add(Component.translatable("gui.scp_unleashed.archive.slot.fragment").withStyle(ChatFormatting.WHITE));
            Lines.add(Component.translatable("gui.scp_unleashed.archive.slot.fragment.hint").withStyle(ChatFormatting.GRAY));
        } else if (isHovering(ArchiveMenu.PaperX, ArchiveMenu.PaperY, 16, 16, MouseX, MouseY) && menu.getPaper().isEmpty()) {
            Lines.add(Component.translatable("gui.scp_unleashed.archive.slot.paper").withStyle(ChatFormatting.WHITE));
            Lines.add(Component.translatable("gui.scp_unleashed.archive.slot.paper.hint", ArchiveBlockEntity.PaperCost).withStyle(ChatFormatting.GRAY));
        } else if (isHovering(ArchiveMenu.OutputX, ArchiveMenu.OutputY, 16, 16, MouseX, MouseY) && menu.getOutput().isEmpty()) {
            Lines.add(Component.translatable("gui.scp_unleashed.archive.slot.output").withStyle(ChatFormatting.WHITE));
        } else {
            String[] Keys = {"gui.scp_unleashed.archive.lamp.power", "gui.scp_unleashed.archive.lamp.busy", "gui.scp_unleashed.archive.lamp.alert"};
            for (int Index = 0; Index < StatusLamps.length; Index++) {
                if (isHovering(StatusLamps[Index][0] - 1, StatusLamps[Index][1] - 1, 7, 7, MouseX, MouseY)) {
                    Lines.add(Component.translatable(Keys[Index]));
                }
            }
        }
        return Lines;
    }
}
