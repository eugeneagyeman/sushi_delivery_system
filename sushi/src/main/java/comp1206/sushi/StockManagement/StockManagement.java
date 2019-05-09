package comp1206.sushi.StockManagement;

import comp1206.sushi.common.Dish;
import comp1206.sushi.common.Ingredient;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Integer.valueOf;

public class StockManagement {
    private final static Lock dishesLock = new ReentrantLock(true);
    private final static Lock ingredientsLock = new ReentrantLock(true);
    static boolean restockIngredientsEnabled = true;
    static boolean restockDishesEnabled = true;
    private static Map<Ingredient, Number> ingredientsStock = new ConcurrentHashMap<>();
    private static Map<Dish, Number> dishesStock = new ConcurrentHashMap<>();
    private List<Dish> dishes;
    private static List<Ingredient> ingredients;

    public static Map<Ingredient, Number> getIngredientsStock() {
        ingredientsLock.lock();
        try {
            return ingredientsStock;
        } finally {
            ingredientsLock.unlock();

        }
    }


    public static Map<Dish, Number> getDishesStock() {
        dishesLock.lock();
        try {
            return dishesStock;
        } finally {
            dishesLock.unlock();
        }
    }

    public static void restockIngredient(Ingredient ingredient) {
        int restockThreshold = ingredient.getRestockThreshold().intValue();
        int restockAmount = ingredient.getRestockAmount().intValue();

        ingredientsLock.lock();
        try {
            int quantity = ingredientsStock.get(ingredient).intValue();

            if (quantity < restockThreshold) {
                ingredientsStock.replace(ingredient, quantity + restockAmount);
            }
        } finally {
            ingredientsLock.unlock();
        }

    }

    public static boolean isRestockIngredientsEnabled() {
        return restockIngredientsEnabled;
    }

    public static void setRestockIngredientsEnabled(boolean restockIngredientsEnabled) {
        StockManagement.restockIngredientsEnabled = restockIngredientsEnabled;
    }

    public static boolean isRestockDishesEnabled() {
        return restockDishesEnabled;
    }

    public static void setRestockDishesEnabled(boolean restockDishesEnabled) {
        StockManagement.restockDishesEnabled = restockDishesEnabled;
    }

    private void ingredientsTracker() {

        for (Ingredient existingIngredient : getIngredients()) {
            System.out.println("Ingredient: " + existingIngredient.getName()
                    + " Quantity:" + ingredientsStock.get(existingIngredient));
        }
    }

    private void dishesTracker() {

        for (Dish existingDish : getDishes()) {
            System.out.println("Dish: " + existingDish.getName()
                    + " Quantity: " + dishesStock.get(existingDish));
        }
    }

    public List<Dish> getDishes() {
        dishesLock.lock();
        try {
            dishesStock.keySet();
            dishes = new ArrayList<Dish>(dishesStock.keySet());
        } finally {
            dishesLock.unlock();
        }

        return dishes;
    }

    public Dish getDish(String name) {
        for (Dish dishes : getDishes()) {
            if (dishes.getName().equals(name)) {
                return dishes;
            }
        }
        return null;
    }

    public synchronized static List<Ingredient> getIngredients() {
        Set<Ingredient> ingredientsSet;
            ingredientsSet = ingredientsStock.keySet();


        ingredients = Collections.synchronizedList(new ArrayList<Ingredient>(ingredientsSet));
        return ingredients;
    }

    public void setIngredients(ArrayList<Ingredient> is) {
        ingredientsLock.lock();
        try {
            ingredients = is;

        } finally {
            ingredientsLock.unlock();
        }
    }

    public void dishIngredientFinder(String itemName, String itemQuantity) {
        for (Dish dish : getDishes()) {
            if (dish.getName().equals(itemName)) {
                StockManagement.getDishesStock().replace(dish, valueOf(itemQuantity));
            }
        }

        for (Ingredient ingredient : getIngredients()) {
            if (ingredient.getName().equals(itemName)) {
                StockManagement.getIngredientsStock().replace(ingredient, valueOf(itemQuantity));
            }
        }
    }


}