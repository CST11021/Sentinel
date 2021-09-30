package com.alibaba.csp.sentinel.dashboard;

import org.apache.curator.test.TestingServer;

import java.io.File;
import java.io.IOException;

/**
 * 内存版的内嵌Zookeeper.
 */
public final class EmbedZookeeperServer {
    
    private static TestingServer testingServer;
    
    /**
     * 内存版的内嵌Zookeeper.
     * 
     * @param port Zookeeper的通信端口号
     */
    public static void start(final int port) {
        try {
            testingServer = new TestingServer(port, new File(String.format("target/test_zk_data/%s/", System.nanoTime())));
        } catch (final Exception ex) {
            ex.printStackTrace();
        } finally {
            Runtime.getRuntime().addShutdownHook(new Thread() {
                
                @Override
                public void run() {
                    try {
                        Thread.sleep(1000L);
                        testingServer.close();
                    } catch (final InterruptedException | IOException ex) {
                    }
                }
            });
        }
    }

    public static void main(String[] args) {
        start(2181);
    }
}