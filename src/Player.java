
public class Player {
	public int x, y;
	boolean offense;
	
	public Player(int x, int y, boolean offense) {
		this.x = x;
		this.y = y;
		this.offense = offense;
	}
	
	public void move(RunNGun rng, RunNGun.Move m) {
		Player[][] grid = rng.grid;
		int x = this.x, y = this.y;
		if (m == RunNGun.Move.UP && y < 99) {
			if (grid[x][y+1] == null) {
				this.y = y + 1;
			} else if (grid[x][y+1].offense != this.offense) {
				this.y = y + 1;
				rng.tackle();
			}
			
		} else if (m == RunNGun.Move.DOWN && y > 0) {
			if (grid[x][y-1] == null) {
				this.y = y - 1;
			} else if (grid[x][y-1].offense != this.offense) {
				this.y = y - 1;
				rng.tackle();
			}
			
		} else if (m == RunNGun.Move.RIGHT && x < 6) {
			if (grid[x+1][y] == null) {
				this.x = x + 1;
			} else if (grid[x+1][y].offense != this.offense) {
				this.x = x + 1;
				rng.tackle();
			}
			
		} else if (m == RunNGun.Move.LEFT && x > 0) {
			if (grid[x-1][y] == null) {
				this.x = x - 1;
			} else if (grid[x-1][y].offense != this.offense) {
				this.x = x - 1;
				rng.tackle();
			}
			
		}
		grid[x][y] = null;
		grid[this.x][this.y] = this;
	}
}
