package net.rai.scp_unleashed.scp151;

import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public enum Scp151Sheet implements StringRepresentable {
    None("none", null, Items.AIR),
    White("white", DyeColor.WHITE, Items.WHITE_WOOL),
    Orange("orange", DyeColor.ORANGE, Items.ORANGE_WOOL),
    Magenta("magenta", DyeColor.MAGENTA, Items.MAGENTA_WOOL),
    LightBlue("light_blue", DyeColor.LIGHT_BLUE, Items.LIGHT_BLUE_WOOL),
    Yellow("yellow", DyeColor.YELLOW, Items.YELLOW_WOOL),
    Lime("lime", DyeColor.LIME, Items.LIME_WOOL),
    Pink("pink", DyeColor.PINK, Items.PINK_WOOL),
    Gray("gray", DyeColor.GRAY, Items.GRAY_WOOL),
    LightGray("light_gray", DyeColor.LIGHT_GRAY, Items.LIGHT_GRAY_WOOL),
    Cyan("cyan", DyeColor.CYAN, Items.CYAN_WOOL),
    Purple("purple", DyeColor.PURPLE, Items.PURPLE_WOOL),
    Blue("blue", DyeColor.BLUE, Items.BLUE_WOOL),
    Brown("brown", DyeColor.BROWN, Items.BROWN_WOOL),
    Green("green", DyeColor.GREEN, Items.GREEN_WOOL),
    Red("red", DyeColor.RED, Items.RED_WOOL),
    Black("black", DyeColor.BLACK, Items.BLACK_WOOL);

    private final String Name;
    private final DyeColor Dye;
    private final Item Wool;

    Scp151Sheet(String Name, DyeColor Dye, Item Wool) {
        this.Name = Name;
        this.Dye = Dye;
        this.Wool = Wool;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }

    public boolean isDrawn() {
        return this != None;
    }

    public ItemStack wool() {
        return isDrawn() ? new ItemStack(Wool) : ItemStack.EMPTY;
    }

    public Component colourName() {
        return Dye == null ? Component.empty() : Component.translatable("color.minecraft." + Dye.getName());
    }

    public static boolean isWool(ItemStack Stack) {
        return !Stack.isEmpty() && Stack.is(ItemTags.WOOL);
    }

    public static Scp151Sheet fromWool(ItemStack Stack) {
        if (!isWool(Stack)) {
            return None;
        }
        for (Scp151Sheet Each : values()) {
            if (Each.isDrawn() && Stack.is(Each.Wool)) {
                return Each;
            }
        }
        return White;
    }
}
