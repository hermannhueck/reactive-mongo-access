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

    val client: MongoClient = MongoClient(MONGODB_URI)
    val db: MongoDatabase = client.getDatabase(SHOP_DB_NAME)
    val usersCollection: MongoCollection[Document] = db.getCollection(USERS_COLLECTION_NAME)
    val ordersCollection: MongoCollection[Document] = db.getCollection(ORDERS_COLLECTION_NAME)

    private def _findUserByName(name: String): MongoObservable[User] = {
      usersCollection
        .find(Filters.eq("_id", name))
        .first()
        .map(doc => User(doc))
    }

    private def _findOrdersByUsername(username: String): MongoObservable[Order] = {
      ordersCollection
        .find(Filters.eq("username", username))
        .map(doc => Order(doc))
    }

    def findUserByName(name: String): rx.Observable[User] = {
      RxScalaConversions.observableToRxObservable(_findUserByName(name)).single
    }

    def findOrdersByUsername(username: String): rx.Observable[Order] = {
      RxScalaConversions.observableToRxObservable(_findOrdersByUsername(username))
    }
  }   // end dao


  def logIn(credentials: Credentials): rx.Observable[String] = {
    dao.findUserByName(credentials.username)
      .map(user => checkCredentials(user, credentials))
      .map(user => user.name)
  }

  private def processOrdersOf(username: String): rx.Observable[Result] = {
    val obsOrders: rx.Observable[Order] = dao.findOrdersByUsername(username)
    val obsPairs: rx.Observable[(Int, Int)] = obsOrders.map(order => (order.amount, 1))
    val obsPair: rx.Observable[(Int, Int)] = obsPairs.foldLeft(0, 0)((t1, t2) => (t1._1 + t2._1, t1._2 + t2._2))
    obsPair.map(pair => Result(username, pair._2, pair._1))
  }

  def eCommerceStatistics(credentials: Credentials): Unit = {

    println(s"--- Calculating eCommerce statistics for user ${credentials.username} ...")

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
