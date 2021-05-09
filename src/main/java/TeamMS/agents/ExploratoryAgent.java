package TeamMS.agents;

import java.util.*;
import java.util.Random;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import eis.iilang.*;
import TeamMS.MailService;
import TeamMS.utility.*;

// Todo: 
// find goal zone
// wander in a consistent direction towards areas you havent seen yet
//  -count up the number of seen-before blocks in each quadrant of agents vision
// weight blocks in a radius according to their desirability 
// (unknown blocks desirable, walls & obstacles undesirable, )
// negative weigts for 

// import TeamMS.agents.*;
// import TeamMS.*;
// import massim.javaagents.agents.*;
// import massim.javaagents.MailService;


/**
 * An agent which encapsulates the logic of exploration (and functions).
 */
public class ExploratoryAgent extends BaseAgent {

    protected long seed;
    protected Random random_generator;

    protected Location currentLocation;
    protected MentalMap<String> mapstate;
    protected boolean disabled;
    protected float energy;
    protected int steps;
    protected int step;
    protected int sight;
    protected String team;

    protected String OBSTACLE = "X";
    protected String UNKNOWN = "-";
    protected String CLEAR = " ";



    /**
     * Constructor.
     * @param name    the agent's name
     * @param mailbox the mail facility
     */
    public ExploratoryAgent(String name, MailService mailbox) {
        super(name, mailbox);
        seed = (long) name.hashCode();
        random_generator = new Random(seed);
        steps = -1;
        // make a mental map of default size (twice the size of the true map)
        mapstate = new MentalMap<String>(UNKNOWN); 
        currentLocation = new Location(0,0);
    }

    public ExploratoryAgent(String name) {
        this(name, null);
    }

    @Override
    public void handlePercept(Percept percept) {}

    @Override
    public void handleMessage(Percept message, String sender) {}


    protected void processPercepts(Collection<Percept> percepts){
        // List<Percept> percepts = getPercepts();

        Map<String, List<Percept>> categorizedPercepts = percepts.stream()
            .collect(Collectors.groupingBy(per -> per.getName()));

        // handle initial percept
        if(steps == -1){
            if(categorizedPercepts.containsKey("vision")) sight = (int)processSingleNumeralPercept(categorizedPercepts.get("vision").get(0));
            if(categorizedPercepts.containsKey("team")) team = (String)processSingleIdentifierPercept(categorizedPercepts.get("team").get(0));
            if(categorizedPercepts.containsKey("steps")) steps = (int)processSingleNumeralPercept(categorizedPercepts.get("steps").get(0));
        }

        // check that the last action is successful
        // if not, do we need to undo it? (i.e. if we updated our beliefs to reflect that action already)
        List<Percept> lastActionResult = categorizedPercepts.get("lastActionResult");
        if( lastActionResult!=null && !lastActionResult.isEmpty() ){

            String result = processSingleIdentifierPercept(lastActionResult.get(0));
            Percept lastAction = categorizedPercepts.get("lastAction").get(0);
            String action = processSingleIdentifierPercept(lastAction);

            say("Result: "+ result + " Action: "+action);

            // update agent's current position if a move action was sucessful
            if(result.equals("success") && action.equals("move")){
                String lastActionParamsStr= categorizedPercepts.get("lastActionParams").toString();
                Percept lastActionParams = categorizedPercepts.get("lastActionParams").get(0);
                
                String wrappedDirStr = lastActionParams.getParameters().get(0).toString();
                int dirNum = currentLocation.getDirInt(wrappedDirStr);
                say(lastActionParams.getParameters().get(0).toString());
                
                currentLocation = currentLocation.add(new CardinalVector(dirNum, 1));
            }
        }
        say("currentLocation: "+currentLocation);

        // for each grid square we know we can see, mark it as seen
        // (will will re-mark any obstacles we 'unsaw' here in the next loop)
        List<Location> seen_locations = new ArrayList<Location>();
        for(int i=-5; i<=5; i++){
            for(int j=-5; j <=5; j++){
                Location loc = new Location(i,j).add(currentLocation);
                seen_locations.add(loc);
            }
        }
        mapstate.set_locations(seen_locations, CLEAR);
        mapstate.set_location(currentLocation, "A");

        // update mental map according to percepts
        List<Percept> obstacles = categorizedPercepts.get("obstacle");
        if( obstacles!=null && !obstacles.isEmpty() ){
            ArrayList<Location> obsLocs = new ArrayList<Location>(obstacles.size());
            for(Percept obs : obstacles){

                // extract the local coordinates
                List<Parameter> params = obs.getParameters();
                Number val0 = ((Numeral)params.get(0)).getValue();
                Number val1 = ((Numeral)params.get(1)).getValue();
                Location loc = new Location((int)val0, (int)val1);

                // convert from local (agent-relative) coordinates to global coordinates
                Location shifted = loc.add(currentLocation);
                mapstate.set_location(shifted, OBSTACLE);
            }
            mapstate.print_array();
        }
    }

    public Location [] getMinAndMax(Set<Location> locs){
        int minX, maxX, minY, maxY;
        minX = Integer.MAX_VALUE;
        minY = Integer.MAX_VALUE;
        maxX = Integer.MIN_VALUE;
        maxY = Integer.MIN_VALUE;
        for(Location loc : locs){
            int x = loc.X();
            int y = loc.Y();
            if(x < minX) minX = x;
            if(y < minY) minY = y;
            if(x > maxX) maxX = x;
            if(y > maxY) maxY = y;
        } 
        return new Location[]{new Location(minX, minY), new Location(maxX, maxY)};
    }

    /** return (width, height) using either the position class 
    * or location class (bc they are convenient, even if not precisely correct) */
    public Location getDimensions(Set<Location> attachedThings){
        // figure out how wide agent is according to the blocks its carrying
        // specified in attachedThings
        Location minp,maxp;
        Location[] pos = getMinAndMax(attachedThings);
        minp = pos[0];
        maxp = pos[1];
        int xdir = maxp.X() - minp.X();
        int ydir = maxp.Y() - minp.Y();
        return new Location(xdir,ydir);        
    }


    @Override
    public Action chooseAction(Collection<Percept> percepts){
        processPercepts(percepts);

        // look at map
        // public HashMap<Location, T> getSurroundingFeatures(Location xy, int radius)
        // public int count_marked(T mark)
        // MentalMap<String> submap = mapstate.submap(currentLocation, 10);
        say("ULOCS");

        HashMap<Location, String> features = mapstate.getSurroundingFeatures(currentLocation, 2);
        say(features.toString());

        Set<Location> locs = features.keySet();
        List<Location> ulocs = locs.stream().filter(loc -> features.get(loc).equals(UNKNOWN))
                                    .map(Location::binary)
                                    .collect(Collectors.toList());
        Location loc0 = new Location(0,0);
        for(Location loc: ulocs){
            loc0 = loc.add(loc0);
        }
        loc0 = currentLocation.add(loc0);
        
        // determine the direction of most unknowns within the window
        
        say("\n\nULOCS" + loc0);
        // say(ulocs.toString());

        // Location::binary
        //get origin centered on agent, add up the Locations of each square that is valuable
        // (if )

        // return randomMove();
        return randomBiasedMove(1,0,0,0);
        // return null;
    }

    /** return a direction string, randomly selected according to bias */
    public String randomBiasedDirection(int n, int s, int w, int e){
        int total = n + s + e + w;
        String direc = null;

        int r = getRandomNumberUsingNextInt(0,total);
        if(r<n){ // r is [0, n)
            direc = "n";
        }else if( r>=n && r<(n+s) ){ // r is [n, n+s)
            direc = "s";
        }else if( r>=(n+s) && r<(n+s+w) ){ // r is [n+s, n+s+w)
            direc = "w";
        }else if( r>=(n+s+w) ){ // r is [n+s+w, total]
            direc = "e";
        }
        return direc;
    }

    /** 
    * return a Move action, with direction biased according to values of n, s, e, or w.
    */
    public Action randomBiasedMove(int n, int s, int w, int e){
        String direc = randomBiasedDirection(n, s, w, e);
        return new Action("move", new Identifier(direc));
    }

    /** return a completely random direction */
    public String randomDirection(){
        int r = getRandomNumberUsingNextInt(0,4);
        String direc = null;

        switch (r) {
            case 0:
                direc = "n";
                break;
            case 1:
                direc = "s";
                break;
            case 2:
                direc = "e";
                break;
            case 3:
                direc = "w";
                break;
        }
        return direc;
    }

    /** return a Move with a completely random direction */
    public Action randomMove(){
        String direc = randomDirection();
        return new Action("move", new Identifier(direc));
    }

    public int getRandomNumberUsingNextInt(int min, int max) {
        Random random;
        if(random_generator != null) random = random_generator;
        else                         random = new Random(); 

        return random.nextInt(max - min) + min;
    }
}

