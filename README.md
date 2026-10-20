# 大学选课系统

## 1. 项目名称

大学选课系统（University Course Selection System）

本项目使用 Java SE 开发，提供命令行版本和本地网页版两种入口。系统以学生为使用者，提供课程查询、课程详情查看、选课、退课和个人课表查看等功能，并在选课过程中自动检查课程容量、先修课程、时间冲突和学分上限。

## 2. 已实现的功能

### 2.1 登录与学生信息

- 通过学号进入系统。
- 内置两个演示学生账号：
  - `2023001`：汪思，人工智能，已修 `CS101`，学分上限 15。
  - `2023002`：陈悦，计算机科学与技术，未修先修课，学分上限 12。
- 查看学号、姓名、专业、已修课程和本学期已选课程。

### 2.2 课程查询

- 查看全部课程。
- 按课程编号、课程名称或教师姓名进行关键词搜索。
- 查看课程编号、名称、教师、学分、上课时间、先修课、课程容量和课程简介。

### 2.3 选课与退课

- 选课成功后，同时更新学生课表和课程已选人数。
- 退课成功后释放课程名额，并从学生课表中移除课程。
- 重复选课、选择不存在的课程、退选未选择的课程都会给出明确提示。

### 2.4 选课规则

- **容量校验**：课程达到容量后不能继续选课。
- **先修课校验**：学生必须完成课程要求的全部先修课程。
- **时间冲突校验**：新课程与已选课程在同一天的时间段重叠时不能选课。
- **学分上限校验**：选课后总学分不能超过学生的本学期上限。

### 2.5 本地网页版

- 通过 `com.course.WebMain` 启动本地 Web 服务。
- 终端输入学号后，程序会自动打开浏览器进入选课页面。
- 网页中可以查看学生信息、搜索课程、查看课程详情、选课、退课和查看当前课表。
- 网页端复用原有的 `EnrollmentService`、`CourseQueryService` 和选课规则，不重复编写业务逻辑。

## 3. 运行环境与启动方式

### 3.1 环境要求

- JDK 8 或更高版本。
- 使用 Java 标准库，不依赖第三方库。
- 终端建议使用 UTF-8 编码，以正常显示中文。

### 3.2 编译

在 `UniversityCourseSelection` 目录下执行：

```bash
mkdir -p out
javac -encoding UTF-8 -d out $(find src -name "*.java")
```

Windows PowerShell 可使用：

```powershell
New-Item -ItemType Directory -Force out
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse src -Filter *.java)
```

### 3.3 运行命令行版本

```bash
java -cp out com.course.Main
```

程序启动后输入 `2023001` 或 `2023002` 登录，再按菜单提示操作。

### 3.4 运行网页版

推荐使用以下命令启动网页版：

```bash
java -cp out com.course.WebMain --student=2023001
```

程序会在终端启动本地 Web 服务，并自动打开浏览器。若浏览器没有自动跳转，可复制终端输出的地址，例如：

```text
http://localhost:8080/
```

也可以不写学生参数，由程序在终端中提示输入学号：

```bash
java -cp out com.course.WebMain
```

保持该终端窗口打开即可继续访问网页；按 Enter 可停止 Web 服务。

## 4. 使用流程示例

1. 启动命令行程序，输入 `2023001`。
2. 输入 `1` 查看全部课程，确认课程编号和余量。
3. 输入 `4`，输入 `CS201`，由于该学生已修 `CS101`，选课成功。
4. 再输入 `4`，输入 `CS202`，系统提示与 `CS201` 时间冲突。
5. 输入 `6` 查看当前课表和总学分。
6. 输入 `5`，输入 `CS201`，系统完成退课并释放名额。
7. 使用 `2023002` 登录并选择 `CS201`，系统提示缺少先修课程 `CS101`。

## 5. 系统设计说明

### 5.1 总体结构

项目采用分层设计：

```text
com.course
├── Main.java                    命令行程序入口与依赖组装
├── WebMain.java                 网页版程序入口与本地服务启动
├── bootstrap/SampleData.java    演示数据初始化
├── model/                       领域实体
├── repository/                  数据访问抽象与内存实现
├── rule/                       可插拔的选课规则
├── service/                    查询和选课业务服务
├── ui/                         命令行交互界面
└── web/                        本地网页界面
```

主要调用关系如下：

```text
ConsoleApplication
    ├── CourseQueryService -> CourseRepository
    └── EnrollmentService
            ├── CapacityRule
            ├── PrerequisiteRule
            ├── ScheduleConflictRule
            └── CreditLimitRule

WebApplication
    ├── CourseQueryService -> CourseRepository
    └── EnrollmentService
            ├── CapacityRule
            ├── PrerequisiteRule
            ├── ScheduleConflictRule
            └── CreditLimitRule
```

### 5.2 封装

- `Person`、`Student`、`Course` 的属性均为 `private`，通过方法访问状态。
- `Student` 的已修课程和已选课程对外只提供不可修改视图，状态变化必须通过领域方法完成。
- `Course` 负责维护容量和选课学生集合，外部不能直接修改内部集合。
- `TimeSlot` 为不可变对象，避免上课时间在业务执行过程中被意外修改。

### 5.3 继承与抽象

- `Person` 是抽象基类，提取学号、姓名等公共属性；`Student` 继承 `Person` 并扩展专业、学分上限和选课信息。
- `CourseRepository` 和 `StudentRepository` 定义数据访问抽象，当前分别由内存实现类完成。未来可替换为文件或数据库实现而不影响上层业务。

### 5.4 多态

- `EnrollmentRule` 是选课规则接口，容量、先修课、时间冲突和学分上限分别由四个规则类实现。
- `EnrollmentService` 只依赖 `EnrollmentRule` 接口，按规则列表依次校验。新增规则时只需增加实现类并注入，不需要修改选课主流程。

### 5.5 类职责与关系

| 类/接口                 | 主要职责              | 关系                            |
| -------------------- | ----------------- | ----------------------------- |
| `Person`             | 公共人员属性            | `Student` 的抽象父类               |
| `Student`            | 维护学生档案和选课集合       | 聚合课程编号                        |
| `Course`             | 维护课程信息、容量和选课名册    | 聚合 `TimeSlot`                 |
| `TimeSlot`           | 表示不可变的每周上课时间      | 被 `Course` 组合                 |
| `CourseRepository`   | 课程查询抽象            | `InMemoryCourseRepository` 实现 |
| `EnrollmentRule`     | 选课校验抽象            | 四个规则类实现                       |
| `EnrollmentService`  | 处理选课/退课事务         | 组合多个规则                        |
| `CourseQueryService` | 处理课程搜索            | 依赖课程仓库                        |
| `ConsoleApplication` | 菜单和输入输出           | 调用业务服务                        |
| `WebApplication`     | 本地网页界面与 HTTP 请求处理 | 复用业务服务                        |

## 6. 关键业务逻辑

### 6.1 选课流程

```text
输入课程编号
  -> 查询课程是否存在
  -> 检查是否重复选课
  -> 依次执行容量、先修课、时间冲突、学分上限规则
  -> 所有规则通过
  -> 更新 Course 的选课名册
  -> 更新 Student 的已选课程
  -> 返回操作结果
```

### 6.2 时间冲突判断

两个 `TimeSlot` 只有在同一天且满足以下条件时发生冲突：

```text
新课程开始时间 < 已选课程结束时间
并且 已选课程开始时间 < 新课程结束时间
```

这种判断能够正确识别部分重叠、完全包含和相同时间等情况，而相邻但不重叠的课程不会被误判。

## 7. 测试记录

| 测试场景   | 操作                                                  | 预期结果              |
| ------ | --------------------------------------------------- | ----------------- |
| 查看全部课程 | 菜单 `1`                                              | 显示 6 门课程及余量       |
| 关键词搜索  | 菜单 `2` 输入 `Java`                                    | 显示 `CS101`        |
| 正常选课   | `2023001` 选择 `CS201`                                | 成功，课表增加该课程        |
| 缺少先修课  | `2023002` 选择 `CS201`                                | 失败，提示缺少 `CS101`   |
| 时间冲突   | `2023001` 先选 `CS201` 再选 `CS202`                     | 失败，提示时间冲突         |
| 重复选课   | 对同一课程连续选两次                                          | 第二次失败并提示已选择       |
| 退课     | 退选已选课程                                              | 成功，名额恢复           |
| 非法课程编号 | 输入 `ABC999`                                         | 提示课程不存在，程序继续运行    |
| 网页启动   | `java -cp out com.course.WebMain --student=2023001` | 启动本地服务并打开浏览器      |
| 网页选课   | 在网页点击 `CS201` 的选课按钮                                 | 成功后页面显示提示，课表增加该课程 |

## 8. 代码质量与可扩展性

- 业务规则与界面层分离，命令行界面和网页界面都复用同一套业务服务。
- 使用接口、抽象类和组合方式组织职责，避免把所有逻辑堆积到 `Main` 类。
- 关键状态使用不可变集合视图和校验方法保护，降低非法状态风险。
- 规则采用策略式设计，未来可加入“专业限制”“选课时间窗口”“先到先得”等规则。
- 当前数据为内存演示数据，重启后恢复初始状态；若需要持久化，可新增 `CourseRepository` 和 `StudentRepository` 的文件/数据库实现。

## 9. 文件说明

- `src/com/course/Main.java`：命令行程序入口。
- `src/com/course/WebMain.java`：网页版程序入口。
- `src/com/course/model/`：实体类。
- `src/com/course/repository/`：仓库接口及内存实现。
- `src/com/course/rule/`：选课规则接口及实现。
- `src/com/course/service/`：业务服务。
- `src/com/course/ui/`：命令行界面。
- `src/com/course/web/`：本地网页界面。

第一个演示学生账号已按要求录入姓名“汪思颖”和专业“人工智能”。如课程平台要求匿名提交，可在提交前改为化名。
