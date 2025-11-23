# 测试说明文档

本项目使用Spock框架编写单元测试。Spock是一个基于Groovy的测试框架，提供了更简洁和表达力更强的测试语法。

## 测试框架

- **Spock Framework**: 2.3-groovy-3.0
- **Groovy**: 3.0.17

## 运行测试

### 运行所有测试

```bash
mvn test
```

### 运行特定测试类

```bash
mvn test -Dtest=TypedValueSpec
```

### 运行特定测试方法

```bash
mvn test -Dtest=TypedValueSpec#测试TypedValue的基本创建和getValue方法
```

## 测试覆盖范围

### 1. 核心类测试

- **TypedValueSpec**: 测试TypedValue类的类型转换和值获取
- **RuleExecutionContextSpec**: 测试规则执行上下文的功能

### 2. 服务层测试

- **RuleServiceImplSpec**: 测试规则服务的CRUD操作
  - 保存和获取规则
  - 保存执行序列
  - 规则状态过滤
  - 异常处理

- **ABTestServiceImplSpec**: 测试A/B测试服务
  - A/B测试决策逻辑
  - 记录和查询A/B测试记录
  - 参数验证

- **EvalServiceImplSpec**: 测试规则计算服务
  - 正常执行流程
  - 消息队列异常处理
  - 执行上下文构建

### 3. 规则执行器测试

- **ExpressionRuleExecutorSpec**: 测试表达式规则执行器
  - 布尔表达式执行
  - 算术表达式执行
  - 系统变量和上下文变量使用
  - 异常处理

- **ScriptRuleExecutorSpec**: 测试Groovy脚本执行器
  - 简单脚本执行
  - 复杂脚本执行
  - 变量访问
  - 异常处理

### 4. 规则引擎测试

- **RuleExecutionEngineSpec**: 测试规则执行引擎
  - 规则序列执行
  - 规则状态控制
  - 环境判断
  - A/B测试规则执行
  - 提前跳出逻辑
  - 异常处理

### 5. 控制器测试

- **RuleControllerSpec**: 测试HTTP接口
  - 正常请求处理
  - 异常处理
  - 参数传递验证

## Spock测试语法说明

### Given-When-Then结构

```groovy
def "测试示例"() {
    given: "准备测试数据"
    def value = 100
    
    when: "执行操作"
    def result = value * 2
    
    then: "验证结果"
    result == 200
}
```

### Mock对象

```groovy
def service = Mock(Service)

when:
service.doSomething()

then:
1 * service.doSomething()  // 验证方法被调用一次
```

### 参数化测试

```groovy
@Unroll
def "测试类型转换 - 值: #value, 类型: #type"() {
    expect:
    convert(value) == expected
    
    where:
    value | type | expected
    100   | INT  | 100
    99.9  | DEC  | 99.9
}
```

### 异常测试

```groovy
when:
service.doSomething()

then:
thrown(RuntimeException)  // 验证抛出异常
```

## 测试覆盖率

运行测试覆盖率报告：

```bash
mvn clean test jacoco:report
```

覆盖率报告将生成在 `target/site/jacoco/index.html`

## 注意事项

1. **Groovy版本**: 确保Groovy版本与Spock兼容（当前使用3.0.17）
2. **测试隔离**: 每个测试方法都是独立的，不会相互影响
3. **Mock对象**: 使用Spock的Mock功能可以轻松模拟依赖
4. **参数化测试**: 使用`@Unroll`注解可以生成多个测试用例

## 扩展测试

### 添加新的测试类

1. 在`src/test/groovy`目录下创建对应的测试类
2. 类名以`Spec`结尾（Spock约定）
3. 继承`Specification`类
4. 使用Spock的测试语法编写测试用例

### 示例

```groovy
package lab.zhang.rule.rule_engine.service.impl

import spock.lang.Specification

class MyServiceSpec extends Specification {

    def service = new MyService()

    def "测试方法"() {
        given:
        // 准备数据

        when:
        // 执行操作

        then:
        // 验证结果
    }
}
```

## 持续集成

测试可以在CI/CD流程中自动运行：

```yaml
# 示例：GitHub Actions
- name: Run Tests
  run: mvn test
```

