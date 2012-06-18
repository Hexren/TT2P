package praktikum;

import org.rlcommunity.rlglue.codec.RLGlue;


public class Experiment {

	private static boolean timeIsOver = false;
	private static final int TRAININGDURATION_IN_MINUTES = 1;

	private void runExperiment(){
		print("Experiment initialised");

		print("Agent Team-Name:" + RLGlue.RL_agent_message("team name"));
		print("Agent Team-Members:" + RLGlue.RL_agent_message("team members"));
		print("Enviroment Team-Name:" + RLGlue.RL_env_message("team name"));
		print("Enviroment Team-Members:" + RLGlue.RL_env_message("team members"));

		StopThread trainingTimer = new StopThread(TRAININGDURATION_IN_MINUTES);

		print("Agent: " + RLGlue.RL_agent_message("training start"));
		print("Enviroment: " + RLGlue.RL_env_message("training start"));

		trainingTimer.start();

		RLGlue.RL_init();		//Enviroment und Agent initialisieren

		while(!timeIsOver){
			RLGlue.RL_episode(0);	//Episode vom Start bis zum Endzustand durchspielen
		}

		print("Agent: " + RLGlue.RL_agent_message("training end"));
		print("Enviroment: " + RLGlue.RL_env_message("training end"));

		//Status zum Training anzeigen
		String env_status = RLGlue.RL_env_message("get stats");
		print("Enviroment: " + env_status);
		//
		//		String ag_status = RLGlue.RL_agent_message("get stats");
		//		print("Agent: \n" + ag_status);

		//jetzt kommt das entscheidende Spiel mit Output
		RLGlue.RL_episode(0);	//Episode vom Start bis zum Endzustand durchspielen

		print("Experiment ended!");
		RLGlue.RL_cleanup();	//aufräumen
	}

	private void print(String msg){
		System.out.println(msg);
	}

	public static void setTimeIsOver(boolean flag){
		timeIsOver = flag;
	}


	public static void main(String[] args) {
		Experiment theExperiment = new Experiment();
		theExperiment.runExperiment();
	}
}

// StopTask setzt das Flag nach Ablauf der spezifizierten Zeit auf flase
class StopThread extends Thread{

	long sleepTime = 0;

	public StopThread(int delayInMinutes){
		sleepTime = delayInMinutes * 60 * 1000;
	}

	@Override
	public void run() {
		try {
			sleep(sleepTime);
			Experiment.setTimeIsOver(true);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}