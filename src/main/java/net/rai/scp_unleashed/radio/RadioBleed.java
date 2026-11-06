package net.rai.scp_unleashed.radio;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.radio.network.RadioGlitchPayload;
import net.rai.scp_unleashed.scp012.Scp012Registry;
import net.rai.scp_unleashed.scp472.Scp472Registry;
import net.rai.scp_unleashed.scp513.Scp513Registry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

public final class RadioBleed {
    public static final int Reach = 2;
    public static final int TickEvery = 20;
    public static final int PruneEvery = 200;
    public static final int PulseMin = 800;
    public static final int PulseMax = 1600;
    public static final int ListenerCooldown = 600;
    public static final int HauntTicks = 200;
    public static final int CorruptTicks = 120;
    public static final int GlitchTicks = 30;
    public static final int MachineLines = 8;
    public static final float Volume = 0.55f;
    private static final Map<ResourceKey<Level>, Map<BlockPos, Long>> NextPulse = new HashMap<>();
    private static final Map<UUID, Long> Cooldowns = new HashMap<>();
    private static final Map<UUID, Long> HauntedUntil = new HashMap<>();
    private static final Map<UUID, Long> CorruptUntil = new HashMap<>();

    private RadioBleed() {
    }

    @Nullable
    public static RadioBleedKind kindOf(ItemStack Stack) {
        if (Stack.isEmpty()) {
            return null;
        }
        if (Stack.is(Scp513Registry.Bell.get())) {
            return RadioBleedKind.Clank;
        }
        if (Stack.is(Scp472Registry.BloodstoneItem.get())) {
            return RadioBleedKind.Pulse;
        }
        if (Stack.is(Scp012Registry.Composition.get())) {
            return RadioBleedKind.Refrain;
        }
        return null;
    }

    @Nullable
    public static RadioBleedKind kindOf(BlockState State) {
        return State.is(Scp472Registry.Bloodstone.get()) ? RadioBleedKind.Pulse : null;
    }

    public static List<RadioBleedKind> scan(ServerLevel World, BlockPos Base) {
        Set<RadioBleedKind> Found = EnumSet.noneOf(RadioBleedKind.class);
        for (BlockPos Each : BlockPos.betweenClosed(Base.offset(-Reach, -Reach, -Reach), Base.offset(Reach, Reach, Reach))) {
            if (!World.isLoaded(Each)) {
                continue;
            }
            RadioBleedKind Kind = kindOf(World.getBlockState(Each));
            if (Kind != null) {
                Found.add(Kind);
            }
        }
        AABB Area = new AABB(Base).inflate(Reach);
        for (Entity Each : World.getEntities((Entity) null, Area, Each -> Each instanceof ItemEntity || Each instanceof ItemFrame || Each instanceof ArmorStand)) {
            if (Each instanceof ItemEntity Dropped) {
                add(Found, kindOf(Dropped.getItem()));
            } else if (Each instanceof ItemFrame Frame) {
                add(Found, kindOf(Frame.getItem()));
            } else if (Each instanceof ArmorStand Stand) {
                add(Found, kindOf(Stand.getMainHandItem()));
                add(Found, kindOf(Stand.getOffhandItem()));
            }
        }
        for (Direction Side : Direction.values()) {
            BlockPos Next = Base.relative(Side);
            if (!World.isLoaded(Next) || World.getBlockState(Next).getBlock() instanceof RadioMastBlock) {
                continue;
            }
            IItemHandler Stored = World.getCapability(Capabilities.ItemHandler.BLOCK, Next, Side.getOpposite());
            if (Stored == null) {
                continue;
            }
            for (int Slot = 0; Slot < Stored.getSlots(); Slot++) {
                add(Found, kindOf(Stored.getStackInSlot(Slot)));
            }
        }
        return List.copyOf(Found);
    }

    private static void add(Set<RadioBleedKind> Found, @Nullable RadioBleedKind Kind) {
        if (Kind != null) {
            Found.add(Kind);
        }
    }

    public static void tick(MinecraftServer Server) {
        if (Server.getTickCount() % TickEvery != 0) {
            return;
        }
        RadioNetwork.Ranges Limits = VoiceConfig.ranges();
        for (ServerLevel World : Server.getAllLevels()) {
            Map<BlockPos, Long> Schedule = NextPulse.computeIfAbsent(World.dimension(), Key -> new HashMap<>());
            RadioTowerData Records = RadioTowerData.get(World);
            if (Server.getTickCount() % PruneEvery == 0) {
                Records.prune(World);
            }
            List<RadioTowerData.Tower> Active = new ArrayList<>();
            for (RadioTowerData.Tower Each : Records.towers()) {
                if (Each.relays() && !Each.Sources().isEmpty()) {
                    Active.add(Each);
                }
            }
            Schedule.keySet().removeIf(Pos -> Active.stream().noneMatch(Each -> Each.Pos().equals(Pos)));
            if (Active.isEmpty()) {
                continue;
            }
            long Now = World.getGameTime();
            List<RadioNetwork.Node> Nodes = null;
            for (RadioTowerData.Tower Each : Active) {
                Long Due = Schedule.get(Each.Pos());
                if (Due == null) {
                    Schedule.put(Each.Pos(), Now + Mth.nextInt(World.random, PulseMin, PulseMax));
                    continue;
                }
                if (Now < Due) {
                    continue;
                }
                Schedule.put(Each.Pos(), Now + Mth.nextInt(World.random, PulseMin, PulseMax));
                if (Nodes == null) {
                    Nodes = RadioTowerData.nodes(World, Limits);
                }
                RadioBleedKind Kind = Each.Sources().get(World.random.nextInt(Each.Sources().size()));
                pulse(World, Each, Kind, Limits, Nodes);
            }
        }
    }

    public static List<RadioRelay.Hearer> pulse(ServerLevel World, RadioTowerData.Tower Tower, RadioBleedKind Kind, RadioNetwork.Ranges Limits, List<RadioNetwork.Node> Nodes) {
        return broadcast(World, List.of(Tower), Tower.Channel(), Kind, Limits, Nodes);
    }

    public static List<RadioRelay.Hearer> reached(ServerLevel World, List<RadioTowerData.Tower> Towers, int Channel, RadioNetwork.Ranges Limits, List<RadioNetwork.Node> Nodes) {
        Map<UUID, RadioRelay.Hearer> Best = new LinkedHashMap<>();
        for (RadioTowerData.Tower Each : Towers) {
            RadioTowerData.Tower Tuned = new RadioTowerData.Tower(Each.Pos(), Each.Masts(), Each.Powered(), Channel, Each.Sources());
            for (RadioRelay.Hearer Heard : RadioRelay.fromTower(World, Tuned, Limits, Nodes)) {
                RadioRelay.Hearer Known = Best.get(Heard.Listener().getUUID());
                if (Known == null || Heard.Strength() > Known.Strength()) {
                    Best.put(Heard.Listener().getUUID(), Heard);
                }
            }
        }
        return new ArrayList<>(Best.values());
    }

    public static List<RadioRelay.Hearer> broadcast(ServerLevel World, List<RadioTowerData.Tower> Towers, int Channel, RadioBleedKind Kind, RadioNetwork.Ranges Limits,
                                                    List<RadioNetwork.Node> Nodes) {
        long Now = World.getGameTime();
        List<RadioRelay.Hearer> Reached = new ArrayList<>();
        for (RadioRelay.Hearer Each : reached(World, Towers, Channel, Limits, Nodes)) {
            ServerPlayer Listener = Each.Listener();
            if (isCooling(Listener.getUUID(), Now)) {
                continue;
            }
            Cooldowns.put(Listener.getUUID(), Now + ListenerCooldown);
            HauntedUntil.put(Listener.getUUID(), Now + HauntTicks);
            apply(Listener, Kind, Channel, Each.Strength());
            Reached.add(Each);
        }
        return Reached;
    }

    public static Map<Integer, Integer> listenedChannels(ServerLevel World, List<RadioTowerData.Tower> Towers, RadioNetwork.Ranges Limits, List<RadioNetwork.Node> Nodes) {
        List<RadioNetwork.Node> Including = new ArrayList<>(Nodes);
        for (RadioTowerData.Tower Each : Towers) {
            if (Including.stream().noneMatch(Node -> Node.Pos().equals(Each.Pos()))) {
                Including.add(RadioNetwork.node(Limits, Each.Pos(), Math.max(1, Each.Masts())));
            }
        }
        List<RadioNetwork.Reach> Reaches = new ArrayList<>();
        for (RadioTowerData.Tower Each : Towers) {
            Reaches.add(RadioNetwork.fromTower(Limits, Including, Each.Pos()));
        }
        Map<Integer, Integer> Counts = new TreeMap<>();
        for (ServerPlayer Each : World.players()) {
            Set<Integer> Tuned = RadioCarry.channels(Each);
            if (Tuned.isEmpty()) {
                continue;
            }
            boolean Covered = false;
            for (RadioNetwork.Reach Reach : Reaches) {
                if (Reach.reaches(RadioRelay.mouth(Each))) {
                    Covered = true;
                    break;
                }
            }
            if (Covered) {
                for (int Channel : Tuned) {
                    Counts.merge(Channel, 1, Integer::sum);
                }
            }
        }
        return Counts;
    }

    public static boolean isCooling(UUID Listener, long Now) {
        Long Ready = Cooldowns.get(Listener);
        return Ready != null && Now < Ready;
    }

    public static void apply(ServerPlayer Listener, RadioBleedKind Kind, int Channel, double Strength) {
        long Now = Listener.serverLevel().getGameTime();
        float Pitch = 0.92f + Listener.getRandom().nextFloat() * 0.12f;
        RadioSignal.playTo(Listener, Kind.layers(), Volume * (0.7f + 0.3f * (float) Strength), Pitch);
        Listener.playNotifySound(RadioRegistry.Crackle.get(), RadioSignal.Source, 0.4f, 1.0f);
        if (Kind.effect() != null && !Listener.isCreative()) {
            Listener.addEffect(new MobEffectInstance(Kind.effect(), Kind.ticks(), 0, false, true, true));
        }
        if (Kind == RadioBleedKind.Corrupt) {
            CorruptUntil.put(Listener.getUUID(), Now + CorruptTicks);
            Listener.sendSystemMessage(machineLine(Channel, Listener.getRandom()));
        } else if (Kind == RadioBleedKind.Glitch) {
            PacketDistributor.sendToPlayer(Listener, new RadioGlitchPayload(GlitchTicks, Listener.getRandom().nextInt()));
        }
        Listener.displayClientMessage(Component.translatable("radio.scp_unleashed.bleed", RadioChat.tag(Channel)).withStyle(ChatFormatting.DARK_RED), true);
    }

    public static Component machineLine(int Channel, RandomSource Random) {
        Component Spoken = Component.empty().append(RadioChat.noise(3, Random)).append(" ")
                .append(Component.translatable("radio.scp_unleashed.machine.line." + Random.nextInt(MachineLines)).withStyle(ChatFormatting.RED)).append(" ").append(RadioChat.noise(3, Random));
        return Component.translatable("radio.scp_unleashed.chat", RadioChat.tag(Channel), Component.translatable("radio.scp_unleashed.machine.sender")
                .withStyle(ChatFormatting.DARK_RED), Spoken).withStyle(ChatFormatting.GRAY);
    }

    public static boolean isCorrupt(UUID Listener, long Now) {
        Long Until = CorruptUntil.get(Listener);
        return Until != null && Now < Until;
    }

    public static boolean isHaunted(UUID Listener, long Now) {
        Long Until = HauntedUntil.get(Listener);
        return Until != null && Now < Until;
    }

    public static void forget(ServerLevel World, BlockPos Pos) {
        Map<BlockPos, Long> Schedule = NextPulse.get(World.dimension());
        if (Schedule != null) {
            Schedule.remove(Pos);
        }
    }

    public static void forgetPlayer(UUID Listener) {
        Cooldowns.remove(Listener);
        HauntedUntil.remove(Listener);
        CorruptUntil.remove(Listener);
    }

    public static void clear() {
        NextPulse.clear();
        Cooldowns.clear();
        HauntedUntil.clear();
        CorruptUntil.clear();
    }
}
