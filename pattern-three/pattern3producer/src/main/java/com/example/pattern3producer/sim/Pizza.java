package com.example.pattern3producer.sim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import com.example.objects.PizzaDough;
import com.example.objects.PizzaSauce;
import com.example.objects.PizzaTopping;

public class Pizza {
    private String id;
    private ArrayList<Object> parts;
    private PizzaDough dough;
    private PizzaSauce sauce;
    private ArrayList<PizzaTopping> toppings;
    private ArrayList<String> pizzaDoughKinds = new ArrayList<String>(Arrays.asList("wheat", "gf"));
    private ArrayList<String> pizzaDoughSizes = new ArrayList<String>(Arrays.asList("10in", "12in", "16in"));
    private ArrayList<String> pizzaSauceKinds = new ArrayList<String>(Arrays.asList("red", "white"));
    private ArrayList<String> pizzaSauceAmounts = new ArrayList<String>(Arrays.asList("light", "normal", "extra"));
    private ArrayList<String> pizzaToppingKinds = new ArrayList<String>(Arrays.asList("pepperoni", "pineapple"));
    private ArrayList<String> pizzaToppingAmounts = new ArrayList<String>(Arrays.asList("light", "normal", "extra"));
    private ArrayList<String> pizzaToppingDistributions = new ArrayList<String>(Arrays.asList("half", "whole"));

    public Pizza() {
        this.id = UUID.randomUUID().toString();
        randomizePizza();
    }

    public void randomizePizza() {
        this.dough = choosePizzaDough();
        this.sauce = choosePizzaSauce();
        this.toppings = chooseMultiplePizzaToppings();
        this.parts = new ArrayList<>();
        this.parts.add(this.dough);
        this.parts.add(this.sauce);
        this.parts.addAll(this.toppings);
    }
    public PizzaDough choosePizzaDough() {
        PizzaDough pizzaDough = new PizzaDough();
        pizzaDough.setPizza(id);
        pizzaDough.setPart("dough");
        pizzaDough.setKind(pizzaDoughKinds.get((int) Math.floor(Math.random()*pizzaDoughKinds.size())));
        pizzaDough.setSize(pizzaDoughSizes.get((int) Math.floor(Math.random()*pizzaDoughSizes.size())));
        return pizzaDough;
    }
    public PizzaSauce choosePizzaSauce() {
        PizzaSauce pizzaSauce = new PizzaSauce();
        pizzaSauce.setPizza(id);
        pizzaSauce.setPart("sauce");
        pizzaSauce.setKind(pizzaSauceKinds.get((int) Math.floor(Math.random()*pizzaSauceKinds.size())));
        pizzaSauce.setAmount(pizzaSauceAmounts.get((int) Math.floor(Math.random()*pizzaSauceAmounts.size())));
        return pizzaSauce;
    }
    public PizzaTopping choosePizzaTopping() {
        PizzaTopping pizzaTopping = new PizzaTopping();
        pizzaTopping.setPizza(id);
        pizzaTopping.setPart("topping");
        pizzaTopping.setKind(pizzaToppingKinds.get((int) Math.floor(Math.random()*pizzaToppingKinds.size())));
        pizzaTopping.setAmount(pizzaToppingAmounts.get((int) Math.floor(Math.random()*pizzaToppingAmounts.size())));
        pizzaTopping.setDsitribution(pizzaToppingDistributions.get((int) Math.floor(Math.random()*pizzaToppingDistributions.size())));
        return pizzaTopping;
    }
    // TODO Probably a better way to do this, like pop elements from the list after selected
    public ArrayList<PizzaTopping> chooseMultiplePizzaToppings() {
        ArrayList<PizzaTopping> toppings = new ArrayList<>();
        // For the dairy sensitive, 5% chance to not add cheese
        if (Math.random() < 0.95) {
            PizzaTopping cheese = choosePizzaTopping();
            cheese.setPizza(id);
            cheese.setPart("topping");
            cheese.setKind("cheese");
            cheese.setDsitribution("whole");
            toppings.add(cheese);
        }
        // Choose a number of toppings <= the number from array in chooseToppings()
        for (int i=0; i<Math.random()*pizzaToppingKinds.size(); i++) {
            PizzaTopping topping = choosePizzaTopping();
            boolean existsAlready = false;
            // If a topping is already chosen, skip it
            for (PizzaTopping existingTopping : toppings) {
                if (existingTopping.getKind().equals(topping.getKind())) {
                    existsAlready = true;
                }
            }
            if (!existsAlready) {
                toppings.add(topping);
            }
        }
        return toppings;
    }


    public String getId() {
        return this.id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public ArrayList<Object> getParts() {
        return this.parts;
    }
    public void setParts(ArrayList<Object> parts) {
        this.parts = parts;
    }
    public PizzaDough getDough() {
        return this.dough;
    }
    public void setDough(PizzaDough dough) {
        this.dough = dough;
    }
    public PizzaSauce getSauce() {
        return this.sauce;
    }
    public void setSauce(PizzaSauce sauce) {
        this.sauce = sauce;
    }
    public ArrayList<PizzaTopping> getToppings() {
        return this.toppings;
    }
    public void setToppings(ArrayList<PizzaTopping> toppings) {
        this.toppings = toppings;
    }  
    public ArrayList<String> getPizzaDoughKinds() {
        return this.pizzaDoughKinds;
    }
    public void setPizzaDoughKinds(ArrayList<String> pizzaDoughKinds) {
        this.pizzaDoughKinds = pizzaDoughKinds;
    }
    public ArrayList<String> getPizzaDoughSizes() {
        return this.pizzaDoughSizes;
    }
    public void setPizzaDoughSizes(ArrayList<String> pizzaDoughSizes) {
        this.pizzaDoughSizes = pizzaDoughSizes;
    }
    public ArrayList<String> getPizzaSauceKinds() {
        return this.pizzaSauceKinds;
    }
    public void setPizzaSauceKinds(ArrayList<String> pizzaSauceKinds) {
        this.pizzaSauceKinds = pizzaSauceKinds;
    }
    public ArrayList<String> getPizzaSauceAmounts() {
        return this.pizzaSauceAmounts;
    }
    public void setPizzaSauceAmounts(ArrayList<String> pizzaSauceAmounts) {
        this.pizzaSauceAmounts = pizzaSauceAmounts;
    }
    public ArrayList<String> getPizzaToppingKinds() {
        return this.pizzaToppingKinds;
    }
    public void setPizzaToppingKinds(ArrayList<String> pizzaToppingKinds) {
        this.pizzaToppingKinds = pizzaToppingKinds;
    }
    public ArrayList<String> getPizzaToppingAmounts() {
        return this.pizzaToppingAmounts;
    }
    public void setPizzaToppingAmounts(ArrayList<String> pizzaToppingAmounts) {
        this.pizzaToppingAmounts = pizzaToppingAmounts;
    }
    public ArrayList<String> getPizzaToppingDistributions() {
        return this.pizzaToppingDistributions;
    }
    public void setPizzaToppingDistributions(ArrayList<String> pizzaToppingDistributions) {
        this.pizzaToppingDistributions = pizzaToppingDistributions;
    }
}
