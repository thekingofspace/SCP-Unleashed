package net.rai.scp_unleashed.daevite;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.daevite.entity.DaeviteMob;
import net.rai.scp_unleashed.daevite.entity.DaeviteOverseer;
import net.rai.scp_unleashed.faction.FactionControl;
import net.rai.scp_unleashed.faction.FactionLive;
import net.rai.scp_unleashed.faction.FactionPhase;
import net.rai.scp_unleashed.faction.FactionRules;
import net.rai.scp_unleashed.faction.FactionSite;
import net.rai.scp_unleashed.faction.FactionState;
import net.rai.scp_unleashed.faction.FactionType;
import net.rai.scp_unleashed.raid.RaidStyle;
import net.rai.scp_unleashed.raid.daevite.DaeviteRaidStyle;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class DaeviteFaction extends FactionType {
    public static final ResourceLocation Id = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "daevites");
    public static final int Colour = 0xB0201A;
    public static final double MaxPower = 1000.0;
    public static final double PerMemberMinute = 0.5;
    public static final double BaseMinute = 1.5;
    public static final double KnightLoss = 8.0;
    public static final double OverseerLoss = 12.0;
    public static final double PlayerKill = 40.0;
    public static final double OtherKill = 15.0;
    public static final double CampBurst = 100.0;
    public static final double MarchBurst = 150.0;
    public static final double Surge = 300.0;
    public static final double SurgeFloor = 850.0;
    public static final int TempleKnights = 8;
    public static final int TempleOverseers = 4;
    public static final int CampKnights = 5;
    public static final int CampOverseers = 2;
    public static final int TempleStartKnights = 5;
    public static final int TempleStartOverseers = 3;
    public static final int CampStartKnights = 3;
    public static final int CampStartOverseers = 1;
    public static final long TempleReinforce = 1200L;
    public static final long CampReinforce = 1800L;
    public static final int SiteCap = 5;
    public static final int RosterCap = 96;
    public static final long ExpansionInterval = 48000L;
    public static final long PatrolInterval = 4800L;
    public static final long RaidInterval = 7200L;
    public static final long PatrolsAfter = 72000L;
    public static final int PatrolRange = 96;
    public static final int TerritoryRange = 320;
    public static final long PatrolLength = 2400L;
    public static final int AnnounceRange = 160;
    public static final String Temple = "temple";
    public static final String Camp = "camp";
    public static final String RoleKnight = "knight";
    public static final String RoleOverseer = "overseer";
    public static final String PatrolsKey = "Patrols";
    public static final DaeviteFaction Instance = new DaeviteFaction();

    private DaeviteFaction() {
        super(Id, Colour, new FactionRules(MaxPower, PerMemberMinute / 1200.0, BaseMinute / 1200.0, SiteCap, RosterCap, ExpansionInterval, PatrolInterval, PatrolRange,
                TerritoryRange, RaidInterval));
    }

    @Override
    public int siteCap(FactionState State, FactionSite Site) {
        return Site.is(Temple) ? TempleKnights + TempleOverseers : CampKnights + CampOverseers;
    }

    @Override
    public long reinforceInterval(FactionState State, FactionSite Site) {
        long Base = Site.is(Temple) ? TempleReinforce : CampReinforce;
        return State.phase() == FactionPhase.Ascendant ? Base * 3L / 4L : Base;
    }

    @Override
    public int siteRadius(FactionSite Site) {
        return Site.built() ? (Site.is(Temple) ? 30 : 16) : DaeviteSites.searchRadius(Site);
    }

    @Nullable
    public FactionSite answerPage(ServerLevel Overworld, FactionState State, int Page, BlockPos Origin) {
        MinecraftServer Server = Overworld.getServer();
        if (State.phase() == FactionPhase.Defeated) {
            return DaeviteSites.templeSite(State);
        }
        if (State.phase() == FactionPhase.Dormant) {
            FactionControl.awaken(Overworld, this, Origin, null);
        }
        FactionSite Shrine = DaeviteSites.templeSite(State);
        if (Shrine == null) {
            Shrine = DaeviteSites.planTemple(Overworld, State, Origin);
        }
        switch (Page) {
            case 33 -> {
                FactionSite Site = DaeviteSites.planCamp(Server, State, Origin);
                FactionControl.addPower(Server, this, CampBurst);
                return Site != null ? Site : Shrine;
            }
            case 34 -> {
                FactionSite Site = DaeviteSites.planCamp(Server, State, Origin);
                FactionControl.addPower(Server, this, MarchBurst);
                State.extra().putBoolean(PatrolsKey, true);
                State.changed();
                announce(Overworld, Origin, "daevite.scp_unleashed.march", true);
                return Site != null ? Site : Shrine;
            }
            case 35 -> {
                State.setPower(Math.max(State.power() + Surge, SurgeFloor), MaxPower);
                announce(Overworld, Origin, "daevite.scp_unleashed.surge", true);
                return Shrine;
            }
            default -> {
                return Shrine;
            }
        }
    }

    public void announce(ServerLevel World, BlockPos Near, String Key, boolean Horn) {
        for (ServerPlayer User : World.players()) {
            if (User.blockPosition().distSqr(Near) <= (double) AnnounceRange * AnnounceRange) {
                User.displayClientMessage(Component.translatable(Key).withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), true);
                if (Horn) {
                    Vec3 Toward = Vec3.atCenterOf(Near).subtract(User.position());
                    Vec3 At = User.position().add(Toward.lengthSqr() > 1.0 ? Toward.normalize().scale(12.0) : Vec3.ZERO);
                    User.connection.send(new ClientboundSoundPacket(DaeviteRegistry.Horn, SoundSource.HOSTILE, At.x, At.y, At.z,
                            2.5f, 0.85f, World.random.nextLong()));
                }
            }
        }
    }

    @Override
    public void awaken(ServerLevel World, FactionState State, BlockPos Near, @Nullable Entity Cause) {
        ServerLevel Overworld = World.getServer().overworld();
        DaeviteSites.planTemple(Overworld, State, World == Overworld ? Near : DaeviteSites.overworldOrigin(World, Near));
        announce(World, Near, "daevite.scp_unleashed.awaken", true);
    }

    @Override
    public void advance(ServerLevel World, FactionState State, BlockPos Near) {
        ServerLevel Overworld = World.getServer().overworld();
        if (DaeviteSites.templeSite(State) == null) {
            DaeviteSites.planTemple(Overworld, State, Near);
            return;
        }
        DaeviteSites.planCamp(World.getServer(), State, Near);
        if (State.countSites(Camp) >= 2) {
            State.extra().putBoolean(PatrolsKey, true);
            State.changed();
        }
    }

    @Override
    public void defeat(MinecraftServer Server, FactionState State, @Nullable BlockPos Where) {
        DaeviteRitual.abort(Server, State);
        for (ServerLevel World : Server.getAllLevels()) {
            BlockPos From = Where != null ? Where : World.getSharedSpawnPos();
            List<Entity> Doomed = new ArrayList<>();
            for (Entity Body : World.getAllEntities()) {
                if (Body.isAlive() && (DaeviteTargets.isDaevite(Body) || Body instanceof DaeviteMob)) {
                    Doomed.add(Body);
                }
            }
            for (Entity Body : Doomed) {
                DaeviteCollapse.doom(World, Body, DaeviteCollapse.rippleDelay(Body, From, World.random));
            }
            for (ServerPlayer User : World.players()) {
                User.displayClientMessage(Component.translatable("daevite.scp_unleashed.defeated").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), true);
            }
        }
    }

    @Override
    public void reset(MinecraftServer Server, FactionState State) {
        DaeviteRitual.abort(Server, State);
        for (ServerLevel World : Server.getAllLevels()) {
            for (Entity Body : FactionLive.members(Id)) {
                if (Body.level() == World) {
                    DaeviteCollapse.doom(World, Body, 2 + World.random.nextInt(20));
                }
            }
        }
    }

    @Override
    public boolean buildSite(ServerLevel World, FactionState State, FactionSite Site) {
        return DaeviteSites.build(World, State, Site);
    }

    @Override
    public void siteBuilt(ServerLevel World, FactionState State, FactionSite Site) {
        DaeviteSites.garrison(World, State, Site);
    }

    @Nullable
    @Override
    public Mob recruit(ServerLevel World, FactionState State, FactionSite Site) {
        return DaeviteSites.recruit(World, State, Site);
    }

    @Override
    public boolean planExpansion(MinecraftServer Server, FactionState State, @Nullable BlockPos Near) {
        return DaeviteSites.planCamp(Server, State, Near) != null;
    }

    @Override
    public boolean patrolsEnabled(FactionState State) {
        if (State.phase() == FactionPhase.Ascendant || State.extra().getBoolean(PatrolsKey)) {
            return true;
        }
        return State.awakenedAt() > 0L && State.lastAccrual() - State.awakenedAt() > PatrolsAfter;
    }

    @Override
    public void patrol(ServerLevel World, FactionState State, FactionSite Site, Vec3 Target, List<Mob> Group) {
        long Until = World.getGameTime() + PatrolLength;
        for (Mob Member : Group) {
            if (Member instanceof DaeviteMob Daevite) {
                Daevite.setPatrol(Target, Until);
            }
        }
        BlockPos Gate = Site.mark("gate") != null ? Site.mark("gate") : Site.anchor();
        World.playSound(null, Gate.getX(), Gate.getY(), Gate.getZ(), DaeviteRegistry.Horn.get(), SoundSource.HOSTILE, 3.0f, 1.0f);
    }

    @Override
    public void raid(ServerLevel World, FactionState State, ServerPlayer Target, FactionSite From) {
        DaeviteSites.warParty(World, State, Target, From);
    }

    @Override
    public RaidStyle raidStyle() {
        return DaeviteRaidStyle.Instance;
    }

    @Override
    public boolean canRaid(FactionState State) {
        return DaeviteRaidStyle.ready(State);
    }

    @Override
    public void tick(MinecraftServer Server, FactionState State) {
        DaeviteRitual.tick(Server, State);
    }

    @Override
    public boolean claims(Entity Body) {
        return DaeviteTargets.isDaevite(Body);
    }

    @Override
    public void staleMember(ServerLevel World, Entity Member) {
        DaeviteCollapse.doom(World, Member, 5 + World.random.nextInt(40));
    }

    @Override
    public void memberLost(ServerLevel World, FactionState State, LivingEntity Member, DamageSource Source) {
        if (Member instanceof DaeviteMob Daevite && Daevite.unmade()) {
            return;
        }
        double Loss = Member instanceof DaeviteOverseer ? OverseerLoss : KnightLoss;
        State.setPower(State.power() - Loss, MaxPower);
    }

    @Override
    public void memberKilled(ServerLevel World, FactionState State, LivingEntity Member, LivingEntity Victim) {
        double Gain = Victim instanceof Player ? PlayerKill : OtherKill;
        if (World.getDifficulty() == Difficulty.HARD) {
            Gain *= 1.25;
        }
        State.setPower(State.power() + Gain, MaxPower);
    }

    @Override
    public boolean idleMember(Mob Member) {
        if (Member.getTarget() != null) {
            return false;
        }
        if (Member instanceof DaeviteOverseer Seer && Seer.channelPoint() != null) {
            return false;
        }
        return !(Member instanceof DaeviteMob Daevite) || !Daevite.onPatrol() && !Daevite.doomed();
    }
}
