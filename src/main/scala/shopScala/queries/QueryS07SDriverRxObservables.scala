package shopScala.queries

import java.util.concurrent.CountDownLatch

import org.mongodb.scala._
import org.mongodb.scala.model.Filters
import rx.lang.{scala => rx}
import shopScala.util.Constants._
import shopScala.util.Util._
import shopScala.util._
import shopScala.util.conversion.RxScalaConversions

/*
    For conversions from MongoDBObservable to RxScala Observable see:
    https://mongodb.github.io/mongo-scala-driver/1.1/integrations/
    https://github.com/mongodb/mongo-scala-driver/tree/master/examples/src/test/scala/rxScala
 */

object QueryS07SDriverRxObservables extends App {

  type MongoObservable[T] = org.mongodb.scala.Observable[T]

  object dao {

    val client: MongoClient = MongoClient()
    val db: MongoDatabase = client.getDatabase(SHOP_DB_NAME)
    val usersCollection: MongoCollection[Document] = db.getCollection(USERS_COLLECTION_NAME)
    val ordersCollection: MongoCollection[Document] = db.getCollection(ORDERS_COLLECTION_NAME)

    private def _findUserByName(name: String): MongoObservable[Option[User]] = {
      usersCollection
        .find(Filters.eq("_id", name))
        .first()
        .map(doc => User(doc))
        .collect()
        .map(seq => seq.headOption)
    }

    private def _findOrdersByUsername(username: String): MongoObservable[Seq[Order]] = {
      ordersCollection
        .find(Filters.eq("username", username))
        .map(doc => Order(doc))
        .collect()
    }

    def findUserByName(name: String): rx.Observable[Option[User]] = {
      RxScalaConversions.observableToRxObservable(_findUserByName(name))
    }

    def findOrdersByUsername(username: String): rx.Observable[Seq[Order]] = {
      RxScalaConversions.observableToRxObservable(_findOrdersByUsername(username))
    }
  }   // end dao


  def logIn(credentials: Credentials): rx.Observable[String] = {
    dao.findUserByName(credentials.username)
      .map(optUser => checkUserLoggedIn(optUser, credentials))
      .map(user => user.name)
  }

  private def processOrdersOf(username: String): rx.Observable[Result] = {
    dao.findOrdersByUsername(username)
      .map(orders => new Result(username, orders))
  }

  def eCommerceStatistics(credentials: Credentials): Unit = {

    println("--- Calculating eCommerce statistings for user \"" + credentials.username + "\" ...")

    val latch: CountDownLatch = new CountDownLatch(1)

    logIn(credentials)
      .flatMap(username => processOrdersOf(username))
      .subscribe(result => result.display(),
        t => { Console.err.println(t.toString); latch.countDown()},
        () => latch.countDown()
      )

    latch.await()
  }

  eCommerceStatistics(Credentials(LISA, "password"))
  Thread sleep 2000L
  eCommerceStatistics(Credentials(LISA, "bad_password"))
  Thread sleep 2000L
  eCommerceStatistics(Credentials(LISA.toUpperCase, "password"))
}
