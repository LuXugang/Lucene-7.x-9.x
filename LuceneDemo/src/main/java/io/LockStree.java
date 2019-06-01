package io;

import org.apache.lucene.store.*;
import org.apache.lucene.util.SuppressForbidden;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

public class LockStree {

    static final String LOCK_FILE_NAME = "test.lock";

    @SuppressForbidden(reason = "System.out required: command line tool")
    @SuppressWarnings("try")
    public static void main(String[] args) throws Exception {
        if (args.length != 7) {
            System.out.println("Usage: java org.apache.lucene.store.LockStressTest myID verifierHost verifierPort lockFactoryClassName lockDirName sleepTimeMS count\n" +
                    "\n" +
                    "  myID = int from 0 .. 255 (should be unique for test process)\n" +
                    "  verifierHost = hostname that LockVerifyServer is listening on\n" +
                    "  verifierPort = port that LockVerifyServer is listening on\n" +
                    "  lockFactoryClassName = primary FSLockFactory class that we will use\n" +
                    "  lockDirName = path to the lock directory\n" +
                    "  sleepTimeMS = milliseconds to pause betweeen each lock obtain/release\n" +
                    "  count = number of locking tries\n" +
                    "\n" +
                    "You should run multiple instances of this process, each with its own\n" +
                    "unique ID, and each pointing to the same lock directory, to verify\n" +
                    "that locking is working correctly.\n" +
                    "\n" +
                    "Make sure you are first running LockVerifyServer.");
            System.exit(1);
        }

        int arg = 0;
        final int myID = 23;
        arg++;

        if (myID < 0 || myID > 255) {
            System.out.println("myID must be a unique int 0..255");
            System.exit(1);
        }

        final String verifierHost = "127.0.0.1";
        final int verifierPort = 8810;
        final String lockFactoryClassName = "io.NativeFSLockFactory";
        final Path lockDirPath = Paths.get("/Users/luxugang/project/Lucene-7.5.0/LuceneDemo/data");
        final int sleepTimeMS = 3000;
        final int count = 1;

        final LockFactory lockFactory = getNewLockFactory(lockFactoryClassName);
        // we test the lock factory directly, so we don't need it on the directory itsself (the directory is just for testing)
        final FSDirectory lockDir = new SimpleFSDirectory(lockDirPath, NoLockFactory.INSTANCE);
        final InetSocketAddress addr = new InetSocketAddress(verifierHost, verifierPort);
        System.out.println("Connecting to server " + addr +
                " and registering as client " + myID + "...");
        try (Socket socket = new Socket()) {
            socket.setReuseAddress(true);
            socket.connect(addr, 500000);
            final OutputStream out = socket.getOutputStream();
            final InputStream in = socket.getInputStream();

            out.write(myID);
            out.flush();
            LockFactory verifyLF = new VerifyingLockFactory(lockFactory, in, out);
            final Random rnd = new Random();

            // wait for starting gun
            if (in.read() != 43) {
                throw new IOException("Protocol violation");
            }

            for (int i = 0; i < count; i++) {
                try (final Lock l = verifyLF.obtainLock(lockDir, LOCK_FILE_NAME)) {
                    out.write(1);
                    out.flush();
                    if (rnd.nextInt(10) == 0) {
                        if (rnd.nextBoolean()) {
                            verifyLF = new VerifyingLockFactory(getNewLockFactory(lockFactoryClassName), in, out);
                        }
                        try (final Lock secondLock = verifyLF.obtainLock(lockDir, LOCK_FILE_NAME)) {
                            throw new IOException("Double obtain");
                        } catch (LockObtainFailedException loe) {
                            // pass
                        }
                    }
                    Thread.sleep(sleepTimeMS);
                } catch (LockObtainFailedException loe) {
                    // obtain failed
                }

                if (i % 500 == 0) {
                    System.out.println((i * 100. / count) + "% done.");
                }

                Thread.sleep(sleepTimeMS);
            }
        }

        System.out.println("Finished " + count + " tries.");
    }


    private static FSLockFactory getNewLockFactory(String lockFactoryClassName) throws IOException {
        // try to get static INSTANCE field of class
        try {
            return (FSLockFactory) Class.forName(lockFactoryClassName).getField("INSTANCE").get(null);
        } catch (ReflectiveOperationException e) {
            // fall-through
        }

        // try to create a new instance
        try {
            return Class.forName(lockFactoryClassName).asSubclass(FSLockFactory.class).newInstance();
        } catch (ReflectiveOperationException | ClassCastException e) {
            // fall-through
        }

        throw new IOException("Cannot get lock factory singleton of " + lockFactoryClassName);
    }
}
