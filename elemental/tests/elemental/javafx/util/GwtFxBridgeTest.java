package elemental.javafx.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.scene.web.WebEngine;
import netscape.javascript.JSObject;

import org.junit.Assert;
import org.junit.Test;

import elemental.dom.TimeoutHandler;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.html.Location;
import elemental.html.Window;
import elemental.javafx.html.FxSpanElement;
import elemental.javafx.html.FxWindow;
import elemental.javafx.test.Fx;
import elemental.javafx.test.Fx.FxWebViewTestRunnable;

public class GwtFxBridgeTest {
  @Test
  public void testCast() {
    Fx.runBlankWebPageInFx(new FxWebViewTestRunnable<RuntimeException>() {
      @Override
      public void run(WebEngine engine) {
        // Try to rewrap an FxObject to a different type (i.e. Window)
        FxElementalBase obj = new FxElementalBase((JSObject)engine
            .executeScript("window"));
        Window win = GwtFxBridge.cast(obj, Window.class);
        Location loc = win.getLocation();
        Assert.assertEquals("about:", loc.getProtocol());
      }
    });
  }
  
  @Test
  public void testWrapJs() {
    Fx.runBlankWebPageInFx(new FxWebViewTestRunnable<RuntimeException>() {
      @Override
      public void run(WebEngine engine) {
        Assert.assertEquals(Integer.class, GwtFxBridge.wrapJs(5).getClass());
        Assert.assertEquals(Boolean.class, GwtFxBridge.wrapJs(true).getClass());

        Object obj = engine.executeScript("5.5");
        Assert.assertEquals(Double.class, GwtFxBridge.wrapJs(obj).getClass());
        obj = engine.executeScript("true");
        Assert.assertEquals(Boolean.class, GwtFxBridge.wrapJs(obj).getClass());
        obj = engine.executeScript("document.createElement('span')");
        Assert.assertEquals(FxSpanElement.class, GwtFxBridge.wrapJs(obj).getClass());
        // Non-elemental types map to FxElementalBase
        obj = engine.executeScript("[]");
        Assert.assertEquals(FxElementalBase.class, GwtFxBridge.wrapJs(obj).getClass());
        
        // Check that the same JSObject will map to the same FxObject
        obj = engine.executeScript("document");
        Object obj2 = engine.executeScript("document");
        Assert.assertEquals(GwtFxBridge.wrapJs(obj2), GwtFxBridge.wrapJs(obj));
      }
    });
  }
  
  @Test
  public void testUnwrapToJs() {
    Fx.runBlankWebPageInFx(new FxWebViewTestRunnable<RuntimeException>() {
      @Override
      public void run(WebEngine engine) {
        Assert.assertEquals(Integer.class, GwtFxBridge.unwrapToJs(5).getClass());
        Assert.assertEquals(Boolean.class, GwtFxBridge.unwrapToJs(true).getClass());

        Object obj = engine.executeScript("[]");
        Assert.assertEquals(obj, 
            GwtFxBridge.unwrapToJs(GwtFxBridge.wrapJs(obj)));
      }
    });
  }
  
  @Test
  public void testEntryPoint() {
    final AtomicInteger counter = new AtomicInteger(0);
    Fx.runBlankWebPageInFx(new FxWebViewTestRunnable<RuntimeException>() {
      @Override
      public void run(WebEngine engine) {
        TimeoutHandler incrementer = new TimeoutHandler() {
          @Override
          public void onTimeoutHandler() {
            counter.incrementAndGet();
          }};
        
        JSObject win = (JSObject)engine.executeScript("window");
        JSObject entry = GwtFxBridge.entryPoint(win, incrementer, "onTimeoutHandler");
        entry.call("call", new Object[]{});
      }
    });
    Assert.assertEquals(1, counter.get());
  }
  
  @Test
  public void testCallbacks() {
    // Create a timeout callback, and wait for it to trigger
    final CountDownLatch doneSignal = new CountDownLatch(1);
    final AtomicBoolean isTriggered = new AtomicBoolean();
    Fx.runBlankWebPageInFx(new FxWebViewTestRunnable<RuntimeException>() {
      @Override
      public void run(WebEngine engine) {
        Window win = FxWindow.wrap(engine.executeScript("window"));
        win.setTimeout(new TimeoutHandler() {
          @Override
          public void onTimeoutHandler() {
            isTriggered.set(true);
            doneSignal.countDown();
          }
        }, 1);
      }
    });
    Fx.awaitUninterruptibly(doneSignal);
    Assert.assertTrue(isTriggered.get());
  }
  
  @Test
  public void testEventListenerSetter() {
    // Set an event listener, trigger the corresponding event, and check
    // it was triggered.
    final CountDownLatch doneSignal = new CountDownLatch(1);
    Fx.runBlankWebPageInFx(new FxWebViewTestRunnable<RuntimeException>() {
      @Override
      public void run(WebEngine engine) {
        Window win = FxWindow.wrap(engine.executeScript("window"));
        win.getDocument().getBody().setOnkeyup(new EventListener() {
          @Override
          public void handleEvent(Event evt) {
            doneSignal.countDown();
          }});
        Event evt = win.getDocument().createEvent("KeyboardEvent");
        evt.initEvent("keyup", true, false);
        win.getDocument().getBody().dispatchEvent(evt);
      }
    });
    Fx.awaitUninterruptibly(doneSignal);
  }
  
  @Test
  public void testEventListenerGetter() {
    // See if event listeners can be round-tripped being passed into JS
    Fx.runBlankWebPageInFx(new FxWebViewTestRunnable<RuntimeException>() {
      @Override
      public void run(WebEngine engine) {
        Window win = FxWindow.wrap(engine.executeScript("window"));
        EventListener keyUp = new EventListener() {
          @Override
          public void handleEvent(Event evt) {}};
        win.getDocument().getBody().setOnkeyup(keyUp);
        Assert.assertEquals(keyUp, win.getDocument().getBody().getOnkeyup());
      }
    });
  }
  
  @Test
  public void testNewJsObject() {
    // Try creating a Date
    Fx.runBlankWebPageInFx(new FxWebViewTestRunnable<RuntimeException>() {
      @Override
      public void run(WebEngine engine) {
        JSObject win = (JSObject)engine.executeScript("window");
        JSObject date = (JSObject)GwtFxBridge.newJsObject((JSObject)win.getMember("Date"));
        Assert.assertTrue((int)date.call("getFullYear", new Object[] {}) > 2014);

        date = (JSObject)GwtFxBridge.newJsObject((JSObject)win.getMember("Date"), 30.0 * 24.0 * 60.0 * 60.0 * 1000.0);
        Assert.assertEquals(1970, (int)date.call("getFullYear", new Object[] {}));
      }
    });
  }
}
