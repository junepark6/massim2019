package TeamMS.agents;

import java.util.Random;
import java.util.Collection;
import java.util.*;
import java.util.List;
import eis.iilang.*;
import TeamMS.MailService;
import TeamMS.agents.*;

/**
 * An agent that contains different role-specific agents 
 * (each of which handles the logic for its specific role).
 * A MultiAgent recognizes when to switch between roles, 
 * and ensures each of its role-agents has the data require to make correct decisions .
 * This is not extremely efficient because we might end up with multiple mental maps mantained by a
 * single MultiAgent, but it seems like the easiest way to structure things for now.
 * (I'm hoping this will make it easier to test the behavior of each specific role.)
 * All role agents, as well as MultiAgent should extend BasicAgent, 
 * so we can put any common functionality in BasicAgent.
 */
public class MultiAgent extends BasicAgent {
    
    private BasicAgent currentRole;

    private BasicAgent exploratoryRole;
    private BasicAgent finderRole;
    private BasicAgent builderRole;
    
    public MultiAgent(String name, MailService mailbox) {
        super(name, mailbox);
        exploratoryRole = new ExploratoryAgent(name);
        currentRole = exploratoryRole;
    }

    @Override
    public Action chooseAction(Collection<Percept> percepts){
        // Collection<Percept> percepts = getPercepts();
        // process percepts enough to decide if we need to switch roles
        // then update currentRole with any percept data that is pertinent to its role
        // (either pass a subset of the percepts to )
        exploratoryRole.chooseAction(percepts);
        return currentRole.chooseAction(percepts);
    }

    // @Override
    // public Action step() {
    //     Action act = currentRole.step();
    //     return act;
    // }


}