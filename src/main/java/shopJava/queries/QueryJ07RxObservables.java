package shopJava.queries;

import com.mongodb.rx.client.MongoClient;
import com.mongodb.rx.client.MongoClients;
import com.mongodb.rx.client.MongoCollection;
import com.mongodb.rx.client.MongoDatabase;
import org.bson.Document;
import rx.Observable;
import rx.Observer;
import rx.Single;
import shopJava.model.*;

import java.util.concurrent.CountDownLatch;

import static com.mongodb.client.model.Filters.eq;
import static java.lang.Thread.sleep;
import static shopJava.util.Constants.*;
import static shopJava.util.Util.average;
import static shopJava.util.Util.checkUserLoggedIn;

@SuppressWarnings("Convert2MethodRef")
public class QueryJ07RxObservables {

    public static void main(String[] args) throws Exception {
        new QueryJ07RxObservables();
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

        private Single<User> _findUserByName(final String name) {
            return usersCollection
                    .find(eq("_id", name))
                    .first()
                    .toSingle()
                    .map(doc -> new User(doc));
        }

        private Observable<Order> _findOrdersByUsername(final String username) {
            return ordersCollection
                    .find(eq("username", username))
                    .toObservable()
                    .map(doc -> new Order(doc));
        }

        Single<User> findUserByName(final String name) {
            return _findUserByName(name);
        }

        Observable<Order> findOrdersByUsername(final String username) {
            return _findOrdersByUsername(username);
        }
    }   // end DAO


    private Single<String> logIn(final Credentials credentials) {
        return dao.findUserByName(credentials.username)
                .map(user -> checkUserLoggedIn(user, credentials))
                .map(user -> user.name);
    }

    private Observable<Result> processOrdersOf(final String username) {
        return dao.findOrdersByUsername(username)
                .map(order -> new IntPair(order.amount, 1))
                .reduce(new IntPair(0, 0), (p1, p2) -> new IntPair(p1.first + p2.first, p1.second + p2.second))
                .map(pair -> new Result(username, pair.second, pair.first));
    }

    @SuppressWarnings("unused")
    private void eCommerceStatistics_withObserver(final Credentials credentials) throws Exception {

        System.out.println("--- Calculating eCommerce statistics for user \"" + credentials.username + "\" ...");

        final CountDownLatch latch = new CountDownLatch(1);

        logIn(credentials).toObservable()
                .flatMap(username -> processOrdersOf(username))
                .subscribe(new Observer<Result>() {
                    @Override public void onNext(Result result) {
                        result.display();
                    }
                    @Override public void onError(Throwable t) {
                        System.err.println(t.toString());
                        latch.countDown();
                    }
                    @Override public void onCompleted() {
                        latch.countDown();
                    }
                });

        latch.await();
    }

    private void eCommerceStatistics_withLambdas(final Credentials credentials) throws Exception {

        System.out.println("--- Calculating eCommerce statistics for user \"" + credentials.username + "\" ...");

        final CountDownLatch latch = new CountDownLatch(1);

        logIn(credentials).toObservable()
                .flatMap(username -> processOrdersOf(username))
                .subscribe(
                        result -> result.display(),
                        t -> { System.err.println(t.toString()); latch.countDown(); },
                        () -> latch.countDown()
                );

        latch.await();
    }

    private void eCommerceStatistics(final Credentials credentials) throws Exception {
        eCommerceStatistics_withLambdas(credentials);
    }

    private QueryJ07RxObservables() throws Exception {

        eCommerceStatistics(new Credentials(LISA, "password"));
        sleep(2000L);
        eCommerceStatistics(new Credentials(LISA, "bad_password"));
        sleep(2000L);
        eCommerceStatistics(new Credentials(LISA.toUpperCase(), "password"));
    }
}
