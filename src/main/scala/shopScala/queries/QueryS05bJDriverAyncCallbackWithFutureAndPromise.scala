package shopScala.queries

import java.util.concurrent.CountDownLatch

import com.mongodb.async.SingleResultCallback
import com.mongodb.async.client.{MongoClient, MongoClients, MongoCollection, MongoDatabase}
import com.mongodb.client.model.Filters
import org.bson.Document
import shopScala.util.Constants._
import shopScala.util.Util._
import shopScala.util._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}


object QueryS05bJDriverAyncCallbackWithFutureAndPromise extends App {

  type JList[T] = java.util.List[T]
  type JArrayList[T] = java.util.ArrayList[T]

  object dao {

    val client: MongoClient = MongoClients.create(MONGODB_URI)
    val db: MongoDatabase = client.getDatabase(SHOP_DB_NAME)
    val usersCollection: MongoCollection[Document] = db.getCollection(USERS_COLLECTION_NAME)
    val ordersCollection: MongoCollection[Document] = db.getCollection(ORDERS_COLLECTION_NAME)

    private def _findUserByName(name: String, callback: SingleResultCallback[Option[User]]): Unit = {
      usersCollection
        .find(Filters.eq("_id", name))
        .map(scalaMapper2MongoMapper { doc => Option(doc).map(User(_)) })
        .first(callback)
    }

    private def _findOrdersByUsername(username: String, callback: SingleResultCallback[JList[Order]]): Unit = {
      ordersCollection
        .find(Filters.eq("username", username))
        .map(scalaMapper2MongoMapper { doc => Order(doc) })
        .into(new JArrayList[Order], callback)
    }

    class PromiseCallback[T1, T2](promise: Promise[T2], convert: T1 => T2) extends SingleResultCallback[T1] {

      override def onResult(result: T1, t: Throwable): Unit = {
        if (t == null) {
          promise.success(convert(result))
        } else {
          promise.failure(t)
        }
      }
    }

    def findUserByName(name: String): Future[Option[User]] = {

      val promise = Promise[Option[User]]

      _findUserByName(name, new PromiseCallback[Option[User], Option[User]](promise, optUser => optUser))

      promise.future
    }

    def findOrdersByUsername(username: String): Future[Seq[Order]] = {

      val promise = Promise[Seq[Order]]

      _findOrdersByUsername(username, new PromiseCallback[JList[Order], Seq[Order]](promise, orders => jListToSeq(orders)))

      promise.future
    }
  }   // end dao


  def logIn(credentials: Credentials): Future[String] = {
    dao.findUserByName(credentials.username)
      .map(optUser => checkUserLoggedIn(optUser, credentials))
      .map(user => user.name)
  }

  private def processOrdersOf(username: String): Future[Result] = {
    dao.findOrdersByUsername(username)
      .map(orders => {
        val (totalAmount, orderCount) =
          orders.map(order => (order.amount, 1))
            .fold(0, 0)((t1, t2) => (t1._1 + t2._1, t1._2 + t2._2))
        Result(username, orderCount, totalAmount)
      })
  }

  def eCommerceStatistics(credentials: Credentials): Unit = {

    println(s"--- Calculating eCommerce statistics for user ${credentials.username} ...")

    val latch: CountDownLatch = new CountDownLatch(1)

    logIn(credentials)
      .flatMap(username => processOrdersOf(username))
      .onComplete {
        case Success(result) =>
          result.display()
          latch.countDown()
        case Failure(t) =>
          Console.err.println(t.toString)
          latch.countDown()
      }

    latch.await()
  }

  eCommerceStatistics(Credentials(LISA, "password"))
  Thread sleep 2000L
  eCommerceStatistics(Credentials(LISA, "bad_password"))
  Thread sleep 2000L
  eCommerceStatistics(Credentials(LISA.toUpperCase, "password"))
}
