package TeamMS.agents;

import TeamMS.utility.Direction;
import TeamMS.utility.PathFinding;
import eis.iilang.*;
import TeamMS.MailService;
// import javafx.scene.transform.Rotate;
import massim.protocol.data.*;

import java.util.*;


public class GreedyAgent extends Agent{

    private String team = "A";
    private int step = 0;
    private Set<Task> tasks = new HashSet<>();
    private LinkedList<String> blocks = new LinkedList<String>();
    private LinkedList<String> attachedIndex = new LinkedList<String>();
    private LinkedList<Action> plans = new LinkedList<Action>();
    private LinkedList<String> dispenser = new LinkedList<String>(); // type.x.y
    //private LinkedList<String> doneDispenser = new LinkedList<String>(); // type.x.y
    private Set<LinkedList<Parameter>> things = new HashSet<>();
    private Set<LinkedList<Parameter>> obstacles = new HashSet<>();
    private Set<LinkedList<Parameter>> attached = new HashSet<>();
    private Set<Dot> goals = new HashSet<>();
    private int[][] map = new int[100][100];
    private Set<String> visited = new HashSet<>();
    private boolean initMap = false;
    protected ArrayList<Message> messages = new ArrayList<Message>();

    private Set<String> entity = new HashSet<>();
    private String prev_entity = null;
    protected HashMap<String, stepInfo> prevCoor = new HashMap<>();
    protected HashMap<String, stepInfo> currCoor = new HashMap<>();

    private Task task = null;
    private String target = null;
    private Info info = new Info();
    private int x = 50;
    private int y = 50;
    private int px = 0;
    private int py = 0;
    private int vision = 5;
    private Map<String, String> hold = new HashMap<String, String>() {{
        put("n", null);
        put("e", null);
        put("s", null);
        put("w", null);
    }};

    private class stepInfo {
        public int step;
        public String agentName;
        public int absx;
        public int absy;
        public int relx;
        public int rely;

        stepInfo(int step, String name, int x, int y, int x0, int y0) {
            this.step = step;
            agentName = name;
            absx = x;
            absy = y;
            relx = x0;
            rely = y0;
        }

        public String toString() {
            return "step: " + step +
                    " name: " + agentName +
                    " abs: (" + absx + "," + absy + ")," +
                    " rel: (" + relx + "," + rely + ")";
        }
    }


    public GreedyAgent(String name, MailService mailbox) {
        super(name, mailbox);
    }

    protected class Message{
        public final Percept message;
        public final String sender;
        Message(Percept message, String sender){
            this.message = message;
            this.sender = sender;
        }
    }

    private class Dot {
        public int x;
        public int y;

        Dot(int x, int y) {
            this.x = x;
            this.y = y;
        }

        Dot(String st){
            String[] xy = st.split("\\.");
            this.x = new Integer(xy[0]);
            this.y = new Integer(xy[1]);
        }

        Dot(Position p){
            this.x = p.x;
            this.y = p.y;
        }

        public String toString() {
            return String.format("%d.%d", x, y);
        }

        public Position toPosition(){
            return new Position(x,y);
        }
    }

    private class Info {
        private String lastAction = null;
        private String lastActionParams = null;
        private String lastActionResult = null;
        Info() {}
    }

    private class Task {
        private String name;
        private int deadline;
        private int reward;
        private Set<LinkedList<Parameter>> taskStructure = new HashSet<>();

        Task(LinkedList<Parameter> parameters) {
            name = parameters.get(0).toString();
            deadline = Integer.parseInt(parameters.get(1).toString());
            reward = Integer.parseInt(parameters.get(2).toString());
            for (Parameter block : ((ParameterList) parameters.get(3))) {
                taskStructure.add(((Function) block).getParameters());
            }
            // TODO: update required blocks
            //for (LinkedList<Parameter> param : taskStructure) {
            //    int x = Integer.parseInt(param.get(0).toString());
            //    int y = Integer.parseInt(param.get(1).toString());
            //    String type = param.get(2).toString();
            //}
        }
    }


    @Override
    public void handlePercept(Percept percept) {
        switch (percept.getName()) {
            case "thing":
                //say("thing is " + percept.getParameters());
                things.add(percept.getParameters());
                break;
            case "obstacle":
                //say("obstacle is " + percept.getParameters());
                obstacles.add(percept.getParameters());
                break;
            case "goal":
                //say("goal is " + percept.getParameters());
                int gx = Integer.parseInt(percept.getParameters().get(0).toString());
                int gy = Integer.parseInt(percept.getParameters().get(1).toString());
                goals.add(new Dot(gx,gy));
                break;
            case "attached":
                attached.add(percept.getParameters());
                break;
            //case "name":
            //    if (name == null)
            //        name = percept.getParameters().get(0).toString();
            //    break;
            case "team":
                team = percept.getParameters().get(0).toString();
                break;
            case "step":
                step = Integer.parseInt(percept.getParameters().get(0).toString());
                break;
            case "lastAction":
                Parameter lastAction = percept.getParameters().get(0);
                info.lastAction = lastAction.toString();
                //say("lastAction is " + info.lastAction);
                break;
            case "lastActionParams":
                Parameter lastActionParams = percept.getParameters().get(0);
                info.lastActionParams = lastActionParams.toString();
                //say("lastActionParams is " + info.lastActionParams);
                break;
            case "lastActionResult":
                Parameter lastActionResult = percept.getParameters().get(0);
                info.lastActionResult = lastActionResult.toString();
                //say("lastActionResult is " + info.lastActionResult);
                break;
            case "task":
                //say("task is " + percept.getParameters());
                Task task = new Task(percept.getParameters());
                tasks.add(task);
                break;
        }
    }

    @Override
    public Action step() {
        things.clear();
        obstacles.clear();
        attached.clear();
        attachedIndex.clear();
        dispenser.clear();
        blocks.clear();
        prev_entity = String.join("|", entity);
        entity.clear();

        List<Percept> percepts = getPercepts();
        for (Percept percept: percepts) {
            handlePercept(percept);
        }

        if (!initMap) {
            initMap = true;
            setMap();
        }
        if (info.lastAction != null)
            updateCoord();
        say("x and y is " + x + "," + y);
        visited.add(new Dot(x,y).toString());
        updateEmpty();
        updateThings();
        updateObstacles();
        updateAttached();

        Iterator<Message> m = messages.iterator();
        say("step is " + step);
        say("prev_entity is " + prev_entity);
        while (m.hasNext()) {
            Percept xx = m.next().message;
            String contents = xx.getParameters().get(0).toString();
            String[] minfo = contents.split(";");

            int iStep = Integer.parseInt(minfo[1]);
            String name = minfo[2];

            // enroll prev step if their percept entity is opposite
            if (iStep == step-1 && minfo.length > 6) {
                //say("PREV STEP!!!!!!!!!!!");
                for (String re: minfo[6].split("\\|")) {
                    String[] xy = re.split(",");
                    String per = String.format("%d,%d",  -Integer.parseInt(xy[0]), -Integer.parseInt(xy[1]));
                    for (String pe:prev_entity.split("\\|")) {
                        //say("PR is " + pe);
                        //say("per is " + per);
                        if (per.equals(pe)) {
                            //say("PREV~~~~~EQUAL!!");
                            String[] gap = pe.split(",");
                            String[] absxy = minfo[5].split(",");
                            stepInfo si = new stepInfo(iStep,
                                    name,
                                    Integer.parseInt(absxy[0]),
                                    Integer.parseInt(absxy[1]),
                                    px + Integer.parseInt(gap[0]),
                                    py + Integer.parseInt(gap[1]));
                            prevCoor.put(name, si);
                        }
                    }
                }
                // update prev step
            } else if (iStep == step-1 && prevCoor.containsKey(name)){
                stepInfo si = prevCoor.get(name);
                String[] absxy = minfo[5].split(",");
                int prevX = Integer.parseInt(absxy[0]);
                int prevY = Integer.parseInt(absxy[1]);
                int dx = prevX - prevCoor.get(name).absx; // diff by target agent
                int dy = prevY - prevCoor.get(name).absy;
                int relx = prevCoor.get(name).relx + dx;
                int rely = prevCoor.get(name).rely + dy;
                si.step = iStep;
                si.absx = prevX;
                si.absy = prevY;
                si.relx = relx;
                si.rely = rely;
                prevCoor.put(name, si);
            }

            // update current step
            if (iStep == step && prevCoor.containsKey(name)) {
                String[] absxy = minfo[5].split(",");
                int currX = Integer.parseInt(absxy[0]);
                int currY = Integer.parseInt(absxy[1]);
                int dx = currX - prevCoor.get(name).absx; // diff by target agent
                int dy = currY - prevCoor.get(name).absy;
                int relx = prevCoor.get(name).relx + dx;
                int rely = prevCoor.get(name).rely + dy;
                stepInfo si = new stepInfo(iStep, name, currX, currY, relx, rely);
                currCoor.put(name, si);
            }

            if (iStep < step-2) {
                m.remove();
            }
            //say("text is " + contents);
        }
        //say("PREV COOR is " + prevCoor.toString());
        //say("CURR COOR is " + currCoor.toString());
        //say("PREV KEY IS "  + prevCoor.keySet().toString());
        //say("CURR KEY IS "  + currCoor.keySet().toString());




        if (info.lastAction.equals("request") && info.lastActionResult.equals("success")) {
            say("TARGET WAS " + target);
            //doneDispenser.add(target);
            target = null;
        }

        if (info.lastAction.equals("attach") && info.lastActionResult.equals("success")) {
            String btype = target.split("\\.")[0];
            switch (info.lastActionParams) {
                case "[n]":
                    hold.put("n", btype);
                    break;
                case "[e]":
                    hold.put("e", btype);
                    break;
                case "[s]":
                    hold.put("s", btype);
                    break;
                case "[w]":
                    hold.put("w", btype);
                    break;
            }
            target = null;
        }

        if (plans.size() > 0 && info.lastActionResult.equals("success")) {
            say("KEEP WORKING ON PLAN, " + target);
            Action a = plans.remove(0);
            String d = a.getParameters().toString().substring(1,2);
            reportEntity(true, d);
            return a;
        }
        //else if (plans.size() > 0 && info.lastAction.equals("move") && info.lastActionResult.equals("failed_path")) {
        //    String direc = info.lastActionParams.toString().substring(1,2);
        //    switch (direc) {
        //        case "n":
        //            map[x][y-1] = 1;
        //            break;
        //        case "s":
        //            map[x][y+1] = 1;
        //            break;
        //        case "w":
        //            map[x-1][y] = 1;
        //            break;
        //        case "e":
        //            map[x+1][y] = 1;
        //            break;
        //    }
        //}

        if (target == null) {
            int min_dist = 9999;
            for (String d : blocks) {
                String[] dinfo = d.split("\\.");
                if (!hold.containsValue(dinfo[0])) {
                    int x0 = Integer.parseInt(dinfo[2]);
                    int y0 = Integer.parseInt(dinfo[3]);
                    String key = String.format("%d.%d", x0, y0);
                    if (attachedIndex.contains(key)) continue;
                    int dist = getManhattanDistance(x,y,x0,y0);
                    if (dist < min_dist) {
                        min_dist = dist;
                        target = d;
                    }
                }
            }
        }



        if (target == null) {
            int min_dist = 9999;
            //target = null;
            //say("WHAT LOOKS DISPENSOR");
            say("NO TARGET!!!!");
            say("dispenser: " + dispenser.toString());
            for (String d : dispenser) {
                String[] dinfo = d.split("\\.");
                say("DINFO[0] is " + dinfo[0]);
                say("HOLD.VALUES() is " + hold.values().toString());
                if (!hold.containsValue(dinfo[0])) {
                    //if (doneDispenser.contains(d)) continue;
                    int x0 = Integer.parseInt(dinfo[2]);
                    int y0 = Integer.parseInt(dinfo[3]);
                    int dist = getManhattanDistance(x,y,x0,y0);
                    if (dist < min_dist) {
                        min_dist = dist;
                        target = d;
                        //say("TARGET IN FOR LOOP(DISPENSOR):" + target);
                    }
                }
            }
        }

        if (target != null) {
            say("TARGET~!: " + target);
            String[] dinfo = target.split("\\.");
            int tx = Integer.parseInt(dinfo[2]);
            int ty = Integer.parseInt(dinfo[3]);

            plans.clear();
            PathFinding pf = new PathFinding(map, x, y, tx, ty);
            say("ANSWER IS " + pf.ans);
            int size = pf.ans.size();
            if (size > 2) {
                for (int i=0; i < size-2; i++) {
                    String[] p1 = pf.ans.get(i).split("\\.");
                    String[] p2 = pf.ans.get(i+1).split("\\.");
                    Integer horizonal = Integer.parseInt(p2[0]) - Integer.parseInt(p1[0]);
                    Integer vertical = Integer.parseInt(p2[1]) - Integer.parseInt(p1[1]);
                    // TODO: should rotate if agent has attached at given direction
                    switch (horizonal) {
                        case 1:
                            plans.add(new Action("move", new Identifier("e")));
                            break;
                        case -1:
                            plans.add(new Action("move", new Identifier("w")));
                            break;
                    }
                    switch (vertical) {
                        case 1:
                            plans.add(new Action("move", new Identifier("s")));
                            break;
                        case -1:
                            plans.add(new Action("move", new Identifier("n")));
                            break;
                    }
                    target = null;
                }
                Action a = plans.remove(0);
                String d = a.getParameters().toString().substring(1,2);
                reportEntity(true, d);
                return a;
            }

            int subx = tx - x;
            int suby = ty - y;
            String dx = "w";
            String dy = "n";
            if (subx > 0) dx = "e";
            if (suby > 0) dy = "s";

            //say("TX: " + tx);
            //say("TY: " + ty);
            //say("SUBX: " + subx);
            //say("SUBY: " + suby);
            //say("DX: " + dx);
            //say("DY: " + dy);

            int dist = getManhattanDistance(x, y, tx, ty);
            if (dist == 1) {
                reportEntity(false, "r");
                if (dinfo[1].equals("dispenser")) {
                    if (subx == -1) return new Action("request", new Identifier("w"));
                    if (subx ==  1) return new Action("request", new Identifier("e"));
                    if (suby == -1) return new Action("request", new Identifier("n"));
                    if (suby ==  1) return new Action("request", new Identifier("s"));
                } else if (dinfo[1].equals("block")) {
                    if (subx == -1) return new Action("attach", new Identifier("w"));
                    if (subx ==  1) return new Action("attach", new Identifier("e"));
                    if (suby == -1) return new Action("attach", new Identifier("n"));
                    if (suby ==  1) return new Action("attach", new Identifier("s"));

                }
            }

            //if (Math.random() > 0.5) {
            //    return new Action("move", new Identifier(dx));
            //} else {
            //    return new Action("move", new Identifier(dy));
            //}

        }

        //String[] hands = new String[]{"NORTH", "EAST", "SOUTH", "WEST"};
        //return randomMove();

        //say("NO TARGET~~");
        //say("substring: " + info.lastActionParams.toString());
        if (info.lastAction.equals("move") && info.lastActionResult.equals("success")) {
            say("STRAIGHT!!!");
            String direc = info.lastActionParams.toString().substring(1,2);
            String curr = new Dot(x,y).toString();
            switch (direc) {
                case "n":
                    curr = new Dot(x,y-1).toString();
                    break;
                case "s":
                    curr = new Dot(x,y+1).toString();
                    break;
                case "w":
                    curr = new Dot(x-1,y).toString();
                    break;
                case "e":
                    curr  = new Dot(x+1,y).toString();
                    break;
            }
            if (!visited.contains(curr)) {
                reportEntity(true, direc);
                return new Action("move", new Identifier(direc));
            }
        }
        //return valuableMove();
        return randomMove();
    }


    @Override
    public void handleMessage(Percept message, String sender) {
        messages.add(new Message(message, sender));
    }

    private void reportEntity(boolean isMove, String direc) {
        String move_str = "None";
        if (isMove) move_str = direc;
        Vector<String> boxes = new Vector<String>();
        for (String block:hold.keySet()) {
            String btype = hold.get(block);
            if (btype != null) {
                boxes.add(btype);
            }
        }
        String entities = String.join("|", entity);
        String carrying = String.join(",", boxes);
        String message = String.format("status;%d;%s;%s;%s;%d,%d;%s", step, getName(), carrying, move_str, x, y, entities);
        Percept per = new Percept("seeAgent", new Identifier(message));
        broadcast(per, getName());
    }

    private void reportGoal() {
        for (Dot d : goals) {
            int absx = x + d.x;
            int absy = y + d.y;
            //for (String key: prevCoor.keySet()) {
            //
            //}
        }
    }

    /** broadcast to agents on team that we have seen an agent */
    public void broadcastAgentSightings(){
        // say("Entities"+entity);

        Position self = new Position(x,y);
        String selfStr = new Dot(x,y).toString();
        String name = getName();

        // for each string in format x.y in entity (entity is in global coords):
        //  make a new Dot object for it
        //  discard it if its the same as the agents current loc
        //  convert to global coords
        //  make a new 'seeAgent' percept
        //  and broadcast the percept
        entity.stream().map(loc->new Dot(loc))
                .filter(loc->(!(loc.x==this.x && loc.y==this.y)))
                .map(loc->new Dot(loc.toPosition().toLocal(self)))
                .map(loc->new Percept("seeAgent", new Identifier(loc.toString())))
                .forEach(per->broadcast(per, name));
        // for(String loc: entity){ // entity is in global coords
        //     Dot d = new Dot(loc);
        //     if(d.x == this.x && d.y == this.y) continue;
        //     Position pos = d.toPosition();
        //     d = new Dot(pos.toLocal(new Position(x,y)));
        //     Percept p = new Percept("seeAgent", new Identifier(d.toString()));
        //     broadcast(p, getName());
        // }
    }



    private void setMap() {
        for (int[] row:map) {
            Arrays.fill(row, 0);
        }
    }

    private void updateCoord() {
        px = x;
        py = y;
        if (info.lastAction.equals("move") && info.lastActionResult.equals("success")) {
            switch (info.lastActionParams) {
                case "[n]":
                    this.y -= 1;
                    break;
                case "[e]":
                    this.x += 1;
                    break;
                case "[s]":
                    this.y += 1;
                    break;
                case "[w]":
                    this.x -= 1;
                    break;
            }
        }
    }

    private void updateEmpty() {
        for (int i=-vision; i < vision+1; i++) {
            for (int j=-vision; j < vision+1; j++) {
                if (Math.abs(i) + Math.abs(j) > 5) continue;
                map[x+i][y+j] = 0;
            }

        }
    }

    private void updateThings() {
        for (LinkedList<Parameter> parameters : things) {
            int dx;
            int dy;
            int tx;
            int ty;
            String type;
            switch (parameters.get(2).toString()) {
                case "dispenser":
                    dx = Integer.parseInt(parameters.get(0).toString());
                    dy = Integer.parseInt(parameters.get(1).toString());
                    say("DISPENSER " + dx + "." + dy);
                    type = parameters.get(3).toString();
                    tx = x + dx;
                    ty = y + dy;
                    dispenser.add(String.format("%s.dispenser.%d.%d", type, tx, ty));
                    say("dispenser " + String.format("%s.%d.%d", type, tx, ty));
                    break;
                case "block":
                    dx = Integer.parseInt(parameters.get(0).toString());
                    dy = Integer.parseInt(parameters.get(1).toString());
                    say("BLOCK" + dx + "." + dy);
                    type = parameters.get(3).toString();
                    tx = x + dx;
                    ty = y + dy;
                    blocks.add(String.format("%s.block.%d.%d", type, tx, ty));
                    say("block" + String.format("%s.%d.%d", type, tx, ty));
                    break;
                case "entity":
                    dx = Integer.parseInt(parameters.get(0).toString());
                    dy = Integer.parseInt(parameters.get(1).toString());
                    String tmp = new Dot(dx,dy).toString();
                    String XXX = parameters.get(3).toString();
                    boolean test = XXX.equals(team);
                    boolean test2 = !tmp.equals("0.0");

                    if (XXX.equals(team) && !tmp.equals("0.0")) {
                        entity.add(String.format("%d,%d", dx, dy));
                    }
                    break;
            }
        }
    }

    private void updateObstacles() {
        for (LinkedList<Parameter> parameters : obstacles) {
            int dx = Integer.parseInt(parameters.get(0).toString());
            int dy = Integer.parseInt(parameters.get(1).toString());
            map[x+dx][y+dy] = 1;
        }
    }

    private void updateAttached() {
        for (LinkedList<Parameter> parameters : attached) {
            int dx = Integer.parseInt(parameters.get(0).toString());
            int dy = Integer.parseInt(parameters.get(1).toString());
            int tx = x + dx;
            int ty = y + dy;
            attachedIndex.add(String.format("%d.%d", tx, ty));
        }
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
        reportEntity(true, direc);
        return new Action("move", new Identifier(direc));
    }

    public int getRandomNumberUsingNextInt(int min, int max) {
        Random random = new Random();
        return random.nextInt(max - min) + min;
    }

    public int getManhattanDistance(int x1, int y1, int x2, int y2) {
        int x0 = x1 - x2;
        int y0 = y1 - y2;
        return Math.abs(x0) + Math.abs(y0);
    }

    /** return a direction string, randomly selected according to bias */
    public String randomBiasedDirection(int n, int e, int s, int w){
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
    public Action randomBiasedMove(int n, int e, int s, int w){
        String direc = randomBiasedDirection(n, e, s, w);
        return new Action("move", new Identifier(direc));
    }

    public static int fromDirSymbol(String sym){
        int direc=-1;
        switch (sym) {
            case "n":
            case "[n]":
                direc = 0;
                break;
            case "e":
            case "[e]":
                direc = 1;
                break;
            case "s":
            case "[s]":
                direc = 2;
                break;
            case "w":
            case "[w]":
                direc = 3;
                break;
        }
        return direc;
    }

    public static String toDirSymbol(int r){
        String direc = "";
        switch (r) {
            case 0:
                direc = "n";
                break;
            case 1:
                direc = "e";
                break;
            case 2:
                direc = "s";
                break;
            case 3:
                direc = "w";
                break;
        }
        return direc;
    }

    /** determine which direction is most useful to move in according to how
     much unvisited area is within some number of squares */
    public Action valuableMove(){
        Map<String, Boolean> valid = calcValidDirections(x,y);
        if(trapped(valid)) return null;

        Map<String, Integer> weights = calcDirectionWeight(x, y, 10);
        // if we can't go in that direction (due to obstacle),
        // overwrite the weights of that direction
        for(String key: weights.keySet()){
            int dir = fromDirSymbol(key);
            if(!valid.get(key)) weights.put(key, 0);
        }
        return randomBiasedMove(weights.get("n"), weights.get("e"), weights.get("s"), weights.get("w"));
    }

    /** calc the 'value' for each of the 4 directions up to a distance away */
    public Map<String, Integer> calcDirectionWeight(int x, int y, int distance){
        Map<String, Integer> dirValues = calcDirectionValue(x, y, distance);
        // int total = dirValues.values().stream().reduce(0, Integer::sum);
        // int number = dirValues.values().min();
        int number = Collections.min(dirValues.values());
        if(number >= 0) number = number - 1;
        for(Map.Entry<String,Integer> entry :dirValues.entrySet()){
            dirValues.put(entry.getKey(), entry.getValue().intValue() - number);
        }
        return dirValues;
    }

    public Map<String, Integer> calcDirectionValue(int x, int y, int distance){
        int lefthalf = valueOfSection(x-distance, x, y-distance, y+distance);
        int righthalf = valueOfSection(x, x+distance, y-distance, y+distance);
        int tophalf = valueOfSection(x-distance, x+distance, y, y+distance); // south actually
        int bottomhalf = valueOfSection(x-distance, x+distance, y-distance, y);

        Map<String, Integer> value = new HashMap<String, Integer>() {{
            put(toDirSymbol(Direction.NORTH), bottomhalf);
            put(toDirSymbol(Direction.EAST), righthalf);
            put(toDirSymbol(Direction.SOUTH), tophalf);
            put(toDirSymbol(Direction.WEST), lefthalf);
        }};
        return value;
    }

    /** estimate how valuable the range of locations within the bounds are according to entryValue */
    public int valueOfSection(int xmin, int xmax, int ymin, int ymax){
        int value = 0;
        for(int i=0; i<map.length; i++){
            if(i<xmin || i>xmax) continue;
            for(int j=0; j<map[0].length; j++){
                if(j<ymin || j>ymax) continue;
                value += positionValue(i, j);
            }
        }
        return value;
    }

    public int positionValue(int x, int y){
        return entryValue(map[x][y]);
    }

    public int entryValue(int value){
        switch (value) {
            case 0: // no obstacle, not seen before, very valuable
                return 2;
            case 1: // obstacle! has no value
                return 0;
            case -1: // indicates 'seen' but no obstacle, kinda valuable
                return 1;
        }
        return 0;
    }







    /** return a random, but valid move action */
    public Action validMove(){
        Map<String, Boolean> valid = calcValidDirections(x,y);
        if(trapped(valid)) return null;

        int [] dirs = new int [4];
        // convert 0 to 1 and 1 to 0
        for(int i=0; i<4; i++){
            String s = toDirSymbol(i);
            boolean b = valid.get(s);
            if(b) dirs[i] = 1;
            else dirs[i] = 0;
        }
        return randomBiasedMove(dirs[0], dirs[1], dirs[2], dirs[3]);
    }

    public Map<String, Boolean> calcValidDirections(int x, int y){
        // int [] validDirs = calcValidDirections();
        Map<String, Boolean> valid = new HashMap<String, Boolean>();

        // convert 0 to 1 and 1 to 0
        for(int dir=0; dir<4; dir++){
            String symb = toDirSymbol(dir);
            int adjacent_entry = getAdjacentEntry(x, y, dir);
            int value;
            switch (adjacent_entry) {
                case 1: // obstacle!
                    valid.put(symb, false);
                    break;
                case 0: // no obstacle
                default: // other, (e.g. -1 if we've seen it before)
                    valid.put(symb, true);
                    break;
            }
        }
        return valid;
    }

    /** are we surrounded on all sides? */
    public Boolean trapped(Map<String, Boolean> dirs){
        HashSet<Boolean> valid = new HashSet<Boolean>(dirs.values());
        boolean singleentry = (valid.size() == 1);
        if(singleentry && valid.contains(true)) return false;
        else if(singleentry && valid.contains(false)) return true;
        return false;
    }

    public int getAdjacentEntry(int x, int y, int dir){
        int newx = x + Direction.DELTA_X[dir];
        int newy = y + Direction.DELTA_Y[dir];
        return map[newx][newy];
    }






}
