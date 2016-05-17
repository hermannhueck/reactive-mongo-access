package shopScala.util

object Util {

  def checkUserLoggedIn(optUser: Option[User], credentials: Credentials): User = {
    // print("checkUserLoggedIn for user: ")
    optUser match {
      case None =>
        //println("--- NOT FOUND ---")
        throw new IllegalAccessException("User unknown: " + credentials.username)
      case Some(user) =>
        //println(user.toString)
        checkCredentials(user, credentials)
    }
  }

  def checkCredentials(user: User, credentials: Credentials): User = {
    if (user.name != credentials.username)
      throw new IllegalAccessException("Incorrect username: " + credentials.username)
    else if (user.password != credentials.password)
      throw new IllegalAccessException("Bad password supplied for user: " + credentials.username)
    else
      user
  }
}
