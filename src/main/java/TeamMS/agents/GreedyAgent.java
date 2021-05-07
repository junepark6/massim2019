package TeamMS.agents;

import TeamMS.utility.Direction;
import TeamMS.utility.PathFinding;
import eis.iilang.*;
import TeamMS.MailService;
// import javafx.scene.transform.Rotate;

import java.util.*;


public class GreedyAgent extends Agent{

    private Set<Task> tasks = new HashSet<>();
    private LinkedList<String> blocks = new LinkedList<String>();
    private LinkedList<String> attachedIndex = new LinkedList<String>();
    private LinkedList<String> blockType = new LinkedList<String>();
    private LinkedList<Action> plans = new LinkedList<Action>();
    private LinkedList<String> dispenser = new LinkedList<String>(); // type.x.y
    private LinkedList<String> doneDispenser = new LinkedList<String>(); // type.x.y
    private LinkedList<String> entity = new LinkedList<String>();
    private Set<LinkedList<Parameter>> things = new HashSet<>();
    private Set<LinkedList<Parameter>> obstacles = new HashSet<>();
    private Set<LinkedList<Parameter>> attached = new HashSet<>();
    private int[][] map = new int[100][100];

    private Task task = null;
    private String target = null;
    private Info info = new Info();
    private int x = 50;
    private int y = 50;
    private int vision = 5;
    private Map<Integer, String> hold = new HashMap<Integer, String>() {{
        put(Direction.NORTH, null);
        put(Direction.EAST, null);
        put(Direction.SOUTH, null);
        put(Direction.WEST, null);
    }};

    public GreedyAgent(String name, MailService mailbox) {
        super(name, mailbox);
    }

    private class Dot {
        public int x;
        public int y;

        Dot(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public String toString() {
            return String.format("%d.%d", x, y);
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
                break;
            case "attached":
                attached.add(percept.getParameters());
                break;
            case "name":
                //say("name is " + percept.getParameters().get(0));
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
        updateEmpty();
        if (info.lastAction != null)
            updateCoord();
        things.clear();
        obstacles.clear();
        attached.clear();
        attachedIndex.clear();
        dispenser.clear();
        blocks.clear();
        List<Percept> percepts = getPercepts();
        for (Percept percept: percepts) {
            handlePercept(percept);
        }
        updateThings();
        updateObstacles();
        updateAttached();

        int min_dist = 9999;
        target = null;
        for (String d : dispenser) {
            String[] dinfo = d.split("\\.");
            if (!blockType.contains(dinfo[0])) {
                if (doneDispenser.contains(d)) continue;
                int x0 = Integer.parseInt(dinfo[2]);
                int y0 = Integer.parseInt(dinfo[3]);
                int dist = getManhattanDistance(x,y,x0,y0);
                if (dist < min_dist) {
                    min_dist = dist;
                    target = d;
                }
            }
        }
        for (String d : blocks) {
            String[] dinfo = d.split("\\.");
            if (!blockType.contains(dinfo[0])) {
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
        if (target != null) {
            say("TARGET~!: " + target);
            String[] dinfo = target.split("\\.");
            int tx = Integer.parseInt(dinfo[2]);
            int ty = Integer.parseInt(dinfo[3]);
            int subx = tx - x;
            int suby = ty - y;
            String dx = "w";
            String dy = "n";
            if (subx > 0) dx = "e";
            if (suby > 0) dy = "s";

            say("TX: " + tx);
            say("TY: " + ty);
            say("SUBX: " + subx);
            say("SUBY: " + suby);
            say("DX: " + dx);
            say("DY: " + dy);

            int dist = getManhattanDistance(x, y, tx, ty);
            if (dist == 1) {
                if (dinfo[1].equals("dispenser")) {
                    doneDispenser.add(target);
                    target = null;
                    if (subx == Direction.DELTA_X[Direction.WEST]) return new Action("request", new Identifier("w"));
                    if (subx == Direction.DELTA_X[Direction.EAST]) return new Action("request", new Identifier("e"));
                    if (suby == Direction.DELTA_Y[Direction.NORTH]) return new Action("request", new Identifier("n"));
                    if (suby == Direction.DELTA_Y[Direction.SOUTH]) return new Action("request", new Identifier("s"));
                } else if (dinfo[1].equals("block")) {
                    target = null;
                    String key = String.format("%d.%d", tx, ty);
                    boolean test = attachedIndex.contains(key);
                    say("attachedIndex:" + attachedIndex.toString());
                    if (subx == Direction.DELTA_X[Direction.WEST]) return new Action("attach", new Identifier("w"));
                    if (subx == Direction.DELTA_X[Direction.EAST]) return new Action("attach", new Identifier("e"));
                    if (suby == Direction.DELTA_Y[Direction.NORTH]) return new Action("attach", new Identifier("n"));
                    if (suby == Direction.DELTA_Y[Direction.SOUTH]) return new Action("attach", new Identifier("s"));

                }
            }


            if (Math.random() > 0.5) {
                return new Action("move", new Identifier(dx));
            } else {
                return new Action("move", new Identifier(dy));
            }

        }

        //String[] hands = new String[]{"NORTH", "EAST", "SOUTH", "WEST"};
        return randomMove();
    }

    @Override
    public void handleMessage(Percept message, String sender) {

    }

    private void updateCoord() {
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
        say("x and y is " + x + "," + y);
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
                //case "entity":
                //    dx = Integer.parseInt(parameters.get(0).toString());
                //    dy = Integer.parseInt(parameters.get(1).toString());
                //    entity.add(String.format("%d.%d", x+dx, y+dy));
                //    break;
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

}
