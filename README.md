# thread-pool
工程有两个目的：
1.测试同步日志和异步日志的差异
2.根据kitty的代码做线程池监控和动态线程池调配，做了部分处理
  优化点：1.ResizableCapacityLinkedBlockIngQueue本质就是linkedblockingqueue,在压入任务入队列的时候判断task的数据和capacity，原逻辑用的=，新逻辑调整为>0.
          2.原始逻辑用cat做单个任务类的监控，本地没有接，就直接用任务名替换
          3.使用@Aysnc，做线程池任务处理，需要将本地生成的线程池对象交由spring管理
原始代码来源：https://github.com/yinjihuan/kitty
关注公众号:木巴沙
![image](https://user-images.githubusercontent.com/26828775/115985937-a554fd80-a5e0-11eb-94d3-07ac7051ec8f.png)
