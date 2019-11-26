package com.github.jmatss.torc.util.com;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Com {
    public static final int QUEUE_SIZE = 10;
    private static final Logger LOGGER = Logger.getLogger(Com.class.getName());

    private final BlockingQueue<Message> parent;
    private final LockableHashMap<String, BlockingQueue<Message>> children;

    Com() {
        this.parent = new ArrayBlockingQueue<>(QUEUE_SIZE);
        this.children = new LockableHashMap<>();
    }

    /**
     * Sends a message to the parent.
     *
     * @param message the message to be sent to the parent.
     * @return a boolean indicating if it was able to send the message or not.
     */
    public boolean sendParent(Message message) {
        try {
            return send(this.parent, message, 0);
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * Sends a message to the parent with the specified timeout.
     *
     * @param message the message to be sent to the parent.
     * @param timeout in second after which the function gives up sending data. A value less than or
     *                equal to zero indicates no timeout.
     * @return a boolean indicating if it was able to send the message or not.
     * @throws TimeoutException if a timeout happens.
     */
    public boolean sendParent(Message message, int timeout) throws TimeoutException {
        return send(this.parent, message, timeout);
    }

    /**
     * Sends a message to the specified child.
     *
     * @param message the message to be sent to the child.
     * @param childId the id of the child this message is directed to.
     * @return a boolean indicating if it was able to send the message or not.
     * @throws IllegalArgumentException if the specified child doesn't exist.
     */
    public boolean sendChild(Message message, String childId) throws IllegalArgumentException {
        try (LockableHashMap<?, ?> ignored = this.children.lock()) {
            if (!this.children.containsKey(childId))
                throw new IllegalArgumentException("The child with id \"" + childId + "\" could not be found.");
            return sendWithoutTimeout(this.children.get(childId), message);
        }
    }

    /**
     * Sends a message to the specified child with the specified timeout.
     *
     * @param message the message to be sent to the child.
     * @param childId the id of the child this message is directed to.
     * @param timeout in second after which the function gives up sending data. A value less than or
     *                equal to zero indicates no timeout.
     * @return a boolean indicating if it was able to send the message or not.
     * @throws IllegalArgumentException if the specified child doesn't exist.
     * @throws TimeoutException         if a timeout happens.
     */
    public boolean sendChild(Message message, String childId, int timeout)
    throws IllegalArgumentException, TimeoutException {
        try (LockableHashMap<?, ?> ignored = this.children.lock()) {
            if (!this.children.containsKey(childId))
                throw new IllegalArgumentException("The child with id \"" + childId + "\" could not be found.");
            return send(this.children.get(childId), message, timeout);
        }
    }

    private boolean sendWithoutTimeout(BlockingQueue<Message> queue, Message message) {
        try {
            return send(queue, message, 0);
        } catch (TimeoutException e) {
            // Should not be possible to end up here since the timeout is set to infinite.
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            LOGGER.log(Level.SEVERE, "Got TimeoutException while sending using no timeout: " + sw.toString());
            return false;
        }
    }

    private boolean send(BlockingQueue<Message> queue, Message message, int timeout)
    throws IllegalArgumentException, TimeoutException {
        try {
            if (timeout <= 0)
                queue.put(message);
            else if (!queue.offer(message, timeout, TimeUnit.SECONDS))
                throw new TimeoutException("timed out while sending message.");

            return true;
        } catch (InterruptedException e) {
            return false;
        }
    }

    /**
     * Receives a message on the parent "channel".
     *
     * @return a Optional containing a Message or null if it was unable to receive a message.
     */
    public Message recvParent() {
        try {
            return recv(this.parent, 0);
        } catch (TimeoutException e) {
            // Should not be possible to end up here since the timeout is set to infinite.
            return null;
        }
    }

    /**
     * Receives a message on the parent "channel" with the specified timeout.
     *
     * @param timeout in second after which the function gives up receiving data. A value less than or
     *                equal to zero indicates no timeout.
     * @return an Optional containing a Message.
     * @throws TimeoutException if a timeout happens.
     */
    public Message recvParent(int timeout) throws TimeoutException {
        return recv(this.parent, timeout);
    }

    /**
     * Receives a message on the "channel" for the specified childId.
     *
     * @param childId the id of the child to receive on.
     * @return a Optional containing a Message or null if it was unable to receive a message.
     * @throws IllegalArgumentException if the specified child doesn't exist.
     */
    public Message recvChild(String childId) throws IllegalArgumentException {
        try (LockableHashMap<?, ?> ignored = this.children.lock()) {
            if (!this.children.containsKey(childId))
                throw new IllegalArgumentException("The child with id \"" + childId + "\" could not be found.");
            return recvWithoutTimeout(this.children.get(childId));
        }
    }

    /**
     * Receives a message on the "channel" for the specified childId with the specified timeout.
     *
     * @param childId the id of the child to receive on.
     * @param timeout in second after which the function gives up receiving data. A value less than or
     *                equal to zero indicates no timeout.
     * @return an Optional containing a Message.
     * @throws IllegalArgumentException if the specified child doesn't exist.
     * @throws TimeoutException         if a timeout happens.
     */
    public Message recvChild(String childId, int timeout)
    throws IllegalArgumentException, TimeoutException {
        try (LockableHashMap<?, ?> ignored = this.children.lock()) {
            if (!this.children.containsKey(childId))
                throw new IllegalArgumentException("The child with id \"" + childId + "\" could not be found");
            return recv(this.children.get(childId), timeout);
        }
    }

    private Message recvWithoutTimeout(BlockingQueue<Message> queue) {
        try {
            return recv(queue, 0);
        } catch (TimeoutException e) {
            // Should not be possible to end up here since the timeout is set to infinite.
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            LOGGER.log(Level.SEVERE, "Got TimeoutException while receiving using no timeout: " + sw.toString());
            return null;
        }
    }

    // Doesn't use the timeout on the receive if the "timeout" argument is less than or equal to zero.
    private Message recv(BlockingQueue<Message> queue, int timeout) throws TimeoutException {
        Message message;
        try {
            if (timeout <= 0) {
                message = queue.take();
            } else {
                message = queue.poll(timeout, TimeUnit.SECONDS);
                if (message == null) throw new TimeoutException("timed out while receiving message");
            }
        } catch (InterruptedException e) {
            return null;
        }

        return message;
    }

    /**
     * Adds a "channel" for the child to receive messages from the parent.
     *
     * @param childId the id of the child that the parent should send to
     * @return this
     */
    public Com addChild(String childId) {
        try (LockableHashMap<?, ?> ignored = this.children.lock()) {
            if (this.children.containsKey(childId))
                return this; // TODO: return exception or just let it be ok?

            this.children.put(childId, new ArrayBlockingQueue<>(QUEUE_SIZE));
            return this;
        }
    }

    /**
     * Removes the "channel" corresponding to the child with specified childId.
     *
     * @param childId the id of the child that is to be removed.
     * @return this
     */
    public Com removeChild(String childId) {
        try (LockableHashMap<?, ?> ignored = this.children.lock()) {
            if (!this.children.containsKey(childId))
                return this; // TODO: return exception or just let it be ok?

            this.children.remove(childId);
            return this;
        }
    }
}
