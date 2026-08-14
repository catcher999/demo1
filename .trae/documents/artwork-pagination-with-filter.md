# 分页查询公开 AI 生成内容方案

## 概述

实现画廊模块的分页查询接口，支持按热度(heatScore)排序、按分类(category)筛选，只返回公开的作品。

## 当前状态分析

| 文件 | 状态 |
|------|------|
| [Artwork.java](file:///c:/Users/pluck/Desktop/demo1/src/main/java/com/example/demo/entity/Artwork.java) | 空类，无字段 |
| [ArtworkMapper.java](file:///c:/Users/pluck/Desktop/demo1/src/main/java/com/example/demo/mapper/ArtworkMapper.java) | 空接口，缺 `@Mapper` 注解和 import |
| [ArtworkService.java](file:///c:/Users/pluck/Desktop/demo1/src/main/java/com/example/demo/service/ArtworkService.java) | 空接口 |
| [ArtworkServiceImpl.java](file:///c:/Users/pluck/Desktop/demo1/src/main/java/com/example/demo/service/impl/ArtworkServiceImpl.java) | 空实现 |
| [GalleryController.java](file:///c:/Users/pluck/Desktop/demo1/src/main/java/com/example/demo/controller/GalleryController.java) | 有两个方法骨架，但注入了 ArtworkVO 作为 Bean（错误） |
| [ArtworkVO.java](file:///c:/Users/pluck/Desktop/demo1/src/main/java/com/example/demo/dto/response/ArtworkVO.java) | 已有字段：id, title, description, imageUrl, artist, date |
| MybatisPlusConfig | 设计文档提到但**不存在**，分页插件未配置 |
| Category 相关 | **完全不存在** |

## 技术决策

- **热度**：Artwork 实体加 `heatScore` 字段（综合热度分，整数）
- **分类**：独立 category 表，Artwork 通过 `category_id` 关联，查询用 XML JOIN
- **分页**：MyBatis-Plus `Page` + `PaginationInnerInterceptor`
- **公开筛选**：Artwork 实体加 `isPublic` 字段，查询条件 `WHERE is_public = 1`
- **遵循设计文档**：Controller 不做逻辑，Service 组装 VO，Mapper 用 Entity

## API 设计

```
GET /api/gallery/artworks?page=1&size=10&categoryId=1&sortBy=heatScore
```

| 参数 | 必填 | 默认值 | 说明 |
|------|------|--------|------|
| page | 否 | 1 | 页码 |
| size | 否 | 10 | 每页条数 |
| categoryId | 否 | null | 不传=查全部分类 |
| sortBy | 否 | heatScore | 排序字段，目前支持 heatScore / date |

响应结构：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "title": "日落",
        "description": "...",
        "imageUrl": "https://...",
        "artist": "zhangsan",
        "date": "2026-08-01T10:00:00",
        "heatScore": 85,
        "categoryName": "绘画"
      }
    ],
    "total": 100,
    "current": 1,
    "size": 10
  }
}
```

## 改动清单（按顺序）

### 步骤 1：创建 MybatisPlusConfig（分页插件）

**新建** `config/MybatisPlusConfig.java`

```java
@Configuration
public class MybatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

**为什么**：MyBatis-Plus 分页需要 `PaginationInnerInterceptor` 才能自动拼接 `LIMIT`。设计文档 [设计2.md](file:///c:/Users/pluck/Desktop/demo1/设计2.md) 第 63 行提到了这个配置类。

### 步骤 2：创建 Category 实体

**新建** `entity/Category.java`

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("category")
public class Category {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private Integer sortOrder;
}
```

### 步骤 3：创建 CategoryMapper

**新建** `mapper/CategoryMapper.java`

```java
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
```

简单 CRUD 走 BaseMapper，不需要 XML。

### 步骤 4：填充 Artwork 实体

**修改** [Artwork.java](file:///c:/Users/pluck/Desktop/demo1/src/main/java/com/example/demo/entity/Artwork.java)

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("artwork")
public class Artwork {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    private String imageUrl;
    private String artist;
    private Date date;
    private Integer heatScore;    // 综合热度分
    private Long categoryId;      // 关联 category 表
    private Boolean isPublic;     // 是否公开
    private Long userId;          // 创建者ID
}
```

### 步骤 5：修改 ArtworkVO

**修改** [ArtworkVO.java](file:///c:/Users/pluck/Desktop/demo1/src/main/java/com/example/demo/dto/response/ArtworkVO.java)

在现有字段基础上增加 `heatScore` 和 `categoryName`：

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArtworkVO {
    private long id;
    private String title;
    private String description;
    private String imageUrl;
    private String artist;
    private Date date;
    private Integer heatScore;      // 新增
    private String categoryName;   // 新增（来自 JOIN category 表）
}
```

> 注意：把 `private String title, description, imageUrl, artist;` 拆成独立行，便于阅读。

### 步骤 6：ArtworkMapper 加自定义方法 + XML

**修改** [ArtworkMapper.java](file:///c:/Users/pluck/Desktop/demo1/src/main/java/com/example/demo/mapper/ArtworkMapper.java)

```java
@Mapper
public interface ArtworkMapper extends BaseMapper<Artwork> {

    IPage<ArtworkVO> selectPublicArtworks(
            IPage<ArtworkVO> page,
            @Param("categoryId") Long categoryId,
            @Param("sortBy") String sortBy
    );
}
```

**新建** `resources/mapper/ArtworkMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.demo.mapper.ArtworkMapper">

    <select id="selectPublicArtworks" resultType="com.example.demo.dto.response.ArtworkVO">
        SELECT
            a.id,
            a.title,
            a.description,
            a.image_url,
            a.artist,
            a.date,
            a.heat_score,
            c.name AS category_name
        FROM artwork a
        LEFT JOIN category c ON a.category_id = c.id
        WHERE a.is_public = 1
        <if test="categoryId != null">
            AND a.category_id = #{categoryId}
        </if>
        ORDER BY
        <choose>
            <when test="sortBy == 'date'">a.date DESC</when>
            <otherwise>a.heat_score DESC</otherwise>
        </choose>
    </select>

</mapper>
```

**为什么用 XML**：多表 JOIN 查询，wrapper 不擅长。这也是设计文档里"简单用 wrapper，复杂用 XML"原则的体现。

### 步骤 7：填充 ArtworkService 接口

**修改** [ArtworkService.java](file:///c:/Users/pluck/Desktop/demo1/src/main/java/com/example/demo/service/ArtworkService.java)

```java
public interface ArtworkService {
    IPage<ArtworkVO> getPublicArtworks(
            int page,
            int size,
            Long categoryId,
            String sortBy
    );
}
```

### 步骤 8：填充 ArtworkServiceImpl

**修改** [ArtworkServiceImpl.java](file:///c:/Users/pluck/Desktop/demo1/src/main/java/com/example/demo/service/impl/ArtworkServiceImpl.java)

```java
@Service
public class ArtworkServiceImpl implements ArtworkService {

    private final ArtworkMapper artworkMapper;

    public ArtworkServiceImpl(ArtworkMapper artworkMapper) {
        this.artworkMapper = artworkMapper;
    }

    @Override
    public IPage<ArtworkVO> getPublicArtworks(
            int page,
            int size,
            Long categoryId,
            String sortBy
    ) {
        Page<ArtworkVO> pageParam = new Page<>(page, size);
        return artworkMapper.selectPublicArtworks(pageParam, categoryId, sortBy);
    }
}
```

**为什么 Service 不做 toVO 转换**：XML 查询直接返回 ArtworkVO（resultType 指向 VO），JOIN 时 category_name 直接映射到 VO 的 categoryName 字段，不需要手动转换。

### 步骤 9：修正 GalleryController

**修改** [GalleryController.java](file:///c:/Users/pluck/Desktop/demo1/src/main/java/com/example/demo/controller/GalleryController.java)

```java
@RestController
@RequestMapping("/api/gallery")
public class GalleryController {

    private final ArtworkService artworkService;

    public GalleryController(ArtworkService artworkService) {
        this.artworkService = artworkService;
    }

    @GetMapping("/artworks")
    public ResponseEntity<?> getArtworks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "heatScore") String sortBy
    ) {
        IPage<ArtworkVO> result = artworkService.getPublicArtworks(page, size, categoryId, sortBy);
        return ResponseEntity.ok(Result.success("Get artworks successful", result));
    }
}
```

**改动点**：
- 移除 ArtworkVO 注入（它不是 Bean，是数据载体）
- `getAllArtworks` → `getArtworks`，改为分页查询
- 用 `@RequestParam` 接收筛选参数，有默认值
- `getArtworkById` 暂时移除（当前不在本次范围内）

## 假设与决策

1. **数据库表结构**：假设 `artwork` 和 `category` 表已在数据库中创建，或用户会自行建表
2. **date 字段**：保持 `java.util.Date` 类型，和现有 ArtworkVO 一致
3. **getArtworkById**：本次不实现，只做分页列表查询
4. **分页响应格式**：直接用 MyBatis-Plus 的 `IPage`，不额外包装 PageVO，减少类数量
5. **sortBy 安全性**：XML 用 `<choose>` 固定列名，不接受任意字符串拼接，防止 SQL 注入

## 验证步骤

1. 项目能正常编译（`mvn compile`）
2. `GET /api/gallery/artworks` 返回分页 JSON 结构
3. `GET /api/gallery/artworks?categoryId=1` 只返回该分类的作品
4. `GET /api/gallery/artworks?sortBy=date` 按日期降序排列
5. `GET /api/gallery/artworks?page=2&size=5` 返回第二页，每页 5 条
6. 响应中 `categoryName` 字段有值（JOIN 成功）
