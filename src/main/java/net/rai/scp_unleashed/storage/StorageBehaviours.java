package net.rai.scp_unleashed.storage;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

public final class StorageBehaviours {
    private static final List<Entry> Entries = new CopyOnWriteArrayList<>();

    private StorageBehaviours() {
    }

    public static void register(Predicate<ItemStack> Matches, ContainedBehaviour Behaviour) {
        Entries.add(0, new Entry(Matches, Behaviour));
    }

    public static void register(ItemLike Subject, ContainedBehaviour Behaviour) {
        register(Stack -> Stack.is(Subject.asItem()), Behaviour);
    }

    public static Optional<ContainedBehaviour> find(ItemStack Stack) {
        if (Stack.isEmpty()) {
            return Optional.empty();
        }
        for (Entry Candidate : Entries) {
            if (Candidate.Matches().test(Stack)) {
                return Optional.of(Candidate.Behaviour());
            }
        }
        return Optional.empty();
    }

    private record Entry(Predicate<ItemStack> Matches, ContainedBehaviour Behaviour) {
    }
}
