package com.example.pattern3producer.sim;

import java.io.IOException;
import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.common.config.ConfigException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.example.objects.PizzaDough;
import com.example.objects.PizzaSauce;
import com.example.objects.PizzaTopping;
import com.example.pattern3producer.consumer.ConsumerService;
import com.example.pattern3producer.producer.ProducerService;

import jakarta.annotation.PreDestroy;

@Service
public class Simulation {
    @Autowired
    private ProducerService producer;
    @Autowired
    private ConsumerService consumer;
    
    private Thread consumerLoop;
    private Thread produceLoop;

    private Timer buildTimer = new Timer("buildTimer");
    private TimerTask buildTimerTask = new TimerTask() {
        public void run() {
            if (producer.ready && consumer.ready) {
                System.out.println("\nMakin' a new pizza!");
                Pizza pizza = new Pizza();
                BoobyTrap boobyTrap = createBoobyTrap();
                for (Object part : pizza.getParts()) {
                    String type = part.getClass().getSimpleName();
                    try {
                        TimeUnit.MILLISECONDS.sleep((long) (Math.random()*1000));
                    } catch (InterruptedException interruptedException) {
                        interruptedException.printStackTrace();
                    }
                    switch (type) {
                        case "PizzaDough": producer.send((PizzaDough) part, boobyTrap); break;
                        case "PizzaSauce": producer.send((PizzaSauce) part, boobyTrap); break;
                        case "PizzaTopping": producer.send((PizzaTopping) part, boobyTrap); break;
                        default: 
                            System.out.println(type);
                            break;
                    }
                }
            }
        }
    };

    @EventListener(ApplicationStartedEvent.class)
    public void start() throws InterruptedException, ConfigException, IOException {
        this.producer.initService();
        this.consumer.initService();
        while (!producer.ready && !consumer.ready) {
            System.out.println("Producer and consumer not initialized, waiting 1 second...");
            TimeUnit.SECONDS.sleep(1);
        }
        consumerLoop = new Thread(new Runnable() {
            public void run() {
                consumer.start();
            }
        });
        consumerLoop.start();
        produceLoop = new Thread(new Runnable() {
            public void run() {
                buildTimer.schedule(buildTimerTask, Duration.ofSeconds(1).toMillis(), Duration.ofSeconds(15).toMillis());
            }
        });
        produceLoop.start();
    }

    @PreDestroy
    public void stop() {
        if (consumerLoop != null) {
            consumerLoop.interrupt();
        } else {
            System.out.println("ConsumerLoop is null. Can't stop");
        }
        if (produceLoop != null) {
            produceLoop.interrupt();
        } else {
            System.out.println("ProduceLoop is null. Can't stop");
        }
    }

    private static BoobyTrap createBoobyTrap() {
        boolean retryableDoughError;
        boolean nonretryableDoughError;
        boolean retryableSauceError;
        boolean nonretryableSauceError;
        boolean retryableToppingError;
        boolean nonretryableToppingError;
        // 99% chance of creating a retryable or normal event
        if (Math.random() < 0.99) {
            nonretryableDoughError = false;
            nonretryableSauceError = false;
            nonretryableToppingError = false;
            // 90% chance of a normal pizza dough event
            if (Math.random() < 0.90) {
                retryableDoughError = false;
                // 90% chance of a normal pizza sauce event
                if (Math.random() < 0.90){
                    retryableSauceError = false;
                    // 90% chance of a normal pizza topping event
                    if (Math.random() < 0.90) {
                        retryableToppingError = false;
                        return new BoobyTrap(retryableDoughError, nonretryableDoughError, retryableSauceError, nonretryableSauceError, retryableToppingError, nonretryableToppingError);
                    // 10% chance of creating a retryable pizza topping event
                    } else {
                        retryableToppingError = true;
                        return new BoobyTrap(retryableDoughError, nonretryableDoughError, retryableSauceError, nonretryableSauceError, retryableToppingError, nonretryableToppingError);
                    }
                // 10% chance of create a retryable pizza sauce event
                } else {
                    retryableSauceError = true;
                    retryableToppingError = false;
                    return new BoobyTrap(retryableDoughError, nonretryableDoughError, retryableSauceError, nonretryableSauceError, retryableToppingError, nonretryableToppingError);
                }
            // 10% chance of creating a retryable pizza dough event
            } else {
                retryableDoughError = true;
                retryableSauceError = false;
                retryableToppingError = false;
                return new BoobyTrap(retryableDoughError, nonretryableDoughError, retryableSauceError, nonretryableSauceError, retryableToppingError, nonretryableToppingError);
            }
        // 1% chance of creating a completely unretryable event
        } else {
            retryableDoughError = false;
            nonretryableDoughError = true;
            retryableSauceError = false;
            nonretryableSauceError = true;
            retryableToppingError = false;
            nonretryableToppingError = false;
            return new BoobyTrap(retryableDoughError, nonretryableDoughError, retryableSauceError, nonretryableSauceError, retryableToppingError, nonretryableToppingError);
        }
    }
}
