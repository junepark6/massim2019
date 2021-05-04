package TeamMS.agents;

/**
 * A very basic agent.
 */
public class ExploratoryAgent extends Agent {

    protected long seed;
    protected Random random_generator;

    /**
     * Constructor.
     * @param name    the agent's name
     * @param mailbox the mail facility
     */
    public ExploratoryAgent(String name, MailService mailbox) {
        super(name, mailbox);
        seed = (long) name.hashCode();
        random_generator = new Random(seed);
        say("Test");
    }

    @Override
    public void handlePercept(Percept percept) {}

    @Override
    public void handleMessage(Percept message, String sender) {}

    @Override
    public Action step() {
        Collection<Percept> percepts = getPercepts();
        return randomMove();
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
