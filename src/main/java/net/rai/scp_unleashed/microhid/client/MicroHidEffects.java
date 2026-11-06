package net.rai.scp_unleashed.microhid.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.guns.GunsRegistry;
import net.rai.scp_unleashed.guns.client.GunClientState;
import net.rai.scp_unleashed.guns.client.GunMuzzle;
import net.rai.scp_unleashed.microhid.MicroHidBeam;
import net.rai.scp_unleashed.microhid.MicroHidEnergy;
import net.rai.scp_unleashed.microhid.MicroHidGlow;
import net.rai.scp_unleashed.microhid.MicroHidItem;
import net.rai.scp_unleashed.microhid.MicroHidRegistry;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import net.rai.scp_unleashed.tesla.TeslaRegistry;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class MicroHidEffects {
    public static final ResourceLocation ArcTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/block/tesla/fx_arc.png");
    public static final ResourceLocation GlowTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/block/tesla/fx_glow.png");
    public static final int WindTicks = 12;
    public static final int MainLevels = 6;
    public static final int BranchLevels = 3;
    public static final double MaxSwing = 1.5;
    public static final double SwingShare = 0.11;
    public static final double TaperLength = 1.2;
    public static final float[] Core = {0.92F, 0.97F, 1.0F};
    public static final float[] Halo = {0.36F, 0.6F, 1.0F};
    public static final float[] Haze = {0.14F, 0.28F, 0.9F};
    public static final float ShakeFire = 0.16F;
    public static final float ShakeSpin = 0.07F;
    public static final double NearFadeStart = 0.15;
    public static final double NearFadeRange = 0.6;
    public static final Set<Phase> SpinPhases = EnumSet.of(Phase.Spin, Phase.Fire);
    public static final Set<Phase> FirePhases = EnumSet.of(Phase.Fire);
    private static final Map<Integer, Shooter> Shooters = new HashMap<>();
    @Nullable
    private static Vec3 LocalMuzzle;
    private static long Ticks;

    public enum Phase {
        Idle,
        Spin,
        Fire,
        Wind
    }

    private MicroHidEffects() {
    }

    private record Bolt(float[] Along, float[] Across, float[] Over, float Width, float Strength, boolean Taper) {
    }

    private record Arc(List<Vec3> Offsets, float Width, float Strength, boolean AtEnd) {
    }

    private record Shot(Shooter State, Vec3 Muzzle, @Nullable Vec3 End, @Nullable Vec3 Surface, boolean Seen, boolean Close, double Edge, float Strength) {
    }

    private record Painter(VertexConsumer Consumer, PoseStack.Pose Last, Vec3 Eye, Pass Stage, double Edge, float Alpha, @Nullable Vec3 Impact, @Nullable Vec3 Surface) {
    }

    private enum Pass {
        World,
        Hand,
        Depth;

        RenderType type(ResourceLocation Texture) {
            return switch (this) {
                case World -> MicroHidRenderTypes.world(Texture);
                case Hand -> MicroHidRenderTypes.hand(Texture);
                case Depth -> MicroHidRenderTypes.depth(Texture);
            };
        }

        boolean takes(boolean InHand) {
            return switch (this) {
                case World -> !InHand;
                case Hand -> InHand;
                case Depth -> true;
            };
        }
    }

    private static final class Shooter {
        private Phase Current = Phase.Idle;
        private long Since;
        private final List<Bolt> Bolts = new ArrayList<>();
        private final List<Arc> Arcs = new ArrayList<>();
        private float Flicker = 1.0F;
        private long NextCrackle;
        private long NextSpark;
        private double Length = 1.0;
        private boolean Seen;
        private boolean Holding;
    }

    public static Phase phaseOf(Entity Holder) {
        Shooter Found = Shooters.get(Holder.getId());
        return Found == null ? Phase.Idle : Found.Current;
    }

    public static float spinProgress(LivingEntity Holder, float Partial) {
        return Mth.clamp((Holder.getTicksUsingItem() + Partial) / MicroHidEnergy.SpinUpTicks, 0.0F, 1.0F);
    }

    public static float windProgress(Entity Holder, float Partial) {
        Shooter Found = Shooters.get(Holder.getId());
        if (Found == null || Found.Current != Phase.Wind) {
            return 1.0F;
        }
        return Mth.clamp((Ticks - Found.Since + Partial) / WindTicks, 0.0F, 1.0F);
    }

    public static boolean isFiring(Entity Holder) {
        return phaseOf(Holder) == Phase.Fire;
    }

    private static Phase wanted(Player Holder, Shooter State) {
        ItemStack Stack = Holder.getMainHandItem();
        boolean Using = MicroHidItem.isMicroHid(Stack) && Holder.isUsingItem() && Holder.getUsedItemHand() == InteractionHand.MAIN_HAND && MicroHidEnergy.canFire(Stack)
                && Holder.isAlive();
        if (Using) {
            return MicroHidBeam.firing(Holder.getTicksUsingItem()) ? Phase.Fire : Phase.Spin;
        }
        if (State.Current == Phase.Spin || State.Current == Phase.Fire) {
            return Phase.Wind;
        }
        if (State.Current == Phase.Wind && Ticks - State.Since < WindTicks) {
            return Phase.Wind;
        }
        return Phase.Idle;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post Event) {
        Ticks++;
        Minecraft Game = Minecraft.getInstance();
        ClientLevel World = Game.level;
        if (World == null) {
            Shooters.clear();
            LocalMuzzle = null;
            return;
        }
        for (Shooter State : Shooters.values()) {
            State.Seen = false;
        }
        RandomSource Random = World.getRandom();
        for (Player Holder : World.players()) {
            Shooter State = Shooters.get(Holder.getId());
            if (State == null) {
                if (!MicroHidItem.isMicroHid(Holder.getMainHandItem())) {
                    continue;
                }
                State = new Shooter();
                State.Since = Ticks;
                Shooters.put(Holder.getId(), State);
            }
            State.Seen = true;
            State.Holding = MicroHidItem.isMicroHid(Holder.getMainHandItem());
            Phase Next = wanted(Holder, State);
            if (Next != State.Current) {
                Phase Previous = State.Current;
                State.Current = Next;
                State.Since = Ticks;
                changed(World, Holder, State, Previous, Next);
            }
            State.Bolts.clear();
            State.Arcs.clear();
            if (State.Current == Phase.Fire) {
                fireTick(Game, World, Holder, State, Random);
            } else if (State.Current == Phase.Spin) {
                spinTick(Game, World, Holder, State, Random);
            } else if (State.Current == Phase.Wind) {
                windTick(Game, World, Holder, State, Random);
            }
        }
        Iterator<Map.Entry<Integer, Shooter>> Entries = Shooters.entrySet().iterator();
        while (Entries.hasNext()) {
            Shooter State = Entries.next().getValue();
            if (!State.Seen || !State.Holding && State.Current == Phase.Idle && Ticks - State.Since > WindTicks * 4L) {
                Entries.remove();
            }
        }
    }

    private static void changed(ClientLevel World, Player Holder, Shooter State, Phase Previous, Phase Next) {
        Minecraft Game = Minecraft.getInstance();
        RandomSource Random = World.getRandom();
        if (Next == Phase.Spin) {
            loop(Game, Holder, MicroHidRegistry.ChargeUp.get(), 0.9F, 1.0F, false, SpinPhases);
            loop(Game, Holder, MicroHidRegistry.SpinWhine.get(), 0.3F, 1.55F, false, SpinPhases);
            loop(Game, Holder, MicroHidRegistry.SpinCharge.get(), 0.25F, 1.3F, false, SpinPhases);
        } else if (Next == Phase.Fire) {
            Vec3 At = Holder.getEyePosition();
            World.playLocalSound(At.x, At.y, At.z, MicroHidRegistry.Zap.get(), SoundSource.PLAYERS, 0.9F, 0.95F + Random.nextFloat() * 0.1F, false);
            World.playLocalSound(At.x, At.y, At.z, MicroHidRegistry.Ignite.get(), SoundSource.PLAYERS, 0.25F, 1.8F + Random.nextFloat() * 0.1F, false);
            loop(Game, Holder, MicroHidRegistry.ArcLoop.get(), 0.75F, 1.0F, true, FirePhases);
            loop(Game, Holder, MicroHidRegistry.LoopDrone.get(), 0.18F, 1.3F, true, FirePhases);
            State.NextCrackle = Ticks + 2;
            State.NextSpark = Ticks + 3;
        } else if (Next == Phase.Wind && (Previous == Phase.Fire || Previous == Phase.Spin)) {
            Vec3 At = Holder.getEyePosition();
            float Scale = Previous == Phase.Fire ? 1.0F : 0.7F;
            World.playLocalSound(At.x, At.y, At.z, MicroHidRegistry.PowerDown.get(), SoundSource.PLAYERS, 0.85F * Scale, 1.0F, false);
            World.playLocalSound(At.x, At.y, At.z, MicroHidRegistry.WindDown.get(), SoundSource.PLAYERS, 0.25F * Scale, 1.5F, false);
            World.playLocalSound(At.x, At.y, At.z, MicroHidRegistry.WindDeplete.get(), SoundSource.PLAYERS, 0.2F * Scale, 1.6F, false);
        }
    }

    private static void loop(Minecraft Game, Player Holder, SoundEvent Sound, float Loudness, float Pitch, boolean Looping, Set<Phase> Keep) {
        Game.getSoundManager().play(new MicroHidSound(Sound, Holder, Loudness, Pitch, Looping, Keep));
    }

    @Nullable
    private static Vec3 muzzleAt(Minecraft Game, Player Holder, float Partial) {
        boolean FirstPerson = Holder == Game.player && Game.options.getCameraType().isFirstPerson();
        if (FirstPerson) {
            return LocalMuzzle;
        }
        return GunMuzzle.thirdPersonWorld(Holder, Holder.getMainHandItem(), GunKind.MicroHid, Partial);
    }

    private static BlockHitResult impactAt(ClientLevel World, Player Holder, float Partial) {
        Vec3 Eye = Holder.getEyePosition(Partial);
        Vec3 Far = Eye.add(Holder.getViewVector(Partial).scale(GunKind.MicroHid.getStats().getRange()));
        BlockHitResult Wall = World.clip(new ClipContext(Eye, Far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, Holder));
        return Wall.getType() == HitResult.Type.MISS ? BlockHitResult.miss(Far, Direction.getNearest(Far.x - Eye.x, Far.y - Eye.y, Far.z - Eye.z), BlockPos.containing(Far)) : Wall;
    }

    private static Vec3 endAt(ClientLevel World, Player Holder, float Partial) {
        Vec3 Eye = Holder.getEyePosition(Partial);
        Vec3 Look = Holder.getViewVector(Partial);
        return MicroHidBeam.reach(World, Holder, Eye, Look, GunKind.MicroHid.getStats().getRange());
    }

    @Nullable
    private static Vec3 surfaceNormal(ClientLevel World, Player Holder) {
        Vec3 Eye = Holder.getEyePosition();
        Vec3 Far = Eye.add(Holder.getLookAngle().scale(GunKind.MicroHid.getStats().getRange()));
        BlockHitResult Wall = World.clip(new ClipContext(Eye, Far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, Holder));
        return Wall.getType() == HitResult.Type.MISS ? null : Vec3.atLowerCornerOf(Wall.getDirection().getNormal());
    }

    private static void fireTick(Minecraft Game, ClientLevel World, Player Holder, Shooter State, RandomSource Random) {
        Vec3 Start = muzzleAt(Game, Holder, 1.0F);
        Vec3 End = endAt(World, Holder, 1.0F);
        if (Start == null) {
            Start = MicroHidBeam.front(Holder);
        }
        double Length = Math.max(0.5, Start.distanceTo(End));
        State.Length = Length;
        State.Flicker = 0.78F + Random.nextFloat() * 0.22F;
        double Swing = Math.min(MaxSwing, Length * SwingShare);
        State.Bolts.add(bolt(Random, Length, MainLevels, Swing, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, false));
        State.Bolts.add(bolt(Random, Length, MainLevels - 1, Swing * 0.85, 0.7F, 0.8F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, false));
        State.Bolts.add(bolt(Random, Length, MainLevels - 1, Swing * 1.2, 0.45F, 0.55F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, false));
        int Branches = 4 + Random.nextInt(4);
        Bolt Spine = State.Bolts.getFirst();
        for (int Index = 0; Index < Branches; Index++) {
            int Count = Spine.Along().length;
            int From = (int) (Count * (0.15 + Random.nextDouble() * 0.7));
            double Reach = 1.2 + Random.nextDouble() * 2.0;
            double Ahead = 0.45 + Random.nextDouble() * 0.45;
            double Turn = Random.nextDouble() * Math.PI * 2.0;
            double Side = Math.sqrt(1.0 - Ahead * Ahead) * Reach;
            float Tip = (float) Math.min(1.0, Spine.Along()[From] + Ahead * Reach / Length);
            float TipAcross = (float) (Spine.Across()[From] + Math.cos(Turn) * Side);
            float TipOver = (float) (Spine.Over()[From] + Math.sin(Turn) * Side);
            State.Bolts.add(bolt(Random, Reach, BranchLevels, Reach * 0.22, 0.45F, 0.75F, Spine.Along()[From], Spine.Across()[From], Spine.Over()[From], Tip, TipAcross, TipOver, true));
        }
        Vec3 Normal = surfaceNormal(World, Holder);
        int Burst = 5 + Random.nextInt(4);
        for (int Index = 0; Index < Burst; Index++) {
            Vec3 Heading = new Vec3(Random.nextGaussian(), Random.nextGaussian(), Random.nextGaussian()).normalize();
            if (Normal != null) {
                Heading = Heading.add(Normal.scale(0.9)).normalize();
            }
            State.Arcs.add(new Arc(path(Random, Vec3.ZERO, Heading.scale(0.6 + Random.nextDouble() * 1.2), 3, 0.3), 1.0F, 0.9F, true));
        }
        for (int Index = 0; Index < 3; Index++) {
            double Along = Random.nextDouble();
            Vec3 Point = Start.add(End.subtract(Start).scale(Along));
            World.addParticle(ParticleTypes.ELECTRIC_SPARK, Point.x, Point.y, Point.z, Random.nextGaussian() * 0.06, Random.nextGaussian() * 0.06, Random.nextGaussian() * 0.06);
        }
        for (int Index = 0; Index < 3; Index++) {
            World.addParticle(TeslaRegistry.Spark.get(), End.x, End.y, End.z, Random.nextGaussian() * 0.15, Random.nextDouble() * 0.12, Random.nextGaussian() * 0.15);
        }
        if (Random.nextInt(3) == 0) {
            World.addParticle(GunsRegistry.EnergySpark.get(), End.x, End.y, End.z, Random.nextGaussian() * 0.05, 0.04, Random.nextGaussian() * 0.05);
        }
        int Used = Holder.getTicksUsingItem();
        if (MicroHidBeam.pulsing(Used)) {
            LocalPlayer User = Game.player;
            if (Holder == User) {
                GunClientState.onLocalShot(User, GunKind.MicroHid);
            } else {
                GunClientState.onShotSeen(Holder.getId(), GunKind.MicroHid, User);
            }
        }
        if (Ticks >= State.NextCrackle) {
            State.NextCrackle = Ticks + 2 + Random.nextInt(5);
            Vec3 At = Random.nextBoolean() ? Start.add(End.subtract(Start).scale(Random.nextDouble() * 0.3)) : End;
            World.playLocalSound(At.x, At.y, At.z, MicroHidRegistry.ArcCrackle.get(), SoundSource.PLAYERS, 0.4F + Random.nextFloat() * 0.3F, 0.85F + Random.nextFloat() * 0.35F, false);
        }
        if (Ticks >= State.NextSpark) {
            State.NextSpark = Ticks + 4 + Random.nextInt(6);
            World.playLocalSound(Start.x, Start.y, Start.z, MicroHidRegistry.Spark.get(), SoundSource.PLAYERS, 0.2F, 1.3F + Random.nextFloat() * 0.4F, false);
        }
    }

    private static void spinTick(Minecraft Game, ClientLevel World, Player Holder, Shooter State, RandomSource Random) {
        float Progress = spinProgress(Holder, 0.0F);
        State.Flicker = 0.7F + Random.nextFloat() * 0.3F;
        int Count = Random.nextFloat() < 0.35F + 0.65F * Progress ? 1 + Math.round(Progress * 2.0F) : 0;
        for (int Index = 0; Index < Count; Index++) {
            Vec3 First = new Vec3(Random.nextGaussian(), Random.nextGaussian(), Random.nextGaussian()).normalize().scale(0.12 + 0.18 * Progress);
            Vec3 Second = new Vec3(Random.nextGaussian(), Random.nextGaussian(), Random.nextGaussian()).normalize().scale(0.12 + 0.18 * Progress);
            State.Arcs.add(new Arc(path(Random, First, Second, 2, 0.35), 0.35F, 0.5F + 0.5F * Progress, false));
        }
        Vec3 Muzzle = muzzleAt(Game, Holder, 1.0F);
        if (Muzzle != null && Random.nextFloat() < 0.25F + 0.5F * Progress) {
            Vec3 Offset = new Vec3(Random.nextGaussian(), Random.nextGaussian(), Random.nextGaussian()).normalize().scale(0.2 + 0.2 * Random.nextDouble());
            World.addParticle(ParticleTypes.ELECTRIC_SPARK, Muzzle.x + Offset.x, Muzzle.y + Offset.y, Muzzle.z + Offset.z, -Offset.x * 0.2, -Offset.y * 0.2, -Offset.z * 0.2);
        }
    }

    private static void windTick(Minecraft Game, ClientLevel World, Player Holder, Shooter State, RandomSource Random) {
        float Left = 1.0F - windProgress(Holder, 0.0F);
        State.Flicker = 0.6F + Random.nextFloat() * 0.4F;
        Vec3 Muzzle = muzzleAt(Game, Holder, 1.0F);
        if (Muzzle != null && Random.nextFloat() < 0.4F * Left) {
            World.addParticle(ParticleTypes.SMOKE, Muzzle.x, Muzzle.y, Muzzle.z, 0.0, 0.02, 0.0);
        }
    }

    private static Bolt bolt(RandomSource Random, double Length, int Levels, double Swing, float Width, float Strength, float FromAlong, float FromAcross, float FromOver,
                             float ToAlong, float ToAcross, float ToOver, boolean Taper) {
        int Count = (1 << Levels) + 1;
        float[] Along = new float[Count];
        float[] Across = new float[Count];
        float[] Over = new float[Count];
        Along[0] = FromAlong;
        Across[0] = FromAcross;
        Over[0] = FromOver;
        Along[Count - 1] = ToAlong;
        Across[Count - 1] = ToAcross;
        Over[Count - 1] = ToOver;
        int Step = Count - 1;
        double Amount = Swing;
        while (Step > 1) {
            int Half = Step / 2;
            for (int Index = Half; Index < Count; Index += Step) {
                Along[Index] = (Along[Index - Half] + Along[Index + Half]) * 0.5F;
                Across[Index] = (float) ((Across[Index - Half] + Across[Index + Half]) * 0.5 + (Random.nextDouble() * 2.0 - 1.0) * Amount);
                Over[Index] = (float) ((Over[Index - Half] + Over[Index + Half]) * 0.5 + (Random.nextDouble() * 2.0 - 1.0) * Amount);
            }
            Amount *= 0.55;
            Step = Half;
        }
        if (!Taper) {
            for (int Index = 1; Index < Count - 1; Index++) {
                float Near = (float) Mth.clamp(Along[Index] * Length / TaperLength, 0.0, 1.0);
                Across[Index] *= Near;
                Over[Index] *= Near;
            }
        }
        return new Bolt(Along, Across, Over, Width, Strength, Taper);
    }

    private static List<Vec3> path(RandomSource Random, Vec3 Start, Vec3 End, int Levels, double Roughness) {
        List<Vec3> Points = new ArrayList<>();
        Points.add(Start);
        Points.add(End);
        double Amount = Start.distanceTo(End) * Roughness;
        for (int Step = 0; Step < Levels; Step++) {
            List<Vec3> Next = new ArrayList<>(Points.size() * 2);
            Next.add(Points.getFirst());
            for (int Index = 0; Index + 1 < Points.size(); Index++) {
                Vec3 A = Points.get(Index);
                Vec3 B = Points.get(Index + 1);
                Vec3 Middle = A.add(B).scale(0.5).add((Random.nextDouble() * 2.0 - 1.0) * Amount, (Random.nextDouble() * 2.0 - 1.0) * Amount, (Random.nextDouble() * 2.0 - 1.0) * Amount);
                Next.add(Middle);
                Next.add(B);
            }
            Points = Next;
            Amount *= 0.5;
        }
        return Points;
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent Event) {
        if (Event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft Game = Minecraft.getInstance();
        ClientLevel World = Game.level;
        if (World == null || ShaderCompat.renderingShadowPass()) {
            return;
        }
        float Partial = Event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Camera View = Event.getCamera();
        Matrix4f Projection = Event.getProjectionMatrix();
        LocalPlayer User = Game.player;
        LocalMuzzle = null;
        if (User != null && Game.options.getCameraType().isFirstPerson() && MicroHidItem.isMicroHid(User.getMainHandItem())) {
            LocalMuzzle = GunMuzzle.firstPersonWorld(User, User.getMainHandItem(), GunKind.MicroHid, View, Projection, Partial);
        }
        if (Shooters.isEmpty()) {
            return;
        }
        Vec3 Eye = View.getPosition();
        List<Shot> Shots = new ArrayList<>();
        for (Map.Entry<Integer, Shooter> Entry : Shooters.entrySet()) {
            Shooter State = Entry.getValue();
            if (State.Current == Phase.Idle || !(World.getEntity(Entry.getKey()) instanceof Player Holder)) {
                continue;
            }
            Vec3 Muzzle = muzzleAt(Game, Holder, Partial);
            if (Muzzle == null) {
                continue;
            }
            boolean Close = Holder == User && Game.options.getCameraType().isFirstPerson();
            double Edge = Close ? MicroHidGlow.handEdge(Muzzle.distanceTo(Eye)) : -1.0;
            if (State.Current == Phase.Fire) {
                BlockHitResult Impact = impactAt(World, Holder, Partial);
                Vec3 End = Impact.getLocation();
                Vec3 Surface = Impact.getType() == HitResult.Type.MISS ? null : Vec3.atLowerCornerOf(Impact.getDirection().getNormal());
                Shots.add(new Shot(State, Muzzle, End, Surface, MicroHidGlow.seen(World, Eye, End), Close, Edge, State.Flicker));
            } else {
                float Glow = State.Current == Phase.Spin ? spinProgress(Holder, Partial) : 1.0F - windProgress(Holder, Partial);
                Shots.add(new Shot(State, Muzzle, null, null, true, Close, Edge, State.Flicker * Glow));
            }
        }
        if (Shots.isEmpty()) {
            return;
        }
        boolean Fabulous = Minecraft.useShaderTransparency() && !ShaderCompat.shaderPackInUse();
        float Alpha = Fabulous ? MicroHidGlow.FabulousAlpha : 1.0F;
        MultiBufferSource.BufferSource Buffers = Game.renderBuffers().bufferSource();
        PoseStack.Pose Last = Event.getPoseStack().last();
        paint(Buffers, Last, Eye, Shots, Pass.World, Alpha);
        paint(Buffers, Last, Eye, Shots, Pass.Hand, Alpha);
        if (Fabulous) {
            paint(Buffers, Last, Eye, Shots, Pass.Depth, 1.0F);
        }
    }

    private static void paint(MultiBufferSource.BufferSource Buffers, PoseStack.Pose Last, Vec3 Eye, List<Shot> Shots, Pass Stage, float Alpha) {
        RenderType Arcs = Stage.type(ArcTexture);
        VertexConsumer Consumer = Buffers.getBuffer(Arcs);
        for (Shot Each : Shots) {
            Painter Paint = painter(Consumer, Last, Eye, Stage, Alpha, Each);
            Shooter State = Each.State();
            if (Each.End() != null) {
                drawBeam(Paint, State, Each.Muzzle(), Each.End(), Each.Close());
            } else {
                for (Arc Spark : State.Arcs) {
                    ribbon(Paint, offset(Spark.Offsets(), Each.Muzzle()), 0.022F * Spark.Width(), Core, Spark.Strength() * State.Flicker, false);
                    ribbon(Paint, offset(Spark.Offsets(), Each.Muzzle()), 0.07F * Spark.Width(), Halo, 0.6F * Spark.Strength() * State.Flicker, false);
                }
            }
        }
        Buffers.endBatch(Arcs);
        RenderType Glows = Stage.type(GlowTexture);
        Consumer = Buffers.getBuffer(Glows);
        for (Shot Each : Shots) {
            Painter Paint = painter(Consumer, Last, Eye, Stage, Alpha, Each);
            float Strength = Each.Strength();
            float Scale = Each.Close() ? 0.45F : 1.0F;
            Vec3 Muzzle = Each.Muzzle();
            Vec3 End = Each.End();
            if (End != null) {
                sprite(Paint, Muzzle, 0.32F * Scale, Halo, 0.9F * Strength, 0.32F * Scale);
                sprite(Paint, Muzzle, 0.14F * Scale, Core, Strength, 0.14F * Scale);
                sprite(Paint, End, 1.7F, Haze, 0.75F * Strength, Each.Seen() ? 1.7F : 0.0F);
                sprite(Paint, End, 0.9F, Halo, 0.9F * Strength, Each.Seen() ? 0.9F : 0.0F);
                sprite(Paint, End, 0.35F, Core, Strength, Each.Seen() ? 0.35F : 0.0F);
            } else {
                float Flare = 0.25F * Scale * (0.4F + 0.6F * Strength);
                sprite(Paint, Muzzle, Flare, Halo, 0.8F * Strength, Flare);
                sprite(Paint, Muzzle, 0.1F * Scale, Core, 0.7F * Strength, 0.1F * Scale);
            }
        }
        Buffers.endBatch(Glows);
    }

    private static void drawBeam(Painter Paint, Shooter State, Vec3 Start, Vec3 End, boolean Close) {
        Vec3 Span = End.subtract(Start);
        double Length = Span.length();
        if (Length < 0.05) {
            return;
        }
        Vec3 Forward = Span.scale(1.0 / Length);
        Vec3 Helper = Math.abs(Forward.y) > 0.9 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 AxisA = Forward.cross(Helper).normalize();
        Vec3 AxisB = Forward.cross(AxisA).normalize();
        float Flicker = State.Flicker;
        float Scale = Close ? 0.75F : 1.0F;
        List<Vec3> Straight = List.of(Start, Start.add(Span.scale(0.5)), End);
        ribbon(Paint, Straight, 0.34F * Scale, Haze, 0.45F * Flicker, false);
        for (Bolt Streak : State.Bolts) {
            List<Vec3> Points = new ArrayList<>(Streak.Along().length);
            for (int Index = 0; Index < Streak.Along().length; Index++) {
                Points.add(Start.add(Forward.scale(Streak.Along()[Index] * Length)).add(AxisA.scale(Streak.Across()[Index])).add(AxisB.scale(Streak.Over()[Index])));
            }
            float Width = Streak.Width() * Scale;
            float Strength = Streak.Strength() * Flicker;
            ribbon(Paint, Points, 0.07F * Width, Core, Strength, Streak.Taper());
            ribbon(Paint, Points, 0.24F * Width, Halo, 0.7F * Strength, Streak.Taper());
            if (!Streak.Taper()) {
                ribbon(Paint, Points, 0.6F * Width, Haze, 0.25F * Strength, false);
            }
        }
        for (Arc Spark : State.Arcs) {
            List<Vec3> Points = offset(Spark.Offsets(), Spark.AtEnd() ? End : Start);
            ribbon(Paint, Points, 0.05F * Spark.Width(), Core, Spark.Strength() * Flicker, true);
            ribbon(Paint, Points, 0.16F * Spark.Width(), Halo, 0.6F * Spark.Strength() * Flicker, true);
        }
    }

    private static List<Vec3> offset(List<Vec3> Offsets, Vec3 Anchor) {
        List<Vec3> Points = new ArrayList<>(Offsets.size());
        for (Vec3 Point : Offsets) {
            Points.add(Anchor.add(Point));
        }
        return Points;
    }

    private static void ribbon(Painter Paint, List<Vec3> Path, float HalfWidth, float[] Colour, float Strength, boolean Taper) {
        int Count = Path.size();
        if (Strength <= 0.0F || Count < 2) {
            return;
        }
        Vec3 Eye = Paint.Eye();
        Vec3[] Sides = new Vec3[Count];
        for (int Index = 0; Index < Count; Index++) {
            Vec3 Along = Path.get(Math.min(Count - 1, Index + 1)).subtract(Path.get(Math.max(0, Index - 1)));
            Vec3 Side = Along.cross(Eye.subtract(Path.get(Index)));
            if (Side.lengthSqr() < 1.0E-12) {
                Sides[Index] = Index > 0 ? Sides[Index - 1] : Vec3.ZERO;
                continue;
            }
            float Width = Taper ? HalfWidth * (1.0F - 0.85F * Index / (Count - 1)) : HalfWidth;
            Side = Side.normalize().scale(Width);
            if (Index > 0 && Side.dot(Sides[Index - 1]) < 0.0) {
                Side = Side.scale(-1.0);
            }
            Sides[Index] = Side;
        }
        double Pull = MicroHidGlow.ribbonPull(HalfWidth);
        for (int Index = 0; Index + 1 < Count; Index++) {
            Vec3 A = Path.get(Index);
            Vec3 B = Path.get(Index + 1);
            float Amount = Strength * nearFade(Eye, A, B);
            float Red = Mth.clamp(Colour[0] * Amount, 0.0F, 1.0F);
            float Green = Mth.clamp(Colour[1] * Amount, 0.0F, 1.0F);
            float Blue = Mth.clamp(Colour[2] * Amount, 0.0F, 1.0F);
            if (Red + Green + Blue <= 0.0F) {
                continue;
            }
            Vec3 Facing = Eye.subtract(A);
            Vec3 Normal = Facing.lengthSqr() < 1.0E-9 ? new Vec3(0.0, 1.0, 0.0) : Facing.normalize();
            double FromDistance = A.distanceTo(Eye);
            double ToDistance = B.distanceTo(Eye);
            boolean FromHand = MicroHidGlow.inHand(FromDistance, Paint.Edge());
            boolean ToHand = MicroHidGlow.inHand(ToDistance, Paint.Edge());
            if (FromHand == ToHand) {
                if (Paint.Stage().takes(FromHand)) {
                    strip(Paint, A, Sides[Index], 1.0F, B, Sides[Index + 1], 0.0F, Red, Green, Blue, Normal, Pull);
                }
                continue;
            }
            double Cut = MicroHidGlow.crossing(FromDistance, ToDistance, Paint.Edge());
            Vec3 Middle = A.add(B.subtract(A).scale(Cut));
            Vec3 MiddleSide = Sides[Index].add(Sides[Index + 1].subtract(Sides[Index]).scale(Cut));
            float MiddleV = (float) (1.0 - Cut);
            if (Paint.Stage().takes(FromHand)) {
                strip(Paint, A, Sides[Index], 1.0F, Middle, MiddleSide, MiddleV, Red, Green, Blue, Normal, Pull);
            }
            if (Paint.Stage().takes(ToHand)) {
                strip(Paint, Middle, MiddleSide, MiddleV, B, Sides[Index + 1], 0.0F, Red, Green, Blue, Normal, Pull);
            }
        }
    }

    private static void strip(Painter Paint, Vec3 A, Vec3 SideA, float FromV, Vec3 B, Vec3 SideB, float ToV, float Red, float Green, float Blue, Vec3 Normal, double Pull) {
        Vec3 Eye = Paint.Eye();
        vertex(Paint, A.subtract(SideA).subtract(Eye), 0.0F, FromV, Red, Green, Blue, Normal, Pull);
        vertex(Paint, A.add(SideA).subtract(Eye), 1.0F, FromV, Red, Green, Blue, Normal, Pull);
        vertex(Paint, B.add(SideB).subtract(Eye), 1.0F, ToV, Red, Green, Blue, Normal, Pull);
        vertex(Paint, B.subtract(SideB).subtract(Eye), 0.0F, ToV, Red, Green, Blue, Normal, Pull);
    }

    private static float nearFade(Vec3 Eye, Vec3 A, Vec3 B) {
        Vec3 Along = B.subtract(A);
        double Fraction = Mth.clamp(Eye.subtract(A).dot(Along) / Math.max(Along.lengthSqr(), 1.0E-9), 0.0, 1.0);
        double Distance = Eye.distanceTo(A.add(Along.scale(Fraction)));
        return (float) Mth.clamp((Distance - NearFadeStart) / NearFadeRange, 0.0, 1.0);
    }

    private static Painter painter(VertexConsumer Consumer, PoseStack.Pose Last, Vec3 Eye, Pass Stage, float Alpha, Shot Each) {
        boolean Lifted = Each.Seen() && Each.End() != null && Each.Surface() != null;
        return new Painter(Consumer, Last, Eye, Stage, Each.Edge(), Alpha, Lifted ? Each.End().subtract(Eye) : null, Lifted ? Each.Surface() : null);
    }

    private static void sprite(Painter Paint, Vec3 Centre, float Radius, float[] Colour, float Strength, double Pull) {
        if (Strength <= 0.0F) {
            return;
        }
        Vec3 Eye = Paint.Eye();
        if (!Paint.Stage().takes(MicroHidGlow.inHand(Centre.distanceTo(Eye), Paint.Edge()))) {
            return;
        }
        Vec3 Toward = Eye.subtract(Centre);
        Vec3 Facing = Toward.lengthSqr() < 1.0E-9 ? new Vec3(0.0, 0.0, 1.0) : Toward.normalize();
        Vec3 Across = Facing.cross(new Vec3(0.0, 1.0, 0.0));
        Across = Across.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : Across.normalize();
        Vec3 Over = Across.cross(Facing).normalize();
        Vec3 U = Across.scale(Radius);
        Vec3 V = Over.scale(Radius);
        Vec3 Local = Centre.subtract(Eye);
        float Red = Mth.clamp(Colour[0] * Strength, 0.0F, 1.0F);
        float Green = Mth.clamp(Colour[1] * Strength, 0.0F, 1.0F);
        float Blue = Mth.clamp(Colour[2] * Strength, 0.0F, 1.0F);
        vertex(Paint, Local.subtract(U).subtract(V), 0.0F, 1.0F, Red, Green, Blue, Facing, Pull);
        vertex(Paint, Local.add(U).subtract(V), 1.0F, 1.0F, Red, Green, Blue, Facing, Pull);
        vertex(Paint, Local.add(U).add(V), 1.0F, 0.0F, Red, Green, Blue, Facing, Pull);
        vertex(Paint, Local.subtract(U).add(V), 0.0F, 0.0F, Red, Green, Blue, Facing, Pull);
    }

    private static void vertex(Painter Paint, Vec3 Local, float U, float V, float Red, float Green, float Blue, Vec3 Normal, double Pull) {
        Vec3 Point = Local;
        if (Paint.Stage() != Pass.Hand) {
            Point = MicroHidGlow.pulled(Local, Pull);
            if (Paint.Impact() != null && Paint.Surface() != null) {
                Point = MicroHidGlow.cleared(Point, Paint.Impact(), Paint.Surface());
            }
        }
        PoseStack.Pose Last = Paint.Last();
        Paint.Consumer().addVertex(Last, (float) Point.x, (float) Point.y, (float) Point.z).setColor(Red, Green, Blue, Paint.Alpha()).setUv(U, V)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(Last, (float) Normal.x, (float) Normal.y, (float) Normal.z);
    }

    @SubscribeEvent
    public static void shake(ViewportEvent.ComputeCameraAngles Event) {
        Minecraft Game = Minecraft.getInstance();
        LocalPlayer User = Game.player;
        if (User == null || !Game.options.getCameraType().isFirstPerson()) {
            return;
        }
        Phase Current = phaseOf(User);
        float Amount = Current == Phase.Fire ? ShakeFire : Current == Phase.Spin ? ShakeSpin * spinProgress(User, (float) Event.getPartialTick()) : 0.0F;
        if (Amount <= 0.0F) {
            return;
        }
        float Clock = Ticks + (float) Event.getPartialTick();
        Event.setYaw(Event.getYaw() + Amount * Mth.sin(Clock * 3.7F));
        Event.setPitch(Event.getPitch() + Amount * Mth.cos(Clock * 2.9F + 0.6F));
        Event.setRoll(Event.getRoll() + Amount * 0.6F * Mth.sin(Clock * 5.3F + 1.1F));
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Shooters.clear();
        LocalMuzzle = null;
    }
}
