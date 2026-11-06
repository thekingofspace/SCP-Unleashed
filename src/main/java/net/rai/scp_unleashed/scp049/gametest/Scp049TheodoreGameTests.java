package net.rai.scp_unleashed.scp049.gametest;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassCrime;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.dclass.DClassTrait;
import net.rai.scp_unleashed.dclass.theodore.Theodore;
import net.rai.scp_unleashed.dclass.theodore.TheodoreRegistry;
import net.rai.scp_unleashed.scp049.Scp049Cured;
import net.rai.scp_unleashed.scp049.Scp049Discovery;
import net.rai.scp_unleashed.scp049.Scp049Doctor;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp049TheodoreGameTests {
    public static final String Arena = Scp049AppleGameTests.Arena;
    public static final int Floor = Scp049AppleGameTests.Floor;
    public static final double Search = 48.0;
    private static final Logger Log = LogUtils.getLogger();

    private Scp049TheodoreGameTests() {
    }

    private static Scp049Doctor doctor(GameTestHelper Helper, int X, int Z, double FaceX, double FaceZ) {
        ServerLevel World = Helper.getLevel();
        BlockPos Tray = Helper.absolutePos(new BlockPos(X, Floor, Z));
        Scp049Doctor Doctor = Scp049Discovery.stage(World, Tray, Helper.absoluteVec(new Vec3(FaceX, Floor, FaceZ)));
        if (Doctor == null) {
            throw new IllegalStateException("SCP-049 could not be staged for the Theodore test at " + Tray);
        }
        return Doctor;
    }

    private static Theodore theodore(GameTestHelper Helper, int X, int Z) {
        Theodore Made = TheodoreRegistry.TheodoreType.get().spawn(Helper.getLevel(), Helper.absolutePos(new BlockPos(X, Floor, Z)), MobSpawnType.SPAWN_EGG);
        if (Made == null) {
            throw new IllegalStateException("Theodore could not be spawned");
        }
        return Made;
    }

    private static <T extends LivingEntity> List<T> around(GameTestHelper Helper, Class<T> Kind) {
        Vec3 Centre = Helper.absoluteVec(new Vec3(24.0, Floor, 24.0));
        return Helper.getLevel().getEntitiesOfClass(Kind, new AABB(Centre, Centre).inflate(Search), LivingEntity::isAlive);
    }

    private static void tidy(GameTestHelper Helper) {
        Helper.killAllEntitiesOfClass(Scp049Doctor.class);
        Helper.killAllEntitiesOfClass(Scp049Cured.class);
        Helper.killAllEntitiesOfClass(Theodore.class);
        Helper.killAllEntitiesOfClass(ItemEntity.class);
    }

    private static String state(Scp049Doctor Doctor) {
        return " [routine " + Doctor.routine() + ", action " + Doctor.action() + ", held " + Doctor.heldId() + ", quarry " + Doctor.quarryId() + ", at " + Doctor.blockPosition() + "]";
    }

    private static void mimicProbe(GameTestHelper Helper, Scp049Cured Follower) {
        Optional<EntityType<?>> Kind = EntityType.byString(Follower.mimicType());
        Helper.assertTrue(Kind.isPresent(), "the cured look names no known type: " + Follower.mimicType());
        Entity Made = Kind.get().create(Helper.getLevel());
        Helper.assertTrue(Made instanceof LivingEntity, "the cured look could not be rebuilt");
        LivingEntity Body = (LivingEntity) Made;
        CompoundTag Data = Follower.mimicTag().copy();
        try {
            Body.load(Data);
            Body.setSilent(true);
            Body.noPhysics = true;
            Body.setHealth(Body.getMaxHealth());
            for (int Index = 0; Index < 40; Index++) {
                Body.setPos(Follower.getX(), Follower.getY(), Follower.getZ());
                Body.tickCount = Follower.tickCount;
                Body.tick();
            }
            Helper.assertTrue(!Body.isRemoved(), "the rebuilt look of " + Follower.mimicType() + " removed itself");
        } catch (Throwable Problem) {
            throw new IllegalStateException("the cured look could not be worn: " + Problem, Problem);
        }
    }

    private static void cureFlow(GameTestHelper Helper, Scp049Doctor Doctor, LivingEntity Patient, String Name) {
        UUID Id = Patient.getUUID();
        String Type = EntityType.getKey(Patient.getType()).toString();
        boolean Uniformed = Patient instanceof Theodore;
        Scp049Cured[] Risen = new Scp049Cured[1];
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Id.equals(Doctor.heldId()), "SCP-049 never took hold of " + Name + state(Doctor)))
                .thenWaitUntil(() -> Helper.assertTrue(!Patient.isAlive(), "SCP-049 never finished operating on " + Name + state(Doctor)))
                .thenWaitUntil(() -> {
                    List<Scp049Cured> Found = around(Helper, Scp049Cured.class);
                    Helper.assertTrue(!Found.isEmpty(), "nothing rose after SCP-049 operated on " + Name + state(Doctor));
                    Risen[0] = Found.get(0);
                })
                .thenIdle(40)
                .thenExecute(() -> {
                    Scp049Cured Follower = Risen[0];
                    Log.info("SCP-049 cured {} into look kind {} type {}", Name, Follower.lookKind(), Follower.mimicType());
                    Helper.assertTrue(Follower.isAlive(), "the cured " + Name + " did not survive its first moments");
                    Helper.assertTrue(Doctor.getUUID().equals(Follower.master()), "the cured " + Name + " does not serve SCP-049");
                    Helper.assertTrue(Follower.lookKind() == Scp049Cured.LookMimic && Type.equals(Follower.mimicType()), "the cured " + Name + " lost its look: kind "
                            + Follower.lookKind() + " type " + Follower.mimicType());
                    if (Uniformed) {
                        Helper.assertTrue(Follower.mimicTag().getList("ArmorItems", Tag.TAG_COMPOUND).stream().anyMatch(Each -> !((CompoundTag) Each).isEmpty()),
                                "the cured Theodore lost his uniform");
                        Helper.assertTrue(Patient.isRemoved() || Patient.isDeadOrDying(), "the cured Theodore is still walking around");
                    }
                    mimicProbe(Helper, Follower);
                    tidy(Helper);
                    Helper.killAllEntitiesOfClass(DClass.class);
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 1200, batch = "scp049_theodore_cure")
    public static void curesTheodore(GameTestHelper Helper) {
        Scp049Doctor Doctor = doctor(Helper, 24, 24, 24.5, 16.0);
        cureFlow(Helper, Doctor, theodore(Helper, 24, 20), "Theodore");
    }

    @GameTest(template = Arena, timeoutTicks = 1200, batch = "scp049_theodore_eater")
    public static void curesCloningTheodore(GameTestHelper Helper) {
        Scp049Doctor Doctor = doctor(Helper, 24, 24, 24.5, 16.0);
        Theodore Patient = theodore(Helper, 24, 20);
        Patient.beginEating();
        cureFlow(Helper, Doctor, Patient, "a Theodore eating arsenic");
    }

    @GameTest(template = Arena, timeoutTicks = 1200, batch = "scp049_theodore_clone")
    public static void curesTheodoreClone(GameTestHelper Helper) {
        Scp049Doctor Doctor = doctor(Helper, 24, 24, 24.5, 16.0);
        Theodore Patient = theodore(Helper, 24, 20);
        Patient.joinCluster(UUID.randomUUID());
        cureFlow(Helper, Doctor, Patient, "a Theodore clone");
    }

    @GameTest(template = Arena, timeoutTicks = 1200, batch = "scp049_theodore_dclass")
    public static void curesArmouredDClass(GameTestHelper Helper) {
        Scp049Doctor Doctor = doctor(Helper, 24, 24, 24.5, 16.0);
        DClass Patient = DClassRegistry.DClassType.get().spawn(Helper.getLevel(), Helper.absolutePos(new BlockPos(24, Floor, 20)), MobSpawnType.SPAWN_EGG);
        if (Patient == null) {
            throw new IllegalStateException("the D-Class could not be spawned");
        }
        Patient.identify(DClassTrait.Lazy, DClassCrime.random(Patient.getRandom()), Patient.appearance());
        Patient.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        Patient.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STICK));
        cureFlow(Helper, Doctor, Patient, "an armoured D-Class");
    }
}
