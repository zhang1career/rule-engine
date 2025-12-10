
# 1. 生成mock规则

```shell
python3 convert_swagger_to_mock.py --swagger docs/api/swagger.yaml --output_dir mock
```


# 2. 插入数据

## 2.1. 事件

插入1000类事件：

```shell
python3 insert_data.py --config mock/create_event.mock --count 1000 --output out/mock_data.json
```


## 2.2. 规则

调整[mock文件](mock/create_rule.mock)，设置合适的用户属性生成规则：

```yaml
    content:
      type: STRING
      required: true
      description: Rule content (required)
      generation:
        method: "random_choice"
        values: [
          "userId == 10000100",
          "userId == 10000101",
          "userId == 10000102",
          "userId == 10000103",
          "userId == 10000104",
          "userId == 10000105",
          "userId == 10000106",
          "userId == 10000107",
          "userId == 10000108",
          "userId == 10000109",
          "(userId % 2) == 0",
          "(userId % 3) == 0",
          "(userId % 5) == 0",
          "(userId % 7) == 0",
          "(userId % 11) == 0",
          "(userId % 13) == 0",
          "(userId % 17) == 0",
          "(userId % 19) == 0",
          "(userId % 23) == 0",
          "(userId % 29) == 0",
          "(userId % age) == 0",
          "age < 16",
          "age < 18",
          "age > 80",
          "(age % 12) == 0",
          "amount < 0",
          "amount > 1000",
          "amount > 5000",
          "amount > 20000",
          "isVip == true",
          "isVip == false"
        ]
```

插入10000条规则：

```shell
python3 insert_data.py --config mock/create_rule.mock --count 10000
```

## 2.3. 事件-规则的关联

确认[mock文件](mock/set_execution_items_for_event.mock)，设置合适的事件ID和规则ID生成规则：

```yaml
api:
#  ...
  path_variables:
    eventId:
      type: LONG
      required: true
      generation:
        method: random_int
        min: 60000000
        max: 60000999
request_schema:
  fields:
    rules:
      type: ARRAY
      required: true
      description: List of execution items (rules and rule groups). The order in the list represents the execution order.
      element:
        type: Long
        required: true
        generation:
          method: random_int
          min: 10000000
          max: 10009999
      generation:
        method: array
        min_length: 3
        max_length: 15
```

插入事件-规则关联数据：

```shell
python3 insert_data.py --config mock/set_execution_items_for_event.mock --count 1000
```

## 2.4. 规则上线

### 2.4.1. 提测

确认[mock文件](mock/update_rule_to_test.mock)，设置顺序更新rule的状态的规则：

```yaml
api:
  path_variables:
  ruleId:
    type: LONG
    required: true
    generation:
      method: increment
      start: 10000000
      step: 1
request_schema:
  fields:
    ruleStatus:
      type: INTEGER
      required: false
      description: 'Rule status ID (optional, enumeration ID: 0=OFFLINE, 1=TEST, 2=GRAY,
        3=FULL)'
      generation:
        method: fixed
        value: 1
```

执行命令：

```shell
python3 insert_data.py --config  mock/update_rule_to_test.mock --count 10000
```

### 2.4.2. 灰度

确认[mock文件](mock/update_rule_to_gray.mock)，设置顺序更新rule的状态的规则：

```yaml
api:
  path_variables:
  ruleId:
    type: LONG
    required: true
    generation:
      method: increment
      start: 10000000
      step: 1
request_schema:
  fields:
    ruleStatus:
      type: INTEGER
      required: false
      description: 'Rule status ID (optional, enumeration ID: 0=OFFLINE, 1=TEST, 2=GRAY,
        3=FULL)'
      generation:
        method: fixed
        value: 2
```

执行命令：

```shell
python3 insert_data.py --config  mock/update_rule_to_gray.mock --count 10000
```

### 2.4.3. 上线

确认[mock文件](mock/update_rule_to_prod.mock)，设置随机更新rule的状态的规则：

```yaml
api:
  path_variables:
  ruleId:
    type: LONG
    required: true
    generation:
      method: random_int
      min: 10000000
      max: 10009999
request_schema:
  fields:
    ruleStatus:
      type: INTEGER
      required: false
      description: 'Rule status ID (optional, enumeration ID: 0=OFFLINE, 1=TEST, 2=GRAY,
        3=FULL)'
      generation:
        method: fixed
        value: 3
```

执行命令：

```shell
python3 insert_data.py --config  mock/update_rule_to_prod.mock --count 10000
```

### 2.5. 调节流量

确认[mock文件](mock/update_rule_group.mock)，设置随机更新ratio的状态的规则：

```yaml
api:
  path_variables:
  groupId:
    type: LONG
    required: true
    generation:
      method: increment
      start: 10000000
      step: 1
request_schema:
  fields:
    ruleRatios:
      type: ARRAY
      required: true
      description: List of execution items (rules and rule groups). The order in the list represents the execution order.
      element:
        type: OBJECT
        required: true
        description: Map of rule ID to A/B test ratio (0-100). Traffic ratios must be integers between 0-100, and sum must not exceed 100.
        fields:
          ruleId:
            type: LONG
            required: true
            description: Rule ID
            generation:
              method: random_int
              min: 10000000
              max: 10009999
          ratio:
            type: INTEGER
            required: true
            description: Traffic ratio for the rule (0-100)
            generation:
              method: random_int
              min: 0
              max: 100
      generation:
        method: array
        min_length: 1
        max_length: 1
```

执行命令：

```shell
python3 insert_data.py --config  mock/update_rule_group.mock --count 1300
```


# 3. 测试

```shell
python3 load_test.py --config  mock/execute_rule_evaluation.mock --concurrent 4 --total 1
```