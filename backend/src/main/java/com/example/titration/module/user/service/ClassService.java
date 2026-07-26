package com.example.titration.module.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.titration.common.exception.BusinessException;
import com.example.titration.module.user.entity.ClassEntity;
import com.example.titration.module.user.entity.User;
import com.example.titration.module.user.mapper.ClassMapper;
import com.example.titration.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClassService {

    private final ClassMapper classMapper;
    private final UserMapper userMapper;

    public List<ClassEntity> listClasses() {
        LambdaQueryWrapper<ClassEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(ClassEntity::getId);
        return classMapper.selectList(wrapper);
    }

    public ClassEntity getClassById(Long id) {
        ClassEntity clazz = classMapper.selectById(id);
        if (clazz == null) {
            throw new BusinessException(404, "班级不存在");
        }
        return clazz;
    }

    public ClassEntity createClass(ClassEntity clazz) {
        ClassEntity existing = classMapper.selectOne(
                new LambdaQueryWrapper<ClassEntity>()
                        .eq(ClassEntity::getClassName, clazz.getClassName()));
        if (existing != null) {
            throw new BusinessException(400, "班级名称已存在");
        }
        classMapper.insert(clazz);
        return clazz;
    }

    public ClassEntity updateClass(Long id, ClassEntity clazz) {
        getClassById(id);
        clazz.setId(id);
        classMapper.updateById(clazz);
        return classMapper.selectById(id);
    }

    public void deleteClass(Long id) {
        // 检查班级下是否有学生
        Long studentCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getClassId, id));
        if (studentCount > 0) {
            throw new BusinessException(400,
                    "班级下还有 " + studentCount + " 名学生，无法删除");
        }
        classMapper.deleteById(id);
    }
}
