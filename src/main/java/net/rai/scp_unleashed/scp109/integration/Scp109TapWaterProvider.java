package net.rai.scp_unleashed.scp109.integration;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluids;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp109.Scp109TapBlockEntity;
import org.jetbrains.annotations.Nullable;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.FluidView;
import snownee.jade.api.view.IClientExtensionProvider;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ViewGroup;

import java.util.List;

public enum Scp109TapWaterProvider implements IServerExtensionProvider<CompoundTag>, IClientExtensionProvider<CompoundTag, FluidView> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp109_canteen_tap_water");

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Nullable
    @Override
    public List<ViewGroup<CompoundTag>> getGroups(Accessor<?> Viewed) {
        if (!(Viewed instanceof BlockAccessor Target) || !(Target.getBlockEntity() instanceof Scp109TapBlockEntity Tap)) {
            return null;
        }
        if (!Tap.hasCanteen()) {
            return List.of();
        }
        CompoundTag Water = FluidView.writeDefault(JadeFluidObject.of(Fluids.WATER, Scp109TapBlockEntity.ShownAmount), Scp109TapBlockEntity.ShownAmount, Viewed.nbtOps());
        return List.of(new ViewGroup<>(List.of(Water)));
    }

    @Override
    public List<ClientViewGroup<FluidView>> getClientGroups(Accessor<?> Viewed, List<ViewGroup<CompoundTag>> Groups) {
        return ClientViewGroup.map(Groups, Tag -> {
            FluidView View = FluidView.readDefault(Tag, Viewed.nbtOps());
            if (View != null) {
                View.overrideText = Component.translatable("jade.scp_unleashed.scp109_canteen_tap.endless", View.fluidName);
            }
            return View;
        }, null);
    }
}
