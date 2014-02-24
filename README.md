AppEvaluation
=============

Distributed REST Client to perform massive web site evaluations.
The Puppet Master controls a set of REST clients to perform requests on cloud deployed websites. Since cloud will scale, the number of REST clients must be scalable too.

Unlike siege, this project can evaluate a single HTTP Get request or perform a sequence of API requests.
The implementation calls functions in my AskScale (https://github.com/dnascimento/askScale), a basic open-source implementation of Stackoverflow for cloud deployments with NO-SQL backend.

*WebContainer:  [Grizzly](https://grizzly.java.net/)
*JAX-RS: [Jersey](https://jersey.java.net/) 
*Binary Communication Protocol [Google Protocol Buffers](https://developers.google.com/protocol-buffers/)

**Characteristics:**
*Slaves registration on master using REST
*Multi-thread HTTP client in each slave
*Statistic analyse per node.
*The master aggregates each node satistic

