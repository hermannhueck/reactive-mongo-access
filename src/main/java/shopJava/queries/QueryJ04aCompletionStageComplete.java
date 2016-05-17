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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.mongodb.client.model.Filters.eq;
import static java.lang.Thread.sleep;
import static java.util.stream.Collectors.toList;
import static shopJava.util.Constants.*;
import static shopJava.util.Util.checkUserLoggedIn;

@SuppressWarnings("Convert2MethodRef")
public class QueryJ04aCompletionStageComplete {

    private static final int nCores = Runtime.getRuntime().availableProcessors();
    private static final ExecutorService executor = Executors.newFixedThreadPool(nCores);

    public static void main(String[] args) throws Exception {
        new QueryJ04aCompletionStageComplete();
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

            final CompletableFuture<Optional<User>> future = new CompletableFuture<>();

            final Runnable runnable = () -> {
                try {
                    Document doc = usersCollection.find(eq("_id", name)).first();
                    Optional<User> optUser = doc == null ? Optional.empty() : Optional.of(new User(doc));
                    future.complete(optUser);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            };

            executor.execute(runnable);

            return future;
        }

        CompletionStage<List<Order>> findOrdersByUsername(final String username) {

            final CompletableFuture<List<Order>> future = new CompletableFuture<>();

            final Runnable runnable = () -> {
                try {
                    List<Document> docs = ordersCollection.find(eq("username", username)).into(new ArrayList<>());
                    List<Order> orders = docs.stream().map(doc -> new Order(doc)).collect(toList());
                    future.complete(orders);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            };

            executor.execute(runnable);

            return future;
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

    private void eCommercStatistics(final Credentials credentials, final boolean isLastInvocation) {

        System.out.println("--- Calculating eCommerce statistings for user \"" + credentials.username + "\" ...");

        logIn(credentials)
                .thenCompose(username -> processOrdersOf(username))     // flatMap of CompletionStage
                .whenComplete((result, t) -> {
                    if (t == null) {
                        result.display();
                    } else {
                        System.err.println(t.toString());
                    }
                    if (isLastInvocation) {
                        executor.shutdown();
                    }
                });
    }

    private QueryJ04aCompletionStageComplete() throws Exception {

        eCommercStatistics(new Credentials(LISA, "password"), false);
        sleep(2000L);
        eCommercStatistics(new Credentials(LISA, "bad_password"), false);
        sleep(2000L);
        eCommercStatistics(new Credentials(LISA.toUpperCase(), "password"), true);
    }
}
