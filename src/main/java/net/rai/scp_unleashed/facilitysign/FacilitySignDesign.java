package net.rai.scp_unleashed.facilitysign;

public enum FacilitySignDesign {
    GateA("gate_a", false),
    GateB("gate_b", false),
    GateC("gate_c", false),
    RestroomMen("restroom_men", false),
    RestroomWomen("restroom_women", false),
    RestroomAllGender("restroom_all_gender", false),
    EmergencyExit("emergency_exit", true),
    Cafeteria("cafeteria", false),
    Medical("medical", false),
    SecurityCheckpoint("security_checkpoint", false),
    Stairs("stairs", false),
    Elevator("elevator", false),
    LightContainment("light_containment", false),
    HeavyContainment("heavy_containment", false),
    EntranceZone("entrance_zone", false),
    Decontamination("decontamination", false),
    ServerRoom("server_room", false),
    Armory("armory", false),
    Maintenance("maintenance", false),
    EvacuationShelter("evacuation_shelter", false);

    public static final String Prefix = "facility_sign_";

    private final String Name;
    private final boolean Glow;

    FacilitySignDesign(String Name, boolean Glow) {
        this.Name = Name;
        this.Glow = Glow;
    }

    public String getName() {
        return Name;
    }

    public boolean glows() {
        return Glow;
    }

    public String largeId() {
        return Prefix + Name;
    }

    public String smallId() {
        return Prefix + Name + "_small";
    }

    public boolean hasHeader() {
        return Name.startsWith("gate_");
    }

    public String headerId() {
        return Prefix + Name + "_header";
    }
}
