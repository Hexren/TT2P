package praktikum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.rlcommunity.rlglue.codec.AgentInterface;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpec;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;
import org.rlcommunity.rlglue.codec.util.AgentLoader;

public class SarsaAgent implements AgentInterface {
	private static final String TEAM_NAME ="Team 1";
	private static final String TEAM_MEMBERS ="Oliver Steenbuck, Svend-Anjes Pahl, Stefan Muenchow, Milena Roetting, Armin Steudte, Carsten Noetzel, Pascal Jaeger";

	private boolean showOutput;

	private final Random randGenerator = new Random();
	private ArrayList<Integer> lastAction;
	private ArrayList<Integer> lastObservation;

	private ArrayList<Integer[]> numActions;

	private double gamma = 0.0;
	private final double epsilon = 0.1;
	private final double alpha = 0.1;

	private HashMap<StateActionPair, Double> actionValueFunction;

	public SarsaAgent(){
		super();

		showOutput = false;
		// Action-Value-Function initialisieren n-dimensionales Array Q(s,a)
		actionValueFunction = new HashMap<StateActionPair, Double>();
	}

	@Override
	public void agent_cleanup() {
		// alles löschen
		lastAction = null;
		lastObservation = null;
		actionValueFunction = null;
	}

	@Override
	public void agent_end(double reward) {
		// Die Episode wurde beendet, vom letzten Reward muss noch einmal gerlernt werden

		Double Q_sa = actionValueFunction.get(new StateActionPair(lastObservation, lastAction)); // Q(s,a) bestimmen
		Q_sa = (Q_sa == null) ? 0 : Q_sa;		// Q_sa könnte null sein wenn es noch nicht berechnet wurde
		Double new_Q_sa = Q_sa + alpha * (reward - Q_sa);

		// mit berechneten Wert ActionValueFunction aktualisieren
		actionValueFunction.put(new StateActionPair(lastObservation, lastAction), new_Q_sa);

		lastObservation = null;
		lastAction = null;
	}

	@Override
	public void agent_init(String taskSpecification) {
		// Diese Methode initialisiert den Agenten
		// übergebenen String in TaskSpecification umwandeln
		TaskSpec taskSpec = new TaskSpec(taskSpecification);

		// prüfen auf diskreten Zustandsraum und Aktionen
		assert (taskSpec.getNumDiscreteObsDims() >= 1);
		assert (taskSpec.getNumContinuousObsDims() == 0);		//keine continuous Observations
		assert (taskSpec.getNumDiscreteActionDims() >= 1);
		assert (taskSpec.getNumContinuousActionDims() == 0);	//keine continuous Actions

		// Dimensionen der Aktionen bestimmen
		int actionDimensions = taskSpec.getNumDiscreteActionDims();

		// Anzahl der möglichen Aktionen speichern für jede Dimension wird in der ArrayList ein neues Integer-Array mit oberer und unterer Grenze angelegt
		numActions = new ArrayList<Integer[]>();
		for (int i = 0; i < actionDimensions; i++) {
			Integer[] range = new Integer[]{taskSpec.getDiscreteActionRange(i).getMin(), taskSpec.getDiscreteActionRange(i).getMax()};
			numActions.add(range);
		}

		// Discountfaktor bestimmen
		gamma = taskSpec.getDiscountFactor();
	}

	@Override
	public String agent_message(String msg) {
		// Diese Methode reagiert auf Messages die eintreffen

		if(msg.equals("team name")){
			return TEAM_NAME;
		} else if(msg.equals("team members")){
			return TEAM_MEMBERS;
		} else if(msg.equals("training start")){
			showOutput = false;
			return "SarsaAgent Start training: Output " + showOutput;
		} else if(msg.equals("training end")){
			showOutput = true;
			return "SarsaAgent Training ended: Output " + showOutput;
		} else if(msg.equals("get stats")){
			StringBuilder sb = new StringBuilder();
			for (StateActionPair saPair : actionValueFunction.keySet()) {
				sb.append(saPair.toString());
				sb.append(" Value: " + actionValueFunction.get(saPair));
				sb.append("\n");
			}
			return sb.toString();
		} else {
			return "Message not understood! You sent: " + msg;
		}
	}

	@Override
	public Action agent_start(Observation observation) {
		// Diese Methode startet den Agenten

		// Zustandsinformationen dürfen nur durch Integer-Werte beschrieben werden
		// die Dimensionen sind dabei frei wählbar
		assert (observation.getNumInts()>=1);
		assert (observation.getNumDoubles()==0);
		assert (observation.getNumChars()==0);

		// Zustand für aktuelle Beobachtung erstellen
		ArrayList<Integer> actualObservation = new ArrayList<Integer>();
		for (int i = 0; i < observation.getNumInts(); i++) {
			actualObservation.add(i, observation.getInt(i));
		}

		ArrayList<Integer> actualAction = eGreedy(actualObservation); 	//neue Aktion auf Basis der aktuellen Beobachtung wählen

		Action returnAction = new Action(actualAction.size(), 0, 0);
		int[] actions = new int[actualAction.size()];					//Arraylist in Integer umwandeln
		for (int i = 0; i < actions.length; i++) {
			actions[i] = actualAction.get(i);
		}
		returnAction.intArray= actions;

		lastAction = actualAction;			//aktuelle Aktion sichern
		saveObservation(actualObservation); //aktuelle Beobachtung sichern

		return returnAction;
	}

	@Override
	public Action agent_step(double reward, Observation observation) {
		//Diese Methode führt einen Step im Agenten durch

		// Zustandsinformationen dürfen nur durch Integer-Werte beschrieben werden
		// die Dimensionen sind dabei frei wählbar
		assert (observation.getNumInts()>=1);
		assert (observation.getNumDoubles()==0);
		assert (observation.getNumChars()==0);

		// Zustand für aktuelle Beobachtung erstellen
		ArrayList<Integer> actualObservation = new ArrayList<Integer>();
		for (int i = 0; i < observation.getNumInts(); i++) {
			actualObservation.add(i, observation.getInt(i));
		}

		ArrayList<Integer> actualAction = eGreedy(actualObservation); 	//neue Aktion auf Basis der aktuellen Beobachtung wählen

		Double Q_sa = actionValueFunction.get(new StateActionPair(lastObservation, lastAction));				// Q(s,a) bestimmen
		Q_sa = (Q_sa == null) ? 0 : Q_sa;							// Q_sa könnte null sein wenn es noch nicht berechnet wurde
		Double Q_sprime_aprime = actionValueFunction.get(new StateActionPair(actualObservation, actualAction)); // Q(s',a') bestimmen
		Q_sprime_aprime = (Q_sprime_aprime == null) ? 0 : Q_sa;		// Q_sprime_aprime könnte null sein wenn es noch nicht berechnet wurde

		// neuen Wert für Q(s,a) berechnen
		double new_Q_sa = Q_sa + alpha * (reward + gamma * Q_sprime_aprime - Q_sa);

		// mit berechneten Wert ActionValueFunction aktualisieren
		actionValueFunction.put(new StateActionPair(lastObservation, lastAction), new_Q_sa);

		Action returnAction = new Action(actualAction.size(), 0, 0);
		int[] actions = new int[actualAction.size()];					//Arraylist in Integer umwandeln
		for (int i = 0; i < actions.length; i++) {
			actions[i] = actualAction.get(i);
		}
		returnAction.intArray= actions;

		lastAction = actualAction;			//aktuelle Aktion sichern
		saveObservation(actualObservation); //aktuelle Beobachtung sichern

		return returnAction;
	}

	private ArrayList<Integer> eGreedy(ArrayList<Integer> state){
		//aus jeder Dimension eine zufällige Aktionen bestimmen und diese in der ArrayList speichern
		ArrayList<Integer> chosenActions = new ArrayList<Integer>();

		int minValue = 0;
		int maxValue = 0;
		int rndAction = 0;

		// Zufallszahl generieren und prüfen ob zufällig eine andere
		// Aktion ausgewählt wird als die optimale
		if(randGenerator.nextDouble() <= epsilon){
			print("Choosing random action");
			for (int i = 0; i < numActions.size(); i++) {
				minValue = numActions.get(i)[0];											//untere Grenze der Dimension bestimmen
				maxValue = numActions.get(i)[1];											//obere Grenze der Dimension bestimmen
				rndAction = minValue + (int)(Math.random() * ((maxValue - minValue) + 1));	//zufällige Aktion zwischen Min und Max der Dimension
				chosenActions.add(i, rndAction);
			}
			return chosenActions;
		} else {
			print("Choosing action with max estimated return");
			ArrayList<Integer> minValues = new ArrayList<Integer>();
			ArrayList<Integer> maxValues = new ArrayList<Integer>();
			for (int i = 0; i < numActions.size(); i++) {
				minValues.add(i, numActions.get(i)[0]);					//für jede Dimension kleinste Aktion auswählen
				maxValues.add(i, numActions.get(i)[1]);					//für jede Dimension größte Aktion auswählen
			}

			chosenActions = minValues;
			ArrayList< ArrayList<Integer>> permutations = getPermutations(minValues, maxValues);

			for (ArrayList<Integer> actions : permutations) {
				Double val1 = actionValueFunction.get(new StateActionPair(state, actions));
				Double val2 = actionValueFunction.get(new StateActionPair(state, chosenActions));
				if (val1 != null && val2 != null && val1 > val2 ){
					chosenActions = actions;
				}
			}
			return chosenActions;
		}
	}

	private void print(String msg){
		if(showOutput){
			System.out.println(msg);
		}
	}

	public static void main(String[] args) {
		AgentLoader theLoader = new AgentLoader(new SarsaAgent());
		theLoader.run();

		//    	SarsaAgent x = new SarsaAgent();
		//
		//    	ArrayList<Integer> min = new ArrayList<Integer>();
		//    	min.add(0);
		//    	min.add(1);
		//
		//    	ArrayList<Integer> max = new ArrayList<Integer>();
		//    	max.add(1);
		//    	max.add(3);
		//
		//
		//    	ArrayList<ArrayList<Integer>> test = x.getPermutations(min, max);
		//
		//    	for (ArrayList<Integer> list : test) {
		//			System.out.println(list.toString());
		//		}

	}

	private void saveObservation(ArrayList<Integer> observation){
		lastObservation = new ArrayList<Integer>();
		for (Integer integer : observation) {
			lastObservation.add(integer);
		}
	}


	/**
	 * Diese Methode dient zum Herstellen alle möglichen Permutationen von Aktionen die geprüft werden müssen, wenn es mehrdimensionale
	 * Aktionen gibt. Liegt zum Beispiel eine 2-dimensionale Aktion vor mit den Grenzen [0,1] und [1,3] müssen alle möglichen Kombinationen
	 * ([0,1][0,2][0,3][1,1][1,2][1,3]) geprüft werden.
	 * @param minValues - kleinste Werte je Dimension [0,1]
	 * @param maxValues - größte Werte je Dimension [1,3]
	 * @return Permutationen
	 */
	private ArrayList<ArrayList<Integer>> getPermutations(ArrayList<Integer> minValues, ArrayList<Integer> maxValues){
		ArrayList<ArrayList<Integer>> permutations = new ArrayList<ArrayList<Integer>>();
		ArrayList<Integer> tempPerm = null;

		for (int dim = 0; dim < minValues.size(); dim++) {						//über alle Dimensionen laufen
			for (int i = minValues.get(dim); i <= maxValues.get(dim); i++) {	//über den Wertebereich der aktuellen Dimension laufen
				tempPerm = new ArrayList<Integer>();
				for (int j = 0; j < minValues.size(); j++) {
					tempPerm.add(minValues.get(j));
				}
				tempPerm.set(dim,i);
				permutations.add(tempPerm);
			}
		}


		return permutations;
	}

}