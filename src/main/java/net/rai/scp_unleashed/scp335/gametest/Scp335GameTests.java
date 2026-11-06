package net.rai.scp_unleashed.scp335.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.antimemetic.AntiMemeticRegistry;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.computer.ComputerBlockEntity;
import net.rai.scp_unleashed.computer.ComputerDeviceBlock;
import net.rai.scp_unleashed.computer.ComputerImages;
import net.rai.scp_unleashed.computer.ComputerRegistry;
import net.rai.scp_unleashed.computer.PortMedia;
import net.rai.scp_unleashed.computer.UploadPortBlockEntity;
import net.rai.scp_unleashed.computer.data.ComputerFile;
import net.rai.scp_unleashed.control.ControlManager;
import net.rai.scp_unleashed.control.ControlSessions;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import net.rai.scp_unleashed.scp335.Scp335DiskItem;
import net.rai.scp_unleashed.scp335.Scp335Episode;
import net.rai.scp_unleashed.scp335.Scp335Episodes;
import net.rai.scp_unleashed.scp335.Scp335Image;
import net.rai.scp_unleashed.scp335.Scp335Phase;
import net.rai.scp_unleashed.scp335.Scp335Registry;
import net.rai.scp_unleashed.scp335.Scp335Viewing;
import net.rai.scp_unleashed.senses.SensesRegistry;
import net.rai.scp_unleashed.stranger.StrangerEncounterCondition;
import net.rai.scp_unleashed.stranger.StrangerGift;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp335GameTests {
    public static final String Arena = "scp335_arena";
    public static final BlockPos ComputerSpot = new BlockPos(5, 1, 6);
    public static final BlockPos PortSpot = new BlockPos(7, 1, 6);

    private Scp335GameTests() {
    }

    private static ComputerBlockEntity computer(GameTestHelper Helper) {
        Helper.setBlock(ComputerSpot, ComputerRegistry.Computer.get().defaultBlockState().setValue(ComputerDeviceBlock.Facing, Direction.NORTH));
        return (ComputerBlockEntity) Helper.getLevel().getBlockEntity(Helper.absolutePos(ComputerSpot));
    }

    private static UploadPortBlockEntity port(GameTestHelper Helper, ItemStack Disc) {
        Helper.setBlock(PortSpot, ComputerRegistry.UploadPort.get().defaultBlockState());
        UploadPortBlockEntity Port = (UploadPortBlockEntity) Helper.getLevel().getBlockEntity(Helper.absolutePos(PortSpot));
        Port.insert(Disc);
        return Port;
    }

    private static Subject subject(GameTestHelper Helper, double X, double Z, @Nullable GameProfile Profile) {
        ServerLevel World = Helper.getLevel();
        Subject User = new Subject(World, Profile == null ? new GameProfile(UUID.randomUUID(), "scp335_test") : Profile);
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, 1.0, Z));
        User.moveTo(Spot.x, Spot.y, Spot.z, 180.0f, 0.0f);
        World.addNewPlayer(User);
        return User;
    }

    private static void dismiss(GameTestHelper Helper, ServerPlayer User) {
        if (!User.isRemoved()) {
            Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
        }
    }

    private static void skipTo(ServerPlayer User, Scp335Phase Phase, int Tick) {
        Scp335Episode Episode = Scp335Episodes.of(User);
        Episode.Phase = Phase;
        Episode.Tick = Tick;
        if (Phase == Scp335Phase.Dissolve) {
            User.setInvisible(true);
        }
    }

    private static ComputerFile original(UploadPortBlockEntity Port) {
        return Port.files().get(0);
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp335_view")
    public static void viewingStartsSequence(GameTestHelper Helper) {
        ComputerBlockEntity Computer = computer(Helper);
        UploadPortBlockEntity Port = port(Helper, new ItemStack(Scp335Registry.Disk.get()));
        Subject User = subject(Helper, 5.5, 4.5, null);
        Helper.assertTrue(Port.getMedia() == PortMedia.Floppy, "The SCP-335 disk was not read as a floppy by the upload port");
        Helper.assertTrue(Port.files().size() == 1 && Scp335Image.isOriginal(original(Port)), "The SCP-335 disk did not hold exactly the original image file");
        Helper.assertTrue(Scp335Image.FileName.equals(original(Port).Name()), "The image file is named " + original(Port).Name());
        Scp335Viewing.viewed(User, Computer, Port, original(Port));
        Scp335Episode Episode = Scp335Episodes.of(User);
        Helper.assertTrue(Episode != null && Episode.Phase == Scp335Phase.Glimpse, "Viewing the image did not start the glimpse");
        Helper.assertTrue(Episode.Lines.size() >= Scp335Episodes.MinLines && Episode.Lines.size() <= Scp335Episodes.MaxLines,
                "The episode picked " + Episode.Lines.size() + " lines");
        for (int Index = 1; Index < Episode.Lines.size(); Index++) {
            Helper.assertTrue(Episode.Lines.get(Index - 1) < Episode.Lines.get(Index), "The chosen lines are not in order: " + Episode.Lines);
        }
        Helper.runAfterDelay(Scp335Episodes.GlimpseTicks + 4, () -> {
            Scp335Episode Later = Scp335Episodes.of(User);
            Helper.assertTrue(Later != null && Later.Phase == Scp335Phase.Crawl, "The glimpse did not turn into the crawl");
            Helper.assertTrue(User.getForcedPose() == Pose.SWIMMING, "The viewer was not forced into the crawl pose");
            Helper.assertTrue(Scp335Episodes.isLocked(User), "The viewer is not locked during the crawl");
            Scp335Episodes.abort(User);
            Helper.assertTrue(User.getForcedPose() == null && !Scp335Episodes.isLocked(User), "Aborting did not release the viewer");
            dismiss(Helper, User);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp335_death")
    public static void sequenceEndsInDeath(GameTestHelper Helper) {
        ComputerBlockEntity Computer = computer(Helper);
        UploadPortBlockEntity Port = port(Helper, new ItemStack(Scp335Registry.Disk.get()));
        Subject User = subject(Helper, 5.5, 5.4, null);
        User.getInventory().add(new ItemStack(Items.STICK, 3));
        int Before = Scp335Episodes.absorptions();
        Scp335Viewing.viewed(User, Computer, Port, original(Port));
        Helper.assertTrue(Scp335Episodes.of(User) != null, "Viewing did not start the sequence");
        skipTo(User, Scp335Phase.Walk, 0);
        Helper.runAfterDelay(3, () -> {
            Scp335Episode Episode = Scp335Episodes.of(User);
            Helper.assertTrue(Episode != null && Episode.Phase == Scp335Phase.Dissolve, "A viewer standing at the computer did not start dissolving");
        });
        Helper.runAfterDelay(Scp335Episodes.HideAfter + 5, () -> Helper.assertTrue(User.isInvisible(), "The body stayed visible while dissolving"));
        Helper.runAfterDelay(Scp335Episodes.DissolveTicks + 10, () -> {
            Helper.assertTrue(User.isDeadOrDying(), "The viewer survived the sequence");
            Helper.assertTrue(User.getLastDamageSource() != null && User.getLastDamageSource().is(Scp335Registry.Absorbed),
                    "The viewer did not die of the absorbed damage type");
            Helper.assertTrue(Scp335Episodes.absorptions() == Before + 1, "The sequence absorbed " + (Scp335Episodes.absorptions() - Before) + " times");
            Helper.assertTrue(Scp335Episodes.of(User) == null, "The episode outlived the viewer");
            Helper.assertTrue(Port.getContent().getItem() instanceof Scp335DiskItem, "The disk left the computer's drive");
            AABB Around = new AABB(User.blockPosition()).inflate(4.0);
            Helper.assertTrue(!Helper.getLevel().getEntitiesOfClass(ItemEntity.class, Around, Drop -> Drop.getItem().is(Items.STICK)).isEmpty(),
                    "The viewer's belongings were not dropped normally");
            dismiss(Helper, User);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp335_copy")
    public static void copiesCarryIt(GameTestHelper Helper) {
        ComputerBlockEntity Computer = computer(Helper);
        UploadPortBlockEntity Port = port(Helper, new ItemStack(Scp335Registry.Disk.get()));
        ComputerFile Original = original(Port);
        ComputerFile Copy = Original;
        Helper.assertTrue(Scp335Image.isOriginal(Copy) && ComputerImages.isKnown(Copy.Document()), "The copy lost the picture");
        Helper.assertTrue(!Port.isWritable(), "The SCP-335 disk is writable");
        Helper.assertTrue(!Port.write(ComputerFile.note("EXTRA", "text")), "A file was written onto the SCP-335 disk");
        Helper.assertTrue(!Port.delete(0) && !Port.rename(0, "OTHER"), "The image could be deleted or renamed on the disk");
        Helper.assertTrue(Port.files().size() == 1 && Scp335Image.isOriginal(original(Port)), "The disk lost its image");
        Computer.addFile(Copy);
        Helper.assertTrue(Computer.getFiles().size() == 1 && Scp335Image.isOriginal(Computer.getFiles().get(0)), "The local copy lost the picture");
        Subject User = subject(Helper, 5.5, 4.5, null);
        Scp335Viewing.viewed(User, Computer, null, Computer.getFiles().get(0));
        Helper.assertTrue(Scp335Episodes.of(User) != null, "Viewing the local copy did not start the sequence");
        Port.eject();
        Port.insert(new ItemStack(ComputerRegistry.FloppyDisc.get()));
        Helper.assertTrue(Port.isWritable() && Port.write(Copy), "A plain floppy stopped being writable");
        Helper.assertTrue(Scp335Image.isOriginal(Port.files().get(0)), "Writing a copy to a plain floppy lost the picture");
        dismiss(Helper, User);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp335_protected")
    public static void protectedViewersUnaffected(GameTestHelper Helper) {
        ComputerBlockEntity Computer = computer(Helper);
        UploadPortBlockEntity Port = port(Helper, new ItemStack(Scp335Registry.Disk.get()));
        Subject Blind = subject(Helper, 4.5, 4.5, null);
        Blind.setItemSlot(EquipmentSlot.HEAD, new ItemStack(SensesRegistry.Blindfold.get()));
        Scp335Viewing.viewed(Blind, Computer, Port, original(Port));
        Helper.assertTrue(Scp335Episodes.of(Blind) == null, "A blindfolded viewer was affected");
        Subject Shielded = subject(Helper, 5.5, 4.5, null);
        Shielded.addEffect(new MobEffectInstance(AntiMemeticRegistry.Shielded, 600));
        Scp335Viewing.viewed(Shielded, Computer, Port, original(Port));
        Helper.assertTrue(Scp335Episodes.of(Shielded) == null, "An anti-memetic viewer was affected");
        Subject Builder = subject(Helper, 6.5, 4.5, null);
        Builder.setGameMode(GameType.CREATIVE);
        Scp335Viewing.viewed(Builder, Computer, Port, original(Port));
        Helper.assertTrue(Scp335Episodes.of(Builder) == null, "A creative viewer was affected");
        Subject Plain = subject(Helper, 7.5, 4.5, null);
        Scp335Viewing.viewed(Plain, Computer, Port, original(Port));
        Helper.assertTrue(Scp335Episodes.of(Plain) != null, "An unprotected viewer was not affected");
        Scp335Episodes.abort(Plain);
        for (ServerPlayer User : List.of(Blind, Shielded, Builder, Plain)) {
            dismiss(Helper, User);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp335_controlled")
    public static void controlledBodyDiesOnce(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        ComputerBlockEntity Computer = computer(Helper);
        UploadPortBlockEntity Port = port(Helper, new ItemStack(Scp335Registry.Disk.get()));
        Subject User = subject(Helper, 2.5, 2.5, null);
        DClass Body = DClassRegistry.DClassType.get().create(World);
        Vec3 BodySpot = Helper.absoluteVec(new Vec3(5.5, 1.0, 5.4));
        Body.moveTo(BodySpot.x, BodySpot.y, BodySpot.z, 180.0f, 0.0f);
        World.addFreshEntity(Body);
        UUID BodyId = Body.getUUID();
        Helper.assertTrue(ControlManager.possess(User, Body), "The test controller could not possess the D-Class");
        Helper.assertTrue(ControlSessions.isControlling(User), "The controller is not linked");
        String BodyName = ControlSessions.session(User).Identity().getString("ControlName");
        Helper.assertTrue(Scp335Episodes.speakerName(User).getString().equals(BodyName) && !BodyName.isBlank(), "Chat lines would not use the D-Class name");
        int Before = Scp335Episodes.absorptions();
        Scp335Viewing.viewed(User, Computer, Port, original(Port));
        Helper.assertTrue(Scp335Episodes.of(User) != null, "The controlled D-Class was not affected");
        skipTo(User, Scp335Phase.Dissolve, Scp335Episodes.DissolveTicks - 3);
        Helper.runAfterDelay(8, () -> {
            Helper.assertTrue(Scp335Episodes.absorptions() == Before + 1, "The controlled body was absorbed " + (Scp335Episodes.absorptions() - Before) + " times");
            Helper.assertTrue(!ControlSessions.isControlling(User), "The controller stayed in the absorbed body");
            Helper.assertTrue(User.isAlive() && !User.isDeadOrDying() && User.getHealth() > 0.0f, "The controller died with the body");
            Helper.assertTrue(!ControlManager.hasPendingDeath(User), "A deferred death is still pending");
            Helper.assertTrue(Scp335Episodes.of(User) == null && !User.isInvisible() && User.getForcedPose() == null, "The controller kept the episode");
            Helper.assertTrue(!(World.getEntity(BodyId) instanceof DClass Left && Left.isAlive()), "The D-Class body is still alive");
        });
        Helper.runAfterDelay(40, () -> {
            Helper.assertTrue(Scp335Episodes.absorptions() == Before + 1, "The controlled body was absorbed again");
            Helper.assertTrue(User.isAlive() && !User.isDeadOrDying(), "The controller died later");
            dismiss(Helper, User);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp335_released")
    public static void releasedBodyIsAbsorbed(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        ComputerBlockEntity Computer = computer(Helper);
        UploadPortBlockEntity Port = port(Helper, new ItemStack(Scp335Registry.Disk.get()));
        Subject User = subject(Helper, 2.5, 2.5, null);
        DClass Body = DClassRegistry.DClassType.get().create(World);
        Vec3 BodySpot = Helper.absoluteVec(new Vec3(4.5, 1.0, 3.5));
        Body.moveTo(BodySpot.x, BodySpot.y, BodySpot.z, 180.0f, 0.0f);
        World.addFreshEntity(Body);
        UUID BodyId = Body.getUUID();
        Helper.assertTrue(ControlManager.possess(User, Body), "The test controller could not possess the D-Class");
        Scp335Viewing.viewed(User, Computer, Port, original(Port));
        skipTo(User, Scp335Phase.Crawl, 10);
        Helper.runAfterDelay(2, () -> ControlManager.release(User));
        Helper.runAfterDelay(8, () -> {
            Helper.assertTrue(!ControlSessions.isControlling(User), "The release did not happen");
            Helper.assertTrue(Scp335Episodes.of(User) == null && User.getForcedPose() == null, "The controller carried the episode home");
            Helper.assertTrue(User.isAlive() && !User.isDeadOrDying(), "The controller died after releasing");
            Helper.assertTrue(!(World.getEntity(BodyId) instanceof DClass Left && Left.isAlive()), "The abandoned D-Class body was not absorbed");
            dismiss(Helper, User);
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "scp335_logout")
    public static void sequenceSurvivesLogout(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        ComputerBlockEntity Computer = computer(Helper);
        UploadPortBlockEntity Port = port(Helper, new ItemStack(Scp335Registry.Disk.get()));
        Subject User = subject(Helper, 5.5, 3.5, null);
        GameProfile Profile = User.getGameProfile();
        Scp335Viewing.viewed(User, Computer, Port, original(Port));
        skipTo(User, Scp335Phase.Crawl, Scp335Episodes.FirstLine + 2);
        Helper.runAfterDelay(4, () -> {
            Scp335Episode Episode = Scp335Episodes.of(User);
            Helper.assertTrue(Episode != null && Episode.Said == 1, "The first line was not said before the logout");
            Scp335Episodes.loggedOut(User);
            CompoundTag Saved = User.saveWithoutId(new CompoundTag());
            dismiss(Helper, User);
            Subject Back = new Subject(World, Profile);
            Back.load(Saved);
            World.addNewPlayer(Back);
            Helper.runAfterDelay(3, () -> {
                Scp335Episode Resumed = Scp335Episodes.of(Back);
                Helper.assertTrue(Resumed != null && Resumed.Phase == Scp335Phase.Crawl, "The sequence did not resume after the login");
                Helper.assertTrue(Resumed.Said == 1, "The resumed sequence repeated or lost lines: " + Resumed.Said);
                Helper.assertTrue(Back.getForcedPose() == Pose.SWIMMING, "The resumed viewer is not crawling");
                Scp335Episodes.abort(Back);
                dismiss(Helper, Back);
                Helper.succeed();
            });
        });
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp335_gift")
    public static void strangerGiftIsDisk(GameTestHelper Helper) {
        AnomalyDocument Document = AnomalyDocuments.server().get(Scp335Registry.DocumentId);
        Helper.assertTrue(Document != null, "The SCP-335 dossier did not load");
        Helper.assertTrue(Document.Condition() instanceof StrangerEncounterCondition, "SCP-335 does not arrive with a stranger");
        StrangerEncounterCondition Encounter = (StrangerEncounterCondition) Document.Condition();
        Helper.assertTrue("scp079_hobbyist".equals(Encounter.Stranger()), "SCP-335 arrives with " + Encounter.Stranger());
        Helper.assertTrue(Encounter.Gift().size() == 1, "The stranger brings " + Encounter.Gift().size() + " gifts");
        StrangerGift Gift = Encounter.Gift().get(0);
        ItemStack Stack = Gift.toStack();
        Helper.assertTrue(Stack.getItem() instanceof Scp335DiskItem && Stack.getCount() == 1, "The gift is " + Stack);
        Helper.assertTrue(Scp335Image.holdsOriginal(Stack), "The gifted disk does not hold the image");
        Helper.succeed();
    }

    public static final class Subject extends ServerPlayer {
        public Subject(ServerLevel World, GameProfile Profile) {
            super(World.getServer(), World, Profile, ClientInformation.createDefault());
            new Quiet(World.getServer(), this);
        }
    }

    private static final class Quiet extends ServerGamePacketListenerImpl {
        private Quiet(MinecraftServer Server, ServerPlayer User) {
            super(Server, new Connection(PacketFlow.SERVERBOUND), User, CommonListenerCookie.createInitial(User.getGameProfile(), false));
        }

        @Override
        public void send(Packet<?> Sent, @Nullable PacketSendListener Listener) {
        }
    }
}
