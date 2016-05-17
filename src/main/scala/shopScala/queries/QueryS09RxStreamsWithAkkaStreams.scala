package shopScala.queries

import java.util.concurrent.CountDownLatch

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import org.mongodb.scala._
import org.mongodb.scala.model.Filters
import org.reactivestreams.Publisher
import shopScala.util.Constants._
import shopScala.util.Util._
import shopScala.util._
import shopScala.util.conversion.RxStreamsConversions

import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/*
    For conversions from MongoDBObservable to RxStreams Publisher see:
    https://mongodb.github.io/mongo-scala-driver/1.1/integrations/
    https://github.com/mongodb/mongo-scala-driver/tree/master/examples/src/test/scala/rxStreams
 */

object QueryS09RxStreamsWithAkkaStreams extends App {

  object dao {

    val client: MongoClient = MongoClient()
    val db: MongoDatabase = client.getDatabase(SHOP_DB_NAME)
    val usersCollection: MongoCollection[Document] = db.getCollection(USERS_COLLECTION_NAME)
    val ordersCollection: MongoCollection[Document] = db.getCollection(ORDERS_COLLECTION_NAME)

    def findUserByName(name: String): Publisher[Option[User]] = {
      val obs: org.mongodb.scala.Observable[Option[User]] = usersCollection
        .find(Filters.eq("_id", name))
        .first()
        .map(doc => User(doc))
        .collect()
        .map(seq => seq.headOption)
      RxStreamsConversions.observableToPublisher(obs)
    }

    def findOrdersByUsername(username: String): Publisher[Seq[Order]] = {
      val obs: org.mongodb.scala.Observable[Seq[Order]] = ordersCollection
        .find(Filters.eq("username", username))
        .map(doc => Order(doc))
        .collect()
      RxStreamsConversions.observableToPublisher(obs)
    }
  }   // end dao


  def logIn(credentials: Credentials): Source[String, NotUsed] = {
    Source.fromPublisher(dao.findUserByName(credentials.username))
      .map(optUser => checkUserLoggedIn(optUser, credentials))
      .map(user => user.name)
  }

  def processOrdersOf(username: String): Source[Result, NotUsed] = {
    Source.fromPublisher(dao.findOrdersByUsername(username))
      .map(orders => new Result(username, orders))
  }

  val system = ActorSystem.create("Sys")
  implicit val materializer = ActorMaterializer.create(system)

  def eCommerceStatistics(credentials: Credentials, isLastInvocation: Boolean = false): Unit = {

    println("--- Calculating eCommerce statistings for user \"" + credentials.username + "\" ...")

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
  eCommerceStatistics(Credentials(LISA, "bad password"))
  Thread sleep 2000L
  eCommerceStatistics(Credentials(LISA.toUpperCase, "password"), isLastInvocation = true)
}
