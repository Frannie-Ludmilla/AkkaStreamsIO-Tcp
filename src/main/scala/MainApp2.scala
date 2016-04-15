package akkaio

import java.io.File
import java.nio.file.StandardOpenOption._
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, IOResult}
import akka.util.ByteString
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.Future
import akka.NotUsed
import akka.Done




//import scala.util.{Failure, Success}

object MainApp2{

  /**
    * Use without parameters to start both client and
    * server.
    *
    * Use parameters `server 0.0.0.0 6001` to start server listening on port 6001.
    *
    * Use parameters `client 127.0.0.1 6001` to start client connecting to
    * server on 127.0.0.1:6001.
    *
    */
  def main(args: Array[String]): Unit = {

    val systemServer= ActorSystem("Server")
    val serverAddress= "127.0.0.1"
    val serverPort= 6001
    server(systemServer,serverAddress,serverPort)

    val systemClient= ActorSystem("Client")
    client(systemClient,serverAddress,serverPort)

  }

  def server(system: ActorSystem, address: String, port: Int): Unit = {
    implicit val sys = system
    import system.dispatcher
    implicit val materializer = ActorMaterializer()
    val output= new File("./src/main/resources/output.csv")

    import java.nio.file.StandardOpenOption._
    val saveFileFlow = Flow.fromSinkAndSourceMat(FileIO.toFile(output, options=Set(CREATE, WRITE)), Source.repeat(ByteString.empty))(Keep.left)
    //source lazyempty

    val handler = Sink.foreach[Tcp.IncomingConnection] { conn =>
      println("Client connected from: " + conn.remoteAddress)
      //conn.flow.toMat(FileIO.toFile(output))(Keep.right)
      conn.flow.joinMat(saveFileFlow)(Keep.right).run()
    }

    //Flow.fromSinkAndSourceMat(FileIO.toFile(storageFile, options=Set(CREATE, WRITE)), Source.repeat(ByteString.empty))(Keep.left)
    val connections = Tcp().bind(address, port)
    val binding = connections.to(handler).run()

    binding.onComplete {
      case Success(b) =>
        println("Server started, listening on: " + b.localAddress)
      case Failure(e) =>
        println(s"Server could not bind to $address:$port: ${e.getMessage}")
        system.terminate()
    }

  }


  //This server is an actor that listens on the address and port for incoming connections
  //Each flow coming from a different client will be stored in a file

  /*
  def server(system: ActorSystem, address: String, port: Int): Unit = {
    implicit val sys = system
    import system.dispatcher
    implicit val materializer = ActorMaterializer()
    implicit val mat = ActorFlowMaterializer()


    val delim = ByteString("\n")
    val dir= new File("./src/main/resources/")

    //1st step: binding a socket to that address:
    /**
      * +->(inbytes)-->+
      * |              |
      * [TCP]       [HandlerFlow]
      * |              |
      * +<-(outbytes)<-+
      *
      * */

    /**
      * def handleWith[Mat](handler: Flow[ByteString, ByteString, Mat])(implicit materializer: Materializer): Mat =
      * flow.joinMat(handler)(Keep.right).run()
      *
      *
      * */

    val connections: Source[Tcp.IncomingConnection, Future[Tcp.ServerBinding]] = Tcp().bind(address, port)
    val sink = FileIO.toFile(destination, true)
    connections.runForEach{
      connection =>
        println(s"New connection from: ${connection.remoteAddress}")
        val receiveSink = connection.flow.to(destToSink("./src/main/resources/result.txt"))

        receiveSink.runWith(Source.empty)
    }

    def destToSink(destination: File): Sink[(ByteString, File), Future[Long]] = {
      val sink = FileIO.toFile(destination, true)
      Flow[(ByteString, File)].map(_._1).toMat(sink)(Keep.right)
    }

    //val filesink= Sink.FileIO.toFile(new File("src/main/resources/logfile.txt"))
 /*
    val sink = Sink.foreach[Tcp.IncomingConnection]{ connection =>
      println(s"New connection from: ${connection.remoteAddress}")

      val receiveSource= connection.flow.map(_ ++ delim).to(SynchronousFileSink(new File(dir, "result.txt")))
    }



    val serverBinding: Future[Tcp.ServerBinding] = connections.to(sink).run()

    serverBinding.onComplete{
      case Success(b) =>
        println(s"Server started, listening on: ${b.localAddress}")
      case Failure(e) =>
        println(s"Server could not be bound to $address:$port: ${e.getMessage}")
    }

    */
  }*/


  def client(system: ActorSystem, address: String, port: Int): Unit = {
    implicit val sys = system
    import system.dispatcher
    implicit val materializer = ActorMaterializer()

    val inputFile= new File("src/main/resources/medium.csv")

    val testInput= FileIO.fromFile(inputFile)

    val result = testInput.via(Tcp().outgoingConnection(address, port)).to(Sink.ignore).run()


    result.onComplete{
      case Success(_) =>
        //println(s"Result: " + result.utf8String)
        println("Shutting down client")
        system.terminate()
      case Failure(e) =>
        println(s"Error in client: ${e.getMessage}")
    }
  }

}