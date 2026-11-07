package com.example.qi.module.publish.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 发布记录（ADM-13/14），对应表 publish_record。
 * 追加型数据：发布成功后记录不可被后续编辑覆盖（7.14）。
 */
@Data
@TableName("publish_record")
public class PublishRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** page 页面配置 / content 内容对象 */
    private String recordType;

    private String pageCode;

    private String objectType;

    private Long objectId;

    /** 版本号，按页面/对象独立递增（7.15.7） */
    private Integer version;

    private String batchNo;

    /** 修改内容说明，必填（7.14 五项必填信息之一） */
    private String modifySummary;

    /** 系统自动汇总的变更项（JSON） */
    private String autoChanges;

    /** 0 发布中 / 1 发布成功 / 2 发布失败 / 3 回滚成功 / 4 回滚失败 */
    private Integer publishStatus;

    private Integer rollbackFromVersion;

    private Long operatorId;

    private String operatorName;

    /** 发布成功时的内容/配置快照（JSON） */
    private String snapshot;

    private String failReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime publishTime;
}
