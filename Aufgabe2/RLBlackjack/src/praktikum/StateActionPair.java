package praktikum;

import java.util.ArrayList;

public class StateActionPair {
	private ArrayList<Integer> state;
	private ArrayList<Integer> action;
	
	public StateActionPair(ArrayList<Integer> state, ArrayList<Integer> action){
		this.state = state;
		this.action = action;
	}
	
	public ArrayList<Integer> getState(){
		return state;
	}	
	
	public ArrayList<Integer> getAction(){
		return action;
	}
	
	@Override
	public String toString(){
		return "States: " + state.toString() + " Actions:" + action.toString();
	}
	
	@Override
	public boolean equals(Object obj){
		if(this == obj) return true;

		if(!(obj instanceof StateActionPair)) return false;
		
		StateActionPair check = (StateActionPair)obj;
		
		//State Action Pairs sind gleich, wenn beide Listen gleich sind
		return state.equals(check.getState()) && action.equals(check.getAction());
		
	}
	
	@Override
	public int hashCode() {
		int result = 17;
		result += 31 * state.hashCode();
		result += 31 * action.hashCode();
		return result;
	}
}
