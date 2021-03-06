/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package elemental.javafx.util;

import com.google.gwt.core.shared.GwtIncompatible;

import netscape.javascript.JSObject;
import elemental.util.Mappable;

/**
 */
// TODO (cromwellian) add generic when JSO bug in gwt-dev fixed
@GwtIncompatible
public class FxMappable extends FxElementalBase implements Mappable {
  protected FxMappable() {}
  public FxMappable(JSObject obj) { super(obj); }
  public static FxMappable wrap(Object obj) {
    if (obj == null || "undefined".equals(obj)) {
      return null;
    } else if (obj instanceof JSObject) {
      Object autoWrapped = GwtFxBridge.wrapJs(obj);
      if (!(autoWrapped instanceof FxMappable)) {
        return new FxMappable((JSObject)obj);
      } else {
        return (FxMappable)autoWrapped;
      }
    } else if (obj instanceof FxObject) {
      return new FxMappable(FxObject.unwrap((FxObject)obj));
    }
    throw new ClassCastException("Cannot cast object to FxNode");
  }  
}
