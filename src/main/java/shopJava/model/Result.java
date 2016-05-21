package shopJava.model;

import java.util.List;

public class Result {

    public final String username;
    public final int orderCount;
    public final int totalAmount;
    public final int avgAmount;

    public Result(final String username, final int orderCount, final int totalAmount, final int avgAmount) {
        this.username = username;
        this.orderCount = orderCount;
        this.totalAmount = totalAmount;
        this.avgAmount = avgAmount;
    }

    public Result(final String username, final List<Order> orders) {
        this.username = username;
        this.orderCount = orders.size();
        this.totalAmount = calculateTotal(orders);
        this.avgAmount = calculateAverage();
    }

    private int calculateAverage() {
        return Math.round((100.0f * totalAmount / orderCount) / 100);
    }

    private int calculateTotal(final List<Order> orders) {
        return orders.stream().mapToInt(order -> amountOf(order)).sum();
    }

    private int amountOf(final Order order) {
        return order.amount;
    }

    @Override
    public String toString() {
        return "Result{" +
                "first='" + username + '\'' +
                ", orderCount=" + orderCount +
                ", totalAmount=" + totalAmount +
                ", avgAmount=" + avgAmount +
                '}';
    }

    public void display() {
        System.out.println("--------------------------------------------");
        System.out.println("eCommerce Statistics of User \"" + username + "\":");
        System.out.println("\tNo of Orders: " + orderCount);
        System.out.println("\tTotal Amount: " + totalAmount/100.0 + " $");
        System.out.println("\tAverage Amount: " + avgAmount/100.0 + " $");
        System.out.println("--------------------------------------------\n");
    }
}
