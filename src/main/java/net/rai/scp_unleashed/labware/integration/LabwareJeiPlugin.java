package net.rai.scp_unleashed.labware.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.labware.DisinfectingWipeItem;
import net.rai.scp_unleashed.labware.LabAnalyzerBlockEntity;
import net.rai.scp_unleashed.labware.LabExposure;
import net.rai.scp_unleashed.labware.LabOrganism;
import net.rai.scp_unleashed.labware.LabOrganisms;
import net.rai.scp_unleashed.labware.LabTubeHolderBlockEntity;
import net.rai.scp_unleashed.labware.LabVials;
import net.rai.scp_unleashed.labware.LabViralStorageBlockEntity;
import net.rai.scp_unleashed.labware.LabwareRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class LabwareJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "labware_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration Registration) {
        Registration.registerSubtypeInterpreter(LabwareRegistry.Vial.get(), new ISubtypeInterpreter<>() {
            @Nullable
            @Override
            public Object getSubtypeData(ItemStack Stack, UidContext Context) {
                String Key = getLegacyStringSubtypeInfo(Stack, Context);
                return Key.isEmpty() ? null : Key;
            }

            @Override
            public String getLegacyStringSubtypeInfo(ItemStack Stack, UidContext Context) {
                ResourceLocation Sample = LabVials.sampleOf(Stack);
                return (Sample == null ? "" : Sample.toString()) + (LabVials.isOpen(Stack) ? "#open" : "");
            }
        });
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        int Radius = (int) LabExposure.Radius;
        Registration.addItemStackInfo(new ItemStack(LabwareRegistry.Vial.get()),
                Component.translatable(Prefix + "lab_vial.1"),
                Component.translatable(Prefix + "lab_vial.2"),
                Component.translatable(Prefix + "lab_vial.3", Radius));
        Registration.addItemStackInfo(LabVials.openVial(null),
                Component.translatable(Prefix + "lab_vial_open.1"),
                Component.translatable(Prefix + "lab_vial_open.2"));
        for (LabOrganism Organism : LabOrganisms.all()) {
            List<Component> Lines = new ArrayList<>();
            for (int Line = 1; Language.getInstance().has(Organism.getInfoKey(Line)); Line++) {
                Lines.add(Component.translatable(Organism.getInfoKey(Line), Organism.getInfoArguments()));
            }
            if (!Lines.isEmpty()) {
                Registration.addItemStackInfo(List.of(Organism.sealedVial(), Organism.openVial()), Lines.toArray(Component[]::new));
            }
        }
        Registration.addItemStackInfo(new ItemStack(LabwareRegistry.Swab.get()),
                Component.translatable(Prefix + "lab_swab.1"),
                Component.translatable(Prefix + "lab_swab.2"),
                Component.translatable(Prefix + "lab_swab.3"),
                Component.translatable(Prefix + "lab_swab.4"));
        Registration.addItemStackInfo(new ItemStack(LabwareRegistry.Wipe.get()),
                Component.translatable(Prefix + "disinfecting_wipe.1"),
                Component.translatable(Prefix + "disinfecting_wipe.2", DisinfectingWipeItem.Radius),
                Component.translatable(Prefix + "disinfecting_wipe.3"));
        Registration.addItemStackInfo(new ItemStack(LabwareRegistry.ResultSlip.get()),
                Component.translatable(Prefix + "lab_result_slip.1"));
        Registration.addItemStackInfo(new ItemStack(LabwareRegistry.PetriDishItem.get()),
                Component.translatable(Prefix + "lab_petri_dish.1"),
                Component.translatable(Prefix + "lab_petri_dish.2", Radius),
                Component.translatable(Prefix + "lab_petri_dish.3"),
                Component.translatable(Prefix + "lab_petri_dish.4"));
        Registration.addItemStackInfo(new ItemStack(LabwareRegistry.TubeHolderItem.get()),
                Component.translatable(Prefix + "lab_tube_holder.1", LabTubeHolderBlockEntity.SlotCount),
                Component.translatable(Prefix + "lab_tube_holder.2"),
                Component.translatable(Prefix + "lab_tube_holder.3"),
                Component.translatable(Prefix + "lab_tube_holder.4", Radius));
        Registration.addItemStackInfo(new ItemStack(LabwareRegistry.ViralStorageItem.get()),
                Component.translatable(Prefix + "lab_viral_storage.1", LabViralStorageBlockEntity.SlotCount),
                Component.translatable(Prefix + "lab_viral_storage.2", LabViralStorageBlockEntity.PowerDraw),
                Component.translatable(Prefix + "lab_viral_storage.3", Radius));
        Registration.addItemStackInfo(new ItemStack(LabwareRegistry.AnalyzerItem.get()),
                Component.translatable(Prefix + "lab_analyzer.1", LabAnalyzerBlockEntity.WorkDraw, LabAnalyzerBlockEntity.WorkTicks / 20),
                Component.translatable(Prefix + "lab_analyzer.2"),
                Component.translatable(Prefix + "lab_analyzer.3"));
    }
}
