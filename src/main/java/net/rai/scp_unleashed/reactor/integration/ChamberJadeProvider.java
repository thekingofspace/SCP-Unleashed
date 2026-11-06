package net.rai.scp_unleashed.reactor.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.reactor.IrradiationChamberBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.Locale;

public enum ChamberJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "irradiation_chamber");
    private static final String DataKey = "IrradiationChamber";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        if (!(Accessor.getBlockEntity() instanceof IrradiationChamberBlockEntity Chamber)) {
            return;
        }
        CompoundTag Info = new CompoundTag();
        Info.putFloat("Flux", Chamber.getFlux());
        Info.putInt("Sources", Chamber.getSources());
        Info.putInt("Progress", Chamber.progressPermille());
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Info = Data.getCompound(DataKey);
        if (Info.getInt("Sources") <= 0) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.irradiation_chamber.idle").withStyle(ChatFormatting.GOLD));
        } else {
            Tooltip.add(Component.translatable("jade.scp_unleashed.irradiation_chamber.flux", String.format(Locale.ROOT, "%.2f", Info.getFloat("Flux")), Info.getInt("Sources"))
                    .withStyle(ChatFormatting.GREEN));
        }
        if (Info.getInt("Progress") > 0) {
            Tooltip.add(Component.translatable("jade.scp_unleashed.irradiation_chamber.progress", Math.round(Info.getInt("Progress") / 10.0f)));
        }
    }
}
