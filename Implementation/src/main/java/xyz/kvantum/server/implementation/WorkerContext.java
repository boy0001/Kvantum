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

import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import xyz.kvantum.server.api.config.CoreConfig;
import xyz.kvantum.server.api.config.Message;
import xyz.kvantum.server.api.core.Kvantum;
import xyz.kvantum.server.api.core.ServerImplementation;
import xyz.kvantum.server.api.core.WorkerProcedure;
import xyz.kvantum.server.api.io.KvantumOutputStream;
import xyz.kvantum.server.api.logging.Logger;
import xyz.kvantum.server.api.request.AbstractRequest;
import xyz.kvantum.server.api.response.Header;
import xyz.kvantum.server.api.response.ResponseBody;
import xyz.kvantum.server.api.socket.SocketContext;
import xyz.kvantum.server.api.util.AsciiString;
import xyz.kvantum.server.api.views.RequestHandler;

import java.nio.charset.StandardCharsets;

import static xyz.kvantum.server.implementation.KvantumServerHandler.CLOSE;
import static xyz.kvantum.server.implementation.KvantumServerHandler.CONNECTION;
import static xyz.kvantum.server.implementation.KvantumServerHandler.KEEP_ALIVE;

@Getter @Setter @RequiredArgsConstructor final class WorkerContext {

    private static final String CONTENT_TYPE = "content_type";
    private static final byte[] EMPTY = "NULL".getBytes(StandardCharsets.UTF_8);
    private static final AsciiString ACCEPT_ENCODING = AsciiString.of("Accept-Encoding");
    private static final AsciiString GZIP = AsciiString.of("gzip");
    private static final AsciiString NULL = null;

    private final Kvantum server;
    private final WorkerProcedure.WorkerProcedureInstance workerProcedureInstance;

    private final KvantumServerHandler kvantumServerHandler;

    private final Object lock = new Object();
    private RequestHandler requestHandler;
    private AbstractRequest request;
    private ResponseBody body;
    private KvantumOutputStream responseStream;
    private boolean gzip = false;
    private SocketContext socketContext;
    private ChannelHandlerContext lastContext;

    private volatile boolean finished;

    void handleReadCompletion() {
        synchronized (this.lock) {
            if (this.finished) {
                return;
            }
            try {
                this.finished = true;
                this.getRequest().onCompileFinish();
                if (CoreConfig.debug) {
                    this.getRequest().dumpRequest();
                }
                this.handleResponse(this.lastContext);
                if (getRequest().getHeaders().getOrDefault(CONNECTION, CLOSE).equalsIgnoreCase(KEEP_ALIVE)) {
                    this.kvantumServerHandler.reused = true;
                    this.kvantumServerHandler.createNew(getSocketContext());
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressWarnings("unused") private void handleResponse(final ChannelHandlerContext context) {
        final ResponseTask responseTask = new ResponseTask(context, this);
        // Either we reuse the old thread, or we create a new one
        if (Thread.currentThread().getName().startsWith("kvantum-")) {
            responseTask.run();
        } else {
            ServerImplementation.getImplementation().getExecutorService().execute(responseTask);
        }
    }

    /**
     * TODO: I am fairly confident this would be better somewhere else
     *
     * <p> Determine whether or not GZIP compression should be used. This depends on two things: <ol> <li>If GZIP
     * compression is enabled in {@link CoreConfig}</li> <li>If the client has sent a "Accept-Encoding" header</li>
     * </ol> </p> <p> The value can be fetched using {@link #isGzip()} </p>
     */
    void determineGzipStatus() {
        if (CoreConfig.gzip) {
            if (!body.supportsGzip()) {
                if (CoreConfig.debug) {
                    Logger.debug("Response does not support GZIP encoding");
                }
            } else if (request.getHeader(ACCEPT_ENCODING).contains("gzip")) {
                this.gzip = true;
                body.getHeader().set(Header.HEADER_CONTENT_ENCODING, GZIP);
            } else if (CoreConfig.debug) {
                Message.CLIENT_NOT_ACCEPTING_GZIP.log(request.getHeaders());
                body.getHeader().set(Header.HEADER_CONTENT_ENCODING, NULL);
            }
        }
    }

}
