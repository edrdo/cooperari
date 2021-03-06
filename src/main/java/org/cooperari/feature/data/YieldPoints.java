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

package org.cooperari.feature.data;


import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.cooperari.core.CThread;

/**
 * AspectJ instrumentation for data access yield points.
 * 
 * @since 0.2
 */
@Aspect
public class YieldPoints {

  /**
   * Constant used to denote a static field access.
   */
  private static final String STATIC_FIELD = "<static>";
  
  /**
   * Advice executed before static field read accesses.
   * @param thisJoinPoint Join point.
   */
  @Before("get(static * *.*)")
  public void beforeGetStaticField(JoinPoint thisJoinPoint) {
    CThread t = CThread.intercept(thisJoinPoint);
    if (t != null) {
      Read.before(t, STATIC_FIELD, thisJoinPoint.getSignature().getName());
    }
  }
  
  /**
   * Advice executed after static field read accesses.
   * @param thisJoinPoint Join point.
   */
  @After("get(static * *.*)")
  public void afterGetStaticField(JoinPoint thisJoinPoint) {
    CThread t = CThread.self();
    if (t != null) {
      Read.after(t, STATIC_FIELD, thisJoinPoint.getSignature().getName());
    }
  }
  
  /**
   * Advice executed before field read accesses.
   * @param thisJoinPoint Join point.
   * @param o Target object.
   */
  @Before("get(* *.*) && target(o)")
  public void beforeGetField(JoinPoint thisJoinPoint, Object o) {
    CThread t = CThread.intercept(thisJoinPoint);
    if (o != null && t != null) {
      Read.before(t, o, thisJoinPoint.getSignature().getName());
    }
  }
  
  /**
   * Advice executed after field read accesses.
   * @param thisJoinPoint Join point.
   * @param o Target object.
   */
  @After("get(* *.*) && target(o)")
  public void afterGetField(JoinPoint thisJoinPoint, Object o) {
    if (o == null) {
      throw new NullPointerException();
    }
    CThread t = CThread.self();
    if (t != null) {
      Read.after(t, o, thisJoinPoint.getSignature().getName());
    }
  }
  
  /**
   * Advice executed before static field write accesses.
   * @param thisJoinPoint Join point.
   */
  @Before("set(static * *.*)")
  public void beforeSetStaticField(JoinPoint thisJoinPoint) {
    CThread t = CThread.intercept(thisJoinPoint);
    if (t != null) {
      Write.before(t, STATIC_FIELD, thisJoinPoint.getSignature().getName());
    }
  }
  
  /**
   * Advice executed after static field write accesses.
   * @param thisJoinPoint Join point.
   */
  @After("set(static * *.*)")
  public void afterSetStaticField(JoinPoint thisJoinPoint) {
    CThread t = CThread.self();
    if (t != null) {
      Write.after(t, STATIC_FIELD, thisJoinPoint.getSignature().getName());
    }
  }
  /**
   * Advice executed before field write accesses.
   * @param thisJoinPoint Join point.
   * @param o Target object.
   */
  @Before("set(* *.*) && target(o)")
  public void beforeSetField(JoinPoint thisJoinPoint, Object o) {
    CThread t = CThread.intercept(thisJoinPoint);
    if (o != null && t != null) {
      Write.before(t, o, thisJoinPoint.getSignature().getName());
    }
  }
  
  /**
   * Advice executed after field write accesses.
   * @param thisJoinPoint Join point.
   * @param o Target object.
   */
  @After("set(* *.*) && target(o)")
  public void afterSetField(JoinPoint thisJoinPoint, Object o) {
    if (o == null) {
      throw new NullPointerException();
    }
    CThread t = CThread.self();
    if (t != null) {
      Write.after(t, o, thisJoinPoint.getSignature().getName());
    }
  }

  
  /**
   * Advice executed before {@code org.cooperari.CArray.cRead()}.
   * @param thisJoinPoint Join point.
   * @param array Array object.
   * @param index Index.
   * @see org.cooperari.CArray
   */
  @Before("call(* org.cooperari.CArray.cRead(*,int)) && args(array,index)")
  public void beforeArrayRead(JoinPoint thisJoinPoint, Object array, int index) {
    CThread t = CThread.intercept(thisJoinPoint);
    if (t != null) {
      Read.before(t, array, index);
    }
  }
  /**
   * Advice executed after {@code org.cooperari.CArray.cRead()}.
   * @param thisJoinPoint Join point.
   * @param array Array object.
   * @param index Index.
   * @see org.cooperari.CArray
   */
  @After("call(* org.cooperari.CArray.cRead(*,int)) && args(array,index)")
  public void afterArrayRead(JoinPoint thisJoinPoint, Object array, int index) {
    CThread t = CThread.self();
    if (t != null) {
      Read.after(t, array, index);
    }
  }
  /**
   * Advice executed before {@code org.cooperari.CArray.cWrite()}.
   * @param thisJoinPoint Join point.
   * @param array Array object.
   * @param index Index.
   * @param value Value.
   * @see org.cooperari.CArray
   */
  @Before("call(* org.cooperari.CArray.cWrite(*,int,*)) && args(array,index,value)")
  public void beforeArrayWrite(JoinPoint thisJoinPoint, Object array, int index, Object value) {
    CThread t = CThread.intercept(thisJoinPoint);
    if (t != null) {
      Write.before(t, array, index);
    }
  }
  /**
   * Advice executed after {@code org.cooperari.CArray.cWrite()}.
   * @param thisJoinPoint Join point.
   * @param array Array object.
   * @param index Index.
   * @param value Value.
   * @see org.cooperari.CArray
   */
  @After("call(* org.cooperari.CArray.cWrite(*,int,*)) && args(array,index,value)")
  public void afterArrayWrite(JoinPoint thisJoinPoint, Object array, int index, Object value) {
    CThread t = CThread.self();
    if (t != null) {
      Write.after(t, array, index);
    }
  }
}
