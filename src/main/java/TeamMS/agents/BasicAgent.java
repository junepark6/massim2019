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

    /** process a percept with a single numeral parameter (e.g. score, step, or energy) */
    public Number processSingleNumeralPercept(Percept per){
        if(per == null) return -1;
        List<Parameter> params = per.getParameters();
        return ((Numeral)params.get(0)).getValue();
    }

    public String processSingleIdentifierPercept(Percept per){
        if(per == null) return null;
        List<Parameter> params = per.getParameters();
        return ((Identifier)params.get(0)).getValue();
    }

    /** alias for processSingleIdentifierPercept */
    public String processLastActionOrResult(Percept per){
        return processSingleIdentifierPercept(per);
    }

    // public processMultipleIdentifiersPercept(Percept per){
    //     if(per == null) return null;
    //     List<Parameter> params = per.getParameters();
    //     for(Parameter par: params){
            
    //     }
    // }

    /** can be used to process either  */
    public Thing processThing(Percept per){
        if(per == null) return null;
        List<Parameter> params = per.getParameters();
        Number val0 = ((Numeral)params.get(0)).getValue();
        Number val1 = ((Numeral)params.get(1)).getValue();
        Location loc = new Location((int)val0, (int)val1);
        String val2 = ((Identifier)params.get(2)).getValue();

        String val3 = "";
        if(params.size() > 3) val3 = ((Identifier)params.get(3)).getValue();

        return new Thing((int)val0, (int)val1, val2, val3); // x, y, type, detail
    }

    /** convert an obstacle or attached percept into a Location */
    public Location processObstacleOrAttached(Percept per){
        if(per == null) return null;
        List<Parameter> params = per.getParameters();
        Number val0 = ((Numeral)params.get(0)).getValue();
        Number val1 = ((Numeral)params.get(1)).getValue();
        Location loc = new Location((int)val0, (int)val1);
        return loc;
    }

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
