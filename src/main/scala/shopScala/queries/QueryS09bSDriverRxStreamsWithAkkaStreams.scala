package shopScala.queries

import java.util.concurrent.CountDownLatch

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.{Done, NotUsed}
import org.mongodb.scala._
import org.mongodb.scala.model.Filters
import org.reactivestreams.Publisher
import shopScala.util.Constants._
import shopScala.util.Util._
import shopScala.util._
import shopScala.util.conversion.RxStreamsConversions

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

/*
    For conversions from MongoDBObservable to RxStreams Publisher see:
    https://mongodb.github.io/mongo-scala-driver/1.1/integrations/
    https://github.com/mongodb/mongo-scala-driver/tree/master/examples/src/test/scala/rxStreams
 */

object QueryS09bSDriverRxStreamsWithAkkaStreams extends App {

  type MongoObservable[T] = org.mongodb.scala.Observable[T]

  object dao {

    val client: MongoClient = MongoClient(MONGODB_URI)
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

    private def _findOrdersByUsername(username: String): MongoObservable[Order] = {
      ordersCollection
        .find(Filters.eq("username", username))
        .map(doc => Order(doc))
    }

    def findUserByName(name: String): Publisher[Option[User]] = {
      RxStreamsConversions.observableToPublisher(_findUserByName(name))
    }

    def findOrdersByUsername(username: String): Publisher[Order] = {
      RxStreamsConversions.observableToPublisher(_findOrdersByUsername(username))
    }
  }   // end dao


  def logIn(credentials: Credentials): Source[String, NotUsed] = {
    val srcUsers: Source[Option[User], NotUsed] = Source.fromPublisher(dao.findUserByName(credentials.username))
    srcUsers.map(user => checkUserLoggedIn(user, credentials))
      .map(user => user.name)
  }

  def processOrdersOf(username: String): Source[Result, NotUsed] = {
    val srcOrders: Source[Order, NotUsed] = Source.fromPublisher(dao.findOrdersByUsername(username))
    val srcPairs: Source[(Int, Int), NotUsed] = srcOrders.map(order => (order.amount, 1))
    val srcPair: Source[(Int, Int), NotUsed] = srcPairs.fold(0, 0)((t1, t2) => (t1._1 + t2._1, t1._2 + t2._2))
    srcPair.map(pair => Result(username, pair._2, pair._1))
  }

  val system = ActorSystem.create("Sys")
  implicit val materializer = ActorMaterializer.create(system)

  def eCommerceStatistics(credentials: Credentials, isLastInvocation: Boolean = false): Unit = {

    println(s"--- Calculating eCommerce statistics for user ${credentials.username} ...")

    val latch: CountDownLatch = new CountDownLatch(1)

    val f: Future[Done] = logIn(credentials)
      .flatMapMerge(1, username => processOrdersOf(username))
      .runForeach(result => result.display())

    //noinspection MatchToPartialFunction
    f.onComplete(tryy => tryy match {
        case Success(_) =>
          latch.countDown()
          if (isLastInvocation)
            system.terminate()
        case Failure(t) =>
          Console.err.println(t.toString)
          latch.countDown()
          if (isLastInvocation)
            system.terminate()
      })

    latch.await()
  }

  eCommerceStatistics(Credentials(LISA, "password"))
  Thread sleep 2000L
  eCommerceStatistics(Credentials(LISA, "bad_password"))
  Thread sleep 2000L
  eCommerceStatistics(Credentials(LISA.toUpperCase, "password"), isLastInvocation = true)
}
