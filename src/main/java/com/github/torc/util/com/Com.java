package com.github.torc.util.com;

import java.util.Hashtable;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Com {
  public static final int QUEUE_SIZE = 10;

  private final BlockingQueue<Message> parent;
  private final Map<String, BlockingQueue<Message>> children;

  Com() {
    this.parent = new ArrayBlockingQueue<>(QUEUE_SIZE);
    this.children = new Hashtable<>();
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
      // should not be possible to end up here.
      return false;
    }
  }

  /**
   * Sends a message to the parent with the specified timeout.
   *
   * @param message the message to be sent to the parent.
   * @param timeout in second after which the function gives up sending data. A value less than or
   *     equal to zero indicates no timeout.
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
    if (!this.children.containsKey(childId)) {
      throw new IllegalArgumentException(
          "the child with id \"" + childId + "\" could not be found");
    }

    try {
      return send(this.children.get(childId), message, 0);
    } catch (TimeoutException e) {
      // should not be possible to end up here.
      return false;
    }
  }

  /**
   * Sends a message to the specified child with the specified timeout.
   *
   * @param message the message to be sent to the child.
   * @param childId the id of the child this message is directed to.
   * @param timeout in second after which the function gives up sending data. A value less than or
   *     equal to zero indicates no timeout.
   * @return a boolean indicating if it was able to send the message or not.
   * @throws IllegalArgumentException if the specified child doesn't exist.
   * @throws TimeoutException if a timeout happens.
   */
  public boolean sendChild(Message message, String childId, int timeout)
      throws IllegalArgumentException, TimeoutException {
    if (!this.children.containsKey(childId)) {
      throw new IllegalArgumentException(
          "the child with id \"" + childId + "\" could not be found");
    }
    return send(this.children.get(childId), message, timeout);
  }

  private boolean send(BlockingQueue<Message> queue, Message message, int timeout)
      throws IllegalArgumentException, TimeoutException {
    try {
      if (timeout <= 0) {
        queue.put(message);
      } else {
        if (!queue.offer(message, timeout, TimeUnit.SECONDS)) {
          throw new TimeoutException("timed out while sending message");
        }
      }
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
  public Optional<Message> recvParent() {
    try {
      return recv(this.parent, 0);
    } catch (TimeoutException e) {
      // should not be possible to end up here.
      return Optional.empty();
    }
  }

  /**
   * Receives a message on the parent "channel" with the specified timeout.
   *
   * @param timeout in second after which the function gives up receiving data. A value less than or
   *     equal to zero indicates no timeout.
   * @return an Optional containing a Message.
   * @throws TimeoutException if a timeout happens.
   */
  public Optional<Message> recvParent(int timeout) throws TimeoutException {
    return recv(this.parent, timeout);
  }

  /**
   * Receives a message on the "channel" for the specified childId.
   *
   * @param childId the id of the child to receive on.
   * @return a Optional containing a Message or null if it was unable to receive a message.
   * @throws IllegalArgumentException if the specified child doesn't exist.
   */
  public Optional<Message> recvChild(String childId) throws IllegalArgumentException {
    if (!this.children.containsKey(childId)) {
      throw new IllegalArgumentException(
          "the child with id \"" + childId + "\" could not be found");
    }

    try {
      return recv(this.children.get(childId), 0);
    } catch (TimeoutException e) {
      // should not be possible to end up here.
      return Optional.empty();
    }
  }

  /**
   * Receives a message on the "channel" for the specified childId with the specified timeout.
   *
   * @param childId the id of the child to receive on.
   * @param timeout in second after which the function gives up receiving data. A value less than or
   *     equal to zero indicates no timeout.
   * @return an Optional containing a Message.
   * @throws IllegalArgumentException if the specified child doesn't exist.
   * @throws TimeoutException if a timeout happens.
   */
  public Optional<Message> recvChild(String childId, int timeout)
      throws IllegalArgumentException, TimeoutException {
    if (!this.children.containsKey(childId)) {
      throw new IllegalArgumentException(
          "the child with id \"" + childId + "\" could not be found");
    }
    return recv(this.children.get(childId), timeout);
  }

  // doesn't use the timeout on the receive if the "timeout" argument is less than or equal to zero.
  private Optional<Message> recv(BlockingQueue queue, int timeout) throws TimeoutException {
    Object o;

    try {
      if (timeout <= 0) {
        o = queue.take();
      } else {
        o = queue.poll(timeout, TimeUnit.SECONDS);
        if (o == null) throw new TimeoutException("timed out while receiving message");
      }
    } catch (InterruptedException e) {
      return Optional.empty();
    }

    return (o instanceof Message) ? Optional.of((Message) o) : Optional.empty();
  }

  /**
   * Adds a "channel" for the child to receive messages from the parent.
   *
   * @param childId the id of the child that the parent should send to
   * @return this
   */
  public Com addChild(String childId) {
    if (this.children.containsKey(childId))
      return this; // TODO: return exception or just let it be ok?

    this.children.put(childId, new ArrayBlockingQueue<>(QUEUE_SIZE));
    return this;
  }

  /**
   * Removes the "channel" corresponding to the child with specified childId.
   *
   * @param childId the id of the child that is to be removed.
   * @return this
   */
  public Com removeChild(String childId) {
    if (!this.children.containsKey(childId))
      return this; // TODO: return exception or just let it be ok?

    this.children.remove(childId);
    return this;
  }
}
