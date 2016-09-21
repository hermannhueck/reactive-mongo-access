package shopJava.queries;

import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.async.client.MongoDatabase;
import org.bson.Document;
import shopJava.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

import static com.mongodb.client.model.Filters.eq;
import static java.lang.Thread.sleep;
import static shopJava.util.Constants.*;
import static shopJava.util.Util.average;
import static shopJava.util.Util.checkUserLoggedIn;

@SuppressWarnings("Convert2MethodRef")
public class QueryJ05Callback {

    public static void main(String[] args) throws Exception {
        new QueryJ05Callback();
    }

    private final DAO dao = new DAO();

    private class DAO {

        private final MongoCollection<Document> usersCollection;
        private final MongoCollection<Document> ordersCollection;

        DAO() {
            final MongoClient client = MongoClients.create(MONGODB_URI);
            final MongoDatabase db = client.getDatabase(SHOP_DB_NAME);
            this.usersCollection = db.getCollection(USERS_COLLECTION_NAME);
            this.ordersCollection = db.getCollection(ORDERS_COLLECTION_NAME);
        }

        private void _findUserByName(final String name, SingleResultCallback<Optional<User>> callback) {
            usersCollection
                    .find(eq("_id", name))
                    .map(doc -> Optional.ofNullable(doc).map(User::new))
                    .first(callback);
        }

        private void _findOrdersByUsername(final String username, final SingleResultCallback<List<Order>> callback) {
            ordersCollection
                    .find(eq("username", username))
                    .map(doc -> new Order(doc))
                    .into(new ArrayList<>(), callback);
        }

        void findUserByName(final String name, SingleResultCallback<Optional<User>> callback) {
            _findUserByName(name, callback);
        }

        void findOrdersByUsername(final String username, final SingleResultCallback<List<Order>> callback) {
            _findOrdersByUsername(username, callback);
        }
    }   // end DAO


    private void findUser(final String username, SingleResultCallback<Optional<User>> callback) {
        dao.findUserByName(username, callback);
    }

    private void processOrdersOf(String username, SingleResultCallback<List<Order>> callback) {
        dao.findOrdersByUsername(username, callback);
    }

    private void eCommerceStatistics(final Credentials credentials) throws Exception {

        System.out.println("--- Calculating eCommerce statistings of user \"" + credentials.username + "\" ...");

        final CountDownLatch latch = new CountDownLatch(1);

        findUser(credentials.username, (optUser, t1) -> {               // 1st callback

            try {
                if (t1 != null) {
                    throw t1;
                }

                checkUserLoggedIn(optUser, credentials);

                processOrdersOf(credentials.username, (orders, t2) -> {     // 2nd encapsulated callback

                    if (t2 != null) {
                        t2.printStackTrace();
                        return;
                    }

                    final Stream<Order> orderStream = orders.stream();
                    final Stream<IntPair> pairStream = orderStream.map(order -> new IntPair(order.amount, 1));
                    final IntPair pair = pairStream.reduce(new IntPair(0, 0), (p1, p2) -> new IntPair(p1.first + p2.first, p1.second + p2.second));
                    final Result result = new  Result(credentials.username, pair.second, pair.first, average(pair.first, pair.second));
                    result.display();
                });

            } catch (Throwable t) {
                System.err.println(t.toString());
            } finally {
                latch.countDown();
            }
        });

        latch.await();
    }

    private QueryJ05Callback() throws Exception {

        eCommerceStatistics(new Credentials(LISA, "password"));
        sleep(2000L);
        eCommerceStatistics(new Credentials(LISA, "bad_password"));
        sleep(2000L);
        eCommerceStatistics(new Credentials(LISA.toUpperCase(), "password"));
    }
}
