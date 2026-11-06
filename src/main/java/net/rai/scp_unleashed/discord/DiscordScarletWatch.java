package net.rai.scp_unleashed.discord;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.raid.RaidContext;
import net.rai.scp_unleashed.scp001scarlet.ScarletConfig;
import net.rai.scp_unleashed.scp001scarlet.ScarletMeter;
import net.rai.scp_unleashed.scp001scarlet.ScarletWorld;
import net.rai.scp_unleashed.scp001scarlet.core.ScarletState;
import net.rai.scp_unleashed.scp001scarlet.cult.ScarletCultFaction;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class DiscordScarletWatch {
    public static final String RitualEvent = "completed";
    private static final Watcher Live = new Watcher();
    private static volatile boolean Listening;

    private DiscordScarletWatch() {
    }

    public static final class Watcher {
        @Nullable
        private ScarletState.Phase Last;
        private float Meter = -1.0f;
        private int Days = -1;

        @Nullable
        public ScarletState.Phase last() {
            return Last;
        }

        public void forget() {
            Last = null;
            Meter = -1.0f;
            Days = -1;
        }

        public void observe(MinecraftServer Server, ScarletState.Phase Current, float Manifestation, int DaysLeft, int TotalDays, @Nullable BlockPos Throne) {
            ScarletState.Phase Before = Last;
            float Old = Meter;
            Last = Current;
            Meter = Manifestation;
            if (Before == null) {
                Days = Current == ScarletState.Phase.Xk2 || paused(Current) ? DaysLeft : -1;
                return;
            }
            if (Before != Current) {
                changed(Server, Before, Current, DaysLeft, Throne);
                return;
            }
            if (Current == ScarletState.Phase.Stirring && Old >= 0.0f && Manifestation != Old) {
                DiscordApocalypse.scarletManifestation(Server, Old, Manifestation, null);
            } else if (Current == ScarletState.Phase.Xk2 && DaysLeft > 0) {
                if (Days > 0 && DaysLeft < Days) {
                    DiscordApocalypse.scarletCountdown(Server, DaysLeft, TotalDays, Throne);
                }
                Days = DaysLeft;
            }
        }

        private void changed(MinecraftServer Server, ScarletState.Phase Before, ScarletState.Phase Current, int DaysLeft, @Nullable BlockPos Throne) {
            switch (Current) {
                case Xk2 -> {
                    if (paused(Before)) {
                        DiscordApocalypse.scarletEscaped(Server, Throne);
                    } else {
                        DiscordApocalypse.scarletXk2(Server, DaysLeft, Throne);
                    }
                    Days = DaysLeft;
                }
                case Rite -> {
                    if (Before != ScarletState.Phase.Fight) {
                        DiscordApocalypse.scarletRite(Server, null, Throne);
                    }
                }
                case Freed -> DiscordApocalypse.scarletSlain(Server, null, Throne);
                case Asleep, Stirring -> {
                    if (Before == ScarletState.Phase.Xk2 || paused(Before)) {
                        DiscordApocalypse.scarletCancelled(Server, label(Before), Throne);
                    }
                    Days = -1;
                }
                default -> {
                }
            }
        }
    }

    public static boolean paused(ScarletState.Phase Phase) {
        return Phase == ScarletState.Phase.Rite || Phase == ScarletState.Phase.Fight;
    }

    public static Component label(ScarletState.Phase Phase) {
        String Key = switch (Phase) {
            case Asleep -> "dormant";
            case Stirring -> "manifestation";
            case Xk2 -> "xk2";
            case Rite, Fight -> "rite";
            case Freed -> "slain";
        };
        return Component.translatable(DiscordApocalypse.Prefix + "stage." + Key);
    }

    public static Watcher live() {
        return Live;
    }

    public static boolean listening() {
        return Listening;
    }

    public static void snapped(MinecraftServer Server, int Broken) {
        DiscordApocalypse.scarletChain(Server, Broken, null);
    }

    public static void raidNotice(RaidContext Context, String Event) {
        if (RitualEvent.equals(Event) && ScarletCultFaction.Instance.id().equals(Context.Faction().id())) {
            DiscordApocalypse.scarletRitual(Context.Server(), Context.Current().target(), Context.Current().dimension());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void serverTick(ServerTickEvent.Post Event) {
        MinecraftServer Server = Event.getServer();
        ScarletState State = ScarletState.get(Server);
        ScarletState.Phase Current = State.phase();
        if (Current == Live.last() && Current != ScarletState.Phase.Stirring && Current != ScarletState.Phase.Xk2) {
            return;
        }
        long Total = ScarletConfig.xk2Ticks();
        boolean Clocked = Current == ScarletState.Phase.Xk2 || paused(Current);
        int Left = Clocked ? DiscordApocalypse.daysLeft(ScarletWorld.get(Server).clock(), Total) : -1;
        Live.observe(Server, Current, State.manifestation(), Left, DiscordApocalypse.totalDays(Total), State.thronePlaced() ? State.throne() : null);
    }

    @SubscribeEvent
    public static void serverStarting(ServerStartingEvent Event) {
        Live.forget();
        if (!Listening) {
            Listening = true;
            ScarletMeter.listen(DiscordScarletWatch::snapped);
        }
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent Event) {
        Live.forget();
    }
}
