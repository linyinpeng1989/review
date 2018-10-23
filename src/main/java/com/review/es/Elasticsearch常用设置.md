#### 常用设置
- config, data, logs，plugins目录：最好独立于安装目录之外
- 单播地址列表设置，用于集群发现与master选举
- 设置最少可用主节点数为“大多数”，防止脑裂问题
- 内存设置：-Xms与-Xmx最好相等，最好不超过物理机的50% RAM，最好不要超过32GB


#### 常用配置项
- cluster.name：集群名称，多个节点通过cluster.name组成集群，默认为elasticsearch
- node.name：节点名称
- path.data：数据存储路径
- path.logs：日志存储路径
- http.port：elasticsearch对外提供服务的http端口配置，默认情况下ES会取用9200~9299之间的端口，如果9200被占用就会自动使用9201，在单机多实例的配置中这个配置实际是不需要修改的。
- network.host：节点将绑定到一个主机名或者 ip 地址并且会将该节点通知集群中的其他节点（同一台服务器中的多个节点间）。
- node.max_local_storage_nodes：单机可以启动的节点个数，默认为1
- node.master: true，指定该节点是否有资格被选举成为node，默认是true
- node.data: true，指定该节点是否存储索引数据，默认为true
- discovery.zen.ping.unicast.hosts：主要负责集群中节点的自动发现和Master节点（Master节点维护集群的全局状态）选举。使用单播（unicast），即节点向指定的主机发送请求，其他节点接收到请求会做出响应。
- discovery.zen.minimum_master_nodes：配置当前集群中可用的最少主节点数，当可用主节点数小于该配置，则集群不可用。一般情况下，推荐大多数主节点可用即可，(master_eligible_nodes / 2) + 1（防止脑裂问题）。