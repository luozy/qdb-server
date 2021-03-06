/*
 * Copyright 2013 David Tinker
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

package io.qdb.server;

import com.google.common.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;
import io.qdb.kvstore.KeyValueStore;
import io.qdb.server.controller.Router;
import io.qdb.server.model.ModelObject;
import io.qdb.server.repo.Repository;
import io.qdb.server.repo.*;
import org.simpleframework.http.core.Container;
import org.simpleframework.transport.connect.Connection;

import java.io.File;
import java.util.Map;

/**
 * Standard server configuration.
 */
public class QdbServerModule extends AbstractModule {

    private final Config cfg;

    public QdbServerModule() {
        File local = new File(System.getProperty("qdb.conf", "qdb"));
        cfg = ConfigFactory.parseFileAnySyntax(local).withFallback(ConfigFactory.load());
    }

    public Config getCfg() {
        return cfg;
    }

    @Override
    protected void configure() {
        bindProperties();
        bind(EventBus.class).toInstance(new EventBus());
        bind(Connection.class).toProvider(ConnectionProvider.class);
        bind(Repository.class).to(RepositoryImpl.class);
        bind(new TypeLiteral<KeyValueStore<String, ModelObject>>(){}).toProvider(KeyValueStoreProvider.class);
        bind(Container.class).to(Router.class);
    }

    /**
     * Create named bindings for all our configuration properties.
     */
    protected void bindProperties() {
        for (Map.Entry<String, ConfigValue> entry : cfg.entrySet()) {
            ConfigValue value = entry.getValue();
            if (value.origin().url() != null) {
                String key = entry.getKey();
                Named named = Names.named(key);
                Object v = value.unwrapped();
                if (v instanceof String) {
                    if (key.endsWith("Backoff")) {
                        BackoffPolicy bp;
                        try {
                            bp = BackoffPolicy.Standard.parse((String)v);
                        } catch (Exception e) {
                            throw new IllegalArgumentException("Invalid " + key + " [" + v + "]: " + e.getMessage());
                        }
                        bind(Key.get(BackoffPolicy.class, named)).toInstance(bp);
                    } else {
                        bind(Key.get(String.class, named)).toInstance((String)v);
                    }
                } else if (v instanceof Integer) {
                    bind(Key.get(Integer.class, named)).toInstance((Integer)v);
                } else if (v instanceof Boolean) {
                    bind(Key.get(Boolean.class, named)).toInstance((Boolean)v);
                } else {
                    bind(Key.get(Object.class, named)).toInstance(v);
                }
            }
        }
    }
}
