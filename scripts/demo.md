
# 1. 生成mock规则

```shell
python3 scripts/convert_swagger_to_mock.py --swagger docs/api/swagger.yaml --output_dir scripts/mock
```


# 2. 插入数据

## 2.1. 事件

插入1000类事件：

```shell
python3 scripts/insert_data.py --config scripts/mock/create_event.mock --count 1000 --output scripts/out/mock_data.json
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

插入100000条规则：

```shell
python3 scripts/insert_data.py --config scripts/mock/create_rule.mock --count 100000
```