package shopScala.queries

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoClient
import shopScala.util.Constants._
import shopScala.util.Util._
import shopScala.util._


object QueryS01CasbahSync extends App {

  object dao {

    val client: MongoClient = MongoClient(new MongoClientURI(MONGODB_URI))
    val db: MongoDB = client(SHOP_DB_NAME)
    val usersCollection: MongoCollection = db(USERS_COLLECTION_NAME)
    val ordersCollection: MongoCollection = db(ORDERS_COLLECTION_NAME)

    private def _findUserByName(name: String): Option[User] = {
      usersCollection
        .findOne(MongoDBObject("_id" -> name))
        .map(User(_))
    }

    private def _findOrdersByUsername(username: String): Seq[Order] = {
      ordersCollection
        .find(MongoDBObject("username" -> username))
        .toSeq
        .map(Order(_))
    }

    def findUserByName(name: String): Option[User] = {
      _findUserByName(name)
    }

    def findOrdersByUsername(username: String): Seq[Order] = {
      _findOrdersByUsername(username)
    }
  }   // end dao


  def logIn(credentials: Credentials): String = {
    val optUser: Option[User] = dao.findUserByName(credentials.username)
    val user: User = checkUserLoggedIn(optUser, credentials)
    user.name
  }

  private def processOrdersOf(username: String): Result = {
    val (totalAmount, orderCount) = dao.findOrdersByUsername(username)
        .map(order => (order.amount, 1))
        .fold(0, 0)((t1, t2) => (t1._1 + t2._1, t1._2 + t2._2))
    Result(username, orderCount, totalAmount)
  }

  def eCommerceStatistics(credentials: Credentials): Unit = {

    println(s"--- Calculating eCommerce statistics for user ${credentials.username} ...")

    try {
      val username: String = logIn(credentials)
      val result: Result = processOrdersOf(username)
      result.display()
    }
    catch {
      case t: Throwable =>
        Console.err.println(t.toString)
    }
  }

  eCommerceStatistics(Credentials(LISA, "password"))
  Thread sleep 2000L
  eCommerceStatistics(Credentials(LISA, "bad_password"))
  Thread sleep 2000L
  eCommerceStatistics(Credentials(LISA.toUpperCase, "password"))
}
