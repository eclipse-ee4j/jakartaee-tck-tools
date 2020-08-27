/*
 * Copyright (c) 2007, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

/*
 * $Id$
 */

package com.sun.ts.tests.ejb30.bb.session.stateful.interceptor.listener.annotated;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.interceptor.InvocationContext;
import jakarta.interceptor.AroundInvoke;

public class InterceptorInstanceInterceptor {
  private int postConstructCount;

  private int aroundInvokeCount;

  public InterceptorInstanceInterceptor() {
    super();
  }

  @AroundInvoke
  public Object intercept1(InvocationContext ctx) throws Exception {
    aroundInvokeCount++;
    Object[] params = ctx.getParameters();
    int[] initials = (int[]) (params[0]);
    initials[0] = postConstructCount;
    initials[1] = aroundInvokeCount;
    return ctx.proceed();
  }

  @PostConstruct
  public void postConstruct(InvocationContext inv) {
    postConstructCount++;
  }
}