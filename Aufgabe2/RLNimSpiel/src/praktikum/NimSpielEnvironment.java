package praktikum;

import org.rlcommunity.rlglue.codec.EnvironmentInterface;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpec;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpecVRLGLUE3;
import org.rlcommunity.rlglue.codec.taskspec.ranges.DoubleRange;
import org.rlcommunity.rlglue.codec.taskspec.ranges.IntRange;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;
import org.rlcommunity.rlglue.codec.types.Reward_observation_terminal;
import org.rlcommunity.rlglue.codec.util.EnvironmentLoader;

public class NimSpielEnvironment implements EnvironmentInterface {

	private static final String TEAM_NAME ="Team 1";
	private static final String TEAM_MEMBERS ="Carsten Noetzel";
	
	private static WorldDescription world;
	
	@Override
	public void env_cleanup() {
	}

	@Override
	public String env_init() {
		// Diese Methode initialisiert das Enviroment, in disem Fall den 
		// Spieltisch an dem BlackJack gespielt wird
		world.initWorld();
		
		// Erstellen der TaskSpezifikation
		TaskSpecVRLGLUE3 taskSpec = new TaskSpecVRLGLUE3();
		taskSpec.setEpisodic(); 								//episodisches Enviroment
		taskSpec.setDiscountFactor(1.0);						//Discountfaktor = 1 (also kein Discount)
		taskSpec.addDiscreteObservation(new IntRange(0, world.getCountStates())); 	//Anzahl der möglichen Zustände  (Anzahl der Streichhölzer)
		taskSpec.addDiscreteAction(new IntRange(1,3));			//Anzahl der möglichen Aktionen (Ziehe 1, 2 oder 3)
		taskSpec.setRewardRange(new DoubleRange(-1,1));			//Range der Belohnungen (Gewinnen oder Verlieren)
			
		String taskSpecString = taskSpec.toTaskSpec();			//TaskSpezifikation in String umwandeln
		TaskSpec.checkTaskSpec(taskSpecString);					//String prüfen
		
		return taskSpecString;
	}

	@Override
	public String env_message(String msg) {
		// Diese Methode reagiert auf Messages die eintreffen

		if(msg.equals("team name")){
			return TEAM_NAME;
		} else if(msg.equals("team member")){
			return TEAM_MEMBERS;
		} else if(msg.equals("training start")){
			world.setShowOutput(false);
			return "Start training: Output " + world.getShowOutput();
		} else if(msg.equals("training end")){
			world.setShowOutput(true);
			return "Training ended: Output " + world.getShowOutput();
		} else if(msg.equals("get stats")){
			return world.getStats();
		} else {
			return "Message not understood! You sent: " + msg; 
		}
	}

	@Override
	public Observation env_start() {

		Observation observation = new Observation(1, 0, 0); 	//Observation enthält einen Integer-Werte
		observation.setInt(0, world.getCountStates());			//erster Int-Wert = Anzahl Streichhölzer
		return observation;
	}

	@Override
	public Reward_observation_terminal env_step(Action action) {
		// Diese Methode nimmt eine Aktion entgegen und führt diese auf dem Enviroment aus
		// die daraus resultierende Zustandsveränderung und der Rewards, sowie die Rückmeldung ob
		// es sich um einen terminalen Zustand handelt, werden mit dem Observation-Objekt zurückgegeben
		
		// Prüfungen durchführen
        assert (action.getNumInts() == 1) : "Expecting a 1-dimensional integer action. " + action.getNumInts() + "D was provided";
        assert (action.getInt(0) >= 1) : "Action should be in [1,3], " + action.getInt(0) + " was provided";
        assert (action.getInt(0) < 4) : "Action should be in [1,3], " + action.getInt(0) + " was provided";
        
        // Aktion durchführen
        world.executeAction(action.getInt(0));

        // Observation erstellen
		Observation observation = new Observation(1, 0, 0); 	//Observation enthält drei Integer-Werte
		observation.setInt(0, world.getNumMatchsticks());		//erster Int-Wert = Summe der übrigen Streichhölzer
        
        // Terminal Oberservation erstellen
        Reward_observation_terminal rewardObservation = new Reward_observation_terminal();
        rewardObservation.setObservation(observation);			// Observation einfügen
        rewardObservation.setTerminal(world.isTerminal());		// Flag setzen ob Endzustand erreicht wurde
        rewardObservation.setReward(world.getReward());			// Reward setzen
		
		return rewardObservation;
	}
	
	
	// Starten des Enviroments
	public static void main(String[] args) {
		world = new WorldDescription();
        EnvironmentLoader envLoader = new EnvironmentLoader(new NimSpielEnvironment());
        envLoader.run();
	}

}


class WorldDescription{
	
	private int numMatchsticks = 21;
	private int currentNumMatchsticks;
	
	private int gameswon = 0;
	private int gameslost = 0;
	private int totalgames = 0;
	

	private boolean showOutput = true;
	private boolean playerwins = false;

	public void initWorld(){
		currentNumMatchsticks = numMatchsticks;
	}
	
	/**
	 * (De-)Activates output option
	 * @param flag
	 */
	public void setShowOutput(boolean flag){
		showOutput = flag;
	}
	
	/**
	 * Prints output information if activated.
	 * @return
	 */
	public boolean getShowOutput(){
		return showOutput;
	}

	/**
	 * Diese Methode gibt die Anzahl möglicher Zustände zurück
	 * @return
	 */
	public int getCountStates(){
		return numMatchsticks;
	}
	
	/**
	 * Methode gibt den aktuellen Zustand des Spiels zurück
	 * @return
	 */
	public int getNumMatchsticks(){
		return currentNumMatchsticks;
	}
	
	/**
	 * Executes Players actions and the performs Opponents action.<br>
	 * The dominant strategie is to always leave a multiple of 4 matchsticks.
	 * If Player plays this strategie, the opponent will always lose, because starting with 21 matchsticks, the first one to pick a matchstick wins, when 
	 * playing the dominant strategie.
	 * 
	 * @param action
	 */
	public void executeAction(int action){
		int oppenentsChoice = 1;
		int holz1;
		int holz2;

		currentNumMatchsticks = currentNumMatchsticks - action;
		holz1 = currentNumMatchsticks;
		
		if(currentNumMatchsticks == 0){
			playerwins = true;
			print("Player takes "+action+" and leafs "+holz1+" and wins, Opponent loses .");
		} else {
			oppenentsChoice = currentNumMatchsticks % 4;
			if(oppenentsChoice == 0){
				oppenentsChoice = 1; // Sollte eigentlich nicht passieren!
			}
			currentNumMatchsticks = currentNumMatchsticks-oppenentsChoice;
			holz2 = currentNumMatchsticks;
			
			print("Player takes "+action+" and leafs "+holz1+", Opponent takes "+oppenentsChoice+" and leaves "+holz2+" .");
		}
	}
	
	/**
	 * Is terminal when no matchsticks are left to take.
	 * @return
	 */
	public boolean isTerminal(){
		if(currentNumMatchsticks == 0){
			return true;
		}
		return false;
	}
	

	/**
	 * Returns 1 if player wins, -1 if oppenent wins
	 * @return
	 */
	public int getReward(){
		
		if(isTerminal()){	//Spiel ist zu Ende
			
			if(playerwins == true){
				gameswon +=1;
				totalgames = gameslost + gameswon;
				return 1;
			} else {		
				gameslost +=1;
				totalgames = gameslost + gameswon;
				return -1;
			} 
		} else {				//Spiel läuft noch
			return 0;
		}
	}
	
//	// Der Spieler muss ein Ass stets mit elf Punkten zählen, es sei denn, er würde auf diese Weise
//	// den Wert 21 überschreiten nur dann zählt er das Ass mit einem Punkt
//	private int getSum(ArrayList<Card> cards){
//		int size;
//		int sum = 0;
//		boolean ace = false;
//		
//		for (Card card : cards) {
//			size = card.getValue().length;	//Menge der Values bestimmen
//			sum += card.getValue()[size-1];	//größten Wert zum Aufsummieren verwenden
//			
//			if(!ace && card.isAce()){
//				ace = true;
//			}
//		}
//		
//		// Wenn ein Ass vorhanden ist und die Summe der Karten 21 übersteigt, wird der 
//		// Dealer das Ass als 1 bewerten (also -10)
//		return (sum > WIN_VALUE && ace) ? sum-10 : sum;
//	}
	
	public String getStats(){
		return "Totally played Games: " + totalgames + ". Won: " + gameswon + " Lost: " + gameslost;
	}
	
	private void print(String msg){
		if(showOutput){
			System.out.println(msg);
		}
	}
	
}
