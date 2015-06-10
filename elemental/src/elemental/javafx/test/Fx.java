package elemental.javafx.test;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.concurrent.Worker.State;
import javafx.embed.swing.JFXPanel;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class Fx {

  /**
   * Awaits on a cyclic barrier and do appropriate handling of exceptions
   */
  public static void awaitBarrierUninterruptibly(CyclicBarrier barrier) {
    try {
      barrier.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch(BrokenBarrierException e) {
      // Do nothing
    }
  }
  
  /**
   * Creates a blank web page in a JavaFx WebView, waits for it to initialize, 
   * and then runs the provided code in a JavaFx thread that uses that web page.
   * Exceptions from the provided code will be propagated and rethrown.
   */
  public static <E extends Throwable> void runBlankWebPageInFx(final FxWebViewTestRunnable<E> runnable) throws E {
    CyclicBarrier wait = new CyclicBarrier(2);
    final AtomicReference<WebEngine> createdWebEngine = new AtomicReference<>();

    // Create a web view and load a blank page in it.
    runInFx(new FxTestRunnable<E>() {
      @Override
      public void run() throws E {
        WebView browser = new WebView();
        WebEngine engine = browser.getEngine();
        createdWebEngine.set(engine);
        engine.getLoadWorker().stateProperty().addListener(
            new ChangeListener<Worker.State>() {
              @Override
              public void changed(ObservableValue<? extends State> ov,
                  State oldState, State newState) {
                if (newState == Worker.State.SUCCEEDED) {
                  Fx.awaitBarrierUninterruptibly(wait);
                }
              }
            });
        engine.loadContent("<html></html>");
      }
    });
    
    // Wait until the blank web page is loaded.
    Fx.awaitBarrierUninterruptibly(wait);
    
    // Run some code that uses that WebView
    runInFx(new FxTestRunnable<E>() {
      @Override
      public void run() throws E {
        runnable.run(createdWebEngine.get());
      }
    });
  }
  
  public static interface FxWebViewTestRunnable<E extends Throwable> {
    public void run(WebEngine engine) throws E;
  }

  /**
   * Initializes JavaFx (if necessary) and runs some code in the JavaFx thread.
   * Exceptions from the provided code will be propagated and rethrown.
   */
  public static <E extends Throwable> void runInFx(FxTestRunnable<E> runnable) throws E {
    AtomicReference<Throwable> exceptionsThrown = new AtomicReference<>();
    CyclicBarrier wait = new CyclicBarrier(2);
    
    new JFXPanel();
    Platform.runLater(new Runnable() {
      @Override
      public void run() {
        try {
          runnable.run();
        } catch (Throwable e) {
          // Save all exceptions since it's not possible to catch generic exceptions
          exceptionsThrown.set(e);
        } finally {
          Fx.awaitBarrierUninterruptibly(wait);
        }
      }
    });
    
    // Wait for the code running the JavaFx thread finishes
    Fx.awaitBarrierUninterruptibly(wait);
    
    // Assume that exceptions are of type E or are RuntimeExceptions
    Throwable e = exceptionsThrown.get();
    if (e != null) {
      if (e instanceof RuntimeException)
        throw (RuntimeException)e;
      else
        throw (E)e;
    }
  }
  
  public static interface FxTestRunnable<E extends Throwable> {
    public void run() throws E;
  }

}
