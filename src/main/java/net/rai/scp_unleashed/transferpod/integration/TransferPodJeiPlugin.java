package net.rai.scp_unleashed.transferpod.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.transferpod.TransferPodContent;
import net.rai.scp_unleashed.transferpod.TransferPodRegistry;
import net.rai.scp_unleashed.transferpod.TransferPods;
import org.jetbrains.annotations.Nullable;

@JeiPlugin
public class TransferPodJeiPlugin implements IModPlugin {
    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "transfer_pod_jei_plugin");
    private static final String Prefix = "jei.scp_unleashed.info.transfer_pod.";

    @Override
    public ResourceLocation getPluginUid() {
        return Uid;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration Registration) {
        Registration.registerSubtypeInterpreter(TransferPodRegistry.Pod.get(), new ISubtypeInterpreter<>() {
            @Nullable
            @Override
            public Object getSubtypeData(ItemStack Stack, UidContext Context) {
                ResourceLocation Held = TransferPods.contentId(Stack);
                return Held == null ? null : Held.toString();
            }

            @Override
            public String getLegacyStringSubtypeInfo(ItemStack Stack, UidContext Context) {
                ResourceLocation Held = TransferPods.contentId(Stack);
                return Held == null ? "" : Held.toString();
            }
        });
    }

    @Override
    public void registerRecipes(IRecipeRegistration Registration) {
        for (TransferPodContent Held : TransferPods.all()) {
            if (!Held.Listed()) {
                continue;
            }
            Registration.addItemStackInfo(TransferPods.loaded(Held.Id()),
                    Component.translatable(Prefix + "1", Held.label()),
                    Component.translatable(Prefix + "2"),
                    Component.translatable(Prefix + "3"),
                    Component.translatable(Prefix + "4"));
        }
        Registration.addItemStackInfo(TransferPods.spent(),
                Component.translatable(Prefix + "spent.1"),
                Component.translatable(Prefix + "spent.2"));
    }
}
