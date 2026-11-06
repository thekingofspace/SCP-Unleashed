package net.rai.scp_unleashed.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class CreativeTabOrder {
    private static final Map<String, List<Pattern>> Groups = new HashMap<>();
    private static final Map<String, List<Pattern>> Leading = new HashMap<>();
    private static final Map<String, List<Pattern>> Trailing = new HashMap<>();
    private static boolean Attached;
    private static final String[] Zones = {"lcz", "hcz", "ez", "greenhouse", "research", "office", "medical", "power", "containment_room"};
    private static final String[] ZoneParts = {"floor", "floor_marked", "floor_marked_corner", "floor_marked_outer", "floor_arrow", "wall_bottom", "wall_middle", "wall_top", "wall_window",
            "wall_stairs", "wall_slab", "ceiling", "pillar", "stairs", "slab"};
    private static final String[] Lighting = {"^(lcz|hcz|ez|greenhouse|research|office|medical|power|containment_room)_lamp$", "^facility_lamp_", "^fluorescent_light$|^emergency_light$", "^desk_lamp$", "^warning_beacon$"};
    private static final String[] Structures = {"^catwalk|^metal_grate$", "railing", "ladder", "^support_", "decor_pipe|cable_tray", "chain_link", "barrier$|^bollard$"};
    private static final String[] Props = {"^research_|^office_(?!desk|chair)|^power_", "locker|filing_cabinet|supply_crate|archive_box|storage",
            "^(?!exam).*(desk(?!_lamp)|table|counter)", "^(?!wheel).*(chair|couch|sofa|stool)",
            "^(?!vitals|privacy).*(monitor|terminal|screen|server_rack|computer)|tv$", "^(?!medical).*(^lab_|shelf|chemical|biohazard|specimen|beaker)",
            "medical|hospital|^iv_|wheelchair|exam|first_aid|defib|vitals|privacy", "vending|water_cooler|coffee|microwave|fridge|kitchen|sink",
            "light|lamp|camera|extinguisher|clock|sign$", "paper|binder|folder|whiteboard|cork|clipboard|book", "rack$|plant|trash|mop"};

    static {
        order("resources", "_ore$", "^raw_|_shard$", "_dust$|_blend$|^silicon$|^sulfur$", "_nugget$|_ingot$", "_block$", "_plate$|(?<!fuel)_rod$|_sheet$|^plastic$",
                "wire$|_coil$|circuit|capacitor|superconductor|heating_element|electric_motor|^rocket_engine$", "fuel_rod$|^reactive_material$");
        trail("resources", "lunar_");
        order("machines", "^wire_coil$|_wire$|^wire_cutter$|^invisible_ink$", "outlet$|transfer_block$", "^breaker$|^power_cutoff$|^power_sensor$|^charge_disperser$", "^power_receiver$|^power_emitter$",
                "^(?!combustion_).*generator$|solar", "^water_boiler$|^steam_turbine$|^industrial_turbine$|^cooling_tower$", "^oil_extractor$|^distillation_tower$|^chemical_processor$|^combustion_generator$|^oil_refinery$", "^reactor_controller$|^fuel_tower$|^control_rod_tower$|^booster_tower$|^coolant_tower$|^steam_channel$|^irradiation_chamber$", "^(?!instability_).*(reactor|heat_exchanger|vibrator|dissolver|vaporizer)", "^instability_reactor$", "^instability_matrix$|^instability_pylon$|^rift_collector$|^contained_gateway$", "^rocket_platform$|^rocket$|^portable_launch_pad$|^satellite_platform$|^satellite$", "^orbital_", "^battery$", "^micro_charging_platform$", "furnace|press$|centrifuge|assembler|purifier|materials_bench$|^air_infuser$|^disperser$", "^industrial_cooler$|^gas_compressor$|^industrial_tank$", "^chunk_loader$",
                "network_router|^network_hub$|^redstone_|port_range", "^radio_booster_tower$|^radio_mast$", "^computer$|^chat_server$|^message_database$|^network_upload_port$|^floppy_disc$|^point_miner$|^point_bank$|^satellite_terminal$", "^pipe$|^vent$|wall_port$|pressurizer$|tank$|^drain$|^fluid_dump$");
        order("facility", "^sliding_door$|^single_sliding_door$|^glass_(sliding|room|single)_door$", "^room_door$|^(lcz|hcz|ez)_door$|^(lcz|hcz|ez)_(room|single)_door$",
                "^containment_((tall|room|single)_)?door(_xl)?$|^((tall|room|single)_)?blast_gate$", "^containment_room_lockdown_shutter$", "^magnetized_", "^keycard_level|^keycard_omni$", "^keycard_scanner$|button$|lever$", "^screwdriver$",
                "glass|containment_wall|floor_drain|^reinforced|^scp_sign", "^facility_sign_", "tesla", "^sentry_turret$|^motion_detector$|^patrol_", "^cctv_camera$|^camera_tablet$", "^forcefield_", "^speaker$|^sound_console$|cassette|^particle_emitter$", "^storage_container|^sealed_case", "^warhead_",
                "^cloning_machine$|^dclass_blood$|^theodore_blood$|^sampling_needle$", "personnel_file", "^dclass_manager$", "^controller$", "^mtf_signal_radio$|^handheld_radio$", "^anti_memetic_pills$", "^empty_canister$|^amnestic_canister$", "^scramble_goggles$", "^arsenic$|^theodore_dog_tags$",
                "^dclass_spawn_egg$|^theodore_spawn_egg$|^theodore_amalgamation_spawn_egg$");
        List<String> Decoration = new ArrayList<>();
        Decoration.add("^prop_workbench$");
        Decoration.add("^decorative_(concrete|material)$");
        for (String Zone : Zones) {
            for (String Part : ZoneParts) {
                Decoration.add("^" + Zone + "_" + Part + "$");
            }
        }
        Decoration.add("^(lcz|hcz|ez)_(?!lamp$)");
        Decoration.add("^greenhouse_(?!lamp$)");
        Decoration.add("^medical_(floor_guide|dispensary_|headwall_|curtain_|scrub_sink_|sterilizer_|exam_lamp_|lightbox$|defib_cabinet$)");
        Decoration.add("^containment_room_(?!lamp$)");
        Decoration.add("^facility_(?!lamp_)");
        Collections.addAll(Decoration, Lighting);
        Collections.addAll(Decoration, Structures);
        Decoration.add("^scp4666_");
        Decoration.add("^scp902_");
        Collections.addAll(Decoration, Props);
        order("decoration", Decoration.toArray(String[]::new));
        order("decals", "^decal_scraper$", "decal_(blood|strange_fluid|puddle|oil_stain)", "decal_(claw|bullet|scorch|cracks|footprints)",
                "decal_(grime|corrosion|rust)", "decal_(hazard_sign|zone_sign|arrow|foundation)", "decal_(vent|fixture)");
        order("anomalies", "^rift_extractor$|^rift_detector$", "^unstable_fragment_|^apocalyptic_rift_fragment$|^dimensional_shard$", "archive|document");
        order("scp_items", "^scp001_", "^scarlet_(armor|king)", "^scp002_", "^scp003_", "^scp005_", "^scp006_", "^scp007_", "^scp008_", "^scp009_", "^scp012_", "^scp015_", "^scp016_", "^scp017_", "^scp018_", "^scp019_", "^scp020_", "^scp024_", "^scp027_", "^scp031_", "^scp035_", "^scp049_", "^scp055_", "^scp058_", "^scp063_", "^scp066_", "^scp073_", "^scp075_", "^scp076_", "^scp079_", "^scp082_", "^scp085_", "^scp087_", "^scp093_", "^scp096_", "^scp098_", "^scp105_", "^scp106_", "^scp107_", "^scp109_", "^scp111_", "^scp127_", "^scp129_", "^scp131_", "^scp140_", "^daevite_", "^scp143_", "^scp148_", "^scp149_", "^scp151_", "^scp163_", "^scp173_", "^scp178_", "^scp179_|^space_coordinator$", "^scp207_", "^scp215_", "^scp261_", "^scp268_", "^scp280_", "^scp294_", "^scp303_", "^scp323_", "^scp327_", "^scp330_", "^scp335_", "^scp337_", "^scp343_", "^scp346_", "^scp352_", "^scp354_", "^scp372_", "^scp407_", "^scp409_", "^scp420j_", "^scp427_", "^scp428_", "^scp439_", "^scp444_", "^scp457_", "^scp458_", "^scp465_", "^scp469_", "^scp472_", "^scp492_", "^scp500_", "^scp513_", "^scp529_", "^scp538_", "^scp539_", "^scp548_", "^scp553_", "^scp610_", "^scp682_", "^scp860_", "^scp939_", "^scp999_", "^scp3008_|^crowbar$", "^scp3199_", "^scp4666_", "^scp4845_", "^scp5001_", "^z0001_", "^z0002_", "^z0003_", "^z0004_", "^stranger_");
        lead("scp_items", "^lab_vial$");
        trail("scp_items", "_spawn_egg$");
        order("scp_blocks", "^scp001_", "^scarlet_(dormant|king)", "^scp002_", "^scp003_", "^scp005_", "^scp006_", "^scp008_", "^scp009_", "^scp012_", "^scp015_", "^scp016_", "^scp017_", "^scp018_", "^scp019_", "^scp020_", "^scp024_", "^scp031_", "^scp035_", "^scp049_", "^scp055_", "^scp058_", "^scp063_", "^scp066_", "^scp073_", "^scp075_", "^scp076_", "^scp079_", "^scp082_", "^scp085_", "^scp087_", "^scp093_", "^scp096_", "^scp098_", "^scp105_", "^scp106_", "^scp107_", "^scp109_", "^scp111_", "^scp127_", "^scp129_", "^scp131_", "^scp140_", "^daevite_", "^scp143_", "^scp148_", "^scp149_", "^scp151_", "^scp163_", "^scp173_", "^scp205_", "^scp207_", "^scp224_", "^scp244_", "^scp256_", "^scp261_", "^scp294_", "^scp330_", "^scp343_", "^scp354_", "^scp372_", "^scp409_", "^scp457_", "^scp458_", "^scp465_", "^scp472_", "^scp500_", "^scp553_", "^scp610_", "^scp860_", "^scp902_", "^scp1762_", "^scp2317_", "^scp3008_", "^scp4666_", "^scp4845_", "^scp5001_", "^z0004_");
        order("weapons", "bench", "^pistol$", "^revolver$", "^marksman_revolver$", "^shotgun$", "^combat_shotgun$", "^assault_rifle$", "^machine_gun$", "^flamethrower$", "^sniper", "disruptor", "^ultralaser$", "^micro_hid$", "^scp127_", "^scarlet_rifle$", "_grenade$",
                "ammo|round|shell|cartridge|bullet|magazine|^ultralaser_cell$|^fuel_canister$");
        order("equipment", "^mtf_", "^(?!site_guard_tactical_).*guard", "hazmat", "^gas_mask$", "d_class", "^goc_", "^serpents_hand_", "^chaos_insurgency_", "^site_guard_tactical_", "^scientist_", "^broken_god_", "^sarkic_");
        lead("equipment", "^facility_tablet$");
        order("foods", "kola|root_beer|orange|(?<!cho)cola|soda|drink|juice|tea$|water|fizz|energy|pop$|can$|cocoa$", "^scp4845_",
                "crunch|ranch|puffs|chips|crisps|snack|^chocolate_orbs$");
    }

    private CreativeTabOrder() {
    }

    private static void order(String Tab, String... Patterns) {
        Groups.put(Tab, compile(Patterns));
    }

    private static void lead(String Tab, String... Patterns) {
        Leading.put(Tab, compile(Patterns));
    }

    private static void trail(String Tab, String... Patterns) {
        Trailing.put(Tab, compile(Patterns));
    }

    private static List<Pattern> compile(String... Patterns) {
        List<Pattern> Compiled = new ArrayList<>();
        for (String Text : Patterns) {
            Compiled.add(Pattern.compile(Text));
        }
        return Compiled;
    }

    private static boolean matches(List<Pattern> Rules, String Path) {
        if (Rules == null) {
            return false;
        }
        for (Pattern Rule : Rules) {
            if (Rule.matcher(Path).find()) {
                return true;
            }
        }
        return false;
    }

    private static int tier(String Tab, ItemStack Stack) {
        ResourceLocation Id = BuiltInRegistries.ITEM.getKey(Stack.getItem());
        if (!Scp_unleashed.ModId.equals(Id.getNamespace())) {
            return 3;
        }
        if (matches(Trailing.get(Tab), Id.getPath())) {
            return 2;
        }
        return matches(Leading.get(Tab), Id.getPath()) ? 0 : 1;
    }

    private static int rank(List<Pattern> Rules, ItemStack Stack) {
        ResourceLocation Id = BuiltInRegistries.ITEM.getKey(Stack.getItem());
        if (!Scp_unleashed.ModId.equals(Id.getNamespace())) {
            return Rules.size() + 1;
        }
        for (int Index = 0; Index < Rules.size(); Index++) {
            if (Rules.get(Index).matcher(Id.getPath()).find()) {
                return Index;
            }
        }
        return Rules.size();
    }

    @SubscribeEvent
    public static void loaded(FMLLoadCompleteEvent Event) {
        Event.enqueueWork(() -> ModList.get().getModContainerById(Scp_unleashed.ModId).ifPresent(Container -> {
            IEventBus Bus = Container.getEventBus();
            if (Bus != null && !Attached) {
                Attached = true;
                Bus.addListener(EventPriority.LOWEST, BuildCreativeModeTabContentsEvent.class, CreativeTabOrder::reorder);
            }
        }));
    }

    public static void reorder(BuildCreativeModeTabContentsEvent Event) {
        ResourceLocation Tab = Event.getTabKey().location();
        List<Pattern> Rules = Scp_unleashed.ModId.equals(Tab.getNamespace()) ? Groups.get(Tab.getPath()) : null;
        if (Rules == null) {
            return;
        }
        List<ItemStack> Current = new ArrayList<>(Event.getParentEntries());
        List<ItemStack> Sorted = new ArrayList<>(Current);
        Sorted.sort(Comparator.<ItemStack>comparingInt(Stack -> tier(Tab.getPath(), Stack)).thenComparingInt(Stack -> rank(Rules, Stack)));
        if (Sorted.equals(Current)) {
            return;
        }
        for (ItemStack Stack : Sorted) {
            Event.remove(Stack, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
            Event.accept(Stack, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
        }
    }
}
