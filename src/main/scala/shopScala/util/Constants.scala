package shopScala.util

object Constants {

  val MONGODB_HOST_PORT: String = "localhost:27017"
  val MONGODB_URI: String = "mongodb://" + MONGODB_HOST_PORT
  val SHOP_DB_NAME: String = "shop"
  val USERS_COLLECTION_NAME: String = "users"
  val ORDERS_COLLECTION_NAME: String = "orders"

  val HOMER: String = "homer"
  val MARGE: String = "marge"
  val BART: String = "bart"
  val LISA: String = "lisa"
  val MAGGIE: String = "maggie"

  val SIMPSONS: Array[String] = Array(HOMER, MARGE, BART, LISA, MAGGIE)
}
