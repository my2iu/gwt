package elemental.javafx.util;

import javafx.scene.web.WebEngine;
import netscape.javascript.JSObject;

import org.junit.Assert;
import org.junit.Test;

import elemental.html.Location;
import elemental.html.Window;
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
}
