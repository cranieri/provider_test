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
          case (id, amount) => p => pw.write(p.amount)
        }

        pw.close
      }
    }
  }

  val tickActor = system.actorOf(Props[TickActor], name="tick")

  system.scheduler.schedule(
    0 milliseconds,
    2050 milliseconds,
    tickActor,
    Tick)
}