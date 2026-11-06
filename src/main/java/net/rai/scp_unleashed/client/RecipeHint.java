package net.rai.scp_unleashed.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.List;

public final class RecipeHint {
    private RecipeHint() {
    }

    public static List<Component> with(List<Component> Lines, boolean OverArea) {
        if (!OverArea || !ModList.get().isLoaded("jei")) {
            return Lines;
        }
        List<Component> Combined = new ArrayList<>(Lines);
        Combined.add(Component.translatable("gui.scp_unleashed.recipe_hint").withStyle(ChatFormatting.DARK_GRAY));
        return Combined;
    }
}
