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
package xyz.kvantum.server.implementation;

import de.jungblut.datastructure.AsyncBufferedOutputStream;
import xyz.kvantum.server.api.event.Listener;
import xyz.kvantum.server.api.logging.Logger;
import xyz.kvantum.server.api.response.FinalizedResponse;
import xyz.kvantum.server.api.util.Assert;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * Streams to log/access.log
 */
final class AccessLogStream extends PrintStream {

    AccessLogStream(final File logFolder) throws FileNotFoundException {
        super(new AsyncBufferedOutputStream(
            new FileOutputStream(new File(logFolder, "access.log"), true)));
    }

    @Listener @SuppressWarnings("unused")
    private void onRequestFinish(final FinalizedResponse response) {
        final String logString = response.toLogString();
        Assert.notNull(logString);
        Logger.access(logString);
        this.println(logString);
        this.flush();
    }

}
