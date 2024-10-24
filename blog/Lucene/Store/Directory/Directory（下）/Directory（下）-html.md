---
title: Directory（下）（Lucene 7.5.0）
date: 2019-06-15 00:00:00
tags: [directory,mmap]
categories:
- Lucene
- Store
---

&emsp;&emsp;在[Directory（上）](https://www.amazingkoala.com.cn/Lucene/Store/2019/0613/Directory（上）)中，介绍了BaseDirectory类，它作为Directory的子类，该类及其子类实现了维护索引文件的所有操作，即`创建`、`打开`、`删除`、`读取`、`重命名`、`同步`(持久化索引文件至磁盘)、`校验和`（checksum computing）等等，而Directory的其他子类，不具备上述的维护索引文件的操作，而是封装了上述Directory类，提供更多高级功能。

图1：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Store/Directory/Directory（下）/1.png">

&emsp;&emsp;强调是，图1中只列出了Lucene7.5.0的core模块中的类图。

## FilterDirectory
&emsp;&emsp;FilterDirectory类作为一个抽象类，它的子类对封装的Directory类增加了不同的限制(limitation)来实现高级功能。

图2：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Store/Directory/Directory（下）/2.png">

### SleepingLockWrapper
&emsp;&emsp;在文章[索引文件锁LockFactory](https://www.amazingkoala.com.cn/Lucene/Store/2019/0604/索引文件锁LockFactory)中，索引目录同一时间只允许一个IndexWriter对象进行操作，此时另一个IndexWriter对象(不同的引用)操作该目录时会抛出LockObtainFailedException异常：

图3：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Store/Directory/Directory（下）/3.png">

&emsp;&emsp;在使用了SleepingLockWrapper后，会捕获LockObtainFailedException异常，同时等待1秒（默认值为1秒）后重试，如果在重试次数期间仍无法获得索引文件锁，那么抛出LockObtainFailedException异常。

### TrackingTmpOutputDirectoryWrapper
&emsp;&emsp;该类用来记录新创建临时索引文件，即带有.tmp后缀的文件。IndexWriter在调用addDocument()的方法时，flush()或者commit()前，就会生成[.fdx、.fdt](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2019/0301/索引文件之fdx&&fdt)以及[.tvd、.tvx](https://www.amazingkoala.com.cn/Lucene/suoyinwenjian/2019/0429/索引文件之tvx&&tvd)索引文件，而如果IndexWriter配置IndexSort，那么在上述期间内就只会生成临时的索引文件，TrackingTmpOutputDirectoryWrapper会记录这些临时索引文件，在后面介绍IndexWriter时会展开介绍：

图4：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Store/Directory/Directory（下）/4.png">

### TrackingDirectoryWrapper
&emsp;&emsp;该类用来记录新生成的索引文件名，不会记录从已有的索引目录中读取的索引文件名，比如在初始化Directory对象阶段会先读取索引目录中的索引文件。

### LockValidatingDirectoryWrapper
&emsp;&emsp;该类使得在执行`创建`、`删除`、`重命名`、`同步`(持久化索引文件至磁盘)的操作前都会先检查索引文件锁的状态是否有效的，比如说如果用户手动的把write.lock文件删除，那么会导致索引文件锁失效。

&emsp;&emsp;IndexWriter中使用了该类来维护索引文件。

### NRTCachingDirectory
&emsp;&emsp;该类维护了一个RAMDirectory，并封装了另一个Directory类，使用该类需要定义两个重要参数：

- maxMergeSizeBytes：允许的段合并生成的索引文件大小最大值
- maxCachedBytes：RAMDirectory允许存储的索引文件大小总和最大值

&emsp;&emsp;NRTCachingDirectory的处理逻辑就是根据下面的条件来选择使用RAMDirectory或者使用封装的Directory来存储索引条件
```java
boolean doCache = (bytes <= maxMergeSizeBytes) && (bytes + cache.ramBytesUsed()) <= maxCachedBytes
```

&emsp;&emsp;其中bytes为索引文件的大小，cache.ramBytesUsed()为RAMDirectory已经存储的所有索引文件大小总和，当doCache为真，则继续使用RAMDirectory存储该索引文件，否则使用封装的Directory。

## FileSwitchDirectory
&emsp;&emsp;在前面的介绍中我们知道，索引文件都存放同一个目录中，使用一个Directory对象来维护，而FileSwitchDirectory则使用两个Directory对象，即primaryDir跟secondaryDir，用户可以将索引文件分别写到primaryDir或者secondaryDir，使用primaryExtensions的Set对象来指定哪些后缀的索引文件使用primaryDir维护，否则使用secondaryDir维护，另外primaryDir或者secondaryDir可以使用同一个目录。

图5：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Store/Directory/Directory（下）/5.png">

&emsp;&emsp;图5中，primaryExtensions对象指定后缀为fdx、fdt、nvd、nvm的索引文件由primaryDir维护，即存放到 data01目录中，其他的索引文件存放到data02中，由secondaryDire维护，执行一次添加文档操作后，索引目录如下图：

图6：

<img src="http://www.amazingkoala.com.cn/uploads/lucene/Store/Directory/Directory（下）/6.png">

## Lucene50CompoundReader
&emsp;&emsp;该类仅用来读取复合文件(Compound File)，所以它仅支持`打开`、`读取`。比如当我们在初始化IndexWriter时，需要读取旧的索引文件，如果该索引文件使用了复合文件，那么就会调用Lucene50CompoundReader类中的方法来读取旧索引信息。

[点击下载](http://www.amazingkoala.com.cn/attachment/Lucene/Store/Directory/Directory（下）/Directory（下）.zip)Markdown文件

