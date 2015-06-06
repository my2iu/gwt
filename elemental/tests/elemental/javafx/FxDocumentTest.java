package elemental.javafx;

import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import org.junit.Test;

import elemental.javafx.test.Fx;

public class FxDocumentTest {
  @Test
  public void test() {
    Fx.runInFx(()->  {
      WebView browser = new WebView();
      WebEngine engine = browser.getEngine();
      System.err.println("Running in JFX"); 
    });
    System.err.println("Testing");
  }
  
}
