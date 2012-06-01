package praktikum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Stack;

import org.rlcommunity.rlglue.codec.EnvironmentInterface;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpec;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpecVRLGLUE3;
import org.rlcommunity.rlglue.codec.taskspec.ranges.DoubleRange;
import org.rlcommunity.rlglue.codec.taskspec.ranges.IntRange;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;
import org.rlcommunity.rlglue.codec.types.Reward_observation_terminal;
import org.rlcommunity.rlglue.codec.util.EnvironmentLoader;

public class BlackJackEnvironment implements EnvironmentInterface {

	private static final String TEAM_NAME ="Team 1";
	private static final String TEAM_MEMBERS ="Carsten Noetzel";
	
	private static TableDescription table;
	
	@Override
	public void env_cleanup() {
	}

	@Override
	public String env_init() {
		// Diese Methode initialisiert das Enviroment, in disem Fall den 
		// Spieltisch an dem BlackJack gespielt wird
		table.initTable();
		
		// Erstellen der TaskSpezifikation
		TaskSpecVRLGLUE3 taskSpec = new TaskSpecVRLGLUE3();
		taskSpec.setEpisodic(); 														//episodisches Enviroment
		taskSpec.setDiscountFactor(1.0);												//Discountfaktor = 1 (also kein Discount)
		taskSpec.addDiscreteObservation(new IntRange(4, 30)); 	//Anzahl der möglichen Zustände  (Summe der Spielerkarten)
		taskSpec.addDiscreteObservation(new IntRange(0, 1)); 	//Anzahl der möglichen Zustände  (hat der Spieler ein nutzbares Ass)
		taskSpec.addDiscreteObservation(new IntRange(2, 11)); 	//Anzahl der möglichen Zustände  (sichtbare Karte des Dealers)
		taskSpec.addDiscreteAction(new IntRange(0,1)); 									//Anzahl der möglichen Aktionen (hit und stick)
		taskSpec.setRewardRange(new DoubleRange(-1,1));									//Range der Belohnungen
		
		String taskSpecString = taskSpec.toTaskSpec();		//TaskSpezifikation in String umwandeln
		TaskSpec.checkTaskSpec(taskSpecString);				//String prüfen
		
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
			table.setShowOutput(false);
			return "Start training: Output " + table.getShowOutput();
		} else if(msg.equals("training end")){
			table.setShowOutput(true);
			return "Training ended: Output " + table.getShowOutput();
		} else if(msg.equals("get stats")){
			return table.getStats();
		} else {
			return "Message not understood! You sent: " + msg; 
		}
	}

	@Override
	public Observation env_start() {
		// Diese Methode startet eine neue Episode, dazu müssen zunächst die Karten gemischt werden
		// und dem Dealer sowie dem Spieler je zwei Karten hingelegt werden, wovon eine Karte des Dealers
		// umgedreht wird
		
		table.shuffleCards();
		table.dealCards();
		
		Observation observation = new Observation(3, 0, 0); 	//Observation enthält drei Integer-Werte
		observation.setInt(0, table.getSumPlayerCards());		//erster Int-Wert = Summe der Spielerkarten
		observation.setInt(1, table.hasPlayerUsableAce());		//zweiter Int-Wert = hat der Spieler ein nutzbares Ass
		observation.setInt(2, table.getDealersVisibleCard());	//erster Int-Wert = sichtbare Karte des Dealers
		return observation;
	}

	@Override
	public Reward_observation_terminal env_step(Action action) {
		// Diese Methode nimmt eine Aktion entgegen und führt diese auf dem Enviroment aus
		// die daraus resultierende Zustandsveränderung und der Rewards, sowie die Rückmeldung ob
		// es sich um einen terminalen Zustand handelt, werden mit dem Observation-Objekt zurückgegeben
		
		// Prüfungen durchführen
        assert (action.getNumInts() == 1) : "Expecting a 1-dimensional integer action. " + action.getNumInts() + "D was provided";
        assert (action.getInt(0) >= 0) : "Action should be in [0,1], " + action.getInt(0) + " was provided";
        assert (action.getInt(0) < 2) : "Action should be in [0,1], " + action.getInt(0) + " was provided";
        
        // Aktion durchführen
        table.executeAction(action.getInt(0));

        // Observation erstellen
		Observation observation = new Observation(3, 0, 0); 	//Observation enthält drei Integer-Werte
		observation.setInt(0, table.getSumPlayerCards());		//erster Int-Wert = Summe der Spielerkarten
		observation.setInt(1, table.hasPlayerUsableAce());		//zweiter Int-Wert = hat der Spieler ein nutzbares Ass
		observation.setInt(2, table.getDealersVisibleCard());	//erster Int-Wert = sichtbare Karte des Dealers
        
        // Terminal Oberservation erstellen
        Reward_observation_terminal rewardObservation = new Reward_observation_terminal();
        rewardObservation.setObservation(observation);			// Observation einfügen
        rewardObservation.setTerminal(table.isTerminal());		// Flag setzen ob Endzustand erreicht wurde
        rewardObservation.setReward(table.getReward());			// Reward setzen
		
		return rewardObservation;
	}
	
	
	// Starten des Enviroments
	public static void main(String[] args) {
		table = new TableDescription();
        EnvironmentLoader envLoader = new EnvironmentLoader(new BlackJackEnvironment());
        envLoader.run();
	}

}


class TableDescription{
	
	private static final int WIN_VALUE = 21;
	private boolean showOutput = true;
	
	Stack<Card> carddeck;
	
	ArrayList<Card> dealerCards;
	ArrayList<Card> playerCards;
	
	ArrayList<Integer> winValues;
	ArrayList<Integer> looseValues;
	ArrayList<Integer> drawnValues;
	
	boolean playerSticks;
	
	public TableDescription(){
		winValues = new ArrayList<Integer>();
		looseValues = new ArrayList<Integer>();
		drawnValues = new ArrayList<Integer>();		
	}
	
	//Tisch initialisieren
	public void initTable(){
		carddeck = new Stack<Card>();
		dealerCards = new ArrayList<Card>();
		playerCards = new ArrayList<Card>();
		playerSticks = false;
		
		initDeck();
	}
	
	//Setter Output Flag
	public void setShowOutput(boolean flag){
		showOutput = flag;
	}
	
	//Getter Output Flag
	public boolean getShowOutput(){
		return showOutput;
	}
	
	// Diese Methode initialisiert das Kartendeck, es wird 52 Karten gespielt 
	private void initDeck(){
		// alle Karten kommen viermal mit je unterschiedlichem Symbol im Stapel vor (52 Karten)
		Symbol symbol; 
		
		for (int i = 0; i < Symbol.values().length; i++) {
			symbol = Symbol.values()[i]; 
			carddeck.add(new Card("Ace", symbol, new int[]{1,11}));
			carddeck.add(new Card("King", symbol, new int[]{10}));
			carddeck.add(new Card("Queen", symbol, new int[]{10}));
			carddeck.add(new Card("Jack", symbol, new int[]{10}));
			carddeck.add(new Card("Ten", symbol, new int[]{10}));
			carddeck.add(new Card("Nine", symbol, new int[]{9}));
			carddeck.add(new Card("Eight", symbol, new int[]{8}));
			carddeck.add(new Card("Seven", symbol, new int[]{7}));
			carddeck.add(new Card("Six", symbol, new int[]{6}));
			carddeck.add(new Card("Five", symbol, new int[]{5}));
			carddeck.add(new Card("Four", symbol, new int[]{4}));
			carddeck.add(new Card("Three", symbol, new int[]{3}));
			carddeck.add(new Card("Two", symbol, new int[]{2}));
		}
	}
	
	// Diese Methode gibt die Anzahl möglicher Zustände zurück
	public int getCountStates(){
		return 200;
	}
	
	// Methode gibt den aktuellen Zustand des Spiels zurück
	public int getSumPlayerCards(){
		return getSum(playerCards);
	}
	
	// Methode zum Mischen des Kartendecks
	public void shuffleCards(){
		Collections.shuffle(carddeck);
		print("Cards shuffled.");
	}
	
	// Zu Beginn erhalten jeweils der Spieler und der Dealer zwei Karten
	public void dealCards(){
		playerCards.add(carddeck.pop());
		dealerCards.add(carddeck.pop());
		
		playerCards.add(carddeck.pop());
		dealerCards.add(carddeck.pop());
		print("Cards dealt.");
		printStatus();
	}
	
	public void executeAction(int action){
		switch (action) {
		case 0:					//hit = Spieler erhält eine Karte vom Stapel
			print("Player hits.");
			hit(playerCards);
			break;
		case 1:					//stick = keiner Karte mehr ziehen Dealer ist am Zug
			print("Player sticks. Dealers turn.");
			doDealerTurns();
			playerSticks = true;
			break;
		}
		printStatus();
	}
	
	// Ist der Dealer am Zug, zieht dieser solange er unter 17 Punkten ist eine Karte,
	// sobald der 17 Punkte erreicht hat hört er auf Karten zu ziehen
	private void doDealerTurns(){
		while(getSum(dealerCards) < 17){
			print("Dealer hits.");
			hit(dealerCards);
		}
	}
	
	// Diese Methode prüft ob der Endzustand erreicht wurde, diest der Fall wenn entweder der Spieler
	// den WIN_Value erreicht oder überschritten hat oder er keine Karten mehr nehmen will
	public boolean isTerminal(){
		return (getSum(playerCards) >= WIN_VALUE || playerSticks) ? true : false;
	}
	
	// Diese Methode bestimmt den Reward den der Spieler erhält
	// -1: Spieler hat verloren (Spielersumme > 21 oder (Spielersumme < Dealersumme && Dealersumme <= 21))
	//  0: Unentschieden (Spielersumme = Dealersumme) oder Spiel noch nicht zu Ende
	// +1: Spieler hat gewonnen 
	public int getReward(){
		int playerSum = getSum(playerCards);
		int dealerSum = getSum(dealerCards);
		
		if(isTerminal()){	//Spiel ist zu Ende
			if(playerSum==dealerSum){		//Unentschieden
				print("Drawn: Reward = 0");
				drawnValues.add(playerSum);
				return 0;
			} else if(playerSum > WIN_VALUE || (playerSum < dealerSum && dealerSum <= WIN_VALUE)) { 	//Spieler hat verloren
				print("Player lost: Reward = -1");
				looseValues.add(playerSum);
				return -1;
			} else {		//Spieler hat gewonnen
				print("Player won: Reward = 1");
				winValues.add(playerSum);
				return 1;
			} 
		}else{				//Spiel läuft noch
			return 0;
		}
	}
	
	// Bei hit wird der übergebenen Collection eine Karte hinzugefügt
	public void hit(ArrayList<Card> cards){
		cards.add(carddeck.pop());
	}
	
	// Holt den Wert der aufgedeckten Karte des Dealers
	public int getDealersVisibleCard(){
		int size = dealerCards.get(0).getValue().length;
		return dealerCards.get(0).getValue()[size-1];
	}
	
	// Gibt einen Integer Wert zurück der angibt ob der Spieler ein nutzbares Ass
	// auf der Hand hat oder nicht 1: true 0: false
	public int hasPlayerUsableAce(){
		return hasUsableAce(playerCards) ? 1 : 0; 
	}
	
	// Der Spieler muss ein Ass stets mit elf Punkten zählen, es sei denn, er würde auf diese Weise
	// den Wert 21 überschreiten nur dann zählt er das Ass mit einem Punkt
	private int getSum(ArrayList<Card> cards){
		int size;
		int sum = 0;
		boolean ace = false;
		
		for (Card card : cards) {
			size = card.getValue().length;	//Menge der Values bestimmen
			sum += card.getValue()[size-1];	//größten Wert zum Aufsummieren verwenden
			
			if(!ace && card.isAce()){
				ace = true;
			}
		}
		
		// Wenn ein Ass vorhanden ist und die Summe der Karten 21 übersteigt, wird der 
		// Dealer das Ass als 1 bewerten (also -10)
		return (sum > WIN_VALUE && ace) ? sum-10 : sum;
	}
	
	// Diese Methode gibt zurück ob der entsprechende Spieler eine nutzbares Ass auf der Hand hat
	// ein Ass ist dabei nutzbar, wenn das Ass als 11 gezählt werden kann, ohne dass die Summe von
	// 21 dabei überstiegen wird
	private boolean hasUsableAce(ArrayList<Card> cards){
		int size;
		int sum = 0;
		boolean ace = false;
		
		for (Card card : cards) {
			size = card.getValue().length;	//Menge der Values bestimmen
			sum += card.getValue()[size-1];	//größten Wert zum Aufsummieren verwenden
			
			if(!ace && card.isAce()){
				ace = true;
			}
		}
		
		// Wenn die Summe von 21 nicht überschritten wird und ein Ass vorhanden ist, wird dieses als useable bezeichnet
		return (sum <= WIN_VALUE && ace) ? true : false;
		
	}
	
	private void printStatus(){
		StringBuilder sb = new StringBuilder();
		
		sb.append("Player has: ");
		for (Card card : playerCards) {
			int size = card.getValue().length;
			sb.append(card.getValue()[size-1]);
			sb.append(" ");
		}
		sb.append("Sum: ");
		sb.append(getSum(playerCards));
		
		sb.append(" Dealer has: ");
		for (Card card : dealerCards) {
			int size = card.getValue().length;
			sb.append(card.getValue()[size-1]);
			sb.append(" ");
		}
		sb.append("Sum: ");
		sb.append(getSum(dealerCards));
		
		print(sb.toString());
	}
	
	public String getStats(){
		int averageWinValue = 0;
		int averageLooseValue = 0;
		int averageDrawnValue = 0;
		
		int gamesWon = (winValues.size() > 0) ? winValues.size() : 1;
		int gamesLost = (looseValues.size()> 0) ? looseValues.size() : 1;
		int gamesDrawn = (drawnValues.size()> 0) ? drawnValues.size() : 1;
		
		int totalGames = gamesWon + gamesLost + gamesDrawn;
		
		//Mittelwert gewonnen Spiele
		int sumWon = 0;
		for (Integer win : winValues) {
			sumWon += win;
		}
		averageWinValue = sumWon / gamesWon;
		
		//Mittelwert verlorene Spiele
		int sumLost = 0;
		for (Integer loose : looseValues) {
			sumLost += loose;
		}
		averageLooseValue = sumLost / gamesLost;
		
		//Mittelwert unentschiedene Spiele
		int sumDrawn = 0;
		for (Integer drawn : drawnValues) {
			sumDrawn += drawn;
		}
		averageDrawnValue = sumDrawn / gamesDrawn;
		
		return "Totally played Games: " + totalGames + ". Won: " + gamesWon + " (" + averageWinValue +  ")" + " Lost: " + gamesLost + " (" + averageLooseValue +  ")" + " Drawn: " + gamesDrawn + " (" + averageDrawnValue +  ")";
	}
	
	private void print(String msg){
		if(showOutput){
			System.out.println(msg);
		}
	}
	
}

enum Symbol{HEARTS, SPADES, DIAMONDS, CLUBS}

class Card{
	
	String description;
	Symbol symbol;
	int[] value;
	
	public Card(String des, Symbol sym, int[] val){
		this.description = des;
		this.symbol = sym;
		this.value = val;
	}
	
	public int[] getValue(){
		return value;
	}
	
	public boolean isAce(){
		return description.equals("Ace");
	}
}
