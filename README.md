# MySpringMVC
## 实现

1. 配置文件加载，指定需要扫描的包的包名
2. LoadBeans

- 根据包名扫描包下所有的类，递归调用
  `private void loadClass(String packageName)`  
- 将带有MyController和MyService注解的类分别实例化，将实例化的对象加入IOC容器中
  `Object instance= cla.getDeclaredConstructor().newInstance(); ` 
  `addBean(beanName, instance);`  

3. IOC自动注入

- 遍历IOC Map，获取其成员变量
- 将带有AutoWired注解的变量赋值

4. HandlerMapping

- 初始化HandlerMapping，遍历IOC Map中的Values值（已经实例化的Beans）
- 筛选带有MyRequestMapping注解的对象，取出其中baseUrl
- 取出该对象中也被MyRequestMapping标记的所有方法，提取其中该注解中的值作为SubUrl和Method
- 使用baseUrl和SubUrl构造UrlAndMethod类，作为key值
- 针对该方法取出其请求返回类型ResponseType、该方法所在的类cla和该方法本身method，共同构造MVCMapping，作为value值
- 将上述key和value加入HandlerMapping中

5. MySpringMVCServlet处理用户请求的URL

- 获取用户请求的Url信息（包括URL路径，以及请求的方法）
- 根据获得的信息构造UrlAndMethod类，作为handlemapping的key
- 通过上一步构造的key，在HandleMapping中寻找对应的MVCMapping 对象，如果没有找到，则返回404 not found，表示该url并未被定义，或并未被标注
- 如果存在该MVCMapping对象，则利用反射机制，根据其中的信息调用对应的方法，根据请求中所包含的信息以及参数中的方法构造匹配关系，执行方法并返回对应的请求类型
  - 处理请求的类型(POST/GET)
  - 通过getParameterTypes、getParameterMap函数获得请求参数的类型与值，并存至parameterValues
  - 通过method.invoke函数得到请求的执行结果，并根据注解定义的responseType进行视图渲染

6. 视图渲染

- 若返回类型是responseBody，将对象自动转为JSON格式，以流的形式写回 
- 若返回类型是responseView，则将结果转化为modelView，取出其中的model，使用request.setAttribute，添加model数据；取出其中的view来转发到对应的jsp
  tRequestDispatcher("/WEB-INF/" + modelView.getView() +".jsp").forward(req, resp);  

7. 文件上传

获取request中POST方式中包含的文件信息，转化为FileItem类，传入upload函数中，指定文件存储目录；调用。write方法保存为本地文件；返回上传成功的信息

## 实现

## MVC测试

### 说明

1. 将实现的MVC web框架和web应用分开，分别位于mymvc包和app包中。
2. 根据应用场景不同，分别进行JSP页面的app测试，进行Restful服务的api测试。



### 数据 model

定义数据类型，这里定义了两个测试类。

- Book类Book(id, title, author)，实体类，数据对象。

- RestModel类RestModel(code, msg, data)，请求返回类，封装了restful api提供的一般数据类型，即响应状态码String code、响应消息String msg以及数据Object data。



### 服务 service

服务层，我们的项目测试没有使用数据库，在这里定义了BookList成员，。service提供了文件upload方法，bookList的增删改查方法。



### 控制器 controller

#### **请求 Request**

  使用@ResponseMapping注解标记，value定义url路由，method区分GET和POST两种请求方式。  

- GET方法能够通过注解@MyRequestParam接收请求参数，将参数全部解析为String字符串。

- POST方法，entype为application/x-www-form-urlencoded的表单参数可以通过@MyRequestParam获得，类型为String。对于multipart/form-data，基于实验要求，实现了对文件类型的解析。

#### **属性注入**

  使用@MyAutoWired注解标记，为属性自动注入。这里主要是对service层的自动注入。  

#### **响应 Response**  

- 使用@ResponseView注解标记返回jsp视图

  返回类型为MyModelView，需要MyModelView mv = new MyModelView后，使用mv.setView()指定要返回的jsp页面的文件名，使用mv.setData，设置要传递给jsp的数据对象，data可以为单个对象，也可以为List等集合。

- 使用@ResponseBody注解标记返回数据体

  返回类型为Java对象，将自动解析为Json格式，List等将解析为Json数组。实际应用时，可以自定义返回的数据结构。

### 视图 view

使用jsp进行视图层的展示，${model}即可解析在MyModelView中设置的model。
