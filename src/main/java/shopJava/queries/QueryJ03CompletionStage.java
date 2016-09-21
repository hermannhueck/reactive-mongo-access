package shopJava.queries;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import shopJava.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.mongodb.client.model.Filters.eq;
import static java.lang.Thread.sleep;
import static java.util.stream.Collectors.toList;
import static shopJava.util.Constants.*;
import static shopJava.util.Util.average;
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
            final MongoClient client = new MongoClient(new MongoClientURI(MONGODB_URI));
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
                .thenApply(orders -> {
                    final Stream<Order> orderStream = orders.stream();
                    final Stream<IntPair> pairStream = orderStream.map(order -> new IntPair(order.amount, 1));
                    final IntPair pair = pairStream.reduce(new IntPair(0, 0), (p1, p2) -> new IntPair(p1.first + p2.first, p1.second + p2.second));
                    return new  Result(username, pair.second, pair.first, average(pair.first, pair.second));
                });
    }

    private void eCommerceStatistics(final Credentials credentials) {

        System.out.println("--- Calculating eCommerce statistics of user \"" + credentials.username + "\" ...");

        logIn(credentials)
                .thenCompose(username -> processOrdersOf(username))     // flatMap of CompletionStage
                .whenComplete((result, t) -> {
                    if (t == null) {            // no exception occurred
                        result.display();
                    } else {                    // exception occurred
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
