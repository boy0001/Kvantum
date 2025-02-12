/*
 *    _  __                     _
 *    | |/ /__   __ __ _  _ __  | |_  _   _  _ __ ___
 *    | ' / \ \ / // _` || '_ \ | __|| | | || '_ ` _ \
 *    | . \  \ V /| (_| || | | || |_ | |_| || | | | | |
 *    |_|\_\  \_/  \__,_||_| |_| \__| \__,_||_| |_| |_|
 *
 *    Copyright (C) 2019 Alexander Söderberg
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
package xyz.kvantum.server.api.config;

import com.intellectualsites.configurable.ConfigurationImplementation;
import com.intellectualsites.configurable.annotations.ConfigSection;
import com.intellectualsites.configurable.annotations.Configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This is the configuration implementation that is meant to control all easily accessible variables for the web server.
 * This is generated into ".kvantum\config\server.yml" and is loaded on runtime
 */
@Configuration(implementation = ConfigurationImplementation.YAML, name = "server")
public class CoreConfig {

    public static int port = 80;
    public static String webAddress = "localhost";
    public static String logPrefix = "Web";
    public static boolean verbose = false;
    public static boolean disableViews = false;
    public static boolean loadWebJars = true;
    public static boolean autoDetectViews = false;
    public static boolean debug = true;
    public static boolean gzip = true;
    public static boolean enableSecurityManager = true;
    public static boolean enableInputThread = true;
    public static boolean exitOnStop = true;
    public static boolean hideIps = false;
    public static int requestHandlerSortRate = 60; // Once every minute

    public static long timeout = 15;
    public static String timeoutUnit = "SECONDS";
    // Transient makes sure that this is ignored by the config factory
    private transient static boolean preConfigured = false;

    public static boolean isPreConfigured() {
        return preConfigured;
    }

    public static void setPreConfigured(final boolean preConfigured) {
        CoreConfig.preConfigured = preConfigured;
    }

    public enum TemplatingEngine {
        CRUSH, VELOCITY, JTWIG, NONE
    }


    @ConfigSection(name = "Throttling") public static class Throttle {

        public static String timeUnit = "MINUTES";
        public static int limit = 1000;
        public static long timeSpan = 1;
    }


    @ConfigSection(name = "internalAddons") public static class InternalAddons {

        public static List<String> disabled = new ArrayList<>();
    }


    @ConfigSection(name = "logging") public static class Logging {

        public static String logFormat =
            "&0[&c${applicationPrefix}&0]" + "&0[&c${logPrefix}&0]" + "&0[&c${thread}&0]"
                + "&0[&c${timeStamp}&0] " + "&r${message}";
    }


    @ConfigSection(name = "MemoryGuard") public static class MemoryGuard {

        public static long runEveryMillis = 600000; // Every 10 minutes
    }


    @ConfigSection(name = "sessions") public static class Sessions {
        public static boolean enableDb = true;
        public static int sessionTimeout = 86400;
    }


    @ConfigSection(name = "Pools") public static class Pools {

        public static int httpBossGroupThreads = 0;
        public static int httpWorkerGroupThreads = 0;

        public static int httpsBossGroupThreads = 0;
        public static int httpsWorkerGroupThreads = 0;

        public static int gzipHandlers = 2;
        public static int md5Handlers = 2;
    }


    @ConfigSection(name = "templates") public static class Templates {

        public static String engine = TemplatingEngine.CRUSH.name();

        public static List<String> applyTemplates = Collections.singletonList("ALL");

        public static boolean status(final TemplatingEngine engine) {
            return CoreConfig.TemplatingEngine.valueOf(Templates.engine).equals(engine);
        }
    }


    @ConfigSection(name = "ssl") public static class SSL {

        public static boolean enable = false;
        public static int port = 443;
        public static String keyStore = "keyStore";
        public static String keyStorePassword = "password";

    }


    @ConfigSection(name = "buffer") public static class Buffer {

        public static int in = 100_000;
        public static int out = 100_000;
        public static int files = 100_000;
    }


    @ConfigSection(name = "limits") public static class Limits {

        public static int limitRequestLineSize = 8190;
        public static int limitPostBasicSize = 8190;

    }


    @ConfigSection(name = "cache") public static class Cache {
        public static int cachedIncludesExpiry = 60 * 60; // 1h
        public static int cachedIncludesMaxItems = 1000;
        public static int cachedAccountsExpiry = 60 * 30;
        public static int cachedAccountsMaxItems = 1000;
        public static int cachedAccountIdsExpiry = 60 * 60 * 24;
        public static int cachedAccountIdsMaxItems = 1000;
        public static int cachedBodiesExpiry = 60 * 60;
        public static int cachedBodiesMaxItems = 1000;
        public static int cachedFilesExpiry = 60 * 60 * 24;
        public static int cachedFilesMaxItems = 1000;
        public static int cachedSessionsMaxItems = 1000;
        public static int cachedQueryMinimumAccesses = 10;
        public static int cachedFilesMaxSize = 1024 * 1024; // Default max size is 1MB
    }


    @ConfigSection(name = "mongodb") public static class MongoDB {

        public static String uri = "mongodb://localhost:27017";

        public static String dbSessions = "isites";
        public static String dbMorphia = "isites";
        public static String collectionSessions = "sessions";
    }


    @ConfigSection(name = "application") public static class Application {
        public static String main = "";
        public static String databaseImplementation = "sqlite";
    }


    @ConfigSection(name = "middleware") public static class Middleware {

        public static String loginRedirect = "login";

    }

}
