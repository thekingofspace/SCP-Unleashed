package net.rai.scp_unleashed.labware;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class LabVials {
    private LabVials() {
    }

    public static boolean isVial(ItemStack Stack) {
        return Stack.is(LabwareRegistry.Vial.get());
    }

    public static boolean isSwab(ItemStack Stack) {
        return Stack.is(LabwareRegistry.Swab.get());
    }

    @Nullable
    public static ResourceLocation sampleOf(ItemStack Stack) {
        return Stack.get(LabwareRegistry.Sample.get());
    }

    public static boolean hasSample(ItemStack Stack) {
        return sampleOf(Stack) != null;
    }

    public static void setSample(ItemStack Stack, @Nullable ResourceLocation Sample) {
        if (Sample == null) {
            Stack.remove(LabwareRegistry.Sample.get());
        } else {
            Stack.set(LabwareRegistry.Sample.get(), Sample);
        }
    }

    public static boolean isOpen(ItemStack Stack) {
        return Boolean.TRUE.equals(Stack.get(LabwareRegistry.Opened.get()));
    }

    public static void open(ItemStack Stack) {
        Stack.set(LabwareRegistry.Opened.get(), Boolean.TRUE);
    }

    public static void seal(ItemStack Stack) {
        Stack.remove(LabwareRegistry.Opened.get());
    }

    @Nullable
    public static LabSwabRecord recordOf(ItemStack Stack) {
        return Stack.get(LabwareRegistry.Swabbed.get());
    }

    public static boolean isUsedSwab(ItemStack Stack) {
        return isSwab(Stack) && recordOf(Stack) != null;
    }

    public static boolean isSterileSwab(ItemStack Stack) {
        return isSwab(Stack) && recordOf(Stack) == null;
    }

    public static ItemStack usedSwab(LabSwabRecord Record) {
        ItemStack Stack = new ItemStack(LabwareRegistry.Swab.get());
        Stack.set(LabwareRegistry.Swabbed.get(), Record);
        return Stack;
    }

    public static ItemStack sealedVial(@Nullable ResourceLocation Sample) {
        ItemStack Stack = new ItemStack(LabwareRegistry.Vial.get());
        setSample(Stack, Sample);
        return Stack;
    }

    public static ItemStack openVial(@Nullable ResourceLocation Sample) {
        ItemStack Stack = sealedVial(Sample);
        open(Stack);
        return Stack;
    }

    public static boolean isEmptyVial(ItemStack Stack) {
        return isVial(Stack) && !hasSample(Stack);
    }

    @Nullable
    public static ResourceLocation carriedSample(ItemStack Stack) {
        LabSwabRecord Record = isUsedSwab(Stack) ? recordOf(Stack) : null;
        return Record == null || !Record.Labelled() ? null : Record.cultureSample();
    }

    public static boolean isOpenCulture(ItemStack Stack) {
        return isVial(Stack) && isOpen(Stack) && LabOrganisms.isKnown(sampleOf(Stack));
    }

    public static boolean carriesCulture(ItemStack Stack) {
        return isVial(Stack) && LabOrganisms.isKnown(sampleOf(Stack));
    }
}
