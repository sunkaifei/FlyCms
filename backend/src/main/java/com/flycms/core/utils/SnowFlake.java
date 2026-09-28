package com.flycms.core.utils; /**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 16:15 2018/12/18
 */
/**
 * 描述: Twitter的分布式自增ID雪花算法snowflake (Java版)
 * https://github.com/souyunku/SnowFlake
 *
 * @author yanpenglei
 * @create 2018-03-13 12:37
 **/
public class SnowFlake {

    /**
     * 起始的时间戳
     */
    private final static long START_STMP = 1480166465631L;

    /**
     * 每一部分占用的位数
     */
    private final static long SEQUENCE_BIT = 12; //序列号占用的位数
    private final static long MACHINE_BIT = 5;   //机器标识占用的位数
    private final static long DATACENTER_BIT = 5;//数据中心占用的位数

    /**
     * 每一部分的最大值
     */
    private final static long MAX_DATACENTER_NUM = -1L ^ (-1L << DATACENTER_BIT);
    private final static long MAX_MACHINE_NUM = -1L ^ (-1L << MACHINE_BIT);
    private final static long MAX_SEQUENCE = -1L ^ (-1L << SEQUENCE_BIT);

    /**
     * 每一部分向左的位移
     */
    private final static long MACHINE_LEFT = SEQUENCE_BIT;
    private final static long DATACENTER_LEFT = SEQUENCE_BIT + MACHINE_BIT;
    private final static long TIMESTMP_LEFT = DATACENTER_LEFT + DATACENTER_BIT;

    private long datacenterId;  //数据中心
    private long machineId;     //机器标识

    /**
     * 序列号与上次时间戳必须是「全局共享」状态。
     * 历史 Bug：调用方在 38 处使用 new SnowFlake(2, 3) 临时建实例，
     * 每个新实例的 sequence 都从 0 开始、lastStmp 为 -1，
     * 导致同一毫秒内不同实例生成完全相同的 ID（主键冲突）。
     * 现改为类级共享状态 + 静态同步，保证进程内唯一。
     */
    private static long sequence = 0L;      //序列号
    private static long lastStmp = -1L;     //上一次时间戳

    /**
     * 进程内共享的生成器（数据中心 2、机器 3）
     */
    private static final SnowFlake INSTANCE = new SnowFlake(2, 3);

    /**
     * 获取进程内共享的雪花 ID 生成器，取代各处 new SnowFlake(2, 3)
     */
    public static SnowFlake getInstance() {
        return INSTANCE;
    }

    public SnowFlake(long datacenterId, long machineId) {
        if (datacenterId > MAX_DATACENTER_NUM || datacenterId < 0) {
            throw new IllegalArgumentException("datacenterId can't be greater than MAX_DATACENTER_NUM or less than 0");
        }
        if (machineId > MAX_MACHINE_NUM || machineId < 0) {
            throw new IllegalArgumentException("machineId can't be greater than MAX_MACHINE_NUM or less than 0");
        }
        this.datacenterId = datacenterId;
        this.machineId = machineId;
    }

    /**
     * 产生下一个ID
     *
     * @return
     */
    public long nextId() {
        return nextId(datacenterId, machineId);
    }

    /**
     * 生成下一个 ID。
     * 必须是 static synchronized：sequence/lastStmp 是类级共享状态，
     * 若只加在实例方法上，不同实例锁的是各自的 this，无法保证互斥。
     */
    private static synchronized long nextId(long datacenterId, long machineId) {
        long currStmp = getNewstmp();
        if (currStmp < lastStmp) {
            throw new RuntimeException("Clock moved backwards.  Refusing to generate id");
        }

        if (currStmp == lastStmp) {
            //相同毫秒内，序列号自增
            sequence = (sequence + 1) & MAX_SEQUENCE;
            //同一毫秒的序列数已经达到最大
            if (sequence == 0L) {
                currStmp = getNextMill();
            }
        } else {
            //不同毫秒内，序列号置为0
            sequence = 0L;
        }

        lastStmp = currStmp;

        return (currStmp - START_STMP) << TIMESTMP_LEFT //时间戳部分
                | datacenterId << DATACENTER_LEFT       //数据中心部分
                | machineId << MACHINE_LEFT             //机器标识部分
                | sequence;                             //序列号部分
    }

    private static long getNextMill() {
        long mill = getNewstmp();
        while (mill <= lastStmp) {
            mill = getNewstmp();
        }
        return mill;
    }

    private static long getNewstmp() {
        return System.currentTimeMillis();
    }
}
