# ERP-Spring-JDBC

这是 [ERP](https://github.com/framework-erp/ERP) 的 Spring JDBC 实现，为 ERP 提供关系型数据库的持久化机制。

## 功能

* 通过 Spring JDBC 实现 [Store](https://github.com/framework-erp/ERP/blob/master/src/main/java/erp/repository/Store.java)
  接口，实现实体数据持久化。
* 实现 [Mutexes](https://github.com/framework-erp/ERP/blob/master/src/main/java/erp/repository/Mutexes.java)
  接口。注意它不是分布式锁，不需要实现诸如 可重入，过期续约 等一个分布式锁系统方方面面。ERP的设计哲学认为这里需要的只是一个互斥
* 提供继承 XXXRepository 和直接实例化 XXXRepository 两种使用方式
* 提供用于测试的mock实现

## JAVA版本约定

* 用 Java1.8 编译，除非 Spring JDBC 的最新版本必须是比如 Java11 起步，要不就编译报错，那么就用 Java11 编译
* 编译出来的包，要保证1.8及以上的版本都能运行，除非 Spring JDBC 限制运行的java版本。如果出现不能运行，就要重新开发使得兼容

## ERP的其他实现

* [mongodb](https://github.com/framework-erp/ERP-mongodb)
* [redis](https://github.com/framework-erp/ERP-redis)
