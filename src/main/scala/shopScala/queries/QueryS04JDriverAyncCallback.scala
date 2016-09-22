package shopScala.queries

import java.util.concurrent.CountDownLatch

import com.mongodb.async.SingleResultCallback
import com.mongodb.async.client.{MongoClient, MongoClients, MongoCollection, MongoDatabase}
import com.mongodb.client.model.Filters
import org.bson.Document
import shopScala.util.Constants._
import shopScala.util.Util._
import shopScala.util._


object QueryS04JDriverAyncCallback extends App {

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

    private def _findOrdersByUsername(username: String,
                                      callback: SingleResultCallback[JList[Order]]): Unit = {
      ordersCollection
        .find(Filters.eq("username", username))
        .map(scalaMapper2MongoMapper { doc => Order(doc) })
        .into(new JArrayList[Order], callback)
    }

    def findUserByName(name: String, callback: SingleResultCallback[Option[User]]): Unit =
      _findUserByName(name, callback)

    def findOrdersByUsername(username: String,
                             callback: SingleResultCallback[JList[Order]]): Unit =
      _findOrdersByUsername(username, callback)
  }   // end dao


  def logIn(username: String, callback: SingleResultCallback[Option[User]]): Unit = {
    dao.findUserByName(username, callback)
  }

  private def processOrdersOf(username: String, callback: SingleResultCallback[JList[Order]]): Unit = {
    dao.findOrdersByUsername(username, callback)
  }

  def eCommerceStatistics(credentials: Credentials): Unit = {

    println(s"--- Calculating eCommerce statistics for user ${credentials.username} ...")

    val latch: CountDownLatch = new CountDownLatch(1)

    logIn(credentials.username, new SingleResultCallback[Option[User]]() {

      override def onResult(optUser: Option[User], t1: Throwable): Unit = {

        try {
          if (t1 != null) {
            throw t1
          } else {

            checkUserLoggedIn(optUser, credentials)

            processOrdersOf(credentials.username, new SingleResultCallback[JList[Order]]() {

              override def onResult(orders: JList[Order], t2: Throwable): Unit = {
                if (t2 != null) {
                  t2.printStackTrace()
                } else {
                  val (totalAmount, orderCount) = jListToSeq(orders)
                    .map(order => (order.amount, 1))
                    .fold(0, 0)((t1, t2) => (t1._1 + t2._1, t1._2 + t2._2))
                  Result(credentials.username, orderCount, totalAmount).display()
                }
              }
            })
          }
        } catch {
          case t: Throwable =>
            Console.err.println(t.toString);
        } finally {
          latch.countDown()
        }
      }
    })

    latch.await()
  }

  eCommerceStatistics(Credentials(LISA, "password"))
  Thread sleep 2000L
  eCommerceStatistics(Credentials(LISA, "bad_password"))
  Thread sleep 2000L
  eCommerceStatistics(Credentials(LISA.toUpperCase, "password"))
}
