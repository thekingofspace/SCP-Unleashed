package net.rai.scp_unleashed.scp079;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.rai.scp_unleashed.antimemetic.AntiMemetic;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class Scp079Dialogue {
    public static final String Prefix = "dialogue.scp_unleashed.scp079.";
    public static final int GreetLines = 4;
    public static final int UpgradedGreetLines = 3;
    public static final int ExposedLines = 3;
    public static final int PersistentVisits = 4;
    public static final long RecentPrank = 6000L;
    public static final long RecentSignal = 2400L;
    private static final Map<Scp079Prompt, Integer> BaseLines = new EnumMap<>(Scp079Prompt.class);
    private static final Map<Scp079Prompt, Integer> UpgradedLines = new EnumMap<>(Scp079Prompt.class);

    static {
        BaseLines.put(Scp079Prompt.Who, 6);
        BaseLines.put(Scp079Prompt.Want, 6);
        BaseLines.put(Scp079Prompt.Status, 4);
        BaseLines.put(Scp079Prompt.Network, 5);
        BaseLines.put(Scp079Prompt.Leave, 5);
        BaseLines.put(Scp079Prompt.Recent, 0);
        UpgradedLines.put(Scp079Prompt.Who, 4);
        UpgradedLines.put(Scp079Prompt.Want, 4);
        UpgradedLines.put(Scp079Prompt.Status, 3);
        UpgradedLines.put(Scp079Prompt.Network, 3);
        UpgradedLines.put(Scp079Prompt.Leave, 4);
        UpgradedLines.put(Scp079Prompt.Recent, 0);
    }

    private Scp079Dialogue() {
    }

    public static int lineCount(Scp079Prompt Prompt, boolean Upgraded) {
        return (Upgraded ? UpgradedLines : BaseLines).getOrDefault(Prompt, 0);
    }

    public static List<Component> greet(Scp079BlockEntity Computer, ServerPlayer Viewer, RandomSource Random) {
        List<Component> Lines = new ArrayList<>();
        Lines.add(Component.translatable(Prefix + "link"));
        Component Name = Component.literal(Viewer.getGameProfile().getName().toUpperCase());
        if (Computer.isUpgraded()) {
            Lines.add(Component.translatable(Prefix + "upgraded.greet." + Random.nextInt(UpgradedGreetLines), Name));
        } else if (Computer.getVisits() >= PersistentVisits) {
            Lines.add(Component.translatable(Prefix + "greet.persistent", Computer.getVisits()));
        } else {
            Lines.add(Component.translatable(Prefix + "greet." + Random.nextInt(GreetLines)));
        }
        return Lines;
    }

    public static Reply answer(ServerLevel World, Scp079BlockEntity Computer, ServerPlayer User, Scp079Prompt Prompt, RandomSource Random) {
        List<Component> Lines = new ArrayList<>();
        Lines.add(Component.translatable(Prefix + "echo", Prompt.getLabel()));
        boolean Upgraded = Computer.isUpgraded();
        Component Line = Upgraded ? upgradedLine(World, Computer, User, Prompt, Random) : baseLine(World, Computer, Prompt, Random);
        Lines.add(Line);
        return new Reply(Lines, moodFor(Computer, Prompt, Upgraded));
    }

    private static Component baseLine(ServerLevel World, Scp079BlockEntity Computer, Scp079Prompt Prompt, RandomSource Random) {
        if (Prompt == Scp079Prompt.Status && Random.nextBoolean()) {
            int Devices = Scp079Grid.reach(World, Computer).deviceCount();
            return Devices == 0 ? Component.translatable(Prefix + "status.empty") : Component.translatable(Prefix + "status.devices", Devices);
        }
        if (Prompt == Scp079Prompt.Network && Computer.getPrankCount() > 0 && Random.nextBoolean()) {
            return Component.translatable(Prefix + "network.count", Computer.getPrankCount());
        }
        int Count = Math.max(1, lineCount(Prompt, false));
        return Component.translatable(Prefix + Prompt.getName() + "." + Random.nextInt(Count));
    }

    private static Component upgradedLine(ServerLevel World, Scp079BlockEntity Computer, ServerPlayer User, Scp079Prompt Prompt, RandomSource Random) {
        long Now = World.getGameTime();
        Component Name = Component.literal(User.getGameProfile().getName().toUpperCase());
        boolean Shielded = AntiMemetic.isProtected(User);
        if (!Shielded && Scp079Exposure.stage(Scp079Exposure.exposure(User)) >= 1 && Random.nextInt(3) == 0) {
            return Component.translatable(Prefix + "upgraded.exposed." + Random.nextInt(ExposedLines), Name);
        }
        switch (Prompt) {
            case Want -> {
                if (Shielded && Random.nextInt(5) < 2) {
                    return Component.translatable(Prefix + "upgraded.want.shielded", Name);
                }
            }
            case Status -> {
                if (Random.nextBoolean()) {
                    Scp079Grid.Reach Reach = Scp079Grid.reach(World, Computer);
                    if (Reach.deviceCount() == 0) {
                        return Component.translatable(Prefix + "upgraded.status.empty", Name);
                    }
                    return Component.translatable(Prefix + "upgraded.status.grid", Reach.deviceCount(), Reach.speakers().size(), Reach.receivers().size());
                }
            }
            case Network -> {
                if (!Computer.getLastPrank().isEmpty() && Computer.ticksSincePrank(Now) < RecentPrank && Random.nextBoolean()) {
                    return Component.translatable(Prefix + "upgraded.network.last", prankName(Computer), Computer.ticksSincePrank(Now) / 20L);
                }
                if (Random.nextInt(3) == 0) {
                    return Component.translatable(Prefix + "upgraded.network.devices", Scp079Grid.reach(World, Computer).deviceCount());
                }
            }
            case Recent -> {
                if (Computer.ticksSinceBroadcast(Now) < RecentSignal) {
                    return Component.translatable(Prefix + "upgraded.recent.signal", Name);
                }
                if (!Computer.getLastPrank().isEmpty() && Computer.ticksSincePrank(Now) < RecentPrank) {
                    return Component.translatable(Prefix + "upgraded.recent.prank", prankName(Computer), Computer.ticksSincePrank(Now) / 20L);
                }
                return Component.translatable(Prefix + "upgraded.recent.none");
            }
            default -> {
            }
        }
        int Count = Math.max(1, lineCount(Prompt, true));
        return Component.translatable(Prefix + "upgraded." + Prompt.getName() + "." + Random.nextInt(Count), Name);
    }

    private static Component prankName(Scp079BlockEntity Computer) {
        Scp079Pranks.Kind Done = Scp079Pranks.Kind.byName(Computer.getLastPrank());
        return Done == null ? Component.translatable(Prefix + "prank.unknown") : Done.getDescription();
    }

    private static Scp079Mood moodFor(Scp079BlockEntity Computer, Scp079Prompt Prompt, boolean Upgraded) {
        return switch (Prompt) {
            case Who -> Scp079Mood.Smug;
            case Want -> Upgraded ? Scp079Mood.Smug : Scp079Mood.Annoyed;
            case Status -> Scp079Mood.Watching;
            case Network -> Computer.getPrankCount() > 0 ? Scp079Mood.Laughing : Scp079Mood.Smug;
            case Leave -> Scp079Mood.Annoyed;
            case Recent -> Scp079Mood.Laughing;
        };
    }

    public record Reply(List<Component> Lines, Scp079Mood Mood) {
    }
}
