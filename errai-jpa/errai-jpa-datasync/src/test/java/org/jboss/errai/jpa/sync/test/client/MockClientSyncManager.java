/*
 * Copyright 2014 JBoss, by Red Hat, Inc
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

package org.jboss.errai.jpa.sync.test.client;

import java.util.Map;

import org.jboss.errai.common.client.api.ErrorCallback;
import org.jboss.errai.common.client.api.RemoteCallback;
import org.jboss.errai.jpa.sync.client.local.ClientSyncManager;

@SuppressWarnings("rawtypes")
public class MockClientSyncManager extends ClientSyncManager {
  private int coldSyncCallCount;
  private RemoteCallback onCompletion;

  @Override
  public void coldSync(String queryName, Class queryResultType, Map queryParams, RemoteCallback onCompletion,  ErrorCallback onError) {
    this.onCompletion = onCompletion;
    coldSyncCallCount++;
  }
  
  public int getColdSyncCallCount() {
    return coldSyncCallCount;
  }
}