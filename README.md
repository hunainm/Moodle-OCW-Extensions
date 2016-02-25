# Moodle-OCW-Extensions

An open source solution to create Semantic Overlay Networks for Open Coursewares. A complete description  is available [here.](description.txt)

# Installation steps
* [Install moodle](/moodle)
* [Setup python scripts](/scripts)
* [Compile SONNode](/src) / [Use pre-built binary](/build)
* Copy all files from [scripts](/scripts) directory to application path
* Start first node
```shell
    java -jar SONNode.jar 127.0.0.1 [PORT]
```
* Join the recently created network via other nodes
```shell
    java -jar SONNode.jar 127.0.0.1 [PORT] [NETWORK_NODE_IP] [NETWORK_NODE_PORT]
```
