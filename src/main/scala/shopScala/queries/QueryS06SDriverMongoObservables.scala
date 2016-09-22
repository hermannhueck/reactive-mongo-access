package shopScala.queries

import java.util.concurrent.CountDownLatch

import org.mongodb.scala._
import org.mongodb.scala.model.Filters
import shopScala.util.Constants._
import shopScala.util.Util._
import shopScala.util._


object QueryS06SDriverMongoObservables extends App {

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

    def findUserByName(name: String): MongoObservable[Option[User]] = {
      _findUserByName(name)
        .collect()
        .map(seq => seq.headOption)
    }

    def findOrdersByUsername(username: String): MongoObservable[Order] = {
      _findOrdersByUsername(username)
    }
  }   // end dao


  def logIn(credentials: Credentials): MongoObservable[String] = {
    dao.findUserByName(credentials.username)
      // the Exception thrown by checkUserLoggedIn is swallowed by the MongoObservable implementation
      .map(optUser => checkUserLoggedIn(optUser, credentials))
      .map(user => {
        println("user -> user.name")
        user.name
      })
  }

  private def processOrdersOf(username: String): MongoObservable[Result] = {
    println("processOrdersOf: " + username)
    val obsOrders: MongoObservable[Order] = dao.findOrdersByUsername(username)
    val obsPairs: MongoObservable[(Int, Int)] = obsOrders.map(order => (order.amount, 1))
    val obsPair: MongoObservable[(Int, Int)] = obsPairs.foldLeft(0, 0)((t1, t2) => (t1._1 + t2._1, t1._2 + t2._2))
    obsPair.map(pair => Result(username, pair._2, pair._1))
  }

  def eCommerceStatistics(credentials: Credentials): Unit = {

    println(s"--- Calculating eCommerce statistics for user ${credentials.username} ...")

    val latch: CountDownLatch = new CountDownLatch(1)

    try {
      logIn(credentials)
        .flatMap(username => processOrdersOf(username))
        .subscribe(new Observer[Result] {

          override def onNext(result: Result): Unit = {
            println("=== onNext")
            result.display()
          }

          override def onComplete(): Unit = {
            println("=== onComplete")
            latch.countDown()
          }

          override def onError(t: Throwable): Unit = {
            println("=== onError")
            t.printStackTrace()
            latch.countDown()
          }
        })
    } catch {
      case t: Throwable =>
        t.printStackTrace()
        latch.countDown()
    }

    latch.await()
  }

  eCommerceStatistics(Credentials(LISA, "password"))
  Thread sleep 2000L
  eCommerceStatistics(Credentials(LISA, "bad password"))
  Thread sleep 2000L
  eCommerceStatistics(Credentials(LISA.toUpperCase, "password"))
}
