package net.rai.scp_unleashed.rift.client;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.rift.RiftEntity;
import net.rai.scp_unleashed.rift.RiftParticleOptions;
import net.rai.scp_unleashed.rift.RiftRegistry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class RiftAmbience {
    public static final double ParticleReach = 40.0;
    public static final double HumReach = 26.0;
    public static final int HumRetry = 40;

    private static final Map<Integer, Hum> Hums = new HashMap<>();
    private static ClientLevel LastWorld;
    private static long Ticks;

    private RiftAmbience() {
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Minecraft Client = Minecraft.getInstance();
        ClientLevel World = Client.level;
        if (World != LastWorld) {
            Hums.clear();
            LastWorld = World;
        }
        if (World == null || Client.isPaused()) {
            return;
        }
        Ticks++;
        Hums.values().removeIf(Playing -> Playing.Sound().isStopped() && Ticks - Playing.Since() > 2);
        Vec3 Eye = Client.gameRenderer.getMainCamera().getPosition();
        for (RiftEntity Rift : World.getEntitiesOfClass(RiftEntity.class, new AABB(Eye, Eye).inflate(ParticleReach), Candidate -> !Candidate.isRemoved())) {
            Vec3 Center = Rift.getCenter(1.0f);
            double Distance = Center.distanceTo(Eye);
            if (Distance < ParticleReach) {
                emit(World, Rift, Center, Eye);
            }
            if (Distance < HumReach) {
                hum(Client, Rift);
            }
        }
    }

    private static void hum(Minecraft Client, RiftEntity Rift) {
        Hum Existing = Hums.get(Rift.getId());
        if (Existing != null && Existing.Sound().getRift() == Rift && !Existing.Sound().isStopped()) {
            if (Client.getSoundManager().isActive(Existing.Sound()) || Ticks - Existing.Since() < HumRetry) {
                return;
            }
        }
        RiftHumSound Sound = new RiftHumSound(Rift);
        Hums.put(Rift.getId(), new Hum(Sound, Ticks));
        Client.getSoundManager().play(Sound);
    }

    private static void emit(ClientLevel World, RiftEntity Rift, Vec3 Center, Vec3 Eye) {
        RandomSource Random = World.random;
        float Open = Rift.getOpenness(1.0f);
        if (Open <= 0.05f) {
            return;
        }
        int Tier = Rift.getRarity().getTier();
        int Colour = Rift.getRiftColour();
        float Half = Rift.getRiftHeight() * 0.5f;
        float Extraction = Rift.getShownExtraction(1.0f);
        float Collapse = Rift.getCollapse(1.0f);
        float Motes = (0.3f + 0.1f * Tier) * (1.0f + 3.0f * Extraction) * Open * (1.0f - Collapse);
        for (int Count = roll(Random, Motes); Count > 0; Count--) {
            double Theta = Random.nextDouble() * Math.PI * 2.0;
            double Lift = Random.nextDouble() * 2.0 - 1.0;
            double Ring = Math.sqrt(1.0 - Lift * Lift);
            double Radius = Half * (1.5 + Random.nextDouble() * 1.6);
            double X = Center.x + Math.cos(Theta) * Ring * Radius;
            double Y = Center.y + Lift * Radius * 0.8;
            double Z = Center.z + Math.sin(Theta) * Ring * Radius;
            double Speed = 0.012 + 0.02 * Extraction;
            World.addParticle(new RiftParticleOptions(RiftRegistry.RiftMote.get(), Colour, Rift.getId(), 1.0f), X, Y, Z,
                    -Math.sin(Theta) * Speed, (Random.nextDouble() - 0.5) * 0.01, Math.cos(Theta) * Speed);
        }
        float Sparks = (0.06f + 0.05f * Tier + 1.4f * Extraction * Extraction + 2.5f * Collapse) * Open;
        Vec3 Toward = new Vec3(Center.x - Eye.x, 0.0, Center.z - Eye.z);
        Vec3 Depth = Toward.lengthSqr() > 0.0001 ? Toward.normalize() : new Vec3(0.0, 0.0, 1.0);
        Vec3 Across = new Vec3(-Depth.z, 0.0, Depth.x);
        double Width = (0.33 + 0.025 * Tier) * Math.pow(Open, 0.65) * (1.0 - Mth.clamp((Collapse - 0.25) * 2.0, 0.0, 1.0));
        double Bulge = 0.5 * (Width + 1.0 / Math.max(Width, 0.01));
        double Offset = Bulge - Width;
        for (int Count = roll(Random, Sparks); Count > 0; Count--) {
            double Along = (Random.nextDouble() * 2.0 - 1.0) * 0.92;
            double Side = Random.nextBoolean() ? 1.0 : -1.0;
            double Spread = Math.max(0.0, Math.sqrt(Math.max(0.0, Bulge * Bulge - Along * Along)) - Offset) * Side;
            Vec3 Point = Center.add(Across.scale(Spread * Half)).add(0.0, Along * Half, 0.0).subtract(Depth.scale(0.06));
            Vec3 Outward = Across.scale(Side * (0.05 + Random.nextDouble() * 0.08)).add(0.0, Along * 0.05 + (Random.nextDouble() - 0.5) * 0.04, 0.0);
            World.addParticle(new RiftParticleOptions(RiftRegistry.RiftSpark.get(), Colour, -1, 0.8f + 0.1f * Tier), Point.x, Point.y, Point.z,
                    Outward.x, Outward.y, Outward.z);
        }
    }

    private static int roll(RandomSource Random, float Rate) {
        int Whole = Mth.floor(Rate);
        return Whole + (Random.nextFloat() < Rate - Whole ? 1 : 0);
    }

    @SubscribeEvent
    public static void entityLeave(EntityLeaveLevelEvent Event) {
        if (!Event.getLevel().isClientSide() || !(Event.getEntity() instanceof RiftEntity Rift) || !Rift.isCollapsing()) {
            return;
        }
        burst(Rift);
        RiftDistortion.flash(Rift);
    }

    private static void burst(RiftEntity Rift) {
        RandomSource Random = Rift.level().random;
        Vec3 Center = Rift.getCenter(1.0f);
        int Colour = Rift.getRiftColour();
        float Half = Rift.getRiftHeight() * 0.5f;
        int Tier = Rift.getRarity().getTier();
        Rift.level().addParticle(new RiftParticleOptions(RiftRegistry.RiftShockwave.get(), Colour, -1, Half), Center.x, Center.y, Center.z, 0.0, 0.0, 0.0);
        for (int Count = 0; Count < 26 + 8 * Tier; Count++) {
            Vec3 Heading = new Vec3(Random.nextGaussian(), Random.nextGaussian() * 0.8, Random.nextGaussian()).normalize();
            double Speed = 0.25 + Random.nextDouble() * 0.45;
            Rift.level().addParticle(new RiftParticleOptions(RiftRegistry.RiftSpark.get(), Colour, -1, 1.1f), Center.x, Center.y, Center.z,
                    Heading.x * Speed, Heading.y * Speed, Heading.z * Speed);
        }
        for (int Count = 0; Count < 14 + 4 * Tier; Count++) {
            Vec3 Heading = new Vec3(Random.nextGaussian(), Random.nextGaussian() * 0.8, Random.nextGaussian()).normalize();
            double Speed = 0.08 + Random.nextDouble() * 0.18;
            Rift.level().addParticle(new RiftParticleOptions(RiftRegistry.RiftMote.get(), Colour, -1, 1.3f), Center.x, Center.y, Center.z,
                    Heading.x * Speed, Heading.y * Speed, Heading.z * Speed);
        }
    }

    private record Hum(RiftHumSound Sound, long Since) {
    }
}
