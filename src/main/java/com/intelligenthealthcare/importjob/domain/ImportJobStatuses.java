package com.intelligenthealthcare.importjob.domain;

/**
 * 导入任务在 {@code import_job_record.status} 中使用的状态值。
 */
public final class ImportJobStatuses {

    private ImportJobStatuses() {}

    /** 已创建，尚未开始执行（本实现中写库后立即进入 RUNNING，一般不会出现中间态） */
    public static final String PENDING = "PENDING";
    public static final String RUNNING = "RUNNING";
    /** 全部行处理完毕且无解析失败行 */
    public static final String SUCCESS = "SUCCESS";
    /** 有失败行或产生待办审核项，但任务整体执行完 */
    public static final String PARTIAL_SUCCESS = "PARTIAL_SUCCESS";
    /** 文件无法读取或表头/格式错误等致命问题 */
    public static final String FAILED = "FAILED";
}
