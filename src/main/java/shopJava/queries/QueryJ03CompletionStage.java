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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import static com.mongodb.client.model.Filters.eq;
import static java.lang.Thread.sleep;
import static java.util.stream.Collectors.toList;
import static shopJava.util.Constants.*;
import static shopJava.util.Util.checkUserLoggedIn;

@SuppressWarnings("Convert2MethodRef")
public class QueryJ03CompletionStage {

    public static void main(String[] args) throws Exception {
        new QueryJ03CompletionStage();
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

        private Optional<User> _findUserByName(final String name) {
            final Document doc = usersCollection
                    .find(eq("_id", name))
                    .first();
            return Optional.ofNullable(doc).map(User::new);
        }

        private List<Order> _findOrdersByUsername(final String username) {
            final List<Document> docs = ordersCollection
                    .find(eq("username", username))
                    .into(new ArrayList<>());
            return docs.stream()
                    .map(doc -> new Order(doc))
                    .collect(toList());
        }

        CompletionStage<Optional<User>> findUserByName(final String name) {
            Supplier<Optional<User>> supplier = () -> _findUserByName(name);
            return CompletableFuture.supplyAsync(supplier);
        }

        CompletionStage<List<Order>> findOrdersByUsername(final String username) {
            Supplier<List<Order>> supplier = () -> _findOrdersByUsername(username);
            return CompletableFuture.supplyAsync(supplier);
        }
    }   // end DAO


    private CompletionStage<String> logIn(final Credentials credentials) {
        return dao.findUserByName(credentials.username)
                .thenApply(optUser -> checkUserLoggedIn(optUser, credentials))
                .thenApply(user -> user.name);

    }

    private CompletionStage<Result> processOrdersOf(final String username) {
        return dao.findOrdersByUsername(username)
                .thenApply(orders -> new Result(username, orders));
    }

    private void eCommerceStatistics(final Credentials credentials) {

        System.out.println("--- Calculating eCommerce statistings of user \"" + credentials.username + "\" ...");

        logIn(credentials)
                .thenCompose(username -> processOrdersOf(username))     // flatMap of CompletionStage
                .whenComplete((result, t) -> {
                    if (t == null) {
                        result.display();
                    } else {
                        System.err.println(t.toString());
                    }
                });
    }

    private QueryJ03CompletionStage() throws Exception {

        eCommerceStatistics(new Credentials(LISA, "password"));
        sleep(2000L);
        eCommerceStatistics(new Credentials(LISA, "bad_password"));
        sleep(2000L);
        eCommerceStatistics(new Credentials(LISA.toUpperCase(), "password"));
    }
}
