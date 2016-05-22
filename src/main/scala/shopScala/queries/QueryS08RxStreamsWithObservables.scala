package shopScala.queries

import java.util.concurrent.CountDownLatch

import org.mongodb.scala._
import org.mongodb.scala.model.Filters
import org.reactivestreams.Publisher
import shopScala.util.Constants._
import shopScala.util.Util._
import shopScala.util._
import shopScala.util.conversion.RxStreamsConversions

/*
    For conversions from MongoDBObservable to RxStreams Publisher see:
    https://mongodb.github.io/mongo-scala-driver/1.1/integrations/
    https://github.com/mongodb/mongo-scala-driver/tree/master/examples/src/test/scala/rxStreams
 */

object QueryS08RxStreamsWithObservables extends App {

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

    def findUserByName(name: String): Publisher[Option[User]] = {
      RxStreamsConversions.observableToPublisher(_findUserByName(name))
    }

    def findOrdersByUsername(username: String): Publisher[Seq[Order]] = {
      RxStreamsConversions.observableToPublisher(_findOrdersByUsername(username))
    }
  }   // end dao


  def logIn(credentials: Credentials): rx.lang.scala.Observable[String] = {
    publisherToObservable(dao.findUserByName(credentials.username))
      .map(optUser => checkUserLoggedIn(optUser, credentials))
      .map(user => user.name)
  }

  def processOrdersOf(username: String): rx.lang.scala.Observable[Result] = {
    publisherToObservable(dao.findOrdersByUsername(username))
      .map(orders => new Result(username, orders))
  }

  def publisherToObservable[T](pub: Publisher[T]): rx.lang.scala.Observable[T] = {
    val javaObs: rx.Observable[T] = rx.RxReactiveStreams.toObservable(pub)
    rx.lang.scala.JavaConversions.toScalaObservable(javaObs)
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
