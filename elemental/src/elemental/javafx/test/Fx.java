package elemental.javafx.test;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;

public class Fx {
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
          try {
            wait.await();
          } catch (InterruptedException |BrokenBarrierException e) {
            return;
          }
        }
      }
    });
    
    // Wait for the code running the JavaFx thread finishes
    try {
      wait.await();
    } catch (InterruptedException |BrokenBarrierException e) {
      // Eat the error and continue
    }
    
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
