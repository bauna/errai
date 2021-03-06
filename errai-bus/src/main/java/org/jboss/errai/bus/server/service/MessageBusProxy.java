/*
 * Copyright 2012 JBoss, by Red Hat, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.errai.bus.server.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.jboss.errai.bus.client.api.BusMonitor;
import org.jboss.errai.bus.client.api.QueueSession;
import org.jboss.errai.bus.client.api.SubscribeListener;
import org.jboss.errai.bus.client.api.Subscription;
import org.jboss.errai.bus.client.api.UnsubscribeListener;
import org.jboss.errai.bus.client.api.messaging.Message;
import org.jboss.errai.bus.client.api.messaging.MessageCallback;
import org.jboss.errai.bus.server.api.MessageQueue;
import org.jboss.errai.bus.server.api.QueueClosedListener;
import org.jboss.errai.bus.server.api.ServerMessageBus;
import org.jboss.errai.common.client.api.Assert;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

/**
 * @author Mike Brock
 */
public class MessageBusProxy implements ServerMessageBus {
  private List<Message> heldGlobalMessages = new ArrayList<Message>();
  private List<Message> heldMessages = new ArrayList<Message>();
  private Map<Message, Boolean> heldMessageFireListener = new LinkedHashMap<Message, Boolean>();
  private Multimap<String, MessageCallback> heldSubscribe = LinkedHashMultimap.create();
  private Multimap<String, MessageCallback> heldLocalSubscribe = LinkedHashMultimap.create();
//  private List<MessageListener> heldGlobalListener = new ArrayList<MessageListener>();
  private List<SubscribeListener> heldSubscribeListener = new ArrayList<SubscribeListener>();
  private List<UnsubscribeListener> heldUnsubscribeListener = new ArrayList<UnsubscribeListener>();
  private BusMonitor heldBusMonitor;

  private ServerMessageBus proxied;
  private volatile boolean proxyClosed;

  @Override
  public synchronized void sendGlobal(Message message) {
    Assert.notNull("message cannot be null", message);

    if (proxyClosed) {
      proxied.sendGlobal(message);
    }
    else {
      heldGlobalMessages.add(message);
    }
  }

  @Override
  public synchronized void send(Message message) {
    Assert.notNull("message cannot be null", message);

    if (proxyClosed) {
      proxied.send(message);
    }
    else {
      heldMessages.add(message);
    }
  }

  @Override
  public synchronized void send(Message message, boolean fireListeners) {
    Assert.notNull("message cannot be null", message);

    if (proxyClosed) {
      proxied.send(message, fireListeners);
    }
    else {
      heldMessageFireListener.put(message, fireListeners);
    }
  }

  @Override
  public synchronized Subscription subscribe(String subject, MessageCallback receiver) {
    Assert.notNull("message callback cannot be null", receiver);

    if (proxyClosed) {
      return proxied.subscribe(subject, receiver);
    }
    else {
      heldSubscribe.put(subject, receiver);
      return new Subscription() {
        @Override
        public void remove() {
          throw new IllegalStateException("cannot unsubscribe from a proxied MessageBus");
        }
      };
    }
  }

  @Override
  public synchronized Subscription subscribeLocal(String subject, MessageCallback receiver) {
    Assert.notNull("message callback cannot be null", receiver);
    
    if (proxyClosed) {
      return proxied.subscribeLocal(subject, receiver);
    }
    else {
      heldLocalSubscribe.put(subject, receiver);
      return new Subscription() {
        @Override
        public void remove() {
          throw new IllegalStateException("cannot unsubscribe from a proxied MessageBus");
        }
      };
    }
  }

  @Override
  public void unsubscribeAll(String subject) {
  }

  @Override
  public synchronized boolean isSubscribed(String subject) {
    if (proxyClosed) {
      return proxied.isSubscribed(subject);
    }
    else {
      return heldSubscribe.containsKey(subject) || heldLocalSubscribe.containsKey(subject);
    }
  }

  @Override
  public synchronized void addSubscribeListener(SubscribeListener listener) {
    Assert.notNull("subscribe listener cannot be null", listener);

    if (proxyClosed) {
      proxied.addSubscribeListener(listener);
    }
    else {
      heldSubscribeListener.add(listener);
    }
  }

  @Override
  public synchronized void addUnsubscribeListener(UnsubscribeListener listener) {
    Assert.notNull("unsubscribe listener cannot be null", listener);

    if (proxyClosed) {
      proxied.addUnsubscribeListener(listener);
    }
    else {
      heldUnsubscribeListener.add(listener);
    }
  }

  @Override
  public synchronized MessageQueue getQueue(QueueSession session) {
    return proxied.getQueue(session);
  }

  @Override
  public void closeQueue(String sessionId) {
    proxied.closeQueue(sessionId);
  }

  @Override
  public void closeQueue(MessageQueue queue) {
    proxied.closeQueue(queue);
  }

  @Override
  public ExecutorService getScheduler() {
    return proxied.getScheduler();
  }

  @Override
  public void addQueueClosedListener(QueueClosedListener listener) {
    proxied.addQueueClosedListener(listener);
  }

  @Override
  public void configure(ErraiServiceConfigurator service) {
    proxied.configure(service);
  }

  @Override
  public synchronized Collection<MessageCallback> getReceivers(String subject) {
    if (proxyClosed) {
      return proxied.getReceivers(subject);
    }
    else {
      return new ArrayList<MessageCallback>(heldSubscribe.values());
    }
  }

  @Override
  public synchronized boolean hasRemoteSubscriptions(String subject) {
    return proxied.hasRemoteSubscriptions(subject);
  }

  @Override
  public boolean hasRemoteSubscription(String sessionId, String subject) {
    return proxied.hasRemoteSubscription(sessionId, subject);
  }

  @Override
  public Map<QueueSession, MessageQueue> getMessageQueues() {
    return proxied.getMessageQueues();
  }

  @Override
  public MessageQueue getQueueBySession(String id) {
    return proxied.getQueueBySession(id);
  }

  @Override
  public QueueSession getSessionBySessionId(String id) {
    return proxied.getSessionBySessionId(id);
  }

  @Override
  public void associateNewQueue(QueueSession oldSession, QueueSession newSession) {
    proxied.associateNewQueue(oldSession, newSession);
  }

  @Override
  public void stop() {
    proxied.stop();
  }

  @Override
  public Message getDeadLetterMessage(String messageId) {
    return proxied.getDeadLetterMessage(messageId);
  }

  @Override
  public boolean removeDeadLetterMessage(String messageId) {
     return proxied.removeDeadLetterMessage(messageId);
  }

  @Override
  public synchronized void attachMonitor(BusMonitor monitor) {
    this.heldBusMonitor = monitor;
  }

  public synchronized void closeProxy(ServerMessageBus bus) {
    Assert.notNull("message bus reference cannot be null", bus);

    if (proxied != null) {
      throw new IllegalStateException("proxy already closed");
    }

    this.proxied = bus;
    this.proxyClosed = true;

    if (heldBusMonitor != null) {
      bus.attachMonitor(heldBusMonitor);
    }

    for (Map.Entry<String, MessageCallback> entry : heldSubscribe.entries()) {
      bus.subscribe(entry.getKey(), entry.getValue());
    }

    for (Map.Entry<String, MessageCallback> entry : heldLocalSubscribe.entries()) {
      bus.subscribeLocal(entry.getKey(), entry.getValue());
    }

    for (SubscribeListener subscribeListener : heldSubscribeListener) {
      bus.addSubscribeListener(subscribeListener);
    }

    for (UnsubscribeListener unsubscribeListener : heldUnsubscribeListener) {
      bus.addUnsubscribeListener(unsubscribeListener);
    }

    for (Message message : heldMessages) {
      bus.send(message);
    }

    for (Message message : heldGlobalMessages) {
      bus.sendGlobal(message);
    }

    for (Map.Entry<Message, Boolean> entry : heldMessageFireListener.entrySet()) {
      bus.send(entry.getKey(), entry.getValue());
    }

    this.heldBusMonitor = null;
    this.heldSubscribe = null;
    this.heldLocalSubscribe = null;
    this.heldSubscribeListener = null;
    this.heldUnsubscribeListener = null;
  //  this.heldGlobalListener = null;
    this.heldMessages = null;
    this.heldGlobalMessages = null;
    this.heldMessageFireListener = null;
  }
}
