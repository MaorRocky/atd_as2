import java.io.Serializable;

public class Invitation implements Serializable {
    private User source;
    private User target;
    private String groupName;
    private String invitation;
    private String answer;
    private boolean answered;
    private Command.Type type;

    public Invitation(CreateGroupCommand cmd) {
        this.source = cmd.getUserAdmin();
        this.target = cmd.getUserResult();
        this.groupName = cmd.getGroupName();
        this.invitation = cmd.getResultString();
        this.type = Command.Type.Invitation;
        this.answered = false;
    }

    public Invitation(String targetName, String answer) {
        this.target = new User(targetName);
        this.type = Command.Type.Invitation;
        this.answer = answer;
        this.answered = true;
    }

    public User getSource() {
        return source;
    }

    public User getTarget() {
        return target;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getInvitation() {
        return invitation;
    }

    public boolean isAnswered() {
        return answered;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
        this.answered = true;
        this.type = Command.Type.invitationAnswer;
    }

    public Command.Type getType() {
        return type;
    }
}
