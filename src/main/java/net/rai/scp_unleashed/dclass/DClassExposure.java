package net.rai.scp_unleashed.dclass;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp012.Scp012Composer;
import net.rai.scp_unleashed.scp012.Scp012CompositionItem;
import net.rai.scp_unleashed.scp012.Scp012Containment;
import net.rai.scp_unleashed.scp012.Scp012Exposure;
import net.rai.scp_unleashed.scp012.Scp012Registry;
import net.rai.scp_unleashed.scp012.Scp012Sources;
import net.rai.scp_unleashed.storage.StorageContainerBlock;
import net.rai.scp_unleashed.storage.StorageContainerBlockEntity;
import org.jetbrains.annotations.Nullable;

public final class DClassExposure {
    public static final float SoundVolume = 0.5f;

    private boolean Exposed;
    private int MeterClock;
    private int DamageClock;
    private int Steps;
    private int Meter;
    private int Paper = -1;

    public boolean exposed() {
        return Exposed;
    }

    public int meter() {
        return Exposed ? Meter : 0;
    }

    public void forget() {
        Exposed = false;
        MeterClock = 0;
        DamageClock = 0;
        Steps = 0;
        Meter = 0;
        Paper = -1;
    }

    public void tick(DClass Owner) {
        if (!(Owner.level() instanceof ServerLevel World) || !Owner.isAlive()) {
            return;
        }
        if (Math.floorMod(Owner.tickCount + Owner.getId(), Scp012Composer.PaperInterval) == 0) {
            paper(World, Owner);
        }
        if (Math.floorMod(Owner.tickCount + Owner.getId(), Scp012Exposure.Cycle) != 0) {
            return;
        }
        Scp012Exposure.Sighting Found = find(World, Owner);
        if (Found == null) {
            if (Exposed) {
                Exposed = false;
                MeterClock = 0;
                Steps = 0;
            }
            return;
        }
        if (!Exposed) {
            Exposed = true;
            MeterClock = 0;
            DamageClock = Scp012Exposure.DamageInterval - Scp012Exposure.FirstDamageDelay;
        }
        MeterClock += Scp012Exposure.Cycle;
        DamageClock += Scp012Exposure.Cycle;
        int Current = Found.meter();
        if (MeterClock >= Scp012Exposure.MeterInterval) {
            MeterClock -= Scp012Exposure.MeterInterval;
            if (Current < Scp012CompositionItem.Complete) {
                Current = Found.advance(1);
                Steps++;
                if (Steps % Scp012Exposure.PageEvery == 0) {
                    play(World, Owner, Scp012Registry.Page.get(), 0.45f, 0.78f + Owner.getRandom().nextFloat() * 0.1f);
                }
            }
            if (Current >= Scp012CompositionItem.Complete) {
                claim(World, Owner);
            }
        }
        if (DamageClock >= Scp012Exposure.DamageInterval) {
            DamageClock -= Scp012Exposure.DamageInterval;
            drain(World, Owner);
        }
        Meter = Current;
    }

    @Nullable
    public static Scp012Exposure.Sighting find(ServerLevel World, DClass Owner) {
        for (InteractionHand Hand : InteractionHand.values()) {
            if (Scp012CompositionItem.is(Owner.getItemInHand(Hand))) {
                return new Scp012Exposure.Sighting(Owner, Hand, null);
            }
        }
        Vec3 Eye = Owner.getEyePosition();
        StorageContainerBlockEntity Best = null;
        double BestDistance = Double.MAX_VALUE;
        for (BlockPos Pos : Scp012Sources.near(World, Eye, Scp012Exposure.WindowRange)) {
            StorageContainerBlockEntity Locker = StorageContainerBlock.containerAt(World, Pos);
            if (Locker == null || !Scp012Containment.isExposed(Locker) || !Scp012CompositionItem.is(Locker.peekContents())) {
                continue;
            }
            double Distance = Scp012Exposure.sightDistance(World, Owner, Locker);
            if (Distance < BestDistance) {
                BestDistance = Distance;
                Best = Locker;
            }
        }
        return Best == null ? null : new Scp012Exposure.Sighting(Owner, null, Best);
    }

    private void claim(ServerLevel World, DClass Owner) {
        if (Scp012Composer.isComposer(Owner)) {
            return;
        }
        Owner.setData(Scp012Registry.Composer.get(), Boolean.TRUE);
        Paper = countPaper(Owner);
        World.playSound(null, Owner.getX(), Owner.getY() + 1.0, Owner.getZ(), Scp012Registry.Complete.get(), SoundSource.NEUTRAL, 0.8f, 0.92f);
        play(World, Owner, Scp012Registry.Note.get(), SoundVolume, 0.5f);
        play(World, Owner, Scp012Registry.NoteBell.get(), 0.4f, 0.53f);
    }

    private void paper(ServerLevel World, DClass Owner) {
        if (!Scp012Composer.isComposer(Owner)) {
            Paper = -1;
            return;
        }
        int Count = countPaper(Owner);
        if (Paper >= 0 && Count > Paper && consumePaper(Owner)) {
            ItemStack Left = Owner.insert(Scp012CompositionItem.blank());
            if (!Left.isEmpty()) {
                Owner.spawnAtLocation(Left);
            }
            World.playSound(null, Owner.getX(), Owner.getY() + 1.0, Owner.getZ(), Scp012Registry.Copy.get(), SoundSource.NEUTRAL, 0.6f, 0.9f + Owner.getRandom().nextFloat() * 0.1f);
            play(World, Owner, Scp012Registry.Note.get(), 0.35f, 0.59f + Owner.getRandom().nextInt(4) * 0.06f);
            Count = countPaper(Owner);
        }
        Paper = Count;
    }

    private static void drain(ServerLevel World, DClass Owner) {
        if (Owner.hurt(Owner.damageSources().source(Scp012Registry.CompositionDamage), Scp012Exposure.DamageAmount)) {
            float Pitch = 0.5f + Owner.getRandom().nextInt(3) * 0.03f;
            play(World, Owner, Scp012Registry.Toll.get(), 0.55f, Pitch);
            play(World, Owner, Scp012Registry.TollRing.get(), 0.3f, Pitch * 1.06f);
        }
    }

    public static int countPaper(DClass Owner) {
        int Count = 0;
        for (int Slot = 0; Slot < DClass.MainSize; Slot++) {
            ItemStack Stack = Owner.main().get(Slot);
            if (Stack.is(Items.PAPER)) {
                Count += Stack.getCount();
            }
        }
        ItemStack Spare = Owner.getOffhandItem();
        if (Spare.is(Items.PAPER)) {
            Count += Spare.getCount();
        }
        return Count;
    }

    private static boolean consumePaper(DClass Owner) {
        for (int Slot = 0; Slot < DClass.MainSize; Slot++) {
            ItemStack Stack = Owner.main().get(Slot);
            if (Stack.is(Items.PAPER)) {
                Stack.shrink(1);
                if (Stack.isEmpty()) {
                    Owner.main().set(Slot, ItemStack.EMPTY);
                }
                return true;
            }
        }
        ItemStack Spare = Owner.getOffhandItem();
        if (Spare.is(Items.PAPER)) {
            Spare.shrink(1);
            if (Spare.isEmpty()) {
                Owner.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            }
            return true;
        }
        return false;
    }

    private static void play(ServerLevel World, DClass Owner, SoundEvent Sound, float Volume, float Pitch) {
        World.playSound(null, Owner.getX(), Owner.getY() + 1.0, Owner.getZ(), Sound, SoundSource.NEUTRAL, Volume, Pitch);
    }
}
