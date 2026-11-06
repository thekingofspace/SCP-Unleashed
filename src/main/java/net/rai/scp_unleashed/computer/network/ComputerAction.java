package net.rai.scp_unleashed.computer.network;

public enum ComputerAction {
    Close,
    App,
    FileDelete,
    FileRename,
    NoteSave,
    FileImport,
    FileExport,
    DriveDelete,
    DriveRename,
    ChatChannel,
    ChatSend,
    ChatView,
    ChatSave,
    DocumentSave,
    ButtonSave,
    ButtonDelete,
    ButtonPress,
    SoundSave,
    SoundDelete,
    SoundPress,
    ActionSave,
    ActionDelete,
    ActionRun,
    DatabaseRename,
    DatabaseClear,
    ShopBuy,
    ImageView,
    ChatImage;

    public static ComputerAction byId(int Id) {
        ComputerAction[] Every = values();
        return Every[Math.max(0, Math.min(Every.length - 1, Id))];
    }
}
