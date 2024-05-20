package gui.dialogs.prosessors;

import gui.language.Languages;
import serial.InputHandler;

import javax.swing.*;

public class AmountProcessor {
    private volatile char keypress;
    private final JLabel display;
    private final String TEXT = Languages.getLang().getAmount_query();
    private int amount;
    private static volatile boolean going;
    public AmountProcessor(JLabel display) {
        this.display = display;

        going = true;

        Thread keyConsumer = new Thread(new RunnableKeyConsumer());
        Thread keyProducer = new Thread(new RunnableKeyProducer());

        keyConsumer.start();
        keyProducer.start();

        try {
            keyConsumer.join();
            keyProducer.join();
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
            System.out.println(amount);
        }
    }
    public int getAmount() {
        return amount;
    }
    public static void stopKeypad() {
        going = false;
    }
    private void keyConsume() throws InterruptedException {
        String amount = "";
        while (true) {
            synchronized (this) {
                if (!going) throw new InterruptedException();
                display.setText(TEXT + amount);
                wait();
                if (keypress != 'K') {
                    amount = processInput(amount, keypress);
                }
                else {
                    if (amount.isEmpty()) continue;
                    throw new InterruptedException(amount);
                }
            }
        }
    }

    private void keyProduce() throws InterruptedException {
        InputHandler.setKey(false);
        while (true) {
            synchronized (this) {
                if (!going) {
                    notify();
                    throw new InterruptedException();
                }
                wait(10);
                if (InputHandler.isNewKey()) {
                    keypress = InputHandler.getKeyPress();
                    InputHandler.setKey(false);
                    notify();
                }
            }
        }
    }

    private String processInput(String amount, char input) {
        switch (input) {
            case '/':
                return amount;
            case 'D':
                if (!amount.isEmpty()) return amount.substring(0,amount.length() - 1);
            case 'C':
                return "";
            default:
                if (amount.length() < 3) amount += input;
                return amount;
        }
    }

    private class RunnableKeyConsumer implements Runnable {
        @Override
        public void run() {
            try {
                keyConsume();
            } catch (InterruptedException e) {
                going = false;
                try {
                    amount = Integer.parseInt(e.getMessage());
                } catch (NumberFormatException ignored) {
                    System.out.println("Couldn't parse at AmountProcessor");
                }
            }
        }
    }
    private class RunnableKeyProducer implements Runnable {
        @Override
        public void run() {
            try {
                keyProduce();
            } catch (InterruptedException ignored) {}
        }
    }
}
