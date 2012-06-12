package praktikum;

import java.util.List;

public class StateActionPair {
	private List<Integer> state;
	private List<Integer> action;
	
	public StateActionPair(List<Integer> state, List<Integer> action){
		this.state = state;
		this.action = action;
	}
	
	public List<Integer> getState(){
		return state;
	}	
	
	public List<Integer> getAction(){
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
