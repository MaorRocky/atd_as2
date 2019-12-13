import akka.japi.pf.FI;

public class Predicates {

    public FI.TypedPredicate<ConnectCommand> connectCommandPred;
    public FI.TypedPredicate<DisConnectCommand> disconnectCmd;
    public FI.TypedPredicate<TextMessage> sendTextToAnotherClient;
    public FI.TypedPredicate<TextMessage> receiveTextFromAnotherClient;
    public FI.TypedPredicate<FileMessage> sendFileToAnotherClient;
    public FI.TypedPredicate<FileMessage> receiveFileClient;
    public FI.TypedPredicate<CreateGroupCommand> createGroup;
    public FI.TypedPredicate<InviteGroup> InviteGroup;
    public FI.TypedPredicate<InviteGroup> InviteGroup_Error;

    //Server predicate
    public FI.TypedPredicate<CreateGroupCommand> createGroupServer;
    public FI.TypedPredicate<InviteGroup> InviteGroupServer;

    //    UsersConnection
    public FI.TypedPredicate<ConnectCommand> ConnectCommandUserConnection;
    public FI.TypedPredicate<DisConnectCommand> DisConnectCommandUsersConnection;
    public FI.TypedPredicate<TextMessage> TextMessageUsersConnection;

    //    GroupsConnection
    public FI.TypedPredicate<CreateGroupCommand> GroupsConnectionCreateGroup;
    public FI.TypedPredicate<InviteGroup> GroupsConnectionInviteGroup;


    // Group
    public FI.TypedPredicate<InviteGroup> GroupInviteGroup;


    public Predicates() {
        //Client predicates
        connectCommandPred = cmd -> cmd.getType().equals(Command.Type.Connect);
        disconnectCmd = cmd -> cmd.getType().equals(Command.Type.Disconnect);
        sendTextToAnotherClient = cmd -> cmd.getFrom().equals(Command.From.IO)
                && (cmd.getType().equals(Command.Type.UserTextMessage));
        receiveTextFromAnotherClient = cmd -> cmd.getFrom().equals(Command.From.Client);
        sendFileToAnotherClient = cmd -> cmd.getType().equals(Command.Type.UserFileMessage)
                && cmd.getFrom().equals(Command.From.IO);
        receiveFileClient = cmd -> cmd.getType().equals(Command.Type.UserFileMessage);
        createGroup = cmd -> cmd.getType().equals(Command.Type.Create_Group)
                && cmd.getFrom().equals(Command.From.IO);
        InviteGroup = cmd -> cmd.getType().equals(Command.Type.Invite_Group)
                && cmd.getFrom().equals(Command.From.IO);
        InviteGroup_Error = cmd -> cmd.getType().equals(Command.Type.Invite_Group)
                && (!cmd.isSucceeded());
        //Server predicate
        createGroupServer = cmd -> cmd.getType().equals(Command.Type.Create_Group);
        InviteGroupServer = cmd -> cmd.getType().equals(Command.Type.Invite_Group);


        // UsersConnection
        ConnectCommandUserConnection = cmd -> cmd.getType().equals(Command.Type.Connect)
                && cmd.getFrom().equals(Command.From.Server);
        DisConnectCommandUsersConnection = cmd -> cmd.getType().equals(Command.Type.Disconnect)
                && cmd.getFrom().equals(Command.From.Server);
        TextMessageUsersConnection = cmd -> cmd.getType().equals(Command.Type.UserTextMessage)
                && cmd.getFrom().equals(Command.From.Server);
        //GroupsConnectionCreateGroup
        GroupsConnectionCreateGroup = cmd -> cmd.getType().equals(Command.Type.Create_Group)
                && cmd.getFrom().equals(Command.From.Server);
        GroupsConnectionInviteGroup = cmd -> cmd.getType().equals(Command.Type.Invite_Group)
                && cmd.getFrom().equals(Command.From.Server);
        //Group
        GroupInviteGroup = cmd -> cmd.getType().equals(Command.Type.Invite_Group)
                && cmd.getFrom().equals(Command.From.GroupsConnection);
    }
}