package net.rai.scp_unleashed.integration.jei;

import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.recipe.IFocusFactory;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.runtime.IRecipesGui;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;

import java.util.Collection;
import java.util.List;

public final class QuietClickArea {
    private QuietClickArea() {
    }

    public static <T extends AbstractContainerScreen<?>> void register(IGuiHandlerRegistration Registration, Class<? extends T> ScreenClass,
                                                                     int X, int Y, int Width, int Height, RecipeType<?>... Types) {
        List<RecipeType<?>> Shown = List.of(Types);
        IGuiClickableArea Area = new IGuiClickableArea() {
            @Override
            public Rect2i getArea() {
                return new Rect2i(X, Y, Width, Height);
            }

            @Override
            public boolean isTooltipEnabled() {
                return false;
            }

            @Override
            public void onClick(IFocusFactory FocusFactory, IRecipesGui RecipesGui) {
                RecipesGui.showTypes(Shown);
            }
        };
        Registration.addGuiContainerHandler(ScreenClass, new IGuiContainerHandler<T>() {
            @Override
            public Collection<IGuiClickableArea> getGuiClickableAreas(T Screen, double MouseX, double MouseY) {
                return List.of(Area);
            }
        });
    }
}
