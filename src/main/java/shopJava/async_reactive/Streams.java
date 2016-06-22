package shopJava.async_reactive;

import com.mongodb.rx.client.MongoClient;
import com.mongodb.rx.client.MongoClients;
import com.mongodb.rx.client.MongoCollection;
import com.mongodb.rx.client.MongoDatabase;
import org.bson.Document;
import rx.Observable;
import rx.Observer;
import shopJava.model.User;

import static shopJava.util.Constants.SHOP_DB_NAME;
import static shopJava.util.Constants.USERS_COLLECTION_NAME;
import static shopJava.util.Util.sleep;

@SuppressWarnings("Convert2MethodRef")
public class Streams {

    public static void main(String[] args) {
        new Streams();
    }

    private final MongoClient client = MongoClients.create();
    private final MongoDatabase db = client.getDatabase(SHOP_DB_NAME);
    private final MongoCollection<Document> usersCollection = db.getCollection(USERS_COLLECTION_NAME);

    private Streams() {

        Observable<String> obsSimpsons = nonblockingIOWithStreams_GetDataFromDB();

        obsSimpsons.subscribe(new Observer<String>() {

            @Override
            public void onNext(String simpson) {
                System.out.println(simpson);
            }

            @Override
            public void onCompleted() {
                System.out.println("----- DONE -----");
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
            }
        });

        sleep(2);
    }

    private Observable<String> nonblockingIOWithStreams_GetDataFromDB() {

        return usersCollection
                .find()
                .toObservable()
                .map(doc -> new User(doc))
                .map(user -> user.name);
    }
}
