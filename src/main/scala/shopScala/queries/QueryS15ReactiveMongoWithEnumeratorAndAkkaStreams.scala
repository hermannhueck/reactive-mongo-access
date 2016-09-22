package shopScala.queries

import java.util.concurrent.CountDownLatch

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import org.reactivestreams.Publisher
import play.api.libs.iteratee.Enumerator
import play.api.libs.streams.Streams
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.{DefaultDB, MongoConnection, MongoDriver}
import reactivemongo.bson.BSONDocument
import shopScala.util.Constants._
import shopScala.util.Util._
import shopScala.util._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

/*
    For ReactiveMongo see:
      http://reactivemongo.org/
      http://reactivemongo.org/releases/0.11/documentation/index.html
      https://github.com/ReactiveMongo/ReactiveMongo
      https://github.com/sgodbillon/reactivemongo-demo-app
 */

object QueryS15ReactiveMongoWithEnumeratorAndAkkaStreams extends App {

  object dao {

    val driver: MongoDriver = new MongoDriver
    val connection: MongoConnection = driver.connection(List(MONGODB_HOST_PORT))
    val fDatabase: Future[DefaultDB] = connection.database(SHOP_DB_NAME)
    val database = Await.result(fDatabase, Duration.Inf)
    val usersCollection = database.collection[BSONCollection](USERS_COLLECTION_NAME)
    val ordersCollection = database.collection[BSONCollection](ORDERS_COLLECTION_NAME)

    def close(): Unit = {
      connection.close()
      connection.actorSystem.terminate()
    }

    private def _findUserByName(name: String): Enumerator[User] = {
      usersCollection
        .find(BSONDocument("_id" -> name))
        .cursor[BSONDocument]()
        .enumerate()
        .map(User(_))
    }

    private def _findOrdersByUsername(username: String): Enumerator[Order] = {
      ordersCollection
        .find(BSONDocument("username" -> username))
        .cursor[BSONDocument]()
        .enumerate()
        .map(Order(_))
    }


    private def enumeratorToSource[T](enumerator: Enumerator[T]): Source[T, NotUsed] = {

      def toPublisher[T](enumerator: Enumerator[T]): Publisher[T] =
        Streams.enumeratorToPublisher(enumerator)

      def toSource[T](publisher: Publisher[T]): Source[T, NotUsed] =
        Source.fromPublisher(publisher)

      toSource(toPublisher(enumerator))
    }

    def findUserByName(name: String): Source[Option[User], NotUsed] = {
      enumeratorToSource(_findUserByName(name))
        .grouped(1)       // creates a List with max 1 element
        .map(_.headOption)
    }

    def findOrdersByUsername(username: String): Source[Order, NotUsed] = {
      enumeratorToSource(_findOrdersByUsername(username))
    }
  }   // end dao


  def logIn(credentials: Credentials): Source[String, NotUsed] = {
    dao.findUserByName(credentials.username)
      .map(optUser => checkUserLoggedIn(optUser, credentials))
      .map(user => user.name)
  }

  def processOrdersOf(username: String): Source[Result, NotUsed] = {
    dao.findOrdersByUsername(username)
      .grouped(1000)       // creates a List with max 1000 elements
      .map(orders => new Result(username, orders))
  }

  // val system = dao.connection.actorSystem
  val system = ActorSystem.create("Sys")
  implicit val materializer = ActorMaterializer.create(system)

  def eCommerceStatistics(credentials: Credentials, isLastInvocation: Boolean = false): Unit = {

    println(s"--- Calculating eCommerce statistics for user ${credentials.username} ...")

    val latch: CountDownLatch = new CountDownLatch(1)

    logIn(credentials)
      .flatMapMerge(1, username => processOrdersOf(username))
      .runForeach(result => result.display())
      .onComplete {
        case Success(_) =>
          latch.countDown()
          if (isLastInvocation) {
            system.terminate()
            dao.close()
          }
        case Failure(t) =>
          Console.err.println(t.toString)
          latch.countDown()
          if (isLastInvocation) {
            system.terminate()
            dao.close()
          }
      }

    latch.await()
  }

  eCommerceStatistics(Credentials(LISA, "password"))
  Thread sleep 2000L
  eCommerceStatistics(Credentials(LISA, "bad_password"))
  Thread sleep 2000L
  eCommerceStatistics(Credentials(LISA.toUpperCase, "password"), isLastInvocation = true)
}
