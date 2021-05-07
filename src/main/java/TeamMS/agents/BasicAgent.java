package TeamMS.agents;

import eis.iilang.*;
import TeamMS.MailService;
import massim.protocol.data.*;
import TeamMS.utility.*;

import java.util.List;
import java.util.Set;
import java.util.*;


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
    public Action chooseAction(Collection<Percept> percepts){ return null; }

    /** uses chooseAction to select the action, and notes the chosen action. */
    @Override
    public Action step() {
        
        List<Percept> percepts = getPercepts();
        // say(percepts+"\n\n");

        // percepts.stream()
        //         .filter(p -> p.getName().equals("step"))
        //         .findAny()
        //         .ifPresent(p -> {
        //             Parameter param = p.getParameters().getFirst();
        //             if(param instanceof Identifier) say("Step " + ((Identifier) param).getValue());
        // });

        Action nextaction = chooseAction(percepts);
        lastaction=nextaction; // remember the last action so we can reverse our beliefs if it fails
        return nextaction;
    }
}
