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
import net.rai.scp_unleashed.scp5001.core.Scp5001State;
import net.rai.scp_unleashed.scp5001.xk.Scp5001XkConfig;
import net.rai.scp_unleashed.scp5001.xk.XkState;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class DiscordApocalypseWatch {
    private static final Watcher Live = new Watcher();

    private DiscordApocalypseWatch() {
    }

    public static final class Watcher {
        @Nullable
        private Scp5001State.Phase Last;
        private int Days = -1;
        private boolean Dueled;

        @Nullable
        public Scp5001State.Phase last() {
            return Last;
        }

        public int days() {
            return Days;
        }

        public void forget() {
            Last = null;
            Days = -1;
            Dueled = false;
        }

        public void observe(MinecraftServer Server, Scp5001State.Phase Current, int DaysLeft, int TotalDays, @Nullable BlockPos Site) {
            Scp5001State.Phase Before = Last;
            Last = Current;
            if (Before == null) {
                Days = Current == Scp5001State.Phase.Xk2 || paused(Current) ? DaysLeft : -1;
                Dueled = Current == Scp5001State.Phase.Duel;
                return;
            }
            if (Before != Current) {
                changed(Server, Before, Current, DaysLeft, Site);
            } else if (Current == Scp5001State.Phase.Xk2 && DaysLeft > 0) {
                if (Days > 0 && DaysLeft < Days) {
                    DiscordApocalypse.countdown(Server, DiscordApocalypse.Scp5001, DaysLeft, TotalDays, Site);
                }
                Days = DaysLeft;
            }
            if (!paused(Current)) {
                Dueled = false;
            }
        }

        private void changed(MinecraftServer Server, Scp5001State.Phase Before, Scp5001State.Phase Current, int DaysLeft, @Nullable BlockPos Site) {
            switch (Current) {
                case Unlocked -> DiscordApocalypse.scp5001(Server, DiscordApocalypse.Step.Breach, Site);
                case Rising -> {
                    if (Before == Scp5001State.Phase.Dormant) {
                        DiscordApocalypse.scp5001(Server, DiscordApocalypse.Step.Breach, Site);
                    }
                }
                case Overdrive -> DiscordApocalypse.scp5001(Server, DiscordApocalypse.Step.Overdrive, Site);
                case Purging -> DiscordApocalypse.scp5001(Server, DiscordApocalypse.Step.Purge, Site);
                case Xk1 -> DiscordApocalypse.scp5001(Server, DiscordApocalypse.Step.Xk1, Site);
                case Xk2 -> {
                    if (!paused(Before) || Days <= 0) {
                        if (DaysLeft > 0) {
                            DiscordApocalypse.scp5001(Server, DiscordApocalypse.Step.Xk2, Site, DiscordApocalypse.remaining(DaysLeft));
                        } else {
                            DiscordApocalypse.scp5001(Server, DiscordApocalypse.Step.Xk2, Site);
                        }
                    }
                    Days = DaysLeft;
                }
                case Summoning -> {
                    if (!paused(Before)) {
                        DiscordApocalypse.scp5001(Server, DiscordApocalypse.Step.Summoning, Site);
                    }
                }
                case Duel -> {
                    if (!Dueled) {
                        Dueled = true;
                        DiscordApocalypse.scp5001(Server, DiscordApocalypse.Step.Duel, Site);
                    }
                }
                case Saved -> DiscordApocalypse.scp5001(Server, DiscordApocalypse.Step.Saved, Site);
                case Dormant -> {
                    if (Before == Scp5001State.Phase.Resetting) {
                        DiscordApocalypse.scp5001(Server, DiscordApocalypse.Step.Recontained, Site);
                    } else if (Before != Scp5001State.Phase.Saved) {
                        DiscordApocalypse.cancelled(Server, DiscordApocalypse.Scp5001, label(Before), Site);
                    }
                    Days = -1;
                }
                default -> {
                }
            }
        }
    }

    public static boolean paused(Scp5001State.Phase Phase) {
        return Phase == Scp5001State.Phase.Summoning || Phase == Scp5001State.Phase.Duel;
    }

    public static Component label(Scp5001State.Phase Phase) {
        String Key = switch (Phase) {
            case Unlocked, Rising -> "breach";
            case Overdrive -> "overdrive";
            case Purging -> "purge";
            case Resetting -> "recontained";
            case Xk1 -> "xk1";
            case Xk2 -> "xk2";
            case Ended -> "ending";
            case Summoning -> "summoning";
            case Duel -> "duel";
            case Saved -> "saved";
            case Dormant -> "dormant";
        };
        return Component.translatable(DiscordApocalypse.Prefix + "stage." + Key);
    }

    @Nullable
    public static BlockPos site(MinecraftServer Server) {
        Scp5001State State = Scp5001State.get(Server);
        return State.located() ? State.core() : null;
    }

    public static Watcher live() {
        return Live;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void serverTick(ServerTickEvent.Post Event) {
        MinecraftServer Server = Event.getServer();
        Scp5001State State = Scp5001State.get(Server);
        Scp5001State.Phase Current = State.phase();
        if (Current == Live.last() && Current != Scp5001State.Phase.Xk2) {
            return;
        }
        long Total = Scp5001XkConfig.skyTicks();
        boolean Clocked = Current == Scp5001State.Phase.Xk2 || paused(Current);
        int Left = Clocked ? DiscordApocalypse.daysLeft(XkState.get(Server).secondElapsed(), Total) : -1;
        Live.observe(Server, Current, Left, DiscordApocalypse.totalDays(Total), State.located() ? State.core() : null);
    }

    @SubscribeEvent
    public static void serverStarting(ServerStartingEvent Event) {
        Live.forget();
        DiscordApocalypse.forget();
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent Event) {
        Live.forget();
        DiscordApocalypse.forget();
    }
}
