package net.rai.scp_unleashed.reactor;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.reactor.core.CoreTower;

import java.util.Locale;

public final class TowerStatus {
    private TowerStatus() {
    }

    public static Component line(Level World, BlockPos Pos) {
        if (!(World instanceof ServerLevel ServerWorld)) {
            return Component.empty();
        }
        ReactorGrid Grid = ReactorGridManager.get(ServerWorld).gridAt(ServerWorld, Pos);
        GridTower Tower = Grid == null ? null : Grid.towerAt(Pos);
        if (Grid == null || Tower == null) {
            return Component.translatable("message.scp_unleashed.reactor.no_grid").withStyle(ChatFormatting.GRAY);
        }
        if (Grid.getState() != GridState.Ready) {
            return Component.translatable(Grid.getState().getTranslationKey()).withStyle(Grid.getState().getColour());
        }
        CoreTower Data = Tower.Core;
        MutableComponent Text = Component.translatable("block.scp_unleashed." + Data.Type.getName()).append(" x" + Data.Height + ": ");
        switch (Data.Type) {
            case Fuel -> {
                TowerHeat Heat = TowerHeat.of(Data.Temperature, Data.Integrity);
                Text.append(Component.translatable("message.scp_unleashed.fuel_tower.status", Math.round(Data.Temperature), Data.loadedLevels(), Data.Height,
                        Math.round(Data.withdrawal() * 100.0f)).withStyle(Heat.getColour()));
            }
            case Rods -> Text.append(Component.translatable("message.scp_unleashed.control_rod_tower.status", Math.round(Data.Insertion), Math.round(Data.Target))
                    .withStyle(Data.Forced ? ChatFormatting.RED : ChatFormatting.AQUA));
            case Booster -> Text.append(Component.translatable("message.scp_unleashed.booster_tower.status", Math.round(Data.strength() * 100.0f), Data.RodContacts.size(),
                    Math.round(BoosterLinks.gate(Data) * 100.0f)).withStyle(Data.BoostPassed > 0.001f ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY));
            default -> Text.append(Component.translatable("message.scp_unleashed.steam_tower.status", Math.round(Data.Water), Data.waterCapacity(),
                    String.format(Locale.ROOT, "%.1f", Data.SteamMade)).withStyle(Data.Dry ? ChatFormatting.GOLD : ChatFormatting.AQUA));
        }
        return Text;
    }
}
