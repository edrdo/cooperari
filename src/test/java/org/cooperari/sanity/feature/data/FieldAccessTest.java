//
//   Copyright 2014-2019 Eduardo R. B. Marques
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//

package org.cooperari.sanity.feature.data;
import static org.cooperari.CSystem.hotspot;
import static org.junit.Assert.assertEquals;

import org.cooperari.CSystem;
import org.cooperari.config.CNever;
import org.cooperari.config.CSometimes;
import org.cooperari.junit.CJUnitRunner;
import org.cooperari.sanity.feature.Data;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SuppressWarnings("javadoc")
@RunWith(CJUnitRunner.class)
public class FieldAccessTest {

  private static Data A;
  private static Data B;   

  private static final Runnable 
  rTestBasic =
  new Runnable() { 
    public void run() {
      Data a = new Data();
      Data b = new Data();
      b.x = a.x + 1;
      assertEquals(0, a.x);
      assertEquals(1, b.x);
    }
  };

  @Test
  public final void testBasic() {
    CSystem.forkAndJoin(rTestBasic);
  }

  private static final Runnable 
  rTestCoverage[] = {
      new Runnable() {
        public void run() {
          if (A.x == 0) {
            B.x = 1;
            hotspot("B1");
          } else if (A.x == 1) {
            B.x = 2;
            hotspot("B2");
          } else {
            B.x = 3;
            hotspot("B3"); 
          }
        }
      },
      new Runnable() {
        public void run() {
          if (B.x == 0) {
            A.x = 1;
            hotspot("A1");
          } else if (B.x == 1){
            A.x = 2;
            hotspot("A2");
          } else {
            A.x = 3;
            hotspot("A3");
          }
        }
      }
  };

  @Test 
  @CSometimes({"A1", "A2", "B1", "B2"}) 
  @CNever({"A3", "B3"})
  public final void testCoverage() {
    A = new Data();
    B = new Data();
    CSystem.forkAndJoin(rTestCoverage);
  }

  @Test(expected=NullPointerException.class)
  @SuppressWarnings({ "unused", "null" })
  public void testNullPointerRead() {
    CSystem.forkAndJoin(() -> {
      Data d = null;
      int v = d.x;
    });
  }


  @Test(expected=NullPointerException.class)
  @SuppressWarnings("null")
  public void testNullPointerWrite() {
    CSystem.forkAndJoin(() -> {
      Data d = null;
      d.x = 0;
    });
  }


}
