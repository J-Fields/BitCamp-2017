
public interface PlayerController {
	RunNGun.Move chooseMove(Player[][] grid, int playerX, int playerY);
	RunNGun.Play nextPlay();//add more args later like yardage, down...
}
