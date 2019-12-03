package com.github.jmatss.torc.util.com;

import com.github.jmatss.torc.bittorrent.InfoHash;
import org.junit.jupiter.api.Test;

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
    public void testSendToChildAndReceiveOnThatChild() {
        var childInfoHashBytes = new byte[]{
                0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19
        };
        var childInfoHash = new InfoHash(childInfoHashBytes);
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
    public void testSendToAllChildrenAndReceive() {
        var childrenInfoHashBytes = new byte[][]{
                {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19},
                {19, 18, 17, 16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0}
        };
        InfoHash[] childrenInfoHash = {
                new InfoHash(childrenInfoHashBytes[0]),
                new InfoHash(childrenInfoHashBytes[1])
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
                    .submit(() ->  {
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
