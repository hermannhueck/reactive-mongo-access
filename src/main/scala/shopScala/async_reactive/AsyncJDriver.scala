package shopScala.async_reactive

import java.util

import com.mongodb.{MongoClient, MongoClientURI}
import com.mongodb.client.{MongoCollection, MongoDatabase}
import org.bson.Document
import shopScala.util.Constants._
import shopScala.util.User
import shopScala.util.Util._

object AsyncJDriver extends App {

  type JList[T] = util.List[T]
  type JArrayList[T] = util.ArrayList[T]

  val client: MongoClient = new MongoClient(new MongoClientURI(MONGODB_URI))
  val db: MongoDatabase = client.getDatabase(SHOP_DB_NAME)
  val usersCollection: MongoCollection[Document] = db.getCollection(USERS_COLLECTION_NAME)

  def blockingIO_GetDataFromDB: Seq[String] = {
    val jDocs: JList[Document] = usersCollection
      .find()
      .into(new JArrayList[Document])
    jListToSeq(jDocs).map(User(_)).map(_.name)
  }

  val r: Runnable = new Runnable {
    def run(): Unit = {
      val simpsons: Seq[String] = blockingIO_GetDataFromDB
      simpsons.foreach(println)
    }
  }

  new Thread(r).start()
}
