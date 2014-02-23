Objective: Measure the delay, throughput of WebServer performing a sequence of defined requests or getting the same page

DONE:
	- Same request

TODO:
	- Create easy request for Post,Put,Delete,Get
	- Create action per type of interface:
		- Create/Delete/Modify Question
		- Create/Delete/Modify Answer
		- Create/Delete/Modify Comment
	- Create request queue for execution per thread
	- Store anwsers and requests to compare
	- Compare
	- Statistics records
	- Graphics for matlab (file)
	- Create a pupet master and slaves to deploy a distributed evaluation system

OPTIONS FOR REST ON JAVA:
jersey - JAX-RS production quality API
grizzly - The container

http://www.vogella.com/tutorials/REST/article.html
https://grizzly.java.net/
http://www.javarants.com/2008/12/27/using-jax-rs-with-protocol-buffers-for-high-performance-rest-apis/



Generate protocol buffer:
protoc -I=src --java_out=src src/addressbook.proto 