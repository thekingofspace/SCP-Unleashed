package net.rai.scp_unleashed.food.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.food.FoodRegistry;
import net.rai.scp_unleashed.food.FoodSounds;
import net.rai.scp_unleashed.food.Snack;
import net.rai.scp_unleashed.food.VendingMachineBlockEntity;
import net.rai.scp_unleashed.food.VendingMenu;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class VendingScreen extends AbstractContainerScreen<VendingMenu> {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/food/vending_machine.png");
    public static final int ImageHeight = 204;
    public static final int InventoryLabelY = 111;
    public static final int CellX = 9;
    public static final int CellY = 18;
    public static final int CellWidth = 51;
    public static final int CellHeight = 21;
    public static final int IconX = 3;
    public static final int IconY = 2;
    public static final int CodeX = 24;
    public static final int CodeY = 3;
    public static final int PriceX = 24;
    public static final int PriceY = 12;
    public static final int HoverU = 176;
    public static final int HoverV = 0;
    public static final int VendU = 176;
    public static final int VendV = 21;
    public static final int DigitU = 176;
    public static final int DigitV = 42;
    public static final int DigitWidth = 7;
    public static final int DigitHeight = 11;
    public static final int DigitPitch = 8;
    public static final int[] DigitX = {146, 154};
    public static final int DigitY = 20;
    public static final int LcdX = 116;
    public static final int LcdY = 17;
    public static final int LcdWidth = 53;
    public static final int LcdHeight = 28;
    public static final int StatusY = 34;
    public static final int ButtonX = 138;
    public static final int ButtonY = 51;
    public static final int ButtonWidth = 31;
    public static final int ButtonHeight = 16;
    public static final int ButtonU = 176;
    public static final int ButtonV = 54;
    public static final int NuggetU = 208;
    public static final int NuggetV = 54;
    public static final int NuggetSize = 7;
    public static final int DimColour = 0x9A0E1014;
    public static final int CodeColour = 0xFFF2F4F6;
    public static final int PriceColour = 0xFFFFD27A;
    public static final int PoorColour = 0xFFFF7A66;
    public static final int AmberColour = 0xFFFFB347;
    public static final int GreenColour = 0xFF8CFFB4;
    public static final int RedColour = 0xFFFF6A52;
    public static final int DimLcdColour = 0xFF3E8A5E;
    public static final int LocalNoticeTicks = 40;
    public static final float OverlayDepth = 300.0f;
    private static final String Prefix = "gui.scp_unleashed.vending_machine.";

    private boolean ButtonPressed;
    @Nullable
    private Component LocalNotice;
    private int LocalNoticeColour;
    private int LocalNoticeTimer;

    public VendingScreen(VendingMenu Menu, Inventory PlayerInventory, Component Title) {
        super(Menu, PlayerInventory, Title);
        imageWidth = 176;
        imageHeight = ImageHeight;
        inventoryLabelY = InventoryLabelY;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (LocalNoticeTimer > 0 && --LocalNoticeTimer == 0) {
            LocalNotice = null;
        }
    }

    private static ItemStack stackOf(Snack Product) {
        return new ItemStack(FoodRegistry.snack(Product));
    }

    private int cellLeft(int Index) {
        return leftPos + CellX + (Index % Snack.Columns) * CellWidth;
    }

    private int cellTop(int Index) {
        return topPos + CellY + (Index / Snack.Columns) * CellHeight;
    }

    private int cellAt(double MouseX, double MouseY) {
        for (int Index = 0; Index < Snack.values().length; Index++) {
            int X = cellLeft(Index);
            int Y = cellTop(Index);
            if (MouseX >= X && MouseX < X + CellWidth && MouseY >= Y && MouseY < Y + CellHeight) {
                return Index;
            }
        }
        return -1;
    }

    private boolean overButton(double MouseX, double MouseY) {
        return MouseX >= leftPos + ButtonX && MouseX < leftPos + ButtonX + ButtonWidth && MouseY >= topPos + ButtonY && MouseY < topPos + ButtonY + ButtonHeight;
    }

    private long ticks() {
        return minecraft == null || minecraft.level == null ? 0 : minecraft.level.getGameTime();
    }

    @Override
    protected void renderBg(GuiGraphics Graphics, float PartialTick, int MouseX, int MouseY) {
        Graphics.blit(Texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int Credit = menu.getCredit();
        Snack Vending = menu.getVending();
        int Hovered = cellAt(MouseX, MouseY);
        boolean Blink = ticks() / 4 % 2 == 0;
        for (Snack Product : Snack.values()) {
            int Index = Product.ordinal();
            int X = cellLeft(Index);
            int Y = cellTop(Index);
            Graphics.renderItem(stackOf(Product), X + IconX, Y + IconY);
            boolean Affordable = Credit >= Product.getPrice();
            Graphics.drawString(font, Product.getCode(), X + CodeX, Y + CodeY, CodeColour, true);
            String Price = String.valueOf(Product.getPrice());
            Graphics.drawString(font, Price, X + PriceX, Y + PriceY, Affordable ? PriceColour : PoorColour, true);
            Graphics.blit(Texture, X + PriceX + font.width(Price) + 2, Y + PriceY, NuggetU, NuggetV, NuggetSize, NuggetSize);
            Graphics.pose().pushPose();
            Graphics.pose().translate(0.0f, 0.0f, OverlayDepth);
            if (Product == Vending) {
                if (Blink) {
                    Graphics.blit(Texture, X, Y, VendU, VendV, CellWidth, CellHeight);
                }
            } else if (!Affordable || Vending != null) {
                Graphics.fill(X + 1, Y + 1, X + CellWidth - 1, Y + CellHeight - 1, DimColour);
            }
            if (Index == Hovered && Product != Vending) {
                Graphics.blit(Texture, X, Y, HoverU, HoverV, CellWidth, CellHeight);
            }
            Graphics.pose().popPose();
        }
        digits(Graphics, Credit);
        status(Graphics, Credit, Vending);
        int ButtonState = ButtonPressed ? 2 : overButton(MouseX, MouseY) ? 1 : 0;
        Graphics.blit(Texture, leftPos + ButtonX, topPos + ButtonY, ButtonU, ButtonV + ButtonState * ButtonHeight, ButtonWidth, ButtonHeight);
    }

    private void digits(GuiGraphics Graphics, int Credit) {
        int Tens = Credit / 10 % 10;
        int Ones = Credit % 10;
        if (Credit >= 10) {
            Graphics.blit(Texture, leftPos + DigitX[0], topPos + DigitY, DigitU + Tens * DigitPitch, DigitV, DigitWidth, DigitHeight);
        }
        Graphics.blit(Texture, leftPos + DigitX[1], topPos + DigitY, DigitU + Ones * DigitPitch, DigitV, DigitWidth, DigitHeight);
    }

    private void status(GuiGraphics Graphics, int Credit, @Nullable Snack Vending) {
        Component Text;
        int Colour;
        int Notice = menu.getNotice();
        if (Vending != null) {
            Text = Component.translatable(Prefix + "status.vending");
            Colour = AmberColour;
        } else if (LocalNotice != null) {
            Text = LocalNotice;
            Colour = LocalNoticeColour;
        } else if (Notice == VendingMachineBlockEntity.NoticeThanks) {
            Text = Component.translatable(Prefix + "status.thanks");
            Colour = GreenColour;
        } else if (Notice == VendingMachineBlockEntity.NoticeTrayFull) {
            Text = Component.translatable(Prefix + "status.tray_full");
            Colour = RedColour;
        } else if (Notice == VendingMachineBlockEntity.NoticeCreditFull) {
            Text = Component.translatable(Prefix + "status.credit_full");
            Colour = AmberColour;
        } else if (Notice == VendingMachineBlockEntity.NoticeChange) {
            Text = Component.translatable(Prefix + "status.change");
            Colour = GreenColour;
        } else if (Credit <= 0) {
            Text = Component.translatable(Prefix + "status.insert");
            Colour = ticks() / 10 % 2 == 0 ? GreenColour : DimLcdColour;
        } else {
            Text = Component.translatable(Prefix + "status.select");
            Colour = GreenColour;
        }
        int Width = font.width(Text);
        Graphics.drawString(font, Text, leftPos + LcdX + (LcdWidth - Width + 1) / 2, topPos + StatusY, Colour, false);
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float PartialTick) {
        super.render(Graphics, MouseX, MouseY, PartialTick);
        renderTooltip(Graphics, MouseX, MouseY);
        int Hovered = cellAt(MouseX, MouseY);
        if (Hovered >= 0 && menu.getCarried().isEmpty()) {
            Graphics.renderComponentTooltip(font, productLines(Snack.values()[Hovered]), MouseX, MouseY);
        } else if (overButton(MouseX, MouseY)) {
            Graphics.renderComponentTooltip(font, List.of(Component.translatable(Prefix + "return"),
                    Component.translatable(Prefix + "return.detail", menu.getCredit()).withStyle(ChatFormatting.GRAY)), MouseX, MouseY);
        } else if (isHovering(LcdX, LcdY, LcdWidth, LcdHeight, MouseX, MouseY)) {
            Graphics.renderComponentTooltip(font, List.of(Component.translatable(Prefix + "credit", menu.getCredit(), VendingMachineBlockEntity.MaxCredit),
                    Component.translatable(Prefix + "credit.detail").withStyle(ChatFormatting.GRAY)), MouseX, MouseY);
        } else if (hoveredSlot != null && hoveredSlot.index == VendingMenu.CoinSlot && !hoveredSlot.hasItem() && menu.getCarried().isEmpty()) {
            Graphics.renderComponentTooltip(font, List.of(Component.translatable(Prefix + "coin_slot"),
                    Component.translatable(Prefix + "coin_slot.detail").withStyle(ChatFormatting.GRAY)), MouseX, MouseY);
        }
    }

    private List<Component> productLines(Snack Product) {
        List<Component> Lines = new ArrayList<>();
        ItemStack Stack = stackOf(Product);
        Lines.add(Stack.getHoverName().copy().withStyle(ChatFormatting.WHITE));
        Lines.add(Component.translatable(Stack.getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
        MobEffectInstance Effect = Product.effect();
        if (Effect != null) {
            MutableComponent Name = Component.translatable(Effect.getDescriptionId());
            if (Effect.getAmplifier() > 0) {
                Name = Component.translatable("potion.withAmplifier", Name, Component.translatable("potion.potency." + Effect.getAmplifier()));
            }
            float Rate = minecraft == null || minecraft.level == null ? 20.0f : minecraft.level.tickRateManager().tickrate();
            Lines.add(Component.translatable("potion.withDuration", Name, MobEffectUtil.formatDuration(Effect, 1.0f, Rate))
                    .withStyle(Effect.getEffect().value().getCategory().getTooltipFormatting()));
        }
        Lines.add(Component.translatable(Prefix + "price", Product.getPrice(), Product.getCode()).withStyle(ChatFormatting.GOLD));
        int Missing = Product.getPrice() - menu.getCredit();
        if (menu.getVending() != null) {
            Lines.add(Component.translatable(Prefix + "busy").withStyle(ChatFormatting.YELLOW));
        } else if (Missing > 0) {
            Lines.add(Component.translatable(Prefix + "missing", Missing).withStyle(ChatFormatting.RED));
        } else if (!menu.trayAccepts(Stack)) {
            Lines.add(Component.translatable(Prefix + "tray_full").withStyle(ChatFormatting.RED));
        } else {
            Lines.add(Component.translatable(Prefix + "buy").withStyle(ChatFormatting.GREEN));
        }
        return Lines;
    }

    @Override
    public boolean mouseClicked(double MouseX, double MouseY, int Button) {
        if (Button == GLFW.GLFW_MOUSE_BUTTON_LEFT && menu.getCarried().isEmpty()) {
            int Index = cellAt(MouseX, MouseY);
            if (Index >= 0) {
                choose(Snack.values()[Index]);
                return true;
            }
            if (overButton(MouseX, MouseY)) {
                ButtonPressed = true;
                if (menu.getCredit() > 0) {
                    play(FoodSounds.Beep.get(), 0.8f);
                    send(VendingMenu.ReturnButton);
                } else {
                    play(FoodSounds.Error.get(), 1.1f);
                }
                return true;
            }
        }
        return super.mouseClicked(MouseX, MouseY, Button);
    }

    @Override
    public boolean mouseReleased(double MouseX, double MouseY, int Button) {
        ButtonPressed = false;
        return super.mouseReleased(MouseX, MouseY, Button);
    }

    private void choose(Snack Product) {
        if (menu.getVending() != null) {
            play(FoodSounds.Error.get(), 1.0f);
            return;
        }
        int Missing = Product.getPrice() - menu.getCredit();
        if (Missing > 0) {
            play(FoodSounds.Error.get(), 1.0f);
            localNotice(Component.translatable(Prefix + "status.need", Missing), RedColour);
            return;
        }
        if (!menu.trayAccepts(stackOf(Product))) {
            play(FoodSounds.Error.get(), 0.9f);
            localNotice(Component.translatable(Prefix + "status.tray_full"), RedColour);
            return;
        }
        LocalNotice = null;
        LocalNoticeTimer = 0;
        play(FoodSounds.Beep.get(), 1.0f);
        send(Product.ordinal());
    }

    private void localNotice(Component Text, int Colour) {
        LocalNotice = Text;
        LocalNoticeColour = Colour;
        LocalNoticeTimer = LocalNoticeTicks;
    }

    private void send(int Id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, Id);
        }
    }

    private void play(SoundEvent Sound, float Pitch) {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(Sound, Pitch, 0.45f));
        }
    }
}
