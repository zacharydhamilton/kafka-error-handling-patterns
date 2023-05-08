package com.example.pattern3producer.sim;

import com.example.exceptions.doughs.OutOfDoughException;
import com.example.exceptions.doughs.UnavailableDoughException;
import com.example.exceptions.sauces.OutOfSauceException;
import com.example.exceptions.sauces.UnavailableSauceException;
import com.example.exceptions.toppings.IllegalToppingException;
import com.example.exceptions.toppings.UnavailableToppingException;
import com.example.objects.PizzaDough;
import com.example.objects.PizzaSauce;
import com.example.objects.PizzaTopping;

public class BoobyTrap {
    boolean retryableDoughError;
    boolean nonretryableDoughError;
    boolean retryableSauceError;
    boolean nonretryableSauceError;
    boolean retryableToppingError;
    boolean nonretryableToppingError;

    public BoobyTrap(boolean retryableDoughError, boolean nonretryableDoughError, boolean retryableSauceError, boolean nonretryableSauceError, boolean retryableToppingError, boolean nonretryableToppingError) {
        this.retryableDoughError = retryableDoughError;
        this.nonretryableDoughError = nonretryableDoughError;
        this.retryableSauceError = retryableSauceError;
        this.nonretryableSauceError = nonretryableSauceError;
        this.retryableToppingError = retryableToppingError;
        this.nonretryableToppingError = nonretryableToppingError;
    }

    public void disarm(PizzaDough dough) throws OutOfDoughException, UnavailableDoughException {
        if (retryableDoughError) {
            String message = "The dough '%s' currently unavailable for pizza '%s'.";
            throw new OutOfDoughException(String.format(message, dough.getKind(), dough.getPizza()));
        }
        if (nonretryableDoughError) {
            String message = "The dough '%s' is permanently unavailable.";
            throw new UnavailableDoughException(String.format(message, dough.getKind()));
        }
    }
    public void disarm(PizzaSauce sauce) throws OutOfSauceException, UnavailableSauceException {
        if (retryableSauceError) {
            String message = "The sauce '%s' currently unavailable for pizza '%s'.";
            throw new OutOfSauceException(String.format(message, sauce.getKind(), sauce.getPizza()));
        }
        if (nonretryableSauceError) {
            String message = "The sauce '%s' is permanently unavailable.";
            throw new UnavailableSauceException(String.format(message, sauce.getKind()));
        }
    }
    public void disarm(PizzaTopping topping) throws IllegalToppingException, UnavailableToppingException {
        if (retryableToppingError) {
            String message = "An illegal topping '%s' was found on pizza '%s'.";
            throw new IllegalToppingException(String.format(message, topping.getKind(), topping.getPizza()));
        }
        if (nonretryableToppingError) {
            String message = "The topping '%s' is permanently unavailable.";
            throw new UnavailableToppingException(String.format(message, topping.getKind()));
        }
    }
}
