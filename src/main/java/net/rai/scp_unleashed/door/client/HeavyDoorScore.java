package net.rai.scp_unleashed.door.client;

import net.rai.scp_unleashed.door.DoorSounds;
import net.rai.scp_unleashed.door.DoorVoice;

import java.util.List;
import java.util.Optional;

public record HeavyDoorScore(List<DoorPart> Opening, List<DoorPart> Closing, List<DoorPart> Reversing, List<DoorPart> Opened, List<DoorPart> Closed,
                             Optional<DoorPart> Grain, int GrainEvery, Optional<DoorPart> Clank, List<Float> ClankAt) {
    private static final List<DoorPart> GateOpening = List.of(
            DoorPart.loop(DoorSounds.GateMove, 0.5f, 1.0f, 0.5f, 0.18f),
            DoorPart.loop(DoorSounds.GateMotor, 0.6f, 0.8f, 0.45f, 0.25f),
            DoorPart.hit(DoorSounds.GateEngage, 0.8f, 1.0f, 0, 1.0f));
    private static final List<DoorPart> GateClosing = List.of(
            DoorPart.loop(DoorSounds.GateMove, 0.45f, 1.0f, 0.5f, 0.16f),
            DoorPart.loop(DoorSounds.GateMotor, 0.55f, 0.78f, 0.45f, 0.22f),
            DoorPart.hit(DoorSounds.GateEngage, 0.75f, 1.0f, 0, 1.0f));
    private static final List<DoorPart> GateReversing = List.of(
            DoorPart.hit(DoorSounds.GateClank, 0.8f, 1.0f, 0, 1.0f));
    private static final List<DoorPart> GateOpened = List.of(
            DoorPart.hit(DoorSounds.GateSlam, 0.32f, 1.15f, 0, 1.0f),
            DoorPart.hit(DoorSounds.GateClang, 0.12f, 1.1f, 0, 1.0f),
            DoorPart.hit(DoorSounds.GateBoom, 0.3f, 1.2f, 0, 1.0f),
            DoorPart.hit(DoorSounds.GateRattle, 0.25f, 1.05f, 2, 1.0f));
    private static final List<DoorPart> GateClosed = List.of(
            DoorPart.hit(DoorSounds.GateSlam, 0.58f, 1.0f, 0, 1.0f),
            DoorPart.hit(DoorSounds.GateClang, 0.22f, 1.0f, 0, 1.0f),
            DoorPart.hit(DoorSounds.GateBoom, 0.5f, 1.0f, 0, 1.0f),
            DoorPart.hit(DoorSounds.GateRattle, 0.45f, 1.0f, 3, 1.0f));
    private static final DoorPart GateGrain = DoorPart.grain(DoorSounds.GateGrind, 0.7f, 1.0f, 22, 0.35f, 0.08f);
    private static final DoorPart GateClank = DoorPart.hit(DoorSounds.GateClank, 0.65f, 1.0f, 0, 0.4f);
    private static final List<DoorPart> ChamberOpening = List.of(
            DoorPart.loop(DoorSounds.ContainmentMove, 0.5f, 1.0f, 0.45f, 0.15f),
            DoorPart.grain(DoorSounds.ContainmentHiss, 0.9f, 1.0f, 10, 1.0f, 0.0f),
            DoorPart.hit(DoorSounds.ContainmentUnlatch, 0.7f, 1.0f, 0, 1.0f));
    private static final List<DoorPart> ChamberClosing = List.of(
            DoorPart.loop(DoorSounds.ContainmentMove, 0.48f, 1.0f, 0.45f, 0.15f),
            DoorPart.grain(DoorSounds.ContainmentHiss, 0.35f, 1.2f, 5, 1.0f, 0.0f),
            DoorPart.hit(DoorSounds.ContainmentUnlatch, 0.45f, 1.0f, 0, 1.0f));
    private static final List<DoorPart> ChamberReversing = List.of(
            DoorPart.hit(DoorSounds.ContainmentUnlatch, 0.45f, 1.1f, 0, 1.0f));
    private static final List<DoorPart> ChamberOpened = List.of(
            DoorPart.hit(DoorSounds.ContainmentSeal, 0.4f, 1.15f, 0, 1.0f),
            DoorPart.hit(DoorSounds.ContainmentChunk, 0.3f, 1.1f, 0, 1.0f),
            DoorPart.hit(DoorSounds.ContainmentThump, 0.25f, 1.0f, 0, 1.0f));
    private static final List<DoorPart> ChamberClosed = List.of(
            DoorPart.hit(DoorSounds.ContainmentSeal, 0.75f, 1.0f, 0, 1.0f),
            DoorPart.hit(DoorSounds.ContainmentChunk, 0.6f, 1.0f, 0, 1.0f),
            DoorPart.hit(DoorSounds.ContainmentThump, 0.5f, 1.0f, 0, 1.0f),
            DoorPart.hit(DoorSounds.ContainmentVent, 0.22f, 1.0f, 5, 1.0f));
    private static final DoorPart ChamberGrain = DoorPart.grain(DoorSounds.ContainmentGrind, 0.8f, 1.0f, 22, 0.3f, 0.08f);

    public static final HeavyDoorScore Gate = new HeavyDoorScore(GateOpening, GateClosing, GateReversing, GateOpened, GateClosed, Optional.of(GateGrain), 20, Optional.of(GateClank), List.of(0.2f, 0.47f, 0.73f));
    public static final HeavyDoorScore Chamber = new HeavyDoorScore(ChamberOpening, ChamberClosing, ChamberReversing, ChamberOpened, ChamberClosed, Optional.of(ChamberGrain), 20, Optional.empty(), List.of());

    public static Optional<HeavyDoorScore> of(DoorVoice Voice) {
        return switch (Voice) {
            case Gate -> Optional.of(Gate);
            case Chamber -> Optional.of(Chamber);
            default -> Optional.empty();
        };
    }
}
