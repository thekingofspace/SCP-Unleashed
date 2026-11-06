package net.rai.scp_unleashed.scp610.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.rai.scp_unleashed.faction.FactionControl;
import net.rai.scp_unleashed.gear.GearRegistry;
import net.rai.scp_unleashed.labware.LabContact;
import net.rai.scp_unleashed.labware.LabContactEvents;
import net.rai.scp_unleashed.scp610.Scp610Infection;
import net.rai.scp_unleashed.scp610.Scp610Registry;
import net.rai.scp_unleashed.scp610.Scp610Config;
import net.rai.scp_unleashed.scp610.Scp610Creature;
import net.rai.scp_unleashed.scp610.Scp610Faction;
import net.rai.scp_unleashed.scp610.Scp610Merging;
import net.rai.scp_unleashed.scp610.Scp610Rules;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Scp610TestKit {
    public static final String Arena = "scp610_arena";
    public static final int Ground = 29;
    public static final int Wide = 32;

    private Scp610TestKit() {
    }

    private static final class Silent extends ServerGamePacketListenerImpl {
        private Silent(MinecraftServer Server, ServerPlayer User) {
            super(Server, wired(), User, CommonListenerCookie.createInitial(User.getGameProfile(), false));
        }

        private static Connection wired() {
            Connection Link = new Connection(PacketFlow.SERVERBOUND);
            new EmbeddedChannel(Link);
            return Link;
        }

        @Override
        public void send(Packet<?> Sent) {
        }

        @Override
        public void send(Packet<?> Sent, @Nullable PacketSendListener Listener) {
        }

        @Override
        public void tick() {
        }

        @Override
        public void disconnect(Component Reason) {
        }
    }

    private static void enrol(MinecraftServer Server, ServerPlayer User, boolean Joining) {
        List<ServerPlayer> Players = ObfuscationReflectionHelper.getPrivateValue(PlayerList.class, Server.getPlayerList(), "players");
        Map<UUID, ServerPlayer> ByUuid = ObfuscationReflectionHelper.getPrivateValue(PlayerList.class, Server.getPlayerList(), "playersByUUID");
        if (Joining) {
            Players.add(User);
            ByUuid.put(User.getUUID(), User);
        } else {
            Players.remove(User);
            ByUuid.remove(User.getUUID());
        }
    }

    public static ServerPlayer survivor(GameTestHelper Helper, double X, double Y, double Z) {
        ServerLevel World = Helper.getLevel();
        MinecraftServer Server = World.getServer();
        ServerPlayer User = new ServerPlayer(Server, World, new GameProfile(UUID.randomUUID(), "scp610_test"), ClientInformation.createDefault()) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return false;
            }
        };
        User.connection = new Silent(Server, User);
        ObfuscationReflectionHelper.setPrivateValue(ServerPlayer.class, User, 0, "spawnInvulnerableTime");
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, Y, Z));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        enrol(Server, User, true);
        return User;
    }

    public static void dismiss(ServerPlayer User) {
        enrol(User.server, User, false);
        if (!User.isRemoved()) {
            User.serverLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
        }
    }

    public static void suit(LivingEntity Wearer) {
        Wearer.setItemSlot(EquipmentSlot.HEAD, new ItemStack(GearRegistry.HazmatHood.get()));
        Wearer.setItemSlot(EquipmentSlot.CHEST, new ItemStack(GearRegistry.HazmatSuit.get()));
        Wearer.setItemSlot(EquipmentSlot.LEGS, new ItemStack(GearRegistry.HazmatTrousers.get()));
        Wearer.setItemSlot(EquipmentSlot.FEET, new ItemStack(GearRegistry.HazmatBoots.get()));
    }

    public static void mask(LivingEntity Wearer) {
        Wearer.setItemSlot(EquipmentSlot.HEAD, new ItemStack(GearRegistry.GasMask.get()));
    }

    public static void fresh(GameTestHelper Helper) {
        MinecraftServer Server = Helper.getLevel().getServer();
        FactionControl.reset(Server, Scp610Faction.Instance);
        Scp610Merging.clear();
        Scp610Config.overrideForTests(-1, -1, -1, -1);
        Scp610Config.overrideDepthForTests(-1);
        Scp610Rules.setSpread(Server, true);
        Scp610Rules.setUnlimited(Server, false);
    }

    public static void restore(GameTestHelper Helper) {
        Scp610Config.overrideForTests(-1, -1, -1, -1);
        Scp610Config.overrideDepthForTests(-1);
        Scp610Rules.setSpread(Helper.getLevel().getServer(), true);
        Scp610Rules.setUnlimited(Helper.getLevel().getServer(), false);
        clear(Helper);
    }

    public static void clear(GameTestHelper Helper) {
        AABB Area = Helper.getBounds().inflate(24.0);
        for (Scp610Creature Found : Helper.getLevel().getEntitiesOfClass(Scp610Creature.class, Area)) {
            Found.discard();
        }
    }

    public static BlockPos ground(GameTestHelper Helper, int X, int Z) {
        return Helper.absolutePos(new BlockPos(X, Ground, Z));
    }

    public static void touch(LivingEntity Target, LivingEntity Carrier, LabContact Route, int Times) {
        for (int Count = 0; Count < Times && !Scp610Infection.isInfected(Target); Count++) {
            LabContactEvents.transmit(Scp610Registry.organism(), Carrier, Target, Route);
        }
    }

    public static void hit(LivingEntity Target, LivingEntity Attacker, int Times) {
        for (int Count = 0; Count < Times && Target.isAlive(); Count++) {
            Target.invulnerableTime = 0;
            Target.hurt(Attacker.damageSources().mobAttack(Attacker), 1.0f);
            Target.setHealth(Target.getMaxHealth());
        }
    }
}
