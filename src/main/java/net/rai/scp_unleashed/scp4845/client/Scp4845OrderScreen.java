package net.rai.scp_unleashed.scp4845.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.scp4845.Scp4845Registry;
import net.rai.scp_unleashed.scp4845.item.Scp4845MenuItem;
import net.rai.scp_unleashed.scp4845.order.Scp4845OrderMenu;
import net.rai.scp_unleashed.scp4845.order.Scp4845Orders;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Scp4845OrderScreen extends AbstractContainerScreen<Scp4845OrderMenu> {
    public static final ResourceLocation Texture = Scp4845Registry.location("textures/gui/scp4845/order_menu.png");
    public static final int Width = 230;
    public static final int Height = 172;
    public static final int Columns = 4;
    public static final int CardX = 9;
    public static final int CardY = 27;
    public static final int CardWidth = 50;
    public static final int CardHeight = 44;
    public static final int CardGap = 4;
    public static final int IconX = 17;
    public static final int IconY = 5;
    public static final int NameY = 24;
    public static final int PriceY = 33;
    public static final int HoverU = 0;
    public static final int SelectedU = 52;
    public static final int OverlayV = 180;
    public static final int OrderX = 165;
    public static final int OrderY = 145;
    public static final int OrderWidth = 56;
    public static final int OrderHeight = 18;
    public static final int OrderU = 104;
    public static final int ClearX = 120;
    public static final int ClearY = 146;
    public static final int ClearWidth = 40;
    public static final int ClearHeight = 16;
    public static final int ClearU = 162;
    public static final int GlyphU = 204;
    public static final int GlyphV = 180;
    public static final int GlyphSize = 8;
    public static final int BadgeU = 214;
    public static final int BadgeV = 180;
    public static final int BadgeSize = 10;
    public static final int SummaryX = 12;
    public static final int SummaryY = 128;
    public static final int TitleRight = 10;
    public static final int TitleY = 11;
    public static final float NameScale = 0.75f;
    public static final int TextColour = 0xFFEDE6DA;
    public static final int DimColour = 0xFFB9AFC4;
    public static final int PriceColour = 0xFFE6D08E;
    public static final int ShortColour = 0xFFD98A7E;
    public static final int GoodColour = 0xFF9FD3A6;
    public static final int NoticeTicks = 60;
    private static final String Prefix = "gui.scp_unleashed.scp4845_order.";

    private final int[] Cart = new int[Scp4845MenuItem.values().length];
    private boolean OrderPressed;
    private boolean ClearPressed;
    private int NoticeTimer;

    public Scp4845OrderScreen(Scp4845OrderMenu Menu, Inventory PlayerInventory, Component Title) {
        super(Menu, PlayerInventory, Title);
        imageWidth = Width;
        imageHeight = Height;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (NoticeTimer > 0) {
            NoticeTimer--;
        }
    }

    private int cardLeft(int Index) {
        return leftPos + CardX + (Index % Columns) * (CardWidth + CardGap);
    }

    private int cardTop(int Index) {
        return topPos + CardY + (Index / Columns) * (CardHeight + CardGap);
    }

    private int cardAt(double MouseX, double MouseY) {
        for (int Index = 0; Index < Cart.length; Index++) {
            int X = cardLeft(Index);
            int Y = cardTop(Index);
            if (MouseX >= X && MouseX < X + CardWidth && MouseY >= Y && MouseY < Y + CardHeight) {
                return Index;
            }
        }
        return -1;
    }

    private boolean over(double MouseX, double MouseY, int X, int Y, int W, int H) {
        return MouseX >= leftPos + X && MouseX < leftPos + X + W && MouseY >= topPos + Y && MouseY < topPos + Y + H;
    }

    private int total() {
        int Sum = 0;
        for (Scp4845MenuItem Entry : Scp4845MenuItem.values()) {
            Sum += Entry.Price * Cart[Entry.ordinal()];
        }
        return Sum;
    }

    private int items() {
        int Sum = 0;
        for (int Count : Cart) {
            Sum += Count;
        }
        return Sum;
    }

    private int wallet() {
        return minecraft == null || minecraft.player == null ? 0 : Scp4845Orders.emeralds(minecraft.player.getInventory());
    }

    private boolean free() {
        return minecraft != null && minecraft.player != null && minecraft.player.getAbilities().instabuild;
    }

    private boolean affordable() {
        return items() > 0 && (free() || total() <= wallet());
    }

    @Override
    protected void renderBg(GuiGraphics Graphics, float PartialTick, int MouseX, int MouseY) {
        Graphics.blit(Texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int Hovered = cardAt(MouseX, MouseY);
        for (Scp4845MenuItem Entry : Scp4845MenuItem.values()) {
            int Index = Entry.ordinal();
            int X = cardLeft(Index);
            int Y = cardTop(Index);
            if (Cart[Index] > 0) {
                Graphics.blit(Texture, X, Y, SelectedU, OverlayV, CardWidth, CardHeight);
            }
            if (Index == Hovered) {
                Graphics.blit(Texture, X, Y, HoverU, OverlayV, CardWidth, CardHeight);
            }
            Graphics.renderItem(new ItemStack(Scp4845Registry.food(Entry)), X + IconX, Y + IconY);
            Component Short = Component.translatable(Prefix + "short." + Entry.Id);
            Graphics.pose().pushPose();
            Graphics.pose().translate(X + CardWidth / 2.0f, Y + NameY, 0.0f);
            Graphics.pose().scale(NameScale, NameScale, 1.0f);
            Graphics.drawString(font, Short, -font.width(Short) / 2, 0, TextColour, false);
            Graphics.pose().popPose();
            String Price = String.valueOf(Entry.Price);
            int PriceWidth = font.width(Price) + 1 + GlyphSize;
            int PriceLeft = X + (CardWidth - PriceWidth) / 2;
            Graphics.drawString(font, Price, PriceLeft, Y + PriceY, PriceColour, false);
            Graphics.blit(Texture, PriceLeft + font.width(Price) + 1, Y + PriceY, GlyphU, GlyphV, GlyphSize, GlyphSize);
            if (Cart[Index] > 0) {
                int BadgeX = X + CardWidth - BadgeSize - 2;
                int BadgeY = Y + 2;
                Graphics.pose().pushPose();
                Graphics.pose().translate(0.0f, 0.0f, 200.0f);
                Graphics.blit(Texture, BadgeX, BadgeY, BadgeU, BadgeV, BadgeSize, BadgeSize);
                String Count = String.valueOf(Cart[Index]);
                Graphics.drawString(font, Count, BadgeX + (BadgeSize - font.width(Count)) / 2 + 1, BadgeY + 1, TextColour, false);
                Graphics.pose().popPose();
            }
        }
        int OrderState = !affordable() ? 3 : OrderPressed ? 2 : over(MouseX, MouseY, OrderX, OrderY, OrderWidth, OrderHeight) ? 1 : 0;
        Graphics.blit(Texture, leftPos + OrderX, topPos + OrderY, OrderU, OverlayV + OrderState * OrderHeight, OrderWidth, OrderHeight);
        int ClearState = items() == 0 ? 3 : ClearPressed ? 2 : over(MouseX, MouseY, ClearX, ClearY, ClearWidth, ClearHeight) ? 1 : 0;
        Graphics.blit(Texture, leftPos + ClearX, topPos + ClearY, ClearU, OverlayV + ClearState * ClearHeight, ClearWidth, ClearHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics Graphics, int MouseX, int MouseY) {
        Graphics.drawString(font, title, imageWidth - TitleRight - font.width(title), TitleY, TextColour, false);
        Component Order = Component.translatable(Prefix + "order");
        Graphics.drawString(font, Order, OrderX + (OrderWidth - font.width(Order)) / 2, OrderY + 5, affordable() ? TextColour : DimColour, false);
        Component Clear = Component.translatable(Prefix + "clear");
        Graphics.drawString(font, Clear, ClearX + (ClearWidth - font.width(Clear)) / 2, ClearY + 4, items() > 0 ? TextColour : DimColour, false);
        int Total = total();
        int Owned = wallet();
        if (NoticeTimer > 0) {
            Graphics.drawString(font, Component.translatable(Prefix + "thanks"), SummaryX, SummaryY + 4, GoodColour, false);
            Graphics.drawString(font, Component.translatable(Prefix + "for_here"), SummaryX, SummaryY + 18, DimColour, false);
            return;
        }
        Graphics.drawString(font, Component.translatable(Prefix + "items", items()), SummaryX, SummaryY, TextColour, false);
        Component TotalLine = Component.translatable(Prefix + "total", Total);
        Graphics.drawString(font, TotalLine, SummaryX, SummaryY + 12, free() || Total <= Owned ? PriceColour : ShortColour, false);
        Graphics.blit(Texture, SummaryX + font.width(TotalLine) + 2, SummaryY + 12, GlyphU, GlyphV, GlyphSize, GlyphSize);
        Component Have = Component.translatable(Prefix + "wallet", Owned);
        Graphics.drawString(font, Have, SummaryX, SummaryY + 24, DimColour, false);
        Graphics.blit(Texture, SummaryX + font.width(Have) + 2, SummaryY + 24, GlyphU, GlyphV, GlyphSize, GlyphSize);
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        super.render(Graphics, MouseX, MouseY, PartialTick);
        int Hovered = cardAt(MouseX, MouseY);
        if (Hovered >= 0) {
            Scp4845MenuItem Entry = Scp4845MenuItem.values()[Hovered];
            List<Component> Lines = new ArrayList<>();
            Lines.add(Component.translatable("item.scp_unleashed." + Entry.Id));
            Lines.add(Component.translatable("item.scp_unleashed." + Entry.Id + ".tooltip").withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable(Entry.Count > 1 ? Prefix + "price_many" : Prefix + "price", Entry.Price, Entry.Count).withStyle(ChatFormatting.YELLOW));
            Lines.add(Component.translatable(Prefix + "hint").withStyle(ChatFormatting.DARK_GRAY));
            Graphics.renderComponentTooltip(font, Lines, MouseX, MouseY);
        }
    }

    private void click() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        }
    }

    @Override
    public boolean mouseClicked(double MouseX, double MouseY, int Button) {
        int Card = cardAt(MouseX, MouseY);
        if (Card >= 0) {
            if (Button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                Cart[Card] = Math.max(0, Cart[Card] - 1);
            } else {
                Cart[Card] = Math.min(Scp4845MenuItem.MaxPerLine, Cart[Card] + 1);
            }
            NoticeTimer = 0;
            click();
            return true;
        }
        if (Button == GLFW.GLFW_MOUSE_BUTTON_LEFT && over(MouseX, MouseY, OrderX, OrderY, OrderWidth, OrderHeight) && affordable()) {
            OrderPressed = true;
            return true;
        }
        if (Button == GLFW.GLFW_MOUSE_BUTTON_LEFT && over(MouseX, MouseY, ClearX, ClearY, ClearWidth, ClearHeight) && items() > 0) {
            ClearPressed = true;
            return true;
        }
        return super.mouseClicked(MouseX, MouseY, Button);
    }

    @Override
    public boolean mouseReleased(double MouseX, double MouseY, int Button) {
        if (OrderPressed) {
            OrderPressed = false;
            if (over(MouseX, MouseY, OrderX, OrderY, OrderWidth, OrderHeight) && affordable() && minecraft != null && minecraft.gameMode != null) {
                for (Scp4845MenuItem Entry : Scp4845MenuItem.values()) {
                    int Count = Cart[Entry.ordinal()];
                    if (Count > 0) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, Scp4845OrderMenu.button(Entry, Count));
                    }
                    Cart[Entry.ordinal()] = 0;
                }
                NoticeTimer = NoticeTicks;
                click();
            }
            return true;
        }
        if (ClearPressed) {
            ClearPressed = false;
            if (over(MouseX, MouseY, ClearX, ClearY, ClearWidth, ClearHeight)) {
                Arrays.fill(Cart, 0);
                click();
            }
            return true;
        }
        return super.mouseReleased(MouseX, MouseY, Button);
    }
}
