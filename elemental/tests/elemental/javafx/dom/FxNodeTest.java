package elemental.javafx.dom;

import javafx.scene.web.WebEngine;
import netscape.javascript.JSObject;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;

import elemental.dom.Element;
import elemental.javafx.test.Fx;
import elemental.javafx.test.Fx.FxWebViewTestRunnable;

public class FxNodeTest {
  @Test
  public void testGeneratedStringSetters() {
    Fx.runBlankWebPageInFx(new FxWebViewTestRunnable<RuntimeException>() {
      @Override
      public void run(WebEngine engine) {
        // Make a paragraph element
        Document doc = engine.getDocument();
        doc.getElementsByTagName("body").item(0)
            .appendChild(doc.createElement("p"));
        FxNode node = new FxNode((JSObject)engine
            .executeScript("document.getElementsByTagName('p')[0]"));
        node.setTextContent("hello");
        Assert.assertEquals("hello", 
            doc.getElementsByTagName("p").item(0).getTextContent());
      }
    });
  }

  @Test
  public void testGeneratedStringGetters() {
    Fx.runBlankWebPageInFx(new FxWebViewTestRunnable<RuntimeException>() {
      @Override
      public void run(WebEngine engine) {
        // Make a paragraph element with some text and see if we can read it out
        engine.executeScript("document.body.innerHTML = '<p>hello</p>'");
        FxNode node = new FxNode((JSObject)engine
            .executeScript("document.getElementsByTagName('p')[0]"));
        Assert.assertEquals("hello", node.getTextContent()); 
      }
    });
  }

  @Test
  public void testGeneratedJsGetters() {
    Fx.runBlankWebPageInFx(new FxWebViewTestRunnable<RuntimeException>() {
      @Override
      public void run(WebEngine engine) {
        // Make a paragraph element with some text and see if we can read it out
        engine.executeScript("document.body.innerHTML = '<p><b>hello</b></p>'");
        FxNode node = new FxNode((JSObject)engine
            .executeScript("document.body")); 
        Assert.assertNotNull(node.getFirstChild());
        Assert.assertEquals("<b>hello</b>", ((Element)node.getFirstChild()).getInnerHTML());
      }
    });
  }

  @Test
  public void testGeneratedJsSetters() {
    Fx.runBlankWebPageInFx(new FxWebViewTestRunnable<RuntimeException>() {
      @Override
      public void run(WebEngine engine) {
        // Call a setter to replace the body element
        FxDocument doc = new FxDocument((JSObject)engine
            .executeScript("document"));
        FxElement newBody = doc.createElement("BODY");
        newBody.setTextContent("hello");
        doc.setBody(newBody);
        Assert.assertEquals("hello", doc.getBody().getTextContent());
      }
    });
  }
}
