package shopScala.util

import org.mongodb.scala.bson.collection.immutable.Document
import reactivemongo.bson.BSONDocument

case class Credentials(username: String, password: String)

object Order {

  val ID = "_id"
  val USERNAME = "username"
  val AMOUNT = "amount"

  def apply(doc: Document): Order =
    apply(doc.get(ID).get.asInt32.intValue, doc.get(USERNAME).get.asString.getValue, doc.get(AMOUNT).get.asInt32.intValue)

  def apply(doc: BSONDocument): Order =
    apply(doc.getAs[Int](ID).get, doc.getAs[String](USERNAME).get, doc.getAs[Int](AMOUNT).get)

  def apply(doc: org.bson.Document): Order =
    apply(doc.getInteger(ID), doc.getString(USERNAME), doc.getInteger(AMOUNT))
}

case class Order(id: Int, username: String, amount: Int) {

  import Order._

  def toDocument = Document(ID -> id, USERNAME -> username, AMOUNT -> amount)
}

object User {

  val ID = "_id"
  val NAME = "name"
  val PASSWORD = "password"

  def apply(doc: Document): User = apply(doc.get(ID).get.asString.getValue, doc.get(PASSWORD).get.asString.getValue)

  def apply(doc: BSONDocument): User = apply(doc.getAs[String](ID).get, doc.getAs[String](PASSWORD).get)

  def apply(doc: org.bson.Document): User = apply(doc.getString(ID), doc.getString(PASSWORD))
}

case class User(name: String, password: String) {

  import User._

  def toDocument = Document(ID -> name, PASSWORD -> password)
}

case class Result(username: String, orders: Seq[Order]) {

  val orderCount: Int = orders.length
  val totalAmount: Int = orders.map(_.amount).sum
  val avgAmount: Int = ((100.0f * totalAmount / orderCount) / 100).round

  def display(): Unit = {
    println("--------------------------------------------")
    println("eCommerce Orders of User \"" + username + "\":")
    println("\tNo of orders: " + orderCount)
    println("\tTotal Amount: " + totalAmount / 100.0 + " $")
    println("\tAverage Amount: " + avgAmount / 100.0 + " $")
    println("--------------------------------------------\n")
  }
}
