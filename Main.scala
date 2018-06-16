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

case class Payment(id: Int, amount: Int, reference: String, status: String)

class Payments(tag: Tag) extends Table[(Int, Int, String, String)](tag, "payments") {
  def id = column[Int]("id",O.PrimaryKey, O.AutoInc)
  def amount = column[Int]("amount")
  def reference = column[String]("reference")
  def status = column[String]("status")
  def * = (id, amount, reference, status)
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

        val res = Await.result(db.run(payments.filter(_.status =!= "submitted").result), 10.seconds)

        if (res.length > 5) {
          val pw = new PrintWriter(new File(s"""payment_file${System.currentTimeMillis / 1000}.txt""" ),"UTF-8")

          res.foreach {
            case (id, amount, reference, status) => {
              val paymentToUpdate = for { p <- payments if p.id === id } yield p.status
              val updateAction = db.run(paymentToUpdate.update("submitted"))
              println(amount)
              pw.write(String.valueOf(s"""${reference}|${amount}\n"""))
            }
          }
          pw.close

        }

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
  implicit val personFormat = Json.format[PaymentMessage]
  implicit val recoveryStrategy = RecoveryStrategy.none

  case class PaymentMessage(reference: String, amount: Int)

  val subscriptionRef = Subscription.run(rabbitControl) {
    import Directives._

    val db = Database.forConfig("mysqlDB")
    // A qos of 3 will cause up to 3 concurrent messages to be processed at any given time.
    channel(qos = 3) {
      consume(topic(queue("such-message-queue"), List("some-topic.#"))) {
        (body(as[PaymentMessage]) & routingKey) { (payment, key) =>
          /* do work; this body is executed in a separate thread, as
             provided by the implicit execution context */
          println(s"""A payment with amount '${payment.amount}' was received over '${key}'.""")

          val payments = TableQuery[Payments]
          Await.result(db.run(DBIO.seq(payments += (0, payment.amount, payment.reference, "released"))), 10.seconds)

          ack
        }
      }
    }
  }
}