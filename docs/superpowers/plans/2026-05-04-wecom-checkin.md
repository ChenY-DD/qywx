# WeCom Checkin Module Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a `WxCheckinQueryUtil` to `qywx-wecom-spring-boot-starter` that queries WeCom attendance groups, raw checkin records, daily/monthly reports, and business-level lists (late / early-leave / missing-card / absent / location-exception / device-exception).

**Architecture:** Reuse the primary `WxCpService` bean (no separate secret), call `wxCpService.getOaService()` for all checkin endpoints (WxJava 4.8.2 attaches them to `WxCpOaService`, including `getCropCheckinOption()` — note "Crop" typo). Add a dedicated `qywxCheckinExecutor` thread pool. Hide the WeCom API limits (≤100 userIds/call, ≤30 days/call) via transparent 2-D batching (date segments × user batches → cartesian product, run in parallel). Collect per-batch failures into a result record so callers get partial results.

**Tech Stack:** Java 21, Spring Boot 3.5, WxJava 4.8.2 (`weixin-java-cp`), Lombok, Mockito, AssertJ, JUnit 5.

---

## GLOBAL CODING CONVENTION (apply to EVERY new class and EVERY new public method)

**MANDATORY for all code in this plan:**

1. **Class-level Javadoc** in Chinese, ending with the two lines:
   ```
    *
    * @author CY
    * Copyright (c) CY
    */
   ```
   (Javadoc has no `@copyright` tag, so the copyright line is plain text inside the Javadoc body, immediately after `@author CY`.)

2. **Public-method Javadoc** in Chinese describing what the method does, plus `@param` / `@return` / `@throws` lines as applicable. Add `@author CY` only if the method's authorship differs from the class.

3. **Field comments** use single-line `// 中文说明` after the field declaration (or `/** 中文说明 */` above) — pick one style and stay consistent within a file.

4. **Lombok**: VOs use `@Data`. Records and immutable holders use `record` (no Lombok needed).

Every code block in this plan that creates a new class includes the required header — copy verbatim. If you add a class not shown explicitly in a step, you MUST still apply this convention.

---

## File Structure

### New files (`src/main/java/org/cy/qywx/`)

```
util/
  WxCheckinQueryUtil.java              核心查询工具
  WxCheckinConverter.java              WxJava bean → VO 静态转换器
  WxCheckinQueryOptions.java           record(segmentDays, userBatchSize, maxRetryAttempts, retryBackoffMillis, requestsPerSecond)
  WxCheckinFetchFailure.java           record，单批次失败
  WxCheckinRecordResult.java           record(records, failures)
  WxCheckinDayDataResult.java          record(dayDataList, failures)
  WxCheckinMonthDataResult.java        record(monthDataList, failures)
vo/
  WxCheckinGroupVO.java
  WxCheckinScheduleVO.java
  WxCheckinRangeVO.java
  WxCheckinRecordVO.java
  WxCheckinDayDataVO.java
  WxCheckinMonthDataVO.java
  WxCheckinExceptionVO.java
  WxCheckinExceptionItemVO.java
  WxCheckinLeaveVO.java
  WxCheckinOvertimeVO.java
  WxCheckinScheduleListItemVO.java
  WxCheckinApprovalItemVO.java
  WxAttendanceReportVO.java
vo/enums/
  WxCheckinTypeEnum.java
  WxCheckinExceptionTypeEnum.java
  WxCheckinResultEnum.java
```

### New test files (`src/test/java/org/cy/qywx/`)

```
util/
  WxCheckinConverterTest.java
  WxCheckinQueryUtilTest.java
  (existing) QywxWecomAutoConfigurationTest.java  ← extended
vo/enums/
  WxCheckinTypeEnumTest.java
  WxCheckinExceptionTypeEnumTest.java
  WxCheckinResultEnumTest.java
```

### Modified files

```
src/main/java/org/cy/qywx/config/WxCpProperties.java          add Checkin nested class
src/main/java/org/cy/qywx/config/QywxWecomAutoConfiguration.java  add executor + util beans
src/test/java/org/cy/qywx/config/QywxWecomAutoConfigurationTest.java  add Checkin assertions
README.md                                                      add Checkin section
README.zh-CN.md                                                add Checkin section
CLAUDE.md                                                      add Checkin pattern note
pom.xml                                                        1.0.1 → 1.0.2
```

---

## Task 1: Three Enums

**Files:**
- Create: `src/main/java/org/cy/qywx/vo/enums/WxCheckinTypeEnum.java`
- Create: `src/main/java/org/cy/qywx/vo/enums/WxCheckinExceptionTypeEnum.java`
- Create: `src/main/java/org/cy/qywx/vo/enums/WxCheckinResultEnum.java`
- Test: `src/test/java/org/cy/qywx/vo/enums/WxCheckinTypeEnumTest.java`
- Test: `src/test/java/org/cy/qywx/vo/enums/WxCheckinExceptionTypeEnumTest.java`
- Test: `src/test/java/org/cy/qywx/vo/enums/WxCheckinResultEnumTest.java`

- [ ] **Step 1.1: Write enum test files**

```java
// src/test/java/org/cy/qywx/vo/enums/WxCheckinTypeEnumTest.java
package org.cy.qywx.vo.enums;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class WxCheckinTypeEnumTest {

    @Test
    void fromCode_returnsMatchingEnum() {
        assertThat(WxCheckinTypeEnum.fromCode(1)).isEqualTo(WxCheckinTypeEnum.NORMAL);
        assertThat(WxCheckinTypeEnum.fromCode(2)).isEqualTo(WxCheckinTypeEnum.OUTSIDE);
        assertThat(WxCheckinTypeEnum.fromCode(3)).isEqualTo(WxCheckinTypeEnum.ALL);
    }

    @Test
    void fromCode_unknownReturnsNull() {
        assertThat(WxCheckinTypeEnum.fromCode(99)).isNull();
        assertThat(WxCheckinTypeEnum.fromCode(null)).isNull();
    }

    @Test
    void getCodeAndDescription() {
        assertThat(WxCheckinTypeEnum.NORMAL.getCode()).isEqualTo(1);
        assertThat(WxCheckinTypeEnum.NORMAL.getDescription()).isEqualTo("上下班打卡");
    }
}
```

```java
// src/test/java/org/cy/qywx/vo/enums/WxCheckinExceptionTypeEnumTest.java
package org.cy.qywx.vo.enums;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class WxCheckinExceptionTypeEnumTest {

    @Test
    void fromCode_returnsMatchingEnum() {
        assertThat(WxCheckinExceptionTypeEnum.fromCode(1)).isEqualTo(WxCheckinExceptionTypeEnum.LATE);
        assertThat(WxCheckinExceptionTypeEnum.fromCode(2)).isEqualTo(WxCheckinExceptionTypeEnum.EARLY_LEAVE);
        assertThat(WxCheckinExceptionTypeEnum.fromCode(3)).isEqualTo(WxCheckinExceptionTypeEnum.MISSING_CARD);
        assertThat(WxCheckinExceptionTypeEnum.fromCode(4)).isEqualTo(WxCheckinExceptionTypeEnum.ABSENT);
        assertThat(WxCheckinExceptionTypeEnum.fromCode(5)).isEqualTo(WxCheckinExceptionTypeEnum.LOCATION_EXCEPTION);
        assertThat(WxCheckinExceptionTypeEnum.fromCode(6)).isEqualTo(WxCheckinExceptionTypeEnum.DEVICE_EXCEPTION);
    }

    @Test
    void fromCode_unknownReturnsNull() {
        assertThat(WxCheckinExceptionTypeEnum.fromCode(0)).isNull();
        assertThat(WxCheckinExceptionTypeEnum.fromCode(null)).isNull();
    }
}
```

```java
// src/test/java/org/cy/qywx/vo/enums/WxCheckinResultEnumTest.java
package org.cy.qywx.vo.enums;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class WxCheckinResultEnumTest {

    @Test
    void fromText_recognisedValues() {
        assertThat(WxCheckinResultEnum.fromText("正常")).isEqualTo(WxCheckinResultEnum.NORMAL);
        assertThat(WxCheckinResultEnum.fromText("时间异常")).isEqualTo(WxCheckinResultEnum.TIME_EXCEPTION);
        assertThat(WxCheckinResultEnum.fromText("地点异常")).isEqualTo(WxCheckinResultEnum.LOCATION_EXCEPTION);
        assertThat(WxCheckinResultEnum.fromText("未打卡")).isEqualTo(WxCheckinResultEnum.NOT_CHECKED_IN);
        assertThat(WxCheckinResultEnum.fromText("无效")).isEqualTo(WxCheckinResultEnum.INVALID);
    }

    @Test
    void fromText_unrecognisedReturnsUnknown() {
        assertThat(WxCheckinResultEnum.fromText("外星人异常")).isEqualTo(WxCheckinResultEnum.UNKNOWN);
        assertThat(WxCheckinResultEnum.fromText(null)).isEqualTo(WxCheckinResultEnum.UNKNOWN);
        assertThat(WxCheckinResultEnum.fromText("")).isEqualTo(WxCheckinResultEnum.UNKNOWN);
    }
}
```

- [ ] **Step 1.2: Run tests to verify failure**

```
mvn test -Dtest='WxCheckin*EnumTest'
```

Expected: compilation failure (enum classes don't exist).

- [ ] **Step 1.3: Create `WxCheckinTypeEnum.java`**

```java
package org.cy.qywx.vo.enums;

/**
 * 打卡记录类型枚举。
 * <p>对应企业微信 {@code getcheckindata} 接口的 {@code opencheckindatatype} 请求参数：
 * 1=上下班打卡、2=外出打卡、3=全部。</p>
 *
 * @author CY
 * Copyright (c) CY
 */
public enum WxCheckinTypeEnum {

    NORMAL(1, "上下班打卡"),
    OUTSIDE(2, "外出打卡"),
    ALL(3, "全部");

    private final int code;
    private final String description;

    WxCheckinTypeEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取枚举对应的企业微信 API 数值。
     *
     * @return code
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取枚举的中文描述。
     *
     * @return 中文说明
     */
    public String getDescription() {
        return description;
    }

    /**
     * 根据企业微信返回的数值反查枚举。
     *
     * @param code 企业微信数值
     * @return 对应枚举；未匹配返回 {@code null}
     */
    public static WxCheckinTypeEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (WxCheckinTypeEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return null;
    }
}
```

- [ ] **Step 1.4: Create `WxCheckinExceptionTypeEnum.java`**

```java
package org.cy.qywx.vo.enums;

/**
 * 打卡日报异常类型枚举。
 * <p>对应企业微信 {@code getcheckin_daydata} 接口返回的 {@code exception_infos[].exception} 字段：
 * 1=迟到、2=早退、3=缺卡、4=旷工、5=地点异常、6=设备异常。</p>
 *
 * @author CY
 * Copyright (c) CY
 */
public enum WxCheckinExceptionTypeEnum {

    LATE(1, "迟到"),
    EARLY_LEAVE(2, "早退"),
    MISSING_CARD(3, "缺卡"),
    ABSENT(4, "旷工"),
    LOCATION_EXCEPTION(5, "地点异常"),
    DEVICE_EXCEPTION(6, "设备异常");

    private final int code;
    private final String description;

    WxCheckinExceptionTypeEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据企业微信数值反查枚举。
     *
     * @param code 企业微信 exception 数值
     * @return 对应枚举；未匹配返回 {@code null}
     */
    public static WxCheckinExceptionTypeEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (WxCheckinExceptionTypeEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return null;
    }
}
```

- [ ] **Step 1.5: Create `WxCheckinResultEnum.java`**

```java
package org.cy.qywx.vo.enums;

/**
 * 单次打卡结果枚举。
 * <p>对应企业微信 {@code getcheckindata} 接口返回的 {@code exception_type} 字段（中文字符串）。
 * 该字段企业微信文档允许多值分号分隔，本工具按第一个匹配项映射；未识别的字符串归到 {@link #UNKNOWN}。</p>
 *
 * @author CY
 * Copyright (c) CY
 */
public enum WxCheckinResultEnum {

    NORMAL("正常"),
    TIME_EXCEPTION("时间异常"),
    LOCATION_EXCEPTION("地点异常"),
    NOT_CHECKED_IN("未打卡"),
    INVALID("无效"),
    UNKNOWN("未知");

    private final String text;

    WxCheckinResultEnum(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    /**
     * 根据企业微信返回的中文字符串反查枚举。
     *
     * @param text 中文字符串，可能含分号分隔的多个值
     * @return 第一个匹配的枚举；空 / null / 未匹配返回 {@link #UNKNOWN}
     */
    public static WxCheckinResultEnum fromText(String text) {
        if (text == null || text.isBlank()) {
            return UNKNOWN;
        }
        String first = text.contains(";") ? text.substring(0, text.indexOf(';')).trim() : text.trim();
        for (WxCheckinResultEnum e : values()) {
            if (e.text.equals(first)) {
                return e;
            }
        }
        return UNKNOWN;
    }
}
```

- [ ] **Step 1.6: Run enum tests**

```
mvn test -Dtest='WxCheckin*EnumTest'
```

Expected: 3 test classes pass, all tests green.

- [ ] **Step 1.7: Commit**

```
git add src/main/java/org/cy/qywx/vo/enums/ src/test/java/org/cy/qywx/vo/enums/
git commit -m "feat: add WeCom checkin enums (type/exception-type/result)"
```

---

## Task 2: Leaf VOs (no internal cross-references)

**Files (all under `src/main/java/org/cy/qywx/vo/`):**
- Create: `WxCheckinScheduleVO.java`
- Create: `WxCheckinRangeVO.java`
- Create: `WxCheckinExceptionVO.java`
- Create: `WxCheckinLeaveVO.java`
- Create: `WxCheckinOvertimeVO.java`
- Create: `WxCheckinApprovalItemVO.java`
- Create: `WxCheckinScheduleListItemVO.java`
- Create: `WxCheckinExceptionItemVO.java`

These are pure Lombok `@Data` classes — no business logic, no tests required at this layer (they will be exercised through the converter tests in Task 5).

- [ ] **Step 2.1: Create `WxCheckinScheduleVO.java`**

```java
package org.cy.qywx.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 考勤组班次 VO。
 *
 * @author CY
 * Copyright (c) CY
 */
@Data
public class WxCheckinScheduleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 班次 ID */
    private Integer scheduleId;

    /** 班次名称 */
    private String scheduleName;

    /** 时段总数 */
    private Integer timeSection;

    /** 是否禁用 */
    private Boolean disabled;
}
```

- [ ] **Step 2.2: Create `WxCheckinRangeVO.java`**

```java
package org.cy.qywx.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 考勤组可用范围 VO（部门 / 标签 / userId 白名单）。
 *
 * @author CY
 * Copyright (c) CY
 */
@Data
public class WxCheckinRangeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 部门 ID 列表 */
    private List<Integer> partyIds;

    /** 用户 ID 列表 */
    private List<String> userIds;

    /** 标签 ID 列表 */
    private List<Integer> tagIds;
}
```

- [ ] **Step 2.3: Create `WxCheckinExceptionVO.java`**

```java
package org.cy.qywx.vo;

import lombok.Data;
import org.cy.qywx.vo.enums.WxCheckinExceptionTypeEnum;

import java.io.Serializable;

/**
 * 嵌入在日报 / 月报内的异常项 VO。
 *
 * @author CY
 * Copyright (c) CY
 */
@Data
public class WxCheckinExceptionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 异常类型枚举 */
    private WxCheckinExceptionTypeEnum exceptionType;

    /** 当日 / 当月异常次数 */
    private Integer count;

    /** 异常时长（秒） */
    private Integer durationSeconds;
}
```

- [ ] **Step 2.4: Create `WxCheckinLeaveVO.java`**

```java
package org.cy.qywx.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 请假 / 外出项 VO（来源 holiday_infos）。
 *
 * @author CY
 * Copyright (c) CY
 */
@Data
public class WxCheckinLeaveVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 审批单号 */
    private String spNumber;

    /** 假期类型描述 */
    private String spTitle;

    /** 假期时长（秒） */
    private Integer durationSeconds;

    /** 开始时间（unix 时间戳） */
    private Long startTime;

    /** 结束时间（unix 时间戳） */
    private Long endTime;
}
```

- [ ] **Step 2.5: Create `WxCheckinOvertimeVO.java`**

```java
package org.cy.qywx.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 加班信息 VO（来源 ot_info）。
 *
 * @author CY
 * Copyright (c) CY
 */
@Data
public class WxCheckinOvertimeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 加班状态 */
    private Integer otStatus;

    /** 加班时长（秒） */
    private Integer otDurationSeconds;

    /** 加班异常时长（秒）数组 */
    private List<Integer> exceptionDurationSeconds;
}
```

- [ ] **Step 2.6: Create `WxCheckinApprovalItemVO.java`**

```java
package org.cy.qywx.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 影响某一打卡日 / 月的审批单项 VO（来源 sp_items）。
 *
 * @author CY
 * Copyright (c) CY
 */
@Data
public class WxCheckinApprovalItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 审批单号 */
    private String spNumber;

    /** 审批类型 */
    private Integer type;

    /** 审批分类描述 */
    private String spTitle;

    /** 审批通过的工时（秒） */
    private Integer durationSeconds;

    /** 关联日期（unix 时间戳） */
    private Long timeUnix;
}
```

- [ ] **Step 2.7: Create `WxCheckinScheduleListItemVO.java`**

```java
package org.cy.qywx.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 排班列表单项 VO（来源 getcheckinschedulist）。
 *
 * @author CY
 * Copyright (c) CY
 */
@Data
public class WxCheckinScheduleListItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    private String userId;

    /** 排班日期 */
    private Date scheduleDate;

    /** 考勤组 ID */
    private Integer groupId;

    /** 班次 ID（0 表示休息） */
    private Integer scheduleId;

    /** 班次名称 */
    private String scheduleName;
}
```

- [ ] **Step 2.8: Create `WxCheckinExceptionItemVO.java`**

```java
package org.cy.qywx.vo;

import lombok.Data;
import org.cy.qywx.vo.enums.WxCheckinExceptionTypeEnum;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 业务语义方法（{@code getLatePersons} 等）返回的扁平异常行 VO。
 * <p>每行表示「某 userId 在某天发生了某种异常」，多次迟到的人会按日期粒度出现多行。</p>
 *
 * @author CY
 * Copyright (c) CY
 */
@Data
public class WxCheckinExceptionItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    private String userId;

    /** 用户姓名（来自日报 base_info.name） */
    private String name;

    /** 用户所属部门（按分号拆分后的列表） */
    private List<String> departments;

    /** 异常发生的日期 */
    private Date date;

    /** 异常类型 */
    private WxCheckinExceptionTypeEnum exceptionType;

    /** 当日该类型的异常次数 */
    private Integer exceptionCount;

    /** 当日该类型的异常时长（分钟，由 WeCom 秒数 / 60 向下取整） */
    private Long exceptionDurationMinutes;
}
```

- [ ] **Step 2.9: Compile sanity check**

```
mvn clean compile
```

Expected: no errors.

- [ ] **Step 2.10: Commit**

```
git add src/main/java/org/cy/qywx/vo/WxCheckinScheduleVO.java \
        src/main/java/org/cy/qywx/vo/WxCheckinRangeVO.java \
        src/main/java/org/cy/qywx/vo/WxCheckinExceptionVO.java \
        src/main/java/org/cy/qywx/vo/WxCheckinLeaveVO.java \
        src/main/java/org/cy/qywx/vo/WxCheckinOvertimeVO.java \
        src/main/java/org/cy/qywx/vo/WxCheckinApprovalItemVO.java \
        src/main/java/org/cy/qywx/vo/WxCheckinScheduleListItemVO.java \
        src/main/java/org/cy/qywx/vo/WxCheckinExceptionItemVO.java
git commit -m "feat: add leaf VOs for WeCom checkin module"
```

---

## Task 3: Composite VOs

**Files (all under `src/main/java/org/cy/qywx/vo/`):**
- Create: `WxCheckinGroupVO.java`
- Create: `WxCheckinRecordVO.java`
- Create: `WxCheckinDayDataVO.java`
- Create: `WxCheckinMonthDataVO.java`
- Create: `WxAttendanceReportVO.java`

- [ ] **Step 3.1: Create `WxCheckinGroupVO.java`**

```java
package org.cy.qywx.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 考勤组 VO（来源 getcorpcheckinoption / WxJava 中实际为 getCropCheckinOption）。
 *
 * @author CY
 * Copyright (c) CY
 */
@Data
public class WxCheckinGroupVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 考勤组 ID */
    private Integer groupId;

    /** 考勤组名称 */
    private String groupName;

    /** 考勤组类型描述（FIXED / SCHEDULE / FLEX 等） */
    private String groupType;

    /** 创建时间 */
    private Date createTime;

    /** 班次列表 */
    private List<WxCheckinScheduleVO> schedules;

    /** 可用范围（部门 / 标签 / userId） */
    private WxCheckinRangeVO range;

    /** 白名单（不需要打卡的 userId） */
    private List<String> whiteUsers;

    /** 是否需要拍照 */
    private Boolean needPhoto;

    /** 备注是否允许使用本地图片 */
    private Boolean noteCanUseLocalPic;

    /** 非工作日是否允许打卡 */
    private Boolean allowCheckinOffWorkday;

    /** 非工作日是否允许补卡 */
    private Boolean allowApplyOffWorkday;

    /** 是否自动同步法定节假日 */
    private Boolean syncHolidays;

    /** 特殊上班日 */
    private List<Date> speWorkdays;

    /** 特殊放假日 */
    private List<Date> speOffdays;
}
```

- [ ] **Step 3.2: Create `WxCheckinRecordVO.java`**

```java
package org.cy.qywx.vo;

import lombok.Data;
import org.cy.qywx.vo.enums.WxCheckinResultEnum;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 单条打卡记录 VO（来源 getcheckindata）。
 *
 * @author CY
 * Copyright (c) CY
 */
@Data
public class WxCheckinRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    private String userId;

    /** 考勤组名称 */
    private String groupName;

    /** 考勤组 ID */
    private Integer groupId;

    /** 班次 ID */
    private Integer scheduleId;

    /** 时段 ID */
    private Integer timelineId;

    /** 打卡类型（上班打卡 / 下班打卡 / 外出打卡，企业微信原始字符串） */
    private String checkinType;

    /** 打卡时间 */
    private Date checkinTime;

    /** 应打卡时间（仅排班 / 固定时段） */
    private Date schCheckinTime;

    /** 打卡地点标题 */
    private String locationTitle;

    /** 打卡地点详情 */
    private String locationDetail;

    /** Wi-Fi 名称 */
    private String wifiName;

    /** Wi-Fi MAC */
    private String wifiMac;

    /** 备注 */
    private String notes;

    /** 附件 mediaId 列表 */
    private List<String> mediaIds;

    /** 设备 ID */
    private String deviceId;

    /** 经度（实际值的 1_000_000 倍） */
    private Integer lng;

    /** 纬度（实际值的 1_000_000 倍） */
    private Integer lat;

    /** 异常结果（首个匹配） */
    private WxCheckinResultEnum exceptionResult;

    /** 原始异常字符串（企业微信原文，多值分号分隔） */
    private String rawExceptionType;
}
```

- [ ] **Step 3.3: Create `WxCheckinDayDataVO.java`**

```java
package org.cy.qywx.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 单人单日打卡日报 VO（来源 getcheckin_daydata.datas[]）。
 *
 * @author CY
 * Copyright (c) CY
 */
@Data
public class WxCheckinDayDataVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    private String userId;

    /** 用户姓名 */
    private String name;

    /** 部门列表（companies 拆分后） */
    private List<String> departments;

    /** 日报日期 */
    private Date date;

    /** 记录类型（1 固定上下班 / 3 班次 / 4 自由签到 / 5 加班 / 7 无规则 等） */
    private Integer recordType;

    /** 应工作时长（秒） */
    private Integer standardWorkSeconds;

    /** 弹性工作时长（秒，可正可负） */
    private Integer flexWorkSeconds;

    /** 规则工作时长（秒） */
    private Integer regularWorkSeconds;

    /** 最早打卡时间 */
    private Date earliestCheckinTime;

    /** 最晚打卡时间 */
    private Date latestCheckinTime;

    /** 异常项列表 */
    private List<WxCheckinExceptionVO> exceptions;

    /** 请假 / 外出项列表 */
    private List<WxCheckinLeaveVO> holidays;

    /** 加班信息 */
    private WxCheckinOvertimeVO overtime;

    /** 影响该日的审批单 */
    private List<WxCheckinApprovalItemVO> approvals;
}
```

- [ ] **Step 3.4: Create `WxCheckinMonthDataVO.java`**

```java
package org.cy.qywx.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 单人单月打卡月报 VO（来源 getcheckin_monthdata.datas[]）。
 *
 * @author CY
 * Copyright (c) CY
 */
@Data
public class WxCheckinMonthDataVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    private String userId;

    /** 用户姓名 */
    private String name;

    /** 部门列表 */
    private List<String> departments;

    /** 月份首日 */
    private Date monthDate;

    /** 应出勤天数 */
    private Integer workDays;

    /** 排除天数（请假 / 外出等） */
    private Integer excludedDays;

    /** 实际工作时长（秒） */
    private Integer workSeconds;

    /** 标准工作时长（秒） */
    private Integer standardWorkSeconds;

    /** 月度异常汇总 */
    private List<WxCheckinExceptionVO> exceptions;

    /** 月度请假项 */
    private List<WxCheckinLeaveVO> holidays;

    /** 月度加班信息 */
    private WxCheckinOvertimeVO overtime;

    /** 月度相关审批 */
    private List<WxCheckinApprovalItemVO> approvals;
}
```

- [ ] **Step 3.5: Create `WxAttendanceReportVO.java`**

```java
package org.cy.qywx.vo;

import lombok.Data;
import org.cy.qywx.util.WxCheckinFetchFailure;
import org.cy.qywx.util.WxDateRange;

import java.io.Serializable;
import java.util.List;

/**
 * 一次性聚合的考勤报表 VO。{@link org.cy.qywx.util.WxCheckinQueryUtil#getAttendanceReport}
 * 返回此结构，避免调用方为 6 种异常类型分别发起请求。
 *
 * @author CY
 * Copyright (c) CY
 */
@Data
public class WxAttendanceReportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 报表时间范围 */
    private WxDateRange range;

    /** 入参 userIds 去重后总数 */
    private int totalUsers;

    /** 实际返回了日报的人数（无打卡的会缺失） */
    private int reportedUsers;

    /** 迟到记录（按日扁平） */
    private List<WxCheckinExceptionItemVO> late;

    /** 早退记录 */
    private List<WxCheckinExceptionItemVO> earlyLeave;

    /** 缺卡记录 */
    private List<WxCheckinExceptionItemVO> missingCard;

    /** 旷工记录 */
    private List<WxCheckinExceptionItemVO> absent;

    /** 地点异常记录 */
    private List<WxCheckinExceptionItemVO> locationException;

    /** 设备异常记录 */
    private List<WxCheckinExceptionItemVO> deviceException;

    /** 失败的批次列表（如有） */
    private List<WxCheckinFetchFailure> failures;
}
```

> Note: `WxCheckinFetchFailure` is created in Task 4. The compile-check at Step 3.6 will fail until Task 4 is done. That's expected — proceed to Task 4 immediately, then come back if needed.

- [ ] **Step 3.6: Skip standalone compile (deferred until Task 4); commit composites**

```
git add src/main/java/org/cy/qywx/vo/WxCheckinGroupVO.java \
        src/main/java/org/cy/qywx/vo/WxCheckinRecordVO.java \
        src/main/java/org/cy/qywx/vo/WxCheckinDayDataVO.java \
        src/main/java/org/cy/qywx/vo/WxCheckinMonthDataVO.java \
        src/main/java/org/cy/qywx/vo/WxAttendanceReportVO.java
git commit -m "feat: add composite VOs for WeCom checkin module"
```

---

## Task 4: Failure record + Options record + Result records

**Files:**
- Create: `src/main/java/org/cy/qywx/util/WxCheckinFetchFailure.java`
- Create: `src/main/java/org/cy/qywx/util/WxCheckinQueryOptions.java`
- Create: `src/main/java/org/cy/qywx/util/WxCheckinRecordResult.java`
- Create: `src/main/java/org/cy/qywx/util/WxCheckinDayDataResult.java`
- Create: `src/main/java/org/cy/qywx/util/WxCheckinMonthDataResult.java`

- [ ] **Step 4.1: Create `WxCheckinFetchFailure.java`**

```java
package org.cy.qywx.util;

import java.util.Date;
import java.util.List;

/**
 * 单次「日期段 × 用户批」查询失败的记录。一个批次失败不影响其他批次合并。
 *
 * @author CY
 * Copyright (c) CY
 *
 * @param segmentStart  失败的日期段开始时间
 * @param segmentEnd    失败的日期段结束时间
 * @param userIdBatch   失败批次涉及的 userId
 * @param attempts      实际重试次数
 * @param errorType     异常类型简称
 * @param errorMessage  异常消息
 */
public record WxCheckinFetchFailure(
        Date segmentStart,
        Date segmentEnd,
        List<String> userIdBatch,
        int attempts,
        String errorType,
        String errorMessage
) {
}
```

- [ ] **Step 4.2: Create `WxCheckinQueryOptions.java`**

```java
package org.cy.qywx.util;

/**
 * 考勤查询参数（线程池由 Spring 注入）。
 *
 * @author CY
 * Copyright (c) CY
 *
 * @param segmentDays         单段最大天数（API 上限 30）
 * @param userBatchSize       单批最大 userId 数（API 上限 100）
 * @param maxRetryAttempts    单批次最大重试次数
 * @param retryBackoffMillis  重试初始退避毫秒
 * @param requestsPerSecond   全局限流每秒请求数（&lt;=0 表示不限流）
 */
public record WxCheckinQueryOptions(
        int segmentDays,
        int userBatchSize,
        int maxRetryAttempts,
        long retryBackoffMillis,
        double requestsPerSecond
) {
    /**
     * 构造默认参数：30 天/段、100 人/批、3 次重试、500ms 退避、不限流。
     *
     * @return 默认 options
     */
    public static WxCheckinQueryOptions defaults() {
        return new WxCheckinQueryOptions(30, 100, 3, 500L, 0D);
    }
}
```

- [ ] **Step 4.3: Create `WxCheckinRecordResult.java`**

```java
package org.cy.qywx.util;

import org.cy.qywx.vo.WxCheckinRecordVO;

import java.util.List;

/**
 * 打卡记录查询结果，包含成功列表与失败批次列表。
 *
 * @author CY
 * Copyright (c) CY
 *
 * @param records   成功获取的打卡记录
 * @param failures  失败的批次
 */
public record WxCheckinRecordResult(
        List<WxCheckinRecordVO> records,
        List<WxCheckinFetchFailure> failures
) {
    /** 返回空结果。 */
    public static WxCheckinRecordResult empty() {
        return new WxCheckinRecordResult(List.of(), List.of());
    }
}
```

- [ ] **Step 4.4: Create `WxCheckinDayDataResult.java`**

```java
package org.cy.qywx.util;

import org.cy.qywx.vo.WxCheckinDayDataVO;

import java.util.List;

/**
 * 打卡日报查询结果，包含成功列表与失败批次列表。
 *
 * @author CY
 * Copyright (c) CY
 *
 * @param dayDataList  成功获取的日报数据
 * @param failures     失败的批次
 */
public record WxCheckinDayDataResult(
        List<WxCheckinDayDataVO> dayDataList,
        List<WxCheckinFetchFailure> failures
) {
    /** 返回空结果。 */
    public static WxCheckinDayDataResult empty() {
        return new WxCheckinDayDataResult(List.of(), List.of());
    }
}
```

- [ ] **Step 4.5: Create `WxCheckinMonthDataResult.java`**

```java
package org.cy.qywx.util;

import org.cy.qywx.vo.WxCheckinMonthDataVO;

import java.util.List;

/**
 * 打卡月报查询结果，包含成功列表与失败批次列表。
 *
 * @author CY
 * Copyright (c) CY
 *
 * @param monthDataList  成功获取的月报数据
 * @param failures       失败的批次
 */
public record WxCheckinMonthDataResult(
        List<WxCheckinMonthDataVO> monthDataList,
        List<WxCheckinFetchFailure> failures
) {
    /** 返回空结果。 */
    public static WxCheckinMonthDataResult empty() {
        return new WxCheckinMonthDataResult(List.of(), List.of());
    }
}
```

- [ ] **Step 4.6: Compile**

```
mvn clean compile
```

Expected: no errors. Composites from Task 3 + records from Task 4 now resolve.

- [ ] **Step 4.7: Commit**

```
git add src/main/java/org/cy/qywx/util/WxCheckinFetchFailure.java \
        src/main/java/org/cy/qywx/util/WxCheckinQueryOptions.java \
        src/main/java/org/cy/qywx/util/WxCheckinRecordResult.java \
        src/main/java/org/cy/qywx/util/WxCheckinDayDataResult.java \
        src/main/java/org/cy/qywx/util/WxCheckinMonthDataResult.java
git commit -m "feat: add records (failure/options/results) for WeCom checkin module"
```

---

## Task 5: WxCheckinConverter

**Files:**
- Create: `src/main/java/org/cy/qywx/util/WxCheckinConverter.java`
- Test: `src/test/java/org/cy/qywx/util/WxCheckinConverterTest.java`

The converter maps WxJava beans (`WxCpCheckinData`, `WxCpCheckinDayData`, `WxCpCheckinMonthData`, `WxCpCropCheckinOption`, `WxCpCheckinSchedule`) to local VOs and provides `explodeExceptions(...)` used by the business-semantic layer.

- [ ] **Step 5.1: Write the converter test**

```java
// src/test/java/org/cy/qywx/util/WxCheckinConverterTest.java
package org.cy.qywx.util;

import me.chanjar.weixin.cp.bean.oa.WxCpCheckinData;
import me.chanjar.weixin.cp.bean.oa.WxCpCheckinDayData;
import org.cy.qywx.vo.WxCheckinDayDataVO;
import org.cy.qywx.vo.WxCheckinExceptionItemVO;
import org.cy.qywx.vo.WxCheckinExceptionVO;
import org.cy.qywx.vo.WxCheckinRecordVO;
import org.cy.qywx.vo.enums.WxCheckinExceptionTypeEnum;
import org.cy.qywx.vo.enums.WxCheckinResultEnum;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WxCheckinConverterTest {

    @Test
    void fromRecord_mapsCoreFields() {
        WxCpCheckinData src = new WxCpCheckinData();
        src.setUserId("u-001");
        src.setGroupName("总部考勤");
        src.setGroupId(10);
        src.setCheckinType("上班打卡");
        src.setCheckinTime(1714800000L);
        src.setExceptionType("时间异常");
        src.setLocationTitle("上海办公室");
        src.setWifiName("Office-5G");
        src.setMediaIds(List.of("m1", "m2"));

        WxCheckinRecordVO vo = WxCheckinConverter.fromRecord(src);

        assertThat(vo.getUserId()).isEqualTo("u-001");
        assertThat(vo.getGroupName()).isEqualTo("总部考勤");
        assertThat(vo.getCheckinType()).isEqualTo("上班打卡");
        assertThat(vo.getCheckinTime().getTime()).isEqualTo(1714800000L * 1000L);
        assertThat(vo.getRawExceptionType()).isEqualTo("时间异常");
        assertThat(vo.getExceptionResult()).isEqualTo(WxCheckinResultEnum.TIME_EXCEPTION);
        assertThat(vo.getMediaIds()).containsExactly("m1", "m2");
    }

    @Test
    void fromRecord_handlesNullCheckinTime() {
        WxCpCheckinData src = new WxCpCheckinData();
        src.setUserId("u-002");

        WxCheckinRecordVO vo = WxCheckinConverter.fromRecord(src);

        assertThat(vo.getCheckinTime()).isNull();
    }

    @Test
    void fromDayData_mapsBaseInfoAndExceptions() {
        WxCpCheckinDayData src = new WxCpCheckinDayData();
        WxCpCheckinDayData.BaseInfo base = new WxCpCheckinDayData.BaseInfo();
        base.setName("张三");
        base.setDepartsName("研发;运维");
        base.setDate((int) (1714800000));
        base.setRecordType(1);
        WxCpCheckinDayData.BaseInfo.Acctlnfo acct = new WxCpCheckinDayData.BaseInfo.Acctlnfo();
        acct.setUserId("u-001");
        base.setAcctInfo(acct);
        src.setBaseInfo(base);

        WxCpCheckinDayData.SummaryInfo summary = new WxCpCheckinDayData.SummaryInfo();
        summary.setStandardWorkSec(28800);
        src.setSummaryInfo(summary);

        WxCpCheckinDayData.ExceptionInfos ex = new WxCpCheckinDayData.ExceptionInfos();
        ex.setException(1);
        ex.setCount(1);
        ex.setDuration(900);
        src.setExceptionInfos(List.of(ex));

        WxCheckinDayDataVO vo = WxCheckinConverter.fromDayData(src);

        assertThat(vo.getUserId()).isEqualTo("u-001");
        assertThat(vo.getName()).isEqualTo("张三");
        assertThat(vo.getDepartments()).containsExactly("研发", "运维");
        assertThat(vo.getRecordType()).isEqualTo(1);
        assertThat(vo.getStandardWorkSeconds()).isEqualTo(28800);
        assertThat(vo.getExceptions()).hasSize(1);
        assertThat(vo.getExceptions().get(0).getExceptionType()).isEqualTo(WxCheckinExceptionTypeEnum.LATE);
        assertThat(vo.getExceptions().get(0).getCount()).isEqualTo(1);
        assertThat(vo.getExceptions().get(0).getDurationSeconds()).isEqualTo(900);
    }

    @Test
    void explodeExceptions_filtersByType() {
        WxCheckinDayDataVO day = new WxCheckinDayDataVO();
        day.setUserId("u-1");
        day.setName("张三");
        day.setDate(new java.util.Date(1714800000L * 1000));
        day.setDepartments(List.of("研发"));

        WxCheckinExceptionVO late = new WxCheckinExceptionVO();
        late.setExceptionType(WxCheckinExceptionTypeEnum.LATE);
        late.setCount(2);
        late.setDurationSeconds(1800);

        WxCheckinExceptionVO absent = new WxCheckinExceptionVO();
        absent.setExceptionType(WxCheckinExceptionTypeEnum.ABSENT);
        absent.setCount(1);
        absent.setDurationSeconds(28800);

        day.setExceptions(List.of(late, absent));

        List<WxCheckinExceptionItemVO> lates = WxCheckinConverter.explodeExceptions(day, WxCheckinExceptionTypeEnum.LATE);
        assertThat(lates).hasSize(1);
        assertThat(lates.get(0).getExceptionType()).isEqualTo(WxCheckinExceptionTypeEnum.LATE);
        assertThat(lates.get(0).getExceptionCount()).isEqualTo(2);
        assertThat(lates.get(0).getExceptionDurationMinutes()).isEqualTo(30L);

        List<WxCheckinExceptionItemVO> all = WxCheckinConverter.explodeExceptions(day, null);
        assertThat(all).hasSize(2);
    }

    @Test
    void explodeExceptions_emptyDayReturnsEmpty() {
        WxCheckinDayDataVO empty = new WxCheckinDayDataVO();
        empty.setUserId("u-1");
        empty.setExceptions(List.of());

        assertThat(WxCheckinConverter.explodeExceptions(empty, null)).isEmpty();
    }
}
```

> The exact nested-class names (e.g., `WxCpCheckinDayData.BaseInfo.Acctlnfo`) come from WxJava 4.8.2 sources. If a name differs in your local jar, adjust the test to match — the converter implementation must use whatever is exposed by the dependency.

- [ ] **Step 5.2: Run test — expect compile failure (no converter yet)**

```
mvn test -Dtest=WxCheckinConverterTest
```

Expected: compilation failure pointing at missing `WxCheckinConverter`.

- [ ] **Step 5.3: Create `WxCheckinConverter.java`**

```java
package org.cy.qywx.util;

import me.chanjar.weixin.cp.bean.oa.WxCpCheckinData;
import me.chanjar.weixin.cp.bean.oa.WxCpCheckinDayData;
import me.chanjar.weixin.cp.bean.oa.WxCpCheckinMonthData;
import me.chanjar.weixin.cp.bean.oa.WxCpCheckinSchedule;
import me.chanjar.weixin.cp.bean.oa.WxCpCropCheckinOption;
import org.cy.qywx.vo.WxAttendanceReportVO;
import org.cy.qywx.vo.WxCheckinApprovalItemVO;
import org.cy.qywx.vo.WxCheckinDayDataVO;
import org.cy.qywx.vo.WxCheckinExceptionItemVO;
import org.cy.qywx.vo.WxCheckinExceptionVO;
import org.cy.qywx.vo.WxCheckinGroupVO;
import org.cy.qywx.vo.WxCheckinLeaveVO;
import org.cy.qywx.vo.WxCheckinMonthDataVO;
import org.cy.qywx.vo.WxCheckinOvertimeVO;
import org.cy.qywx.vo.WxCheckinRecordVO;
import org.cy.qywx.vo.WxCheckinScheduleListItemVO;
import org.cy.qywx.vo.WxCheckinScheduleVO;
import org.cy.qywx.vo.enums.WxCheckinExceptionTypeEnum;
import org.cy.qywx.vo.enums.WxCheckinResultEnum;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * WxJava 考勤 Bean 到本地 VO 的静态转换器。所有方法对 null 入参返回 {@code null}（除显式说明外）。
 *
 * @author CY
 * Copyright (c) CY
 */
public final class WxCheckinConverter {

    private WxCheckinConverter() {
    }

    /**
     * 将单条原始打卡 bean 转为 {@link WxCheckinRecordVO}。
     *
     * @param src 原始 bean
     * @return VO；入参为 null 时返回 null
     */
    public static WxCheckinRecordVO fromRecord(WxCpCheckinData src) {
        if (src == null) {
            return null;
        }
        WxCheckinRecordVO vo = new WxCheckinRecordVO();
        vo.setUserId(src.getUserId());
        vo.setGroupId(src.getGroupId());
        vo.setGroupName(src.getGroupName());
        vo.setScheduleId(src.getScheduleId());
        vo.setTimelineId(src.getTimelineId());
        vo.setCheckinType(src.getCheckinType());
        vo.setCheckinTime(unixToDate(src.getCheckinTime()));
        vo.setSchCheckinTime(unixToDate(src.getSchCheckinTime()));
        vo.setLocationTitle(src.getLocationTitle());
        vo.setLocationDetail(src.getLocationDetail());
        vo.setWifiName(src.getWifiName());
        vo.setWifiMac(src.getWifiMac());
        vo.setNotes(src.getNotes());
        vo.setMediaIds(src.getMediaIds());
        vo.setDeviceId(src.getDeviceId());
        vo.setLat(src.getLat());
        vo.setLng(src.getLng());
        vo.setRawExceptionType(src.getExceptionType());
        vo.setExceptionResult(WxCheckinResultEnum.fromText(src.getExceptionType()));
        return vo;
    }

    /**
     * 将日报 bean 转为 {@link WxCheckinDayDataVO}。
     *
     * @param src 日报 bean
     * @return VO；入参为 null 时返回 null
     */
    public static WxCheckinDayDataVO fromDayData(WxCpCheckinDayData src) {
        if (src == null) {
            return null;
        }
        WxCheckinDayDataVO vo = new WxCheckinDayDataVO();

        WxCpCheckinDayData.BaseInfo base = src.getBaseInfo();
        if (base != null) {
            vo.setName(base.getName());
            vo.setDepartments(splitSemicolon(base.getDepartsName()));
            vo.setDate(unixIntToDate(base.getDate()));
            vo.setRecordType(base.getRecordType());
            if (base.getAcctInfo() != null) {
                vo.setUserId(base.getAcctInfo().getUserId());
            }
        }

        WxCpCheckinDayData.SummaryInfo summary = src.getSummaryInfo();
        if (summary != null) {
            vo.setStandardWorkSeconds(summary.getStandardWorkSec());
            vo.setFlexWorkSeconds(summary.getFlexWorkSec());
            vo.setRegularWorkSeconds(summary.getRegularWorkSec());
            vo.setEarliestCheckinTime(unixToDate(summary.getEarliestTime()));
            vo.setLatestCheckinTime(unixToDate(summary.getLastestTime()));
        }

        vo.setExceptions(toExceptionList(src.getExceptionInfos()));
        vo.setHolidays(toLeaveList(src.getHolidayInfos()));
        vo.setOvertime(toOvertime(src.getOtInfo()));
        vo.setApprovals(toApprovalList(src.getSpItems()));
        return vo;
    }

    /**
     * 将月报 bean 转为 {@link WxCheckinMonthDataVO}。
     *
     * @param src 月报 bean
     * @return VO；入参为 null 时返回 null
     */
    public static WxCheckinMonthDataVO fromMonthData(WxCpCheckinMonthData src) {
        if (src == null) {
            return null;
        }
        WxCheckinMonthDataVO vo = new WxCheckinMonthDataVO();

        WxCpCheckinMonthData.BaseInfo base = src.getBaseInfo();
        if (base != null) {
            vo.setName(base.getName());
            vo.setDepartments(splitSemicolon(base.getDepartsName()));
            vo.setMonthDate(unixIntToDate(base.getDate()));
            if (base.getAcctInfo() != null) {
                vo.setUserId(base.getAcctInfo().getUserId());
            }
        }

        WxCpCheckinMonthData.SummaryInfo summary = src.getSummaryInfo();
        if (summary != null) {
            vo.setWorkDays(summary.getWorkDays());
            vo.setExcludedDays(summary.getExcludedDays());
            vo.setWorkSeconds(summary.getRegularWorkSec());
            vo.setStandardWorkSeconds(summary.getStandardWorkSec());
        }

        vo.setExceptions(toExceptionList(src.getExceptionInfos()));
        vo.setHolidays(toLeaveList(src.getHolidayInfos()));
        vo.setOvertime(toOvertime(src.getOtInfo()));
        vo.setApprovals(toApprovalList(src.getSpItems()));
        return vo;
    }

    /**
     * 将考勤组 bean 转为 {@link WxCheckinGroupVO}。
     *
     * @param src 考勤组 bean
     * @return VO；入参为 null 时返回 null
     */
    public static WxCheckinGroupVO fromGroup(WxCpCropCheckinOption src) {
        if (src == null) {
            return null;
        }
        WxCheckinGroupVO vo = new WxCheckinGroupVO();
        vo.setGroupId(src.getGroupId());
        vo.setGroupName(src.getGroupName());
        vo.setGroupType(src.getGrouptype() == null ? null : String.valueOf(src.getGrouptype()));
        vo.setCreateTime(unixToDate(src.getCreateTime()));
        vo.setNeedPhoto(src.getNeedPhoto());
        vo.setNoteCanUseLocalPic(src.getNoteCanUseLocalPic());
        vo.setAllowCheckinOffWorkday(src.getAllowCheckinOffWorkday());
        vo.setAllowApplyOffWorkday(src.getAllowApplyOffWorkday());
        vo.setSyncHolidays(src.getSyncHolidays());
        vo.setWhiteUsers(src.getWhiteUsers());
        // schedules / range / spe-workdays / spe-offdays 等字段视 WxJava 实际暴露程度填充；
        // 若 WxCpCheckinGroupBase 暴露了 getSchedulelist()，调用之并 stream→fromSchedule 转换
        return vo;
    }

    /**
     * 将排班 bean 转为 {@link WxCheckinScheduleListItemVO}。
     *
     * @param src 排班 bean
     * @return VO；入参为 null 时返回 null
     */
    public static WxCheckinScheduleListItemVO fromScheduleItem(WxCpCheckinSchedule src) {
        if (src == null) {
            return null;
        }
        WxCheckinScheduleListItemVO vo = new WxCheckinScheduleListItemVO();
        vo.setUserId(src.getUserId());
        vo.setScheduleDate(unixToDate(src.getYearMonthDay() == null ? null : Long.valueOf(src.getYearMonthDay())));
        if (src.getSchedule() != null) {
            vo.setGroupId(src.getSchedule().getGroupid());
            vo.setScheduleId(src.getSchedule().getScheduleid());
            vo.setScheduleName(src.getSchedule().getScheduleName());
        }
        return vo;
    }

    /**
     * 将日报 VO 中嵌入的异常项展开为业务语义层的扁平异常行。
     *
     * @param dayData 日报 VO
     * @param filter  仅保留指定异常类型；传 null 表示全部保留
     * @return 扁平异常行列表，可能为空
     */
    public static List<WxCheckinExceptionItemVO> explodeExceptions(WxCheckinDayDataVO dayData,
                                                                   WxCheckinExceptionTypeEnum filter) {
        if (dayData == null || dayData.getExceptions() == null || dayData.getExceptions().isEmpty()) {
            return Collections.emptyList();
        }
        List<WxCheckinExceptionItemVO> out = new ArrayList<>();
        for (WxCheckinExceptionVO ex : dayData.getExceptions()) {
            if (ex == null || ex.getExceptionType() == null) {
                continue;
            }
            if (filter != null && filter != ex.getExceptionType()) {
                continue;
            }
            WxCheckinExceptionItemVO item = new WxCheckinExceptionItemVO();
            item.setUserId(dayData.getUserId());
            item.setName(dayData.getName());
            item.setDepartments(dayData.getDepartments());
            item.setDate(dayData.getDate());
            item.setExceptionType(ex.getExceptionType());
            item.setExceptionCount(ex.getCount());
            item.setExceptionDurationMinutes(ex.getDurationSeconds() == null ? null
                    : (long) (ex.getDurationSeconds() / 60));
            out.add(item);
        }
        return out;
    }

    private static List<WxCheckinExceptionVO> toExceptionList(List<WxCpCheckinDayData.ExceptionInfos> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        List<WxCheckinExceptionVO> out = new ArrayList<>(source.size());
        for (WxCpCheckinDayData.ExceptionInfos info : source) {
            if (info == null) {
                continue;
            }
            WxCheckinExceptionVO vo = new WxCheckinExceptionVO();
            vo.setExceptionType(WxCheckinExceptionTypeEnum.fromCode(info.getException()));
            vo.setCount(info.getCount());
            vo.setDurationSeconds(info.getDuration());
            out.add(vo);
        }
        return out;
    }

    private static List<WxCheckinLeaveVO> toLeaveList(List<WxCpCheckinDayData.HolidayInfos> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        List<WxCheckinLeaveVO> out = new ArrayList<>(source.size());
        for (WxCpCheckinDayData.HolidayInfos h : source) {
            if (h == null) {
                continue;
            }
            WxCheckinLeaveVO vo = new WxCheckinLeaveVO();
            vo.setSpNumber(h.getSpNumber());
            vo.setSpTitle(h.getSpTitle() == null ? null : h.getSpTitle().getData() == null ? null
                    : h.getSpTitle().getData().stream().findFirst().map(d -> d.getText()).orElse(null));
            vo.setDurationSeconds(h.getSpDuration());
            vo.setStartTime(h.getStartTime());
            vo.setEndTime(h.getEndTime());
            out.add(vo);
        }
        return out;
    }

    private static WxCheckinOvertimeVO toOvertime(WxCpCheckinDayData.OtInfo source) {
        if (source == null) {
            return null;
        }
        WxCheckinOvertimeVO vo = new WxCheckinOvertimeVO();
        vo.setOtStatus(source.getOtStatus());
        vo.setOtDurationSeconds(source.getOtDuration());
        vo.setExceptionDurationSeconds(source.getExceptionDuration());
        return vo;
    }

    private static List<WxCheckinApprovalItemVO> toApprovalList(List<WxCpCheckinDayData.SpItems> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        List<WxCheckinApprovalItemVO> out = new ArrayList<>(source.size());
        for (WxCpCheckinDayData.SpItems s : source) {
            if (s == null) {
                continue;
            }
            WxCheckinApprovalItemVO vo = new WxCheckinApprovalItemVO();
            vo.setSpNumber(s.getSpNumber());
            vo.setType(s.getType());
            vo.setSpTitle(s.getName());
            vo.setDurationSeconds(s.getDuration());
            vo.setTimeUnix(s.getTime());
            out.add(vo);
        }
        return out;
    }

    private static List<WxCheckinExceptionVO> toExceptionListMonth(
            List<WxCpCheckinMonthData.ExceptionInfos> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        List<WxCheckinExceptionVO> out = new ArrayList<>(source.size());
        for (WxCpCheckinMonthData.ExceptionInfos info : source) {
            if (info == null) {
                continue;
            }
            WxCheckinExceptionVO vo = new WxCheckinExceptionVO();
            vo.setExceptionType(WxCheckinExceptionTypeEnum.fromCode(info.getException()));
            vo.setCount(info.getCount());
            vo.setDurationSeconds(info.getDuration());
            out.add(vo);
        }
        return out;
    }

    private static List<String> splitSemicolon(String value) {
        if (value == null || value.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(value.split("[;；]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private static Date unixToDate(Long seconds) {
        if (seconds == null || seconds <= 0L) {
            return null;
        }
        return new Date(seconds * 1000L);
    }

    private static Date unixIntToDate(Integer seconds) {
        if (seconds == null || seconds <= 0) {
            return null;
        }
        return new Date(((long) seconds) * 1000L);
    }
}
```

> **Important**: WxJava's accessor names sometimes differ from raw JSON field names (`getDepartsName` / `getOtInfo` etc). If the implementer hits a missing getter, look at the actual `WxCpCheckinDayData.java` from `weixin-java-cp-4.8.2-sources.jar` — the converter's job is to bridge whatever WxJava exposes. The list above (`HolidayInfos`, `OtInfo`, `SpItems`, `ExceptionInfos`) was confirmed against 4.8.2 sources. If the month-data inner class names differ for `ExceptionInfos`/`SpItems`, switch the helper to use the appropriate WxJava class — keep the VO surface unchanged.

- [ ] **Step 5.4: Run converter test — should pass**

```
mvn test -Dtest=WxCheckinConverterTest
```

If a WxJava nested-class-name mismatch causes a compile error, fix it in the converter (NOT in the VO) by following the actual WxJava signature, and re-run.

- [ ] **Step 5.5: Commit**

```
git add src/main/java/org/cy/qywx/util/WxCheckinConverter.java \
        src/test/java/org/cy/qywx/util/WxCheckinConverterTest.java
git commit -m "feat: add WxCheckinConverter and tests"
```

---

## Task 6: WxCheckinQueryUtil — skeleton + getCheckinGroups

**Files:**
- Create: `src/main/java/org/cy/qywx/util/WxCheckinQueryUtil.java`
- Test: `src/test/java/org/cy/qywx/util/WxCheckinQueryUtilTest.java`

The skeleton holds the constructor, options field, executor field, internal `SimpleRateLimiter`, and the simplest public method `getCheckinGroups()` (no batching). Subsequent tasks add methods to this class.

- [ ] **Step 6.1: Write the skeleton + getCheckinGroups test**

```java
// src/test/java/org/cy/qywx/util/WxCheckinQueryUtilTest.java
package org.cy.qywx.util;

import me.chanjar.weixin.cp.api.WxCpOaService;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.oa.WxCpCropCheckinOption;
import org.cy.qywx.vo.WxCheckinGroupVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class WxCheckinQueryUtilTest {

    private WxCpService cpService;
    private WxCpOaService oaService;
    private WxCheckinQueryUtil util;

    @BeforeEach
    void setUp() {
        cpService = Mockito.mock(WxCpService.class);
        oaService = Mockito.mock(WxCpOaService.class);
        when(cpService.getOaService()).thenReturn(oaService);
        util = new WxCheckinQueryUtil(
                cpService,
                Executors.newFixedThreadPool(2),
                WxCheckinQueryOptions.defaults()
        );
    }

    @Test
    void getCheckinGroups_returnsConvertedList() throws Exception {
        WxCpCropCheckinOption option = new WxCpCropCheckinOption();
        option.setGroupId(7);
        option.setGroupName("总部");
        when(oaService.getCropCheckinOption()).thenReturn(List.of(option));

        List<WxCheckinGroupVO> result = util.getCheckinGroups();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getGroupId()).isEqualTo(7);
        assertThat(result.get(0).getGroupName()).isEqualTo("总部");
    }

    @Test
    void getCheckinGroups_emptyResponseReturnsEmpty() throws Exception {
        when(oaService.getCropCheckinOption()).thenReturn(List.of());

        assertThat(util.getCheckinGroups()).isEmpty();
    }
}
```

- [ ] **Step 6.2: Run test, expect compile failure**

```
mvn test -Dtest=WxCheckinQueryUtilTest
```

- [ ] **Step 6.3: Create `WxCheckinQueryUtil.java`**

```java
package org.cy.qywx.util;

import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.bean.oa.WxCpCropCheckinOption;
import org.cy.qywx.vo.WxCheckinGroupVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.LockSupport;

/**
 * 企业微信打卡查询工具，覆盖考勤组、原始打卡记录、日报、月报、排班，
 * 并提供「迟到 / 早退 / 缺卡 / 旷工 / 地点异常 / 设备异常」业务语义查询及一站式 {@link org.cy.qywx.vo.WxAttendanceReportVO} 报表。
 *
 * <p>该工具复用主 {@link WxCpService}（与 {@code WxApprovalQueryUtil} 一致），
 * 通过 {@code wxCpService.getOaService()} 调用 WxJava 暴露的考勤接口；
 * 内部对企业微信「单次 ≤100 userId、≤30 天」硬限制做透明分批 / 分段。</p>
 *
 * @author CY
 * Copyright (c) CY
 */
public class WxCheckinQueryUtil {

    private static final Logger log = LoggerFactory.getLogger(WxCheckinQueryUtil.class);

    /** 打卡类型常量：上下班打卡。 */
    public static final int CHECKIN_TYPE_NORMAL = 1;
    /** 打卡类型常量：外出打卡。 */
    public static final int CHECKIN_TYPE_OUTSIDE = 2;
    /** 打卡类型常量：全部。 */
    public static final int CHECKIN_TYPE_ALL = 3;

    private final WxCpService wxCpService;
    private final Executor executor;
    private final WxCheckinQueryOptions options;

    /**
     * 构造方法。
     *
     * @param wxCpService 主 WxCpService
     * @param executor    异步执行器（由 Spring 注入 qywxCheckinExecutor）
     * @param options     查询参数
     */
    public WxCheckinQueryUtil(WxCpService wxCpService, Executor executor, WxCheckinQueryOptions options) {
        this.wxCpService = wxCpService;
        this.executor = executor == null ? Runnable::run : executor;
        this.options = options == null ? WxCheckinQueryOptions.defaults() : options;
        log.info("WxCheckinQueryUtil initialized: segmentDays={}, userBatchSize={}, maxRetryAttempts={}, retryBackoffMillis={}, requestsPerSecond={}",
                this.options.segmentDays(), this.options.userBatchSize(), this.options.maxRetryAttempts(),
                this.options.retryBackoffMillis(), this.options.requestsPerSecond());
    }

    /**
     * 拉取企业全部考勤组配置。
     *
     * @return 考勤组列表，可能为空
     * @throws WxErrorException 调用企业微信失败
     */
    public List<WxCheckinGroupVO> getCheckinGroups() throws WxErrorException {
        List<WxCpCropCheckinOption> source = wxCpService.getOaService().getCropCheckinOption();
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        List<WxCheckinGroupVO> out = new ArrayList<>(source.size());
        for (WxCpCropCheckinOption o : source) {
            WxCheckinGroupVO vo = WxCheckinConverter.fromGroup(o);
            if (vo != null) {
                out.add(vo);
            }
        }
        return out;
    }

    // -------------------- 内部工具 --------------------

    /**
     * 简易令牌桶限流器（与 WxApprovalQueryUtil / WxHrRosterQueryUtil 保持一致的内嵌实现）。
     */
    static final class SimpleRateLimiter {
        private final long intervalNanos;
        private long nextAllowedNanos;

        SimpleRateLimiter(double requestsPerSecond) {
            this.intervalNanos = requestsPerSecond <= 0D ? 0L : (long) (1_000_000_000D / requestsPerSecond);
            this.nextAllowedNanos = System.nanoTime();
        }

        synchronized void acquire() {
            if (intervalNanos <= 0L) {
                return;
            }
            long now = System.nanoTime();
            if (now < nextAllowedNanos) {
                LockSupport.parkNanos(nextAllowedNanos - now);
                now = System.nanoTime();
            }
            nextAllowedNanos = Math.max(nextAllowedNanos, now) + intervalNanos;
        }
    }
}
```

- [ ] **Step 6.4: Run test — should pass**

```
mvn test -Dtest=WxCheckinQueryUtilTest
```

- [ ] **Step 6.5: Commit**

```
git add src/main/java/org/cy/qywx/util/WxCheckinQueryUtil.java \
        src/test/java/org/cy/qywx/util/WxCheckinQueryUtilTest.java
git commit -m "feat: add WxCheckinQueryUtil skeleton with getCheckinGroups"
```

---

## Task 7: getCheckinDayData — first batched method (introduces the 2-D fan-out engine)

**Files:**
- Modify: `src/main/java/org/cy/qywx/util/WxCheckinQueryUtil.java`
- Modify: `src/test/java/org/cy/qywx/util/WxCheckinQueryUtilTest.java`

This task introduces the core internal helpers: `partitionUsers`, `segmentDates`, `runFanout`, `withRetry`. All later batched methods (Tasks 8–10) reuse these helpers.

- [ ] **Step 7.1: Add tests for `getCheckinDayData`**

Append to `WxCheckinQueryUtilTest`:

```java
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import me.chanjar.weixin.cp.bean.oa.WxCpCheckinDayData;
import org.cy.qywx.vo.WxCheckinDayDataVO;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;

@Test
void getCheckinDayData_segmentsAndBatches() throws Exception {
    // 90 days × 250 users → 3 segments × 3 batches = 9 calls
    AtomicInteger callCount = new AtomicInteger();
    when(oaService.getCheckinDayData(any(), any(), anyList())).thenAnswer(inv -> {
        callCount.incrementAndGet();
        WxCpCheckinDayData d = new WxCpCheckinDayData();
        WxCpCheckinDayData.BaseInfo base = new WxCpCheckinDayData.BaseInfo();
        base.setName("u");
        d.setBaseInfo(base);
        return List.of(d);
    });

    Date start = new Date(1714521600000L);                  // 2024-05-01
    Date end = new Date(start.getTime() + 90L * 86_400_000); // +90 天
    List<String> userIds = new java.util.ArrayList<>();
    for (int i = 0; i < 250; i++) {
        userIds.add("u" + i);
    }

    WxCheckinDayDataResult result = util.getCheckinDayData(start, end, userIds);

    assertThat(callCount.get()).isEqualTo(9);
    assertThat(result.dayDataList()).hasSize(9);
    assertThat(result.failures()).isEmpty();
}

@Test
void getCheckinDayData_collectsFailuresAfterRetry() throws Exception {
    when(oaService.getCheckinDayData(any(), any(), anyList()))
            .thenThrow(new RuntimeException("boom"));

    Date start = new Date(1714521600000L);
    Date end = new Date(start.getTime() + 86_400_000);
    List<String> userIds = List.of("u1");

    WxCheckinDayDataResult result = util.getCheckinDayData(start, end, userIds);

    assertThat(result.dayDataList()).isEmpty();
    assertThat(result.failures()).hasSize(1);
    assertThat(result.failures().get(0).errorMessage()).contains("boom");
    assertThat(result.failures().get(0).attempts()).isEqualTo(3);
}

@Test
void getCheckinDayData_emptyUsersShortCircuit() throws Exception {
    Date start = new Date(1714521600000L);
    Date end = new Date(start.getTime() + 86_400_000);

    WxCheckinDayDataResult result = util.getCheckinDayData(start, end, List.of());

    assertThat(result.dayDataList()).isEmpty();
    assertThat(result.failures()).isEmpty();
    Mockito.verifyNoInteractions(oaService);
}
```

- [ ] **Step 7.2: Run tests — expect compile failure**

```
mvn test -Dtest=WxCheckinQueryUtilTest
```

- [ ] **Step 7.3: Add the fan-out engine + getCheckinDayData**

Append to `WxCheckinQueryUtil` (inside the class, before the trailing `SimpleRateLimiter`):

```java
// -------------------- getCheckinDayData --------------------

/**
 * 拉取指定 userId 在指定时间范围内的打卡日报。
 *
 * @param start   开始时间
 * @param end     结束时间
 * @param userIds 用户 ID 集合（去重后，{@code <=100 人/批} 由内部分批）
 * @return 日报查询结果（含成功 + 失败批次）
 */
public WxCheckinDayDataResult getCheckinDayData(Date start, Date end, java.util.Collection<String> userIds) {
    java.util.List<String> normalized = normaliseUserIds(userIds);
    if (normalized.isEmpty()) {
        return WxCheckinDayDataResult.empty();
    }
    validateRange(start, end);

    long t0 = System.currentTimeMillis();
    java.util.List<WxDateRange> segments = segmentDates(start, end, options.segmentDays());
    java.util.List<java.util.List<String>> batches = partitionUsers(normalized, options.userBatchSize());
    java.util.List<FanoutTask<java.util.List<WxCpCheckinDayData>>> tasks = new java.util.ArrayList<>();
    for (WxDateRange seg : segments) {
        for (java.util.List<String> batch : batches) {
            tasks.add(new FanoutTask<>(seg, batch,
                    () -> wxCpService.getOaService().getCheckinDayData(seg.startTime(), seg.endTime(), batch)));
        }
    }

    SimpleRateLimiter limiter = new SimpleRateLimiter(options.requestsPerSecond());
    java.util.List<java.util.concurrent.CompletableFuture<FanoutOutcome<java.util.List<WxCpCheckinDayData>>>> futures =
            new java.util.ArrayList<>(tasks.size());
    for (FanoutTask<java.util.List<WxCpCheckinDayData>> task : tasks) {
        futures.add(java.util.concurrent.CompletableFuture.supplyAsync(() -> withRetry(task, limiter), executor));
    }
    java.util.concurrent.CompletableFuture
            .allOf(futures.toArray(new java.util.concurrent.CompletableFuture<?>[0])).join();

    java.util.List<WxCheckinDayDataVO> success = new java.util.ArrayList<>();
    java.util.List<WxCheckinFetchFailure> failures = new java.util.ArrayList<>();
    for (java.util.concurrent.CompletableFuture<FanoutOutcome<java.util.List<WxCpCheckinDayData>>> f : futures) {
        FanoutOutcome<java.util.List<WxCpCheckinDayData>> o = f.join();
        if (o.failure() != null) {
            failures.add(o.failure());
            continue;
        }
        if (o.success() != null) {
            for (WxCpCheckinDayData d : o.success()) {
                WxCheckinDayDataVO vo = WxCheckinConverter.fromDayData(d);
                if (vo != null) {
                    success.add(vo);
                }
            }
        }
    }

    log.info("Checkin day data query: tasks={}, success={}, failures={}, durationMs={}",
            tasks.size(), success.size(), failures.size(), System.currentTimeMillis() - t0);
    return new WxCheckinDayDataResult(java.util.List.copyOf(success), java.util.List.copyOf(failures));
}

/** 对 {@link WxDateRange} 重载。 */
public WxCheckinDayDataResult getCheckinDayData(WxDateRange range, java.util.Collection<String> userIds) {
    return getCheckinDayData(range.startTime(), range.endTime(), userIds);
}

// -------------------- fan-out 引擎 --------------------

private static java.util.List<String> normaliseUserIds(java.util.Collection<String> in) {
    if (in == null || in.isEmpty()) {
        return java.util.List.of();
    }
    return in.stream()
            .filter(java.util.Objects::nonNull)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .distinct()
            .toList();
}

private static void validateRange(Date start, Date end) {
    if (start == null || end == null) {
        throw new IllegalArgumentException("start and end must not be null");
    }
    if (start.after(end)) {
        throw new IllegalArgumentException("start must not be after end");
    }
}

private static java.util.List<WxDateRange> segmentDates(Date start, Date end, int days) {
    int safeDays = Math.max(1, days);
    long stepMillis = safeDays * 86_400_000L;
    java.util.List<WxDateRange> out = new java.util.ArrayList<>();
    Date cursor = start;
    while (!cursor.after(end)) {
        Date segEnd = new Date(Math.min(cursor.getTime() + stepMillis, end.getTime()));
        out.add(new WxDateRange(cursor, segEnd));
        if (segEnd.equals(end)) {
            break;
        }
        cursor = new Date(segEnd.getTime() + 1000L);
    }
    return out;
}

private static java.util.List<java.util.List<String>> partitionUsers(java.util.List<String> userIds, int batchSize) {
    int safe = Math.max(1, batchSize);
    java.util.List<java.util.List<String>> out = new java.util.ArrayList<>();
    for (int i = 0; i < userIds.size(); i += safe) {
        out.add(java.util.List.copyOf(userIds.subList(i, Math.min(i + safe, userIds.size()))));
    }
    return out;
}

private <T> FanoutOutcome<T> withRetry(FanoutTask<T> task, SimpleRateLimiter limiter) {
    int max = Math.max(1, options.maxRetryAttempts());
    Throwable last = null;
    for (int attempt = 1; attempt <= max; attempt++) {
        try {
            limiter.acquire();
            T result = task.callable().call();
            return new FanoutOutcome<>(result, null);
        } catch (Throwable t) {
            last = unwrap(t);
            log.warn("Checkin batch failed: range=[{},{}], users={}, attempt={}/{}, error={}",
                    task.range().startTime(), task.range().endTime(), task.userIds().size(),
                    attempt, max, last.getMessage());
            if (attempt < max) {
                long backoff = exponentialBackoff(attempt);
                if (backoff > 0L) {
                    LockSupport.parkNanos(java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(backoff));
                }
            }
        }
    }
    String type = last == null ? "UNKNOWN" : last.getClass().getSimpleName();
    String msg = last == null ? "unknown error" : String.valueOf(last.getMessage());
    return new FanoutOutcome<>(null, new WxCheckinFetchFailure(
            task.range().startTime(), task.range().endTime(), task.userIds(), max, type, msg));
}

private long exponentialBackoff(int attempt) {
    long base = Math.max(0L, options.retryBackoffMillis());
    if (base == 0L) {
        return 0L;
    }
    long backoff = base * (1L << (attempt - 1));
    return Math.min(backoff, base * 32);
}

private static Throwable unwrap(Throwable t) {
    Throwable c = t;
    while (c.getCause() != null && c != c.getCause()) {
        c = c.getCause();
    }
    return c;
}

private record FanoutTask<T>(WxDateRange range, java.util.List<String> userIds, java.util.concurrent.Callable<T> callable) {
}

private record FanoutOutcome<T>(T success, WxCheckinFetchFailure failure) {
}
```

> The deeply-qualified `java.util.*` references avoid clashing with new imports; tidy them in a single import-organisation pass once all batched methods are added in Task 8–10.

- [ ] **Step 7.4: Run tests — should pass**

```
mvn test -Dtest=WxCheckinQueryUtilTest
```

If retry timing makes the failure-collection test slow (3 retries × 500ms backoff), keep it — it validates that the backoff actually fires. If you want it fast, override options in `setUp()` with `new WxCheckinQueryOptions(30, 100, 3, 1L, 0D)`.

- [ ] **Step 7.5: Commit**

```
git add src/main/java/org/cy/qywx/util/WxCheckinQueryUtil.java \
        src/test/java/org/cy/qywx/util/WxCheckinQueryUtilTest.java
git commit -m "feat: add getCheckinDayData with 2D batching engine"
```

---

## Task 8: getCheckinRecords (raw checkin data)

**Files:**
- Modify: `src/main/java/org/cy/qywx/util/WxCheckinQueryUtil.java`
- Modify: `src/test/java/org/cy/qywx/util/WxCheckinQueryUtilTest.java`

- [ ] **Step 8.1: Add test**

Append to `WxCheckinQueryUtilTest`:

```java
import me.chanjar.weixin.cp.bean.oa.WxCpCheckinData;

@Test
void getCheckinRecords_appliesType3WhenOmitted() throws Exception {
    WxCpCheckinData d = new WxCpCheckinData();
    d.setUserId("u1");
    d.setCheckinTime(1714521600L);
    when(oaService.getCheckinData(eq(3), any(), any(), anyList())).thenReturn(List.of(d));

    Date start = new Date(1714521600000L);
    Date end = new Date(start.getTime() + 86_400_000);

    WxCheckinRecordResult result = util.getCheckinRecords(start, end, List.of("u1"));

    assertThat(result.records()).hasSize(1);
    assertThat(result.records().get(0).getUserId()).isEqualTo("u1");
}
```

Add `import static org.mockito.ArgumentMatchers.eq;` if not present.

- [ ] **Step 8.2: Run — expect compile failure**

```
mvn test -Dtest=WxCheckinQueryUtilTest#getCheckinRecords_appliesType3WhenOmitted
```

- [ ] **Step 8.3: Add `getCheckinRecords` to `WxCheckinQueryUtil`**

Append after the `getCheckinDayData` block:

```java
// -------------------- getCheckinRecords --------------------

/**
 * 拉取原始打卡记录。
 *
 * @param type    打卡类型（{@link #CHECKIN_TYPE_NORMAL} / {@link #CHECKIN_TYPE_OUTSIDE} / {@link #CHECKIN_TYPE_ALL}）
 * @param start   开始时间
 * @param end     结束时间
 * @param userIds 用户 ID 集合
 * @return 打卡记录查询结果
 */
public WxCheckinRecordResult getCheckinRecords(int type, Date start, Date end, java.util.Collection<String> userIds) {
    java.util.List<String> normalized = normaliseUserIds(userIds);
    if (normalized.isEmpty()) {
        return WxCheckinRecordResult.empty();
    }
    validateRange(start, end);

    java.util.List<WxDateRange> segments = segmentDates(start, end, options.segmentDays());
    java.util.List<java.util.List<String>> batches = partitionUsers(normalized, options.userBatchSize());
    java.util.List<FanoutTask<java.util.List<me.chanjar.weixin.cp.bean.oa.WxCpCheckinData>>> tasks = new java.util.ArrayList<>();
    for (WxDateRange seg : segments) {
        for (java.util.List<String> batch : batches) {
            tasks.add(new FanoutTask<>(seg, batch,
                    () -> wxCpService.getOaService().getCheckinData(type, seg.startTime(), seg.endTime(), batch)));
        }
    }

    SimpleRateLimiter limiter = new SimpleRateLimiter(options.requestsPerSecond());
    java.util.List<java.util.concurrent.CompletableFuture<FanoutOutcome<java.util.List<me.chanjar.weixin.cp.bean.oa.WxCpCheckinData>>>> futures =
            new java.util.ArrayList<>(tasks.size());
    for (FanoutTask<java.util.List<me.chanjar.weixin.cp.bean.oa.WxCpCheckinData>> task : tasks) {
        futures.add(java.util.concurrent.CompletableFuture.supplyAsync(() -> withRetry(task, limiter), executor));
    }
    java.util.concurrent.CompletableFuture
            .allOf(futures.toArray(new java.util.concurrent.CompletableFuture<?>[0])).join();

    java.util.List<org.cy.qywx.vo.WxCheckinRecordVO> success = new java.util.ArrayList<>();
    java.util.List<WxCheckinFetchFailure> failures = new java.util.ArrayList<>();
    for (java.util.concurrent.CompletableFuture<FanoutOutcome<java.util.List<me.chanjar.weixin.cp.bean.oa.WxCpCheckinData>>> f : futures) {
        FanoutOutcome<java.util.List<me.chanjar.weixin.cp.bean.oa.WxCpCheckinData>> o = f.join();
        if (o.failure() != null) {
            failures.add(o.failure());
            continue;
        }
        if (o.success() != null) {
            for (me.chanjar.weixin.cp.bean.oa.WxCpCheckinData d : o.success()) {
                org.cy.qywx.vo.WxCheckinRecordVO vo = WxCheckinConverter.fromRecord(d);
                if (vo != null) {
                    success.add(vo);
                }
            }
        }
    }
    return new WxCheckinRecordResult(java.util.List.copyOf(success), java.util.List.copyOf(failures));
}

/** {@link #CHECKIN_TYPE_ALL} 默认重载。 */
public WxCheckinRecordResult getCheckinRecords(Date start, Date end, java.util.Collection<String> userIds) {
    return getCheckinRecords(CHECKIN_TYPE_ALL, start, end, userIds);
}

/** {@link WxDateRange} 重载，type=ALL。 */
public WxCheckinRecordResult getCheckinRecords(WxDateRange range, java.util.Collection<String> userIds) {
    return getCheckinRecords(CHECKIN_TYPE_ALL, range.startTime(), range.endTime(), userIds);
}

/** {@link WxDateRange} + 指定 type 重载。 */
public WxCheckinRecordResult getCheckinRecords(int type, WxDateRange range, java.util.Collection<String> userIds) {
    return getCheckinRecords(type, range.startTime(), range.endTime(), userIds);
}
```

- [ ] **Step 8.4: Run — pass**

```
mvn test -Dtest=WxCheckinQueryUtilTest
```

- [ ] **Step 8.5: Commit**

```
git add src/main/java/org/cy/qywx/util/WxCheckinQueryUtil.java \
        src/test/java/org/cy/qywx/util/WxCheckinQueryUtilTest.java
git commit -m "feat: add getCheckinRecords with type overloads"
```

---

## Task 9: getCheckinMonthData

**Files:**
- Modify: `src/main/java/org/cy/qywx/util/WxCheckinQueryUtil.java`
- Modify: `src/test/java/org/cy/qywx/util/WxCheckinQueryUtilTest.java`

WeCom's `getcheckin_monthdata` accepts a date range but each call covers one natural month. We treat segmentation the same way as day-data; the WeCom server collapses to month boundaries.

- [ ] **Step 9.1: Add test**

Append:

```java
import me.chanjar.weixin.cp.bean.oa.WxCpCheckinMonthData;

@Test
void getCheckinMonthData_returnsConvertedList() throws Exception {
    WxCpCheckinMonthData m = new WxCpCheckinMonthData();
    WxCpCheckinMonthData.BaseInfo base = new WxCpCheckinMonthData.BaseInfo();
    base.setName("张三");
    m.setBaseInfo(base);
    when(oaService.getCheckinMonthData(any(), any(), anyList())).thenReturn(List.of(m));

    Date start = new Date(1714521600000L);
    Date end = new Date(start.getTime() + 28L * 86_400_000);

    WxCheckinMonthDataResult result = util.getCheckinMonthData(start, end, List.of("u1"));

    assertThat(result.monthDataList()).hasSize(1);
    assertThat(result.monthDataList().get(0).getName()).isEqualTo("张三");
}
```

- [ ] **Step 9.2: Run — expect failure**

```
mvn test -Dtest=WxCheckinQueryUtilTest#getCheckinMonthData_returnsConvertedList
```

- [ ] **Step 9.3: Add `getCheckinMonthData`**

Append:

```java
// -------------------- getCheckinMonthData --------------------

/**
 * 拉取打卡月报。
 *
 * @param start   开始时间
 * @param end     结束时间
 * @param userIds 用户 ID 集合
 * @return 月报查询结果
 */
public WxCheckinMonthDataResult getCheckinMonthData(Date start, Date end, java.util.Collection<String> userIds) {
    java.util.List<String> normalized = normaliseUserIds(userIds);
    if (normalized.isEmpty()) {
        return WxCheckinMonthDataResult.empty();
    }
    validateRange(start, end);

    java.util.List<WxDateRange> segments = segmentDates(start, end, options.segmentDays());
    java.util.List<java.util.List<String>> batches = partitionUsers(normalized, options.userBatchSize());
    java.util.List<FanoutTask<java.util.List<me.chanjar.weixin.cp.bean.oa.WxCpCheckinMonthData>>> tasks = new java.util.ArrayList<>();
    for (WxDateRange seg : segments) {
        for (java.util.List<String> batch : batches) {
            tasks.add(new FanoutTask<>(seg, batch,
                    () -> wxCpService.getOaService().getCheckinMonthData(seg.startTime(), seg.endTime(), batch)));
        }
    }

    SimpleRateLimiter limiter = new SimpleRateLimiter(options.requestsPerSecond());
    java.util.List<java.util.concurrent.CompletableFuture<FanoutOutcome<java.util.List<me.chanjar.weixin.cp.bean.oa.WxCpCheckinMonthData>>>> futures =
            new java.util.ArrayList<>(tasks.size());
    for (FanoutTask<java.util.List<me.chanjar.weixin.cp.bean.oa.WxCpCheckinMonthData>> task : tasks) {
        futures.add(java.util.concurrent.CompletableFuture.supplyAsync(() -> withRetry(task, limiter), executor));
    }
    java.util.concurrent.CompletableFuture
            .allOf(futures.toArray(new java.util.concurrent.CompletableFuture<?>[0])).join();

    java.util.List<org.cy.qywx.vo.WxCheckinMonthDataVO> success = new java.util.ArrayList<>();
    java.util.List<WxCheckinFetchFailure> failures = new java.util.ArrayList<>();
    for (java.util.concurrent.CompletableFuture<FanoutOutcome<java.util.List<me.chanjar.weixin.cp.bean.oa.WxCpCheckinMonthData>>> f : futures) {
        FanoutOutcome<java.util.List<me.chanjar.weixin.cp.bean.oa.WxCpCheckinMonthData>> o = f.join();
        if (o.failure() != null) {
            failures.add(o.failure());
            continue;
        }
        if (o.success() != null) {
            for (me.chanjar.weixin.cp.bean.oa.WxCpCheckinMonthData d : o.success()) {
                org.cy.qywx.vo.WxCheckinMonthDataVO vo = WxCheckinConverter.fromMonthData(d);
                if (vo != null) {
                    success.add(vo);
                }
            }
        }
    }
    return new WxCheckinMonthDataResult(java.util.List.copyOf(success), java.util.List.copyOf(failures));
}

/** {@link WxDateRange} 重载。 */
public WxCheckinMonthDataResult getCheckinMonthData(WxDateRange range, java.util.Collection<String> userIds) {
    return getCheckinMonthData(range.startTime(), range.endTime(), userIds);
}
```

- [ ] **Step 9.4: Run — pass**

```
mvn test -Dtest=WxCheckinQueryUtilTest
```

- [ ] **Step 9.5: Commit**

```
git add src/main/java/org/cy/qywx/util/WxCheckinQueryUtil.java \
        src/test/java/org/cy/qywx/util/WxCheckinQueryUtilTest.java
git commit -m "feat: add getCheckinMonthData"
```

---

## Task 10: getScheduleList

**Files:**
- Modify: `src/main/java/org/cy/qywx/util/WxCheckinQueryUtil.java`
- Modify: `src/test/java/org/cy/qywx/util/WxCheckinQueryUtilTest.java`

`getScheduleList` returns a flat list (not partitioned by failure-record-friendly batches) — but we still apply 2-D batching for the API limits and merge.

- [ ] **Step 10.1: Add test**

Append:

```java
import me.chanjar.weixin.cp.bean.oa.WxCpCheckinSchedule;
import org.cy.qywx.vo.WxCheckinScheduleListItemVO;

@Test
void getScheduleList_returnsFlatList() throws Exception {
    WxCpCheckinSchedule s = new WxCpCheckinSchedule();
    s.setUserId("u1");
    when(oaService.getCheckinScheduleList(any(), any(), anyList())).thenReturn(List.of(s));

    Date start = new Date(1714521600000L);
    Date end = new Date(start.getTime() + 86_400_000);

    List<WxCheckinScheduleListItemVO> result = util.getScheduleList(start, end, List.of("u1"));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getUserId()).isEqualTo("u1");
}
```

- [ ] **Step 10.2: Run — expect failure**

```
mvn test -Dtest=WxCheckinQueryUtilTest#getScheduleList_returnsFlatList
```

- [ ] **Step 10.3: Add `getScheduleList`**

Append:

```java
// -------------------- getScheduleList --------------------

/**
 * 拉取排班信息。
 *
 * @param start   开始时间
 * @param end     结束时间
 * @param userIds 用户 ID 集合
 * @return 排班列表（失败的批次仅 WARN 级别日志，不抛出）
 */
public java.util.List<org.cy.qywx.vo.WxCheckinScheduleListItemVO> getScheduleList(
        Date start, Date end, java.util.Collection<String> userIds) {
    java.util.List<String> normalized = normaliseUserIds(userIds);
    if (normalized.isEmpty()) {
        return java.util.List.of();
    }
    validateRange(start, end);

    java.util.List<WxDateRange> segments = segmentDates(start, end, options.segmentDays());
    java.util.List<java.util.List<String>> batches = partitionUsers(normalized, options.userBatchSize());
    java.util.List<FanoutTask<java.util.List<me.chanjar.weixin.cp.bean.oa.WxCpCheckinSchedule>>> tasks = new java.util.ArrayList<>();
    for (WxDateRange seg : segments) {
        for (java.util.List<String> batch : batches) {
            tasks.add(new FanoutTask<>(seg, batch,
                    () -> wxCpService.getOaService().getCheckinScheduleList(seg.startTime(), seg.endTime(), batch)));
        }
    }

    SimpleRateLimiter limiter = new SimpleRateLimiter(options.requestsPerSecond());
    java.util.List<java.util.concurrent.CompletableFuture<FanoutOutcome<java.util.List<me.chanjar.weixin.cp.bean.oa.WxCpCheckinSchedule>>>> futures =
            new java.util.ArrayList<>(tasks.size());
    for (FanoutTask<java.util.List<me.chanjar.weixin.cp.bean.oa.WxCpCheckinSchedule>> task : tasks) {
        futures.add(java.util.concurrent.CompletableFuture.supplyAsync(() -> withRetry(task, limiter), executor));
    }
    java.util.concurrent.CompletableFuture
            .allOf(futures.toArray(new java.util.concurrent.CompletableFuture<?>[0])).join();

    java.util.List<org.cy.qywx.vo.WxCheckinScheduleListItemVO> out = new java.util.ArrayList<>();
    for (java.util.concurrent.CompletableFuture<FanoutOutcome<java.util.List<me.chanjar.weixin.cp.bean.oa.WxCpCheckinSchedule>>> f : futures) {
        FanoutOutcome<java.util.List<me.chanjar.weixin.cp.bean.oa.WxCpCheckinSchedule>> o = f.join();
        if (o.failure() != null) {
            log.warn("Schedule list batch failed: range=[{},{}], users={}, error={}",
                    o.failure().segmentStart(), o.failure().segmentEnd(),
                    o.failure().userIdBatch().size(), o.failure().errorMessage());
            continue;
        }
        if (o.success() != null) {
            for (me.chanjar.weixin.cp.bean.oa.WxCpCheckinSchedule s : o.success()) {
                org.cy.qywx.vo.WxCheckinScheduleListItemVO vo = WxCheckinConverter.fromScheduleItem(s);
                if (vo != null) {
                    out.add(vo);
                }
            }
        }
    }
    return java.util.List.copyOf(out);
}

/** {@link WxDateRange} 重载。 */
public java.util.List<org.cy.qywx.vo.WxCheckinScheduleListItemVO> getScheduleList(
        WxDateRange range, java.util.Collection<String> userIds) {
    return getScheduleList(range.startTime(), range.endTime(), userIds);
}
```

- [ ] **Step 10.4: Run — pass**

```
mvn test -Dtest=WxCheckinQueryUtilTest
```

- [ ] **Step 10.5: Commit**

```
git add src/main/java/org/cy/qywx/util/WxCheckinQueryUtil.java \
        src/test/java/org/cy/qywx/util/WxCheckinQueryUtilTest.java
git commit -m "feat: add getScheduleList"
```

---

## Task 11: Six business-semantic methods (late / early-leave / missing-card / absent / location-exception / device-exception)

**Files:**
- Modify: `src/main/java/org/cy/qywx/util/WxCheckinQueryUtil.java`
- Modify: `src/test/java/org/cy/qywx/util/WxCheckinQueryUtilTest.java`

All six are thin wrappers around `getCheckinDayData` + `WxCheckinConverter.explodeExceptions`.

- [ ] **Step 11.1: Add test**

Append:

```java
import org.cy.qywx.vo.WxCheckinExceptionItemVO;
import org.cy.qywx.vo.enums.WxCheckinExceptionTypeEnum;

@Test
void getLatePersons_filtersToLateOnly() throws Exception {
    WxCpCheckinDayData d = new WxCpCheckinDayData();
    WxCpCheckinDayData.BaseInfo base = new WxCpCheckinDayData.BaseInfo();
    base.setName("u");
    WxCpCheckinDayData.BaseInfo.Acctlnfo acct = new WxCpCheckinDayData.BaseInfo.Acctlnfo();
    acct.setUserId("u1");
    base.setAcctInfo(acct);
    base.setDate((int) (1714521600));
    d.setBaseInfo(base);

    WxCpCheckinDayData.ExceptionInfos late = new WxCpCheckinDayData.ExceptionInfos();
    late.setException(1);
    late.setCount(1);
    late.setDuration(600);

    WxCpCheckinDayData.ExceptionInfos absent = new WxCpCheckinDayData.ExceptionInfos();
    absent.setException(4);
    absent.setCount(1);
    absent.setDuration(28800);

    d.setExceptionInfos(List.of(late, absent));
    when(oaService.getCheckinDayData(any(), any(), anyList())).thenReturn(List.of(d));

    Date start = new Date(1714521600000L);
    Date end = new Date(start.getTime() + 86_400_000);

    List<WxCheckinExceptionItemVO> lates = util.getLatePersons(start, end, List.of("u1"));

    assertThat(lates).hasSize(1);
    assertThat(lates.get(0).getExceptionType()).isEqualTo(WxCheckinExceptionTypeEnum.LATE);
    assertThat(lates.get(0).getExceptionCount()).isEqualTo(1);
}
```

- [ ] **Step 11.2: Run — expect failure**

```
mvn test -Dtest=WxCheckinQueryUtilTest#getLatePersons_filtersToLateOnly
```

- [ ] **Step 11.3: Add the six methods**

Append:

```java
// -------------------- 业务语义方法 --------------------

/**
 * 时段内迟到记录（按 {@code (userId, date)} 粒度展开）。
 *
 * @param start   开始时间
 * @param end     结束时间
 * @param userIds 用户 ID 集合
 * @return 迟到记录列表
 */
public java.util.List<org.cy.qywx.vo.WxCheckinExceptionItemVO> getLatePersons(
        Date start, Date end, java.util.Collection<String> userIds) {
    return collectExceptions(start, end, userIds, org.cy.qywx.vo.enums.WxCheckinExceptionTypeEnum.LATE);
}

/** {@link WxDateRange} 重载。 */
public java.util.List<org.cy.qywx.vo.WxCheckinExceptionItemVO> getLatePersons(
        WxDateRange range, java.util.Collection<String> userIds) {
    return getLatePersons(range.startTime(), range.endTime(), userIds);
}

/** 时段内早退记录。 */
public java.util.List<org.cy.qywx.vo.WxCheckinExceptionItemVO> getEarlyLeavePersons(
        Date start, Date end, java.util.Collection<String> userIds) {
    return collectExceptions(start, end, userIds, org.cy.qywx.vo.enums.WxCheckinExceptionTypeEnum.EARLY_LEAVE);
}

public java.util.List<org.cy.qywx.vo.WxCheckinExceptionItemVO> getEarlyLeavePersons(
        WxDateRange range, java.util.Collection<String> userIds) {
    return getEarlyLeavePersons(range.startTime(), range.endTime(), userIds);
}

/** 时段内缺卡记录。 */
public java.util.List<org.cy.qywx.vo.WxCheckinExceptionItemVO> getMissingCardPersons(
        Date start, Date end, java.util.Collection<String> userIds) {
    return collectExceptions(start, end, userIds, org.cy.qywx.vo.enums.WxCheckinExceptionTypeEnum.MISSING_CARD);
}

public java.util.List<org.cy.qywx.vo.WxCheckinExceptionItemVO> getMissingCardPersons(
        WxDateRange range, java.util.Collection<String> userIds) {
    return getMissingCardPersons(range.startTime(), range.endTime(), userIds);
}

/** 时段内旷工记录。 */
public java.util.List<org.cy.qywx.vo.WxCheckinExceptionItemVO> getAbsentPersons(
        Date start, Date end, java.util.Collection<String> userIds) {
    return collectExceptions(start, end, userIds, org.cy.qywx.vo.enums.WxCheckinExceptionTypeEnum.ABSENT);
}

public java.util.List<org.cy.qywx.vo.WxCheckinExceptionItemVO> getAbsentPersons(
        WxDateRange range, java.util.Collection<String> userIds) {
    return getAbsentPersons(range.startTime(), range.endTime(), userIds);
}

/** 时段内地点异常记录。 */
public java.util.List<org.cy.qywx.vo.WxCheckinExceptionItemVO> getLocationExceptions(
        Date start, Date end, java.util.Collection<String> userIds) {
    return collectExceptions(start, end, userIds, org.cy.qywx.vo.enums.WxCheckinExceptionTypeEnum.LOCATION_EXCEPTION);
}

public java.util.List<org.cy.qywx.vo.WxCheckinExceptionItemVO> getLocationExceptions(
        WxDateRange range, java.util.Collection<String> userIds) {
    return getLocationExceptions(range.startTime(), range.endTime(), userIds);
}

/** 时段内设备异常记录。 */
public java.util.List<org.cy.qywx.vo.WxCheckinExceptionItemVO> getDeviceExceptions(
        Date start, Date end, java.util.Collection<String> userIds) {
    return collectExceptions(start, end, userIds, org.cy.qywx.vo.enums.WxCheckinExceptionTypeEnum.DEVICE_EXCEPTION);
}

public java.util.List<org.cy.qywx.vo.WxCheckinExceptionItemVO> getDeviceExceptions(
        WxDateRange range, java.util.Collection<String> userIds) {
    return getDeviceExceptions(range.startTime(), range.endTime(), userIds);
}

private java.util.List<org.cy.qywx.vo.WxCheckinExceptionItemVO> collectExceptions(
        Date start, Date end, java.util.Collection<String> userIds,
        org.cy.qywx.vo.enums.WxCheckinExceptionTypeEnum filter) {
    WxCheckinDayDataResult dayResult = getCheckinDayData(start, end, userIds);
    java.util.List<org.cy.qywx.vo.WxCheckinExceptionItemVO> out = new java.util.ArrayList<>();
    for (org.cy.qywx.vo.WxCheckinDayDataVO day : dayResult.dayDataList()) {
        out.addAll(WxCheckinConverter.explodeExceptions(day, filter));
    }
    return java.util.List.copyOf(out);
}
```

- [ ] **Step 11.4: Run — pass**

```
mvn test -Dtest=WxCheckinQueryUtilTest
```

- [ ] **Step 11.5: Commit**

```
git add src/main/java/org/cy/qywx/util/WxCheckinQueryUtil.java \
        src/test/java/org/cy/qywx/util/WxCheckinQueryUtilTest.java
git commit -m "feat: add 6 business-semantic exception methods"
```

---

## Task 12: getAttendanceReport (one-shot multi-bucket report)

**Files:**
- Modify: `src/main/java/org/cy/qywx/util/WxCheckinQueryUtil.java`
- Modify: `src/test/java/org/cy/qywx/util/WxCheckinQueryUtilTest.java`

- [ ] **Step 12.1: Add test**

Append:

```java
import org.cy.qywx.vo.WxAttendanceReportVO;

@Test
void getAttendanceReport_singleApiCallMultipleBuckets() throws Exception {
    AtomicInteger callCount = new AtomicInteger();
    when(oaService.getCheckinDayData(any(), any(), anyList())).thenAnswer(inv -> {
        callCount.incrementAndGet();
        WxCpCheckinDayData d = new WxCpCheckinDayData();
        WxCpCheckinDayData.BaseInfo base = new WxCpCheckinDayData.BaseInfo();
        base.setName("u");
        WxCpCheckinDayData.BaseInfo.Acctlnfo acct = new WxCpCheckinDayData.BaseInfo.Acctlnfo();
        acct.setUserId("u1");
        base.setAcctInfo(acct);
        base.setDate((int) 1714521600);
        d.setBaseInfo(base);

        WxCpCheckinDayData.ExceptionInfos late = new WxCpCheckinDayData.ExceptionInfos();
        late.setException(1); late.setCount(1); late.setDuration(600);
        WxCpCheckinDayData.ExceptionInfos absent = new WxCpCheckinDayData.ExceptionInfos();
        absent.setException(4); absent.setCount(1); absent.setDuration(0);

        d.setExceptionInfos(List.of(late, absent));
        return List.of(d);
    });

    Date start = new Date(1714521600000L);
    Date end = new Date(start.getTime() + 86_400_000);

    WxAttendanceReportVO report = util.getAttendanceReport(start, end, List.of("u1"));

    assertThat(callCount.get()).isEqualTo(1);
    assertThat(report.getLate()).hasSize(1);
    assertThat(report.getAbsent()).hasSize(1);
    assertThat(report.getEarlyLeave()).isEmpty();
    assertThat(report.getMissingCard()).isEmpty();
    assertThat(report.getTotalUsers()).isEqualTo(1);
    assertThat(report.getReportedUsers()).isEqualTo(1);
}
```

- [ ] **Step 12.2: Run — expect failure**

```
mvn test -Dtest=WxCheckinQueryUtilTest#getAttendanceReport_singleApiCallMultipleBuckets
```

- [ ] **Step 12.3: Add `getAttendanceReport`**

Append:

```java
// -------------------- getAttendanceReport --------------------

/**
 * 一次性聚合考勤报表。底层只调一次 {@code getcheckin_daydata}，按异常类型分桶。
 *
 * @param start   开始时间
 * @param end     结束时间
 * @param userIds 用户 ID 集合
 * @return 聚合报表
 */
public org.cy.qywx.vo.WxAttendanceReportVO getAttendanceReport(
        Date start, Date end, java.util.Collection<String> userIds) {
    java.util.List<String> normalized = normaliseUserIds(userIds);
    org.cy.qywx.vo.WxAttendanceReportVO report = new org.cy.qywx.vo.WxAttendanceReportVO();
    report.setRange(new WxDateRange(start, end));
    report.setTotalUsers(normalized.size());

    if (normalized.isEmpty()) {
        report.setLate(java.util.List.of());
        report.setEarlyLeave(java.util.List.of());
        report.setMissingCard(java.util.List.of());
        report.setAbsent(java.util.List.of());
        report.setLocationException(java.util.List.of());
        report.setDeviceException(java.util.List.of());
        report.setFailures(java.util.List.of());
        report.setReportedUsers(0);
        return report;
    }

    WxCheckinDayDataResult dayResult = getCheckinDayData(start, end, normalized);

    java.util.List<org.cy.qywx.vo.WxCheckinExceptionItemVO> late = new java.util.ArrayList<>();
    java.util.List<org.cy.qywx.vo.WxCheckinExceptionItemVO> earlyLeave = new java.util.ArrayList<>();
    java.util.List<org.cy.qywx.vo.WxCheckinExceptionItemVO> missingCard = new java.util.ArrayList<>();
    java.util.List<org.cy.qywx.vo.WxCheckinExceptionItemVO> absent = new java.util.ArrayList<>();
    java.util.List<org.cy.qywx.vo.WxCheckinExceptionItemVO> locationEx = new java.util.ArrayList<>();
    java.util.List<org.cy.qywx.vo.WxCheckinExceptionItemVO> deviceEx = new java.util.ArrayList<>();
    java.util.Set<String> reportedUsers = new java.util.HashSet<>();

    for (org.cy.qywx.vo.WxCheckinDayDataVO day : dayResult.dayDataList()) {
        if (day.getUserId() != null) {
            reportedUsers.add(day.getUserId());
        }
        for (org.cy.qywx.vo.WxCheckinExceptionItemVO item :
                WxCheckinConverter.explodeExceptions(day, null)) {
            switch (item.getExceptionType()) {
                case LATE -> late.add(item);
                case EARLY_LEAVE -> earlyLeave.add(item);
                case MISSING_CARD -> missingCard.add(item);
                case ABSENT -> absent.add(item);
                case LOCATION_EXCEPTION -> locationEx.add(item);
                case DEVICE_EXCEPTION -> deviceEx.add(item);
            }
        }
    }

    report.setLate(java.util.List.copyOf(late));
    report.setEarlyLeave(java.util.List.copyOf(earlyLeave));
    report.setMissingCard(java.util.List.copyOf(missingCard));
    report.setAbsent(java.util.List.copyOf(absent));
    report.setLocationException(java.util.List.copyOf(locationEx));
    report.setDeviceException(java.util.List.copyOf(deviceEx));
    report.setFailures(dayResult.failures());
    report.setReportedUsers(reportedUsers.size());
    return report;
}

/** {@link WxDateRange} 重载。 */
public org.cy.qywx.vo.WxAttendanceReportVO getAttendanceReport(
        WxDateRange range, java.util.Collection<String> userIds) {
    return getAttendanceReport(range.startTime(), range.endTime(), userIds);
}
```

- [ ] **Step 12.4: Run — pass**

```
mvn test -Dtest=WxCheckinQueryUtilTest
```

- [ ] **Step 12.5: Commit**

```
git add src/main/java/org/cy/qywx/util/WxCheckinQueryUtil.java \
        src/test/java/org/cy/qywx/util/WxCheckinQueryUtilTest.java
git commit -m "feat: add getAttendanceReport one-shot multi-bucket method"
```

---

## Task 13: Wire WxCpProperties.Checkin + Auto-Configuration

**Files:**
- Modify: `src/main/java/org/cy/qywx/config/WxCpProperties.java`
- Modify: `src/main/java/org/cy/qywx/config/QywxWecomAutoConfiguration.java`
- Modify: `src/test/java/org/cy/qywx/config/QywxWecomAutoConfigurationTest.java`

- [ ] **Step 13.1: Add test assertions to existing auto-config test**

Open `QywxWecomAutoConfigurationTest.java` and append:

```java
@Test
void checkinBeansRegisteredWhenCorpSecretConfigured() {
    contextRunner
            .withPropertyValues(
                    "wx.cp.corp-id=ww-test",
                    "wx.cp.corp-secret=secret",
                    "wx.cp.agent-id=1000001"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(org.cy.qywx.util.WxCheckinQueryUtil.class);
                assertThat(context).hasBean("qywxCheckinExecutor");
            });
}

@Test
void checkinBeansAbsentWhenCorpSecretMissing() {
    contextRunner
            .run(context -> {
                assertThat(context).doesNotHaveBean(org.cy.qywx.util.WxCheckinQueryUtil.class);
                assertThat(context).doesNotHaveBean("qywxCheckinExecutor");
            });
}

@Test
void checkinPropertiesPropagated() {
    contextRunner
            .withPropertyValues(
                    "wx.cp.corp-id=ww-test",
                    "wx.cp.corp-secret=secret",
                    "wx.cp.agent-id=1000001",
                    "wx.cp.checkin.segment-days=15",
                    "wx.cp.checkin.user-batch-size=50",
                    "wx.cp.checkin.requests-per-second=2"
            )
            .run(context -> {
                WxCpProperties props = context.getBean(WxCpProperties.class);
                assertThat(props.getCheckin().getSegmentDays()).isEqualTo(15);
                assertThat(props.getCheckin().getUserBatchSize()).isEqualTo(50);
                assertThat(props.getCheckin().getRequestsPerSecond()).isEqualTo(2D);
            });
}
```

> If existing tests use a different `contextRunner` setup, copy from those — keep style consistent. Tests should fail because (a) `WxCpProperties.Checkin` does not yet exist and (b) `QywxWecomAutoConfiguration` does not register the new beans.

- [ ] **Step 13.2: Run — expect compile failure**

```
mvn test -Dtest=QywxWecomAutoConfigurationTest
```

- [ ] **Step 13.3: Add `Checkin` nested class to `WxCpProperties`**

Open `WxCpProperties.java`. Add a `private Checkin checkin = new Checkin();` field plus accessor (mirroring the existing `Hr` pair), then append the nested class at the end:

```java
    public Checkin getCheckin() {
        return checkin;
    }

    public void setCheckin(Checkin checkin) {
        this.checkin = checkin;
    }

    /**
     * 打卡 / 考勤查询调优参数。复用顶层 {@code corpSecret}，因此本嵌套类没有 {@code secret} 字段。
     *
     * @author CY
     * Copyright (c) CY
     */
    public static class Checkin {
        /** 单段最大天数（API 上限 30）。 */
        private int segmentDays = 30;
        /** 单批最大 userId 数（API 上限 100）。 */
        private int userBatchSize = 100;
        /** 单批次最大重试次数。 */
        private int maxRetryAttempts = 3;
        /** 重试初始退避毫秒。 */
        private long retryBackoffMillis = 500L;
        /** 全局每秒请求数（&le;0 不限流）。 */
        private double requestsPerSecond = 0D;
        /** {@code qywxCheckinExecutor} 线程数。 */
        private int executorThreads = 8;

        public int getSegmentDays() { return segmentDays; }
        public void setSegmentDays(int segmentDays) { this.segmentDays = segmentDays; }
        public int getUserBatchSize() { return userBatchSize; }
        public void setUserBatchSize(int userBatchSize) { this.userBatchSize = userBatchSize; }
        public int getMaxRetryAttempts() { return maxRetryAttempts; }
        public void setMaxRetryAttempts(int maxRetryAttempts) { this.maxRetryAttempts = maxRetryAttempts; }
        public long getRetryBackoffMillis() { return retryBackoffMillis; }
        public void setRetryBackoffMillis(long retryBackoffMillis) { this.retryBackoffMillis = retryBackoffMillis; }
        public double getRequestsPerSecond() { return requestsPerSecond; }
        public void setRequestsPerSecond(double requestsPerSecond) { this.requestsPerSecond = requestsPerSecond; }
        public int getExecutorThreads() { return executorThreads; }
        public void setExecutorThreads(int executorThreads) { this.executorThreads = executorThreads; }
    }
```

- [ ] **Step 13.4: Wire beans in `QywxWecomAutoConfiguration`**

Add at the top of the class with the other constants:

```java
public static final String CHECKIN_EXECUTOR_BEAN_NAME = "qywxCheckinExecutor";
```

Add at the end of the class:

```java
@Bean(name = CHECKIN_EXECUTOR_BEAN_NAME, destroyMethod = "shutdown")
@ConditionalOnMissingBean(name = CHECKIN_EXECUTOR_BEAN_NAME)
public java.util.concurrent.ExecutorService qywxCheckinExecutor(WxCpProperties properties) {
    int threads = Math.max(1, properties.getCheckin().getExecutorThreads());
    return java.util.concurrent.Executors.newFixedThreadPool(threads);
}

@Bean
@ConditionalOnMissingBean
@ConditionalOnBean(me.chanjar.weixin.cp.api.WxCpService.class)
public org.cy.qywx.util.WxCheckinQueryUtil wxCheckinQueryUtil(
        me.chanjar.weixin.cp.api.WxCpService wxCpService,
        @org.springframework.beans.factory.annotation.Qualifier(CHECKIN_EXECUTOR_BEAN_NAME)
                java.util.concurrent.ExecutorService checkinExecutor,
        WxCpProperties properties) {
    WxCpProperties.Checkin c = properties.getCheckin();
    org.cy.qywx.util.WxCheckinQueryOptions options = new org.cy.qywx.util.WxCheckinQueryOptions(
            c.getSegmentDays(), c.getUserBatchSize(),
            c.getMaxRetryAttempts(), c.getRetryBackoffMillis(),
            c.getRequestsPerSecond());
    return new org.cy.qywx.util.WxCheckinQueryUtil(wxCpService, checkinExecutor, options);
}
```

- [ ] **Step 13.5: Run — pass**

```
mvn test -Dtest=QywxWecomAutoConfigurationTest
mvn test
```

The full `mvn test` should now have all WxCheckin* tests + existing tests green.

- [ ] **Step 13.6: Commit**

```
git add src/main/java/org/cy/qywx/config/WxCpProperties.java \
        src/main/java/org/cy/qywx/config/QywxWecomAutoConfiguration.java \
        src/test/java/org/cy/qywx/config/QywxWecomAutoConfigurationTest.java
git commit -m "feat: wire WxCheckinQueryUtil into Spring auto-configuration"
```

---

## Task 14: Docs + version bump

**Files:**
- Modify: `README.md`
- Modify: `README.zh-CN.md`
- Modify: `CLAUDE.md`
- Modify: `pom.xml`

No tests for documentation. Just verify the project still compiles after the version bump.

- [ ] **Step 14.1: Append "Checkin Query" section to `README.md`**

Add at the end of the file:

```markdown
## Checkin Query

`WxCheckinQueryUtil` queries WeCom attendance data. It reuses the primary `WxCpService` (no separate secret) and exposes:

- `getCheckinGroups()` — list all attendance group configurations
- `getCheckinRecords(start, end, userIds)` — raw checkin records (use `CHECKIN_TYPE_NORMAL/OUTSIDE/ALL` for finer control)
- `getCheckinDayData(start, end, userIds)` — daily report
- `getCheckinMonthData(start, end, userIds)` — monthly report
- `getScheduleList(start, end, userIds)` — schedule list
- Business-level helpers: `getLatePersons / getEarlyLeavePersons / getMissingCardPersons / getAbsentPersons / getLocationExceptions / getDeviceExceptions`
- One-shot aggregate: `getAttendanceReport(start, end, userIds)` returning all six exception buckets in one API call

The starter hides WeCom's `≤100 userIds / call` and `≤30 days / call` limits via internal 2-D batching with retry and rate-limit knobs under `wx.cp.checkin.*`.

```yaml
wx.cp:
  corp-id: ww1234abcd
  corp-secret: xxx
  agent-id: 1000001
  checkin:
    segment-days: 30
    user-batch-size: 100
    requests-per-second: 5
    executor-threads: 8
```

```java
@Autowired
private WxCheckinQueryUtil checkin;

List<WxCheckinGroupVO> groups = checkin.getCheckinGroups();

WxDateRange april = ...;          // build a 30-day range
List<String> userIds = ...;        // explicit list — no auto-fetch
List<WxCheckinExceptionItemVO> late = checkin.getLatePersons(april, userIds);

WxAttendanceReportVO report = checkin.getAttendanceReport(april, userIds);
report.getLate();
report.getAbsent();
report.getFailures();
```

- [ ] **Step 14.2: Mirror the section in `README.zh-CN.md`**

Translate the same content; reuse the example block verbatim.

- [ ] **Step 14.3: Update `CLAUDE.md`**

Insert at the end of "Two `WxCpService` instances":

```
Note: starting in 1.0.2, `WxCheckinQueryUtil` reuses the primary `WxCpService` (like Approval) — checkin endpoints live on `WxCpOaService` (`getCheckinData`, `getCheckinDayData`, `getCheckinMonthData`, `getCheckinScheduleList`, `getCropCheckinOption` — note the "Crop" typo).
```

Insert at the end of "Cross-cutting patterns to preserve when editing":

```
- **User-batching for ≤100/call APIs**: `WxCheckinQueryUtil` partitions userIds into batches of `userBatchSize` and crosses with date segments. Reuse the same `partitionUsers` + `segmentDates` helpers when adding endpoints with similar API limits, instead of re-implementing.
```

- [ ] **Step 14.4: Bump `pom.xml` version**

Change `<version>1.0.1</version>` → `<version>1.0.2</version>`.

- [ ] **Step 14.5: Final compile + full test pass**

```
mvn clean test
```

Expected: all tests green, no warnings related to checkin module.

- [ ] **Step 14.6: Commit**

```
git add README.md README.zh-CN.md CLAUDE.md pom.xml
git commit -m "docs: document Checkin Query module and bump to 1.0.2"
```

---

## Self-Review

After all tasks compile and tests pass:

1. **Spec coverage check** — every public method listed in spec §6 has at least one test:
   - `getCheckinGroups()` ✔ Task 6
   - `getCheckinRecords(...)` × 4 overloads ✔ Task 8
   - `getCheckinDayData(...)` × 2 ✔ Task 7
   - `getCheckinMonthData(...)` × 2 ✔ Task 9
   - `getScheduleList(...)` × 2 ✔ Task 10
   - 6 × business semantic methods × 2 ✔ Task 11
   - `getAttendanceReport(...)` × 2 ✔ Task 12
   - Spec §7.6 logging — covered by `log.info` in fan-out engine
   - Spec §11 risk mitigations — N/A (decisions, not code)

2. **Convention check** — every new class file should have `@author CY` and `Copyright (c) CY` in the Javadoc header, and every public method should have Chinese Javadoc.

3. **Lint** — Run `mvn clean compile -X 2>&1 | grep -i warning` once at the end; resolve any new warnings (unused imports, raw types, etc.) before finalising.

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-05-04-wecom-checkin.md`. Two execution options:

**1. Subagent-Driven (recommended)** — fresh subagent per task, review between tasks, fast iteration.

**2. Inline Execution** — execute tasks in this session using executing-plans, batch execution with checkpoints.

Which approach?
