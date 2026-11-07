package com.example.qi.module.publish.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.qi.module.auth.service.AuthService;
import com.example.qi.module.publish.entity.PublishRecord;
import com.example.qi.module.publish.mapper.PublishRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 发布记录服务（ADM-13/14）。
 * 内容发布按“插入发布中记录 → 执行发布 → 更新为成功/失败”推进（7.15.4），
 * 失败记录保留且不影响线上版本。
 */
@Service
@RequiredArgsConstructor
public class PublishRecordService {

    /** 发布状态（与表注释一致） */
    public static final int PUBLISHING = 0;
    public static final int SUCCESS = 1;
    public static final int FAILED = 2;

    private final PublishRecordMapper publishRecordMapper;
    private final AuthService authService;

    /** 计算某内容对象/页面的下一个版本号（按对象独立递增，7.15.7） */
    public int nextVersion(String recordType, String pageCode, String objectType, Long objectId) {
        PublishRecord latest = publishRecordMapper.selectOne(Wrappers.<PublishRecord>lambdaQuery()
                .eq(PublishRecord::getRecordType, recordType)
                .eq(pageCode != null, PublishRecord::getPageCode, pageCode)
                .eq(objectType != null, PublishRecord::getObjectType, objectType)
                .eq(objectId != null, PublishRecord::getObjectId, objectId)
                .orderByDesc(PublishRecord::getVersion)
                .last("LIMIT 1"));
        return latest == null ? 1 : latest.getVersion() + 1;
    }

    /** 创建“发布中”记录 */
    public PublishRecord createPublishing(String recordType, String pageCode, String objectType,
                                          Long objectId, String modifySummary, String snapshot) {
        PublishRecord record = new PublishRecord();
        record.setRecordType(recordType);
        record.setPageCode(pageCode);
        record.setObjectType(objectType);
        record.setObjectId(objectId);
        record.setVersion(nextVersion(recordType, pageCode, objectType, objectId));
        record.setModifySummary(modifySummary);
        record.setPublishStatus(PUBLISHING);
        record.setSnapshot(snapshot);
        AuthService.Operator operator = authService.currentOperator();
        record.setOperatorId(operator.id());
        record.setOperatorName(operator.name());
        publishRecordMapper.insert(record);
        return record;
    }

    /** 更新发布结果 */
    public void finish(PublishRecord record, boolean success, String failReason) {
        record.setPublishStatus(success ? SUCCESS : FAILED);
        record.setFailReason(failReason);
        publishRecordMapper.updateById(record);
    }

    /** 内容发布记录分页（ADM-13：后台统一查看，仅查看不回滚，D-23） */
    public Page<PublishRecord> pageContentRecords(long pageNum, long pageSize,
                                                  String objectType, String keyword) {
        return publishRecordMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<PublishRecord>lambdaQuery()
                        .eq(PublishRecord::getRecordType, "content")
                        .eq(objectType != null && !objectType.isBlank(), PublishRecord::getObjectType, objectType)
                        .like(keyword != null && !keyword.isBlank(), PublishRecord::getModifySummary, keyword)
                        .orderByDesc(PublishRecord::getPublishTime));
    }

    public List<PublishRecord> listByObject(String objectType, Long objectId) {
        return publishRecordMapper.selectList(Wrappers.<PublishRecord>lambdaQuery()
                .eq(PublishRecord::getRecordType, "content")
                .eq(PublishRecord::getObjectType, objectType)
                .eq(PublishRecord::getObjectId, objectId)
                .orderByDesc(PublishRecord::getVersion));
    }
}
