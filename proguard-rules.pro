-dontoptimize
-dontpreverify
-ignorewarnings
-useuniqueclassmembernames
-overloadaggressively
-allowaccessmodification
-adaptclassstrings
-keepattributes Signature,*Annotation*,EnclosingMethod,InnerClasses,Exceptions

# 输出映射，便于核对混淆是否生效
-printmapping target/mapping.txt
-printseeds target/seeds.txt

# 仅保留对外暴露 API 类名（供 esop2 直接 import）
-keep class org.cy.qywx.util.WxContactQueryUtil { public *; }
-keep class org.cy.qywx.util.WxApprovalQueryUtil { public *; }
-keep class org.cy.qywx.config.WxCpConfig { *; }

# DTO 对外序列化使用，保持字段名稳定
-keep class org.cy.qywx.vo.** { *; }

# 仅保留 Spring 注解相关成员元数据，避免运行时注入失效
-keepclassmembers class org.cy.qywx.** {
    @org.springframework.beans.factory.annotation.Value *;
    @org.springframework.context.annotation.Bean *;
}

# 三方库与 JDK 告警抑制
-dontwarn me.chanjar.weixin.**
-dontwarn java.**
-dontwarn javax.**
-dontwarn jakarta.**
-dontwarn org.springframework.**
