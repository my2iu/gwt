package elemental.javafx.html;

import javafx.scene.web.WebEngine;

import org.junit.Assert;
import org.junit.Test;

import elemental.javafx.test.Fx;
import elemental.javafx.test.Fx.FxWebViewTestRunnable;
import elemental.xml.XMLHttpRequest;

public class FxWindowTest {
  @Test
  public void testConstructors() {
    Fx.runBlankWebPageInFx(new FxWebViewTestRunnable<RuntimeException>() {
      @Override
      public void run(WebEngine engine) {
        // Try using one of the generated constructors
        FxWindow win = FxWindow.wrap(engine.executeScript("window"));
        FxUint8Array arr = win.newUint8Array(5);
        arr.setAt(0, -1);
        Assert.assertEquals(255, arr.intAt(0));
        
        XMLHttpRequest xhr = win.newXMLHttpRequest();
        Assert.assertEquals("", xhr.getStatusText());
      }
    });
  }
}
