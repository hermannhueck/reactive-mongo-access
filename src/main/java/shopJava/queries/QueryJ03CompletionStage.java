package shopJava.queries;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import shopJava.model.Order;
import shopJava.model.User;
import shopJava.model.Credentials;
import shopJava.model.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

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

        CompletionStage<Optional<User>> findUserByName(final String name) {
            return CompletableFuture.supplyAsync(() -> {
                Document doc = usersCollection.find(eq("_id", name)).first();
                return doc == null ? Optional.empty() : Optional.of(new User(doc));
            });
        }

        CompletionStage<List<Order>> findOrdersByUsername(final String username) {
            return CompletableFuture.supplyAsync(() -> {
                List<Document> docs = ordersCollection.find(eq("username", username)).into(new ArrayList<>());
                return docs.stream().map(doc -> new Order(doc)).collect(toList());
            });
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

    private void eCommercStatistics(final Credentials credentials) {

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

        eCommercStatistics(new Credentials(LISA, "password"));
        sleep(2000L);
        eCommercStatistics(new Credentials(LISA, "bad_password"));
        sleep(2000L);
        eCommercStatistics(new Credentials(LISA.toUpperCase(), "password"));
    }
}
