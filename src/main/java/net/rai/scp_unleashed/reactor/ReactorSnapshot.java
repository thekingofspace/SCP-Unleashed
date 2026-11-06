package net.rai.scp_unleashed.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.rai.scp_unleashed.reactor.core.CoreTower;
import net.rai.scp_unleashed.reactor.core.ReactorCore;

import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ReactorSnapshot {
    private ReactorSnapshot() {
    }

    public static CompoundTag build(ServerLevel World, ReactorControllerBlockEntity Controller) {
        CompoundTag Data = new CompoundTag();
        Data.putBoolean("Linked", Controller.isLinked());
        ReactorGrid Grid = Controller.resolve(World);
        if (Grid == null) {
            Data.putByte("State", (byte) GridState.Missing.ordinal());
            return Data;
        }
        BlockPos Origin = Controller.getBlockPos();
        ReactorCore Core = Grid.getCore();
        Data.putByte("State", (byte) Grid.getState().ordinal());
        Data.putBoolean("Scram", Grid.isScramHeld(World.getGameTime()));
        Data.putFloat("Heat", Core.totalHeat());
        Data.putFloat("Steam", Core.totalSteam());
        Data.putFloat("Hottest", Core.hottest());
        Data.putFloat("Weakest", Core.weakest());
        ListTag Towers = new ListTag();
        Map<CoreTower, Integer> Indices = new IdentityHashMap<>();
        for (GridTower Tower : Grid.getTowers()) {
            Indices.put(Tower.Core, Indices.size());
        }
        for (GridTower Tower : Grid.getTowers()) {
            CoreTower Info = Tower.Core;
            CompoundTag Entry = new CompoundTag();
            Entry.putByte("T", (byte) Info.Type.ordinal());
            Entry.putShort("X", (short) (Info.X - Origin.getX()));
            Entry.putShort("Y", (short) (Info.Bottom - Origin.getY()));
            Entry.putShort("Z", (short) (Info.Z - Origin.getZ()));
            Entry.putByte("H", (byte) Info.Height);
            switch (Info.Type) {
                case Fuel -> {
                    Entry.putShort("Temp", (short) Math.round(Math.min(Short.MAX_VALUE, Info.Temperature)));
                    Entry.putByte("Int", (byte) Math.round(Info.Integrity));
                    Entry.putByte("Out", (byte) Math.round(Info.withdrawal() * 100.0f));
                    Entry.putFloat("Made", Info.HeatMade);
                    Entry.putFloat("Cool", Info.HeatRemoved);
                    Entry.putByte("Rods", (byte) Info.loadedLevels());
                    Entry.putByte("Spent", (byte) Tower.spentRods());
                    Entry.putByte("Left", (byte) Tower.fuelPercent());
                    Entry.putInt("Minutes", Tower.fuelMinutes());
                    Entry.putByte("Gas", (byte) Math.round(Info.gasCapacity() <= 0 ? 0 : Info.Gas * 100.0f / Info.gasCapacity()));
                    Entry.putBoolean("Leak", Tower.Leaking);
                    Entry.putBoolean("Vent", Tower.Vented || Tower.Captured);
                    Entry.putByte("Feeds", (byte) Info.RodContacts.size());
                    Entry.putByte("Coolers", (byte) Info.CoolantContacts.size());
                    Entry.putByte("Boost", (byte) Math.round(Info.boost() * 100.0f));
                    Set<Integer> Boosters = new LinkedHashSet<>();
                    for (CoreTower.Contact Rod : Info.RodContacts) {
                        for (CoreTower.Contact Link : Rod.Other().BoosterContacts) {
                            if (Math.max(Rod.Low(), Link.Low()) <= Math.min(Rod.High(), Link.High())) {
                                Boosters.add(Indices.getOrDefault(Link.Other(), -1));
                            }
                        }
                    }
                    Boosters.remove(-1);
                    Entry.putIntArray("Links", Boosters.stream().mapToInt(Integer::intValue).toArray());
                }
                case Rods -> {
                    Entry.putByte("Ins", (byte) Math.round(Info.Insertion));
                    Entry.putByte("Tgt", (byte) Math.round(Info.Target));
                    Entry.putBoolean("Forced", Info.Forced);
                    Entry.putBoolean("Moving", Info.Moving);
                    Entry.putByte("Feeds", (byte) Info.FuelContacts.size());
                    Entry.putByte("Boosters", (byte) Info.BoosterContacts.size());
                    Entry.putByte("Pass", (byte) Math.round(BoosterLinks.rodPass(Info) * 100.0f));
                    Entry.putIntArray("Links", links(Info.BoosterContacts, Indices));
                }
                case Booster -> {
                    Entry.putByte("Str", (byte) Math.round(Info.strength() * 100.0f));
                    Entry.putByte("Gate", (byte) Math.round(BoosterLinks.gate(Info) * 100.0f));
                    Entry.putByte("Rods", (byte) Info.RodContacts.size());
                    Entry.putByte("Reach", (byte) BoosterLinks.reachedFuel(Info));
                    Entry.putBoolean("Live", Info.BoostPassed > 0.001f);
                    Entry.putIntArray("Links", links(Info.RodContacts, Indices));
                }
                default -> {
                    Entry.putInt("Water", Math.round(Info.Water));
                    Entry.putInt("Cap", Info.waterCapacity());
                    Entry.putFloat("Made", Info.SteamMade);
                    Entry.putBoolean("Dry", Info.Dry);
                    Entry.putBoolean("Blow", Tower.Blowing);
                    Entry.putBoolean("Vent", Tower.Vented || Tower.Captured);
                    Entry.putByte("Feeds", (byte) Info.FuelContacts.size());
                }
            }
            Towers.add(Entry);
        }
        Data.put("Towers", Towers);
        return Data;
    }

    private static int[] links(List<CoreTower.Contact> Contacts, Map<CoreTower, Integer> Indices) {
        Set<Integer> Found = new LinkedHashSet<>();
        for (CoreTower.Contact Contact : Contacts) {
            Found.add(Indices.getOrDefault(Contact.Other(), -1));
        }
        Found.remove(-1);
        return Found.stream().mapToInt(Integer::intValue).toArray();
    }
}
