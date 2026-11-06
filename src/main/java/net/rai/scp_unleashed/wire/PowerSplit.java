package net.rai.scp_unleashed.wire;

import java.util.List;

public record PowerSplit(long[] Outputs, long[] Inputs, long Supply, long Demand, long Backup, long StorageIn, long SinkWeight,
                         long Matched, long FromBackup, long ToStorage, long Sunk, long Stored, long Capacity) {
    public static PowerSplit run(List<? extends WireDevice> Devices) {
        int Count = Devices.size();
        long[] Offers = new long[Count];
        long[] Reserves = new long[Count];
        long[] Demands = new long[Count];
        long[] StorageOffers = new long[Count];
        long[] StorageDemands = new long[Count];
        long[] Sinks = new long[Count];
        long Stored = 0;
        long Capacity = 0;
        for (int Index = 0; Index < Count; Index++) {
            WireDevice Device = Devices.get(Index);
            if (Device.isSink()) {
                Sinks[Index] = 1;
                continue;
            }
            long Offer = Math.max(0, Device.getPowerOutput());
            long Want = Math.max(0, Device.getPowerDemand());
            if (Device.isStorage()) {
                StorageOffers[Index] = Offer;
                StorageDemands[Index] = Want;
            } else {
                Offers[Index] = Offer;
                Reserves[Index] = Math.max(0, Device.getReserveOutput());
                Demands[Index] = Want;
            }
            if (Device.isStorage() || Device.isGenerator()) {
                Stored += Math.max(0, Device.getStoredPower());
                Capacity += Math.max(0, Device.getPowerCapacity());
            }
        }
        PowerSplit Split = solve(Offers, Reserves, Demands, StorageOffers, StorageDemands, Sinks, Stored, Capacity);
        for (int Index = 0; Index < Count; Index++) {
            WireDevice Device = Devices.get(Index);
            if (Split.Outputs[Index] > 0) {
                Device.extractPower(toInt(Split.Outputs[Index]));
            }
            if (Split.Inputs[Index] > 0) {
                Device.insertPower(toInt(Split.Inputs[Index]));
            }
        }
        long Available = Split.getAvailable();
        for (WireDevice Device : Devices) {
            Device.observeNetwork(Available);
        }
        return Split;
    }

    public static PowerSplit solve(long[] Offers, long[] Reserves, long[] Demands, long[] StorageOffers, long[] StorageDemands, long[] Sinks, long Stored, long Capacity) {
        int Count = Offers.length;
        long[] BackupOffers = new long[Count];
        for (int Index = 0; Index < Count; Index++) {
            BackupOffers[Index] = Reserves[Index] + StorageOffers[Index];
        }
        long Supply = sum(Offers);
        long Demand = sum(Demands);
        long Backup = sum(BackupOffers);
        long StorageIn = sum(StorageDemands);
        long SinkWeight = sum(Sinks);
        long Matched = Math.min(Supply, Demand);
        long FromBackup = Math.min(Demand - Matched, Backup);
        long Spare = Supply - Matched;
        long ToStorage = Math.min(Spare, StorageIn);
        long Sunk = SinkWeight > 0 ? Spare - ToStorage : 0;
        long[] FromGenerators = share(Matched + ToStorage + Sunk, Offers, Supply);
        long[] FromStored = share(FromBackup, BackupOffers, Backup);
        long[] ToConsumers = share(Matched + FromBackup, Demands, Demand);
        long[] IntoStorage = share(ToStorage, StorageDemands, StorageIn);
        long[] IntoSinks = spread(Sunk, Sinks, SinkWeight);
        long[] Outputs = new long[Count];
        long[] Inputs = new long[Count];
        for (int Index = 0; Index < Count; Index++) {
            Outputs[Index] = FromGenerators[Index] + FromStored[Index];
            Inputs[Index] = ToConsumers[Index] + IntoStorage[Index] + IntoSinks[Index];
        }
        return new PowerSplit(Outputs, Inputs, Supply, Demand, Backup, StorageIn, SinkWeight, Matched, FromBackup, ToStorage, Sunk, Stored, Capacity);
    }

    public long getAvailable() {
        return Supply + Backup;
    }

    public long getShortfall() {
        return Demand - Matched - FromBackup;
    }

    public long getDelivered() {
        return Matched + FromBackup;
    }

    private static long sum(long[] Values) {
        long Total = 0;
        for (long Value : Values) {
            Total += Value;
        }
        return Total;
    }

    private static int toInt(long Value) {
        return (int) Math.min(Integer.MAX_VALUE, Value);
    }

    static long[] share(long Total, long[] Weights, long WeightSum) {
        long[] Shares = new long[Weights.length];
        if (Total <= 0 || WeightSum <= 0) {
            return Shares;
        }
        long Assigned = 0;
        for (int Index = 0; Index < Weights.length; Index++) {
            Shares[Index] = Math.min(Weights[Index], (long) Math.floor((double) Weights[Index] * Total / WeightSum));
            Assigned += Shares[Index];
        }
        for (int Index = 0; Index < Weights.length && Assigned < Total; Index++) {
            long Room = Math.min(Weights[Index] - Shares[Index], Total - Assigned);
            Shares[Index] += Room;
            Assigned += Room;
        }
        for (int Index = Weights.length - 1; Index >= 0 && Assigned > Total; Index--) {
            long Excess = Math.min(Shares[Index], Assigned - Total);
            Shares[Index] -= Excess;
            Assigned -= Excess;
        }
        return Shares;
    }

    private static long[] spread(long Total, long[] Weights, long WeightSum) {
        long[] Shares = new long[Weights.length];
        if (Total <= 0 || WeightSum <= 0) {
            return Shares;
        }
        long Assigned = 0;
        for (int Index = 0; Index < Weights.length; Index++) {
            Shares[Index] = (long) Math.floor((double) Weights[Index] * Total / WeightSum);
            Assigned += Shares[Index];
        }
        for (int Index = 0; Index < Weights.length && Assigned < Total; Index++) {
            if (Weights[Index] > 0) {
                Shares[Index] += Total - Assigned;
                Assigned = Total;
            }
        }
        for (int Index = Weights.length - 1; Index >= 0 && Assigned > Total; Index--) {
            long Excess = Math.min(Shares[Index], Assigned - Total);
            Shares[Index] -= Excess;
            Assigned -= Excess;
        }
        return Shares;
    }
}
