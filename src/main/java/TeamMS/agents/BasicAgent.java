package TeamMS.agents;

import eis.iilang.*;
import TeamMS.MailService;

import java.util.List;

/**
 * A very basic agent.
 */
public class BasicAgent extends Agent {

    protected lastaction;

    /**
     * Constructor.
     * @param name    the agent's name
     * @param mailbox the mail facility
     */
    public BasicAgent(String name, MailService mailbox) {
        super(name, mailbox);
    }

    @Override
    public void handlePercept(Percept percept) {}

    @Override
    public void handleMessage(Percept message, String sender) {}

    @Override
    public Action step() {
        Action nextaction = chooseAction();
        lastaction=nextaction; // remember the last action so we can reverse our beliefs if it fails
        return lastaction;
    }
}
