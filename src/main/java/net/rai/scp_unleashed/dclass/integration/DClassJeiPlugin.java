package net.rai.scp_unleashed.dclass.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.control.ControlRegistry;
import net.rai.scp_unleashed.control.ControllerItem;
import net.rai.scp_unleashed.dclass.CloningMachineBlockEntity;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.dclass.theodore.Theodore;
import net.rai.scp_unleashed.dclass.theodore.TheodoreClusters;
import net.rai.scp_unleashed.dclass.theodore.TheodoreRegistry;

@JeiPlugin
public class DClassJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "dclass");

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        Registration.addItemStackInfo(new ItemStack(DClassRegistry.Blood.get()),
                Component.translatable("jei.scp_unleashed.dclass_blood"),
                Component.translatable("jei.scp_unleashed.dclass_blood.use"));
        Registration.addItemStackInfo(new ItemStack(DClassRegistry.CloningMachineItem.get()),
                Component.translatable("jei.scp_unleashed.cloning_machine"),
                Component.translatable("jei.scp_unleashed.cloning_machine.power", CloningMachineBlockEntity.WorkingDraw, CloningMachineBlockEntity.CycleTicks / 20),
                Component.translatable("jei.scp_unleashed.cloning_machine.release"));
        Registration.addItemStackInfo(new ItemStack(DClassRegistry.BlankFile.get()),
                Component.translatable("jei.scp_unleashed.blank_personnel_file"));
        Registration.addItemStackInfo(new ItemStack(ControlRegistry.Controller.get()),
                Component.translatable("jei.scp_unleashed.controller"),
                Component.translatable("jei.scp_unleashed.controller.release", Component.keybind(ControllerItem.ReleaseKey)),
                Component.translatable("jei.scp_unleashed.controller.death"));
        Registration.addItemStackInfo(new ItemStack(DClassRegistry.DClassEgg.get()),
                Component.translatable("jei.scp_unleashed.dclass"),
                Component.translatable("jei.scp_unleashed.dclass.care"));
        Registration.addItemStackInfo(new ItemStack(TheodoreRegistry.Arsenic.get()),
                Component.translatable("jei.scp_unleashed.arsenic"),
                Component.translatable("jei.scp_unleashed.arsenic.clones", Theodore.ClusterSize),
                Component.translatable("jei.scp_unleashed.arsenic.caps", TheodoreClusters.WorldCap, TheodoreClusters.AmalgamationCap));
        Registration.addItemStackInfo(new ItemStack(TheodoreRegistry.TheodoreBlood.get()),
                Component.translatable("jei.scp_unleashed.theodore_blood"));
        Registration.addItemStackInfo(new ItemStack(TheodoreRegistry.TheodoreEgg.get()),
                Component.translatable("jei.scp_unleashed.theodore"),
                Component.translatable("jei.scp_unleashed.theodore.refuses"));
        Registration.addItemStackInfo(new ItemStack(TheodoreRegistry.AmalgamationEgg.get()),
                Component.translatable("jei.scp_unleashed.theodore_amalgamation"),
                Component.translatable("jei.scp_unleashed.theodore_amalgamation.slam"));
        Registration.addItemStackInfo(new ItemStack(TheodoreRegistry.DogTags.get()),
                Component.translatable("jei.scp_unleashed.theodore_dog_tags"));
    }
}
