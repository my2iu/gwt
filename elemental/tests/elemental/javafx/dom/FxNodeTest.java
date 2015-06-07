package elemental.javafx.dom;

import netscape.javascript.JSObject;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;

import elemental.dom.Element;
import elemental.javafx.test.Fx;

public class FxNodeTest {
  @Test
  public void testGeneratedStringSetters() {
    Fx.runBlankWebPageInFx((engine) -> {
      // Make a paragraph element
      Document doc = engine.getDocument();
      doc.getElementsByTagName("body").item(0)
          .appendChild(doc.createElement("p"));
      FxNode node = new FxNode((JSObject)engine
          .executeScript("document.getElementsByTagName('p')[0]"));
      node.setTextContent("hello");
      Assert.assertEquals("hello", 
          doc.getElementsByTagName("p").item(0).getTextContent());
    }); 
  }

  @Test
  public void testGeneratedStringGetters() {
    Fx.runBlankWebPageInFx((engine) -> {
      // Make a paragraph element with some text and see if we can read it out
      engine.executeScript("document.body.innerHTML = '<p>hello</p>'");
      FxNode node = new FxNode((JSObject)engine
          .executeScript("document.getElementsByTagName('p')[0]"));
      Assert.assertEquals("hello", node.getTextContent()); 
    }); 
  }

  @Test
  public void testGeneratedJsGetters() {
    Fx.runBlankWebPageInFx((engine) -> {
      // Make a paragraph element with some text and see if we can read it out
      engine.executeScript("document.body.innerHTML = '<p><b>hello</b></p>'");
      FxNode node = new FxNode((JSObject)engine
          .executeScript("document.body"));
      Assert.assertNotNull(node.getFirstChild());
      Assert.assertEquals("<b>hello</b>", ((Element)node.getFirstChild()).getInnerHTML());
    }); 
  }

}
