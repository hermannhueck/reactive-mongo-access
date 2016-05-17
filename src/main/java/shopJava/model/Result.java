package shopJava.model;

import java.util.List;

public class Result {

    public final String username;
    // public final List<Order> orders;
    public final int orderCount;
    public final int totalAmount;
    public final int avgAmount;

    public Result(String username, List<Order> orders) {
        this.username = username;
        // this.orders = orders;
        this.orderCount = orders.size();
        this.totalAmount = calculateTotal(orders);
        this.avgAmount = Math.round((100.0f * totalAmount / orderCount) / 100);
    }

    private int calculateTotal(List<Order> orders) {
        return orders.stream().mapToInt(order -> amountOf(order)).sum();
    }

    private int amountOf(Order order) {
        return order.amount;
    }

    @Override
    public String toString() {
        return "Result{" +
                "username='" + username + '\'' +
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
