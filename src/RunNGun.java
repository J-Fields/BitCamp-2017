import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

public class RunNGun extends Thread {
	public enum GameState {
		SPLASH,
		EXITED,
		PAUSED,
		INGAME
	}
	
	// Window stuff
	private final static RunNGun runNGun = new RunNGun();
    private GameState gameState = GameState.SPLASH;
    private Canvas canvas;
    private BufferStrategy strategy;
    private BufferedImage background;
    private Graphics2D backgroundGraphics;
    private Graphics2D graphics;
    private JFrame frame;
    private int width = 800;
    private int height = 800;
    private GraphicsConfiguration config =
    		GraphicsEnvironment.getLocalGraphicsEnvironment()
    			.getDefaultScreenDevice()
    			.getDefaultConfiguration();
    
    //Stuff we actually care about
    private final static double TICK = 1;
    Player[][] grid;
    Player offense;
    int down;
    int goalYard;
    double timeUntilNextTick;
    ArrayList<Player> defenders;
    Random rand = new Random();

    // create a hardware accelerated image
    public final BufferedImage create(final int width, final int height,
    		final boolean alpha) {
    	return config.createCompatibleImage(width, height, alpha
    			? Transparency.TRANSLUCENT : Transparency.OPAQUE);
    }

    private RunNGun() {
    	reset();

    	// JFrame
    	frame = new JFrame();
    	frame.addWindowListener(new FrameClose());
    	frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    	frame.setSize(width, height);
    	frame.setVisible(true);

    	// Canvas
    	canvas = new Canvas(config);
    	canvas.setSize(width, height);
    	frame.add(canvas, 0);

    	// Background & Buffer
    	background = create(width, height, false);
    	canvas.createBufferStrategy(2);
    	do {
    		strategy = canvas.getBufferStrategy();
    	} while (strategy == null);
    	start();
    }

    private class FrameClose extends WindowAdapter {
    	@Override
    	public void windowClosing(final WindowEvent e) {
    		gameState = GameState.EXITED;
    	}
    }

    // Screen and buffer stuff
    private Graphics2D getBuffer() {
    	if (graphics == null) {
    		try {
    			graphics = (Graphics2D) strategy.getDrawGraphics();
    		} catch (IllegalStateException e) {
    			return null;
    		}
    	}
    	return graphics;
    }

    private boolean updateScreen() {
    	graphics.dispose();
    	graphics = null;
    	try {
    		strategy.show();
    		Toolkit.getDefaultToolkit().sync();
    		return (!strategy.contentsLost());
    	} catch (NullPointerException e) {
    		return true;

    	} catch (IllegalStateException e) {
    		return true;
    	}
    }

    /**
     * Specifies the game loop.
     */
    public void run() {
    	backgroundGraphics = (Graphics2D) background.getGraphics();
    	long fpsWait = (long) (1.0 / 60 * 1000);
    	main: while (gameState != GameState.EXITED) {
    		long renderStart = System.nanoTime();

    		// TODO:  Passing fpsWait into here assumes we're running at 60 FPS
    		updateGame(fpsWait/1000.0);

    		// Update Graphics
    		do {
    			Graphics2D bg = getBuffer();
    			if (gameState == GameState.EXITED) {
    				break main;
    			}
    			renderGame(backgroundGraphics);
    			bg.drawImage(background, 0, 0, null);
    			bg.dispose();
    		} while (!updateScreen());

    		// Better do some FPS limiting here
    		long renderTime = (System.nanoTime() - renderStart) / 1000000;
    		try {
    			Thread.sleep(Math.max(0, fpsWait - renderTime));
    		} catch (InterruptedException e) {
    			Thread.interrupted();
    			break;
    		}
    	}
    	frame.dispose();
    }

    public void reset() {
    	down = 1;
    	goalYard = 30;
    	timeUntilNextTick = TICK;
    	
    	defenders = new ArrayList<Player>();

    	offense = new Player(3, 20, true);
    	grid = new Player[7][100];
    	for (int i = 0; i < 7; i++) {
    		for (int j = 0; j < 100; j++) {
    			if (i == offense.x && j == offense.y) {
					grid[i][j] = offense;
				} else if (j > 20 && rand.nextInt(10) <= 1) {
					Player defender = new Player(i, j, false);
    				grid[i][j] = defender; 
    				defenders.add(defender);
    			} else {
					grid[i][j] = null;
    			}
    		}
		}
    }
    
    /**
     * Applies game logic each frame.
     */
    public void updateGame(double delta) {
    	timeUntilNextTick -= delta;
    	if (timeUntilNextTick <= 0) {
    		moveDefenders();
    		timeUntilNextTick += TICK;
    	}
    }

    public void renderGame(Graphics2D g) {
    	//g.setColor(Color.GREEN);
    	//g.fillRect(0, 0, width, height);
    	int startY = offense.y - 1;
    	for (int y = startY; y < startY + 6; y++) {
    		for (int x = 0; x < 7; x++) {
				if (grid[x][y] == null) {
					g.setColor(Color.GREEN);
				} else if (grid[x][y].offense) {
					g.setColor(Color.BLUE);
				} else {
					g.setColor(Color.RED);
				}
				g.fillRect((int)(x/6.0*width), (int)(height-(y-startY+1)/6.0*height), (int)(width/6.0), (int)(height/6.0));
    		}
    	}
    }

    void moveDefenders() {
    	System.out.println("tick");
    	for (Player defender : defenders) {
    		int r = rand.nextInt(6);
			if (r == 0 && defender.y < 99 && grid[defender.x][defender.y+1] == null) {
				grid[defender.x][defender.y+1] = defender;
				defender.y = defender.y + 1;
			} else if (r == 1 && defender.y > 0 && grid[defender.x][defender.y-1] == null) {
				grid[defender.x][defender.y-1] = defender;
				defender.y = defender.y - 1;
			} else if (r == 2 && defender.x < 6 && grid[defender.x+1][defender.y] == null) {
				grid[defender.x+1][defender.y] = defender;
				defender.x = defender.x + 1;
			} else if (r == 3 && defender.x > 0 && grid[defender.x-1][defender.y] == null) {
				grid[defender.x-1][defender.y] = defender;
				defender.x = defender.x - 1;
			}
			grid[defender.x][defender.y] = null;
    	}
    }

	public static void main(String[] args) {}

}
