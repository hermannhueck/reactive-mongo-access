package shopJava.queries;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import shopJava.model.Credentials;
import shopJava.model.Order;
import shopJava.model.Result;
import shopJava.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.mongodb.client.model.Filters.eq;
import static java.lang.Thread.sleep;
import static java.util.stream.Collectors.toList;
import static shopJava.util.Constants.*;
import static shopJava.util.Util.checkUserLoggedIn;

@SuppressWarnings("Convert2MethodRef")
public class QueryJ02Future {

    private static final int nCores = Runtime.getRuntime().availableProcessors();
    private static final ExecutorService executor = Executors.newFixedThreadPool(nCores);

    public static void main(String[] args) throws Exception {
        new QueryJ02Future();
    }

    private final DAO dao = new DAO();

    private class DAO {

        private final MongoCollection<Document> usersCollection;
        private final MongoCollection<Document> ordersCollection;

        DAO() {
            final MongoClient client = new MongoClient();
            final MongoDatabase db = client.getDatabase(SHOP_DB_NAME);
            this.usersCollection = db.getCollection(USERS_COLLECTION_NAME);
            this.ordersCollection = db.getCollection(ORDERS_COLLECTION_NAME);
        }

        Future<Optional<User>> findUserByName(final String name) {
            return executor.submit(() -> {
                final Document doc = usersCollection
                        .find(eq("_id", name))
                        .first();
                return Optional.ofNullable(doc).map(User::new);
            });
        }

        Future<List<Order>> findOrdersByUsername(final String username) {
            return executor.submit(() -> {
                final List<Document> docs = ordersCollection
                        .find(eq("username", username))
                        .into(new ArrayList<>());
                return docs.stream()
                        .map(doc -> new Order(doc))
                        .collect(toList());
            });
        }
    }   // end DAO


    private String logIn(final Credentials credentials) {
        try {
            final Future<Optional<User>> future = dao.findUserByName(credentials.username);
            final Optional<User> optUser = future.get();
            final User user = checkUserLoggedIn(optUser, credentials);
            return user.name;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Result processOrdersOf(final String username) {
        try {
            final Future<List<Order>> future = dao.findOrdersByUsername(username);
            final List<Order> orders = future.get();
            return new Result(username, orders);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void eCommerceStatistics(final Credentials credentials) {

        System.out.println("--- Calculating eCommerce statistings of user \"" + credentials.username + "\" ...");

        try {
            final String username = logIn(credentials);
            final Result result = processOrdersOf(username);
            result.display();
        } catch (Exception e) {
            System.err.println(e.toString());
        }
    }

    private QueryJ02Future() throws Exception {

        eCommerceStatistics(new Credentials(LISA, "password"));
        sleep(2000L);
        eCommerceStatistics(new Credentials(LISA, "bad_password"));
        sleep(2000L);
        eCommerceStatistics(new Credentials(LISA.toUpperCase(), "password"));

        executor.shutdown();
    }
}
