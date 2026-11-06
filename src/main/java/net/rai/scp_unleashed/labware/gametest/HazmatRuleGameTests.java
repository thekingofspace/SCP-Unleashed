package net.rai.scp_unleashed.labware.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.gear.GearRegistry;
import net.rai.scp_unleashed.gear.GearTags;
import net.rai.scp_unleashed.labware.LabContamination;
import net.rai.scp_unleashed.labware.LabExposure;
import net.rai.scp_unleashed.labware.LabOrganism;
import net.rai.scp_unleashed.labware.LabOrganisms;
import net.rai.scp_unleashed.labware.LabwareEvents;
import net.rai.scp_unleashed.scp008.Scp008Infection;
import net.rai.scp_unleashed.scp008.Scp008Organism;
import net.rai.scp_unleashed.scp016.Scp016Affliction;
import net.rai.scp_unleashed.scp016.Scp016Organism;
import net.rai.scp_unleashed.scp020.Scp020Affliction;
import net.rai.scp_unleashed.scp020.Scp020Organism;
import net.rai.scp_unleashed.scp098.Scp098Host;
import net.rai.scp_unleashed.scp129.Scp129Infection;
import net.rai.scp_unleashed.scp129.Scp129Spread;
import net.rai.scp_unleashed.scp129.Scp129Strain;
import net.rai.scp_unleashed.scp217.Scp217Clockwork;
import net.rai.scp_unleashed.scp217.Scp217Organism;
import net.rai.scp_unleashed.scp217.Scp217PerfectOrganism;
import net.rai.scp_unleashed.scp253.Scp253Infection;
import net.rai.scp_unleashed.scp253.Scp253Organism;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class HazmatRuleGameTests {
    public static final String Arena = "disperser_arena";
    public static final double Floor = 2.0;
    public static final Vec3 Release = new Vec3(10.5, 3.5, 10.5);
    public static final double Reach = 5.0;
    public static final double Spot = 13.0;

    private HazmatRuleGameTests() {
    }

    private enum Outfit {
        Bare,
        GasMask,
        Hood,
        Suit
    }

    private record Check(String Name, Predicate<ServerPlayer> Protected, boolean MaskEnough) {
    }

    private static ServerPlayer subject(GameTestHelper Helper, String Name, double X, double Z, Outfit Worn) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = new FakePlayer(World, new GameProfile(UUID.randomUUID(), Name));
        Vec3 At = Helper.absoluteVec(new Vec3(X, Floor, Z));
        User.moveTo(At.x, At.y, At.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        User.setGameMode(GameType.SURVIVAL);
        dress(User, Worn);
        return User;
    }

    private static void dress(ServerPlayer User, Outfit Worn) {
        switch (Worn) {
            case GasMask -> User.setItemSlot(EquipmentSlot.HEAD, new ItemStack(GearRegistry.GasMask.get()));
            case Hood -> User.setItemSlot(EquipmentSlot.HEAD, new ItemStack(GearRegistry.HazmatHood.get()));
            case Suit -> {
                User.setItemSlot(EquipmentSlot.HEAD, new ItemStack(GearRegistry.HazmatHood.get()));
                User.setItemSlot(EquipmentSlot.CHEST, new ItemStack(GearRegistry.HazmatSuit.get()));
                User.setItemSlot(EquipmentSlot.LEGS, new ItemStack(GearRegistry.HazmatTrousers.get()));
                User.setItemSlot(EquipmentSlot.FEET, new ItemStack(GearRegistry.HazmatBoots.get()));
            }
            default -> {
            }
        }
    }

    private static void dismiss(GameTestHelper Helper, List<ServerPlayer> Users) {
        for (ServerPlayer User : Users) {
            LabContamination.clear(User);
            if (!User.isRemoved()) {
                Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
            }
        }
    }

    private static boolean maskEnough(LabOrganism Organism) {
        return Scp020Organism.Id.equals(Organism.getId());
    }

    private static void release(GameTestHelper Helper, LabOrganism Organism) {
        LabOrganisms.spread(Helper.getLevel(), Helper.absoluteVec(Release), LabExposure.NothingIgnored, Organism, Reach, null);
    }

    private static void unequip(ServerPlayer User, EquipmentSlot Slot) {
        ItemStack Old = User.getItemBySlot(Slot).copy();
        User.setItemSlot(Slot, ItemStack.EMPTY);
        LabwareEvents.unmaskCarrier(new LivingEquipmentChangeEvent(User, Slot, Old, ItemStack.EMPTY));
    }

    private static List<Check> predicates() {
        return List.of(
                new Check("SCP-008", Scp008Infection::isProtected, false),
                new Check("SCP-016", Scp016Affliction::isProtected, false),
                new Check("SCP-020", Scp020Affliction::isProtected, true),
                new Check("SCP-098", Scp098Host::isProtected, false),
                new Check("SCP-129", Scp129Infection::isProtected, false),
                new Check("SCP-217", Scp217Clockwork::isProtected, false),
                new Check("SCP-253", Scp253Infection::isProtected, false));
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "hazmat_rule_masks")
    public static void hazmatRuleMaskOnlyStopsScp020(GameTestHelper Helper) {
        List<ResourceLocation> Expected = List.of(Scp008Organism.Id, Scp016Organism.Id, Scp020Organism.Id, Scp129Strain.One.id(), Scp217Organism.Id, Scp217PerfectOrganism.Id, Scp253Organism.Id);
        for (ResourceLocation Id : Expected) {
            Helper.assertTrue(LabOrganisms.isKnown(Id), Id + " is not a registered organism");
        }
        List<String> Wrong = new ArrayList<>();
        for (LabOrganism Organism : LabOrganisms.all()) {
            boolean Enough = maskEnough(Organism);
            String Name = Organism.getId().toString();
            ServerPlayer Masked = subject(Helper, "hazmat_masked", Spot, 10.5, Outfit.GasMask);
            ServerPlayer Hooded = subject(Helper, "hazmat_hooded", 10.5, Spot, Outfit.Hood);
            ServerPlayer Bare = subject(Helper, "hazmat_bare", 8.0, 10.5, Outfit.Bare);
            if (Organism.protects(Masked) != Enough) {
                Wrong.add(Name + ": a gas mask " + (Enough ? "does not protect" : "protects"));
            }
            if (Organism.protects(Hooded) != Enough) {
                Wrong.add(Name + ": a hood alone " + (Enough ? "does not protect" : "protects"));
            }
            if (Organism.protects(Bare)) {
                Wrong.add(Name + ": bare skin protects");
            }
            release(Helper, Organism);
            if (Organism.isPresentOn(Masked) == Enough) {
                Wrong.add(Name + ": the masked player was " + (Enough ? "infected" : "spared"));
            }
            if (Organism.isPresentOn(Hooded) == Enough) {
                Wrong.add(Name + ": the hooded player was " + (Enough ? "infected" : "spared"));
            }
            if (!Organism.isPresentOn(Bare)) {
                Wrong.add(Name + ": the bare player was spared");
            }
            dismiss(Helper, List.of(Masked, Hooded, Bare));
        }
        ServerPlayer Masked = subject(Helper, "hazmat_masked", Spot, 10.5, Outfit.GasMask);
        for (Check Each : predicates()) {
            if (Each.Protected().test(Masked) != Each.MaskEnough()) {
                Wrong.add(Each.Name() + " isProtected with a gas mask is " + !Each.MaskEnough());
            }
        }
        Helper.assertTrue(GearTags.protectsFromToxicGas(Masked), "the gas mask stopped filtering toxic gas");
        Helper.assertTrue(Scp098Host.canHost(Masked), "a gas mask alone still keeps SCP-098 spores out");
        dismiss(Helper, List.of(Masked));
        Helper.assertTrue(Wrong.isEmpty(), "the gas mask rule is wrong: " + Wrong);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "hazmat_rule_suits")
    public static void hazmatRuleSuitStopsEverything(GameTestHelper Helper) {
        List<String> Wrong = new ArrayList<>();
        for (LabOrganism Organism : LabOrganisms.all()) {
            String Name = Organism.getId().toString();
            ServerPlayer Suited = subject(Helper, "hazmat_suited", Spot, 10.5, Outfit.Suit);
            if (!Organism.protects(Suited)) {
                Wrong.add(Name + ": the full suit does not protect");
            }
            release(Helper, Organism);
            if (Organism.isPresentOn(Suited)) {
                Wrong.add(Name + ": the suited player was infected");
            }
            if (!LabContamination.carries(Suited, Organism.getId())) {
                Wrong.add(Name + ": the suited player did not pick it up on the suit");
            }
            dismiss(Helper, List.of(Suited));
        }
        ServerPlayer Suited = subject(Helper, "hazmat_suited", Spot, 10.5, Outfit.Suit);
        for (Check Each : predicates()) {
            if (!Each.Protected().test(Suited)) {
                Wrong.add(Each.Name() + " isProtected ignores the full suit");
            }
        }
        Helper.assertFalse(Scp098Host.canHost(Suited), "a full suit does not keep SCP-098 spores out");
        Suited.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
        for (Check Each : predicates()) {
            if (Each.Protected().test(Suited) != Each.MaskEnough()) {
                Wrong.add(Each.Name() + " isProtected counts a suit without boots as " + !Each.MaskEnough());
            }
        }
        dismiss(Helper, List.of(Suited));
        Helper.assertTrue(Wrong.isEmpty(), "the full suit rule is wrong: " + Wrong);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "hazmat_rule_carriers")
    public static void hazmatRuleCarriersFollowEachOrganism(GameTestHelper Helper) {
        LabOrganism Plague = LabOrganisms.find(Scp008Organism.Id).orElseThrow();
        LabOrganism Mold = LabOrganisms.find(Scp020Organism.Id).orElseThrow();
        ServerPlayer Suited = subject(Helper, "hazmat_carrier", Spot, 10.5, Outfit.Suit);
        ServerPlayer Masked = subject(Helper, "hazmat_masked", 10.5, Spot, Outfit.GasMask);
        release(Helper, Plague);
        release(Helper, Mold);
        Helper.assertTrue(LabContamination.carries(Suited, Scp008Organism.Id) && LabContamination.carries(Suited, Scp020Organism.Id), "the suit did not carry both agents");
        Helper.assertFalse(Plague.isPresentOn(Suited) || Mold.isPresentOn(Suited), "the suited carrier was infected");
        Helper.assertTrue(Plague.isPresentOn(Masked), "a gas mask alone kept SCP-008 out");
        Helper.assertFalse(LabContamination.carries(Masked, Scp008Organism.Id), "a gas mask wearer carried SCP-008 instead of catching it");
        Helper.assertTrue(LabContamination.carries(Masked, Scp020Organism.Id), "a gas mask wearer did not carry SCP-020 on their gear");
        Helper.assertFalse(Mold.isPresentOn(Masked), "a gas mask did not keep SCP-020 out");
        Helper.assertFalse(LabContamination.canCarry(Masked, Plague), "a gas mask wearer can carry SCP-008");
        Helper.assertTrue(LabContamination.canCarry(Masked, Mold), "a gas mask wearer cannot carry SCP-020");
        ServerPlayer Bystander = subject(Helper, "hazmat_bystander", Spot, 12.0, Outfit.Bare);
        unequip(Suited, EquipmentSlot.CHEST);
        Helper.assertTrue(Plague.isPresentOn(Suited), "opening the suit did not release SCP-008 onto its wearer");
        Helper.assertTrue(Plague.isPresentOn(Bystander), "opening the suit did not release SCP-008 around it");
        Helper.assertFalse(LabContamination.carries(Suited, Scp008Organism.Id), "SCP-008 stayed on the opened suit");
        Helper.assertTrue(LabContamination.carries(Suited, Scp020Organism.Id), "SCP-020 left the suit while the hood was still on");
        Helper.assertFalse(Mold.isPresentOn(Suited) || Mold.isPresentOn(Bystander), "SCP-020 escaped while the hood was still on");
        unequip(Suited, EquipmentSlot.HEAD);
        Helper.assertTrue(Mold.isPresentOn(Suited), "taking the hood off did not release SCP-020 onto its wearer");
        Helper.assertTrue(Mold.isPresentOn(Bystander), "taking the hood off did not release SCP-020 around it");
        Helper.assertFalse(LabContamination.isContaminated(Suited), "the stripped carrier still carries something");
        ServerPlayer Shielded = subject(Helper, "hazmat_shielded", 10.5, 8.0, Outfit.Suit);
        Helper.assertTrue(Scp129Spread.pass(Masked, Scp129Strain.One.bit(), 1.0f, true) != 0, "a gas mask alone kept SCP-129 out of the air");
        Helper.assertFalse(LabContamination.carries(Masked, Scp129Strain.One.id()), "a gas mask wearer carried SCP-129");
        Helper.assertTrue(Scp129Spread.sheds(Masked), "a gas mask alone stops an SCP-129 carrier from spreading it");
        Helper.assertTrue(Scp129Spread.pass(Shielded, Scp129Strain.One.bit(), 1.0f, true) == 0, "a full suit let SCP-129 in");
        Helper.assertTrue(LabContamination.carries(Shielded, Scp129Strain.One.id()), "a full suit did not carry airborne SCP-129");
        ServerPlayer Patient = subject(Helper, "hazmat_patient", 8.0, 10.5, Outfit.Bare);
        Helper.assertTrue(Scp129Infection.infect(Patient, Scp129Strain.Two), "SCP-129 could not be applied");
        dress(Patient, Outfit.Suit);
        Helper.assertFalse(Scp129Spread.sheds(Patient), "a suited SCP-129 carrier still spreads it");
        dismiss(Helper, List.of(Suited, Masked, Bystander, Shielded, Patient));
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 120, batch = "hazmat_rule_carriers")
    public static void hazmatRuleCarrierSheddingPartOfTheSuit(GameTestHelper Helper) {
        LabOrganism Bloom = LabOrganisms.find(Scp253Organism.Id).orElseThrow();
        ServerPlayer Suited = subject(Helper, "hazmat_carrier", Spot, 10.5, Outfit.Suit);
        release(Helper, Bloom);
        Helper.assertTrue(LabContamination.carries(Suited, Scp253Organism.Id), "the suit did not carry SCP-253");
        Suited.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
        Helper.onEachTick(() -> LabContamination.tick(Suited));
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertFalse(LabContamination.isContaminated(Suited), "the bootless carrier never lost SCP-253"))
                .thenExecute(() -> {
                    Helper.assertTrue(Bloom.isPresentOn(Suited), "the bootless carrier lost SCP-253 without catching it");
                    dismiss(Helper, List.of(Suited));
                })
                .thenSucceed();
    }
}
