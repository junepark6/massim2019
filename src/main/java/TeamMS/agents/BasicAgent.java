package TeamMS.agents;

import eis.iilang.*;
import TeamMS.MailService;
import massim.protocol.data.*;

import java.util.List;
import java.util.Set;

/**
 * A very basic agent.
 */
public class BasicAgent extends Agent {

    protected Action lastaction;

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

    // /** decide on the next action */
    // public abstract Action chooseAction();

    /** uses chooseAction to select the action, and notes the chosen action. */
    @Override
    public Action step() {
        List<Percept> percepts = getPercepts();
        percepts.stream()
                .filter(p -> p.getName().equals("step"))
                .findAny()
                .ifPresent(p -> {
                    Parameter param = p.getParameters().getFirst();
                    if(param instanceof Identifier) say("Step " + ((Identifier) param).getValue());
        });

        Action nextaction = chooseAction();
        lastaction=nextaction; // remember the last action so we can reverse our beliefs if it fails
        return nextaction;
    }

    /** return (width, height) as a position */
    public Position getWidth(Set<Position> attachedThings){
        // figure out how wide agent is according to the blocks its carrying
        // specified in attachedThings

        // attachedThings
        return new Position(0,0);
        
    }
}
