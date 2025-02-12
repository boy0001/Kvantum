package xyz.kvantum.server.implementation.compression;

import xyz.kvantum.server.implementation.compression.ParallelGZIPEnvironment;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * A multi-threaded version of {@link GZIPOutputStream}.
 *
 * @author shevek
 */
public class ParallelGZIPOutputStream extends FilterOutputStream {

    private static final int GZIP_MAGIC = 0x8b1f;
    private static final int SIZE = 64 * 1024;

    @NonNull
    private static Deflater newDeflater() {
        return new Deflater(Deflater.BEST_SPEED, true);
    }

    @NonNull
    private static DeflaterOutputStream newDeflaterOutputStream(@NonNull OutputStream out, @NonNull Deflater deflater) {
        return ParallelGZIPEnvironment.newDeflaterOutputStream(out, deflater);
    }

    /* Allow write into byte[] directly */
    private static class ByteArrayOutputStreamExposed extends ByteArrayOutputStream {

        public ByteArrayOutputStreamExposed(int size) {
            super(size);
        }

        public void writeTo(@NonNull byte[] buf) throws IOException {
            System.arraycopy(this.buf, 0, buf, 0, count);
        }
    }

    private static class State {

        private final Deflater def = newDeflater();
        private final ByteArrayOutputStreamExposed buf = new ByteArrayOutputStreamExposed(SIZE + (SIZE >> 3));
        private final DeflaterOutputStream str = newDeflaterOutputStream(buf, def);
    }

    /** This ThreadLocal avoids the recycling of a lot of memory, causing lumpy performance. */
    private static final ThreadLocal<State> STATE = new ThreadLocal<State>() {
        @Override
        protected State initialValue() {
            return new State();
        }
    };

    private static class Block implements Callable<Block> {

        // private final int index;
        private byte[] buf = new byte[SIZE + (SIZE >> 3)];
        private int buf_length = 0;

        /*
         public Block( int index) {
         this.index = index;
         }
         */
        // Only on worker thread
        @Override
        public Block call() throws IOException {
            // LOG.info("Processing " + this + " on " + Thread.currentThread());

            State state = STATE.get();
            // ByteArrayOutputStream buf = new ByteArrayOutputStream(in.length);   // Overestimate output size required.
            // DeflaterOutputStream def = newDeflaterOutputStream(buf);
            state.def.reset();
            state.buf.reset();
            state.str.write(buf, 0, buf_length);
            state.str.flush();

            // int in_length = buf_length;
            int out_length = state.buf.size();
            if (out_length > buf.length) {
                this.buf = new byte[out_length];
            }
            // System.out.println("Compressed " + in_length + " to " + out_length + " bytes.");
            this.buf_length = out_length;
            state.buf.writeTo(buf);

            // return Arrays.copyOf(in, in_length);
            return this;
        }

        @Override
        public String toString() {
            return "Block" /* + index */ + "(" + buf_length + "/" + buf.length + " bytes)";
        }
    }

    
    private static int getThreadCount(@NonNull ExecutorService executor) {
        if (executor instanceof ThreadPoolExecutor)
            return ((ThreadPoolExecutor) executor).getMaximumPoolSize();
        return Runtime.getRuntime().availableProcessors();
    }

    // TODO: Share, daemonize.
    private final ExecutorService executor;
    private final CRC32 crc = new CRC32();
    private final int emitQueueSize;
    private final BlockingQueue<Future<Block>> emitQueue;
    @NonNull
    private Block block = new Block();
    private ArrayDeque<Block> freeBlocks = new ArrayDeque<>();
    /** Used as a sentinel for 'closed'. */
    private long bytesWritten = 0;

    // Master thread only
    @Deprecated // Doesn't really use the given number of threads.
    public ParallelGZIPOutputStream(@NonNull OutputStream out, @NonNull ExecutorService executor,  int nthreads) throws IOException {
        super(out);
        this.executor = executor;
        // Some blocks compress faster than others; allow a long enough queue to keep all CPUs busy at least for a bit.
        this.emitQueueSize = nthreads * 3;
        this.emitQueue = new ArrayBlockingQueue<Future<Block>>(emitQueueSize);
        writeHeader();
    }

    public void reset() throws IOException {
        crc.reset();
        bytesWritten = 0;
        writeHeader();
    }

    /**
     * Creates a ParallelGZIPOutputStream
     * using {@link ParallelGZIPEnvironment#getSharedThreadPool()}.
     *
     * @param out the eventual output stream for the compressed data.
     * @throws IOException if it all goes wrong.
     */
    @Deprecated // Doesn't really use the given number of threads.
    public ParallelGZIPOutputStream(@NonNull OutputStream out,  int nthreads) throws IOException {
        this(out, ParallelGZIPEnvironment.getSharedThreadPool(), nthreads);
    }

    public ParallelGZIPOutputStream(@NonNull OutputStream out, @NonNull ExecutorService executor) throws IOException {
        this(out, executor, getThreadCount(executor));
    }

    /**
     * Creates a ParallelGZIPOutputStream
     * using {@link ParallelGZIPEnvironment#getSharedThreadPool()}.
     *
     * @param out the eventual output stream for the compressed data.
     * @throws IOException if it all goes wrong.
     */
    public ParallelGZIPOutputStream(@NonNull OutputStream out) throws IOException {
        this(out, ParallelGZIPEnvironment.getSharedThreadPool());
    }

    /*
     * @see http://www.gzip.org/zlib/rfc-gzip.html#file-format
     */
    private void writeHeader() throws IOException {
        out.write(new byte[]{
                (byte) GZIP_MAGIC, // ID1: Magic number (little-endian short)
                (byte) (GZIP_MAGIC >> 8), // ID2: Magic number (little-endian short)
                Deflater.DEFLATED, // CM: Compression method
                0, // FLG: Flags (byte)
                0, 0, 0, 0, // MTIME: Modification time (int)
                0, // XFL: Extra flags
                3 // OS: Operating system (3 = Linux)
        });
    }

    // Master thread only
    @Override
    public void write(int b) throws IOException {
        byte[] single = new byte[1];
        single[0] = (byte) (b & 0xFF);
        write(single);
    }

    // Master thread only
    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    // Master thread only
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        crc.update(b, off, len);
        bytesWritten += len;

        while (len > 0) {
            final byte[] blockBuf = block.buf;
            // assert block.in_length < block.in.length
            int capacity = SIZE - block.buf_length; // Make sure we don't grow the block buf repeatedly.
            if (len >= capacity) {
                System.arraycopy(b, off, blockBuf, block.buf_length, capacity);
                block.buf_length += capacity;   // == block.in.length
                off += capacity;
                len -= capacity;
                submit();
            } else {
                System.arraycopy(b, off, blockBuf, block.buf_length, len);
                block.buf_length += len;
                // off += len;
                // len = 0;
                break;
            }
        }
    }

    // Master thread only
    private void submit() throws IOException {
        emitUntil(emitQueueSize - 1);
        emitQueue.add(executor.submit(block));
        Block b = freeBlocks.poll();
        if (b == null) {
            b = new Block();
        }
        block = b;
    }

    // Emit If Available - submit always
    // Emit At Least one - submit when executor is full
    // Emit All Remaining - flush(), close()
    // Master thread only
    private void tryEmit() throws IOException, InterruptedException, ExecutionException {
        for (;;) {
            Future<Block> future = emitQueue.peek();
            // LOG.info("Peeked future " + future);
            if (future == null)
                return;
            if (!future.isDone())
                return;
            // It's an ordered queue. This MUST be the same element as above.
            Block b = emitQueue.remove().get();
            // System.out.println("Chance-emitting block " + b);
            out.write(b.buf, 0, b.buf_length);
            b.buf_length = 0;
            freeBlocks.add(b);
        }
    }

    // Master thread only
    /** Emits any opportunistically available blocks. Furthermore, emits blocks until the number of executing tasks is less than taskCountAllowed. */
    private void emitUntil( int taskCountAllowed) throws IOException {
        try {
            while (emitQueue.size() > taskCountAllowed) {
                // LOG.info("Waiting for taskCount=" + emitQueue.size() + " -> " + taskCountAllowed);
                Block b = emitQueue.remove().get();  // Valid because emitQueue.size() > 0
                // System.out.println("Force-emitting block " + b);
                out.write(b.buf, 0, b.buf_length);  // Blocks until this task is done.
                b.buf_length = 0;
                freeBlocks.add(b);
            }
            // We may have achieved more opportunistically available blocks
            // while waiting for a block above. Let's emit them here.
            tryEmit();
        } catch (ExecutionException e) {
            throw new IOException(e);
        } catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
    }

    // Master thread only
    @Override
    public void flush() throws IOException {
        // LOG.info("Flush: " + block);
        if (block.buf_length > 0)
            submit();
        emitUntil(0);
        super.flush();
    }

    private final ByteBuffer footer = ByteBuffer.allocate(8);

    // Master thread only
    @Override
    public void close() throws IOException {
        // LOG.info("Closing: bytesWritten=" + bytesWritten);
        if (bytesWritten >= 0) {
            flush();

            out.write(3);
            out.write(0);

            footer.mark();
            footer.order(ByteOrder.LITTLE_ENDIAN);
            // LOG.info("CRC is " + crc.getValue());
            footer.putInt((int) crc.getValue());
            footer.putInt((int) (bytesWritten % 4294967296L));
            out.write(footer.array()); // allocate() guarantees a backing array.
            footer.reset();
            // LOG.info("trailer is " + Arrays.toString(buf.array()));

            out.flush();
            out.close();

            bytesWritten = Integer.MIN_VALUE;
            // } else {
            // LOG.warn("Already closed.");
        }
    }
}