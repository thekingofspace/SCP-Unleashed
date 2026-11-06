package net.rai.scp_unleashed.scp5001.xk;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.rai.scp_unleashed.scp610.Scp610Creature;
import net.rai.scp_unleashed.scp610.Scp610Form;
import net.rai.scp_unleashed.scp610.Scp610Hosts;
import net.rai.scp_unleashed.scp610.Scp610Registry;
import net.rai.scp_unleashed.scp610.Scp610Sounds;
import net.rai.scp_unleashed.scp610.Scp610Spawning;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class XkRising {
    public static final int Delay = 50;
    public static final int GlobalCap = 96;
    private static final List<Pending> Queue = new ArrayList<>();

    public static final class Pending {
        private final ResourceKey<Level> Dimension;
        private final Vec3 Where;
        private final float Yaw;
        private final Scp610Form Form;
        private final long Chunk;
        private int Age;

        private Pending(ResourceKey<Level> Dimension, Vec3 Where, float Yaw, Scp610Form Form, long Chunk) {
            this.Dimension = Dimension;
            this.Where = Where;
            this.Yaw = Yaw;
            this.Form = Form;
            this.Chunk = Chunk;
        }

        public Scp610Form form() {
            return Form;
        }
    }

    private XkRising() {
    }

    public static boolean eligible(LivingEntity Victim) {
        if (!(Victim instanceof Mob) || Victim instanceof Player || Victim instanceof Scp610Creature || Victim instanceof Yaldabaoth) {
            return false;
        }
        return !Victim.getType().is(Tags.EntityTypes.BOSSES) && !Victim.getType().is(Scp5001XkRegistry.Immune);
    }

    public static int pending() {
        return Queue.size();
    }

    public static List<Scp610Form> pendingForms() {
        List<Scp610Form> Forms = new ArrayList<>();
        for (Pending Each : Queue) {
            Forms.add(Each.Form);
        }
        return Forms;
    }

    public static void clear() {
        Queue.clear();
    }

    public static int inChunk(ServerLevel World, ChunkPos Chunk) {
        AABB Box = new AABB(Chunk.getMinBlockX(), World.getMinBuildHeight(), Chunk.getMinBlockZ(), Chunk.getMaxBlockX() + 1, World.getMaxBuildHeight(), Chunk.getMaxBlockZ() + 1);
        int Count = World.getEntitiesOfClass(Scp610Creature.class, Box, Found -> Found.isAlive()).size();
        long Key = Chunk.toLong();
        for (Pending Each : Queue) {
            if (Each.Chunk == Key && Each.Dimension == World.dimension()) {
                Count++;
            }
        }
        return Count;
    }

    public static boolean died(LivingEntity Victim) {
        if (!(Victim.level() instanceof ServerLevel World)) {
            return false;
        }
        MinecraftServer Server = World.getServer();
        if (!Scp5001Xk.rising(Server) || !eligible(Victim) || Queue.size() >= GlobalCap) {
            return false;
        }
        if (World.random.nextDouble() >= Scp5001XkConfig.riseChance()) {
            return false;
        }
        ChunkPos Chunk = new ChunkPos(Victim.blockPosition());
        if (inChunk(World, Chunk) >= Scp5001XkConfig.chunkCap()) {
            return false;
        }
        Scp610Form Form = Scp610Hosts.formFor(Victim);
        Queue.add(new Pending(World.dimension(), Victim.position(), Victim.getYRot(), Form, Chunk.toLong()));
        World.playSound(null, Victim.getX(), Victim.getY(), Victim.getZ(), Scp610Sounds.Stage.get(), SoundSource.HOSTILE, 1.0f, 0.6f);
        return true;
    }

    public static void tick(MinecraftServer Server) {
        if (Queue.isEmpty()) {
            return;
        }
        Iterator<Pending> Each = Queue.iterator();
        while (Each.hasNext()) {
            Pending Item = Each.next();
            ServerLevel World = Server.getLevel(Item.Dimension);
            if (World == null) {
                Each.remove();
                continue;
            }
            Item.Age++;
            BlockParticleOption Flesh = new BlockParticleOption(ParticleTypes.BLOCK, Scp610Registry.Flesh.get().defaultBlockState());
            if (Item.Age % 6 == 0) {
                double Swell = 0.2 + Item.Age / (double) Delay * 0.6;
                World.sendParticles(Flesh, Item.Where.x, Item.Where.y + 0.3, Item.Where.z, 4 + Item.Age / 8, Swell, 0.2 + Swell * 0.5, Swell, 0.05);
            }
            if (Item.Age == Delay / 2) {
                World.playSound(null, Item.Where.x, Item.Where.y, Item.Where.z, Scp5001XkRegistry.Rise.get(), SoundSource.HOSTILE, 1.0f, 0.8f);
            }
            if (Item.Age >= Delay) {
                Each.remove();
                if (!World.isLoaded(BlockPos.containing(Item.Where))) {
                    continue;
                }
                World.sendParticles(Flesh, Item.Where.x, Item.Where.y + 0.6, Item.Where.z, 40, 0.6, 0.8, 0.6, 0.25);
                World.sendParticles(ParticleTypes.DAMAGE_INDICATOR, Item.Where.x, Item.Where.y + 0.8, Item.Where.z, 6, 0.4, 0.4, 0.4, 0.1);
                World.playSound(null, Item.Where.x, Item.Where.y, Item.Where.z, Scp610Sounds.Burst.get(), SoundSource.HOSTILE, 1.2f, 0.8f);
                World.playSound(null, Item.Where.x, Item.Where.y, Item.Where.z, Scp610Sounds.ConvertTear.get(), SoundSource.HOSTILE, 1.0f, 0.9f);
                Scp610Spawning.raise(World, Item.Form, Item.Where, Item.Yaw, true);
            }
        }
    }
}
