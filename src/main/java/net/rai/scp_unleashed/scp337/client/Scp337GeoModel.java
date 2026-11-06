package net.rai.scp_unleashed.scp337.client;

import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp337.Scp337Mass;
import net.rai.scp_unleashed.scp337.Scp337Registry;
import net.rai.scp_unleashed.scp337.Scp337Strands;
import software.bernie.geckolib.animation.AnimationProcessor;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.state.BoneSnapshot;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import java.util.ArrayList;
import java.util.List;

public class Scp337GeoModel extends GeoModel<Scp337Mass> {
    public static final ResourceLocation ModelPath = Scp337Registry.location("geo/entity/scp337/scp337.geo.json");
    public static final ResourceLocation TexturePath = Scp337Registry.location("textures/entity/scp337/scp337.png");
    public static final ResourceLocation AnimationPath = Scp337Registry.location("animations/entity/scp337/scp337.animation.json");
    public static final String[] ChainPrefixes = {"strand_", "wisp_"};
    public static final String LobePrefix = "lobe_";
    public static final int MaxChains = 256;
    public static final int MaxSegments = 8;
    private Scp337Strands.Table Table;
    private GeoBone[][] Chains;
    private GeoBone[] Lobes;
    private double[][] LobeAxes;
    private GeoBone Anchor;
    private final double[][] Buffer = new double[MaxSegments][3];

    @Override
    public ResourceLocation getModelResource(Scp337Mass Mass) {
        return ModelPath;
    }

    @Override
    public ResourceLocation getTextureResource(Scp337Mass Mass) {
        return TexturePath;
    }

    @Override
    public ResourceLocation getAnimationResource(Scp337Mass Mass) {
        return AnimationPath;
    }

    @Override
    public void setCustomAnimations(Scp337Mass Mass, long InstanceId, AnimationState<Scp337Mass> Animated) {
        if (!bindBones()) {
            return;
        }
        Scp337Strands Strands = Mass.strands();
        Strands.bind(Table);
        float Partial = Animated.getPartialTick();
        for (int Chain = 0; Chain < Chains.length; Chain++) {
            GeoBone[] Bones = Chains[Chain];
            Strands.pose(Chain, Partial, Buffer);
            for (int Part = 0; Part < Bones.length; Part++) {
                Bones[Part].setRotX((float) Buffer[Part][0]);
                Bones[Part].setRotY((float) Buffer[Part][1]);
                Bones[Part].setRotZ((float) Buffer[Part][2]);
            }
        }
        double Flinch = Strands.flinch(Partial);
        if (Flinch > 1.0e-4 && !Mass.isDeadOrDying()) {
            for (int Index = 0; Index < Lobes.length; Index++) {
                double Swing = Flinch * Math.sin(Index * 2.399 + (Mass.tickCount + Partial) * 0.9);
                GeoBone Lobe = Lobes[Index];
                Lobe.setRotX((float) (Lobe.getRotX() + Swing * LobeAxes[Index][0]));
                Lobe.setRotZ((float) (Lobe.getRotZ() + Swing * LobeAxes[Index][1]));
            }
        }
    }

    private boolean bindBones() {
        AnimationProcessor<Scp337Mass> Processor = getAnimationProcessor();
        GeoBone First = Processor.getBone("strand_0_0");
        if (First == null) {
            return false;
        }
        if (First == Anchor && Table != null) {
            return true;
        }
        List<String[]> Names = new ArrayList<>();
        List<GeoBone[]> Found = new ArrayList<>();
        List<double[][]> Rests = new ArrayList<>();
        List<double[]> Roots = new ArrayList<>();
        List<Boolean> Rising = new ArrayList<>();
        for (String Prefix : ChainPrefixes) {
            for (int Chain = 0; Chain < MaxChains; Chain++) {
                List<GeoBone> Bones = new ArrayList<>();
                for (int Part = 0; Part < MaxSegments; Part++) {
                    GeoBone Bone = Processor.getBone(Prefix + Chain + "_" + Part);
                    if (Bone == null) {
                        break;
                    }
                    Bones.add(Bone);
                }
                if (Bones.isEmpty()) {
                    break;
                }
                String[] ChainNames = new String[Bones.size()];
                double[][] Rest = new double[Bones.size()][3];
                for (int Part = 0; Part < Bones.size(); Part++) {
                    ChainNames[Part] = Bones.get(Part).getName();
                    BoneSnapshot Initial = Bones.get(Part).getInitialSnapshot();
                    Rest[Part][0] = Initial.getRotX();
                    Rest[Part][1] = Initial.getRotY();
                    Rest[Part][2] = Initial.getRotZ();
                }
                Names.add(ChainNames);
                Found.add(Bones.toArray(new GeoBone[0]));
                Rests.add(Rest);
                Roots.add(new double[]{Bones.get(0).getPivotX(), Bones.get(0).getPivotZ()});
                Rising.add(Prefix.equals("wisp_"));
            }
        }
        List<GeoBone> LobeList = new ArrayList<>();
        for (GeoBone Bone : Processor.getRegisteredBones()) {
            if (Bone.getName().startsWith(LobePrefix)) {
                LobeList.add(Bone);
            }
        }
        Lobes = LobeList.toArray(new GeoBone[0]);
        LobeAxes = new double[Lobes.length][2];
        for (int Index = 0; Index < Lobes.length; Index++) {
            double Ox = Lobes[Index].getPivotX();
            double Oz = Lobes[Index].getPivotZ();
            double Length = Math.sqrt(Ox * Ox + Oz * Oz);
            if (Length > 1.0e-3) {
                LobeAxes[Index][0] = -Oz / Length;
                LobeAxes[Index][1] = Ox / Length;
            }
        }
        Chains = Found.toArray(new GeoBone[0][]);
        Table = new Scp337Strands.Table(Names, Rests, Roots, Rising);
        Anchor = First;
        return true;
    }
}
