package net.rai.scp_unleashed.scp3008.plan;

public final class Scp3008Standins {
    public static final String[] Focal = {"minecraft:smooth_quartz_stairs", "minecraft:stripped_birch_wood", "minecraft:smoker", "minecraft:white_wool",
            "minecraft:cauldron", "minecraft:lectern", "minecraft:lime_wool", "minecraft:sea_lantern", "minecraft:loom", "minecraft:flowering_azalea", "minecraft:barrel"};
    public static final String[] Small = {"minecraft:oak_stairs", "minecraft:birch_stairs", "minecraft:birch_slab", "minecraft:light_gray_wool", "minecraft:white_concrete",
            "minecraft:oak_stairs", "minecraft:red_wool", "minecraft:lantern", "minecraft:white_wool", "minecraft:potted_oak_sapling", "minecraft:barrel"};
    public static final String[] Containers = {"minecraft:barrel", "minecraft:chest", "minecraft:barrel", "minecraft:chest", "minecraft:barrel", "minecraft:barrel",
            "minecraft:chest", "minecraft:barrel", "minecraft:barrel", "minecraft:barrel", "minecraft:barrel"};
    public static final String[] Plants = {"minecraft:potted_azalea_bush", "minecraft:potted_fern", "minecraft:potted_flowering_azalea_bush"};
    public static final String Wall = "minecraft:birch_trapdoor[open=true,half=top]";
    public static final String Ceiling = "minecraft:lantern[hanging=true]";

    private Scp3008Standins() {
    }

    public static String pick(int Variant) {
        int Department = Math.floorMod(Scp3008Code.slotDepartment(Variant), Focal.length);
        int Salt = Scp3008Code.slotSalt(Variant);
        return switch (Scp3008Code.slotRole(Variant)) {
            case Scp3008Code.RoleFocal -> Focal[Department];
            case Scp3008Code.RoleContainer -> Containers[Department];
            case Scp3008Code.RolePlant -> Plants[Math.floorMod(Salt, Plants.length)];
            case Scp3008Code.RoleWall -> Wall;
            case Scp3008Code.RoleCeiling -> Ceiling;
            default -> Small[Department];
        };
    }

    public static boolean reversed(String Id) {
        return Id.contains("_stairs");
    }
}
