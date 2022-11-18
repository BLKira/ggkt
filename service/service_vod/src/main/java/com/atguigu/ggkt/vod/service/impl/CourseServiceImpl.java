package com.atguigu.ggkt.vod.service.impl;

import com.atguigu.ggkt.model.vod.Course;
import com.atguigu.ggkt.model.vod.CourseDescription;
import com.atguigu.ggkt.model.vod.Subject;
import com.atguigu.ggkt.model.vod.Teacher;
import com.atguigu.ggkt.vo.vod.CourseFormVo;
import com.atguigu.ggkt.vo.vod.CoursePublishVo;
import com.atguigu.ggkt.vo.vod.CourseQueryVo;
import com.atguigu.ggkt.vod.mapper.CourseMapper;
import com.atguigu.ggkt.vod.service.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 课程 服务实现类
 * </p>
 *
 * @author atguigu
 * @since 2022-11-13
 */
@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseService {

    @Autowired
    private TeacherService teacherService;

    @Autowired
    private SubjectService subjectService;

    @Autowired
    private CourseDescriptionService descriptionService;

    @Autowired
    private VideoService videoService;

    @Autowired
    private ChapterService chapterService;

    //点播课程列表
    @Override
    public Map<String, Object> findPageCourse(Page<Course> pageParam,
                                              CourseQueryVo courseQueryVo) {
        //获取条件值
        String title = courseQueryVo.getTitle();
        Long subjectId = courseQueryVo.getSubjectId();//二层分类
        Long subjectParentId = courseQueryVo.getSubjectParentId();//一层分类
        Long teacherId = courseQueryVo.getTeacherId();
        //判断条件为空，封装条件
        QueryWrapper<Course> wrapper = new QueryWrapper<>();
        if (!StringUtils.isEmpty(title)) {
            wrapper.like("title", title);
        }
        if (!StringUtils.isEmpty(subjectId)) {
            wrapper.eq("subject_id", subjectId);
        }
        if (!StringUtils.isEmpty(subjectParentId)) {
            wrapper.eq("subject_parent_id", subjectParentId);
        }
        if (!StringUtils.isEmpty(teacherId)) {
            wrapper.eq("teacher_id", teacherId);
        }

        //调用方法实现条件查询分页
        Page<Course> pages = baseMapper.selectPage(pageParam, wrapper);
        long totalCount = pages.getTotal();
        long totalPage = pages.getPages();
        List<Course> list = pages.getRecords();
        //查询数据里面有几个id
        //讲师id  课程分类id（一层 和 二层）
        //获取这些id对应名称，进行封装，最终显示
        list.stream().forEach(item -> {
            this.getNameById(item);
        });
        //封装数据
        Map<String, Object> map = new HashMap<>();
        map.put("totalCount", totalCount);
        map.put("totalPage", totalPage);
        map.put("records", list);
        return map;
    }

    //添加课程基本信息
    @Override
    public Long saveCourseInfo(CourseFormVo courseFormVo) {
        //添加课程基本信息，操作course表
        Course course = new Course();
        BeanUtils.copyProperties(courseFormVo, course);
        baseMapper.insert(course);

        //添加课程描述信息，操作course_description
        CourseDescription courseDescription = new CourseDescription();
        courseDescription.setDescription(courseFormVo.getDescription());
        //设置课程id
        courseDescription.setId(course.getId());
        descriptionService.save(courseDescription);

        return course.getId();
    }

    @Override
    public CourseFormVo getCourseInfoById(Long id) {
        Course course = baseMapper.selectById(id);
        if (course == null) {
            return null;
        }
        CourseDescription courseDescription = descriptionService.getById(id);
//        QueryWrapper<CourseDescription> wrapper = new QueryWrapper<>();
//        wrapper.eq("course_id", id);
//        CourseDescription courseDescription = descriptionService.getOne(wrapper);

        CourseFormVo courseFormVo = new CourseFormVo();
        BeanUtils.copyProperties(course, courseFormVo);

        if (courseDescription != null) {
            courseFormVo.setDescription(courseDescription.getDescription());
        }
        return courseFormVo;
    }

    @Override
    public void updateCourseId(CourseFormVo courseFormVo) {
        Course course = new Course();
        BeanUtils.copyProperties(courseFormVo, course);
        baseMapper.updateById(course);

        CourseDescription description = new CourseDescription();
        description.setDescription(courseFormVo.getDescription());
        description.setId(course.getId());
        descriptionService.updateById(description);
    }

    @Override
    public CoursePublishVo getCoursePublishVo(Long id) {
        return baseMapper.selectCoursePublishVoById(id);
    }

    @Override
    public void publishCourse(Long id) {
        Course course = baseMapper.selectById(id);
        course.setStatus(1);
        course.setPublishTime(new Date());
        baseMapper.updateById(course);
    }

    @Override
    public void removeCourseId(Long id) {
            videoService.removeVideoByCourseId(id);
            chapterService.removeChapterByCourseId(id);
            descriptionService.removeById(id);
            baseMapper.deleteById(id);
    }

    @Override
    public List<Course> findlist() {
        return null;
    }

    private Course getNameById(Course course) {
        //根据讲师id获取讲师名称
        Teacher teacher = teacherService.getById(course.getTeacherId());
        if (teacher != null) {
            String name = teacher.getName();
            course.getParam().put("teacherName", name);
        }

        //根据课程分类id获取课程分类名称
        Subject subjectOne = subjectService.getById(course.getSubjectParentId());
        if (subjectOne != null) {
            course.getParam().put("subjectParentTitle", subjectOne.getTitle());
        }
        Subject subjectTwo = subjectService.getById(course.getSubjectId());
        if (subjectTwo != null) {
            course.getParam().put("subjectTitle", subjectTwo.getTitle());
        }
        return course;
    }

}

