package elemental.javafx.util;

import com.google.gwt.core.shared.GwtIncompatible;

import netscape.javascript.JSObject;

@GwtIncompatible
public class FxObject {
  protected JSObject obj;
  
  public FxObject(JSObject obj) {
    this.obj = obj;
  }
  
  public static JSObject unwrap(FxObject fxobj) {
    return fxobj.obj;
  }
  
  public static FxObject wrap(Object obj)
  {
    if (!(obj instanceof JSObject)) 
      throw new ClassCastException("Can only wrap JSObject in an FxObject");
    FxObject fxObj = new FxObject();
    fxObj.obj = (JSObject)obj;
    return fxObj;
  }
  
  
  // Temporary methods to help the transition from JavaScriptObject to FxObject.
  // (It allows FxObject to be used with a JavaScriptObject API temporarily)
  protected FxObject() {
    
  }
  
  @SuppressWarnings("unchecked")
  public final <T extends FxObject> T cast() {
    return (T) this;
  }

}
