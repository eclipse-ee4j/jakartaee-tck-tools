/*
 * Copyright (c) 2014, 2020 Oracle and/or its affiliates and others.
 * All rights reserved.
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

package com.sun.ts.tests.websocket.ee.jakarta.websocket.session11.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.LinkedList;

import jakarta.websocket.EndpointConfig;
import jakarta.websocket.PongMessage;
import jakarta.websocket.Session;

import com.sun.ts.tests.websocket.common.client.ClientEndpoint;
import com.sun.ts.tests.websocket.common.client.SendMessageCallback;
import com.sun.ts.tests.websocket.common.client.WebSocketCommonClient.Entity;
import com.sun.ts.tests.websocket.common.stringbean.StringBean;
import com.sun.ts.tests.websocket.ee.jakarta.websocket.session11.common.StringList;
import com.sun.ts.tests.websocket.ee.jakarta.websocket.session11.common.TypeEnum;

public class MixedProgramaticEndpoint extends ClientEndpoint<String> {

  TypeEnum type;

  Entity entity;

  public MixedProgramaticEndpoint(TypeEnum type, Entity entity) {
    this.type = type;
    this.entity = entity;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void onOpen(Session session, EndpointConfig config) {
    switch (type) {
    case LINKEDLIST_HASHSET_TEXT:
      LinkedList<HashSet<String>> list = new LinkedList<>();

      Class<LinkedList<HashSet<String>>> clzLLHS = (Class<LinkedList<HashSet<String>>>) list
          .getClass();
      session.addMessageHandler(clzLLHS,
          new LinkedListHashSetMessageHandler(this));
      break;
    case LIST_TEXT:
      session.addMessageHandler(StringList.class,
          new StringListWholeMessageHandler(this));
      break;
    case STRINGBEAN:
      session.addMessageHandler(StringBean.class,
          new StringBeanMessageHandler(this));
      break;
    case STRING_WHOLE:
      session.addMessageHandler(String.class,
          new StringTextMessageHandler(this));
      break;
    case STRING_PARTIAL:
      session.addMessageHandler(String.class,
          new StringPartialMessageHandler(this));
      break;
    case READER:
      session.addMessageHandler(Reader.class, new ReaderMessageHandler(this));
      break;
    case PONG:
      session.addMessageHandler(PongMessage.class,
          new PongMessageHandler(this));
      // send pingmessage to receive pongmessage
      break;
    case BYTEBUFFER_WHOLE:
      session.addMessageHandler(ByteBuffer.class,
          new ByteBufferMessageHandler(this));
      break;
    case BYTEBUFFER_PARTIAL:
      session.addMessageHandler(ByteBuffer.class,
          new ByteBufferPartialMessageHandler(this));
      break;
    case BYTEARRAY_WHOLE:
      byte[] ba = new byte[0];
      Class<byte[]> baclz = (Class<byte[]>) ba.getClass();
      session.addMessageHandler(baclz, new ByteArrayMessageHandler(this));
      break;
    case BYTEARRAY_PARTIAL:
      ba = new byte[0];
      baclz = (Class<byte[]>) ba.getClass();
      session.addMessageHandler(baclz,
          new ByteArrayPartialMessageHandler(this));
      break;
    case INPUTSTREAM:
      session.addMessageHandler(InputStream.class,
          new InputStreamMessageHandler(this));
      break;
    default:
      break;
    }
    new SendMessageCallback(entity).onOpen(session, config);
  }

  public void sendMessage(Session session) {
    try {
      session.getBasicRemote().sendText(entity.getEntityAt(String.class, 0));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
