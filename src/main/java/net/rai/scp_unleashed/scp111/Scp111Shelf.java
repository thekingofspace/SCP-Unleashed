package net.rai.scp_unleashed.scp111;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.storage.ContainedBehaviour;
import net.rai.scp_unleashed.storage.StorageChange;
import net.rai.scp_unleashed.storage.StorageContainerBlockEntity;
import net.rai.scp_unleashed.storage.StorageEnvironment;
import net.rai.scp_unleashed.storage.StorageGeometry;
import net.rai.scp_unleashed.storage.StorageView;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class Scp111Shelf implements ContainedBehaviour {
    public static final Scp111Shelf Instance = new Scp111Shelf();
    public static final int SmallCapacity = 16;
    public static final int TallCapacity = 32;
    public static final int CountInterval = 20;
    public static final int StirInterval = 80;
    public static final int HeatedRate = 2;
    public static final double LowerShelfY = 5.0;
    public static final double UpperShelfY = 18.0;
    public static final double BoxSpillY = 7.0;
    public static final double SpillZ = -2.5;
    public static final double SpillOut = 0.16;
    public static final double SpillUp = 0.16;
    public static final double SeamZ = -0.4;

    private Scp111Shelf() {
    }

    public static boolean handles(ItemStack Stack) {
        return Scp111EggItem.is(Stack) || Scp111BoxItem.is(Stack);
    }

    public static int capacity(StorageView Locker) {
        return Locker.isTall() ? TallCapacity : SmallCapacity;
    }

    public static int rate(StorageEnvironment Environment) {
        return switch (Environment) {
            case Cold -> 0;
            case Heated -> HeatedRate;
            default -> 1;
        };
    }

    public static ItemStack shelfStack(Scp111Clutch Clutch) {
        ItemStack Made = new ItemStack(Scp111Registry.EggItem.get());
        Made.set(Scp111Registry.ClutchData.get(), Clutch);
        return Made;
    }

    @Nullable
    @Override
    public ItemStack absorb(StorageView Locker, ItemStack Stored, ItemStack Held, Player User) {
        if (!Scp111EggItem.is(Held) || Scp111EggItem.isShelf(Held) || !Stored.isEmpty() && !Scp111EggItem.is(Stored)) {
            return null;
        }
        Scp111Clutch Current = Scp111Clutch.of(Stored);
        int Space = capacity(Locker) - Current.size();
        if (Space <= 0) {
            return null;
        }
        int Moved = Math.min(Space, Held.getCount());
        Scp111Clutch Grown = Current.plus(Scp111Egg.of(Held).entry(), Moved);
        Held.consume(Moved, User);
        return shelfStack(Grown);
    }

    @Override
    public List<ItemStack> unpack(ItemStack Stored) {
        if (Scp111EggItem.isShelf(Stored)) {
            return Scp111Clutch.of(Stored).eggStacks();
        }
        return List.of(Stored);
    }

    @Nullable
    @Override
    public Split takeOne(ItemStack Stored) {
        if (!Scp111EggItem.isShelf(Stored)) {
            return null;
        }
        List<Scp111Clutch.Entry> Eggs = Scp111Clutch.of(Stored).Eggs();
        if (Eggs.size() <= 1) {
            return null;
        }
        Scp111Clutch.Entry Last = Eggs.get(Eggs.size() - 1);
        return new Split(Last.egg().stack(1), shelfStack(new Scp111Clutch(Eggs.subList(0, Eggs.size() - 1))));
    }

    @Override
    public void tick(ServerLevel World, StorageContainerBlockEntity Locker) {
        long Clock = World.getGameTime() + Math.floorMod(Locker.getStoragePos().hashCode(), CountInterval);
        if (Clock % CountInterval != 0L) {
            return;
        }
        ItemStack Stored = Locker.peekContents();
        Scp111Clutch Current = Scp111Clutch.of(Stored);
        if (Current.isEmpty()) {
            return;
        }
        int Rate = rate(Locker.getEnvironment());
        if (Rate > 0) {
            Scp111Clutch Aged = Current.aged(CountInterval * Rate);
            if (Scp111EggItem.is(Stored) && !Scp111EggItem.isShelf(Stored)) {
                Locker.setContents(shelfStack(Aged));
            } else if (visiblyChanged(Current, Aged)) {
                ItemStack Synced = Stored.copy();
                Synced.set(Scp111Registry.ClutchData.get(), Aged);
                Locker.setContents(Synced);
            } else {
                Stored.set(Scp111Registry.ClutchData.get(), Aged);
                Locker.setChanged();
            }
            Current = Aged;
        }
        if (Current.due() == 0) {
            return;
        }
        if (Locker.isOpen()) {
            spill(World, Locker);
        } else if (Clock % StirInterval == 0L) {
            stir(World, Locker);
        }
    }

    @Override
    public void changed(ServerLevel World, StorageContainerBlockEntity Locker, StorageChange Change, ItemStack Subject) {
        if (Change == StorageChange.Opened && Scp111Clutch.of(Locker.peekContents()).due() > 0) {
            spill(World, Locker);
        }
    }

    private static boolean visiblyChanged(Scp111Clutch Before, Scp111Clutch After) {
        List<Scp111Clutch.Entry> Old = Before.Eggs();
        List<Scp111Clutch.Entry> New = After.Eggs();
        for (int Index = 0; Index < Math.min(Old.size(), New.size()); Index++) {
            if (Old.get(Index).egg().Stage() != New.get(Index).egg().Stage() || Old.get(Index).isDue() != New.get(Index).isDue()) {
                return true;
            }
        }
        return false;
    }

    public static boolean isStirring(StorageContainerBlockEntity Locker) {
        return handles(Locker.peekContents()) && Scp111Clutch.of(Locker.peekContents()).due() > 0;
    }

    public static int ripen(StorageContainerBlockEntity Locker) {
        ItemStack Stored = Locker.peekContents();
        if (!handles(Stored)) {
            return 0;
        }
        Scp111Clutch Current = Scp111Clutch.of(Stored);
        if (Current.isEmpty()) {
            return 0;
        }
        if (Scp111BoxItem.is(Stored)) {
            ItemStack Ripe = Stored.copy();
            Ripe.set(Scp111Registry.ClutchData.get(), Current.dueNow());
            Locker.setContents(Ripe);
        } else {
            Locker.setContents(shelfStack(Current.dueNow()));
        }
        return Current.size();
    }

    public static int spill(ServerLevel World, StorageContainerBlockEntity Locker) {
        ItemStack Stored = Locker.peekContents();
        Scp111Clutch Current = Scp111Clutch.of(Stored);
        if (Current.due() == 0) {
            return 0;
        }
        boolean Boxed = Scp111BoxItem.is(Stored);
        BlockPos Pos = Locker.getStoragePos();
        Direction Facing = Locker.getFacing();
        RandomSource Random = World.random;
        List<Scp111Clutch.Entry> All = Current.Eggs();
        int Born = 0;
        Scp111Variant Last = Scp111Variant.Ember;
        for (int Index = 0; Index < All.size(); Index++) {
            Scp111Clutch.Entry Egg = All.get(Index);
            if (!Egg.isDue()) {
                continue;
            }
            double Height = Boxed ? BoxSpillY : Index >= SmallCapacity ? UpperShelfY : LowerShelfY;
            Vec3 Spot = StorageGeometry.point(Pos, Facing, 3.5 + Random.nextDouble() * 9.0, Height, SpillZ);
            Vec3 Push = StorageGeometry.motion(Facing, (Random.nextDouble() - 0.5) * 0.12, SpillUp + Random.nextDouble() * 0.06, -(SpillOut + Random.nextDouble() * 0.08));
            if (Scp111Hatching.hatchling(World, Spot, Egg.Variant(), Push, Pos) != null) {
                Born++;
                Last = Egg.Variant();
            }
        }
        Vec3 Door = StorageGeometry.point(Pos, Facing, StorageGeometry.CentreX, Locker.isTall() ? 12.0 : 6.0, SpillZ);
        Scp111Hatching.shellBurst(World, Door, Last, 8 + Math.min(24, Born * 2));
        Scp111Breath.puff(World, Door, StorageGeometry.motion(Facing, 0.0, 0.4, -1.0).normalize(), Last, 6, 0.8f);
        Scp111Snail.playLayers(World, Door, Scp111Registry.SpillLayers, 1.0f, 0.95f + Random.nextFloat() * 0.1f);
        Scp111Clutch Rest = Current.waiting();
        if (Rest.isEmpty()) {
            if (Boxed) {
                World.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.PAPER)), Door.x, Door.y, Door.z, 12, 0.2, 0.15, 0.2, 0.05);
            }
            Locker.setContents(ItemStack.EMPTY);
        } else if (Boxed) {
            ItemStack Left = Stored.copy();
            Left.set(Scp111Registry.ClutchData.get(), Rest);
            Locker.setContents(Left);
        } else {
            Locker.setContents(shelfStack(Rest));
        }
        return Born;
    }

    private static void stir(ServerLevel World, StorageContainerBlockEntity Locker) {
        RandomSource Random = World.random;
        double Top = Locker.isTall() ? 28.0 : 12.0;
        Vec3 Seam = StorageGeometry.point(Locker.getStoragePos(), Locker.getFacing(), 2.0 + Random.nextDouble() * 12.0, 3.0 + Random.nextDouble() * (Top - 3.0), SeamZ);
        World.sendParticles(ParticleTypes.SMOKE, Seam.x, Seam.y, Seam.z, 2, 0.02, 0.03, 0.02, 0.004);
        Vec3 Where = Locker.soundCentre();
        World.playSound(null, Where.x, Where.y, Where.z, Scp111Registry.Stir.get(), SoundSource.BLOCKS, 0.5f, 0.85f + Random.nextFloat() * 0.2f);
    }
}
