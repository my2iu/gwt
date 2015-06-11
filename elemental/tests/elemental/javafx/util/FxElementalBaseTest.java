package elemental.javafx.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import org.junit.Assert;
import org.junit.Test;

import elemental.events.Event;
import elemental.events.EventListener;
import elemental.events.EventRemover;
import elemental.html.Window;
import elemental.javafx.html.FxWindow;
import elemental.javafx.test.Fx;
import elemental.javafx.test.Fx.FxTestRunnable;
import elemental.javafx.test.Fx.FxWebViewTestRunnable;

public class FxElementalBaseTest {
  @Test
  public void testArray() {
    Fx.runInFx(new FxTestRunnable<RuntimeException>() {
      @Override
      public void run() {
        WebView browser = new WebView();
        WebEngine engine = browser.getEngine();
        FxElementalBase obj = new FxElementalBase((JSObject)engine.executeScript("[]"));
        Assert.assertEquals(0, obj.length());
        obj.setAt(0, 5);
        Assert.assertEquals(5, obj.intAt(0));
        Assert.assertEquals(1, obj.length());
      }
    });
  }
  
  private Event createKeyUpEvent(Window win) {
    Event evt = win.getDocument().createEvent("KeyboardEvent");
    evt.initEvent("keyup", true, false);
    return evt;
  }
  
  @Test
  public void testAddEventListener() {
    // Set an event listener, trigger the corresponding event, and check
    // it was triggered.
    final CountDownLatch doneSignal = new CountDownLatch(1);
    Fx.runBlankWebPageInFx(new FxWebViewTestRunnable<RuntimeException>() {
      @Override
      public void run(WebEngine engine) {
        Window win = FxWindow.wrap(engine.executeScript("window"));
        win.getDocument().getBody().addEventListener(Event.KEYUP, new EventListener() {
          @Override
          public void handleEvent(Event evt) {
            doneSignal.countDown();
          }});
        win.getDocument().getBody().dispatchEvent(createKeyUpEvent(win));
      }
    });
    Fx.awaitUninterruptibly(doneSignal);
  }

  @Test
  public void testEventRemover() {
    // Set an event listener, trigger the corresponding event, and check
    // it was triggered.
    final AtomicInteger eventCount = new AtomicInteger(0);
    final CountDownLatch doneSignal = new CountDownLatch(2);
    Fx.runBlankWebPageInFx(new FxWebViewTestRunnable<RuntimeException>() {
      @Override
      public void run(WebEngine engine) {
        Window win = FxWindow.wrap(engine.executeScript("window"));
        EventListener listener = new EventListener() {
          @Override
          public void handleEvent(Event evt) {
            eventCount.incrementAndGet();
            doneSignal.countDown();
          }};
        
        // Add an event listener and wait for it to trigger
        EventRemover remover = 
            win.getDocument().getBody().addEventListener(Event.KEYUP, listener);
        win.getDocument().getBody().dispatchEvent(createKeyUpEvent(win));
        
        // Remove the listener and trigger an event
        remover.remove();
        win.getDocument().getBody().dispatchEvent(createKeyUpEvent(win));
        
        // Add the event listener back and wait for it to trigger 
        win.getDocument().getBody().addEventListener(Event.KEYUP, listener);
        win.getDocument().getBody().dispatchEvent(createKeyUpEvent(win));
      }
    });
    Fx.awaitUninterruptibly(doneSignal);
    Assert.assertEquals(2, eventCount.get());
  }

}
