import java.awt.BasicStroke;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
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
		PAUSED_TD,
		PAUSED_TACKLED,
		PAUSED_EOQ,
		INGAME
	}
	
	public enum Move {
		UP,
		DOWN,
		LEFT,
		RIGHT,
		NO_MOVE
	}
	
	public enum Play {
		RUN,
		PASS
	}
	
	// Window stuff
	private final static RunNGun runNGun = new RunNGun();
    private Canvas canvas;
    private BufferStrategy strategy;
    private BufferedImage background;
    private Graphics2D backgroundGraphics;
    private Graphics2D graphics;
    private JFrame frame;
    private int gamewidth = 600;
    private int scoreboardWidth = 200;
    private int height = 600;
    private GraphicsConfiguration config =
    		GraphicsEnvironment.getLocalGraphicsEnvironment()
    			.getDefaultScreenDevice()
    			.getDefaultConfiguration();
    
    //Stuff we actually care about
    private GameState gameState = GameState.INGAME;
    private final static double TICK = 0.2;
    Player[][] grid;
    Player offense;
    int down;
    int firstDownMark;
    int startRow;
    int quarter;
    double gameClock;
    double timeUntilNextTick;
    ArrayList<Player> defenders;
    Random rand = new Random();
    PlayerController controller1 = new HumanPlayer();
    PlayerController controller2 = new HumanPlayer();
    PlayerController currController = controller1;
    private int score1 = 0;
    private int score2 = 0;

    // create a hardware accelerated image
    public final BufferedImage create(final int width, final int height,
    		final boolean alpha) {
    	return config.createCompatibleImage(width, height, alpha
    			? Transparency.TRANSLUCENT : Transparency.OPAQUE);
    }

    private RunNGun() {
    	quarter = 1;
    	gameClock = 10;
    	down = 1;
    	reset(20,30);
    	
    	// JFrame
    	frame = new JFrame();
    	frame.addWindowListener(new FrameClose());
    	frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    	frame.setSize(gamewidth+scoreboardWidth, height);
    	frame.setVisible(true);

    	// Canvas
    	canvas = new Canvas(config);
    	canvas.setSize(gamewidth+scoreboardWidth, height);
    	frame.add(canvas, 0);

    	// Background & Buffer
    	background = create(gamewidth+scoreboardWidth, height, false);
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

    public void reset(int row, int goalY) {
    	firstDownMark = goalY;
    	startRow = row;
    	timeUntilNextTick = TICK;
    	
    
    	//pause game, prompt...
    	
    	
    	defenders = new ArrayList<Player>();

    	offense = new Player(3, row, true);
    	grid = new Player[7][100];
    	for (int i = 0; i < 7; i++) {
    		for (int j = 0; j < 100; j++) {
    			if (i == offense.x && j == offense.y) {
					grid[i][j] = offense;
				} else if (j > row && rand.nextInt(10) <= 0) {
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
    	if (gameState == GameState.INGAME) {
	    	timeUntilNextTick -= delta;
	    	gameClock-=delta;
	    	if (gameClock < 0) {
	    		gameClock = 0;
	    	}
	    	if (timeUntilNextTick <= 0) {
	    		moveDefenders();
	    		moveOffense();
    			timeUntilNextTick += TICK;
    		}
    	} else if (gameState == GameState.PAUSED_TACKLED || gameState == GameState.PAUSED_TD || gameState == GameState.PAUSED_EOQ) {
    		if ((int) gameClock == 0) {
    			gameState = GameState.PAUSED_EOQ;
    			if (quarter == 4) {
    				//end of game prompt
    			} else {
    				quarter++;
    				//"end of <quarter> lol"
    				gameClock = 60;
    			}
    		}
    		if (currController.nextPlay() == Play.RUN) {
    			gameState = GameState.INGAME;
    			updateGame(delta);
    		}
    	}
    }

    public void renderGame(Graphics2D g) {
    	g.setColor(Color.GREEN);
    	g.fillRect(0, 0, gamewidth, height);
    	int startY = offense.y - 1;
    	if (startY > 93) {
    		startY = 93;
    	}
    	
    	g.setColor(Color.BLACK);
    	g.fillRect(gamewidth, 0, scoreboardWidth, height);
    	g.setColor(Color.GRAY);
    	g.fillRect(gamewidth+scoreboardWidth/10, height/8, (scoreboardWidth*4/5), height/2);
    	g.setColor(Color.WHITE);
    	g.setFont(new Font("TimesRoman", Font.PLAIN, 24));
    	g.drawString("Player1", gamewidth+scoreboardWidth/3, height/8+30);
    	g.drawString(String.valueOf(score1), gamewidth+scoreboardWidth/2, height/8+55);
    	g.drawString("Player2", gamewidth+scoreboardWidth/3, height/8+80);
    	g.drawString(String.valueOf(score2), gamewidth+scoreboardWidth/2, height/8+105);
    	g.drawString("DOWN", gamewidth+scoreboardWidth/7, height/8+130);
    	g.drawString(String.valueOf(down), gamewidth+scoreboardWidth/4, height/8+155);
    	g.drawString("TO GO", gamewidth+scoreboardWidth/2+10, height/8+130);
    	g.drawString(String.valueOf(firstDownMark-startRow), gamewidth+scoreboardWidth*3/4-15, height/8+155);
    	g.drawString("QTR", gamewidth+scoreboardWidth/7, height/8+180);
    	g.drawString("TIME", gamewidth+scoreboardWidth/2+10, height/8+180);
    	g.drawString(String.valueOf(quarter), gamewidth+scoreboardWidth/4, height/8+205);
    	g.drawString(String.valueOf((int) gameClock), gamewidth+scoreboardWidth*3/4-15, height/8+205);


    	
    	
    	for (int y = 0; y < 7; y++) {
    		int yCoord = (int)(y/7.0*height);
    		if ((startY - y) % 10 == 0) {
    			g.setStroke(new BasicStroke(7));
    			g.setColor(Color.WHITE);
    		} else {
    			g.setStroke(new BasicStroke(1));
    			g.setColor(Color.BLACK);
    		}
    		g.drawLine(0, yCoord, gamewidth, yCoord);
    	}
		g.setStroke(new BasicStroke(1));
		g.setColor(Color.BLACK);
    	for (int x = 0; x < 7; x++) {
    		int xCoord = (int)(x/7.0*height);
    		g.drawLine(xCoord, 0, xCoord, height);
    	}
    	
    	for (int y = startY; y < startY + 7; y++) {
    		for (int x = 0; x < 7; x++) {
    			if (y >= 98) {
					g.setColor(Color.YELLOW);
					g.fillRect((int)(x/7.0*gamewidth), (int)(height-(y-startY+1)/7.0*height), (int)(gamewidth/7.0), (int)(height/7.0));
    			} 
    			if (grid[x][y] == null) {
					// Nothing there
				} else if (grid[x][y].offense) {
					g.setColor(Color.BLUE);
					g.fillOval((int)(x/7.0*gamewidth), (int)(height-(y-startY+1)/7.0*height), (int)(gamewidth/7.0), (int)(height/7.0));
				} else {
					g.setColor(Color.RED);
					g.fillOval((int)(x/7.0*gamewidth), (int)(height-(y-startY+1)/7.0*height), (int)(gamewidth/7.0), (int)(height/7.0));
				}
    		}
    	}
    	if (gameState == GameState.PAUSED_TACKLED) {
    		g.setColor(Color.BLUE);
    		g.fillRect(gamewidth/4, height/4, gamewidth/2, height/2);
    		g.setColor(Color.WHITE);
    		g.drawString("You got tackled!", gamewidth/3+25, height/4+25);
    		g.drawString("LMAO @ U", gamewidth/3+25, height/4+50);
        	g.setFont(new Font("TimesRoman", Font.PLAIN, 15));
    		g.drawString("PRESS ENTER FOR NEXT PLAY", gamewidth/4+25, height*3/4-25);
        	g.setFont(new Font("TimesRoman", Font.PLAIN, 24));
    	} else if (gameState == GameState.PAUSED_TD) {
    		g.setColor(Color.BLUE);
    		g.fillRect(gamewidth/4, height/4, gamewidth/2, height/2);
    		g.setColor(Color.WHITE);
    		g.drawString("You scored!", gamewidth/3+25, height/4+25);
    		g.drawString("THIS WOULD BE COOL IF IT MATTERED!", gamewidth/3, height/4+50);
        	g.setFont(new Font("TimesRoman", Font.PLAIN, 15));
    		g.drawString("PRESS ENTER FOR NEXT DRIVE", gamewidth/4+25, height*3/4-25);
        	g.setFont(new Font("TimesRoman", Font.PLAIN, 24));
    	}
    	
    	
    }

    void moveDefenders() {
    	for (Player defender : defenders) {
    		int r = rand.nextInt(6);
    		if (r == 0) {
    			defender.move(this, Move.UP);
    		} else if (r == 1) {
    			defender.move(this, Move.DOWN);
    		} else if (r == 2) {
    			defender.move(this, Move.LEFT);
    		} else if (r == 3) {
    			defender.move(this, Move.RIGHT);
    		}
    	}
    }
    
    void moveOffense() {
    	offense.move(this, currController.chooseMove(grid, offense.x, offense.y));
    	if (offense.y >=99) {
    		//touchdown wowowowoowowwo
    		if (currController == controller1) {
    			score1+=7;
    			currController = controller2;
    		} else {
    			score2+=7;
    			currController = controller1;
    		}
    		down = 1;
    		gameState = GameState.PAUSED_TD;
			reset(20,30);
    	}
    }
    
    void tackle() {
    	gameState = GameState.PAUSED_TACKLED;
    	if (offense.y >= firstDownMark) {//first down, lmao
    		down = 1;
    		if (offense.y >= 90) {
    			firstDownMark = 100;
    		} else {
    			firstDownMark = offense.y + 10;
    		}
    		reset(offense.y,firstDownMark);
    	} else {
    		down++;
    		if (down > 4) {
    			//lmao turnover
    			if (currController == controller1) {
    				currController = controller2;
    			} else {
    				currController = controller1;
    			}
    			down = 1;
    			reset(20,30);
    		} else {
    			reset(offense.y,firstDownMark);
    		}
    	}
    }

	public static void main(String[] args) {}

}
