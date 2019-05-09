package comp1206.sushi.common;

import comp1206.sushi.common.Supplier;

import java.io.Serializable;

public class Ingredient extends Model implements Serializable {
	public static final long serialVersionUID = -7090774425612083896L;
	private String name;
	private String unit;
	private Supplier supplier;
	private Number restockThreshold;
	private Number restockAmount;
	private Number weight;

	public Ingredient(String name, String unit, Supplier supplier, Number restockThreshold,
			Number restockAmount, Number weight) {
		this.setName(name);
		this.setUnit(unit);
		this.setSupplier(supplier);
		this.setRestockThreshold(restockThreshold);
		this.setRestockAmount(restockAmount);
		this.setWeight(weight);
	}

	public Ingredient() {

	}

	public  String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public  String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	public Supplier getSupplier() {
		return supplier;
	}

	public void setSupplier(Supplier supplier) {
		this.supplier = supplier;
	}

	public synchronized Number getRestockThreshold() {
		return restockThreshold;
	}

	public synchronized void setRestockThreshold(Number restockThreshold) {
		this.restockThreshold = restockThreshold;
	}

	public synchronized Number getRestockAmount() {
		return restockAmount;
	}

	public synchronized void setRestockAmount(Number restockAmount) {
		this.restockAmount = restockAmount;
	}

	public synchronized Number getWeight() {
		return weight;
	}

	public synchronized void setWeight(Number weight) {
		this.weight = weight;
	}
}
