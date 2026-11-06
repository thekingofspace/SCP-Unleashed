package net.rai.scp_unleashed.scp024;

public enum Scp024Cue {
    None,
    Applause,
    Cheer,
    Ooh,
    Laugh,
    Drumroll,
    Fanfare,
    Sting,
    Buzzer,
    Ding,
    Tick,
    Go,
    Whistle,
    Welcome,
    Reveal,
    Finale,
    MusicLobby,
    MusicFade;

    private static final Scp024Cue[] All = values();

    public static Scp024Cue of(int Index) {
        return Index >= 0 && Index < All.length ? All[Index] : None;
    }
}
