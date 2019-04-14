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

import xyz.kvantum.server.api.util.Assert;
import xyz.kvantum.server.api.util.AutoCloseable;

import java.io.IOException;

/**
 * Handler for Gzip compression
 */
final class GzipHandler extends AutoCloseable {

    private final ReusableGzipOutputStream reusableGzipOutputStream;

    GzipHandler() {
        this.reusableGzipOutputStream = new ReusableGzipOutputStream();
    }

    @Override protected void handleClose() {
        try {
            this.reusableGzipOutputStream.close();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Compress bytes using gzip
     *
     * TODO optimize (though not super important since it's only used for fixed size)
     *
     * @param data Bytes to compress
     * @return GZIP compressed data
     * @throws IOException If compression fails
     */
    byte[] compress(final byte[] data) throws IOException {
        Assert.notNull(data);

        reusableGzipOutputStream.reset();
        reusableGzipOutputStream.write(data);
        reusableGzipOutputStream.finish();
        reusableGzipOutputStream.flush();

        final byte[] compressed = reusableGzipOutputStream.getData();

        Assert.equals(compressed != null && compressed.length > 0, true, "Failed to compress data");

        return compressed;
    }

    /**
     * Compresses the input bytes into the buffer and returns the new length
     * @param input
     * @param buffer
     * @param inputLength length of input to compress
     * @return compressed length (in buffer)
     */
    int compress(byte[] input, byte[] buffer, int inputLength) {
        // TODO compress using buffer
    }
}
