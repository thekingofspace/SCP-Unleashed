package net.rai.scp_unleashed.guns.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.guns.GunSoundMixer;
import net.rai.scp_unleashed.guns.GunSounds;
import net.rai.scp_unleashed.guns.GunVoiceBudget;
import net.rai.scp_unleashed.guns.GunsRegistry;
import net.rai.scp_unleashed.guns.npc.NpcGunner;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class GunSoundGameTests {
    public static final String Arena = GunPerfGameTests.Arena;
    public static final float Sturdy = 400.0F;

    private GunSoundGameTests() {
    }

    private static int floor(GameTestHelper Helper) {
        for (int Y = 0; Y < 5; Y++) {
            if (Helper.getBlockState(new BlockPos(1, Y, 1)).is(Blocks.SMOOTH_STONE) && Helper.getBlockState(new BlockPos(1, Y + 1, 1)).isAir()) {
                return Y + 1;
            }
        }
        return 1;
    }

    private static GunPerfListener ear(GameTestHelper Helper, String Name, Vec3 Spot) {
        GunPerfListener Made = new GunPerfListener(Helper.getLevel(), Name, Helper.absoluteVec(Spot));
        Made.enlist(true);
        return Made;
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "gunsound_budget")
    public static void voiceBudgetStealsQuietest(GameTestHelper Helper) {
        GunVoiceBudget<String> Budget = new GunVoiceBudget<>(3);
        List<String> Stolen = new ArrayList<>();
        Helper.assertTrue(Budget.offer("a", 0.5F, 0L, 20, Stolen), "first voice refused");
        Helper.assertTrue(Budget.offer("b", 0.4F, 0L, 20, Stolen), "second voice refused");
        Helper.assertTrue(Budget.offer("c", 0.3F, 0L, 20, Stolen), "third voice refused");
        Helper.assertTrue(Stolen.isEmpty(), "a voice was stolen below the limit");
        Helper.assertFalse(Budget.offer("d", 0.1F, 1L, 20, Stolen), "a quieter voice got in over the limit");
        Helper.assertTrue(Stolen.isEmpty(), "a refused voice stole a slot");
        Helper.assertTrue(Budget.offer("e", 0.6F, 1L, 20, Stolen), "a louder voice was refused");
        Helper.assertTrue(Stolen.size() == 1 && "c".equals(Stolen.get(0)), "the quietest voice was not the one stolen: " + Stolen);
        Helper.assertTrue(Budget.size(1L) == 3, "the budget overflowed");
        Helper.assertTrue(Budget.size(21L) == 0, "voices never finished");
        Stolen.clear();
        Helper.assertTrue(Budget.offer("f", 0.01F, 22L, 5, Stolen), "an empty budget refused a voice");
        Helper.assertTrue(GunVoiceBudget.managed(GunsRegistry.ShotEcho.getId()), "shot layers are not managed");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "gunsound_lone")
    public static void loneShotKeepsEveryLayer(GameTestHelper Helper) {
        int F = floor(Helper);
        Husk Rifleman = Helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(4.5, F, 15.5));
        Rifleman.setItemSlot(EquipmentSlot.MAINHAND, GunItem.loaded(GunKind.Pistol));
        Husk Target = Helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(12.5, F, 15.5));
        Target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(Sturdy);
        Target.setHealth(Sturdy);
        GunPerfListener Near = ear(Helper, "gunsound_near", new Vec3(4.5, F, 19.5));
        GunPerfListener Far = ear(Helper, "gunsound_far", new Vec3(-95.5, F, 15.5));
        int Layers = GunSounds.shot(GunKind.Pistol).size();
        Helper.startSequence()
                .thenExecute(() -> Helper.assertTrue(NpcGunner.fireAt(Rifleman, Rifleman.getMainHandItem(), Target, null, 1.0F), "the pistol did not fire"))
                .thenIdle(5)
                .thenExecute(() -> {
                    int Close = Near.managed();
                    int Distant = Far.managed();
                    List<String> Names = new ArrayList<>();
                    for (ClientboundSoundPacket Played : Far.heard()) {
                        Names.add(Played.getSound().value().getLocation().getPath());
                    }
                    Near.enlist(false);
                    Far.enlist(false);
                    Rifleman.discard();
                    Target.discard();
                    Helper.assertTrue(Close == Layers, "a nearby listener heard " + Close + " of " + Layers + " layers");
                    Helper.assertTrue(Distant == 1 && Names.contains(GunsRegistry.ShotEcho.getId().getPath()), "a distant listener heard " + Names);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "gunsound_merge")
    public static void crowdedLayersKeepLoudest(GameTestHelper Helper) {
        int F = floor(Helper);
        ServerLevel World = Helper.getLevel();
        GunPerfListener Near = ear(Helper, "gunsound_crowd", new Vec3(2.5, F, 2.5));
        int Count = GunSoundMixer.MaxOverlap + 6;
        GunSoundMixer.begin(World);
        for (int Index = 0; Index < Count; Index++) {
            Vec3 Spot = Helper.absoluteVec(new Vec3(2.5 + Index, F + 1.0, 2.5));
            GunSoundMixer.emit(World, null, Spot.x, Spot.y, Spot.z, GunsRegistry.ShotPunch.get(), SoundSource.PLAYERS, 0.5F, 1.0F);
        }
        GunSoundMixer.flush(World);
        int Heard = Near.managed();
        double Farthest = 0.0;
        Vec3 Ear = Helper.absoluteVec(new Vec3(2.5, F, 2.5));
        for (ClientboundSoundPacket Played : Near.heard()) {
            Farthest = Math.max(Farthest, Math.abs(Played.getX() - Ear.x));
        }
        Near.enlist(false);
        Helper.assertTrue(Heard == GunSoundMixer.MaxOverlap, "identical layers were not merged: " + Heard + " of " + Count);
        Helper.assertTrue(Farthest < GunSoundMixer.MaxOverlap, "a farther layer won over a nearer one: " + Farthest);
        Helper.succeed();
    }
}
