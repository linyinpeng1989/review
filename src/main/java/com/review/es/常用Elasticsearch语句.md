- 设置索引属性（会自动创建索引）
```
PUT /house
{
    "settings": {
        "number_of_shards" : 3,
        "number_of_replicas" : 1
    }
}
```

- 指定映射（索引不存在时报错）
```
PUT /house/_mappings/villa
{
    "properties": {
        "address1": {
            "type": "text",
            "analyzer": "ik_smart"
        },
        "address2": {
            "type": "text",
            "analyzer": "ik_max_word"
        },
        "location": {
            "type": "geo_point"
        },
        "house_name": {
            "type": "text",
            "analyzer": "standard"
        },
        "city": {
            "type": "keyword"
        },
        "floor": {
            "type": "long"
        }
    }
}
```

- 创建索引、设置属性、指定映射
```
PUT /house
{
    "settings": {
        "number_of_shards" : 3,
        "number_of_replicas" : 1
    },
    "mappings": {
        "villa": {
            "properties": {
                "address1": {
                    "type": "text",
                    "analyzer": "ik_smart"
                },
                "address2": {
                    "type": "text",
                    "analyzer": "ik_max_word"
                },
                "location": {
                    "type": "geo_point"
                },
                "house_name": {
                    "type": "text",
                    "analyzer": "standard"
                },
                "city": {
                    "type": "keyword"
                },
                "floor": {
                    "type": "long"
                }
            }
        }
    }
}
```


- 分页及排序（默认情况下使用相关性得分_score降序排序）
```
GET /bank/_doc/_search
{
  "query": {
    "range": {
      "balance": {
        "gte": 10000,
        "lte": 30000
      }
    }
  },
  "sort": {
    "balance": "desc",
    "_score": "desc"
  },
  "from": 0,
  "size": 10
}
```

- 组合多查询
```
<!-- firstname必须包含Miller， lastname必须不是Mick，address包含Miller及年龄在20-30之间评分更高（相关性），balance必须大于10000，且balance不影响评分 -->
GET /bank/_doc/_search
{
  "query": {
    "bool": {
      "must": {
        "match": {
          "firstname": "Miller"
        }
      },
      "must_not": {
        "match": {
          "lastname": "Mick"
        }
      },
      "should": [
        {
          "match": {
            "address": "Miller"
          }
        },
        {
          "range": {
            "age": {
              "gte": 20,
              "lt": 30
            }
          }
        }
      ],
      "filter": {
        "range": {
          "balance": {
            "gt": 10000
          }
        }
      }
    }
  }
}

<!-- 与上面一直，地址权重增加 -->
GET /bank/_doc/_search
{
  "query": {
    "bool": {
      "must": {
        "match": {
          "firstname": "Miller"
        }
      },
      "must_not": {
        "match": {
          "lastname": "Mick"
        }
      },
      "should": [
        {
          "match": {
            "address": {
              "query": "Miller",
              "boost": 2
            }
          }
        },
        {
          "range": {
            "age": {
              "gte": 20,
              "lt": 30
            }
          }
        }
      ],
      "filter": {
        "range": {
          "balance": {
            "gt": 10000
          }
        }
      }
    }
  }
}
```


- 查看相关性评分明细
```
 GET /bank/_doc/_search?explain=true
{
  "query": {
    "bool": {
      "must": {
        "match": {
          "firstname": "Miller"
        }
      },
      "must_not": {
        "match": {
          "lastname": "Mick"
        }
      },
      "should": [
        {
          "match": {
            "address": "Miller"
          }
        },
        {
          "range": {
            "age": {
              "gte": 20,
              "lt": 30
            }
          }
        }
      ],
      "filter": {
        "range": {
          "balance": {
            "gt": 10000
          }
        }
      }
    }
  }
}
```

- 解析查询语句解析情况
```
GET /bank/_validate/query?explain
{
  "query": {
    "bool": {
      "must": {
        "match": {
          "firstname": "Miller"
        }
      },
      "must_not": {
        "match": {
          "lastname": "Mick"
        }
      },
      "should": [
        {
          "match": {
            "address": "Miller"
          }
        },
        {
          "range": {
            "age": {
              "gte": 20,
              "lt": 30
            }
          }
        }
      ],
      "filter": {
        "range": {
          "balance": {
            "gt": 10000
          }
        }
      }
    }
  }
}
```

- 使用特定分词器分析字段
```
GET /_analyze
{
  "analyzer": "ik_max_word",
  "text": "中华人民共和国国歌"
}
```
