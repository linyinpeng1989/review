#### 1、Elasticsearch原理

- Elasticsearch集群是由一个或者多个拥有相同cluster.name配置的节点组成，它们共同承担数据和负载的压力。

- 当有节点加入集群中或者从集群中移除节点时，集群将会重新平均分布所有的数据（分片）。

- 索引实际上是一个或多个物理分片的逻辑命名空间，用于保存相关的数据。而分片是底层的工作单元，仅保存全部数据中的一部分，其本身是一个Lucene的实例。

- 副本分片只是主分片的拷贝，作为硬件故障时保护数据不丢失的冗余备份，并为搜索和返回文档等读操作提供服务。（负载）

- 主分片数在索引建立的时候确定，用于数据平均分布时的取模操作，不能修改；副本分片数可以随时修改。
```
shard = hash(routing) % number_of_primary_shards

routing 是一个可变值，默认是文档的 _id ，也可以设置成一个自定义的值。 routing 通过 hash 函数生成一个数字，然后这个数字再除以 number_of_primary_shards （主分片的数量）后得到 余数 。
```

- 主分片不能和它的副本分片存在于同一节点上。

- 用户可以将请求发送到集群中的任何节点（包括主节点），每个节 点都知道任意文档所处的位置，并且能够将我们的请求直接转发到存储我们所需文档的节点。

- 每个文档都有一个版本号_version，每次对文档进行修改或删除时递增，用于乐观并发控制（处理冲突）。在请求中指定version，若与服务器版本一致才更新，可以确保应用中相互冲突的变更不会导致数据丢失
（如果旧版本的文档在新版本之后到达，可以简单地进行忽略）

- 新建、修改和删除（索引、文档）均是写操作，必须在主分片上面完成之后才能被复制到相关的副本分片。

- 在处理读取请求时，协调结点在每次请求的时候都会通过轮询所有副本分片来达到负载均衡。

#### 2、Elasticsearch水平扩容
- 在创建索引时，主分片的数目就已经确定下来了。实际上，这个数目定义了这个索引能够存储的最大数据量。但是读操作（搜索和返回数据）可以同时被主分片或副本分片所处理，所以当你拥有越多的副本分片时，
也将拥有越高的吞吐量。
- 当然，如果只是在相同节点数目的集群上增加更多的副本分片并不能提高性能，因为每个分片从节点上获得的资源会变少。因此需要增加更多的硬件资源来提升吞吐量。

#### 3、搜索知识点
- 倒排索引由文档中所有不重复词的列表构成（分析文档全文域_all），它会保存每一个词项出现过的文档总数， 在对应的文档中一个具体词项出现的总次数，词项在文档中的顺序，每个文档的长度，所有文档的平均
长度等等。

- 倒排索引被写入磁盘后是不可改变的，使用更多的索引实现倒排索引的更新（更新操作即重新索引整个文档）。采用按段搜索（每一段本身都是一个倒排索引），每一个倒排索引都会被轮流查询到--从最早的开始--查询完
后再对结果进行合并。

- 分析包含两个过程：
    - 首先，将一块文本分成适合于倒排索引的独立的词条。
    - 之后，将这些词条统一化为标准格式以提高它们的“可搜索性”。

- 分析器执行分析过程，实际上包含三个功能：
    - 字符过滤器：首先，字符串按顺序通过每个 字符过滤器 ，它们的任务是在分词前整理字符串。比如html清除字符过滤器可以用来去掉HTML，或者将 & 转化成 `and`。
    ```
    一个分析器可能有0个或者多个字符过滤器。如html清除字符过滤器等。
    ```
    - 分词器：其次，字符串被 分词器 分为单个的词条。一个简单的分词器遇到空格和标点的时候，可能会将文本拆分成词条。
    ```
    一个分析器 必须 有一个唯一的分词器。常用的有：标准分词器、关键词分词器、空格分词器、正则分词器等。
    ```
    - Token过滤器(词过滤器)：最后，词条按顺序通过每个 token 过滤器 。这个过程可能会改变词条（例如，小写化 Quick ），删除词条（例如， 像 a`， `and`， `the 等无用词），或者增加词条（例如，像 jump
    和 leap 这种同义词）。
    ```
    一个分析器可以包含多个词过滤器，比如lowercase词过滤器、stop词过滤器、词干过滤器、ngram 词过滤器等。
    ```

- Elasticsearch内置分析器（以文本 “Set the shape to semi-transparent by calling set_trans(5)” 为例）
    - 标准分析器：标准分析器是Elasticsearch默认使用的分析器。它是分析各种语言文本最常用的选择。它根据 Unicode 联盟 定义的 单词边界 划分文本。删除绝大部分标点。最后，将词条小写。
    ```
    set, the, shape, to, semi, transparent, by, calling, set_trans, 5
    ```
    - 简单分析器：简单分析器在任何不是字母的地方分隔文本，将词条小写。
    ```
    set, the, shape, to, semi, transparent, by, calling, set, trans
    ```
    - 空格分析器：空格分析器在空格的地方划分文本（不会将词条小写）。
    ```
    Set, the, shape, to, semi-transparent, by, calling, set_trans(5)
    ```
    - 语言分析器：特定语言分析器可用于 很多语言。它们可以考虑指定语言的特点。例如， 英语 分析器附带了一组英语无用词（常用单词，例如 and 或者 the ，它们对相关性没有多少影响），它们会被删除。
    由于理解英语语法的规则，这个分词器可以提取英语单词的 词干 。
    ```
    set, shape, semi, transpar, call, set_tran, 5
    ```
- 在全文域搜索的时候，需要将查询字符串通过相同的分析过程，以保证搜索的词条格式与索引中的词条格式一致。

- 映射：包含Elasticsearch 文档的每个域的数据类型及其索引类型、分析器。

- 查询与过滤：
    - 当使用过滤情况时，查询被设置成一个“不评分”或者“过滤”查询，只需关心是否匹配。性能较好。
    - 当使用查询情况时，查询被设置成一个“评分”查询，不仅需要关心是否匹配，还要判断文档的匹配程度（相关度）。性能比过滤情况稍低。

- 几个比较重要的查询
    - match_all查询：简单地匹配所有文档。在没有指定查询方式时，默认为match_all。boost用于指定权重，即_score *= boost。
    ```
      GET /bank/_doc/_search

    GET /bank/_doc/_search
    {
        "query": {
          "match_all": {
            "boost": 2
          }
        }
    }
    ```
    - match查询：无论你在任何字段上进行的是全文搜索还是精确查询，match 查询是你可用的标准查询。
        - 如果查询 日期（date） 或 整数（integer） 字段，它们会将查询字符串分别作为日期或整数对待。
        - 如果查询一个（ not_analyzed ）未分析的精确值字符串字段， 它们会将整个查询字符串作为单个词项对待。
        - 但如果要查询一个（ analyzed ）已分析的全文字段， 它们会先将查询字符串传递到一个合适的分析器，然后生成一个供查询的词项列表。一旦组成了词项列表，这个查询会对每个词项逐一执行底层（如term等）
        的查询，再将结果合并，然后为每个文档生成一个最终的相关度评分。
    ```
    <!-- 1、检查字段类型 2、若不是精确查询，则分析查询字符串，否则直接第3步 3、查找匹配文档 4、为每个文档评分-->
    GET /bank/_doc/_search
    {
    	"query": {
    		"match": {
    			"lastname": "Yinpeng"
    		}
    	}
    }

    <!-- 提升查询精度 -->
    GET /bank/_doc/_search
    {
      "query": {
        "match": {
          "lastname": {
            "query": "quick brown dog",
            "operator": "and"
          }
        }
      }
    }


    GET /bank/_doc/_search
    {
      "query": {
        "match": {
          "lastname": {
            "query": "quick brown dog",
            "minimum_should_match": "75%"
          }
        }
      }
    }
    ```
    - match_phrase查询：即短语查询，即将查询条件作为一个整体（不进行分析）进行查询。
    ```
     GET /bank/_doc/_search
     {
     	"query": {
     		"match_phrase": {
     			"lastname": "yinpeng"
     		}
     	}
     }
    ```
    - multi_match查询：multi_match 查询可以在多个字段上执行相同的 match 查询
    ```
    GET /bank/_doc/_search
    {
    	"query": {
    		"multi_match": {
    			"query": "Mirrer Lin Yinpeng",
    			"fields": ["firstname", "lastname"]
    		}
    	}
    }

    <!-- 使用best_fields表示最佳匹配评分作为最终评分，其他匹配字段评分不计算，dis_max查询的简化方式。若包含tie_breaker属性，则其他匹配字段评分乘以tie_breaker，并加上最佳匹配评分作为最终评分。
        另外还有most_fields（在多个字段上匹配同一个内容，且最终评分由全部匹配的评分求和）、cross_fields。
     -->
    GET /bank/_doc/_search
    {
    	"query": {
    		"multi_match": {
    			"query": "Mirrer Lin Yinpeng",
    			"type": "best_fields",
    			"fields": ["firstname", "lastname"],
    			"tie_breaker": 0.3,
    			"minimum_should_match": 50%
    		}
    	}
    }

    <!-- 可以使用模糊匹配，address的boost为2（提升单个字段权重） -->
    GET /bank/_doc/_search
    {
    	"query": {
    		"multi_match": {
    			"query": "Miller Lin Yinpeng",
    			"type": "best_fields",
    			"fields": ["*name", "address^2"]
    		}
    	}
    }
    ```
    - range查询：range 查询找出那些落在指定区间内的数字或者时间。
    ```
    GET /bank/_doc/_search
    {
    	"query": {
    		"range": {
    			"age": {
    				"gte": 20,
    				"lt": 30
    			}
    		}
    	}
    }
    ```
    - term查询（包含操作，而非等值操作）：term 查询被用于精确值 匹配，这些精确值可能是数字、时间、布尔或者那些 not_analyzed 的字符串。term 查询对于输入的文本不进行分析，所以它将给定的值进行
    精确查询。
    ```
    GET /bank/_doc/_search
    {
    	"query": {
    		"term": {
    			"age": "28"
    		}
    	}
    }

    <!-- 一般会将精确查询包装成过滤查询，不进行相关度计算或为常量，可以有效地利用缓存，提高查询效率 -->
    GET /bank/_doc/_search
    {
    	"query": {
    		"constant_score": {
    			"filter": {
    				"term": {
    					"lastname": "yinpeng"
    				}
    			}
    		}
    	}
    }
    ```
    - terms查询（包含操作，而非等值操作）：terms 查询和 term 查询一样，但它允许你指定多值进行匹配。如果这个字段包含了指定值中的任何一个值，那么这个文档满足条件。
    ```
    GET /bank/_doc/_search
    {
    	"query": {
    		"terms": {
    			"age": [28, 30, 40]
    		}
    	}
    }
    ```
    - exists查询：exists 查询被用于查找那些指定字段中有值 (exists)的文档。
    ```
    GET /bank/_doc/_search
    {
    	"query": {
    		"exists": {
    			"field": "firstname"
    		}
    	}
    }
    ```

- 组合多查询：使用bool查询将多查询组合在一起，构建复杂查询。bool可以接收以下参数，用于多查询组合条件。
    - must：文档 必须 匹配这些条件才能被包含进来（等价于and）。
    - must_not：文档 必须不 匹配这些条件才能被包含进来（等价于not）。
    - should：如果满足这些语句中的任意语句（等价于or），将增加 _score ，否则，无任何影响。它们主要用于修正每个文档的相关性得分。当没有must语句时，should语句至少需要匹配一个。
    - filter：必须 匹配，但它以不评分、过滤模式来进行。这些语句对评分没有贡献，只是根据过滤标准来排除或包含文档。
    ```
    <!-- firstname必须包含Miller， lastname必须不是Mick，address包含Miller及年龄在20-30之间评分更高（相关性），balance必须大于10000，且balance不影响评分 -->
    GET /bank/_doc/_search
    {
    	"query": {
    		"bool": {
    			"must": {"match": {"firstname": "Miller"}},
    			"must_not": {"match": {"lastname": "Mick"}},
    			"should": [
    				{"match": {"address": "Miller"}},
    				{"range": {"age": {"gte": 20, "lt": 30}}}
    			],
    			"filter": {"range": {"balance": {"gt": 10000}}}
    		}
    	}
    }
    ```

- explain关键字可以帮助定位查询语句具体的错误信息，或者查询语句具体的分词结果等。
```
<!-- 查询索引中字段的解析情况（分析器确定，即字段的分析器） -->
GET /bank/_validate/query?explain
{
	"query": {
		"match": {
			"lastname": "Yinpeng"
		}
	}
}

<!-- 用指定分析器解析特定字段结果 -->
GET /_analyze
{
  "analyzer": "ik_max_word",
  "text": "中华人名共和国国歌"
}
```

- 排序
    - 默认情况下，Elasticsearch 中使用相关性得分（_score字段）降序排序
    - 按照字段的值排序
    ```
    GET /bank/_doc/_search
    {
    	"query": {
    		"range": {
    			"balance": {
    				"gte": 10000, "lte": 30000
    			}
    		}
    	},
    	"sort": {
    		"balance": "desc", "_score": "desc"
    	},
    	"from": 0,
    	"size": 10
    }
    ```
    - 字符串排序与多字段：以全文analyzed字段（方便搜索）排序会消耗大量的内存，一种简单的方法是用两种方式对同一个字符串进行索引，analyzed用于搜索、not_analyzed用于排序。
    - 多值字段排序：对于数字或者日期等，通过min、max、avg或者sum将多值字段减为单值进行排序。

- 相关性评分：每个文档都有相关性评分，用一个正浮点数字段 _score 来表示 。 _score 的评分越高，相关性越高。Elasticsearch 的相似度算法 被定义为检索词频率/反向文档频率， TF/IDF ，包括以下内容：
    - 检索词频率：检索词在该字段出现的频率？出现频率越高，相关性也越高。字段中出现过 5 次要比只出现过 1 次的相关性高。
    - 反向文档频率：每个检索词在索引中出现的频率？频率越高，相关性越低。检索词出现在多数文档中会比出现在少数文档中的权重更低。
    - 字段长度准则：字段的长度是多少？长度越长，相关性越低。 检索词出现在一个短的 title 要比同样的词出现在一个长的 content 字段权重更大。
    ```
     GET /bank/_doc/_search?explain=true
     {
     	"query": {
     		"bool": {
     			"must": {"match": {"firstname": "Miller"}},
     			"must_not": {"match": {"lastname": "Mick"}},
     			"should": [
     				{"match": {"address": "Miller"}},
     				{"range": {"age": {"gte": 20, "lt": 30}}}
     			],
     			"filter": {"range": {"balance": {"gt": 10000}}}
     		}
     	}
     }
    ```

- Doc Values：倒排索引的检索性能非常快，但是在字段值排序时却不是理想的结构。如果想要实现高效地排序，需要转置倒排索引（列存储），即将所有单字段的值存储在单数据列中。在 Elasticsearch 中，
Doc Values 就是一种列式存储结构，默认情况下每个字段的 Doc Values 都是激活的，Doc Values 是在索引时创建的，当字段索引时，Elasticsearch 为了能够快速检索，会把字段的值加入倒排索引中，同时它
也会存储该字段的 `Doc Values`。

- 在 search 接口返回一个 page 结果之前，多分片中的结果必须组合成单个排序列表。为此，搜索被执行成一个两阶段过程，我们称之为 query then fetch。
    - 查询阶段：协调节点（接收请求的节点）会将查询广播到索引中每一个分片拷贝（主分片或者副本分片）。 每个分片在本地执行搜索并构建一个匹配文档的 优先队列（一个存有 top-n 匹配文档的有序列表）。
    每个分片返回各自优先队列中所有文档的 ID 和排序值给协调节点，协调节点将这些分片级的结果合并到自己的有序优先队列里，它代表了全局排序结果集合。
    - 取回阶段：协调节点辨别出哪些文档需要被取回并向相关的分片提交多个GET请求（multi-get request）；每个分片加载并丰富文档，接着返回文档给协调节点；一旦所有的文档都被取回了，协调节点返回结果给客户端。

- 游标查询scroll：scroll查询可以有效地执行大批量的文档查询，而又不用付出深度分页的代价。它允许我们先做查询初始化（取某个时间点的快照数据），查询初始化之后索引上的任何变化都会被忽略，然后再批量地
拉取结果。游标查询用字段_doc（Doc Values）进行排序，比深度分页的结果集全局排序代价小很多。

- Lucene 没有文档类型的概念，每个文档的类型名被存储在一个叫 _type 的元数据字段上。 当我们要检索某个类型的文档时, Elasticsearch 通过在 _type 字段上使用过滤器限制只返回这个类型的文档。也因为如此，一个
索引中的所有类型最终都共享相同的映射，

- 使用索引别名以及数据重新索引机制，实现零停机数据迁移（创建新索引、数据迁移、原子地删除别名与旧索引关系并创建别名与新索引关系）

- 内部过滤器的操作：创建匹配文档、创建bitset（一个包含0和1的数组，描述哪些文档与过滤器匹配，格式类似于[1,0,0,0]）、迭代bitsets找到满足所有过滤条件的匹配文档的集合、增量使用计数（若查询在最近
256次查询中会被用到，则它会被缓存到内存中）

- 查询语句提升权重
    - 使用bool + should，但无法区分should语句之间权重值，即无法区分firstname与address之间权重谁高谁低
    ```
    GET /bank/_doc/_search
    {
    	"query": {
    		"bool": {
    			"must": {
    				"match": {"lastname": "Yinpeng"}
    			},
    			"should": [
    				{"match": {"firstname": "miller"}},
    				{"match": {"address": "miller"}}
    			]
    		}
    	}
    }
    ```
    - 使用bool + should，并通过指定 boost 来控制任何查询语句的相对的权重， boost 的默认值为 1 ，大于 1 会提升一个语句的相对权重
    ```
    GET /bank/_doc/_search
    {
    	"query": {
    		"bool": {
    			"must": {
    				"match": {"lastname": "Yinpeng"}
    			},
    			"should": [
    				{"match": {"firstname": {"query": "miller", "boost": 3}}},
    				{"match": {"address": {"query": "miller", "boost": 2}}}
    			]
    		}
    	}
    }
    ```

- dis_max查询只会简单地使用单个最佳匹配语句的评分_score作为整体评分，不考虑其他匹配项的评分。通过指定 tie_breaker 这个参数将其他匹配语句的评分也考虑其中（将其他匹配语句的评分结果与 tie_breaker
相乘后求和并规范化）。

