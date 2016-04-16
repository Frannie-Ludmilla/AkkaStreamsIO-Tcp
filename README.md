# AkkaStreamsIO-Tcp

This program just implements a simple File transfer from clients to a server.

It uses `Akka Streams` (2.4.3) via a Tcp connection.


The client creates its own `Source` via *FileIO.fromFile* while  the server redirects its Tcp.incomingConnection to a `Sink ` to save the file (*FileIO.toFile*) and sends back to the client the number of the transferred ByteStrings.
