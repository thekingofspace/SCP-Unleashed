package net.rai.scp_unleashed.storage.client;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.rai.scp_unleashed.storage.StorageTags;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

public final class StorageLayouts {
    public static final float PresetSpin = 1.5f;
    public static final float PresetBobHeight = 0.75f;
    public static final float PresetBobPeriod = 50.0f;
    public static final float PresetFloatLift = 1.5f;
    private static final List<Entry> Entries = new CopyOnWriteArrayList<>();

    private StorageLayouts() {
    }

    public static void register(Predicate<ItemStack> Matches, StorageLayout Layout) {
        Entries.add(0, new Entry(Matches, Layout));
    }

    public static void register(ItemLike Subject, StorageLayout Layout) {
        Item Wanted = Subject.asItem();
        register(Stack -> Stack.is(Wanted), Layout);
    }

    public static void register(TagKey<Item> Tag, StorageLayout Layout) {
        register(Stack -> Stack.is(Tag), Layout);
    }

    public static StorageLayout forStack(ItemStack Stack, boolean Tall) {
        if (Stack.isEmpty()) {
            return InteriorLayout.pedestal(Tall);
        }
        for (Entry Candidate : Entries) {
            if (Candidate.Layout().fits(Tall) && Candidate.Matches().test(Stack)) {
                return Candidate.Layout();
            }
        }
        return preset(Stack, Tall);
    }

    public static InteriorLayout preset(ItemStack Stack, boolean Tall) {
        InteriorLayout Layout = Stack.is(StorageTags.Large) ? InteriorLayout.large(Tall)
                : Stack.is(StorageTags.NoPlatform) ? InteriorLayout.floor(Tall) : InteriorLayout.pedestal(Tall);
        LayoutMotion Motion = Layout.Motion();
        if (Stack.is(StorageTags.Spin)) {
            Motion = Motion.withSpin(PresetSpin);
        }
        if (Stack.is(StorageTags.Floating)) {
            Motion = Motion.withBob(PresetBobHeight, PresetBobPeriod);
            ItemPose Lifted = Layout.Stance().withFlat(Layout.Stance().Flat().moved(0.0f, PresetFloatLift, 0.0f))
                    .withSolid(Layout.Stance().Solid().moved(0.0f, PresetFloatLift, 0.0f));
            Layout = Layout.withStance(Lifted);
        }
        return Motion == Layout.Motion() ? Layout : Layout.withMotion(Motion);
    }

    private record Entry(Predicate<ItemStack> Matches, StorageLayout Layout) {
    }
}
