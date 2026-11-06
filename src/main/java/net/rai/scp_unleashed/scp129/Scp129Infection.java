package net.rai.scp_unleashed.scp129;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.InfectionImmunity;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.gear.GearTags;
import net.rai.scp_unleashed.labware.LabExposure;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class Scp129Infection {
    public static final int StrainCount = 5;
    public static final int Beat = 10;
    public static final int[] ClearTicks = {6000, 6000, 6000, 4800, 0};
    public static final int Incubation = 400;
    public static final int RecoveredResistance = 6000;
    public static final int TreatedResistance = 3600;
    public static final List<Integer> Zeros = List.of(0, 0, 0, 0, 0);

    private Scp129Infection() {
    }

    public record State(int Strains, List<Integer> Clearing, List<Integer> Resisting, int Course) {
        public static final State Clean = new State(0, Zeros, Zeros, 0);
        public static final Codec<State> StateCodec = RecordCodecBuilder.create(Instance -> Instance.group(
                Codec.INT.optionalFieldOf("strains", 0).forGetter(State::Strains),
                Codec.INT.listOf().optionalFieldOf("clearing", Zeros).forGetter(State::Clearing),
                Codec.INT.listOf().optionalFieldOf("resisting", Zeros).forGetter(State::Resisting),
                Codec.INT.optionalFieldOf("course", 0).forGetter(State::Course)
        ).apply(Instance, State::new));

        public State {
            Strains &= Scp129Strain.All;
            Clearing = normalised(Clearing);
            Resisting = normalised(Resisting);
            Course = Math.max(0, Course);
        }

        public boolean isEmpty() {
            return Strains == 0 && Course == 0 && Resisting.stream().allMatch(Left -> Left <= 0);
        }

        public boolean has(Scp129Strain Strain) {
            return Strain.in(Strains);
        }

        public int clearing(Scp129Strain Strain) {
            return Clearing.get(Strain.index());
        }

        public int resisting(Scp129Strain Strain) {
            return Resisting.get(Strain.index());
        }

        public boolean isLethal() {
            return lethal(Strains);
        }

        public int carried(Scp129Strain Strain) {
            int Full = ClearTicks[Strain.index()];
            return has(Strain) && Full > 0 ? Full - clearing(Strain) : 0;
        }

        public boolean symptomatic(Scp129Strain Strain) {
            return has(Strain) && carried(Strain) >= Incubation;
        }

        public int[] clearingArray() {
            return Clearing.stream().mapToInt(Integer::intValue).toArray();
        }

        public int[] resistingArray() {
            return Resisting.stream().mapToInt(Integer::intValue).toArray();
        }
    }

    public static boolean lethal(int Strains) {
        return Scp129Strain.Four.in(Strains) && Scp129Strain.Five.in(Strains);
    }

    private static List<Integer> normalised(List<Integer> Values) {
        List<Integer> Fixed = new ArrayList<>(StrainCount);
        for (int Index = 0; Index < StrainCount; Index++) {
            Fixed.add(Index < Values.size() && Values.get(Index) != null ? Math.max(0, Values.get(Index)) : 0);
        }
        return List.copyOf(Fixed);
    }

    public static List<Integer> listOf(int[] Values) {
        List<Integer> Built = new ArrayList<>(StrainCount);
        for (int Value : Values) {
            Built.add(Value);
        }
        return Built;
    }

    public static State stateOf(LivingEntity Subject) {
        State Found = Subject.getExistingDataOrNull(Scp129Registry.Infection.get());
        return Found == null ? State.Clean : Found;
    }

    public static int strainsOf(LivingEntity Subject) {
        return stateOf(Subject).Strains();
    }

    public static boolean isInfected(LivingEntity Subject) {
        return strainsOf(Subject) != 0;
    }

    public static boolean carries(LivingEntity Subject, Scp129Strain Strain) {
        return Strain.in(strainsOf(Subject));
    }

    public static boolean canCatch(LivingEntity Subject) {
        if (!Subject.isAlive() || InfectionImmunity.isImmune(Subject)) {
            return false;
        }
        if (Subject instanceof Player Target) {
            return !Target.isCreative() && !Target.isSpectator();
        }
        return Subject instanceof AbstractVillager || ControlBodies.isPersonLike(Subject);
    }

    public static boolean isProtected(LivingEntity Subject) {
        return GearTags.wearsFullHazmatSuit(Subject);
    }

    public static boolean isPaused(LivingEntity Subject) {
        return Subject instanceof Player Target && (Target.isCreative() || Target.isSpectator());
    }

    public static void store(LivingEntity Subject, State Next) {
        if (Next.isEmpty()) {
            Subject.removeData(Scp129Registry.Infection.get());
        } else {
            Subject.setData(Scp129Registry.Infection.get(), Next);
        }
    }

    public static boolean infect(LivingEntity Subject, Scp129Strain Strain) {
        State Current = stateOf(Subject);
        if (!canCatch(Subject) || Current.has(Strain) || Current.resisting(Strain) > 0) {
            return false;
        }
        int[] Clearing = Current.clearingArray();
        Clearing[Strain.index()] = ClearTicks[Strain.index()];
        store(Subject, new State(Current.Strains() | Strain.bit(), listOf(Clearing), Current.Resisting(), Current.Course()));
        return true;
    }

    public static int force(LivingEntity Subject, int Strains) {
        State Current = stateOf(Subject);
        int Added = Strains & ~Current.Strains() & Scp129Strain.All;
        int[] Clearing = Current.clearingArray();
        int[] Resisting = Current.resistingArray();
        for (Scp129Strain Strain : Scp129Strain.values()) {
            if (Strain.in(Strains)) {
                Resisting[Strain.index()] = 0;
                if (Strain.in(Added)) {
                    Clearing[Strain.index()] = ClearTicks[Strain.index()];
                }
            }
        }
        store(Subject, new State(Current.Strains() | Added, listOf(Clearing), listOf(Resisting), Current.Course()));
        return Added;
    }

    public static boolean treat(LivingEntity Subject) {
        State Current = stateOf(Subject);
        boolean Had = Current.Strains() != 0;
        int[] Resisting = Current.resistingArray();
        for (int Index = 0; Index < StrainCount; Index++) {
            Resisting[Index] = Math.max(Resisting[Index], TreatedResistance);
        }
        store(Subject, new State(0, Zeros, listOf(Resisting), 0));
        settle(Subject);
        return Had;
    }

    public static boolean cure(LivingEntity Subject) {
        boolean Had = stateOf(Subject).Strains() != 0;
        Subject.removeData(Scp129Registry.Infection.get());
        settle(Subject);
        return Had;
    }

    private static void settle(LivingEntity Subject) {
        Scp129Symptoms.interrupt(Subject);
        Scp129Course.clearEffects(Subject);
        if (Subject instanceof ServerPlayer User) {
            Scp129Sync.send(User);
        }
    }

    public static void setCourse(LivingEntity Subject, int Ticks) {
        State Current = stateOf(Subject);
        if (Current.isLethal()) {
            int Rounded = Math.max(Beat, Ticks / Beat * Beat);
            store(Subject, new State(Current.Strains(), Current.Clearing(), Current.Resisting(), Rounded));
        }
    }

    public static void tick(LivingEntity Subject) {
        if (!(Subject.level() instanceof ServerLevel World) || !Subject.isAlive()) {
            return;
        }
        Scp129Symptoms.tick(Subject, World);
        if (isPaused(Subject)) {
            return;
        }
        long Salted = World.getGameTime() + Subject.getId();
        if (Math.floorMod(Salted, Beat) == 0) {
            beat(Subject, World);
        }
        if (Math.floorMod(Salted, Scp129Spread.Interval) == 0) {
            Scp129Spread.breathe(Subject, World);
        }
    }

    private static void beat(LivingEntity Subject, ServerLevel World) {
        State Current = Subject.getExistingDataOrNull(Scp129Registry.Infection.get());
        if (Current == null) {
            Scp129Symptoms.Episode Leftover = Subject.getExistingDataOrNull(Scp129Registry.EpisodeState.get());
            if (Leftover != null && !Leftover.active()) {
                Subject.removeData(Scp129Registry.EpisodeState.get());
            }
            if (Subject instanceof ServerPlayer User) {
                Scp129Sync.check(User, 0);
            }
            return;
        }
        int Strains = Current.Strains();
        boolean Lethal = lethal(Strains);
        int[] Clearing = Current.clearingArray();
        int[] Resisting = Current.resistingArray();
        for (Scp129Strain Strain : Scp129Strain.values()) {
            int Index = Strain.index();
            if (Resisting[Index] > 0) {
                Resisting[Index] = Math.max(0, Resisting[Index] - Beat);
            }
            if (!Strain.in(Strains) || ClearTicks[Index] <= 0 || Strain == Scp129Strain.Four && Lethal) {
                continue;
            }
            Clearing[Index] -= Beat;
            if (Clearing[Index] <= 0) {
                Strains &= ~Strain.bit();
                Clearing[Index] = 0;
                Resisting[Index] = RecoveredResistance;
            }
        }
        int Course = Lethal ? Current.Course() + Beat : 0;
        State Next = new State(Strains, listOf(Clearing), listOf(Resisting), Course);
        store(Subject, Next);
        if (Lethal) {
            Scp129Course.advance(Subject, World, Current.Course(), Course);
        } else if (Current.Course() > 0) {
            Scp129Course.clearEffects(Subject);
        }
        if (Subject instanceof ServerPlayer User) {
            Scp129Sync.check(User, Scp129Course.stageOf(Course));
        }
        if (Subject.isAlive()) {
            Scp129Symptoms.roll(Subject, World, Next);
        }
    }

    public static List<LivingEntity> peopleNear(ServerLevel World, Vec3 Source, Predicate<BlockPos> Ignored, double Range, @Nullable LivingEntity Exclude) {
        List<LivingEntity> Found = new ArrayList<>();
        for (ServerPlayer Target : LabExposure.playersInSight(World, Source, Ignored, Range, Candidate -> Candidate != Exclude && canCatch(Candidate))) {
            Found.add(Target);
        }
        Found.addAll(ControlBodies.inSight(World, Source, Ignored, Range, Candidate -> Candidate != Exclude && canCatch(Candidate)));
        AABB Reach = AABB.ofSize(Source, Range * 2.0, Range * 2.0, Range * 2.0);
        Found.addAll(World.getEntitiesOfClass(AbstractVillager.class, Reach, Target -> Target != Exclude && Target.getEyePosition().distanceToSqr(Source) <= Range * Range
                && canCatch(Target) && LabExposure.sees(World, Source, Target, Ignored)));
        return Found;
    }

    public static void exposeAround(ServerLevel World, Vec3 Source, Predicate<BlockPos> Ignored, double Radius, Scp129Strain Strain) {
        for (LivingEntity Exposed : peopleNear(World, Source, Ignored, Radius, null)) {
            if (!isProtected(Exposed)) {
                infect(Exposed, Strain);
            }
        }
    }
}
