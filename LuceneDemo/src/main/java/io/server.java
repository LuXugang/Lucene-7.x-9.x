package io;

import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.SuppressForbidden;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class server {

    @SuppressForbidden(reason = "System.out required: command line tool")
    public static void main(String[] args) throws Exception {

        if (args.length != 2) {
            System.out.println("Usage: java org.apache.lucene.store.LockVerifyServer bindToIp clients\n");
            System.exit(1);
        }

        int arg = 0;
        final String hostname = args[arg++];
        final int maxClients = Integer.parseInt(args[arg++]);

        try (final ServerSocket s = new ServerSocket()) {
            s.setReuseAddress(true);
            s.setSoTimeout(3000000); // initially 30 secs to give clients enough time to startup
            s.bind(new InetSocketAddress(hostname, 8810));
            final InetSocketAddress localAddr = (InetSocketAddress) s.getLocalSocketAddress();
            System.out.println("Listening on " + localAddr + "...");

            // we set the port as a sysprop, so the ANT task can read it. For that to work, this server must run in-process:
            System.setProperty("lockverifyserver.port", Integer.toString(9980));

            final Object localLock = new Object();
            final int[] lockedID = new int[1];
            lockedID[0] = -1;
            final CountDownLatch startingGun = new CountDownLatch(1);
            final Thread[] threads = new Thread[maxClients];

            for (int count = 0; count < maxClients; count++) {
                System.out.println("wait");
                final Socket cs = s.accept();
                System.out.println("accept");
                threads[count] = new Thread() {
                    @Override
                    public void run() {
                        try (InputStream in = cs.getInputStream(); OutputStream os = cs.getOutputStream()) {
                            final int id = in.read();
                            System.out.printf("id is: "+id+"");
                            if (id < 0) {
                                throw new IOException("Client closed connection before communication started.");
                            }

                            startingGun.await(3, TimeUnit.SECONDS);
                            os.write(43);
                            os.flush();

                            while(true) {
                                final int command = in.read();
                                if (command < 0) {
                                    return; // closed
                                }

                                synchronized(localLock) {
                                    final int currentLock = lockedID[0];
                                    if (currentLock == -2) {
                                        return; // another thread got error, so we exit, too!
                                    }
                                    switch (command) {
                                        case 1:
                                            // Locked
                                            if (currentLock != -1) {
                                                lockedID[0] = -2;
                                                throw new IllegalStateException("id " + id + " got lock, but " + currentLock + " already holds the lock");
                                            }
                                            lockedID[0] = id;
                                            break;
                                        case 0:
                                            // Unlocked
                                            if (currentLock != id) {
                                                lockedID[0] = -2;
                                                throw new IllegalStateException("id " + id + " released the lock, but " + currentLock + " is the one holding the lock");
                                            }
                                            lockedID[0] = -1;
                                            break;
                                        default:
                                            throw new RuntimeException("Unrecognized command: " + command);
                                    }
                                    os.write(command);
                                    os.flush();
                                }
                            }
                        } catch (RuntimeException | Error e) {
                            throw e;
                        } catch (Exception ioe) {
                            throw new RuntimeException(ioe);
                        } finally {
                            IOUtils.closeWhileHandlingException(cs);
                        }
                    }
                };
                threads[count].start();
            }

            // start
            System.out.println("All clients started, fire gun...");
            startingGun.countDown();

            // wait for all threads to finish
            for (Thread t : threads) {
                t.join();
            }

            // cleanup sysprop
            System.clearProperty("lockverifyserver.port");

            System.out.println("Server terminated.");
        }
    }
}
