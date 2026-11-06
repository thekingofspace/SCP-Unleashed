package net.rai.scp_unleashed.nethub.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.nethub.NetHubPortPayload;
import net.rai.scp_unleashed.nethub.NetworkHubBlockEntity;
import net.rai.scp_unleashed.registry.ModSounds;
import net.rai.scp_unleashed.wire.WireChannels;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NetworkHubScreen extends Screen {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/nethub/network_hub.png");
    public static final int TextureWidth = 512;
    public static final int TextureHeight = 256;
    public static final int PanelWidth = 248;
    public static final int PanelHeight = 200;
    public static final int[] Places = {1000, 100, 10, 1};
    public static final int ColumnPitch = 22;
    public static final int DigitX = 22;
    public static final int DigitY = 29;
    public static final int DigitWidth = 12;
    public static final int DigitHeight = 18;
    public static final int DigitU = 256;
    public static final int GhostU = 376;
    public static final int ButtonX = 18;
    public static final int ButtonY = 58;
    public static final int ButtonWidth = 20;
    public static final int ButtonHeight = 16;
    public static final int ButtonV = 20;
    public static final int ButtonNormalU = 256;
    public static final int ButtonHoverU = 276;
    public static final int ButtonPressedU = 296;
    public static final int LampSize = 6;
    public static final int LampV = 20;
    public static final int LampGreenU = 316;
    public static final int LampAmberU = 322;
    public static final int LampRedU = 328;
    public static final int LampOffU = 334;
    public static final int TitleY = 6;
    public static final int StatusLampX = 119;
    public static final int StatusTextX = 129;
    public static final int StatusTop = 28;
    public static final int StatusPitch = 12;
    public static final int HeaderY = 85;
    public static final int ListLeft = 8;
    public static final int ListTop = 96;
    public static final int ListRight = 240;
    public static final int ListBottom = 188;
    public static final int RowHeight = 22;
    public static final int VisibleRows = 4;
    public static final int RowLampX = 14;
    public static final int RowTextX = 24;
    public static final int RowRight = 234;
    public static final int TitleColour = 0xFFE0E0E0;
    public static final int StatusColour = 0xFFC8C8C8;
    public static final int DimColour = 0xFF8A9096;
    public static final int LinkedColour = 0xFF7CE08A;
    public static final int UnloadedColour = 0xFFE8B04A;
    public static final int NoPowerColour = 0xFFD06A58;
    public static final int HeaderColour = 0xFFD8B866;

    private final BlockPos Pos;
    private int Port;
    private int Pressed = -1;
    private int Scroll;
    private int Left;
    private int Top;

    public NetworkHubScreen(BlockPos Pos) {
        super(Component.translatable("block.scp_unleashed.network_hub"));
        this.Pos = Pos;
    }

    @Override
    protected void init() {
        super.init();
        Left = (width - PanelWidth) / 2;
        Top = (height - PanelHeight) / 2;
        NetworkHubBlockEntity Hub = hub();
        if (Hub != null) {
            Port = Hub.getPort();
        }
    }

    private NetworkHubBlockEntity hub() {
        if (minecraft == null || minecraft.level == null) {
            return null;
        }
        return minecraft.level.getBlockEntity(Pos) instanceof NetworkHubBlockEntity Hub ? Hub : null;
    }

    @Override
    public void tick() {
        super.tick();
        NetworkHubBlockEntity Hub = hub();
        if (Hub == null || minecraft.player == null || !NetHubPortPayload.nearEnough(minecraft.player.position(), Hub.getCentre())) {
            onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        super.render(Graphics, MouseX, MouseY, PartialTick);
        NetworkHubBlockEntity Hub = hub();
        if (Hub == null) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Graphics.blit(Texture, Left, Top, 0, 0, PanelWidth, PanelHeight, TextureWidth, TextureHeight);
        int Hovered = buttonAt(MouseX, MouseY);
        for (int Index = 0; Index < Places.length; Index++) {
            int Column = Index * ColumnPitch;
            Graphics.blit(Texture, Left + DigitX + Column, Top + DigitY, GhostU, 0, DigitWidth, DigitHeight, TextureWidth, TextureHeight);
            Graphics.blit(Texture, Left + DigitX + Column, Top + DigitY, DigitU + digit(Index) * DigitWidth, 0, DigitWidth, DigitHeight, TextureWidth, TextureHeight);
            int ButtonU = Pressed == Index ? ButtonPressedU : Hovered == Index ? ButtonHoverU : ButtonNormalU;
            Graphics.blit(Texture, Left + ButtonX + Column, Top + ButtonY, ButtonU, ButtonV, ButtonWidth, ButtonHeight, TextureWidth, TextureHeight);
        }
        boolean Powered = Hub.isPowered();
        boolean Bridging = Hub.isBridging();
        int Linked = Hub.linkedCount();
        lamp(Graphics, Left + StatusLampX, Top + StatusTop, Powered ? LampGreenU : LampRedU);
        text(Graphics, Powered ? Component.translatable("gui.scp_unleashed.network_hub.power", NetworkHubBlockEntity.Draw)
                : Component.translatable("gui.scp_unleashed.network_hub.no_power"), StatusTextX, StatusTop - 1, StatusColour);
        lamp(Graphics, Left + StatusLampX, Top + StatusTop + StatusPitch, Bridging ? LampGreenU : Powered ? LampAmberU : LampOffU);
        text(Graphics, Bridging ? Component.translatable("gui.scp_unleashed.network_hub.bridging", Linked)
                : Powered ? Component.translatable("gui.scp_unleashed.network_hub.standby") : Component.translatable("gui.scp_unleashed.network_hub.offline"),
                StatusTextX, StatusTop + StatusPitch - 1, StatusColour);
        text(Graphics, Component.translatable("gui.scp_unleashed.network_hub.local", Hub.getNetworkName()), StatusLampX, StatusTop + StatusPitch * 2 - 1, StatusColour);
        text(Graphics, Component.translatable("gui.scp_unleashed.network_hub.devices", Hub.getDeviceCount(), dimensionName(Hub.getDimensionName())),
                StatusLampX, StatusTop + StatusPitch * 3 - 1, DimColour);
        RenderSystem.disableBlend();
        Graphics.drawString(font, title, Left + (PanelWidth - font.width(title)) / 2, Top + TitleY, TitleColour, false);
        Component Header = Component.translatable("gui.scp_unleashed.network_hub.bridged");
        text(Graphics, Header, 10, HeaderY, HeaderColour);
        Component Traffic = Component.translatable("gui.scp_unleashed.network_hub.traffic", Hub.getRelayed(), Hub.getDelivered());
        Graphics.drawString(font, Traffic, Left + ListRight - 2 - font.width(Traffic), Top + HeaderY, DimColour, false);
        List<NetworkHubBlockEntity.FarView> Views = Hub.getViews();
        Scroll = Mth.clamp(Scroll, 0, Math.max(0, Views.size() - VisibleRows));
        if (Views.isEmpty()) {
            Component Empty = Component.translatable("gui.scp_unleashed.network_hub.none", String.format(Locale.ROOT, "%04d", Port));
            Graphics.drawString(font, Empty, Left + (ListLeft + ListRight - font.width(Empty)) / 2, Top + (ListTop + ListBottom) / 2 - 4, DimColour, false);
        }
        int HoveredRow = -1;
        for (int Row = 0; Row < VisibleRows && Scroll + Row < Views.size(); Row++) {
            NetworkHubBlockEntity.FarView View = Views.get(Scroll + Row);
            int Y = Top + ListTop + 3 + Row * RowHeight;
            if (MouseX >= Left + ListLeft && MouseX < Left + ListRight && MouseY >= Y - 1 && MouseY < Y + RowHeight - 1) {
                HoveredRow = Scroll + Row;
            }
            RenderSystem.enableBlend();
            lamp(Graphics, Left + RowLampX, Y + 1, View.Kind() == NetworkHubBlockEntity.ViewLinked ? LampGreenU : View.Kind() == NetworkHubBlockEntity.ViewUnloaded ? LampAmberU : LampRedU);
            RenderSystem.disableBlend();
            Graphics.drawString(font, View.Name().isEmpty() ? NetworkHubBlockEntity.NoNetwork : View.Name(), Left + RowTextX, Top + ListTop + 3 + Row * RowHeight, StatusColour, false);
            Component State = stateOf(View);
            Graphics.drawString(font, State, Left + RowRight - font.width(State), Y, colourOf(View), false);
            Component Where = Component.translatable("gui.scp_unleashed.network_hub.where", dimensionName(View.Dimension()), View.Pos().getX(), View.Pos().getY(), View.Pos().getZ());
            Graphics.drawString(font, Where, Left + RowTextX, Y + 10, DimColour, false);
        }
        if (Views.size() > VisibleRows) {
            Component More = Component.translatable("gui.scp_unleashed.network_hub.more", Scroll + 1, Math.min(Views.size(), Scroll + VisibleRows), Views.size());
            Graphics.drawString(font, More, Left + 10 + font.width(Header) + 6, Top + HeaderY, DimColour, false);
        }
        if (Hovered >= 0 && Pressed < 0) {
            Graphics.renderTooltip(font, Component.translatable("gui.scp_unleashed.network_hub.hint"), MouseX, MouseY);
        } else if (HoveredRow >= 0) {
            Graphics.renderComponentTooltip(font, rowTooltip(Views.get(HoveredRow)), MouseX, MouseY);
        }
    }

    private List<Component> rowTooltip(NetworkHubBlockEntity.FarView View) {
        List<Component> Lines = new ArrayList<>();
        Lines.add(Component.literal(View.Name().isEmpty() ? NetworkHubBlockEntity.NoNetwork : View.Name()));
        Lines.add(Component.translatable("gui.scp_unleashed.network_hub.tip_devices", View.Devices()));
        Lines.add(Component.translatable(switch (View.Kind()) {
            case NetworkHubBlockEntity.ViewLinked -> "gui.scp_unleashed.network_hub.tip_linked";
            case NetworkHubBlockEntity.ViewUnloaded -> "gui.scp_unleashed.network_hub.tip_unloaded";
            default -> "gui.scp_unleashed.network_hub.tip_no_power";
        }));
        if (View.Missed() > 0) {
            Lines.add(Component.translatable("gui.scp_unleashed.network_hub.tip_missed", View.Missed()));
        }
        return Lines;
    }

    private static Component stateOf(NetworkHubBlockEntity.FarView View) {
        return switch (View.Kind()) {
            case NetworkHubBlockEntity.ViewLinked -> Component.translatable("gui.scp_unleashed.network_hub.state_linked");
            case NetworkHubBlockEntity.ViewUnloaded -> View.Missed() > 0 ? Component.translatable("gui.scp_unleashed.network_hub.state_unloaded_missed", View.Missed())
                    : Component.translatable("gui.scp_unleashed.network_hub.state_unloaded");
            default -> Component.translatable("gui.scp_unleashed.network_hub.state_no_power");
        };
    }

    private static int colourOf(NetworkHubBlockEntity.FarView View) {
        return switch (View.Kind()) {
            case NetworkHubBlockEntity.ViewLinked -> LinkedColour;
            case NetworkHubBlockEntity.ViewUnloaded -> UnloadedColour;
            default -> NoPowerColour;
        };
    }

    public static String dimensionName(String Id) {
        ResourceLocation Key = ResourceLocation.tryParse(Id);
        if (Key == null) {
            return Id;
        }
        String LangKey = "gui.scp_unleashed.network_hub.dimension." + Key.getNamespace() + "." + Key.getPath();
        if (I18n.exists(LangKey)) {
            return I18n.get(LangKey);
        }
        StringBuilder Built = new StringBuilder();
        for (String Word : Key.getPath().replace('/', '_').split("_")) {
            if (Word.isEmpty()) {
                continue;
            }
            if (!Built.isEmpty()) {
                Built.append(' ');
            }
            Built.append(Character.toUpperCase(Word.charAt(0))).append(Word.substring(1));
        }
        return Built.toString();
    }

    private void lamp(GuiGraphics Graphics, int X, int Y, int U) {
        Graphics.blit(Texture, X, Y, U, LampV, LampSize, LampSize, TextureWidth, TextureHeight);
    }

    private void text(GuiGraphics Graphics, Component Text, int X, int Y, int Colour) {
        Graphics.drawString(font, Text, Left + X, Top + Y, Colour, false);
    }

    @Override
    public boolean mouseClicked(double MouseX, double MouseY, int Button) {
        int Index = buttonAt(MouseX, MouseY);
        if (Index >= 0 && (Button == GLFW.GLFW_MOUSE_BUTTON_LEFT || Button == GLFW.GLFW_MOUSE_BUTTON_RIGHT)) {
            Pressed = Index;
            play(ModSounds.PanelPress.get(), Button == GLFW.GLFW_MOUSE_BUTTON_LEFT ? 1.0f : 0.92f, 0.3f);
            changeDigit(Index, Button == GLFW.GLFW_MOUSE_BUTTON_LEFT ? 1 : -1);
            return true;
        }
        return super.mouseClicked(MouseX, MouseY, Button);
    }

    @Override
    public boolean mouseReleased(double MouseX, double MouseY, int Button) {
        if (Pressed >= 0) {
            play(ModSounds.PanelRelease.get(), 1.0f, 0.3f);
        }
        Pressed = -1;
        return super.mouseReleased(MouseX, MouseY, Button);
    }

    @Override
    public boolean mouseScrolled(double MouseX, double MouseY, double ScrollX, double ScrollY) {
        int Index = columnAt(MouseX, MouseY);
        if (Index >= 0 && ScrollY != 0.0) {
            play(ModSounds.PanelTick.get(), ScrollY > 0.0 ? 1.05f : 0.95f, 0.3f);
            changeDigit(Index, ScrollY > 0.0 ? 1 : -1);
            return true;
        }
        if (ScrollY != 0.0 && MouseX >= Left + ListLeft && MouseX < Left + ListRight && MouseY >= Top + ListTop && MouseY < Top + ListBottom) {
            Scroll += ScrollY > 0.0 ? -1 : 1;
            return true;
        }
        return super.mouseScrolled(MouseX, MouseY, ScrollX, ScrollY);
    }

    @Override
    public boolean keyPressed(int Key, int ScanCode, int Modifiers) {
        int Typed = Key >= GLFW.GLFW_KEY_0 && Key <= GLFW.GLFW_KEY_9 ? Key - GLFW.GLFW_KEY_0
                : Key >= GLFW.GLFW_KEY_KP_0 && Key <= GLFW.GLFW_KEY_KP_9 ? Key - GLFW.GLFW_KEY_KP_0 : -1;
        if (Typed >= 0) {
            play(ModSounds.PanelPress.get(), 1.0f, 0.3f);
            setValue(Port % 1000 * 10 + Typed);
            return true;
        }
        if (Key == GLFW.GLFW_KEY_BACKSPACE) {
            play(ModSounds.PanelRelease.get(), 0.9f, 0.3f);
            setValue(Port / 10);
            return true;
        }
        if (Key == GLFW.GLFW_KEY_ENTER || Key == GLFW.GLFW_KEY_KP_ENTER || (minecraft != null && minecraft.options.keyInventory.matches(Key, ScanCode))) {
            onClose();
            return true;
        }
        return super.keyPressed(Key, ScanCode, Modifiers);
    }

    private void play(SoundEvent Sound, float Pitch, float Volume) {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(Sound, Pitch, Volume));
        }
    }

    private int digit(int Index) {
        return Port / Places[Index] % 10;
    }

    private void changeDigit(int Index, int Delta) {
        int Current = digit(Index);
        setValue(Port + (Math.floorMod(Current + Delta, 10) - Current) * Places[Index]);
    }

    private void setValue(int Requested) {
        int Clamped = WireChannels.clampPort(Requested);
        if (Clamped != Port) {
            Port = Clamped;
            PacketDistributor.sendToServer(new NetHubPortPayload(Pos, Clamped));
        }
    }

    private int buttonAt(double MouseX, double MouseY) {
        for (int Index = 0; Index < Places.length; Index++) {
            int X = Left + ButtonX + Index * ColumnPitch;
            int Y = Top + ButtonY;
            if (MouseX >= X && MouseX < X + ButtonWidth && MouseY >= Y && MouseY < Y + ButtonHeight) {
                return Index;
            }
        }
        return -1;
    }

    private int columnAt(double MouseX, double MouseY) {
        for (int Index = 0; Index < Places.length; Index++) {
            int X = Left + ButtonX + Index * ColumnPitch;
            if (MouseX >= X && MouseX < X + ButtonWidth && MouseY >= Top + DigitY - 2 && MouseY < Top + ButtonY + ButtonHeight) {
                return Index;
            }
        }
        return -1;
    }
}
