package net.rai.scp_unleashed.scp001scarlet.king;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.discord.DiscordApocalypse;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.scp001scarlet.core.ScarletCourtHook;
import net.rai.scp_unleashed.scp001scarlet.core.ScarletState;
import org.joml.Vector3f;

import java.util.List;

public final class ScarletKingFall {
    public static final DustParticleOptions Crimson = new DustParticleOptions(new Vector3f(0.55f, 0.05f, 0.07f), 2.0f);
    public static final DustParticleOptions Ash = new DustParticleOptions(new Vector3f(0.2f, 0.18f, 0.18f), 1.6f);
    public static final DustParticleOptions Pale = new DustParticleOptions(new Vector3f(0.95f, 0.93f, 0.88f), 1.4f);
    public static final DustParticleOptions Iron = new DustParticleOptions(new Vector3f(0.45f, 0.44f, 0.46f), 1.2f);
    public static final Vec3 CrownKneel = ScarletKing.HeadKneel.add(0.0, 2.5, 0.0);
    public static final Vec3 HaloKneel = ScarletKing.HeadKneel.add(0.0, 6.0, 0.0);
    public static final int[][] Crumbles = {{110, 132}, {112, 128}, {106, 130}, {124, 138}, {126, 146}, {138, 150}, {148, 166}, {152, 167}, {156, 172}};

    private ScarletKingFall() {
    }

    public static void fall(ServerLevel World, ScarletKing King, DamageSource Source) {
        ScarletState State = ScarletState.get(World.getServer());
        State.phase(ScarletState.Phase.Freed, World.getGameTime());
        ScarletKingData Data = ScarletKingData.get(World.getServer());
        Data.king(null);
        Data.endRite();
        ScarletCourtHook Hook = ScarletKing.court();
        if (Hook != null) {
            Hook.end(World);
        }
        BlockPos Throne = throne(King);
        clearArena(World, Throne);
        DiscordApocalypse.scarletSlain(World.getServer(), Source.getEntity(), Throne);
    }

    public static void finish(ServerLevel World, ScarletKing King) {
        BlockPos Throne = throne(King);
        BlockPos Spot = remainsSpot(World, Throne);
        World.setBlock(Spot, ScarletKingRegistry.Remains.get().defaultBlockState(), Block.UPDATE_ALL);
        for (ItemStack Stack : loot()) {
            drop(World, Vec3.atBottomCenterOf(Spot).add(0.0, 1.0, 0.0), Stack);
        }
        World.sendParticles(ParticleTypes.END_ROD, Spot.getX() + 0.5, Spot.getY() + 1.5, Spot.getZ() + 0.5, 60, 1.5, 2.0, 1.5, 0.05);
        Component Line = Component.translatable("message.scp_unleashed.scarlet_king.freed").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC);
        for (ServerPlayer Listener : World.getServer().getPlayerList().getPlayers()) {
            Listener.sendSystemMessage(Line);
            Listener.level().playSound(null, Listener.getX(), Listener.getY(), Listener.getZ(), ScarletKingRegistry.ChoirBell.get(), SoundSource.AMBIENT, 1.0f, 0.8f);
        }
    }

    public static void dying(ServerLevel World, ScarletKing King, int Tick) {
        if (Tick == 1) {
            King.sound(World, ScarletKingRegistry.DeathSlack.get(), 6.0f, 0.85f);
        }
        if (Tick == 3) {
            King.sound(World, ScarletKingRegistry.Death.get(), 8.0f, 0.5f);
        }
        if (Tick <= ScarletKing.SlackTicks && Tick % 3 == 0) {
            for (int Side = -1; Side <= 1; Side += 2) {
                Vec3 Hand = King.local(new Vec3(ScarletKing.HandRest.x * Side, ScarletKing.HandRest.y - Tick * 0.2, ScarletKing.HandRest.z));
                burst(World, Iron, Hand, 6, 0.4, 1.6, 0.4, 0.02);
                burst(World, ParticleTypes.SMOKE, Hand, 3, 0.3, 1.0, 0.3, 0.01);
            }
        }
        if (Tick == 28) {
            King.sound(World, ScarletKingRegistry.Growl.get(), 6.0f, 0.55f);
        }
        if (Tick == ScarletKing.KneelTick) {
            King.sound(World, ScarletKingRegistry.DeathKnee.get(), 8.0f, 0.95f);
            impact(World, King, 5.5, 40);
        }
        if (Tick == ScarletKing.SecondKneelTick) {
            King.sound(World, ScarletKingRegistry.DeathKnee.get(), 9.0f, 0.8f);
            impact(World, King, 7.5, 64);
        }
        if (Tick == ScarletKing.DefianceTick) {
            King.sound(World, ScarletKingRegistry.Roar.get(), 6.0f, 0.42f);
        }
        if (Tick == ScarletKing.CrownCrackTick) {
            King.sound(World, ScarletKingRegistry.DeathCrack.get(), 8.0f, 0.8f);
            Vec3 Crown = King.local(CrownKneel);
            burst(World, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BONE_BLOCK.defaultBlockState()), Crown, 30, 1.6, 1.2, 1.6, 0.15);
            burst(World, Pale, Crown, 20, 1.2, 1.0, 1.2, 0.05);
        }
        if (Tick == ScarletKing.ShatterTick) {
            King.sound(World, ScarletKingRegistry.DeathCrack.get(), 5.0f, 1.3f);
            Vec3 Halo = King.local(HaloKneel);
            burst(World, ParticleTypes.END_ROD, Halo, 40, 1.4, 1.4, 1.4, 0.12);
            burst(World, Pale, Halo, 30, 1.6, 1.2, 1.6, 0.06);
        }
        if (Tick == ScarletKing.CrumbleTick) {
            King.sound(World, ScarletKingRegistry.DeathAsh.get(), 8.0f, 0.9f);
        }
        if (Tick == ScarletKing.CrumbleTick + 30) {
            King.sound(World, ScarletKingRegistry.DeathAsh.get(), 7.0f, 0.8f);
        }
        if (Tick >= ScarletKing.CrumbleTick - 4 && Tick < ScarletKing.LootTick && Tick % 2 == 0) {
            crumble(World, King, Tick);
        }
        if (Tick == ScarletKing.CollapseTick) {
            King.sound(World, ScarletKingRegistry.DeathCollapse.get(), 10.0f, 0.85f);
            Vec3 Heap = King.local(new Vec3(0.0, 1.5, 1.5));
            burst(World, ParticleTypes.LARGE_SMOKE, Heap, 60, 3.0, 2.0, 3.0, 0.06);
            burst(World, Crimson, Heap, 80, 3.5, 2.5, 3.5, 0.08);
            burst(World, ParticleTypes.LAVA, Heap, 20, 2.5, 1.0, 2.5, 0.0);
            burst(World, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, Heap, 10, 1.5, 0.5, 1.5, 0.02);
            impact(World, King, 9.0, 80);
        }
        if (Tick == ScarletKing.CollapseTick + 6) {
            King.sound(World, ScarletKingRegistry.End.get(), 8.0f, 0.7f);
        }
        if (Tick > ScarletKing.CollapseTick && Tick % 3 == 0) {
            Vec3 Heap = King.local(new Vec3(0.0, 0.6, 1.0));
            burst(World, ParticleTypes.SMALL_FLAME, Heap, 3, 2.0, 0.4, 2.0, 0.01);
            burst(World, ParticleTypes.SMOKE, Heap, 4, 2.0, 0.6, 2.0, 0.02);
        }
    }

    private static void crumble(ServerLevel World, ScarletKing King, int Tick) {
        for (int Region = 0; Region < Crumbles.length; Region++) {
            if (Tick < Crumbles[Region][0] || Tick > Crumbles[Region][1]) {
                continue;
            }
            float Share = (Tick - Crumbles[Region][0]) / (float) Math.max(1, Crumbles[Region][1] - Crumbles[Region][0]);
            for (Vec3 Offset : region(Region, Share, World.random.nextDouble())) {
                Vec3 At = King.local(Offset);
                burst(World, Crimson, At, 5, 0.8, 0.8, 0.8, 0.03);
                burst(World, Ash, At, 4, 0.9, 0.9, 0.9, 0.02);
                burst(World, ParticleTypes.SMALL_FLAME, At, 1, 0.6, 0.6, 0.6, 0.02);
                if (Tick % 4 == 0) {
                    burst(World, ParticleTypes.LARGE_SMOKE, At, 2, 0.7, 0.7, 0.7, 0.03);
                    burst(World, ParticleTypes.LAVA, At, 1, 0.5, 0.5, 0.5, 0.0);
                }
            }
        }
    }

    private static List<Vec3> region(int Region, float Share, double Roll) {
        double Angle = Roll * Math.PI * 2.0;
        return switch (Region) {
            case 0 -> List.of(new Vec3(Math.cos(Angle) * (9.0 - Share * 3.0), 0.4 + Share * 1.5, Math.sin(Angle) * (9.0 - Share * 3.0)),
                    new Vec3(Math.cos(Angle + Math.PI) * (8.0 - Share * 2.0), 0.6, Math.sin(Angle + Math.PI) * (8.0 - Share * 2.0)));
            case 1 -> List.of(new Vec3(ScarletKing.HandKneel.x, ScarletKing.HandKneel.y, ScarletKing.HandKneel.z),
                    new Vec3(-ScarletKing.HandKneel.x, ScarletKing.HandKneel.y, ScarletKing.HandKneel.z));
            case 2 -> List.of(CrownKneel.add((Share - 0.5) * 6.0, -Share * 2.0, 0.0));
            case 3 -> List.of(new Vec3(6.0, 2.5, -0.5), new Vec3(-6.0, 2.5, -0.5));
            case 4 -> List.of(new Vec3(5.5 - Share * 2.0, 4.0 + Share, 5.0), new Vec3(-5.5 + Share * 2.0, 4.0 + Share, 5.0));
            case 5 -> List.of(ScarletKing.HeadKneel);
            case 6 -> List.of(ScarletKing.ChestKneel.add(0.0, -Share * 2.0, 0.0));
            case 7 -> List.of(new Vec3(0.0, 3.5 - Share * 2.0, 2.5));
            default -> List.of(new Vec3(Math.cos(Angle) * 3.0, 1.0, Math.sin(Angle) * 3.0));
        };
    }

    private static void impact(ServerLevel World, ScarletKing King, double Radius, int Count) {
        BlockPos Below = King.blockPosition().below();
        BlockState Floor = World.getBlockState(Below);
        if (Floor.isAir()) {
            Floor = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        }
        ParticleOptions Dust = new BlockParticleOption(ParticleTypes.BLOCK, Floor);
        for (int Index = 0; Index < Count; Index++) {
            double Angle = Index * Math.PI * 2.0 / Count;
            double Reach = Radius * (0.6 + 0.4 * World.random.nextDouble());
            World.sendParticles(Dust, King.getX() + Math.cos(Angle) * Reach, King.getY() + 0.2, King.getZ() + Math.sin(Angle) * Reach, 3, 0.3, 0.1, 0.3, 0.12);
        }
        World.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, King.getX(), King.getY() + 0.3, King.getZ(), 8, Radius * 0.5, 0.1, Radius * 0.5, 0.01);
    }

    private static void burst(ServerLevel World, ParticleOptions Kind, Vec3 At, int Count, double Dx, double Dy, double Dz, double Speed) {
        World.sendParticles(Kind, At.x, At.y, At.z, Count, Dx, Dy, Dz, Speed);
    }

    public static BlockPos throne(ScarletKing King) {
        ScarletArena Where = King.arena();
        return Where == null ? King.blockPosition() : Where.Throne();
    }

    public static List<ItemStack> loot() {
        return List.of(new ItemStack(ScarletKingRegistry.Helmet.get()), new ItemStack(ScarletKingRegistry.Chestplate.get()), new ItemStack(ScarletKingRegistry.Leggings.get()),
                new ItemStack(ScarletKingRegistry.Boots.get()), GunItem.loaded(GunKind.ScarletRifle),
                new ItemStack(ScarletKingRegistry.PodItem.get()));
    }

    private static void drop(ServerLevel World, Vec3 At, ItemStack Stack) {
        ItemEntity Item = new ItemEntity(World, At.x, At.y, At.z, Stack);
        Item.setDeltaMovement((World.random.nextDouble() - 0.5) * 0.25, 0.3, (World.random.nextDouble() - 0.5) * 0.25);
        Item.setUnlimitedLifetime();
        Item.setInvulnerable(true);
        Item.setGlowingTag(true);
        Item.setPickUpDelay(20);
        World.addFreshEntity(Item);
    }

    public static BlockPos remainsSpot(ServerLevel World, BlockPos Throne) {
        for (int Lift = -2; Lift <= 6; Lift++) {
            BlockPos At = Throne.above(Lift);
            if (World.getBlockState(At).canBeReplaced() && !World.getBlockState(At.below()).getCollisionShape(World, At.below()).isEmpty()) {
                return At;
            }
        }
        return Throne.above();
    }

    public static void clearArena(ServerLevel World, BlockPos Throne) {
        AABB Area = new AABB(Throne).inflate(ScarletArena.FightRadius + 16.0, 32.0, ScarletArena.FightRadius + 16.0);
        for (Entity Each : World.getEntitiesOfClass(Entity.class, Area, Each -> Each instanceof ScarletThorn || Each.getTags().contains(ScarletKing.ShadeTag))) {
            if (!(Each instanceof ScarletThorn)) {
                World.sendParticles(ParticleTypes.LARGE_SMOKE, Each.getX(), Each.getY() + 1.0, Each.getZ(), 10, 0.3, 0.6, 0.3, 0.02);
            }
            Each.discard();
        }
    }
}
