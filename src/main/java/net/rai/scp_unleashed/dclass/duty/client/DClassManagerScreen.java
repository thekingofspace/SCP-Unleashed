package net.rai.scp_unleashed.dclass.duty.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassNumbers;
import net.rai.scp_unleashed.dclass.duty.DClassManagerMenu;
import net.rai.scp_unleashed.dclass.duty.DClassRole;
import net.rai.scp_unleashed.dclass.duty.DClassTask;

import java.util.function.BooleanSupplier;

public class DClassManagerScreen extends AbstractContainerScreen<DClassManagerMenu> {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/dclass/dclass_manager.png");
    public static final int ImageWidth = 176;
    public static final int ImageHeight = 212;
    public static final int TextureSize = 256;
    public static final int SpriteY = 216;
    public static final int SpriteWidth = 64;
    public static final int SpriteHeight = 14;
    public static final int SpriteCap = 3;
    public static final int HeaderX = 12;
    public static final int HeaderRight = 164;
    public static final int HeaderY = 16;
    public static final int StatusY = 27;
    public static final int RoleLabelY = 45;
    public static final int RoleButtonX = 50;
    public static final int RoleButtonY = 42;
    public static final int OrdersLabelY = 66;
    public static final int FirstRowY = 81;
    public static final int SecondRowY = 98;
    public static final int OrderX = 34;
    public static final int ButtonGap = 4;
    public static final int PlayerLabelY = 119;
    public static final int Ink = 0xFF3A2F25;
    public static final int FadedInk = 0xFF7A6A55;
    public static final int StampInk = 0xFFB4441E;
    public static final int BoardInk = 0xFFEADCBE;
    public static final int ChosenText = 0xFFFFF6E6;
    private static final String Prefix = "gui.scp_unleashed.dclass_manager.";

    public DClassManagerScreen(DClassManagerMenu Menu, Inventory PlayerInventory, Component Title) {
        super(Menu, PlayerInventory, Title);
        imageWidth = ImageWidth;
        imageHeight = ImageHeight;
        inventoryLabelY = PlayerLabelY;
    }

    @Override
    protected void init() {
        super.init();
        int X = leftPos + RoleButtonX;
        for (DClassRole Role : DClassRole.values()) {
            int Width = Math.max(36, font.width(Role.getName()) + 12);
            addRenderableWidget(new DutyButton(X, topPos + RoleButtonY, Width, Role.getName(), DClassManagerMenu.RoleButton + Role.ordinal(), () -> menu.role() == Role && (Role.active() || !menu.patrolling())));
            X += Width + ButtonGap;
        }
        int First = leftPos + OrderX;
        int Eat = Math.max(50, font.width(Component.translatable(Prefix + "eat")) + 12);
        addRenderableWidget(new DutyButton(First, topPos + FirstRowY, Eat, Component.translatable(Prefix + "eat"), DClassManagerMenu.EatButton,
                () -> menu.task() == DClassTask.EatThis));
        int Follow = Math.max(54, font.width(Component.translatable(Prefix + "follow")) + 12);
        addRenderableWidget(new DutyButton(First + Eat + ButtonGap, topPos + FirstRowY, Follow, Component.translatable(Prefix + "follow"), DClassManagerMenu.FollowButton,
                () -> menu.task() == DClassTask.FollowMe));
        int Clean = Math.max(60, font.width(Component.translatable(Prefix + "clean")) + 12);
        addRenderableWidget(new DutyButton(First, topPos + SecondRowY, Clean, Component.translatable(Prefix + "clean"), DClassManagerMenu.CleanButton,
                () -> menu.task() == DClassTask.CleanArea));
        int Cancel = Math.max(44, font.width(Component.translatable(Prefix + "cancel")) + 12);
        addRenderableWidget(new DutyButton(First + Clean + ButtonGap, topPos + SecondRowY, Cancel, Component.translatable(Prefix + "cancel"), DClassManagerMenu.CancelButton,
                () -> false));
    }

    @Override
    protected void renderBg(GuiGraphics Graphics, float Partial, int MouseX, int MouseY) {
        Graphics.blit(Texture, leftPos, topPos, 0.0f, 0.0f, imageWidth, imageHeight, TextureSize, TextureSize);
    }

    @Override
    protected void renderLabels(GuiGraphics Graphics, int MouseX, int MouseY) {
        DClass Subject = menu.subject();
        String Designation = Subject == null ? DClassNumbers.format(0) : Subject.getName().getString();
        Graphics.drawString(font, font.plainSubstrByWidth(Designation, 84), HeaderX, HeaderY, Ink, false);
        if (Subject != null) {
            String Trait = font.plainSubstrByWidth(Subject.trait().getName().getString(), 70);
            Graphics.drawString(font, Trait, HeaderRight - font.width(Trait), HeaderY, FadedInk, false);
        }
        DClassTask Task = menu.task();
        Component Status = Task.active() ? Component.translatable(Prefix + "status_task", Task.getName())
                : menu.role().active() ? Component.translatable(Prefix + "status_role", menu.role().getName())
                : menu.patrolling() ? Component.translatable(Prefix + "status_patrol") : Component.translatable(Prefix + "status_idle");
        Graphics.drawString(font, Status, HeaderX, StatusY, Task.active() || menu.role().active() || menu.patrolling() ? StampInk : FadedInk, false);
        Graphics.drawString(font, Component.translatable(Prefix + "role"), HeaderX, RoleLabelY, Ink, false);
        Graphics.drawString(font, Component.translatable(Prefix + "orders"), HeaderX, OrdersLabelY, Ink, false);
        Graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, BoardInk, false);
    }

    @Override
    public void render(GuiGraphics Graphics, int MouseX, int MouseY, float Partial) {
        super.render(Graphics, MouseX, MouseY, Partial);
        if (hoveredSlot != null && hoveredSlot.index == DClassManagerMenu.MealSlot && !hoveredSlot.hasItem() && menu.getCarried().isEmpty()) {
            Graphics.renderTooltip(font, Component.translatable(Prefix + "meal_hint"), MouseX, MouseY);
        } else {
            renderTooltip(Graphics, MouseX, MouseY);
        }
    }

    private final class DutyButton extends AbstractButton {
        private final int Id;
        private final BooleanSupplier Chosen;

        private DutyButton(int X, int Y, int Width, Component Text, int Id, BooleanSupplier Chosen) {
            super(X, Y, Width, SpriteHeight, Text);
            this.Id = Id;
            this.Chosen = Chosen;
        }

        @Override
        public void onPress() {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, Id);
            }
        }

        @Override
        protected void renderWidget(GuiGraphics Graphics, int MouseX, int MouseY, float Partial) {
            boolean Picked = Chosen.getAsBoolean();
            int U = Picked ? SpriteWidth * 2 : isHoveredOrFocused() ? SpriteWidth : 0;
            int Middle = SpriteWidth - SpriteCap * 2;
            Graphics.blit(Texture, getX(), getY(), U, SpriteY, SpriteCap, SpriteHeight, TextureSize, TextureSize);
            int Left = width - SpriteCap * 2;
            int Drawn = 0;
            while (Drawn < Left) {
                int Piece = Math.min(Middle, Left - Drawn);
                Graphics.blit(Texture, getX() + SpriteCap + Drawn, getY(), U + SpriteCap, SpriteY, Piece, SpriteHeight, TextureSize, TextureSize);
                Drawn += Piece;
            }
            Graphics.blit(Texture, getX() + width - SpriteCap, getY(), U + SpriteWidth - SpriteCap, SpriteY, SpriteCap, SpriteHeight, TextureSize, TextureSize);
            int TextX = getX() + (width - font.width(getMessage())) / 2;
            int TextY = getY() + (height - 8) / 2 + 1;
            Graphics.drawString(font, getMessage(), TextX, TextY, Picked ? ChosenText : Ink, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput Output) {
            defaultButtonNarrationText(Output);
        }
    }
}
