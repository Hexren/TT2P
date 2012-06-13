package praktikum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.rlcommunity.rlglue.codec.AgentInterface;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpec;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;
import org.rlcommunity.rlglue.codec.util.AgentLoader;

public class QLearningAgent implements AgentInterface {
	private static final String TEAM_NAME ="Team 1";
	private static final String TEAM_MEMBERS ="Oliver Steenbuck, Svend-Anjes Pahl, Stefan Münchow, Milena Roetting, Armin Steudte, Carsten Noetzel";
	
	private boolean showOutput;
	
	private Random randGenerator = new Random();
    private ArrayList<Integer> lastAction;
    private ArrayList<Integer> lastObservation;
    
    private ArrayList<Integer[]> numActions;
	
    private double gamma = 0.0;
    private double epsilon = 0.1;
    private double alpha = 0.1;
    
    private HashMap<StateActionPair, Double> actionValueFunction;
    
    public QLearningAgent() {
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
	public void agent_end(double arg0) {
		// TODO Auto-generated method stub
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
        
        // Anzahl der möglichen Aktionen speichern. Für jede Dimension wird in der ArrayList 
        // ein neues Integer-Array mit oberer und unterer Grenze angelegt
        numActions = new ArrayList<Integer[]>();
        for (int i = 0; i < actionDimensions; i++) {
        	Integer[] range = new Integer[] { 
        			taskSpec.getDiscreteActionRange(i).getMin(), 
        			taskSpec.getDiscreteActionRange(i).getMax() };
			numActions.add(range);
		}
		
		// Discountfaktor bestimmen
		gamma = taskSpec.getDiscountFactor(); 
	}

	@Override
	public String agent_message(String msg) {
		// Diese Methode reagiert auf Messages die eintreffen

		if(msg.equals("team name")) {
			return TEAM_NAME;
		} else if(msg.equals("team member")) {
			return TEAM_MEMBERS;
		} else if(msg.equals("training start")) {
			showOutput = false;
			return "Start training: Output " + showOutput;
		} else if(msg.equals("training end")) {
			showOutput = true;
			return "Training ended: Output " + showOutput;
		} else if(msg.equals("get stats")) {
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
		
		ArrayList<Integer> actualAction = nextAction(actualObservation); 	//neue Aktion auf Basis der aktuellen Beobachtung wählen
		
		Action returnAction = new Action(actualAction.size(), 0, 0);
		int[] actions = new int[actualAction.size()];					//Arraylist in Integer umwandeln
		for (int i = 0; i < actions.length; i++) {
			actions[i] = actualAction.get(i);
		}
		returnAction.intArray= actions;
		
		lastAction = actualAction;					//aktuelle Aktion sichern
		saveObservation(actualObservation); 		//aktuelle Beobachtung sichern
		
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
		
		ArrayList<Integer> actualAction = nextAction(actualObservation); 	//neue Aktion auf Basis der aktuellen Beobachtung wählen
		 
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
		
		lastAction = actualAction;					//aktuelle Aktion sichern
		saveObservation(actualObservation); 		//aktuelle Beobachtung sichern
		
		return returnAction;
	}
	
	private ArrayList<Integer> nextAction(ArrayList<Integer> actualObservation) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public static void main(String[] args) {
        AgentLoader theLoader = new AgentLoader(new QLearningAgent());
        theLoader.run();
	}
	
	private void saveObservation(ArrayList<Integer> observation){
		lastObservation = new ArrayList<Integer>();
		for (Integer integer : observation) {
			lastObservation.add(integer);
		}
    }
}
