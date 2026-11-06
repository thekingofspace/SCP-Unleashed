package net.rai.scp_unleashed.space.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.rai.scp_unleashed.integration.jei.QuietClickArea;
import net.rai.scp_unleashed.space.AirCanisterItem;
import net.rai.scp_unleashed.space.AirInfuserBlockEntity;
import net.rai.scp_unleashed.space.SpaceAir;
import net.rai.scp_unleashed.space.SpaceRegistry;
import net.rai.scp_unleashed.space.SpaceSuitPiece;
import net.rai.scp_unleashed.space.client.AirInfuserScreen;

import java.util.Arrays;
import java.util.List;

@JeiPlugin
public class SpaceJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = SpaceRegistry.location("space_jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration Registration) {
        Registration.addRecipeCategories(new AirInfusingCategory(Registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addRecipes(AirInfusingCategory.Type, AirInfusingCategory.Infusing.all());
        Registration.addItemStackInfo(new ItemStack(SpaceRegistry.AirInfuserItem.get()),
                Component.translatable("jei.scp_unleashed.info.air_infuser.1", AirInfuserBlockEntity.fillTicks(0) / 20, AirInfuserBlockEntity.WorkDraw),
                Component.translatable("jei.scp_unleashed.info.air_infuser.2"));
        Registration.addItemStackInfo(List.of(SpaceAir.canister(0), SpaceAir.canister(AirCanisterItem.Capacity)),
                Component.translatable("jei.scp_unleashed.info.air_canister.1", AirCanisterItem.Capacity / 60),
                Component.translatable("jei.scp_unleashed.info.air_canister.2"));
        for (SpaceSuitPiece Piece : SpaceSuitPiece.values()) {
            Registration.addItemStackInfo(new ItemStack(SpaceRegistry.suitItem(Piece).get()),
                    Component.translatable("jei.scp_unleashed.info.space_suit.1"),
                    Component.translatable("jei.scp_unleashed.info.space_suit.2"),
                    Component.translatable("jei.scp_unleashed.info." + Piece.getId()));
        }
        info(Registration, "lunar_dust", SpaceRegistry.LunarDustItem.get());
        info(Registration, "lunar_regolith", SpaceRegistry.LunarRegolithItem.get());
        info(Registration, "lunar_stone", SpaceRegistry.LunarStoneItem.get(), SpaceRegistry.LunarStoneSlabItem.get(), SpaceRegistry.LunarStoneStairsItem.get(),
                SpaceRegistry.LunarStoneWallItem.get(), SpaceRegistry.PolishedLunarStoneItem.get(), SpaceRegistry.LunarStoneBricksItem.get(),
                SpaceRegistry.LunarStoneBrickSlabItem.get(), SpaceRegistry.LunarStoneBrickStairsItem.get(), SpaceRegistry.LunarStoneBrickWallItem.get());
    }

    private static void info(IRecipeRegistration Registration, String Key, ItemLike... Items) {
        List<ItemStack> Stacks = Arrays.stream(Items).map(ItemStack::new).toList();
        Registration.addItemStackInfo(Stacks, Component.translatable("jei.scp_unleashed.info." + Key));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration Registration) {
        Registration.addRecipeCatalyst(new ItemStack(SpaceRegistry.AirInfuserItem.get()), AirInfusingCategory.Type);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration Registration) {
        QuietClickArea.register(Registration, AirInfuserScreen.class, AirInfuserScreen.WindowX, AirInfuserScreen.WindowY, AirInfuserScreen.WindowWidth, 18,
                AirInfusingCategory.Type);
    }
}
