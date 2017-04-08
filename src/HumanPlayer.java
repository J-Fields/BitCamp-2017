
public class HumanPlayer implements PlayerController {

	@Override
	public RunNGun.Move chooseMove(Player[][] grid, int playerX, int playerY) {
		return RunNGun.Move.UP;
	}

}
