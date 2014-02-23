AppEvaluation
=============

Distributed REST Client to perform massive web site evaluations.
A Puppet Master controls a set of REST clients to perform requests on cloud deployed websites. Since cloud will scale, the number of REST clients must be scalable too.

Unlike siege, this project can evaluate a single HTTP Get request or perform a sequence of API requests.
The API is my AskScale (https://github.com/dnascimento/askScale), a basic open-source implementation of Stackoverflow for cloud deployments with NO-SQL backend.
