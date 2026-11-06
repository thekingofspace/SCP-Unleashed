package net.rai.scp_unleashed.raid;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.faction.FactionControl;
import net.rai.scp_unleashed.faction.FactionState;
import net.rai.scp_unleashed.faction.FactionType;
import net.rai.scp_unleashed.faction.Factions;
import net.rai.scp_unleashed.raid.objective.RaidObjective;
import net.rai.scp_unleashed.discord.DiscordNotices;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record RaidContext(MinecraftServer Server, ServerLevel World, Raid Current, FactionType Faction, FactionState State, RaidStyle Style, RaidLedger Ledger,
                          long Now) {
    public RandomSource random() {
        return World.random;
    }

    public CompoundTag data() {
        return Current.extra();
    }

    public void changed() {
        Current.changed();
    }

    public int room() {
        int Own = Style.maxAlive() - Current.remaining();
        int Shared = RaidRules.GlobalRaiders - RaidWorld.get(Server).raiderCount();
        return Math.max(0, Math.min(Own, Shared));
    }

    public void enlist(Mob Raider, String Role) {
        if (!Factions.bondOf(Raider).bound()) {
            FactionControl.stamp(Faction, State, Raider);
        }
        Raider.setData(RaidRegistry.Mark, new RaidMark(Current.id(), Role));
        Raider.setPersistenceRequired();
        Current.enlist(Raider.getUUID(), RaidMark.Caster.equals(Role));
        attach(Raider);
    }

    public void attach(Mob Raider) {
        for (RaidObjective Objective : Style.objectives()) {
            Objective.attach(Current, Raider);
        }
    }

    @Nullable
    public Mob raider(UUID Id) {
        Entity Found = World.getEntity(Id);
        return Found instanceof Mob Raider && Raider.isAlive() ? Raider : null;
    }

    @Nullable
    public Mob caster() {
        UUID Id = Current.caster();
        return Id == null ? null : raider(Id);
    }

    public List<Mob> loadedRaiders() {
        List<Mob> Found = new ArrayList<>();
        for (UUID Id : Current.raiders()) {
            Mob Raider = raider(Id);
            if (Raider != null) {
                Found.add(Raider);
            }
        }
        return Found;
    }

    public List<ServerPlayer> defenders(double Reach) {
        List<ServerPlayer> Found = new ArrayList<>();
        BlockPos Target = Current.target();
        double Square = Reach * Reach;
        for (ServerPlayer User : World.players()) {
            if (!RaidDirector.defender(User)) {
                continue;
            }
            double Dx = User.getX() - (Target.getX() + 0.5);
            double Dz = User.getZ() - (Target.getZ() + 0.5);
            if (Dx * Dx + Dz * Dz <= Square && Math.abs(User.getY() - Target.getY()) <= RaidRules.HomeDepth * 2.0) {
                Found.add(User);
            }
        }
        return Found;
    }

    public void notice(String Event, Object... Extra) {
        Object[] Args = new Object[Extra.length + 2];
        Args[0] = Faction.displayName();
        Args[1] = RaidNotices.direction(Current.heading());
        System.arraycopy(Extra, 0, Args, 2, Extra.length);
        Component Line = Component.translatable(Style.noticeKey(Event), Args);
        RaidNotices.tell(World, Current.target(), RaidRules.NoticeRange, Faction.colour(), Line);
        DiscordNotices.raid(this, Event, Line);
    }

    public boolean issueSpoils(Mob Raider) {
        ItemStack Stack = Ledger.issue();
        if (Stack == null) {
            return false;
        }
        RaidLoot.carry(Raider, Stack);
        return true;
    }

    public void spot(BlockPos Seen) {
        BlockPos Target = Current.target();
        double Dx = Seen.getX() - Target.getX();
        double Dz = Seen.getZ() - Target.getZ();
        double Gap = Math.sqrt(Dx * Dx + Dz * Dz);
        if (Gap < 4.0 || Gap > RaidRules.SpotReach) {
            return;
        }
        BlockPos Shifted = BlockPos.containing(Target.getX() + Dx * RaidRules.SpotPull, Target.getY() + (Seen.getY() - Target.getY()) * RaidRules.SpotPull,
                Target.getZ() + Dz * RaidRules.SpotPull);
        Current.setTarget(Shifted);
    }
}
