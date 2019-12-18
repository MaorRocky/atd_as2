import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.pattern.Patterns;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Stack;

import static akka.japi.pf.UnitMatch.match;
import static java.util.concurrent.TimeUnit.SECONDS;


public class UserActor extends AbstractActor {

    private User myUser;
    private ActorSelection serverRef;
    private ActorRef ParserHandler;
    private Timeout askTimeout;
    private Predicates predicates;
    private Stack<InviteGroup> inviteGroupStack;


    public UserActor() {
        myUser = new User(getSelf());
        askTimeout = new Timeout(Duration.create(1, SECONDS));
        predicates = new Predicates();
        inviteGroupStack = new Stack<>();

    }

    public void preStart() {
        serverRef = getContext().actorSelection(
                "akka.tcp://ServerSystem@127.0.0.1:3553/user/Server");
    }

    //set setParserHandler ActorRef at user connect command
    private void setParserHandler(ActorRef sender) {
        if (ParserHandler == null)
            ParserHandler = sender;
    }

    //sets myUser userName and boolean connected
    private void connectUserName(String name) {
        myUser.setUserName(name);
        myUser.connect();
    }

    //set From command field to Client and sends the command to the wanted ActorRef
    private void sendCommand(Command command, ActorRef actorRef) {
        command.setFrom(Command.From.Client);
        actorRef.tell(command, self());
    }


    //send command to the ParserActor to print the result
    private void print(Command.Type type, String str) {
        if (!str.equals(""))
            sendCommand(new Command(type, Command.From.Client, str), this.ParserHandler);
    }

    private void print2(InviteGroup inviteGroup) {
        this.ParserHandler.tell(inviteGroup, self());
    }

    private void printNotConnected() {
        print(Command.Type.Error, "you are not connected to the system");
    }

    //returns the current time
    private String getTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        return dateFormat.format(new Date());
    }


    //asks the server to connect the new user if he isn't connected already
    //if failed then false command will be printed
    private void connectUser(ConnectCommand command) {
        if (!myUser.isConnected()) {
            command.setUserRef(myUser.getUserActorRef());
            Command result = askServer(command);
            if (result.isSucceeded()) {
                connectUserName(command.getUser().getUserName());
            }
            print(command.getType(), result.getResultString());
        } else
            print(Command.Type.Error, "You're already connected");
    }

    //asks the server to disconnect user, if succeeded set myUser userName to null
    //and boolean connected to false
    //if not succeeded then printing the false command returned
    //from the server
    private void disconnectUser(DisConnectCommand command) {
        if (myUser.isConnected()) {
            command.setUser(myUser);
            Command result = askServer(command);
            if (result.isSucceeded()) {
                myUser.setUserName(null);
                myUser.disconnect();
                this.myUser = null;
                print(Command.Type.Disconnect, result.getResultString());
            } else
                print(Command.Type.Error, "server is offline! try again later!");
        } else
            printNotConnected();
    }

    //asks the server for command target ActorRef
    //if succeeded, send the result command which contains
    //the relevant message to the target user
    //else printing the false command returned from the server


    //asks the server actor the wanted command
    //if the server sends back result before timeout
    //returns the result
    //else returns false command with "server is offline!" result
    private Command askServer(Command command) {
        command.setFrom(Command.From.Client);
        Future<Object> future = Patterns.ask(serverRef, command, askTimeout);
        try {
            return (Command) Await.result(future, askTimeout.duration());
        } catch (Exception e) {
            command.setResult(false, "server is offline!");
            return command;
        }
    }

    private void sendToClient(Command command) {
        if (myUser.isConnected()) {
            Command result = askServer(command);
            if (result.isSucceeded()) {
                sendCommand(result, result.getUserResult().getUserActorRef());
            } else
                print(result.getType(), result.getResultString());
        } else
            printNotConnected();
    }


    //Printing the sent text message from another user
    private void createTextMessageToPrint(TextMessage TextMessage) {
        if (myUser.isConnected()) {
            String message = "[" + getTime() + "][" + TextMessage.getTargetUser().getUserName() + "][" +
                    TextMessage.getSourceUser().getUserName() + "] " + TextMessage.getMessage();
            print(Command.Type.UserTextMessage, message);
        }
    }

    //returns String message of file received from other user
    private String userFileMessage(FileMessage fileMessage) {
        if (myUser.isConnected()) {
            return "[" + getTime() + "][" + myUser.getUserName() + "][" +
                    fileMessage.getSourceUser().getUserName() + "] File received: " + fileMessage.getTargetFilePath();
        }
        return "user is not connected";
    }

    //Printing the sent text message from group
    private void groupUserText(GroupTextMessage textMessage) {
        if (myUser.isConnected()) {
            String message = "[" + getTime() + "][" + textMessage.getGroupName() + "][" +
                    textMessage.getSourceUser().getUserName() + "] " + textMessage.getMessage();
            print(Command.Type.Group_Text, message);
        }
    }

    private void downloadFile(FileMessage fileMessage) {
        Path path = Paths.get(fileMessage.getTargetFilePath());
        try {
            Files.write(path, fileMessage.getFile());
            String message = "";
            if (fileMessage.getType().equals(Command.Type.UserFileMessage))
                message = userFileMessage(fileMessage);
            print(Command.Type.UserFileMessage, message);
        } catch (Exception e) {
            print(Command.Type.Error, "Failed to download the sent file");
        }
    }

    private void groupConnection(CreateGroupCommand command) {
        if (myUser.isConnected()) {
            command.setSourceUser(myUser);
            serverRef.tell(command, self());
        } else {
            printNotConnected();
        }
    }

    /*sending invitation to the server-> groupManager -> Group -> client -> Group */
    private void groupInvitation(InviteGroup inviteGroup) {
        if (myUser.isConnected()) {
            inviteGroup.setSourceUser(myUser);
            this.serverRef.tell(inviteGroup, self());
        } else
            printNotConnected();
    }

    public void displayInvitation(InviteGroup inviteGroup) {
        this.inviteGroupStack.push(inviteGroup);
        print(inviteGroup.type, "You have been invited to " + inviteGroup.getGroupName() + " ,accept?" +
                "\n\"Yes\" or \"No\"");
    }

    private void replyToInvitation(Command cmd) {
        if (!inviteGroupStack.isEmpty()) {
            InviteGroup group = inviteGroupStack.pop();
            group.setAnswer(cmd.getResultString());
            group.setGaveAnswer(true);
            if (cmd.getResultString().equals("Yes")) {
                this.myUser.addGroupToUsersGroups(group.getGroupActorRef());
            }
            group.getGroupActorRef().tell(group, self());
        } else print(new Command(Command.Type.Error, Command.From.UserConnection).getType(),
                "Error no invitations");
    }

    private void sendGroupMessage(GroupTextMessage groupTextMessage) {
        if (myUser.isConnected()) {
            groupTextMessage.setSourceUser(myUser);
            this.serverRef.tell(groupTextMessage, self());
        } else
            printNotConnected();
    }

    /*this method checks that we did manage to create a group*/
    private void replyFromGroupsConnection(CreateGroupCommand command) {
        /*if admin managed to create the group we will add
         * the group to the group hashSet*/
        if (command.getType().equals(Command.Type.Create_Group)
                && command.isSucceeded()) {
            this.myUser.addGroupToUsersGroups(command.getGroupRef());
            print(Command.Type.Error, command.getGroupRef().toString());
            print(command.getType(), command.getResultString());
        }
    }


    /*sendToClient will be used when a user sends a message to another client*/
    /*createTextMessageToPrint will be used when a user will receive a message from another client*/
    public Receive createReceive() {

        return receiveBuilder()
                .match(ConnectCommand.class, predicates.connectCommandPred, (command) -> {
                    setParserHandler(getSender());
                    connectUser(command);
                })
                .match(DisConnectCommand.class, predicates.disconnectCmd, this::disconnectUser)
                .match(TextMessage.class, predicates.sendTextToAnotherClient, this::sendToClient)
                .match(TextMessage.class, predicates.receiveTextFromAnotherClient, this::createTextMessageToPrint)
                .match(FileMessage.class, predicates.sendFileToAnotherClient, this::sendToClient)
                .match(FileMessage.class, predicates.receiveFileClient, this::downloadFile)
                .match(CreateGroupCommand.class, predicates.createGroup, this::groupConnection)
                .match(CreateGroupCommand.class, predicates.createGroup_respond, this::replyFromGroupsConnection)
                .match(InviteGroup.class, predicates.InviteGroup, this::groupInvitation)
                .match(InviteGroup.class, predicates.InviteGroup_Error, (invitation) -> print(invitation.type, invitation.getResultString()))
                .match(InviteGroup.class, predicates.InviteGroup_Answer, (invitation) -> print(invitation.type, invitation.getResultString()))
                .match(InviteGroup.class, predicates.displayInvitation, this::displayInvitation)
                .match(Command.class, predicates.ReplyToInvitation, this::replyToInvitation)
                .match(Command.class, predicates.displayAnswerAndWelcome,
                        cmd -> print(cmd.type, cmd.getResultString()))
                .match(GroupTextMessage.class, predicates.groupTextMessageServer, this::sendGroupMessage)
                .match(GroupTextMessage.class, predicates.groupTextMessage, this::groupUserText)
                .match(Command.class, predicates.GroupError, (cmd) -> print(cmd.type, cmd.getResultString()))
                .match(Command.class, predicates.GroupUserLeft, (cmd) -> print(cmd.type, cmd.getResultString()))
                .match(GroupCommand.class, predicates.removeGroupFromUserActor, (cmd) -> {
                    myUser.getUsersGroups().remove(cmd.getTargetGroupRef());
                    print(cmd.getType(), "deleted " + cmd.getGroupName());
                })
                .match(RemoveUserGroup.class, predicates.removeUserFromGroup, this::groupConnection)
                .match(Command.class, predicates.ErrorCmd, (cmd) -> print(Command.Type.Error, cmd.getResultString()))
                .matchAny(x -> System.out.println("****\nERROR IM IN MATCHANY\n" + x + "\n****\n"))
                .build();
    }
}

