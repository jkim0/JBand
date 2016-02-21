// IScheduleManageService.aidl
package com.itj.jband.schedule;

// Declare any non-default types here with import statements
import com.itj.jband.schedule.Schedule;

interface IScheduleManageService {
    void updateSchedule(in Schedule schedule);
    void cancelSchedule(in Schedule schedule);
}
