package net.rai.scp_unleashed.forcefield.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.forcefield.ForcefieldBarrierBlock;
import net.rai.scp_unleashed.forcefield.ForcefieldEmitterBlock;
import net.rai.scp_unleashed.forcefield.ForcefieldEmitterBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;

public enum ForcefieldJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "forcefield");
    private static final String Prefix = "jade.scp_unleashed.forcefield.";
    private static final String DataKey = "Forcefield";
    private static final int BarColour = 0xFF3FD8F0;
    private static final int BarShadeColour = 0xFF146A78;

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        Level World = Accessor.getLevel();
        BlockState State = Accessor.getBlockState();
        ForcefieldEmitterBlockEntity Emitter = State.getBlock() instanceof ForcefieldBarrierBlock
                ? ForcefieldBarrierBlock.owner(World, Accessor.getPosition())
                : ForcefieldEmitterBlock.bottomFor(World, Accessor.getPosition(), State);
        CompoundTag Field = new CompoundTag();
        if (Emitter == null) {
            Field.putString("Status", "unpaired");
            Data.put(DataKey, Field);
            return;
        }
        ForcefieldEmitterBlockEntity.WallReport Report = Emitter.report();
        Field.putString("Status", status(Report));
        Field.putFloat("Integrity", Report.Integrity());
        Field.putFloat("Boot", Report.Boot());
        Field.putInt("Columns", Report.Columns());
        Field.putInt("Online", Report.Online());
        Field.putInt("Gap", Report.Gap());
        Field.putInt("Draw", Report.Draw());
        Data.put(DataKey, Field);
    }

    private static String status(ForcefieldEmitterBlockEntity.WallReport Report) {
        if (Report.Overlong()) {
            return "overlong";
        }
        if (Report.Gap() <= 0) {
            return "unpaired";
        }
        return switch (Report.Phase()) {
            case Online -> "online";
            case Booting -> "rebooting";
            case Collapsing -> "collapsing";
            default -> Report.Faulted() ? "collapsed" : "unpowered";
        };
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey)) {
            return;
        }
        CompoundTag Field = Data.getCompound(DataKey);
        String Status = Field.getString("Status");
        ChatFormatting Colour = switch (Status) {
            case "online" -> ChatFormatting.AQUA;
            case "rebooting" -> ChatFormatting.YELLOW;
            case "collapsing", "collapsed" -> ChatFormatting.RED;
            default -> ChatFormatting.GOLD;
        };
        if (Status.equals("rebooting")) {
            Tooltip.add(Component.translatable(Prefix + Status, Math.round(Field.getFloat("Boot") * 100.0f)).withStyle(Colour));
        } else {
            Tooltip.add(Component.translatable(Prefix + Status).withStyle(Colour));
        }
        if (Field.getInt("Online") > 0) {
            float Integrity = Field.getFloat("Integrity");
            IElementHelper Helper = IElementHelper.get();
            Tooltip.add(Helper.progress(Integrity, Component.translatable(Prefix + "integrity", Math.round(Integrity * 100.0f)),
                    Helper.progressStyle().color(BarColour, BarShadeColour).textColor(0xFFFFFFFF), BoxStyle.getNestedBox(), true));
        }
        if (Field.getInt("Gap") > 0) {
            Tooltip.add(Component.translatable(Prefix + "size", Field.getInt("Columns"), Field.getInt("Gap")).withStyle(ChatFormatting.GRAY));
        }
        if (Field.getInt("Draw") > 0) {
            Tooltip.add(Component.translatable(Prefix + "draw", Field.getInt("Draw")).withStyle(ChatFormatting.GRAY));
        }
    }
}
