package com.github.jmatss.torc;

import com.github.jmatss.torc.util.com.ComMessage;

import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;

public class View {
    public static final int QUEUE_SIZE = 10;

    public static void main(String[] args) {
        var recvFromController = new ArrayBlockingQueue<ComMessage>(QUEUE_SIZE);
        var sendToController = new ArrayBlockingQueue<ComMessage>(QUEUE_SIZE);
        Controller controller = new Controller(recvFromController, sendToController);
        controller.run();

        while (true) {
            String s;
            try (var input = new Scanner(System.in)) {
                s = input.nextLine();
                if (s.isEmpty()) {
                    continue;
                }

                String[] cmd = s.split(" ");
                switch (cmd[0]) {

                }
            }
        }
    }
}
