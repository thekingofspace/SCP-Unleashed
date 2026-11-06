package net.rai.scp_unleashed.scp129;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import org.jetbrains.annotations.Nullable;

public enum Scp129Strain {
    One(1, 0xC6E48B, 0xB4D63C, 0xB2D25A, 0xDDEAB8),
    Two(2, 0x85D369, 0x4FB32A, 0x5BBE3A, 0xAEDC9C),
    Three(3, 0x5CCC85, 0x1E9E62, 0x2FAA6C, 0x8FD6B0),
    Four(4, 0x9DB04E, 0x3B5A17, 0x4E7023, 0xADC082),
    Five(5, 0x51CDBC, 0x1F8F94, 0x2E9EA0, 0x8AD3D6);

    public static final int All = 0b11111;
    private static final Scp129Strain[] Ordered = values();

    private final int Digit;
    private final int TextColour;
    private final int VialColour;
    private final int CultureColour;
    private final int MistColour;
    private final ResourceLocation Id;

    Scp129Strain(int Digit, int TextColour, int VialColour, int CultureColour, int MistColour) {
        this.Digit = Digit;
        this.TextColour = TextColour;
        this.VialColour = VialColour;
        this.CultureColour = CultureColour;
        this.MistColour = MistColour;
        this.Id = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp129_" + Digit);
    }

    public int digit() {
        return Digit;
    }

    public int index() {
        return Digit - 1;
    }

    public int bit() {
        return 1 << (Digit - 1);
    }

    public int textColour() {
        return TextColour;
    }

    public int vialColour() {
        return VialColour;
    }

    public int cultureColour() {
        return CultureColour;
    }

    public int mistColour() {
        return MistColour;
    }

    public ResourceLocation id() {
        return Id;
    }

    public String nameKey() {
        return "labware.scp_unleashed.agent.scp129_" + Digit;
    }

    public boolean in(int Strains) {
        return (Strains & bit()) != 0;
    }

    public static Scp129Strain byIndex(int Index) {
        return Ordered[Math.floorMod(Index, Ordered.length)];
    }

    @Nullable
    public static Scp129Strain byDigit(int Wanted) {
        return Wanted >= 1 && Wanted <= Ordered.length ? Ordered[Wanted - 1] : null;
    }

    public static int count(int Strains) {
        return Integer.bitCount(Strains & All);
    }

    public static String digits(int Strains) {
        StringBuilder Written = new StringBuilder();
        for (Scp129Strain Strain : Ordered) {
            if (Strain.in(Strains)) {
                Written.append(Strain.Digit);
            }
        }
        return Written.toString();
    }

    public static int parse(String Typed) {
        if (Typed.equalsIgnoreCase("all")) {
            return All;
        }
        int Strains = 0;
        for (char Symbol : Typed.toCharArray()) {
            Scp129Strain Strain = byDigit(Symbol - '0');
            if (Strain == null) {
                return -1;
            }
            Strains |= Strain.bit();
        }
        return Strains;
    }
}
