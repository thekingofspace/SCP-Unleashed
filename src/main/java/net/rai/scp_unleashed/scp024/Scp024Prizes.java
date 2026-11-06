package net.rai.scp_unleashed.scp024;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

public final class Scp024Prizes {
    public static final int JackpotChance = 12;
    public static final int Jackpot = 24;

    public enum Prize {
        DollHairs("doll_hairs", 30, () -> Scp024Registry.DollHairs.get()),
        Dirt("dirt", 10, () -> Items.DIRT),
        Stick("stick", 10, () -> Items.STICK),
        Seed("seed", 10, () -> Items.WHEAT_SEEDS),
        DeadBush("dead_bush", 10, () -> Items.DEAD_BUSH),
        RottenFlesh("rotten_flesh", 10, () -> Items.ROTTEN_FLESH),
        Pebble("pebble", 10, () -> Items.FLINT),
        Ribbon("ribbon", 10, () -> Scp024Registry.Ribbon.get());

        public final String Id;
        public final int Weight;
        private final Supplier<Item> Kind;

        Prize(String Id, int Weight, Supplier<Item> Kind) {
            this.Id = Id;
            this.Weight = Weight;
            this.Kind = Kind;
        }

        public Item item() {
            return Kind.get();
        }
    }

    public record Award(Prize Kind, int Count) {
        public ItemStack stack() {
            return new ItemStack(Kind.item(), Count);
        }

        public Component name() {
            return Component.translatable("prize.scp_unleashed.scp024." + Kind.Id, Count);
        }

        public Component joke() {
            return Component.translatable("prize.scp_unleashed.scp024." + Kind.Id + ".joke");
        }

        public String encode() {
            return Kind.name() + ":" + Count;
        }

        public static Award decode(String Text) {
            String[] Parts = Text.split(":");
            try {
                return new Award(Prize.valueOf(Parts[0]), Math.max(1, Integer.parseInt(Parts[1])));
            } catch (RuntimeException Problem) {
                return new Award(Prize.DollHairs, 1);
            }
        }
    }

    private Scp024Prizes() {
    }

    public static Award roll(RandomSource Random) {
        int Total = 0;
        for (Prize Kind : Prize.values()) {
            Total += Kind.Weight;
        }
        int Pick = Random.nextInt(Total);
        for (Prize Kind : Prize.values()) {
            Pick -= Kind.Weight;
            if (Pick < 0) {
                int Count = Kind == Prize.DollHairs ? (Random.nextInt(JackpotChance) == 0 ? Jackpot : 1 + Random.nextInt(5)) : 1;
                return new Award(Kind, Count);
            }
        }
        return new Award(Prize.DollHairs, 1);
    }

    public static Award grand() {
        return new Award(Prize.Ribbon, 1);
    }

    public static boolean give(ServerPlayer Winner, Award Won) {
        ItemStack Stack = Won.stack();
        boolean Kept = Winner.getInventory().add(Stack);
        if (Kept) {
            Winner.inventoryMenu.broadcastChanges();
        }
        return Kept;
    }

    public static void confetti(ServerLevel World, double X, double Y, double Z, int Count) {
        World.sendParticles(Scp024Registry.Confetti.get(), X, Y, Z, Count, 1.2, 0.4, 1.2, 0.08);
        World.sendParticles(ParticleTypes.FIREWORK, X, Y, Z, Count / 4, 0.6, 0.3, 0.6, 0.05);
        Scp024Sounds.play(World, new Vec3(X, Y, Z), Scp024Registry.ConfettiPop.get(), 0.9f, 1.0f + World.random.nextFloat() * 0.2f);
    }
}
