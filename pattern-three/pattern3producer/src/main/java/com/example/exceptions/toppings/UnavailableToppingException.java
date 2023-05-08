package com.example.exceptions.toppings;

public class UnavailableToppingException extends Exception {
    public UnavailableToppingException(String message) {
        super(message);
    }
}
