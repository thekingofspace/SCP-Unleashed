package net.rai.scp_unleashed.scp610;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.rai.scp_unleashed.faction.FactionState;
import net.rai.scp_unleashed.raid.RaidWorld;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public final class Scp610Pact {
    public static final String PactKey = "Pact";
    public static final String SurgeUntilKey = "SurgeUntil";
    public static final String SurgeScaleKey = "SurgeScale";
    public static final long TributeTicks = 72000L;
    public static final long GraceTicks = 24000L;
    public static final long BreakWindow = 24000L;
    public static final int BreakLimit = 32;
    public static final int TributeMeat = 16;
    public static final long BrokenSurgeTicks = 48000L;
    public static final double BrokenSurge = 2.0;
    public static final long RaidSoon = 1200L;
    public static final int WarnReach = 64;
    public static final List<Item> Meats = List.of(Items.BEEF, Items.PORKCHOP, Items.MUTTON, Items.CHICKEN, Items.RABBIT, Items.ROTTEN_FLESH);

    public enum Breach {
        Tribute,
        Lord,
        Flesh,
        Hive
    }

    private Scp610Pact() {
    }

    public static CompoundTag data(FactionState State) {
        CompoundTag Extra = State.extra();
        if (!Extra.contains(PactKey)) {
            Extra.put(PactKey, new CompoundTag());
        }
        return Extra.getCompound(PactKey);
    }

    public static boolean active(FactionState State) {
        return State.extra().contains(PactKey) && State.extra().getCompound(PactKey).getBoolean("Active");
    }

    public static boolean forbidden(FactionState State) {
        return State.extra().contains(PactKey) && State.extra().getCompound(PactKey).getBoolean("Forbidden");
    }

    public static long nextTribute(FactionState State) {
        return data(State).getLong("NextTribute");
    }

    @Nullable
    public static UUID signer(FactionState State) {
        CompoundTag Pact = data(State);
        return Pact.hasUUID("Signer") ? Pact.getUUID("Signer") : null;
    }

    public static boolean make(FactionState State, Player Signer, long Now) {
        if (active(State) || forbidden(State) || !State.phase().active()) {
            return false;
        }
        CompoundTag Pact = data(State);
        Pact.putBoolean("Active", true);
        Pact.putUUID("Signer", Signer.getUUID());
        Pact.putLong("Since", Now);
        Pact.putLong("NextTribute", Now + TributeTicks);
        Pact.putLong("Paid", Now);
        Pact.putLong("BreakStart", Now);
        Pact.putInt("BreakCount", 0);
        State.changed();
        return true;
    }

    public static void pay(FactionState State, long Now) {
        CompoundTag Pact = data(State);
        Pact.putLong("NextTribute", Now + TributeTicks);
        Pact.putLong("Paid", Now);
        State.changed();
    }

    public static boolean sated(FactionState State, long Now) {
        return active(State) && Now - data(State).getLong("Paid") < 24000L;
    }

    public static int meatIn(Player User) {
        int Count = 0;
        for (ItemStack Stack : User.getInventory().items) {
            if (Meats.contains(Stack.getItem())) {
                Count += Stack.getCount();
            }
        }
        return Count;
    }

    public static boolean offerMeat(FactionState State, Player User, long Now) {
        if (!active(State) || meatIn(User) < TributeMeat && !User.isCreative()) {
            return false;
        }
        int Left = TributeMeat;
        for (ItemStack Stack : User.getInventory().items) {
            if (Left <= 0) {
                break;
            }
            if (Meats.contains(Stack.getItem())) {
                int Taken = Math.min(Left, Stack.getCount());
                Stack.shrink(Taken);
                Left -= Taken;
            }
        }
        pay(State, Now);
        return true;
    }

    public static double surgeScale(FactionState State, long Now) {
        CompoundTag Extra = State.extra();
        if (Extra.getLong(SurgeUntilKey) > Now) {
            return Math.max(1.0, Extra.getDouble(SurgeScaleKey));
        }
        return 1.0;
    }

    public static void surge(FactionState State, long Now, long Ticks, double Scale) {
        CompoundTag Extra = State.extra();
        Extra.putLong(SurgeUntilKey, Math.max(Extra.getLong(SurgeUntilKey), Now + Ticks));
        Extra.putDouble(SurgeScaleKey, Math.max(surgeScale(State, Now), Scale));
        State.changed();
    }

    public static boolean breakPact(MinecraftServer Server, FactionState State, Breach Why) {
        if (!active(State)) {
            return false;
        }
        long Now = Server.overworld().getGameTime();
        CompoundTag Pact = data(State);
        Pact.putBoolean("Active", false);
        Pact.putString("Breach", Why.name());
        Pact.putLong("BrokenAt", Now);
        surge(State, Now, BrokenSurgeTicks, BrokenSurge);
        RaidWorld.get(Server).ledger(Scp610Faction.Instance.id(), State.era()).setNextRaid(Now + RaidSoon);
        State.changed();
        warn(Server, "dialogue.scp_unleashed.scp610.lord.broken");
        return true;
    }

    public static void forbid(FactionState State) {
        CompoundTag Pact = data(State);
        Pact.putBoolean("Active", false);
        Pact.putBoolean("Forbidden", true);
        State.changed();
    }

    public static void clear(FactionState State) {
        State.extra().remove(PactKey);
        State.extra().remove(SurgeUntilKey);
        State.extra().remove(SurgeScaleKey);
        State.changed();
    }

    public static void warn(MinecraftServer Server, String Key) {
        for (Scp610Hive Hive : Scp610Hives.list(Server)) {
            ServerLevel World = Server.getLevel(Hive.Site.dimension());
            if (World == null) {
                continue;
            }
            BlockPos Anchor = Hive.anchor();
            for (ServerPlayer User : World.players()) {
                if (User.blockPosition().distSqr(Anchor) <= (double) WarnReach * WarnReach) {
                    User.displayClientMessage(Component.translatable(Key).withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), true);
                    User.playNotifySound(Scp610Sounds.SpewerAmbient.get(), SoundSource.HOSTILE, 1.0f, 0.5f);
                }
            }
        }
    }

    public static void fleshBroken(MinecraftServer Server, FactionState State) {
        if (!active(State)) {
            return;
        }
        long Now = Server.overworld().getGameTime();
        CompoundTag Pact = data(State);
        if (Now - Pact.getLong("BreakStart") > BreakWindow) {
            Pact.putLong("BreakStart", Now);
            Pact.putInt("BreakCount", 0);
        }
        int Count = Pact.getInt("BreakCount") + 1;
        Pact.putInt("BreakCount", Count);
        State.changed();
        if (Count > BreakLimit) {
            breakPact(Server, State, Breach.Flesh);
        }
    }

    public static void tick(MinecraftServer Server, FactionState State) {
        if (!active(State)) {
            return;
        }
        long Now = Server.overworld().getGameTime();
        long Due = nextTribute(State);
        if (Now >= Due) {
            Scp610HiveLord Lord = Scp610Lord.lord(Server, State);
            AABB Room = Scp610Lord.bounds(State);
            if (Lord != null && Room != null) {
                List<Animal> Offered = Lord.level().getEntitiesOfClass(Animal.class, Room, Found -> Found.isAlive() && !Found.isBaby());
                if (!Offered.isEmpty() && Lord.level() instanceof ServerLevel World) {
                    Animal Taken = Offered.get(0);
                    Scp610Infection.cloud(World, Taken.position().add(0.0, Taken.getBbHeight() * 0.5, 0.0), 1.2, 30);
                    World.playSound(null, Taken.blockPosition(), Scp610Sounds.Merge.get(), SoundSource.HOSTILE, 1.0f, 0.7f);
                    Taken.discard();
                    pay(State, Now);
                    return;
                }
            }
        }
        if (Now > Due + GraceTicks) {
            breakPact(Server, State, Breach.Tribute);
        }
    }

    public static boolean isMeat(ItemStack Stack) {
        return Meats.contains(Stack.getItem()) || Stack.is(ItemTags.MEAT);
    }
}
