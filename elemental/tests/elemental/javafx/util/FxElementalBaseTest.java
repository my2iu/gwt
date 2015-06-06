package elemental.javafx.util;

import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import org.junit.Assert;
import org.junit.Test;

import elemental.javafx.test.Fx;

public class FxElementalBaseTest {
  @Test
  public void testArray() {
    Fx.runInFx(() -> {
      WebView browser = new WebView();
      WebEngine engine = browser.getEngine();
      FxElementalBase obj = new FxElementalBase((JSObject)engine.executeScript("[]"));
      Assert.assertEquals(0, obj.length());
      obj.setAt(0, 5);
      Assert.assertEquals(5, obj.intAt(0));
      Assert.assertEquals(1, obj.length());
    });
  }
}
