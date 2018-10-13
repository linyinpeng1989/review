- ElasticSearch中text类型字段fielddata默认是false的，需要手动打开
```
POST /cars/_mapping/transactions
{
    "properties": {
        "color": {
            "fielddata": true,
            "type": "text"
        },
        "make": {
             "fielddata": true,
             "type": "text"
        }
    }
}
```

- 统计每种颜色的汽车销量（嵌套）
```
<!--
    1、为每个颜色分配一个桶并统计每个桶中的文档数量
    2、第一个size表示不返回任何记录，第二个size表示获取几条聚合结果
    3、可以使用查询或过滤进行范围限定
-->
<!-- 使用查询限定范围 -->
GET /cars/transactions/_search
{
  "size": 0,
  "query": {
    "match": {
        "make": "ford"
    }
  },
  "aggs": {
    "popular_color": {
      "terms": {
        "field": "color",
        "size": 5
      }
    }
  }
}

<!-- 使用过滤限定范围 -->
GET /cars/transactions/_search
{
  "size": 0,
  "query": {
    "constant_score": {
      "filter": {
        "match": {
          "make": "ford"
        }
      }
    }
  },
  "aggs": {
    "popular_color": {
      "terms": {
        "field": "color",
        "size": 5
      }
    }
  }
}
```

- 统计每种颜色的汽车销量、每种颜色的均价、每种颜色的制造商分布、每种颜色制造商的最高价和最低价（多层嵌套）
```
<!--
    1、为每个颜色分配一个桶（多值聚合指标）并统计每个桶中的文档数量，并根据步骤2的avg_price倒序排序（单桶排序）
    2、在步骤1基础上，每个桶再分配一个桶（单值聚合指标，即结果只有一个），用于统计均价
    3、在步骤1基础上，每个桶再为每个make分配一个桶（多值聚合指标），并统计每个桶的文档数量
    4、在步骤3基础上，每个桶再为最低价格和最高价格分别分配一个桶（单值聚合指标），分别统计最低价格和最高价格
-->
GET /cars/transactions/_search
{
  "size": 0,
  "aggs": {
    "popular_color": {
      "terms": {
        "field": "color",
        "order": {
            "avg_price": "desc"
        }
      },
      "aggs": {
        "avg_price": {
          "avg": {
            "field": "price"
          }
        },
        "car_make": {
          "terms": {
            "field": "make"
          },
          "aggs": {
            "max_price": {
              "max": {
                "field": "price"
              }
            },
            "min_price": {
              "min": {
                "field": "price"
              }
            }
          }
        }
      }
    }
  }
}
```

> 上述查询聚合中使用单桶排序，除了单桶排序之外，还可以基于多桶排序，主要分为以下几种：
> 1、内置排序：
>>  _count：按文档数排序。对 terms 、 histogram 、 date_histogram 有效。
>>  _term：按词项的字符串值的字母顺序排序。只在 terms 内使用。
>>  _key：按每个桶的键值数值排序（理论上与 _term 类似）。 只在 histogram 和 date_histogram 内使用。

> 2、按度量排序：在桶中内嵌一个单桶聚合，并使用单桶聚合结果进行排序，就像上述查询聚合中的"order": {"avg_price": "desc"}
> 3、基于“深度”度量排序：使用孙子桶或从孙桶字段进行排序，如"order": {"red_green_cars>stats.variance" : "asc"}，其中red_green_cars为直接子桶，stats为孙子桶，variance为孙子桶的属性。

- 条形图聚合查询
```
<!-- 按数值统计，直方图histogram需要指定字段（数值类型）及大小间隔，0-19999,20000-39999, ... -->
GET /cars/transactions/_search
{
  "size": 0,
  "aggs": {
    "price_show": {
      "histogram": {
        "field": "price",
        "interval": 20000
      },
      "aggs": {
        "sum_price": {
          "sum": {
            "field": "price"
          }
        }
      }
    }
  }
}

<!--
    1、按时间统计，date_histogram需要指定字段（日期类型）、间隔（minuter、day、month、quarter、year等）、日期格式可选（用于key_as_string格式化）
    2、统计每个季度汽车销售总额以及各品牌销售总额
-->
GET /cars/transactions/_search
{
  "size": 0,
  "aggs": {
    "group_by_quarter": {
      "date_histogram": {
        "field": "sold",
        "interval": "quarter",
        "format": "yyyy-MM-dd"
      },
      "aggs": {
      	"total_sum": {
      		"sum": {
      			"field": "price"
      		}
      	},
        "group_by_make": {
          "terms": {
            "field": "make"
          },
          "aggs": {
            "quarter_make_sum": {
              "sum": {
                "field": "price"
              }
            }
          }
        }
      }
    }
  }
}


<!-- 根据make划分桶并进行统计，统计信息包含文档数、最大值、最小值、均值等等 -->
GET /cars/transactions/_search
{
  "size": 0,
  "aggs": {
    "car_make": {
      "terms": {
        "field": "make",
        "size": 10
      },
      "aggs": {
        "status_static": {
          "extended_stats": {
            "field": "price"
          }
        }
      }
    }
  }
}
```

- 使用range分段聚合，实现结果与histogram类似
```
<!-- 需要指定分段信息，并以这些分段信息划分桶 -->
GET /cars/transactions/_search
{
  "size": 0,
  "aggs": {
    "price_range": {
      "range": {
        "field": "price",
        "ranges": [
          {
            "from": 0,
            "to": 20000
          },
          {
            "from": 20000,
            "to": 40000
          },
          {
            "from": 40000,
            "to": 60000
          }
        ]
      },
      "aggs": {
        "sum_price": {
          "sum": {
            "field": "price"
          }
        }
      }
    }
  }
}
```

- 搜索子集并统计子集信息，同时还要统计全局信息（忽略范围限定）
```
<!-- 统计ford品牌均价以及全部品牌均价 -->
GET /cars/transactions/_search
{
  "size": 0,
  "query": {
    "match": {
      "make": "ford"
    }
  },
  "aggs": {
    "single_avg_price": {
      "avg": {
        "field": "price"
      }
    },
    "all_make_avg_price": {
      "global": {},
      "aggs": {
        "avg_price": {
          "avg": {
            "field": "price"
          }
        }
      }
    }
  }
}
```

- 在过滤搜索结果基础上，使用过滤桶（filter）过滤聚合结果（不过滤搜索返回结果，只过滤聚合返回结果）
```
<!-- 统计ford品牌特定时间内的均价 -->
GET /cars/transactions/_search
{
  "size": 0,
  "query": {
    "match": {
      "make": "ford"
    }
  },
  "aggs": {
    "recent_sales": {
      "filter": {
        "range": {
          "sold": {
            "from": "now-1M"
          }
        }
      },
      "aggs": {
        "average_price": {
          "avg": {
            "field": "price"
          }
        }
      }
    }
  }
}
```

- 在过滤搜索结果基础上进行聚合统计，使用后过滤器（post_filter）对返回结果进行再次过滤，但不影响聚合结果（只过滤搜索返回结果，不过滤聚合返回结果）。处于性能考虑，在非必要情况下不建议使用
```
<!-- 统计ford品牌每种颜色的销量，但只返回绿色查询结果 -->
GET /cars/transactions/_search
{
  "size": 0,
  "query": {
    "match": {
      "make": "ford"
    }
  },
  "post_filter": {
    "term": {
      "color": "green"
    }
  },
  "aggs": {
    "all_colors": {
      "terms": {
        "field": "color"
      }
    }
  }
}
```


- 近似聚合：提供准确但不是 100% 精确的结果。以牺牲一点小小的估算错误为代价，这些算法可以为我们换来高速的执行效率和极小的内存消耗。
    - cardinality度量：它提供一个字段的基数，即该字段的 distinct 或者 unique 值的数目。
    ```
    <!--
        1、统计每月有多少颜色的车被售出。
        2、cardinality 度量是一个近似算法，基于 HyperLogLog++ （HLL）算法实现，可以通过precision_threshold配置精度，
     -->
    GET /cars/transactions/_search
    {
      "size": 0,
      "aggs": {
        "month_sales": {
          "date_histogram": {
            "field": "sold",
            "interval": "month",
            "format": "yyyy-MM"
          },
          "aggs": {
            "distinct_colors": {
              "cardinality": {
                "field": "color",
                "precision_threshold": 100
              }
            }
          }
        }
      }
    }
    ```
    - percentiles度量：百分位数展现某以具体百分比下观察到的数值，通常用来找出异常。例如，第95个百分位上的数值，是高于 95% 的数据总和。
    ```
    <!--
       /1、统计网站的访问延时，默认情况下会返回一组预定义的百分位数值： [1, 5, 25, 50, 75, 95, 99]，latency字段表示访问延时
        2、使用TDigest 算法，可以通过修改参数 compression 来控制内存与准确度之间的比值。
    -->
    GET /website/logs/_search
    {
      "size": 0,
      "aggs": {
        "load_times": {
          "percentiles": {
            "field": "latency"
          }
        },
        "avg_load_time": {
          "avg": {
            "field": "latency"
          }
        }
      }
    }

    <!--
        1、首先根据区域分桶
        2、统计每个桶的百分位数值，并求平均值
     -->
    GET /website/logs/_search
    {
      "size": 0,
      "aggs": {
        "zones": {
          "terms": {
            "field": "zone"
          },
          "aggs": {
            "load_times": {
              "percentiles": {
                "field": "latency",
                "percents": [
                  50,
                  95.0,
                  99.0
                ]
              }
            },
            "load_avg": {
              "avg": {
                "field": "latency"
              }
            }
          }
        }
      }
    }
    ```
    - percentile_ranks度量：与percentiles度量紧密相关，percentile_ranks 告诉我们某个具体值属于哪个百分位
    ```
    GET /website/logs/_searchGET /website/logs/_search
    {
      "size": 0,
      "aggs": {
        "zones": {
          "terms": {
            "field": "zone"
          },
          "aggs": {
            "load_times": {
              "percentile_ranks": {
                "field": "latency",
                "values": [
                  210,
                  800
                ]
              }
            }
          }
        }
      }
    }
    ```