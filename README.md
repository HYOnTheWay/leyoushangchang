# 1.乐优商城介绍

## 1.1.项目介绍

- 乐优商城是一个全品类的电商购物网站（B2C）。
- 用户可以在线购买商品、加入购物车、下单、秒杀商品
- 可以品论已购买商品
- 管理员可以在后台管理商品的上下架、促销活动
- 管理员可以监控商品销售状况
- 客服可以在后台处理退款操作



## 1.2.系统架构
#### 1.2.1.架构图
![整体架构](https://img-blog.csdnimg.cn/20190831150135741.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80MzMwODQwNg==,size_16,color_FFFFFF,t_70)
#### 1.2.2.系统架构解读
- 要点:
	- 整个乐优商城可以分为两部分：后台管理系统、前台门户系统。
	- 前端基于Vue相关技术栈进行开发,并通过ajax与后端服务进行交互
	- 前端通过nginx部署,并利用nginx实现对后台服务的反向代理和负载均衡
	- 部分热点静态资源,碰到流量高峰时,会考虑使用CDN服务
	- 使用lvs来实现Nginx的主从,保证nginx的高可用
	- 后端采用SpringCloud技术栈来搭建微服务集群,并对外提供Rest风格
	- zuul作为整个微服务入口,实现请求路由丶负载均衡丶限流丶权限控制等功能
	- 后端微服务集群会通过Spring-Cloud-Config结合Spring-Cloud-Bus来实现统一配置管理和配置动态刷新
	- 通过Spring-cloud-sleuth和Zipkin实现服务的链路追踪

### 1.2.3前端
前端页面分为两部分
  ###### 1.一部分面向公司内部人员,实现对商场日常业务的管理.(后台页面的管理)
- 技术实现
	- 后台管理系统会使用Vue.js框架搭建出单页应用（SPA）
	- Vue.js 2.0以及基于Vue的UI框架：Vuetify 构建也买
	- 前端构建工具：WebPack
	- 前端安装包工具：NPM,作为依赖管理
	- Vue脚手架：Vue-cli
	- Vue路由：vue-router
	- ajax框架：axios发起ajax请求,与后端交互
- 功能模块
	- 后台系统主要包含以下功能：
    - 商品管理，包括商品分类、品牌、商品规格等信息的管理
    - 销售管理，包括订单统计、订单退款处理、促销活动生成等
    - 用户管理，包括用户控制、冻结、解锁等
    - 权限管理，整个网站的权限控制，采用JWT鉴权方案，对用户及API进行权限控制
    - 统计，各种数据的统计分析展示 
  - 预览图：
  ![后台管理系统](https://img-blog.csdnimg.cn/20190831162356938.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80MzMwODQwNg==,size_16,color_FFFFFF,t_70)
  
  ###### 2.一部分面向买家,实现商品的展示,搜索,购买等功能.(前台门户页面)
  - 技术实现
	- 基于Vue技术栈
	- 使用Nuxt结合Vue完成页面开发。出于SEO优化的考虑，将不采用单页应用。
	- 其他与后台管理技术相似
  - 前台门户面向的是客户，包含与客户交互的一切功能。例如：
    - 搜索商品
    - 加入购物车
    - 下单
    - 评价商品等等
    - 预览图：
![前台页面](https://img-blog.csdnimg.cn/20190831162009193.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80MzMwODQwNg==,size_16,color_FFFFFF,t_70)

### 1.2.4后端服务
无论是前台还是后台系统，都共享相同的微服务集群，包括：

- 1.商品微服务：商品及商品分类、品牌、库存等的服务
	- 商品分类管理
	- 商品品牌管理
	- 商品规格参数管理:因为规格的可变性,采用竖表设计,分为规格和规格组
	- 商品管理,难点时spu和sku的设计,以及sku的动态属性
	- 库存管理,库存加减采用乐观锁方案,另外定时对库存进行判断,库存不足可通知管理员
- 2.搜索微服务：实现搜索功能
	- 采用elaticsearch完成商品的全文检索功能
	- 难点时搜索的过滤条件生成
	- 集群
- 3.订单微服务：实现订单相关
	- 订单的表设计,状态记录
	- 创建订单需要减库存,跨服务业务,需要注意事务处理,流程
		- 查询订单提交的商品信息
		- 计算订单总价
		- 写入订单,订单详情,订单状态
		- 减库存,远程同步调用商品微服务,实现减库存 
- 4.购物车微服务：实现购物车相关功能
	- 离线购物车:主要使用localstorage保存到客户端,几乎不与服务端交互
	- 在线购物车:使用redis实现 
- 5.用户服务：用户的登录注册等功能
	- 用户注册
	- 注册数据校验
		- thread的使用用户绑定
	- 查询用户信息
	- 收货地址管理
	- 用户积分管理等 	
- 6.认证服务：用户权限及服务权限认证
	- 权限管理CRUD
	- 登录token生成
	- 登录token认证
	- 服务间token生成
	- 服务间token认证
	- cookie的使用
- 7.短信服务:完成短信的发送
	- 对接阿里大于平台,通过MQ实现异步的短信发送
- 8.支付服务:完成支付平台对接
	- 对接微信支付
	- 对接支付宝支付
	- 对接其他支付 
- 9.Spring Cloud Config配置中心
- ...

## 1.3技术选型
#### 1.3.1.相关技术

前端技术：

- 基础的HTML、CSS、JavaScript（基于ES6标准）
- 后台管理系统会使用Vue.js框架搭建出单页应用（SPA）
- Vue.js 2.0以及基于Vue的UI框架：Vuetify 构建也买
- 前端构建工具：WebPack
- 前端安装包工具：NPM,作为依赖管理
- Vue脚手架：Vue-cli
- Vue路由：vue-router
- ajax框架：axios发起ajax请求,与后端交互
- 基于Vue的富文本框架：quill-editor
- Nuxt:服务端渲染

后端技术：

- 基础的SpringMVC 5.0.8、Spring 5.0.8和MyBatis3
- Spring Boot 2.0.4版本
- Spring Cloud 版本 Finchley.RC1
- Redis-4.0
- RabbitMQ-3.4
- Elasticsearch-6.2.4
- nginx-1.10.2
- FastDFS - 5.0.8
- Thymeleaf-3.0
- JWT

### 1.3.2技术解读

上面的技术组合可以在项目中解决以下电商中的典型问题：

- 利用Node.js及Vue.js技术栈，实现前后端分离开发

- 利用SpringCloud技术栈，实现真正的微服务实战开发，并且是基于SpringBoot2.0和SpringCloud最新版本Finchley.RC1实现，业内领先。

- 贴近真实的电商数据库设计，解决全品类电商的SPU和SKU管理问题

- 基于FastDFS解决大数据量的分布式文件存储问题

- 基于Elasticsearch高级聚合功能，实现商品的智能过滤搜索

- 基于Elasticsearch高级聚合功能，实现销售业务的复杂统计及报表输出

- 基于LocalStorage实现离线客户端购物车，减轻服务端压力。

- 基于JWT技术及RSA非对称加密实现真正无状态的单点登录。

- 结合JWT和RSA非对称加密，自定义Feign过滤器实现自动化服务间鉴权，解决服务对外暴露的安全问题

- 基于阿里大于实现SMS功能，解决电商短信通知问题

- 基于RabbitMQ实现可靠消息服务，解决服务间通信问题

- 基于RabbitMQ实现可靠消息服务，解决分布式事务问题

- 使用微信SDK实现微信扫码支付，符合主流付款方式

- 基于Redis搭建高可用集群，实现可靠缓存服务即热点数据保存。

  redis持久化，集群，哨兵，主从，缓存击穿，热点key。

- 基于Redis和Mq来应对高可用高并发的秒杀场景

- 基于MyCat实现数据库的读写分离和分库分表

  发视频

- 基于Thymeleaf实现页面模板和静态化，提高页面响应速度和并发能力

- 基于Nginx实现初步的请求负载均衡和请求限流

