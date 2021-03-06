# Release Nodes

### 0.2.37
- 优化错误异常的堆栈信息，增加bridge的类名和方法名；
- 修复NotifyCenter从JS发送消息给Native时无法解析的问题；
- 修复Navigator.popToPage无法退回到指定页面的问题；
- 修复Timer组件在闭包中调用clearInterval失效的问题；
- 修复部分动画初始值写死的问题；
- 修复Request组件不支持form格式请求的问题；

### 0.2.36
- 使用Handler重写Timer，修复华为系统线程池过多导致的OOM问题；
- 修复9.0以下系统绘制阴影时可能引起的OOM问题；
- 修复NotifyCenter不能发送数组类型参数的问题；
- 升级soloader到0.9.0版本，修复yoga库在某些机型上加载失败的问题；

### 0.2.35
- 增加单击事件和长按事件的埋点；
- 修复平移动画不支持hm单位的问题；
- JS异常加上JS文件来源；
- 重新修复List组件快速滑动加载更多时导致的crash问题；
- 增加资源混淆白名单文件，配合乘客端打包；

### 0.2.34
- Hummer.isSupport接口增加Context参数；
- 去除RecyclerView默认动画，修复List组件快速滑动加载更多时导致的crash问题；

### 0.2.33
- 修复List组件加载更多时最后一个item会先消失一下再出现的问题；
- 修复Request网络库组件header不生效的问题；
- 修复Input组件进入页面前提前设置focused时不生效的问题；
- Memory组件增加namespace；
- load so 改用 relinker，修复部分机型加载库失败的问题；

### 0.2.31
- 修复List组件中的Input组件移出List可见范围后，组件会自动失焦但是键盘没有自动收起的问题；
- 修复部分空指针问题；

### 0.2.30
- 修复Input组件失焦时事件来两次的问题；
- Input组件新增键盘"完成"按钮的点击事件；
- 修复Scroller组件中的子组件改变高度后，Scroller组件高度不会自适应的问题；
- 修复到渐变色切换到普通颜色不起作用的问题；
- 修复热重载异常逻辑（只在debug版本下才能使用）；
- 加回信任所有证书的逻辑，仅去除信任所有hostname的逻辑；
- 修复quickjs异常错误信息不全的问题；
- 更新混淆规则，修复因为 keep class * 导致所有类名都没有被混淆的问题；

### 0.2.29
- 修复路由跳转时，同时调用popPage和openPage时的bug，在openPage新增closeSelf字段；
- Text组件增加lineSpacingMulti属性；
- 修复页面退出时可能引起的List组件崩溃问题；
- 修复Text组件fontWeight:normal无效的问题；

### 0.2.26
- 修复自定义Dialog部分机型上背景显示黑色问题；
- 自定义Dialog支持View复用；
- 支持点击键盘以外的区域把键盘关闭掉；
- 修复List组件item没有全屏撑开的问题；
- 修复控件在display隐藏再显示之后，边框消失的问题；
- Navigator增加popBack接口，适配cml；
- Navigator的popPage改成关闭自身，而不是依赖页面堆栈；
- 增加基础控件的recycle方法，用于手动释放控件java内存；
- Text组件的fontFamily属性支持多个字体输入；
- Text组件支持字间距；
- 优化setStyle性能问题；
- 更新namespace逻辑，为了支持多业务线；
- yoga库load增加异常捕获；
- 给Hummer默认容器设置一个兜底的初始化，以防业务方没有及时做初始化；

### 0.2.23
- 加入Hummer"命名空间"概念，用于隔离所有适配器的作用范围，为多业务线接入做准备；
- 修改jsc版本的编译选项，对c++_shared库的依赖改成NDK动态库的形式，不再显式引入c++_shared库，避免两个引擎库冲突问题；
- 修复控件只设置backgroundColor不生效的问题；
- 修复Android9.0以下时控件大小改变时阴影绘制crash问题；
- 修复Input文字输入过程中控件长度无法撑开的问题；
- 修复Input组件键盘显示和隐藏问题；
- 去掉Input组件的默认下划线样式，改成和iOS样式一致；
- 修复页面路由时全局UIHandler清除导致页面打不开的问题；
- 图片组件borderRadius支持百分比；
- 新增物理返回键生命周期回调方法；
- 修复自定义Dialog不居中显示的问题；
- 修复系统Dialog按钮颜色问题；
- 修复退出页面后JSCallback无效时的空指针问题；
- 去除SSL信任任何证书的代码，规避安全风险；
- 修复style map排序导致的position和display变为null的bug；
- 修复NotifyCenter消息发送纯字符串或者不填参数时的crash问题；
- 修复NotifyCenter消息未注册时发送消息Crash问题；
- console.log 支持前端的Object类型参数和多参数情况；
- 新增hummer-dev-tools开发工具；
- 修复Storage组件传json对象的String时，保存不了的问题；
- 修复finish时抛出null的异常时quickjs崩溃问题；
- 修复部分QuickJS错误堆栈信息输出不全的问题；

### 0.2.21
- 修复JSC JNI中之前异常参数漏加的问题
- 把quickjs依赖的c++静态库改成了动态库，修复集成到乘客端平台打包不过的问题

### 0.2.19
- 修复RootView设置为position: 'absolute' 布局大小为0的问题
- 修复Android9.0以下重复刷新阴影时阴影叠加的问题
- 修复pan事件的移动方向问题，和ios统一
- 修复没有定位时没有返回任何信息的问题
- 修复debug打印导致的crash问题
- Exception支持JSContext级别的回调监听

### 0.2.18
- 支持diaplay的block、inline、inline-block属性
- 支持position的fixed属性
- Text组件支持富文本属性
- 修复控件阴影显示问题
- 增加自定义对话框和Toast能力
- Scroll组件增加属性和修复bug
- Image组件支持相对路径图片源
- Image组件支持Gif图展示
- Navigator组件增加相对路径跳转
- 增加Touch事件
- 修复RootView的onDestroy生命周期没有被触发的问题
- 升级soloader到0.8.2版本
- 修复Hummer中的Yoga版本过高无法和RN兼容的问题