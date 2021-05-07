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
public class BaseAgent extends BasicAgent {

    protected Action lastaction;

    /**
     * Constructor.
     * @param name    the agent's name
     * @param mailbox the mail facility
     */
    public BaseAgent(String name, MailService mailbox) {
        super(name, mailbox);
    }

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

    /** for a percept with many parameters, return them as a list of Strings */
    public List<String> processMultipleParamsPercept(Percept per){
        if(per == null) return null;
        List<Parameter> params = per.getParameters();
        System.out.println("\n\n\n"+params.getClass() + "  "+paramlist.getClass());
        
        List<String> param_strs = new ArrayList<String>(params.size());
        for(Parameter par: params){
            System.out.println(par +" "+par.getClass()+"\n\n\n");

            param_strs.add(par.toString());
        }
        return param_strs;
    }

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
        Action nextaction = chooseAction(percepts);
        lastaction=nextaction; // remember the last action so we can reverse our beliefs if it fails
        return nextaction;
    }
}
