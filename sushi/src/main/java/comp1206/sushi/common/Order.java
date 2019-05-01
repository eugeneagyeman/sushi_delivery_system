package comp1206.sushi.common;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class Order extends Model implements Serializable {
	public static final long serialVersionUID = -1527491017931721943L;
	private String status;
	private User user;
	private Map<Dish,Number> contents;
	private boolean fufilled;



	private Number cost;

	public Order() {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/YYYY HH:mm:ss");  
		LocalDateTime now = LocalDateTime.now();  
		this.name = dtf.format(now);
		contents = new HashMap<>();
		cost = 0;
		fufilled = false;
	}

	public Order(User user) {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/YYYY HH:mm:ss");
		LocalDateTime now = LocalDateTime.now();
		this.name = dtf.format(now);
		this.user = user;
		contents = new HashMap<>();
		cost = 0;
		fufilled = false;
	}


	public Number getDistance() {
		return 1;
	}

	@Override
	public String getName() {
		return this.name;
	}

	public String getStatus() {
		return status;
	}

	public User getUser() {
		return user;
	}

	public void setStatus(String status) {
		notifyUpdate("status",this.status,status);
		this.status = status;
	}

	public void setContents(Map<Dish, Number> basket){
		this.contents = basket;
	}

	public Number getCost() {
		return cost;
	}

	public void setCost(Number cost) {
		notifyUpdate("cost",this.cost,cost);
		this.cost = cost;
	}


	public Map<Dish, Number> getContents() {
		return contents;
	}
}
