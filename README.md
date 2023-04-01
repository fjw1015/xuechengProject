# xuechengProject

### 学成教学平台

> 简单理解就是机构的教师上传自己的视频课程数据，学生在该平台进行买课程和观看视频学习。为实现该过程而搭建的一个教学平台。其中涉及到课程管理，媒资信息管理、课程审核和发布、文件上传、学生选课、选课支付、学生课程数据管理等功能。

##### 后端使用spring Boot、MySQL、Nacos、MinIO、Spring cloud Alibaba、Spring cloud gateway、Mybatis Plus、xxl-job、ElasticSearch、RabbitMQ、Redis、Freemarker

##### 前端使用 Vue、ts、node.js

> 前端启动 先下载相关modules -> 启动
> 
> npm i
> 
> npm run serve

##### 其中中间件都部署在同一台虚拟机服务器中，使用docker搭建部署

> 为了方便，代码提交到git平台 不再自己搭建gogs 原理和git基本一致
> [![ppRvMxH.png](https://s1.ax1x.com/2023/04/01/ppRvMxH.png)](https://imgse.com/i/ppRvMxH)

##### 前端使用VSCode 后端使用IDEA

> xuecheng-plus-front 为教学机构管理前端 xc-ui-pc-static-portal为普通用户使用界面 xuecheng-plus-project为各个微服务组件
