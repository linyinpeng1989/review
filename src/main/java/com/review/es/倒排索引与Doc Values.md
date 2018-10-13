## 倒排索引
倒排索引将词项映射到包含它们的文档，即可以很方便地查找包含某个词项的文档。

|Term    |  Doc_1| Doc_2|   Doc_3|
|-------|-------|------|------|
|brown   |   X   |   X   |      |
|dog     |   X   |       |   X  |
|dogs    |       |   X   |   X  |
|fox     |   X   |       |   X  |
|foxes   |       |   X   |      |
|in      |       |   X   |      |
|jumped  |   X   |       |   X  |
|lazy    |   X   |   X   |      |
|leap    |       |   X   |      |
|over    |   X   |   X   |   X  |
|quick   |   X   |   X   |   X  |
|summer  |       |   X   |      |
|the     |   X   |       |   X  |


## Doc Values
Doc Values通过转置词项与文档的关系，将文档映射到它们包含的词项，常用于聚合、排序、脚本、父子关系处理等。

Doc Values 是在索引时与 倒排索引同时生成。也就是说 Doc Values 和 倒排索引 一样，基于 Segement 生成并且是不可变的，而且 Doc Values 和 倒排索引一样序列化到磁盘。

Doc Values 默认对所有字段启用，除了 analyzed strings。也就是说所有的数字、地理坐标、日期、IP 和不分析（not_analyzed）字符类型都会默认开启。
因此，类型为text的字段如果需要进行聚合，需要设置fielddata为true（与 doc values 不同，fielddata 构建和管理 100% 在内存中，常驻于 JVM 内存堆）。

|Doc    |  Terms|
|-------|------------------------------------------------|
|Doc_1 | brown, dog, fox, jumped, lazy, over, quick, the        |
|Doc_2 | brown, dogs, foxes, in, lazy, leap, over, quick, summer|
|Doc_3 | dog, dogs, fox, jumped, over, quick, the               |
