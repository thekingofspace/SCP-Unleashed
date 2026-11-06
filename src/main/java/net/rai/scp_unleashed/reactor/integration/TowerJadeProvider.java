package net.rai.scp_unleashed.reactor.integration;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.reactor.BoosterLinks;
import net.rai.scp_unleashed.reactor.GridState;
import net.rai.scp_unleashed.reactor.GridTower;
import net.rai.scp_unleashed.reactor.ReactorGrid;
import net.rai.scp_unleashed.reactor.ReactorGridManager;
import net.rai.scp_unleashed.reactor.TowerHeat;
import net.rai.scp_unleashed.reactor.TowerSection;
import net.rai.scp_unleashed.reactor.TowerSegmentBlock;
import net.rai.scp_unleashed.reactor.core.CoreTower;
import net.rai.scp_unleashed.reactor.core.TowerType;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.Locale;

public enum TowerJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    Instance;

    private static final ResourceLocation Uid = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "fission_tower");
    private static final String DataKey = "FissionTower";

    @Override
    public ResourceLocation getUid() {
        return Uid;
    }

    @Override
    public void appendServerData(CompoundTag Data, BlockAccessor Accessor) {
        if (!(Accessor.getLevel() instanceof ServerLevel World)) {
            return;
        }
        ReactorGrid Grid = ReactorGridManager.get(World).gridAt(World, Accessor.getPosition());
        GridTower Tower = Grid == null ? null : Grid.towerAt(Accessor.getPosition());
        CompoundTag Info = new CompoundTag();
        if (Grid == null || Tower == null) {
            Info.putByte("State", (byte) (Grid == null ? GridState.Missing : Grid.getState()).ordinal());
            Info.putBoolean("Bare", true);
            Data.put(DataKey, Info);
            return;
        }
        CoreTower Core = Tower.Core;
        Info.putByte("State", (byte) Grid.getState().ordinal());
        Info.putByte("Height", (byte) Core.Height);
        Info.putInt("Towers", Grid.getTowers().size());
        switch (Core.Type) {
            case Fuel -> {
                Info.putFloat("Temp", Core.Temperature);
                Info.putFloat("Int", Core.Integrity);
                Info.putInt("Rods", Core.loadedLevels());
                Info.putInt("Spent", Tower.spentRods());
                Info.putInt("Left", Tower.fuelPercent());
                Info.putFloat("Out", Core.withdrawal());
                Info.putFloat("Made", Core.HeatMade);
                Info.putInt("Feeds", Core.RodContacts.size());
                Info.putInt("Coolers", Core.CoolantContacts.size());
                Info.putFloat("Gas", Core.gasCapacity() <= 0 ? 0.0f : Core.Gas / Core.gasCapacity());
                Info.putBoolean("Leak", Tower.Leaking);
                Info.putBoolean("Vent", Tower.Vented || Tower.Captured);
                Info.putFloat("Boost", Core.boost());
            }
            case Rods -> {
                Info.putFloat("Ins", Core.Insertion);
                Info.putFloat("Tgt", Core.Target);
                Info.putBoolean("Forced", Core.Forced);
                Info.putInt("Feeds", Core.FuelContacts.size());
                Info.putInt("Boosters", Core.BoosterContacts.size());
                Info.putFloat("Pass", BoosterLinks.rodPass(Core));
            }
            case Booster -> {
                Info.putFloat("Str", Core.strength());
                Info.putFloat("Gate", BoosterLinks.gate(Core));
                Info.putInt("Rods", Core.RodContacts.size());
                Info.putInt("Reach", BoosterLinks.reachedFuel(Core));
            }
            default -> {
                Info.putFloat("Water", Core.Water);
                Info.putInt("Cap", Core.waterCapacity());
                Info.putFloat("Made", Core.SteamMade);
                Info.putBoolean("Dry", Core.Dry);
                Info.putBoolean("Blow", Tower.Blowing);
                Info.putBoolean("Vent", Tower.Vented || Tower.Captured);
                Info.putInt("Feeds", Core.FuelContacts.size());
            }
        }
        Data.put(DataKey, Info);
    }

    @Override
    public void appendTooltip(ITooltip Tooltip, BlockAccessor Accessor, IPluginConfig Config) {
        CompoundTag Data = Accessor.getServerData();
        if (!Data.contains(DataKey) || !(Accessor.getBlock() instanceof TowerSegmentBlock Segment)) {
            return;
        }
        CompoundTag Info = Data.getCompound(DataKey);
        GridState State = GridState.byIndex(Info.getByte("State"));
        if (State == GridState.Missing) {
            return;
        }
        if (Info.getBoolean("Bare")) {
            Tooltip.add(Component.translatable(State.getTranslationKey()).withStyle(State.getColour()));
            return;
        }
        Tooltip.add(Component.translatable("jade.scp_unleashed.fission_tower.height", Info.getByte("Height"), Info.getInt("Towers")).withStyle(ChatFormatting.GRAY));
        if (State != GridState.Ready) {
            Tooltip.add(Component.translatable(State.getTranslationKey()).withStyle(State.getColour()));
        }
        TowerSection Section = Accessor.getBlockState().getValue(TowerSegmentBlock.Section);
        TowerType Type = Segment.getTowerType();
        switch (Type) {
            case Fuel -> {
                float Temperature = Info.getFloat("Temp");
                float Integrity = Info.getFloat("Int");
                TowerHeat Heat = TowerHeat.of(Temperature, Integrity);
                Tooltip.add(Component.translatable("jade.scp_unleashed.fission_tower.heat", Math.round(Temperature), Component.translatable(Heat.getTranslationKey())).withStyle(Heat.getColour()));
                if (Integrity < 99.5f) {
                    Tooltip.add(Component.translatable("jade.scp_unleashed.fission_tower.integrity", Math.round(Integrity)).withStyle(Integrity < 50.0f ? ChatFormatting.RED : ChatFormatting.GOLD));
                }
                Tooltip.add(Component.translatable("jade.scp_unleashed.fission_tower.rods", Info.getInt("Rods"), Info.getByte("Height"), Info.getInt("Left")));
                if (Info.getInt("Spent") > 0) {
                    Tooltip.add(Component.translatable("jade.scp_unleashed.fission_tower.spent", Info.getInt("Spent")).withStyle(ChatFormatting.GOLD));
                }
                if (Info.getInt("Feeds") == 0) {
                    Tooltip.add(Component.translatable("jade.scp_unleashed.fission_tower.no_rods").withStyle(ChatFormatting.GOLD));
                } else {
                    Tooltip.add(Component.translatable("jade.scp_unleashed.fission_tower.reaction", Math.round(Info.getFloat("Out") * 100.0f),
                            String.format(Locale.ROOT, "%.1f", Info.getFloat("Made"))).withStyle(ChatFormatting.AQUA));
                }
                if (Info.getFloat("Boost") > 0.005f) {
                    Tooltip.add(Component.translatable("jade.scp_unleashed.fission_tower.boosted", Math.round(Info.getFloat("Boost") * 100.0f)).withStyle(ChatFormatting.DARK_AQUA));
                }
                if (Info.getInt("Coolers") == 0) {
                    Tooltip.add(Component.translatable("jade.scp_unleashed.fission_tower.no_coolant").withStyle(ChatFormatting.GOLD));
                }
                String Gas = Info.getBoolean("Leak") ? "leaking" : Info.getBoolean("Vent") ? "vented" : "sealed";
                Tooltip.add(Component.translatable("jade.scp_unleashed.fission_tower.gas." + Gas, Math.round(Info.getFloat("Gas") * 100.0f))
                        .withStyle(Info.getBoolean("Leak") ? ChatFormatting.RED : ChatFormatting.GRAY));
                if (Section.isLowest()) {
                    Tooltip.add(Component.translatable("jade.scp_unleashed.fission_tower.gas_port").withStyle(ChatFormatting.DARK_GRAY));
                }
            }
            case Rods -> {
                Tooltip.add(Component.translatable("jade.scp_unleashed.fission_tower.inserted", Math.round(Info.getFloat("Ins")), Math.round(Info.getFloat("Tgt"))).withStyle(ChatFormatting.AQUA));
                Tooltip.add(Component.translatable("jade.scp_unleashed.fission_tower.controls", Info.getInt("Feeds")).withStyle(Info.getInt("Feeds") > 0 ? ChatFormatting.GRAY : ChatFormatting.GOLD));
                if (Info.getInt("Boosters") > 0) {
                    Tooltip.add(Component.translatable("jade.scp_unleashed.fission_tower.gates", Info.getInt("Boosters"), Math.round(Info.getFloat("Pass") * 100.0f))
                            .withStyle(Info.getFloat("Pass") > 0.005f ? ChatFormatting.DARK_AQUA : ChatFormatting.GRAY));
                }
                if (Info.getBoolean("Forced")) {
                    Tooltip.add(Component.translatable("jade.scp_unleashed.fission_tower.forced").withStyle(ChatFormatting.RED));
                }
            }
            case Booster -> {
                Tooltip.add(Component.translatable("jade.scp_unleashed.fission_tower.strength", Math.round(Info.getFloat("Str") * 100.0f)).withStyle(ChatFormatting.AQUA));
                if (Info.getInt("Rods") == 0) {
                    Tooltip.add(Component.translatable("jade.scp_unleashed.fission_tower.no_gate").withStyle(ChatFormatting.GOLD));
                } else {
                    Tooltip.add(Component.translatable("jade.scp_unleashed.fission_tower.gated", Info.getInt("Rods"), Math.round(Info.getFloat("Gate") * 100.0f))
                            .withStyle(Info.getFloat("Gate") > 0.005f ? ChatFormatting.DARK_AQUA : ChatFormatting.GRAY));
                    if (Info.getInt("Reach") == 0) {
                        Tooltip.add(Component.translatable("jade.scp_unleashed.fission_tower.no_reach").withStyle(ChatFormatting.GOLD));
                    }
                }
            }
            default -> {
                Tooltip.add(Component.translatable("jade.scp_unleashed.fission_tower.water", Math.round(Info.getFloat("Water")), Info.getInt("Cap"))
                        .withStyle(Info.getBoolean("Dry") ? ChatFormatting.GOLD : ChatFormatting.AQUA));
                Tooltip.add(Component.translatable("jade.scp_unleashed.fission_tower.steam", String.format(Locale.ROOT, "%.1f", Info.getFloat("Made"))));
                Tooltip.add(Component.translatable(Type == TowerType.Coolant ? "jade.scp_unleashed.fission_tower.cools" : "jade.scp_unleashed.fission_tower.feeds", Info.getInt("Feeds"))
                        .withStyle(Info.getInt("Feeds") > 0 ? ChatFormatting.GRAY : ChatFormatting.GOLD));
                if (Info.getBoolean("Dry")) {
                    Tooltip.add(Component.translatable("jade.scp_unleashed.fission_tower.dry").withStyle(ChatFormatting.RED));
                }
                String Outlet = Info.getBoolean("Blow") ? "blowing" : Info.getBoolean("Vent") ? "vented" : "open";
                Tooltip.add(Component.translatable("jade.scp_unleashed.fission_tower.outlet." + Outlet).withStyle(Info.getBoolean("Blow") ? ChatFormatting.GOLD : ChatFormatting.GRAY));
                if (Section.isHighest()) {
                    Tooltip.add(Component.translatable("jade.scp_unleashed.fission_tower.steam_port").withStyle(ChatFormatting.DARK_GRAY));
                }
            }
        }
    }
}
