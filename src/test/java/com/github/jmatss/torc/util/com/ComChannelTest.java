package com.github.jmatss.torc.util.com;

import com.github.jmatss.torc.bittorrent.InfoHash;
import org.junit.jupiter.api.Test;

import java.security.NoSuchAlgorithmException;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

public class ComChannelTest {
    private static final int QUEUE_SIZE = 10;

    @Test
    public void testSendAndReceiveOnParent() {
        // EXPECTED
        String expectedFilename = "filename.txt";
        var expectedMessageType = ComMessageType.ADD;

        var parent = new ArrayBlockingQueue<ComMessage>(QUEUE_SIZE);
        var comChannel = new ComChannel(parent);
        comChannel.sendParent(ComMessage.add(expectedFilename));

        // ACTUAL
        var actualMessage = comChannel.recvParent();
        var actualFilename = actualMessage.getProperty("filename");
        var actualMessageType = actualMessage.getType();

        // ASSERT
        assertNotNull(actualFilename);
        assertEquals(expectedFilename, actualFilename);
        assertEquals(expectedMessageType, actualMessageType);
    }

    @Test
    public void testSendToChildAndReceiveOnThatChild() throws NoSuchAlgorithmException {
        var childInfoHashBytes = "01234567890123456789".getBytes();
        var childInfoHash = new InfoHash(childInfoHashBytes, true);
        var comChannel = new ComChannel();
        comChannel.addChild(childInfoHash);

        try {
            // EXPECTED
            var expectedComMessage = ComMessage.remove(childInfoHash);
            var expectedComType = ComMessageType.REMOVE;

            // Send a message on the child channel and then retrieve it in a new "thread" to hinder a dead lock.
            // Will be interrupted and fail after one second if it dead locks.
            comChannel.sendChild(expectedComMessage);

            // ACTUAL
            ComMessage actualComMessage = Executors.newSingleThreadExecutor()
                    .submit(() -> comChannel.recvChild(childInfoHash))
                    .get(1, TimeUnit.SECONDS);
            var actualComType = actualComMessage.getType();

            // ASSERT
            assertEquals(expectedComType, actualComType);

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            fail(e);
        }
    }

    @Test
    public void testSendToAllChildrenAndReceive() throws NoSuchAlgorithmException {
        var childrenInfoHashBytes = new byte[][]{
                "01234567890123456789".getBytes(),
                "98765432109876543210".getBytes()
        };
        InfoHash[] childrenInfoHash = {
                new InfoHash(childrenInfoHashBytes[0], true),
                new InfoHash(childrenInfoHashBytes[1], true)
        };
        var comChannel = new ComChannel();
        comChannel.addChild(childrenInfoHash[0]);
        comChannel.addChild(childrenInfoHash[1]);

        try {
            // EXPECTED
            var expectedComMessage = ComMessage.shutdown();
            var expectedComType = ComMessageType.SHUTDOWN;

            // Send messages to all children and then retrieve them in a new "thread" to hinder a dead lock.
            // Will be interrupted and fail after one second if it dead locks.
            comChannel.sendChildren(expectedComMessage);

            // ACTUAL
            ComMessage[] actualComMessages = Executors.newSingleThreadExecutor()
                    .submit(() -> {
                        ComMessage[] comMessages = new ComMessage[2];
                        comMessages[0] = comChannel.recvChild(childrenInfoHash[0]);
                        comMessages[1] = comChannel.recvChild(childrenInfoHash[1]);
                        return comMessages;
                    })
                    .get(1, TimeUnit.SECONDS);

            // ASSERT
            assertEquals(childrenInfoHash.length, actualComMessages.length);
            assertEquals(expectedComType, actualComMessages[0].getType());
            assertEquals(expectedComType, actualComMessages[1].getType());

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            fail(e);
        }
    }
}
