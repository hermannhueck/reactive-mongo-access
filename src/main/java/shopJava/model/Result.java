package shopJava.model;

import java.util.List;

import static shopJava.util.Util.average;
import static shopJava.util.Util.totalAmountOf;

@SuppressWarnings("WeakerAccess")
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

    public Result(final String username, final int orderCount, final int totalAmount) {
        this(username, orderCount, totalAmount, average(totalAmount, orderCount));
    }

    public Result(final String username, final List<Order> orders) {
        this(username, orders.size(), totalAmountOf(orders));
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
