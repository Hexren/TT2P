package praktikum;

import org.rlcommunity.rlglue.codec.RLGlue;


public class Experiment {

	private static boolean timeIsOver = false;
	private static final int TRAININGDURATION_IN_MINUTES = 5;

	private void runExperiment(){
		print("Experiment initialised");

		print("Agent Team-Name:" + RLGlue.RL_agent_message("team name"));
		print("Environment Team-Name:" + RLGlue.RL_env_message("team name"));

		StopThread trainingTimer = new StopThread(TRAININGDURATION_IN_MINUTES);

		print("Agent: " + RLGlue.RL_agent_message("training start"));
		print("Environment: " + RLGlue.RL_env_message("training start"));

		trainingTimer.start();

		RLGlue.RL_init();		//Enviroment und Agent initialisieren
		while(!timeIsOver){
			RLGlue.RL_episode(0);	//Episode vom Start bis zum Endzustand durchspielen
		}
		RLGlue.RL_cleanup();	//aufräumen

		print("Agent: " + RLGlue.RL_agent_message("training end"));
		print("Enviroment: " + RLGlue.RL_env_message("training end"));

		//Status zum Training anzeigen
		String env_status = RLGlue.RL_env_message("get stats");
		print("Environment: " + env_status);

		String ag_status = RLGlue.RL_agent_message("get stats");
		print("Agent: \n" + ag_status);

		print("Jetzt wird's spannend!");
		//jetzt kommt das entscheidende Spiel mit Output
		RLGlue.RL_init();		//Enviroment und Agent initialisieren
		RLGlue.RL_episode(0);	//Episode vom Start bis zum Endzustand durchspielen

		String env_Gewinn = RLGlue.RL_env_message("get stats");
		print("Gewonnen oder verloren?\n" + env_Gewinn);

		print("Experiment ended!");
		RLGlue.RL_cleanup();
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