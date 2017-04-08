
public class Player {
	public int x, y;
	boolean offense;
	
	public Player(int x, int y, boolean offense) {
		this.x = x;
		this.y = y;
		this.offense = offense;
	}
	
	public void move(Player[][] grid, RunNGun.Move m) {
		int x = this.x, y = this.y;
		if (m == RunNGun.Move.UP && y < 99 && grid[x][y+1] == null) {
			this.y = y + 1;
		} else if (m == RunNGun.Move.DOWN && y > 0 && grid[x][y-1] == null) {
			this.y = y - 1;
		} else if (m == RunNGun.Move.RIGHT && x < 6 && grid[x+1][y] == null) {
			this.x = x + 1;
		} else if (m == RunNGun.Move.LEFT && x > 0 && grid[x-1][y] == null) {
			this.x = x - 1;
		}
		grid[x][y] = null;
		grid[this.x][this.y] = this;
	}
}
