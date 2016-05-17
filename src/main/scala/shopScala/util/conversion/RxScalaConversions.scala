package shopScala.util.conversion

import java.util.concurrent.atomic.AtomicBoolean

import org.mongodb.scala.{Observable, Observer, Subscription}
import rx.lang.{scala => rx}


object RxScalaConversions {

  implicit def observableToRxObservable[T](observable: Observable[T]): rx.Observable[T] =
    rx.Observable[T]((subscriber: rx.Subscriber[_ >: T]) => ObservableToProducer[T](observable, subscriber).subscribe())

  private case class ObservableToProducer[T](observable: Observable[T], rxSubscriber: rx.Subscriber[_ >: T]) extends rx.Producer {
    @volatile
    private var subscription: Option[Subscription] = None

    def subscribe() = {
      observable.subscribe(new Observer[T]() {
        override def onSubscribe(s: Subscription) {
          subscription = Some(s)
          rxSubscriber.add(new rx.Subscription() {
            private final val unsubscribed: AtomicBoolean = new AtomicBoolean

            override def unsubscribe() = {
              if (!unsubscribed.getAndSet(true)) {
                subscription match {
                  case Some(sub) => sub.unsubscribe()
                  case None =>
                }
              }
            }

            override def isUnsubscribed: Boolean = subscription match {
              case Some(sub) => sub.isUnsubscribed
              case None => true
            }
          })
        }

        override def onNext(tResult: T) {
          if (isSubscribed) {
            rxSubscriber.onNext(tResult)
          }
        }

        override def onError(t: Throwable) {
          if (isSubscribed) {
            rxSubscriber.onError(t)
          }
        }

        def onComplete() {
          if (isSubscribed) {
            rxSubscriber.onCompleted()
          }
        }
      })
      rxSubscriber.setProducer(this)
      rxSubscriber.onStart()
      this
    }

    override def request(n: Long) {
      if (isSubscribed) {
        subscription.get.request(n)
      }
    }

    private def isSubscribed: Boolean = !rxSubscriber.isUnsubscribed
  }
}
