package shopJava.util;

import shopJava.model.Credentials;
import shopJava.model.Order;
import shopJava.model.User;

import java.util.List;
import java.util.Optional;

public class Util {

    public static User checkUserLoggedIn(final Optional<User> optUser, final Credentials credentials) {

        if (!optUser.isPresent()) {     // replaces if (user != null)
            throw new RuntimeException(new IllegalAccessException("User unknown: " + credentials.username));
        }
        final User user = optUser.get();
        return checkUserLoggedIn(user, credentials);
    }

    public static User checkUserLoggedIn(final User user, final Credentials credentials) {
        if (!user.name.equals(credentials.username)) {
            throw new RuntimeException(new IllegalAccessException("Incorrect first: " + credentials.username));
        }
        if (!user.password.equals(credentials.password)) {
            throw new RuntimeException(new IllegalAccessException("Bad password supplied for user: " + credentials.username));
        }
        return user;
    }

    public static void sleep(final int seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static int average(final int totalAmount, final int orderCount) {
        return orderCount == 0 ? 0 : Math.round((100.0f * totalAmount / orderCount) / 100);
    }

    public static int totalAmountOf(final List<Order> orders) {
        return orders.stream().mapToInt(order -> order.amount).sum();
    }
}
