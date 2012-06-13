package praktikum;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.rlcommunity.rlglue.codec.EnvironmentInterface;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpec;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpecVRLGLUE3;
import org.rlcommunity.rlglue.codec.taskspec.ranges.DoubleRange;
import org.rlcommunity.rlglue.codec.taskspec.ranges.IntRange;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;
import org.rlcommunity.rlglue.codec.types.Reward_observation_terminal;
import org.rlcommunity.rlglue.codec.util.EnvironmentLoader;

/**
 * Environment für ein 4-Gewinnt-Spiel mit 7 Spalten und 6 Reihen.
 * 
 * @author Milena Roetting
 *
 */
public class VierGewinntEnvironment implements EnvironmentInterface {
	private static final String TEAM_NAME = "Team 1";
	private static final String TEAM_MEMBERS = "Pascal Jager, Stefan Münchow, Armin Steudte, Milena Rötting, Sven-Andjes Pahl, Carsten Noetzel, Oliver Steenbuck";

	private VierGewinntDescription game;

	@Override
	public void env_cleanup() {	}

	@Override
	public String env_init() {
		// Diese Methode initialisiert die Environment
		game = new VierGewinntDescription();
		game.initGame(6, 7);

		TaskSpecVRLGLUE3 taskSpec = new TaskSpecVRLGLUE3();
		//episodisches Enviroment
		taskSpec.setEpisodic();
		//Discountfaktor = 1 (also kein Discount)
		taskSpec.setDiscountFactor(1.0);
		for (int i = 0; i < (game.getColumnSize() * game.getRowSize()); i++) {
			taskSpec.addDiscreteObservation(new IntRange(0, 2));
		}
		//Anzahl der möglichen Aktionen (soviele, wie es Spalten gibt, in die ein Token gelegt werden kann)
		taskSpec.addDiscreteAction(new IntRange(0,game.getColumnSize()-1));
		//Range der Belohnungen (-1 für Verlieren, 1 für Gewinnen)
		taskSpec.setRewardRange(new DoubleRange(-1,1));

		String taskSpecString = taskSpec.toTaskSpec();
		TaskSpec.checkTaskSpec(taskSpecString);

		return taskSpecString;
	}

	@Override
	public String env_message(String msg) {
		if (msg.equals("team name")){
			return TEAM_NAME;
		} else if (msg.equals("team members")){
			return TEAM_MEMBERS;
		} else if (msg.equals("training start")) {
			return "Start training";
		} else if (msg.equals("training end")) {
			return "Training ended";
		} else if(msg.equals("get stats")) {
			return game.getStats();
		} else {
			return "Unknown message: " + msg;
		}
	}

	@Override
	public Observation env_start() {
		// Diese Methode setzt die Environment zurück und gibt den Startzustand zurück
		game.resetGame();

		State startState = game.getCurrentState();
		Observation observation = calcObservation(startState);

		return observation;
	}

	private Observation calcObservation(State state) {
		Observation observation = new Observation((game.getColumnSize() * game.getRowSize()),0,0);
		int i = 0;
		for (int column = 0; column < game.getColumnSize(); column++) {
			for (int row = 0; row < game.getRowSize(); row++) {
				observation.setInt(i, state.getField().get(column).get(row)); // Belegung des aktuellen Feldes
				i++;
			}
		}
		return observation;
	}

	@Override
	public Reward_observation_terminal env_step(Action action) {
		assert (action.getNumInts() == 1) : "Expecting a 1-dimensional integer action. " + action.getNumInts() + "D was provided";
		assert ((action.getInt(0) >= 0) || (action.getInt(0) < game.getColumnSize()-1)) :
			"Action should be in [0," + (game.getColumnSize()-1) + "], " + action.getInt(0) + " was provided";

		game.executeAction(action.getInt(0));

		// Observation erstellen
		State state = game.getCurrentState();
		Observation observation = calcObservation(state);

		// Terminal Oberservation erstellen
		Reward_observation_terminal rewardObservation = new Reward_observation_terminal();
		rewardObservation.setObservation(observation);			// Observation einfügen
		rewardObservation.setTerminal(game.isTerminal());		// Flag setzen ob Endzustand erreicht wurde
		rewardObservation.setReward(game.getReward());			// Reward setzen

		return rewardObservation;
	}

	// Starten des Enviroments
	public static void main(String[] args) {
		EnvironmentLoader envLoader = new EnvironmentLoader(new VierGewinntEnvironment());
		envLoader.run();
	}
}

class VierGewinntDescription {
	private int gameswon;
	private int gameslost;
	private int gamesdrawn;

	private int rows;
	private int columns;

	private State currentState;
	private boolean isTerminal;
	private int winner;

	/**
	 * Reine Initialisierung, die nur einmal geschehen soll.
	 * @param rows
	 * @param columns
	 */
	public void initGame(int rows, int columns) {
		gameswon = 0;
		gameslost = 0;
		gamesdrawn = 0;
		this.rows = rows;
		this.columns = columns;
	}

	/**
	 * Zurück setzen der Environment am Beginn einer Episode.
	 */
	public void resetGame() {
		currentState = new State(rows, columns);
		isTerminal = false;
		winner = 0;
	}

	/**
	 * Agent und Computer ziehen je einmal. Nach jedem Zug wird auf den Gewinn geprüft und die Methode ggf. abgebrochen.
	 * 
	 * Die Spalte des Agenten wird übergeben, der Computer setzt zufällig eine Spalte.
	 * 
	 * @param actionInt Spaltenauswahl des Agenten
	 */
	public void executeAction(int actionInt) {
		// Agent setzen und auf Gewinn prüfen
		int agentRow = setPlace(actionInt, 1);
		checkGameOver(actionInt, agentRow,1);

		if(!isTerminal()) {
			// Gegenspieler setzt in eine zufällige Reihe
			int computerActionInt = new Random().nextInt(columns);
			int computerRow = setPlace(computerActionInt, 2);
			checkGameOver(computerActionInt, computerRow, 2);
		}
	}

	/**
	 * Setzt den gegebenen Token (Farbe) in die gegebene Spalte. Wenn diese schon voll ist, wird einfach nichts gemacht.
	 * Die entsprechende Reihenanzahl, in die das Token letztlich "gefallen" ist, wird zurück gegeben. 0 wenn die Spalte schon voll war.
	 * 
	 * @param columnInt
	 * @param playerToken
	 * @return
	 */
	private int setPlace(int columnInt, int playerToken) {
		if(!currentState.isColumnFull(columnInt)){
			// oberstes leeres Feld finden
			List<Integer> column = currentState.getField().get(columnInt);
			int place = 0;
			while(column.get(place) != 0){
				place++;
			}
			// oberstes leeres Feld setzen
			currentState.getField().get(columnInt).set(place, playerToken);
			return place;
		}
		return 0;
	}

	/**
	 * Berechnet den Reward eines Steps. Wenn das Spiel beendet ist, gibt es +1 für gewonnen und -1 für verloren oder unentschieden, sonst 0.
	 * @return
	 */
	public double getReward() {
		if(isTerminal()){
			if(winner == 1){
				gameswon++;
				return 1.0;
			} else if(winner == 2) {
				gameslost++;
				return -1.0;
			} else {
				gamesdrawn++;
				return -1.0;
			}
		} else {
			return 0.0;
		}
	}


	/**
	 * Prüft, ob das Spiel vorbei ist und setzt ggf. entsprechende Flags und Felder.
	 * 
	 * Das Spiel ist vorbei und für eine Partie gewonnen, wenn es diagonal, horizontal oder vertikal vier Token in einer Reihe gibt.
	 * Das Spiel ist vorbei und unentschieden, wenn alle Felder befüllt sind und es keine 4er-Reihen gibt.
	 * 
	 * @param column
	 * @param row
	 * @param zeichen
	 */
	private void checkGameOver(int column, int row, int zeichen){
		if(viererHorizontal(column, row, zeichen) ||
				viererVertikal(column, row, zeichen) ||
				viererDiagonal1(column, row, zeichen) ||
				viererDiagonal2(column, row, zeichen)){
			isTerminal = true;
			winner = zeichen;
		} else {
			boolean full = true;
			for(int col = 0; col<columns; col++){
				if(!currentState.isColumnFull(col)){
					full = false;
				}
			}
			if(full){
				isTerminal=true;
				winner = 0;
			}
		}

	}

	/**
	 * Vertikale Prüfung auf eine 4er-Reihe.
	 * 
	 * Suche ober- und unterhalb des aktuell gesetzten Zeichens.
	 * @param column
	 * @param row
	 * @param zeichen
	 * @return
	 */
	private boolean viererVertikal(int column, int row, int zeichen) {
		// nach unten
		int goDown = row - 1; // mit dem Punkt unter dem gesetzten beginnen
		int treffer = 1; // der gesetzte Punkt = 1 Treffer
		while (goDown >= 0) {
			if (currentState.getField().get(column).get(goDown) != zeichen) {
				break;
			}
			goDown--;
			treffer++;
		}

		// nach oben
		int goUp = row + 1;
		while (goUp < currentState.getField().get(0).size()) {
			if (currentState.getField().get(column).get(goUp) != zeichen) {
				break;
			}
			goUp++;
			treffer++;
		}

		return (treffer > 3);
	}

	/**
	 * Horizontale Prüfung auf eine 4er-Reihe.
	 * 
	 * Suche links und rechts des aktuell gesetzten Zeichens.
	 * @param column
	 * @param row
	 * @param zeichen
	 * @return
	 */
	private boolean viererHorizontal(int column, int row, int zeichen) {
		// nach links
		int goLeft = column - 1;
		int treffer = 1;
		while (goLeft >= 0) {
			if (currentState.getField().get(goLeft).get(row) != zeichen) {
				break;
			}
			goLeft--;
			treffer++;
		}

		// nach rechts
		int goRight = column + 1;
		while (goRight < currentState.getField().size()) {
			if (currentState.getField().get(goRight).get(row) != zeichen) {
				break;
			}
			goRight++;
			treffer++;
		}

		return (treffer > 3);
	}

	/**
	 * Diagonale Prüfung auf eine 4er-Reihe.
	 * 
	 * Suche links unterhalb und rechts oberhalb des aktuell gesuchten Zeichens.
	 * @param column
	 * @param row
	 * @param zeichen
	 * @return
	 */
	private boolean viererDiagonal1(int column, int row, int zeichen) {
		// nach links unten
		int goRowLeft = row - 1;
		int goColDown = column - 1;
		int treffer = 1;
		while (goRowLeft >= 0 && goColDown >= 0) {
			if (currentState.getField().get(goColDown).get(goRowLeft) != zeichen) {
				break;
			}
			goRowLeft--;
			goColDown--;
			treffer++;
		}

		// nach rechts oben
		int goRowRight = row + 1;
		int goColUp = column + 1;
		while (goRowRight < currentState.getField().get(0).size() && goColUp < currentState.getField().size()) {
			if (currentState.getField().get(goColUp).get(goRowRight) != zeichen) {
				break;
			}
			goRowRight++;
			goColUp++;
			treffer++;
		}

		return (treffer > 3);
	}

	/**
	 * Diagonale Prüfung auf eine 4er-Reihe.
	 * 
	 * Suche links oberhalb und rechts unterhalb des aktuell gesuchten Zeichens.
	 * @param column
	 * @param row
	 * @param zeichen
	 * @return
	 */
	private boolean viererDiagonal2(int column, int row, int zeichen) {
		// nach links oben
		int goRowLeft = row - 1;
		int goColUp = column + 1;
		int treffer = 1;
		while (goRowLeft >= 0 && goColUp < currentState.getField().size()) {
			if (currentState.getField().get(goColUp).get(goRowLeft) == zeichen) {
				break;
			}
			goRowLeft--;
			goColUp++;
			treffer++;
		}

		// nach rechts unten
		int goRowRight = row + 1;
		int goColDown = column - 1;
		while (goRowRight < currentState.getField().get(0).size() && goColDown >= 0) {
			if (currentState.getField().get(goColDown).get(goRowRight) != zeichen) {
				break;
			}
			goRowRight++;
			goColDown--;
			treffer++;
		}

		return (treffer > 3);
	}


	public String getStats(){
		return "Totally played Games: " + (gameswon + gameslost + gamesdrawn) + ". Won: " + gameswon + " Lost: " + gameslost + " Drawn: " + gamesdrawn;
	}

	public int getRowSize(){
		return rows;
	}

	public int getColumnSize(){
		return columns;
	}

	public State getCurrentState() {
		return currentState;
	}

	public boolean isTerminal() {
		return isTerminal;
	}
}

class State {
	private final int rows;
	private final int columns;
	private final List<List<Integer>> field;

	public State(int rows, int columns){
		this.rows = rows;
		this.columns = columns;
		this.field = initField();
	}

	private List<List<Integer>> initField() {
		List<List<Integer>> newStateFields = new ArrayList<List<Integer>>();
		for (int column = 0; column < columns; column++) {
			List<Integer> rowFields = new ArrayList<Integer>();
			for (int row = 0; row < rows; row++) {
				rowFields.add(row, 0);
			}
			newStateFields.add(rowFields);
		}
		return newStateFields;
	}

	public boolean isColumnFull(int columnInt){
		if(field.get(columnInt).get(rows-1) != 0) {
			return true;
		}
		return false;
	}

	public List<List<Integer>> getField(){
		return field;
	}
}