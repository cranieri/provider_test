package com.lightbend.akka.sample

import akka.Done
import scala.util.{Success, Failure}
import scala.concurrent.Future
import scala.concurrent.Await
import akka.actor.ActorSystem
import slick.jdbc.MySQLProfile.api._
import slick.basic._
import scala.concurrent.duration._
import akka.actor.Actor
import akka.actor.Props
import java.io._

case class Payment(id: Int, amount: Int)

class Payments(tag: Tag) extends Table[(Int, Int)](tag, "payments") {
  def id = column[Int]("id")
  def amount = column[Int]("amount")
  def * = (id, amount)
}

object Main extends App {
  implicit val system = ActorSystem("QuickStart")
  implicit val ec = system.dispatcher

  val Tick = "tick"
  class TickActor extends Actor {
    def receive = {
      case Tick => {
        val db = Database.forConfig("mysqlDB")

        val payments = TableQuery[Payments]

        val pw = new PrintWriter(new File("hello.txt" ))

        Await.result(db.run(payments.result), 10.seconds).foreach {
          case (id, amount) => println(amount)
        }

        pw.close
      }
    }
  }

  val tickActor = system.actorOf(Props[TickActor], name="tick")

  system.scheduler.schedule(
    0 milliseconds,
    5050 milliseconds,
    tickActor,
    Tick)
}

import com.spingo.op_rabbit.PlayJsonSupport._
import com.spingo.op_rabbit._
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global
case class Person(name: String, age: Int)

object RabbitmqConsumer extends App {
  implicit val actorSystem = ActorSystem("such-system")
  val rabbitControl = actorSystem.actorOf(Props[RabbitControl])

  // setup play-json serializer
  implicit val personFormat = Json.format[Payment]
  implicit val recoveryStrategy = RecoveryStrategy.none

  val subscriptionRef = Subscription.run(rabbitControl) {
    import Directives._
    // A qos of 3 will cause up to 3 concurrent messages to be processed at any given time.
    channel(qos = 3) {
      consume(topic(queue("such-message-queue"), List("some-topic.#"))) {
        (body(as[Payment]) & routingKey) { (payment, key) =>
          /* do work; this body is executed in a separate thread, as
             provided by the implicit execution context */
          println(s"""A payment with amount '${payment.amount}' was received over '${key}'.""")
          ack
        }
      }
    }
  }
}