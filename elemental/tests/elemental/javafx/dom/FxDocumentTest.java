package elemental.javafx.dom;

import javafx.scene.web.WebEngine;
import netscape.javascript.JSObject;

import org.junit.Assert;
import org.junit.Test;

import elemental.css.CSSStyleDeclaration;
import elemental.javafx.test.Fx;
import elemental.javafx.test.Fx.FxWebViewTestRunnable;

public class FxDocumentTest {
  @Test
  public void testGeneratedOperations() {
    Fx.runBlankWebPageInFx(new FxWebViewTestRunnable<RuntimeException>() {
      @Override
      public void run(WebEngine engine) {
        // Create a new paragraph element and insert it to see if it gets inserted properly
        FxDocument doc = new FxDocument((JSObject)engine
            .executeScript("document"));
        FxElement p = doc.createElement("p");
        p.setTextContent("hello");
        doc.getBody().appendChild(p);
        Assert.assertEquals("hello", doc.getBody().getTextContent());
      }
    });
  }

  @Test
  public void testCss() {
    Fx.runBlankWebPageInFx(new FxWebViewTestRunnable<RuntimeException>() {
      @Override
      public void run(WebEngine engine) {
        // Try setting the style of an element and reading it back.
        FxDocument doc = new FxDocument((JSObject)engine
            .executeScript("document"));
        doc.getBody().getStyle().setMargin(5, CSSStyleDeclaration.Unit.EM);
        Assert.assertEquals("5em", doc.getBody().getStyle().getMarginLeft());
      }
    });
  }
}
