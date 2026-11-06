package net.rai.scp_unleashed.cameras.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.cameras.CameraBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum CameraJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "cctv_camera");
    private static final String DataKey = "CctvCamera";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        if (!(Accessor.getBlockEntity() instanceof CameraBlockEntity Camera)) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        Info.putString("Name", Camera.getCameraName());
        Info.putBoolean("Linked", Camera.isLinked());
        Info.putInt("Viewers", Camera.getViewers());
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Info = Data.getCompound(DataKey);
        String Name = Info.getString("Name");
        Tooltip.add(Name.isEmpty() ? Component.translatable("jade.scp_unleashed.cctv_camera.unnamed").withStyle(ChatFormatting.GRAY)
                : Component.translatable("jade.scp_unleashed.cctv_camera.name", Name).withStyle(ChatFormatting.WHITE));
        Tooltip.add(Info.getBoolean("Linked") ? Component.translatable("jade.scp_unleashed.cctv_camera.linked").withStyle(ChatFormatting.GREEN)
                : Component.translatable("jade.scp_unleashed.cctv_camera.unlinked").withStyle(ChatFormatting.GRAY));
        int Viewers = Info.getInt("Viewers");
        if (Viewers > 0) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.cctv_camera.live", Viewers).withStyle(ChatFormatting.RED));
        }
    }
}
