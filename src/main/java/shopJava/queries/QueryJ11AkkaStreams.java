package shopJava.queries;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Source;
import com.mongodb.rx.client.MongoClient;
import com.mongodb.rx.client.MongoClients;
import com.mongodb.rx.client.MongoCollection;
import com.mongodb.rx.client.MongoDatabase;
import org.bson.Document;
import rx.Observable;
import shopJava.model.Credentials;
import shopJava.model.Order;
import shopJava.model.Result;
import shopJava.model.User;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;

import static com.mongodb.client.model.Filters.eq;
import static java.lang.Thread.sleep;
import static rx.RxReactiveStreams.toPublisher;
import static shopJava.util.Constants.*;
import static shopJava.util.Util.checkUserLoggedIn;

@SuppressWarnings("Convert2MethodRef")
public class QueryJ11AkkaStreams {

    public static void main(String[] args) throws Exception {
        new QueryJ11AkkaStreams();
    }

    private final DAO dao = new DAO();

    private class DAO {

        private final MongoCollection<Document> usersCollection;
        private final MongoCollection<Document> ordersCollection;

        DAO() {
            final MongoClient client = MongoClients.create();
            final MongoDatabase db = client.getDatabase(SHOP_DB_NAME);
            this.usersCollection = db.getCollection(USERS_COLLECTION_NAME);
            this.ordersCollection = db.getCollection(ORDERS_COLLECTION_NAME);
        }

        private Observable<Optional<User>> _findUserByName(final String name) {
            return usersCollection
                    .find(eq("_id", name))
                    .first()
                    .map(doc -> new User(doc))      // no null check as we don't get null objects in the stream
                    .toList()   // conversion to List to check whether we found a user with the specified name
                    .map(users -> users.size() == 0 ? Optional.empty() : Optional.of(users.get(0)));
        }

        private Observable<Order> _findOrdersByUsername(final String username) {
            return ordersCollection
                    .find(eq("username", username))
                    .toObservable()
                    .map(doc -> new Order(doc));
        }

        Source<Optional<User>, NotUsed> findUserByName(final String name) {
            return Source.fromPublisher(toPublisher(_findUserByName(name)));
        }

        Source<List<Order>, NotUsed> findOrdersByUsername(final String username) {
            return Source.fromPublisher(toPublisher(_findOrdersByUsername(username).toList()));
        }
    }   // end DAO


    private Source<String, NotUsed> logIn(final Credentials credentials) {
        return dao.findUserByName(credentials.username)
                .map(optUser -> checkUserLoggedIn(optUser, credentials))
                .map(user -> user.name);
    }

    private Source<Result, NotUsed> processOrdersOf(final String username) {
        return dao.findOrdersByUsername(username)
                .map(orders -> new Result(username, orders));
    }

    private final ActorSystem system = ActorSystem.create("Sys");
    private final ActorMaterializer materializer = ActorMaterializer.create(system);

    private void eCommerceStatistics(final Credentials credentials, final boolean isLastInvocation) throws Exception {

        System.out.println("--- Calculating eCommerce statistings for user \"" + credentials.username + "\" ...");

        final CountDownLatch latch = new CountDownLatch(1);

        logIn(credentials)
                .flatMapMerge(1, username -> processOrdersOf(username))
                .runForeach(result -> result.display(), materializer)
                .whenComplete((done, t) -> {
                    if (t != null) {
                        System.err.println(t.toString());
                    }
                    latch.countDown();
                    if (isLastInvocation) {
                        system.terminate();
                    }
                });

        latch.await();
    }

    private QueryJ11AkkaStreams() throws Exception {

        eCommerceStatistics(new Credentials(LISA, "password"), false);
        sleep(2000L);
        eCommerceStatistics(new Credentials(LISA, "bad_password"), false);
        sleep(2000L);
        eCommerceStatistics(new Credentials(LISA.toUpperCase(), "password"), true);
    }
}
