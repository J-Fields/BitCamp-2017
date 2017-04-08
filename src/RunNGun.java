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

import javax.swing.JFrame;
import javax.swing.WindowConstants;

public class RunNGun extends Thread {
	public enum GameState {
		SPLASH,
		EXITED,
		PAUSED,
		INGAME
	}

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

    // create a hardware accelerated image
    public final BufferedImage create(final int width, final int height,
    		final boolean alpha) {
    	return config.createCompatibleImage(width, height, alpha
    			? Transparency.TRANSLUCENT : Transparency.OPAQUE);
    }

    private RunNGun() {
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

    /**
     * Applies game logic each frame.
     */
    public void updateGame(double delta) {
    	
    }

    public void renderGame(Graphics2D g) {
    	g.setColor(Color.GREEN);
    	g.fillRect(0, 0, width, height);
    }

	public static void main(String[] args) {}

}
