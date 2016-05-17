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

object QueryS10AkkaStreams extends App {

  object dao {

    val client: MongoClient = MongoClient()
    val db: MongoDatabase = client.getDatabase(SHOP_DB_NAME)
    val usersCollection: MongoCollection[Document] = db.getCollection(USERS_COLLECTION_NAME)
    val ordersCollection: MongoCollection[Document] = db.getCollection(ORDERS_COLLECTION_NAME)

    def findUserByName(name: String): Source[Option[User], NotUsed] = {
      val obs: org.mongodb.scala.Observable[Option[User]] = usersCollection
        .find(Filters.eq("_id", name))
        .first()
        .map(doc => User(doc))
        .collect()
        .map(seq => seq.headOption)
      Source.fromPublisher(RxStreamsConversions.observableToPublisher(obs))
    }

    def findOrdersByUsername(username: String): Source[Seq[Order], NotUsed] = {
      val obs: org.mongodb.scala.Observable[Seq[Order]] = ordersCollection
        .find(Filters.eq("username", username))
        .map(doc => Order(doc))
        .collect()
      Source.fromPublisher(RxStreamsConversions.observableToPublisher(obs))
    }
  }   // end dao


  def logIn(credentials: Credentials): Source[String, NotUsed] = {
    dao.findUserByName(credentials.username)
      .map(user => checkUserLoggedIn(user, credentials))
      .map(user => user.name)
  }

  def processOrdersOf(username: String): Source[Result, NotUsed] = {
    dao.findOrdersByUsername(username)
      .map(orders => new Result(username, orders))
  }

  val system = ActorSystem.create("Sys")
  implicit val materializer = ActorMaterializer.create(system)

  def eCommerceStatistics(credentials: Credentials, isLastInvocation: Boolean = false): Unit = {

    println("--- Calculating eCommerce statistings for user \"" + credentials.username + "\" ...")

    val latch: CountDownLatch = new CountDownLatch(1)

    logIn(credentials)
      .flatMapMerge(1, username => processOrdersOf(username))
      .runForeach(result => result.display())
      .onComplete {
        case Success(_) =>
          latch.countDown()
          if (isLastInvocation)
            system.terminate()
        case Failure(t) =>
          Console.err.println(t.toString)
          latch.countDown()
          if (isLastInvocation)
            system.terminate()
      }

    latch.await()
  }

  eCommerceStatistics(Credentials(LISA, "password"))
  Thread sleep 2000L
  eCommerceStatistics(Credentials(LISA, "bad password"))
  Thread sleep 2000L
  eCommerceStatistics(Credentials(LISA.toUpperCase, "password"), isLastInvocation = true)
}
