package comp1206.sushi.common;

import comp1206.sushi.common.Communication.ServerCommunications;
import comp1206.sushi.common.StockManagement.StockManagement;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

public class Drone extends Model implements Runnable, Serializable {
    public static final long serialVersionUID = -1250134517313411885L;
    private final static ReentrantLock dishCheckLock = new ReentrantLock(true);
    private Number speed;
    private Number progress;
    private Number capacity;
    private Number battery;
    private String status;
    private Postcode base;
    private Postcode source;
    private Postcode destination;
    private BlockingQueue<Ingredient> ingredientQueueInstance;
    private BlockingQueue<Order> orderQueueInstance;
    private ServerCommunications updateCommunications;
    private StockManagement stockManagement;
    private volatile boolean exit = false;

    public Drone(Number speed, Postcode restaurantBase, ServerCommunications updateCommunications) {
        this.updateCommunications = updateCommunications;
        this.setSpeed(speed);
        this.setCapacity(1);
        this.setBattery(100);
        this.setBase(restaurantBase);
    }

    public Drone() {

    }

    public Number getSpeed() {
        return speed;
    }

    public synchronized void setSpeed(Number speed) {
        this.speed = speed;
    }

    public Number getProgress() {
        return progress;
    }

    public synchronized void setProgress(Number progress) {
        this.progress = progress;
        notifyUpdate("progress", this.progress, progress);
    }

    @Override
    public String getName() {
        return "Drone (" + getSpeed() + " speed)";
    }

    public Postcode getBase() {
        return base;
    }

    public void setBase(Postcode base) {
        this.base = base;
    }

    public Postcode getSource() {
        return source;
    }

    public synchronized void setSource(Postcode source) {
        this.source = source;
    }

    public Postcode getDestination() {
        return destination;
    }

    public synchronized void setDestination(Postcode destination) {
        this.destination = destination;
    }

    public Number getCapacity() {
        return capacity;
    }

    public synchronized void setCapacity(Number capacity) {
        this.capacity = capacity;
    }

    public Number getBattery() {
        return battery;
    }

    public synchronized void setBattery(Number battery) {
        this.battery = battery;
    }

    public String getStatus() {
        return status;
    }

    public synchronized void setStatus(String status) {
        notifyUpdate("status", this.status, status);
        this.status = status;
    }

    public synchronized void setStatus(String status, Order order) {
        try {
            notifyUpdate("status", this.status, status);
            this.status = status;
            order.setStatus(status);
            updateCommunications.sendMsg(String.format("%d:%s", order.getOrderID(), order.getStatus()), false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void setQueue(BlockingQueue<Ingredient> ingredientQueue, BlockingQueue orderQueue) {
        this.ingredientQueueInstance = ingredientQueue;
        this.orderQueueInstance = orderQueue;
    }

    public void grabIngredient(Ingredient ingredient, Postcode source) throws InterruptedException {
        setStatus("Collecting: " + ingredient.getName());
        setSource(source);
        setDestination(ingredient.getSupplier().getPostcode());

        setProgress(0);

        Long distance = ingredient.getSupplier().getDistance().longValue();
        Long speed = this.speed.longValue();

        travel(distance, speed);
        setStatus("Destination reached... " + ingredient.getName() + "....");
        Thread.sleep(500);
        setDestination(base);
        setSource(ingredient.getSupplier().getPostcode());
        setStatus(ingredient.getName() + " Collected. Returning to base...");
        travel(distance, speed);
        synchronized (ingredient) {
            try {
                stockManagement.restockIngredient(ingredient);
            } catch (NullPointerException e) {
                System.out.println("Error: Attempting to restockIngredient unsuccessful");
            }
        }
        setProgress(0);
        setBattery(getBattery().intValue()- ThreadLocalRandom.current().nextInt(5));
        notifyUpdate();

    }


    public boolean grabOrder(Order order) throws InterruptedException, IOException {
        boolean enoughDishes = true;

        setProgress(null);
        setStatus("Preparing to deliver order: " + order.getName(), order);

        AtomicReference<Map<Dish, Number>> orderContents = new AtomicReference<>(order.getContents());

        dishCheckLock.lock();
        Map<Dish, Number> dishStock = stockManagement.getDishesStock();

        for (Map.Entry<Dish, Number> entry : orderContents.get().entrySet()) {
            Dish currentDish = entry.getKey();
            Number orderQty = entry.getValue();
            int stockAmt;
            try {
                stockAmt = dishStock.get(currentDish).intValue();
            } catch (NullPointerException emptyDishes) {
                stockAmt = 0;
            }

            if (stockAmt < orderQty.intValue()) {
                setStatus("Cannot deliver order" + order.getName() + " as dishes still need to be made", order);
                System.out.println("Cannot deliver order" + order.getName() + " as dishes still need to be made");
                enoughDishes = false;
            } else {
                setStatus("Dishes available, getting ready to deliver.", order);
                for (Map.Entry<Dish, Number> e : orderContents.get().entrySet()) {
                    Dish key = e.getKey();
                    Number value = e.getValue();
                    dishStock.replace(key, dishStock.get(key).intValue() - value.intValue());
                }

            }
        }
        dishCheckLock.unlock();
        if (enoughDishes) {
            setStatus("In Transit: Delivering to:" + order.getUser().getName(), order);
            setProgress(0);
            Long customerDistance = order.getUser().getPostcode().getDistance().longValue();
            Long speed = this.speed.longValue();
            travel(customerDistance, speed);
            setStatus("Complete");
            order.setStatus("Delivered");
            updateCommunications.sendMsg(order.getOrderID() + ":" + order.getStatus(), false);
            setBattery(getBattery().intValue()- ThreadLocalRandom.current().nextInt(5));
            travel(customerDistance, speed);
        }
        return enoughDishes;
    }

    private void travel(Long distance, Long speed) throws InterruptedException {
        long timeTillDestination = distance / speed;
        double timeElapsed = (double) 0;

        while (timeElapsed < timeTillDestination) {
            timeElapsed += 1;
            Thread.sleep(1000);
            setProgress(Math.round((timeElapsed / timeTillDestination) * 100));
        }

    }

    //TODO: Order Build...


    @Override
    public void run() {
        while (!exit) {
            try {
                System.out.println(Thread.currentThread().getName() + ": Initialised");

                while (StockManagement.isRestockIngredientsEnabled()) {
                    while(!(getBattery().intValue() <= 0)){
                        if (ingredientQueueInstance.isEmpty() && orderQueueInstance.isEmpty()) {
                            this.setStatus("Idle");
                        } else if (orderQueueInstance.peek() != null) {
                            Order orderToDeliver = orderQueueInstance.take();
                            synchronized (orderToDeliver) {
                                if (!grabOrder(orderToDeliver)) {
                                    orderQueueInstance.put(orderToDeliver);
                                    System.out.println("Order put back in queue");
                                    setStatus("Idle");
                                }
                            }
                        } else if (ingredientQueueInstance.peek() != null) {
                            Ingredient ingredientToCollect = ingredientQueueInstance.take();
                            grabIngredient(ingredientToCollect, base);
                        }
                    }

                    if(getBattery().intValue() <= 0) {
                        setStatus("Battery Depleted...Charging");
                        Thread.sleep(120000);
                        setBattery(100);
                        setProgress(null);
                        setStatus("Idle");
                    }

                }
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            } catch (NullPointerException ignored) {
                ignored.printStackTrace();
            }

        }
    }

    public void stop() {
        this.exit = true;
    }

    public void start() {
        this.exit=false;
        new Thread(this,this.getName()).start();
    }

    public void setStockManagement(StockManagement stock) {
        stockManagement = stock;
    }

}
