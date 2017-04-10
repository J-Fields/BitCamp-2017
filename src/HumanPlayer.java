import java.util.concurrent.TimeUnit;

public class HumanPlayer implements PlayerController {

	@Override
	public RunNGun.Move chooseMove(Player[][] grid, int playerX, int playerY) {
		return RunNGun.Move.UP;
	}
	
	@Override
	public RunNGun.Play nextPlay() {
		try {
			TimeUnit.SECONDS.sleep(2);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return RunNGun.Play.RUN;
	}

}
