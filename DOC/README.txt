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

http://www.javarants.com/2008/12/27/using-jax-rs-with-protocol-buffers-for-high-performance-rest-apis/


3 Components:
Pupet - Invokes the rest API
restAPI - Retrieves the requests
client - execute the requests

Messages between Pupet and Slaves:
	List Requests:
		Request:
			Type,URL,Parameters,number of executions (Nexec)
	
	List Response:
		Response:
			ClientID, Content, Delay
	
	StartRequesting
	
	InitClient
		ClientID
	


