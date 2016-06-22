package shopScala.async_reactive

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoClient
import shopScala.util.Constants._
import shopScala.util.User

object AsyncCasbah extends App {

  val client: MongoClient = MongoClient()
  val db: MongoDB = client(SHOP_DB_NAME)
  val usersCollection: MongoCollection = db(USERS_COLLECTION_NAME)

  def blockingIO_GetDataFromDB: Seq[String] = {
    usersCollection
      .find()
      .toSeq
      .map(User(_))
      .map(_.name)
  }

  val r: Runnable = new Runnable {
    def run(): Unit = {
      val simpsons: Seq[String] = blockingIO_GetDataFromDB
      simpsons.foreach(println)
    }
  }

  new Thread(r).start()
}
