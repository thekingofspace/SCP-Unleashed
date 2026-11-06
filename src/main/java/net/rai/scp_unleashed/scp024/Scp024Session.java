package net.rai.scp_unleashed.scp024;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp024.plan.Scp024Mat;
import net.rai.scp_unleashed.scp024.plan.Scp024Plan;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class Scp024Session {
    public static final int TauntGap = 80;
    public final ServerLevel World;
    public final UUID PlayerId;
    public final UUID Token;
    public final BlockPos Origin;
    public final Scp024Plan Plan;
    public final long Seed;
    public final RandomSource Random;
    public final Scp024Game Game;
    public final AABB Bounds;
    private final List<Entity> Spawned = new ArrayList<>();
    private final Set<String> Said = new HashSet<>();
    @Nullable
    private Scp024Lights Lights;
    private Component Status = Component.empty();
    private int Ticks;
    private int Quiet;
    private boolean Closed;

    public Scp024Session(ServerLevel World, UUID PlayerId, BlockPos Origin, Scp024Game Game, long Seed) {
        this.World = World;
        this.PlayerId = PlayerId;
        this.Token = UUID.randomUUID();
        this.Origin = Origin;
        this.Game = Game;
        this.Seed = Seed;
        this.Random = RandomSource.create(Seed);
        this.Plan = Game.plan(Seed);
        this.Bounds = Scp024Stage.box(Origin, Plan);
    }

    public boolean closed() {
        return Closed;
    }

    @Nullable
    public ServerPlayer anyPlayer() {
        return World.getServer().getPlayerList().getPlayer(PlayerId);
    }

    @Nullable
    public ServerPlayer player() {
        if (Closed) {
            return null;
        }
        ServerPlayer Found = World.getServer().getPlayerList().getPlayer(PlayerId);
        return Found != null && Found.level() == World && Found.isAlive() ? Found : null;
    }

    public int ticks() {
        return Ticks;
    }

    void advance() {
        Ticks++;
        if (Quiet > 0) {
            Quiet--;
        }
    }

    public BlockPos at(int X, int Y, int Z) {
        return Origin.offset(X, Y, Z);
    }

    public BlockPos at(int[] Mark) {
        return Origin.offset(Mark[0], Mark[1], Mark[2]);
    }

    public Vec3 centre(int[] Mark) {
        return Scp024Stage.centre(Origin, Mark);
    }

    public Vec3 centre(int X, int Y, int Z) {
        return new Vec3(Origin.getX() + X + 0.5, Origin.getY() + Y, Origin.getZ() + Z + 0.5);
    }

    public int[] mark(String Label) {
        return Plan.mark(Label);
    }

    public List<int[]> marks(String Label) {
        return Plan.marks(Label);
    }

    public BlockPos local(BlockPos Pos) {
        return Pos.subtract(Origin);
    }

    public Vec3 local(Vec3 Spot) {
        return Spot.subtract(Origin.getX(), Origin.getY(), Origin.getZ());
    }

    public BlockPos standing(Entity Target) {
        return local(BlockPos.containing(Target.getX(), Target.getY() - 0.2, Target.getZ()));
    }

    public boolean onFinish(Entity Target) {
        if (!Target.onGround()) {
            return false;
        }
        BlockPos Cell = standing(Target);
        for (int[] Mark : marks("finish")) {
            if (Mark[0] == Cell.getX() && Mark[1] == Cell.getY() + 1 && Mark[2] == Cell.getZ()) {
                return true;
            }
        }
        return false;
    }

    public boolean onCell(Entity Target, int X, int Y, int Z) {
        BlockPos Cell = standing(Target);
        return Target.onGround() && Cell.getX() == X && Cell.getY() == Y && Cell.getZ() == Z;
    }

    public void toStart() {
        ServerPlayer User = player();
        if (User == null) {
            return;
        }
        int[] Start = mark("start");
        Scp024Stage.teleport(User, World, centre(Start), Scp024Stage.yaw(Start[3]), 0.0f);
    }

    public double height(Entity Target) {
        return Target.getY() - Origin.getY();
    }

    public BlockState state(int X, int Y, int Z) {
        return World.getBlockState(at(X, Y, Z));
    }

    public void set(int X, int Y, int Z, BlockState State) {
        if (Closed) {
            return;
        }
        World.setBlock(at(X, Y, Z), State, Scp024Palette.Quiet);
    }

    public void set(int X, int Y, int Z, Scp024Mat Mat) {
        set(X, Y, Z, Scp024Palette.state(Mat, Scp024Plan.South, 0));
    }

    public void clearCell(int X, int Y, int Z) {
        set(X, Y, Z, Blocks.AIR.defaultBlockState());
    }

    public void restore(int X, int Y, int Z) {
        if (Closed) {
            return;
        }
        Scp024Palette.restore(World, Origin, Plan, X, Y, Z);
    }

    public boolean isOpen(int X, int Y, int Z) {
        BlockState State = state(X, Y, Z);
        return State.isAir() || State.is(Blocks.LIGHT);
    }

    public <T extends Entity> T spawn(T Created) {
        if (Closed) {
            Created.discard();
            return Created;
        }
        Scp024Stage.own(Created, Token);
        if (Created instanceof Mob Creature) {
            Creature.setPersistenceRequired();
        }
        World.addFreshEntity(Created);
        Spawned.add(Created);
        return Created;
    }

    public List<Entity> spawned() {
        Iterator<Entity> Each = Spawned.iterator();
        while (Each.hasNext()) {
            if (Each.next().isRemoved()) {
                Each.remove();
            }
        }
        return Spawned;
    }

    public int alive(Class<?> Kind) {
        int Count = 0;
        for (Entity Found : spawned()) {
            if (Kind.isInstance(Found) && Found.isAlive()) {
                Count++;
            }
        }
        return Count;
    }

    public ItemStack prop(ItemStack Stack) {
        Stack.set(Scp024Registry.PropComponent.get(), true);
        return Stack;
    }

    public void give(ItemStack Stack) {
        ServerPlayer User = player();
        if (User == null) {
            return;
        }
        ItemStack Given = prop(Stack);
        if (!User.getInventory().add(Given)) {
            User.drop(Given, false);
        }
    }

    public static boolean isProp(ItemStack Stack) {
        return Boolean.TRUE.equals(Stack.get(Scp024Registry.PropComponent.get()));
    }

    public static int takeProps(ServerPlayer User) {
        Inventory Pockets = User.getInventory();
        int Taken = 0;
        for (int Slot = 0; Slot < Pockets.getContainerSize(); Slot++) {
            if (isProp(Pockets.getItem(Slot))) {
                Pockets.setItem(Slot, ItemStack.EMPTY);
                Taken++;
            }
        }
        if (isProp(User.containerMenu.getCarried())) {
            User.containerMenu.setCarried(ItemStack.EMPTY);
            Taken++;
        }
        for (int Slot = 0; Slot < User.inventoryMenu.getCraftSlots().getContainerSize(); Slot++) {
            if (isProp(User.inventoryMenu.getCraftSlots().getItem(Slot))) {
                User.inventoryMenu.getCraftSlots().setItem(Slot, ItemStack.EMPTY);
                Taken++;
            }
        }
        if (Taken > 0) {
            User.inventoryMenu.broadcastChanges();
        }
        return Taken;
    }

    public Component status() {
        return Status;
    }

    public void status(Component Text) {
        Status = Text;
    }

    public void objective(Component Text) {
        ServerPlayer User = player();
        if (User != null) {
            User.displayClientMessage(Text, true);
        }
    }

    public void taunt(String Key) {
        if (Quiet > 0) {
            return;
        }
        Quiet = TauntGap;
        ServerPlayer User = player();
        Scp024Quips.say(User, Key);
        Scp024Sounds.cue(User, Random.nextBoolean() ? Scp024Cue.Ooh : Scp024Cue.Laugh);
        if (Lights != null) {
            Lights.mood(Scp024Lights.Mood.Red, 24);
        }
    }

    public void tauntOnce(String Key) {
        if (Said.add(Key)) {
            Quiet = 0;
            taunt(Key);
        }
    }

    public void cheer(String Key) {
        ServerPlayer User = player();
        Scp024Quips.say(User, Key);
        Scp024Sounds.cue(User, Scp024Cue.Ding);
        Scp024Sounds.cue(User, Scp024Cue.Applause);
        if (Lights != null) {
            Lights.mood(Scp024Lights.Mood.Flash, 20);
        }
    }

    public void cue(Scp024Cue Cue) {
        Scp024Sounds.cue(player(), Cue);
    }

    public void sound(Vec3 Spot, SoundEvent Sound, float Volume, float Pitch) {
        if (Closed) {
            return;
        }
        Scp024Sounds.play(World, Spot, Sound, Volume, Pitch);
    }

    public void particles(ParticleOptions Kind, Vec3 Spot, int Count, double Spread, double Speed) {
        if (Closed) {
            return;
        }
        World.sendParticles(Kind, Spot.x, Spot.y, Spot.z, Count, Spread, Spread, Spread, Speed);
    }

    public void blast(Vec3 Spot, double Radius, float Damage) {
        if (Closed) {
            return;
        }
        Scp024Hazards.blast(this, Spot, Radius, Damage);
    }

    @Nullable
    public Scp024Lights lights() {
        return Lights;
    }

    void lights(@Nullable Scp024Lights Rig) {
        Lights = Rig;
    }

    public void cleanup() {
        Closed = true;
        for (Entity Found : new ArrayList<>(Spawned)) {
            if (!Found.isRemoved()) {
                Found.discard();
            }
        }
        Spawned.clear();
        if (Lights != null) {
            Lights.remove();
            Lights = null;
        }
        Scp024Stage.discardOwned(World, Bounds, null);
    }
}
